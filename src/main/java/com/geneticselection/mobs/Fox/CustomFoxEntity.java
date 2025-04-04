package com.geneticselection.mobs.Fox;

import com.geneticselection.attributes.AttributeKey;
import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.ModEntities; // Use your ModEntities
import com.geneticselection.utils.DescriptionRenderer;

// Vanilla Fox related imports
import net.minecraft.block.*;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.*; // Includes FoxEntity, AnimalEntity, ChickenEntity, RabbitEntity, TurtleEntity, FishEntity
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.ItemTags; // For FOX_FOOD
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.*;
import net.minecraft.util.math.BlockPos;

// Other necessary imports
import org.jetbrains.annotations.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.Map; // For MobAttributes map constructor
import java.util.EnumMap; // For MobAttributes map constructor

// Note: Removed AttributeCarrier interface as direct implementation is clearer

public class CustomFoxEntity extends FoxEntity {

    // --- Custom Instance Attributes ---
    // Base attributes inherited from species average
    private MobAttributes mobAttributes;
    private double MaxHp; // Base MaxHp from mobAttributes
    private double Speed; // Base Speed from mobAttributes

    // Instance-specific dynamic state
    private double ELvl;
    private double MaxEnergy;
    private int breedingCooldown; // In ticks
    private int tickAge = 0;
    private int ticksSinceLastBreeding = 0;

    // Instance-specific evolution bonuses
    private double bonusAttack = 0.0;
    private double bonusHealth = 0.0;
    private double bonusSpeed = 0.0; // Speed bonus multiplier
    private int killCount = 0;

    // Instance state flags
    private boolean wasRecentlyHit = false;
    private static TrackedData<Optional<UUID>> OWNER;
    private static TrackedData<Optional<UUID>> OTHER_TRUSTED;
    static Predicate<ItemEntity> PICKABLE_DROP_FILTER;

    static {
        OWNER = DataTracker.registerData(CustomFoxEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
        OTHER_TRUSTED = DataTracker.registerData(CustomFoxEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
        PICKABLE_DROP_FILTER = (item) -> !item.cannotPickup() && item.isAlive();
    }

    // --- Target Predicates ---
    // Defines what this fox considers prey
    public static final Predicate<LivingEntity> PREY_PREDICATE = (entity) -> {
        EntityType<?> entityType = entity.getType();
        // Target custom small animals + vanilla chickens/rabbits/fish/baby turtles
        return entityType == ModEntities.CUSTOM_CHICKEN
            || entityType == ModEntities.CUSTOM_RABBIT
            // Add other small custom prey if applicable
            || entityType == EntityType.CHICKEN
            || entityType == EntityType.RABBIT
            || entityType == EntityType.COD
            || entityType == EntityType.SALMON
            || entityType == EntityType.TROPICAL_FISH
            || (entity instanceof TurtleEntity && entity.isBaby()); // Baby turtles only
    };
    // Filters prey based on game rules (not tamed, not self, etc.)
    private static final Predicate<LivingEntity> HUNT_TARGET_PREDICATE = (entity) -> {
        if (!PREY_PREDICATE.test(entity)) return false; // Must be valid prey type
        if (entity instanceof TameableEntity && ((TameableEntity) entity).isTamed()) return false; // Cannot be tamed
        if (entity instanceof FoxEntity) return false; // Cannot be another fox
        return true;
    };

    // --- Constructor ---
    public CustomFoxEntity(EntityType<? extends FoxEntity> entityType, World world) {
        super(entityType, world);

        // Initialize base attributes from global manager only on first spawn
        if (this.mobAttributes == null) {
            initFromGlobalAttributes(entityType);
        }

        // Initialize instance-specific stats
        this.MaxEnergy = 10.0; // Start low
        this.ELvl = this.mobAttributes.getEnergyLvl(); // Initialize from base default
        this.breedingCooldown = 4000 + random.nextInt(4000); // Default 3-6 mins cooldown
        this.ticksSinceLastBreeding = 0;
        this.tickAge = 0;
        this.bonusAttack = 0.0;
        this.bonusHealth = 0.0;
        this.bonusSpeed = 0.0;
        this.killCount = 0;

        // Apply initial attributes (base + bonuses)
        this.applyBonuses();

        if (!this.getWorld().isClient) {
            updateDescription(this);
        }
    }

    // Helper to initialize from global attributes
    private void initFromGlobalAttributes(EntityType<?> entityType) {
        this.mobAttributes = GlobalAttributesManager.getAttributes(entityType);
        this.MaxHp = this.mobAttributes.getMaxHealth(); // Store base value
        this.Speed = this.mobAttributes.getMovementSpeed(); // Store base value
    }

    // --- AI Goals ---
    @Override
    protected void initGoals() {
        // Let vanilla FoxEntity add its complex goals first (sleeping, fleeing, pouncing, picking up items, eating berries etc.)
        super.initGoals();

        // Remove vanilla targeting goals if replacing with our hunt goal logic
        // This requires knowing the exact goal instances added in super.initGoals(), which is fragile.
        // Alternative: Add our custom HuntGoal with a high priority (low number) to override vanilla hunting.
        // Let's clear vanilla target selectors related to hunting and add our custom one.
        clearVanillaHuntTargets(); // Helper method to remove vanilla hunt targets

        // Add Custom HuntGoal - Priority 3 to potentially override vanilla flee goals if energy is low enough
        this.goalSelector.add(3, new FoxHuntGoal(this, 1.25, 60)); // Speed 1.25, hunts if energy < 60%

        // Add Custom EatBerriesGoal - Ensure it exists
        this.goalSelector.add(10, new EatBerriesGoal(1.20, 12, 1)); // Same priority as vanilla

        // Add Custom PickupItemGoal - Ensure it exists
        this.goalSelector.add(11, new PickupItemGoal()); // Same priority as vanilla

        // Add custom targeting for *our* prey definition for wild foxes (if desired)
        // super.initGoals() adds targets for vanilla Chickens/Rabbits/Fish/Turtles.
        // If PREY_PREDICATE includes custom mobs, add a target selector for them.
        // Example targeting custom chickens/rabbits (if PREY_PREDICATE includes them):
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, LivingEntity.class, 10, false, false,
            entity -> PREY_PREDICATE.test(entity) && !(entity instanceof PlayerEntity)
        ));
    }

    // Helper to remove vanilla hunt target goals if conflicting
    private void clearVanillaHuntTargets() {
        // This is difficult without knowing the exact instances added by super.
        // We might need to iterate and remove goals targeting specific classes.
        // Or, rely on priority and the custom HuntGoal's canStart() conditions.
        // For now, we assume priority/canStart() is sufficient.
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
    }

    // --- Attribute & Bonus Application ---
    // Apply base attributes (from mobAttributes) + bonuses
    private void applyBonuses() {
        if (this.mobAttributes == null) {
            initFromGlobalAttributes(this.getType());
            if (this.mobAttributes == null) return; // Failsafe
        }

        double inherentMaxHp = this.mobAttributes.getMaxHealth();
        double inherentSpeed = this.mobAttributes.getMovementSpeed();
        double inherentAttack = 2.0; // Base vanilla fox attack

        double effectiveMaxHp = Math.max(1.0, inherentMaxHp + this.bonusHealth); // Ensure at least 1 HP
        double speedMultiplier = Math.max(0.1, 1.0 + this.bonusSpeed); // Prevent zero/negative speed
        double effectiveSpeed = inherentSpeed * speedMultiplier;
        double effectiveAttack = inherentAttack + this.bonusAttack;

        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(effectiveMaxHp);
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(effectiveSpeed);
        this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(effectiveAttack);

        // Clamp health
        if(this.getHealth() > this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
    }

    // --- NBT Saving/Loading ---
    // Save only INSTANCE-SPECIFIC custom data + call super
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt); // Saves vanilla Fox data (Type, Flags, Trusted UUIDs, Held Item)

        // Custom instance data
        nbt.putDouble("ELvl", this.ELvl);
        nbt.putDouble("MaxEnergy", this.MaxEnergy);
        nbt.putInt("BreedingCooldown", this.breedingCooldown);
        nbt.putInt("TickAge", this.tickAge);
        nbt.putInt("TicksSinceLastBreeding", this.ticksSinceLastBreeding);
        nbt.putDouble("BonusAttack", this.bonusAttack);
        nbt.putDouble("BonusHealth", this.bonusHealth);
        nbt.putDouble("BonusSpeed", this.bonusSpeed);
        nbt.putInt("KillCount", this.killCount);
        // Base inherited stats (MaxHp, Speed) are stored in mobAttributes, no need to save per entity
    }

    // Load only INSTANCE-SPECIFIC custom data + call super
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        // Ensure mobAttributes is initialized before loading super or applying bonuses
        if (this.mobAttributes == null) {
            initFromGlobalAttributes(this.getType());
        }

        super.readCustomDataFromNbt(nbt); // Loads vanilla Fox data

        // Custom instance data
        this.ELvl = nbt.getDouble("ELvl");
        this.MaxEnergy = nbt.getDouble("MaxEnergy");
        this.breedingCooldown = nbt.getInt("BreedingCooldown");
        this.tickAge = nbt.getInt("TickAge");
        this.ticksSinceLastBreeding = nbt.getInt("TicksSinceLastBreeding");
        this.bonusAttack = nbt.getDouble("BonusAttack");
        this.bonusHealth = nbt.getDouble("BonusHealth");
        this.bonusSpeed = nbt.getDouble("BonusSpeed");
        this.killCount = nbt.getInt("KillCount");

        // Re-apply attributes after loading instance data
        applyBonuses();
        if (!this.getWorld().isClient) updateDescription(this);
    }

    // --- Description Update ---
    // Update description to show relevant stats
    private void updateDescription(CustomFoxEntity ent) {
        if (ent.getWorld().isClient() || !ent.isAlive()) return;

        double currentMaxHp = ent.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
        double currentSpeed = ent.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        double currentAttack = ent.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        long remainingBreedCDTicks = Math.max(0, ent.breedingCooldown - ent.ticksSinceLastBreeding);

        DescriptionRenderer.setDescription(ent, Text.of(
            "HP: " + String.format("%.1f", ent.getHealth()) + "/" + String.format("%.1f", currentMaxHp) +
                " | Atk: " + String.format("%.2f", currentAttack) +
                "\nSpd: " + String.format("%.3f", currentSpeed) + // Show current speed influenced by energy
                " | Energy: " + String.format("%.1f", ent.ELvl) + "/" + String.format("%.1f", ent.MaxEnergy) +
                "\nAge: " + String.format("%.1f", ent.tickAge / 24000.0) + "d" +
                " | Kills: " + ent.killCount +
                "\nBreed CD: " + String.format("%.1f", remainingBreedCDTicks / 20.0) + "s" +
                (ent.isSitting() ? " | Sitting" : "") +
                (ent.isSleeping() ? " | Sleeping" : "")
        ));
    }

    // --- Energy Management ---
    // Update energy level, clamping and updating description
    public void updateEnergyLevel(double newEnergyLevel) {
        double previousELvl = this.ELvl;
        // Ensure MaxEnergy has a non-zero value before clamping
        double currentMaxEnergy = Math.max(1.0, this.MaxEnergy); // Use at least 1.0
        this.ELvl = Math.max(0.0, Math.min(newEnergyLevel, currentMaxEnergy));

        if (!this.getWorld().isClient && Math.abs(this.ELvl - previousELvl) > 0.01) {
            updateDescription(this);
        }
    }

    // Getter used by breeding logic, potentially external systems
    public double getEnergyLevel() {
        return this.ELvl;
    }

    // --- Core Logic Tick ---
    @Override
    public void tick() {
        // Failsafe initialization for mobAttributes
        if (this.mobAttributes == null && !this.getWorld().isClient) {
            initFromGlobalAttributes(this.getType());
            applyBonuses();
        }

        super.tick(); // Handles vanilla fox behavior (animations, held item eating, sleeping logic, vanilla goals)

        // --- Server-Side Custom Logic ---
        if (!this.getWorld().isClient) {

            // Check if hunt target died
            LivingEntity currentTarget = this.getTarget();
            if (currentTarget != null && currentTarget.isDead() && HUNT_TARGET_PREDICATE.test(currentTarget)) {
                onSuccessfulKill(currentTarget);
                // Clear target handled by vanilla logic usually when target dies
            }

            // Dynamic Max Energy based on Age (similar curve to wolf)
            if (tickAge <= 24000) { MaxEnergy = 10.0 + 90.0 * (tickAge / 24000.0); } // Up to 1 day
            else if (tickAge <= 72000) { MaxEnergy = 100.0; } // Day 1-3
            else { MaxEnergy = Math.max(20.0, 100.0 - (tickAge - 72000.0) / 480.0); } // Decline after Day 3
            tickAge++;

            // Grow up check
            if (tickAge >= 24000 && this.isBaby()) {
                this.growUp(this.getBreedingAge() * -1, false); // Vanilla grow up
                updateDescription(this);
            }

            // Clamp energy & Handle passive gain/loss
            if (ELvl > MaxEnergy) updateEnergyLevel(MaxEnergy);
            else if (ELvl < 0) updateEnergyLevel(0);

            if (wasRecentlyHit) {
                updateEnergyLevel(this.ELvl * 0.80); // Lose 20% energy when hit
                wasRecentlyHit = false;
            }

            // Passive energy drain (less if sleeping/sitting)
            double drainMultiplier = (this.isSleeping() || this.isSitting()) ? 0.2 : 1.0;
            // Foxes likely use less passive energy than wolves
            if (Math.random() < 0.3) { // 30% chance
                updateEnergyLevel(this.ELvl - (0.04 + Math.random() * 0.20) * drainMultiplier);
            }

            // Energy gain from environment (eating berries is handled by EatBerriesGoal)
            // Maybe slight gain if sleeping in shade? (Vanilla AvoidDaylightGoal handles finding shade)
            if (this.isSleeping() && !this.getWorld().isSkyVisible(this.getBlockPos())) {
                if(Math.random() < 0.05) { // 5% chance while sleeping in shade
                    updateEnergyLevel(this.ELvl + (0.05 + Math.random() * 0.1));
                }
            }


            // Health Regen at high energy (only if safe and not sleeping/sitting)
            if (!this.isSleeping() && !this.isSitting() && ELvl >= MaxEnergy * 0.9 && this.getHealth() < this.getMaxHealth() && !wasRecentlyHit && this.getAttacker() == null) {
                this.heal(0.15F); // Slower regen
            }

            // Automated Breeding Logic
            if (canAutoBreed()) {
                double searchRadius = 24.0;
                List<CustomFoxEntity> mateCandidates = this.getWorld().getEntitiesByClass(
                    CustomFoxEntity.class, this.getBoundingBox().expand(searchRadius), this::isValidMate);

                CustomFoxEntity nearestMate = findNearestMate(mateCandidates);
                if (nearestMate != null) {
                    moveToMate(nearestMate);
                    if (this.squaredDistanceTo(nearestMate) < 6.0) {
                        initiateBreeding(nearestMate); // Sets love ticks, vanilla AnimalMateGoal takes over
                    }
                }
            }
            // Increment breeding timer if not in love mode
            if (!this.isInLove()) {
                ticksSinceLastBreeding++;
            }

            // Starvation damage
            if (ELvl <= 0.0) {
                this.damage(this.getDamageSources().starve(), 1.0f);
            }

            // Update speed based on energy (only if not sleeping/sitting)
            if (!this.isSleeping() && !this.isSitting()) {
                double currentBaseSpeed = this.Speed * (1.0 + this.bonusSpeed);
                double energyMultiplier = (this.MaxEnergy > 0) ? Math.max(0.3, this.ELvl / this.MaxEnergy) : 1.0; // Min 30% speed
                this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(currentBaseSpeed * energyMultiplier);
            } else {
                this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.0); // Ensure no speed if sitting/sleeping
            }

            // Periodic description update
            if (tickAge % 40 == 0) updateDescription(this);
        }
        // Client-side animations handled by super.tick()
    }

    // --- Combat & Hunting ---
    // Called when hunt goal succeeds
    public void onSuccessfulKill(LivingEntity killedEntity) {
        if (!this.getWorld().isClient) {
            this.killCount++;
            double energyGain = Math.min(25.0, killedEntity.getMaxHealth() * 2.0); // More energy gain for smaller prey
            updateEnergyLevel(this.ELvl + energyGain);
            this.heal(this.getMaxHealth() * 0.20f); // Heal 20%

            // Evolution chance
            float evolutionChance = 0.06f + (float)Math.min(this.killCount, 200) / 3500.0f; // Slightly higher base chance, slower scaling
            if (this.random.nextFloat() < evolutionChance) {
                int statChoice = this.random.nextInt(3);
                switch(statChoice) {
                    case 0: bonusAttack += 0.04 + random.nextDouble() * 0.08; break; // Slightly lower attack gain
                    case 1: bonusHealth += 0.08 + random.nextDouble() * 0.25; break;
                    case 2: bonusSpeed += 0.0015 + random.nextDouble() * 0.0025; break; // Slightly higher speed potential
                }
                // Cap bonuses
                this.bonusAttack = Math.min(this.bonusAttack, 6.0); // Max +6 bonus damage (total 8)
                this.bonusHealth = Math.min(this.bonusHealth, 15.0); // Max +15 bonus health
                this.bonusSpeed = Math.min(this.bonusSpeed, 0.20);   // Max +20% bonus speed

                applyBonuses();
                updateDescription(this);
            }
            // Fox might pick up drops via PickupItemGoal after the kill
        }
    }

    // Damage handling - simplified
    @Override
    protected void applyDamage(DamageSource source, float amount) {
        super.applyDamage(source, amount); // Let vanilla handle armor, etc.
        if (!this.getWorld().isClient && this.isAlive()) {
            this.wasRecentlyHit = true;
            // Anger logic handled by vanilla FoxEntity/AnimalEntity
            updateDescription(this);
        }
    }

    // --- Breeding & Interaction ---
    // Manual interaction override
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        Item item = itemStack.getItem();

        // Sneak + Empty Hand = Show Stats
        if(player.isSneaking() && itemStack.isEmpty()) {
            if (!this.getWorld().isClient) updateDescription(this);
            return ActionResult.SUCCESS;
        }
        if (player.isSneaking()) { // Prevent other sneak interactions for now
            return ActionResult.PASS;
        }

        // Breeding item interaction (Sweet Berries / Glow Berries)
        if (this.isBreedingItem(itemStack)) {
            // Check energy level before allowing breeding feed
            if (this.ELvl < 20.0 && !this.isBaby()) { // Low energy check
                if(!this.getWorld().isClient) player.sendMessage(Text.of("This fox has low energy and cannot breed."), true);
                // Still allow feeding for energy gain? Yes.
                if(this.ELvl < this.MaxEnergy && !this.isBaby()) {
                    if (!player.isCreative()) itemStack.decrement(1);
                    updateEnergyLevel(this.ELvl + 10.0); // Lower energy gain from berries?
                    this.playSound(SoundEvents.ENTITY_FOX_EAT, 1.0f, 1.0f);
                    return ActionResult.SUCCESS;
                }
                return ActionResult.FAIL; // Fail if energy is low and cannot gain more
            }
            // If energy is sufficient, let vanilla handle feeding for healing/breeding
            // Super checks health, sets love ticks, adds trust etc.
            return super.interactMob(player, hand);
        }

        // Empty hand click (not sneaking) - maybe toggle sitting like wolf? Foxes sit on their own though.
        // Let vanilla handle other interactions (leash, etc.)
        return super.interactMob(player, hand);
    }

    // Create Child override
    @Override
    @Nullable
    public CustomFoxEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof CustomFoxEntity)) return null;

        CustomFoxEntity parent1 = this;
        CustomFoxEntity parent2 = (CustomFoxEntity) mate;
        CustomFoxEntity child = ModEntities.CUSTOM_FOX.create(serverWorld); // Use your entity type
        if (child == null) return null;

        // --- Inherit Type (Vanilla Logic) ---
        child.setVariant(this.random.nextBoolean() ? parent1.getVariant() : parent2.getVariant());

        // --- Inherit Base Attributes ---
        if (parent1.mobAttributes == null) parent1.initFromGlobalAttributes(parent1.getType());
        if (parent2.mobAttributes == null) parent2.initFromGlobalAttributes(parent2.getType());
        double p1Speed = parent1.mobAttributes.getMovementSpeed();
        double p2Speed = parent2.mobAttributes.getMovementSpeed();
        double p1MaxHp = parent1.mobAttributes.getMaxHealth();
        double p2MaxHp = parent2.mobAttributes.getMaxHealth();

        double childBaseSpeed = ((p1Speed + p2Speed) / 2.0) * (0.9 + random.nextDouble() * 0.2);
        double childBaseMaxHp = ((p1MaxHp + p2MaxHp) / 2.0) * (0.9 + random.nextDouble() * 0.2);
        double childBaseEnergy = 100.0;

        child.mobAttributes = new MobAttributes(new EnumMap<>(Map.of(
            AttributeKey.MOVEMENT_SPEED, childBaseSpeed, AttributeKey.MAX_HEALTH, childBaseMaxHp, AttributeKey.ENERGY, childBaseEnergy
        )));
        child.MaxHp = childBaseMaxHp;
        child.Speed = childBaseSpeed;
        child.ELvl = 10.0; // Start low
        child.MaxEnergy = 10.0;
        child.tickAge = 0;

        // --- Inherit Bonus Stats & Cooldown ---
        double inheritanceFactor = Math.max(0.1, Math.min(parent1.getEnergyLevel(), parent2.getEnergyLevel()) / 100.0);
        double randomFactor = 0.85 + random.nextDouble() * 0.3;
        // (Copy bonus inheritance logic from Wolf)
        child.bonusAttack = Math.max(0, ((parent1.bonusAttack + parent2.bonusAttack) / 2.0) * inheritanceFactor * randomFactor);
        child.bonusHealth = Math.max(0, ((parent1.bonusHealth + parent2.bonusHealth) / 2.0) * inheritanceFactor * randomFactor);
        child.bonusSpeed = Math.max(0, ((parent1.bonusSpeed + parent2.bonusSpeed) / 2.0) * inheritanceFactor * randomFactor);
        child.killCount = (int)(((parent1.killCount + parent2.killCount) / 2.0) * inheritanceFactor);
        // Cooldown inheritance
        double inverseFactor = (inheritanceFactor > 0.1) ? (1 / inheritanceFactor) : 10.0;
        child.breedingCooldown = Math.max(1200, (int) (((parent1.breedingCooldown + parent2.breedingCooldown) / 2) * inverseFactor * (0.9 + random.nextDouble() * 0.2)));
        child.ticksSinceLastBreeding = 0;

        // --- Trust Logic (Vanilla Logic) ---
        PlayerEntity playerEntity = parent1.getLovingPlayer();
        PlayerEntity playerEntity2 = parent2.getLovingPlayer();
        if (playerEntity != null) child.addTrustedUuid(playerEntity.getUuid());
        if (playerEntity2 != null && playerEntity != playerEntity2) child.addTrustedUuid(playerEntity2.getUuid());

        // Final setup
        child.applyBonuses();
        // Parent energy reduction
        parent1.updateEnergyLevel(parent1.getEnergyLevel() * 0.5);
        parent2.updateEnergyLevel(parent2.getEnergyLevel() * 0.5);
        // Resetting love ticks is handled by AnimalMateGoal

        if (!serverWorld.isClient) {
            updateDescription(child);
            updateDescription(parent1);
            updateDescription(parent2);
        }
        return child;
    }

    void addTrustedUuid(@Nullable UUID uuid) {
        if (((Optional)this.dataTracker.get(OWNER)).isPresent()) {
            this.dataTracker.set(OTHER_TRUSTED, Optional.ofNullable(uuid));
        } else {
            this.dataTracker.set(OWNER, Optional.ofNullable(uuid));
        }
    }

    // --- Helpers for Auto-Breeding ---
    private boolean canAutoBreed() {
        // Foxes aren't tamed, so no owner check needed
        // Check if adult, not sleeping/sitting/pouncing/chasing, high energy, cooldown ready
        return !this.isBaby() && !this.isSleeping() && !this.isSitting()
            && !this.isChasing() // Added check
            && !this.isInSneakingPose() // Added check
            && ELvl >= (MaxEnergy * 0.9) && ticksSinceLastBreeding >= breedingCooldown;
    }
    private boolean isValidMate(CustomFoxEntity candidate) {
        // Check if candidate is also ready for auto-breeding
        return candidate != this && !candidate.isBaby() && !candidate.isSleeping() && !candidate.isSitting()
            && !candidate.isChasing() // Added check
            && !candidate.isInSneakingPose() // Added check
            && candidate.getEnergyLevel() >= (candidate.MaxEnergy * 0.9)
            && !candidate.isInLove() && candidate.ticksSinceLastBreeding >= candidate.breedingCooldown;
    }
    @Nullable
    private CustomFoxEntity findNearestMate(List<CustomFoxEntity> candidates) {
        return candidates.stream()
            .min((fox1, fox2) -> Double.compare(this.squaredDistanceTo(fox1), this.squaredDistanceTo(fox2)))
            .orElse(null);
    }
    private void moveToMate(CustomFoxEntity mate) {
        double currentSpeed = this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        this.getNavigation().startMovingTo(mate, currentSpeed * 1.1); // Move slightly faster
    }
    private void initiateBreeding(CustomFoxEntity mate) {
        this.setLoveTicks(600); // Vanilla love duration
        mate.setLoveTicks(600);
        this.ticksSinceLastBreeding = 0; // Reset timer
        mate.ticksSinceLastBreeding = 0;
    }

    // --- Custom Fox Hunt Goal ---
    static class FoxHuntGoal extends Goal {
        private final CustomFoxEntity fox;
        private final double speed;
        private final int energyThresholdPercent; // Energy threshold as percentage (e.g., 60)
        private LivingEntity targetPrey;
        private int checkTimer;

        public FoxHuntGoal(CustomFoxEntity fox, double speed, int energyThresholdPercent) {
            this.fox = fox;
            this.speed = speed;
            this.energyThresholdPercent = energyThresholdPercent;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            // Can't hunt if sleeping, sitting, chasing, pouncing, has target, or high energy
            if (this.fox.isSleeping() || this.fox.isSitting() || this.fox.isChasing() || this.fox.isInSneakingPose() || this.fox.getTarget() != null) return false;
            if (this.fox.ELvl > (this.fox.MaxEnergy * (this.energyThresholdPercent / 100.0))) return false; // Check energy %

            // Cooldown timer
            if (this.checkTimer > 0) { this.checkTimer--; return false; }
            this.checkTimer = 40 + this.fox.random.nextInt(40); // Check every 2-4 seconds

            // Find prey
            this.targetPrey = findNearbyPrey();
            return this.targetPrey != null;
        }

        @Nullable
        private LivingEntity findNearbyPrey() {
            // Prioritize based on fox type? Red=Land, Snow=Fish? Let's keep it simple for now.
            return this.fox.getWorld().getClosestEntity(
                LivingEntity.class, // Target general LivingEntity
                TargetPredicate.DEFAULT.setPredicate(HUNT_TARGET_PREDICATE.and(e -> e.isAlive())), // Use our filter, ensure alive
                this.fox,
                this.fox.getX(), this.fox.getY(), this.fox.getZ(),
                this.fox.getBoundingBox().expand(16.0, 6.0, 16.0) // Search box
            );
        }


        @Override
        public boolean shouldContinue() {
            // Stop if target invalid, far, sleeping/sitting, or energy restored
            return this.targetPrey != null && this.targetPrey.isAlive()
                && !this.fox.isSleeping() && !this.fox.isSitting()
                && this.fox.squaredDistanceTo(this.targetPrey) < 300.0 // ~17 blocks
                && this.fox.ELvl <= (this.fox.MaxEnergy * (this.energyThresholdPercent / 100.0));
        }

        @Override
        public void start() {
            // Vanilla fox has complex pre-pounce behavior (MoveToHuntGoal sets crouching/rolling head)
            // This simple goal just sets the target and moves.
            this.fox.wakeUp(); // Wake up if sleeping
            this.fox.setSitting(false); // Stand up if sitting
            this.fox.setTarget(this.targetPrey); // Set vanilla target
            this.checkTimer = 0;
        }

        @Override
        public void stop() {
            if (this.fox.getTarget() == this.targetPrey) {
                this.fox.setTarget(null); // Clear target only if it was ours
            }
            this.targetPrey = null;
            this.fox.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (this.targetPrey == null || !this.targetPrey.isAlive()) return;

            this.fox.getLookControl().lookAt(this.targetPrey, 30.0F, 30.0F);

            double distSq = this.fox.squaredDistanceTo(this.targetPrey);
            double attackDistSq = getSquaredAttackDistance(this.targetPrey);

            if (distSq > attackDistSq) {
                // Move if not close enough
                this.fox.getNavigation().startMovingTo(this.targetPrey, this.speed);
            } else {
                // Stop moving and attack
                this.fox.getNavigation().stop();
                this.fox.tryAttack(this.targetPrey); // Vanilla attack method handles cooldown
            }
        }

        protected double getSquaredAttackDistance(LivingEntity target) {
            // Slightly increased reach compared to wolf? Foxes pounce.
            return Math.pow(this.fox.getWidth() * 1.8F + target.getWidth(), 2);
        }
    }

    // --- Re-implementing necessary goals if super doesn't add them ---
    // These might be needed if super.initGoals() doesn't add them, or if we need custom versions.

    // Example: EatBerriesGoal (adapted from vanilla decompiled source)
    public class EatBerriesGoal extends MoveToTargetPosGoal {
        protected int timer;
        public EatBerriesGoal(double speed, int range, int maxYDifference) {
            super(CustomFoxEntity.this, speed, range, maxYDifference);
        }
        public double getDesiredDistanceToTarget() { return 2.0; }
        public boolean shouldResetPath() { return this.tryingTime % 100 == 0; }
        protected boolean isTargetPos(WorldView world, BlockPos pos) {
            BlockState blockState = world.getBlockState(pos);
            return (blockState.isOf(Blocks.SWEET_BERRY_BUSH) && blockState.get(SweetBerryBushBlock.AGE) >= 2) || CaveVines.hasBerries(blockState);
        }
        public void tick() {
            if (this.hasReached()) {
                if (this.timer >= 40) eatBerries(); else ++this.timer;
            } else if (!this.hasReached() && CustomFoxEntity.this.random.nextFloat() < 0.05F) {
                CustomFoxEntity.this.playSound(SoundEvents.ENTITY_FOX_SNIFF, 1.0F, 1.0F);
            }
            super.tick();
        }
        protected void eatBerries() {
            if (!CustomFoxEntity.this.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) return;
            BlockState blockState = CustomFoxEntity.this.getWorld().getBlockState(this.targetPos);
            if (blockState.isOf(Blocks.SWEET_BERRY_BUSH)) pickSweetBerries(blockState);
            else if (CaveVines.hasBerries(blockState)) pickGlowBerries(blockState);
            // Custom: Gain energy from eating berries
            CustomFoxEntity.this.updateEnergyLevel(CustomFoxEntity.this.ELvl + 5.0); // Gain 5 energy
        }
        private void pickGlowBerries(BlockState state) { CaveVines.pickBerries(CustomFoxEntity.this, state, CustomFoxEntity.this.getWorld(), this.targetPos); }
        private void pickSweetBerries(BlockState state) {
            int age = state.get(SweetBerryBushBlock.AGE);
            CustomFoxEntity.this.getWorld().setBlockState(this.targetPos, state.with(SweetBerryBushBlock.AGE, 1), 2);
            int dropCount = 1 + CustomFoxEntity.this.getWorld().random.nextInt(2) + (age == 3 ? 1 : 0);
            ItemStack heldItem = CustomFoxEntity.this.getMainHandStack();
            if (heldItem.isEmpty()) {
                CustomFoxEntity.this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.SWEET_BERRIES)); dropCount--;
            }
            if (dropCount > 0) Block.dropStack(CustomFoxEntity.this.getWorld(), this.targetPos, new ItemStack(Items.SWEET_BERRIES, dropCount));
            CustomFoxEntity.this.playSound(SoundEvents.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 1.0F, 1.0F);
        }
        public boolean canStart() { return !CustomFoxEntity.this.isSleeping() && super.canStart(); }
        public void start() { this.timer = 0; CustomFoxEntity.this.setSitting(false); super.start(); }
    }

    // Example: PickupItemGoal (adapted from vanilla decompiled source)
    public class PickupItemGoal extends Goal {
        public PickupItemGoal() { this.setControls(EnumSet.of(Control.MOVE)); }
        public boolean canStart() {
            if (!CustomFoxEntity.this.getMainHandStack().isEmpty()) return false;
            if (CustomFoxEntity.this.getTarget() != null || CustomFoxEntity.this.getAttacker() != null) return false;
            if (!CustomFoxEntity.this.canPickUpLoot()) return false; // Check vanilla flags (not sleeping/sitting etc)
            if (CustomFoxEntity.this.getRandom().nextInt(toGoalTicks(10)) != 0) return false;
            List<ItemEntity> list = CustomFoxEntity.this.getWorld().getEntitiesByClass(ItemEntity.class, CustomFoxEntity.this.getBoundingBox().expand(8.0), PICKABLE_DROP_FILTER);
            return !list.isEmpty() && CustomFoxEntity.this.getMainHandStack().isEmpty();
        }
        public void tick() {
            List<ItemEntity> list = CustomFoxEntity.this.getWorld().getEntitiesByClass(ItemEntity.class, CustomFoxEntity.this.getBoundingBox().expand(8.0), PICKABLE_DROP_FILTER);
            ItemStack itemStack = CustomFoxEntity.this.getMainHandStack();
            if (itemStack.isEmpty() && !list.isEmpty()) {
                CustomFoxEntity.this.getNavigation().startMovingTo(list.get(0), 1.2);
            }
        }
        public void start() {
            List<ItemEntity> list = CustomFoxEntity.this.getWorld().getEntitiesByClass(ItemEntity.class, CustomFoxEntity.this.getBoundingBox().expand(8.0), PICKABLE_DROP_FILTER);
            if (!list.isEmpty()) {
                CustomFoxEntity.this.getNavigation().startMovingTo(list.get(0), 1.2);
            }
        }
    }
}