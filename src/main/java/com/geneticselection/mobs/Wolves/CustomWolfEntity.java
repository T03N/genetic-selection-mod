package com.geneticselection.mobs.Wolves;

import com.geneticselection.attributes.AttributeKey;
import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.Cows.CustomCowEntity;
import com.geneticselection.mobs.ModEntities; // Assuming your custom entities are registered here
import com.geneticselection.utils.DescriptionRenderer;
import io.netty.buffer.Unpooled;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.*; // Import AI goals
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker; // Required for tracked data
import net.minecraft.entity.data.TrackedData; // Required for tracked data
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.passive.*; // Import passive entities like SheepEntity, RabbitEntity, etc.
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeItem; // Required for collar color
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound; // Required for saving/loading data
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor; // Required for collar color
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate; // For targeting predicates
import static com.geneticselection.genetics.ChildInheritance.*;

public class CustomWolfEntity extends WolfEntity
{
    private MobAttributes mobAttributes;
    private double MaxHp; // Base MaxHp from mobAttributes
    private double Speed; // Base Speed from mobAttributes
    private double ELvl; // Current Energy Level (Instance Specific)
    private double MaxEnergy; // Max Energy Capacity (Instance Specific, dynamic)
    private int breedingCooldown; // (Instance Specific)
    private int tickAge = 0; // (Instance Specific)
    private int ticksSinceLastBreeding = 0; // (Instance Specific)

    // Evolution / Hunting Bonus Attributes (Instance Specific)
    private double bonusAttack = 0.0;
    private double bonusHealth = 0.0;
    private double bonusSpeed = 0.0; // Multiplier bonus
    private int killCount = 0;

    // State variable (Instance Specific)
    private boolean wasRecentlyHit = false;
    private static TrackedData<Integer> COLLAR_COLOR;

    // Target Predicates (similar to vanilla) - Adjusted to use ModEntities
    public static final Predicate<LivingEntity> PREY_PREDICATE = (entity) -> {
        EntityType<?> entityType = entity.getType();
        // Target your custom entities
        return entityType == ModEntities.CUSTOM_SHEEP
            || entityType == ModEntities.CUSTOM_RABBIT
            || entityType == ModEntities.CUSTOM_FOX
            || entityType == ModEntities.CUSTOM_CHICKEN
            || entityType == ModEntities.CUSTOM_PIG
            || entityType == ModEntities.CUSTOM_COW
            || entityType == ModEntities.CUSTOM_CAMEL
            || entityType == ModEntities.CUSTOM_DONKEY
            || entityType == ModEntities.CUSTOM_MOOSHROOM
            || entityType == ModEntities.CUSTOM_OCELOT; // Ocelots might be too fast?
        // Add vanilla turtles if desired: || entityType == EntityType.TURTLE;
    };
    // Predicate for hunting goal to prevent attacking own species/tamed animals/owner
    private static final Predicate<LivingEntity> HUNT_TARGET_PREDICATE = (entity) -> {
        // Must be valid prey according to PREY_PREDICATE
        if (!PREY_PREDICATE.test(entity)) {
            return false;
        }
        // Cannot be tamed
        if (entity instanceof TameableEntity && ((TameableEntity) entity).isTamed()) {
            return false;
        }
        // Cannot be another wolf (custom or vanilla)
        if (entity instanceof WolfEntity) {
            return false;
        }
        // Potentially add check for owner if needed (don't hunt owner's other pets even if untamed?)
        return true; // Is valid prey
    };


    public CustomWolfEntity(EntityType<? extends WolfEntity> entityType, World world) {
        super(entityType, world);

        // Initialize mob attributes (directly within the class)
        if (this.mobAttributes == null) {
            MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
            double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
            double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
            double energy = global.getEnergyLvl() * (0.9 + Math.random() * 0.1);
            this.mobAttributes = new MobAttributes(speed, health, energy, Optional.empty(), Optional.empty(),Optional.empty(),Optional.empty(), Optional.empty());
            this.tickAge = 0;
        }

        // Apply attributes to the entity
        this.MaxHp = this.mobAttributes.getMaxHealth();
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
        this.Speed = this.mobAttributes.getMovementSpeed();
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);
        this.ELvl = this.mobAttributes.getEnergyLvl();

        this.breedingCooldown = 3000 + (int)((1 - (ELvl / 100.0)) * 2000) + random.nextInt(2001);

        // Initialize instance-specific stats that aren't saved/loaded via mobAttributes
        this.MaxEnergy = 40.0; // Start low for babies/initial spawn
        this.breedingCooldown = 5000 + (int)((1 - (ELvl / 100.0)) * 2000) + random.nextInt(2001);
        this.ticksSinceLastBreeding = 0;
        this.tickAge = 0;
        this.bonusAttack = 0.0;
        this.bonusHealth = 0.0;
        this.bonusSpeed = 0.0;
        this.killCount = 0;

        // Apply initial attributes (applies base + bonuses + taming)
        this.applyBonuses();

        // Update description on server side
        if (!this.getWorld().isClient) {
            updateDescription(this);
        }
    }

    // Helper to initialize from global/default attributes
    private void initFromGlobalAttributes(EntityType<?> entityType) {
        this.mobAttributes = GlobalAttributesManager.getAttributes(entityType);
        // Set the base MaxHp/Speed fields from these attributes
        this.MaxHp = this.mobAttributes.getMaxHealth();
        this.Speed = this.mobAttributes.getMovementSpeed();
        this.MaxEnergy = 100.0;
        this.ELvl = this.mobAttributes.getEnergyLvl();
    }


    // Register tracked data - ONLY add custom ones if needed. Vanilla WolfEntity handles the rest.
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
    }

    // Define base attributes - inherited createWolfAttributes likely sufficient unless changing base values
    // public static DefaultAttributeContainer.Builder createWolfAttributes() { ... }


    // Initialize AI Goals - Call super first, then add/modify
    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new SitGoal(this));
        this.goalSelector.add(3, new PounceAtTargetGoal(this, 0.4F));
        this.goalSelector.add(4, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.add(5, new FollowOwnerGoal(this, 1.0, 10.0F, 2.0F));
        this.goalSelector.add(6, new HuntGoal(this, 2.5, 80));
        this.goalSelector.add(7, new AnimalMateGoal(this, 1.0));
        this.goalSelector.add(8, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(9, new WolfBegGoal(this, 8.0F));
        this.goalSelector.add(10, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(10, new LookAroundGoal(this));
        this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new AttackWithOwnerGoal(this));
        this.targetSelector.add(3, (new RevengeGoal(this, new Class[0])).setGroupRevenge(new Class[0]));
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::shouldAngerAt));
        this.targetSelector.add(5, new UntamedActiveTargetGoal<>(this, AnimalEntity.class, false, FOLLOW_TAMED_PREDICATE));
        this.targetSelector.add(6, new UntamedActiveTargetGoal<>(this, TurtleEntity.class, false, TurtleEntity.BABY_TURTLE_ON_LAND_FILTER));
        this.targetSelector.add(7, new ActiveTargetGoal<>(this, AbstractSkeletonEntity.class, false));
        this.targetSelector.add(8, new UniversalAngerGoal<>(this, true));
    }

    // Apply inherent base attributes (from mobAttributes) + bonuses + taming modifications
    private void applyBonuses() {
        // Ensure mobAttributes is initialized
        if (this.mobAttributes == null) {
            initFromGlobalAttributes(this.getType());
            if (this.mobAttributes == null) { // Still null? Error condition
                System.err.println("ERROR: MobAttributes still null after initialization attempt for " + this.getUuidAsString());
                return;
            }
        }

        double inherentMaxHp = this.mobAttributes.getMaxHealth();
        double inherentSpeed = this.mobAttributes.getMovementSpeed();
        double inherentAttack = 4.0; // Base vanilla attack damage

        double effectiveMaxHp = inherentMaxHp + this.bonusHealth;
        double speedMultiplier = Math.max(0.0, 1.0 + this.bonusSpeed); // Prevent negative multiplier
        double effectiveSpeed = inherentSpeed * (1.0 + this.bonusSpeed);
        double effectiveAttack = inherentAttack + this.bonusAttack;

        // Apply vanilla taming boost logic *after* calculating effective base from inheritance+bonus
        // Tamed wolves get a base of 40 + bonus health. Wild wolves get inherent + bonus.
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.isTamed() ? 40.0 + this.bonusHealth : effectiveMaxHp);
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(effectiveSpeed);
        this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(effectiveAttack);

        // Clamp current health to new max health
        if(this.getHealth() > this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
    }


    // Called by HuntGoal upon successful kill
    public void onSuccessfulKill(LivingEntity killedEntity) {
        if (!this.getWorld().isClient) {
            this.killCount++;

            double energyGain = Math.min(8.0, killedEntity.getMaxHealth() * 1.5);
            updateEnergyLevel(this.ELvl + energyGain);
            this.heal(this.getMaxHealth() * 0.15f); // Heal 15%

            // Evolution chance
            float evolutionChance = 0.05f + (float)Math.min(this.killCount, 150) / 3000.0f;
            if (this.random.nextFloat() < evolutionChance) {
                int statChoice = this.random.nextInt(3);
                String statIncreased = "";
                switch(statChoice) {
                    case 0: bonusAttack += 0.05 + random.nextDouble() * 0.1; statIncreased = "Atk"; break;
                    case 1: bonusHealth += 0.1 + random.nextDouble() * 0.3; statIncreased = "Hp"; break;
                    case 2: bonusSpeed += 0.001 + random.nextDouble() * 0.002; statIncreased = "Spd"; break;
                }
                // Cap bonuses
                this.bonusAttack = Math.min(this.bonusAttack, 8.0);
                this.bonusHealth = Math.min(this.bonusHealth, 20.0);
                this.bonusSpeed = Math.min(this.bonusSpeed, 0.15);

                applyBonuses(); // Re-apply attributes
                updateDescription(this); // Update display
            }
        }
    }

    // --- NBT Saving/Loading ---
    // Save only INSTANCE-SPECIFIC custom data + call super
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        // Custom instance attributes
        nbt.putDouble("ELvl", this.ELvl);
        nbt.putDouble("MaxEnergy", this.MaxEnergy);
        nbt.putInt("BreedingCooldown", this.breedingCooldown);
        nbt.putInt("TickAge", this.tickAge);
        nbt.putInt("TicksSinceLastBreeding", this.ticksSinceLastBreeding);
        // Evolution attributes
        nbt.putDouble("BonusAttack", this.bonusAttack);
        nbt.putDouble("BonusHealth", this.bonusHealth);
        nbt.putDouble("BonusSpeed", this.bonusSpeed);
        nbt.putInt("KillCount", this.killCount);
        // Do NOT save inherited base attributes (MaxHp, Speed) - they come from mobAttributes/GlobalManager
    }

    // Load only INSTANCE-SPECIFIC custom data + call super
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        // Initialize mobAttributes FIRST, either from Global or keep existing if already loaded
        if (this.mobAttributes == null) {
            initFromGlobalAttributes(this.getType());
        }

        super.readCustomDataFromNbt(nbt); // Load vanilla WolfEntity data

        // Custom instance attributes
        if (nbt.contains("ELvl")) {
            this.ELvl = nbt.getDouble("ELvl");
        } else
        {
            this.ELvl = this.mobAttributes.getEnergyLvl();
        }
        this.MaxEnergy = nbt.getDouble("MaxEnergy");
        this.breedingCooldown = nbt.getInt("BreedingCooldown");
        this.tickAge = nbt.getInt("TickAge");
        this.ticksSinceLastBreeding = nbt.getInt("TicksSinceLastBreeding");
        // Evolution attributes
        this.bonusAttack = nbt.getDouble("BonusAttack");
        this.bonusHealth = nbt.getDouble("BonusHealth");
        this.bonusSpeed = nbt.getDouble("BonusSpeed");
        this.killCount = nbt.getInt("KillCount");
        // Do NOT load inherited base attributes or reconstruct mobAttributes here

        // Re-apply attributes after loading instance data and vanilla data (which sets isTamed)
        applyBonuses();
        if (!this.getWorld().isClient)
            updateDescription(this);
    }

    // Update description to show relevant stats
    private void updateDescription(CustomWolfEntity ent) {
        if (ent.getWorld().isClient() || !ent.isAlive()) return; // Only update server-side for living entities

        double currentMaxHp = ent.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
        double currentSpeed = ent.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        double currentAttack = ent.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        long remainingBreedCDTicks = Math.max(0, ent.breedingCooldown - ent.ticksSinceLastBreeding);

        DescriptionRenderer.setDescription(ent, Text.of(
            "HP: " + String.format("%.1f", ent.getHealth()) + "/" + String.format("%.1f", currentMaxHp) +
                " | Atk: " + String.format("%.2f", currentAttack) +
                "\nSpd: " + String.format("%.3f", currentSpeed) +
                " | Energy: " + String.format("%.1f", ent.ELvl) + "/" + String.format("%.1f", ent.MaxEnergy) +
                "\nAge: " + ent.tickAge +
                " | Kills: " + ent.killCount +
                "\nBreed CD: " + String.format("%.1f", remainingBreedCDTicks / 20.0) + "s" +
                (ent.isTamed() ? " | Owner: " + (ent.getOwner() != null ? ent.getOwner().getName().getString() : "?") : " | (Wild)") +
                (ent.hasAngerTime() ? " | !!ANGRY!!" : "") // Use hasAngerTime()
        ));
    }

    public void updateEnergyLevel(double newEnergyLevel) {
        this.ELvl = newEnergyLevel;

        // If the energy level changes, notify the renderer to update.
        if (this.getWorld().isClient) {
            // In case this is client-side, trigger a re-render.
            this.markForRenderUpdate();
        }

        // Sync energy level with server if needed
        if (!this.getWorld().isClient) {
            this.syncEnergyLevelToClient();
        }
    }

    private void syncEnergyLevelToClient() {
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
        data.writeInt(this.getId());  // Send entity ID
        data.writeDouble(this.ELvl);  // Send the updated energy level
    }

    public void markForRenderUpdate() {
        // This triggers the renderer to update the texture the next time it's rendered
        MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(this).render(this, 0, 0, new MatrixStack(), MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers(), 0);
    }

    // Getter for external use if needed (e.g., by AttributeCarrier interface)
    public double getEnergyLevel() {
        return this.ELvl;
    }

    // Handle interactions: Feeding, Sitting, Dyeing Collar
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        Item item = itemStack.getItem();

        // Allow stat check with sneak + empty hand
        if(player.isSneaking() && itemStack.isEmpty() && this.isTamed()) {
            if (!this.getWorld().isClient) updateDescription(this);
            return ActionResult.SUCCESS;
        }
        // Prevent other interactions while sneaking
        if (player.isSneaking()) {
            return ActionResult.PASS;
        }

        // Taming attempt (copied from previous version, seems fine)
        if (!this.isTamed() && itemStack.isOf(Items.BONE) && !this.hasAngerTime()) {
            if (!player.isCreative()) itemStack.decrement(1);
            if (this.random.nextInt(3) == 0) {
                this.setOwner(player); // This calls setTamed(true, true) internally
                this.navigation.stop();
                this.setTarget(null);
                this.getWorld().sendEntityStatus(this, (byte)7);
            } else {
                this.getWorld().sendEntityStatus(this, (byte)6);
            }
            return ActionResult.SUCCESS;
        }

        // Interactions for Tamed wolves
        if (this.isTamed() && this.isOwner(player)) { // Ensure player is owner for most actions
            // Healing with food (if health < max)
            if (this.isBreedingItem(itemStack) && this.getHealth() < this.getMaxHealth()) {
                if (!player.isCreative()) itemStack.decrement(1);
                net.minecraft.component.type.FoodComponent foodComponent = itemStack.get(net.minecraft.component.DataComponentTypes.FOOD);
                float healAmount = foodComponent != null ? (float) foodComponent.nutrition() * 2.0f : 2.0f;
                this.heal(healAmount);
                this.playSound(SoundEvents.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
                return ActionResult.SUCCESS;
            }

            // Manual breeding trigger (requires food, full health, adult, not already in love)
            if (this.isBreedingItem(itemStack) && this.getHealth() >= this.getMaxHealth() && !this.isBaby() && !this.isInLove()) {
                if (!player.isCreative()) itemStack.decrement(1);
                this.lovePlayer(player); // Sets love ticks
                return ActionResult.SUCCESS;
            }

            // Feed for energy gain (if not breeding and energy < max)
            if (this.isBreedingItem(itemStack) && this.ELvl < this.MaxEnergy && !this.isBaby() && this.getHealth() >= this.getMaxHealth()) {
                if (!player.isCreative()) itemStack.decrement(1);
                updateEnergyLevel(this.ELvl + 15.0); // Gain flat 15 energy
                this.playSound(SoundEvents.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
                return ActionResult.SUCCESS;
            }

            // Sitting toggle (if not holding food/dye) - Let super handle leash first
            ActionResult actionResult = super.interactMob(player, hand);
            if (!actionResult.isAccepted() && !(item instanceof DyeItem) && !this.isBreedingItem(itemStack)) {
                this.setSitting(!this.isSitting());
                this.jumping = false;
                this.navigation.stop();
                this.setTarget(null);
                return ActionResult.SUCCESS_NO_ITEM_USED;
            }
            return actionResult; // Return result from super (e.g., leash)
        }

        // Fallback to superclass interaction if not tamed/not owner/etc.
        return super.interactMob(player, hand);
    }

    // updateAttributesForTamed is handled by setTamed calling applyBonuses

    // ApplyDamage override - simplified, only sets hit flag and handles anger
    @Override
    protected void applyDamage(DamageSource source, float amount) {
        // Let super handle damage reduction, events, etc.
        super.applyDamage(source, amount);
        if (!this.getWorld().isClient && this.isAlive()) { // Check if still alive after damage
            this.wasRecentlyHit = true;
            // Anger logic remains handled by vanilla WolfEntity/Angerable logic called in super.damage()
            updateDescription(this); // Update display
        }
    }

    // Tick method for ongoing logic
    @Override
    public void tick() {
        // mobAttributes should be initialized in constructor or readNbt
        if (this.mobAttributes == null && !this.getWorld().isClient) {
            initFromGlobalAttributes(this.getType()); // Failsafe initialization
            applyBonuses();
        }

        super.tick(); // Handles vanilla tick logic (anger, sitting, movement, goals, shake, beg anim etc.)

        // Server-side custom logic
        if (!this.getWorld().isClient) {

            // Check kill target status
            LivingEntity currentTarget = this.getTarget();
            if (currentTarget != null && currentTarget.isDead()) {
                if (HUNT_TARGET_PREDICATE.test(currentTarget)) {
                    onSuccessfulKill(currentTarget);
                }
                this.setTarget(null);
            }

            // Dynamic Max Energy
            if (tickAge <= 4404){
                MaxEnergy = 10 * Math.log(5 * tickAge + 5);
            } else if (tickAge < 16000) {
                MaxEnergy = 100;
            } else {
                MaxEnergy = -(tickAge - 16000) / 16.0 + 100;
            }
            tickAge++;

            // Grow up check
            if (tickAge >= 4404 && this.isBaby()) {
                this.growUp(this.getBreedingAge() * -1, false);
                updateDescription(this);
            }

            // Clamp current energy & handle loss/gain
            if (ELvl > MaxEnergy)
                updateEnergyLevel(MaxEnergy);
            else if (ELvl < 0)
                updateEnergyLevel(0);

            if (wasRecentlyHit) {
                updateEnergyLevel(this.ELvl * 0.85); // Lose 15%
                wasRecentlyHit = false;
            }

            // Passive energy drain
            double drainMultiplier = (this.isSitting() || !this.isOnGround()) ? 0.3 : 2.5; // Less drain when sitting/mid-air
            if (Math.random() < 0.1) { // 10% chance
                updateEnergyLevel(this.ELvl - (0.05) * drainMultiplier);
            }

            // Health Regen (requires high energy, tamed, not sitting/angry/hit)
            if (this.isTamed() && !this.isSitting() && ELvl >= MaxEnergy * 0.95 && this.getHealth() < this.getMaxHealth() && !this.hasAngerTime() && !wasRecentlyHit) {
                this.heal(0.20F); // Regen slightly slower
            }

            // Automated Breeding Logic (Most conditions checked within the check)
            if (ELvl >= 70.0 && !isBaby() && ticksSinceLastBreeding >= breedingCooldown) {
                double searchRadius = 64.0;

                List<CustomWolfEntity> mateCandidates = this.getWorld().getEntitiesByClass(
                    CustomWolfEntity.class,
                    this.getBoundingBox().expand(searchRadius),
                    candidate -> candidate != this && candidate.getEnergyLevel() >= 90.0 && !candidate.isBaby()
                );

                // Find the nearest candidate
                CustomWolfEntity nearestMate = null;
                double minDistanceSquared = Double.MAX_VALUE;
                for (CustomWolfEntity candidate : mateCandidates) {
                    double distSq = this.squaredDistanceTo(candidate);
                    if (distSq < minDistanceSquared) {
                        minDistanceSquared = distSq;
                        nearestMate = candidate;
                    }
                }

                // If we found a mate candidate, move towards it
                if (nearestMate != null) {
                    // Start moving towards the nearest cow; adjust speed as needed
                    this.getNavigation().startMovingTo(nearestMate, this.Speed * 5.0F * (this.ELvl / MaxEnergy));

                    // If close enough (e.g., within 2 blocks; adjust the threshold as needed)
                    if (minDistanceSquared < 4.0) {
                        // Only start breeding if both cows are not already in love
                        if (!this.isInLove() && !nearestMate.isInLove()) {
                            this.setLoveTicks(500);
                            nearestMate.setLoveTicks(500);
                            ticksSinceLastBreeding = 0;
                        }
                    }
                }
            }
            // Increment breeding timer if not in love
            if (!this.isInLove()) {
                ticksSinceLastBreeding++;
            }

            // Starvation damage
            if (ELvl <= 0.0) {
                this.damage(this.getDamageSources().starve(), 1.0f);
            }

            // Update speed based on energy (if not sitting/angry) - applied in applyBonuses? Re-apply here too?
            // applyBonuses handles base speed + bonus. We need to factor in energy here.
            if (!this.isSitting()) {
                double currentBaseSpeed = this.Speed * (1.0 + this.bonusSpeed);
                double energyMultiplier = (this.MaxEnergy > 0) ? (this.ELvl / this.MaxEnergy) : 1.0;
                double calculatedSpeed = currentBaseSpeed * energyMultiplier;
                // Ensure the speed is never below the vanilla wolf speed stored in this.Speed
                double newSpeed = Math.max(calculatedSpeed, this.Speed);
                Objects.requireNonNull(
					this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)).setBaseValue(newSpeed);
            } else {
                Objects.requireNonNull(
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.0);
            }

            // Periodic description update
            if (tickAge % 40 == 0)
                updateDescription(this);
        }
        // Client-side shake animation update is handled by super.tick()
    }

    // Create Child - inheriting attributes and bonuses
    @Override
    @Nullable
    public CustomWolfEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof CustomWolfEntity)) return null;

        CustomWolfEntity parent1 = this;
        CustomWolfEntity parent2 = (CustomWolfEntity) mate;
        CustomWolfEntity child = ModEntities.CUSTOM_WOLF.create(serverWorld);
        if (child == null) return null;

        // --- Inherit Base Attributes (from parents' mobAttributes) ---
        // Ensure parents have mobAttributes initialized
        if (parent1.mobAttributes == null) parent1.initFromGlobalAttributes(parent1.getType());
        if (parent2.mobAttributes == null) parent2.initFromGlobalAttributes(parent2.getType());

        double p1Speed = parent1.mobAttributes.getMovementSpeed();
        double p2Speed = parent2.mobAttributes.getMovementSpeed();
        double p1MaxHp = parent1.mobAttributes.getMaxHealth();
        double p2MaxHp = parent2.mobAttributes.getMaxHealth();

        double childBaseSpeed = ((p1Speed + p2Speed) / 2.0) * (0.9 + random.nextDouble() * 0.2);
        double childBaseMaxHp = ((p1MaxHp + p2MaxHp) / 2.0) * (0.9 + random.nextDouble() * 0.2);
        double childBaseEnergy = 100.0; // Child starts with default base energy level

        // Create MobAttributes for the child
        child.mobAttributes = new MobAttributes(new EnumMap<>(Map.of(
            AttributeKey.MOVEMENT_SPEED, childBaseSpeed,
            AttributeKey.MAX_HEALTH, childBaseMaxHp,
            AttributeKey.ENERGY, childBaseEnergy // Store base energy default here
            // Add other relevant AttributeKeys if needed, ensure no drops for wolf
        )));
        child.MaxHp = childBaseMaxHp; // Update field
        child.Speed = childBaseSpeed; // Update field
        child.ELvl = 10.0; // Actual starting energy level is low
        child.MaxEnergy = 10.0; // Baby starts with low max energy capacity
        child.tickAge = 0;

        // --- Inherit Bonus Stats & Cooldown ---
        double inheritanceFactor = Math.max(0.1, Math.min(parent1.getEnergyLevel(), parent2.getEnergyLevel()) / 100.0);
        double randomFactor = 0.85 + random.nextDouble() * 0.3;

        double avgBonusAttack = (parent1.bonusAttack + parent2.bonusAttack) / 2.0;
        double avgBonusHealth = (parent1.bonusHealth + parent2.bonusHealth) / 2.0;
        double avgBonusSpeed = (parent1.bonusSpeed + parent2.bonusSpeed) / 2.0;
        int avgKillCount = (parent1.killCount + parent2.killCount) / 2;

        child.bonusAttack = Math.max(0, avgBonusAttack * inheritanceFactor * randomFactor);
        child.bonusHealth = Math.max(0, avgBonusHealth * inheritanceFactor * randomFactor);
        child.bonusSpeed = Math.max(0, avgBonusSpeed * inheritanceFactor * randomFactor);
        child.killCount = (int)(avgKillCount * inheritanceFactor);

        // Calculate child breeding cooldown
        double inverseFactor = (inheritanceFactor > 0.1) ? (1 / inheritanceFactor) : 10.0;
        int childBreedingCooldown = (int) ((
			(double) (parent1.breedingCooldown + parent2.breedingCooldown) / 2) * inverseFactor * (0.9 + random.nextDouble() * 0.2));
        child.breedingCooldown = Math.max(1200, childBreedingCooldown); // Min 1 min cooldown
        child.ticksSinceLastBreeding = 0;


        // --- Handle Taming & Collar ---
        // Child is born wild unless *both* parents are tamed and have same owner
        if (parent1.isTamed() && parent2.isTamed() && parent1.getOwnerUuid() != null && parent1.getOwnerUuid().equals(parent2.getOwnerUuid())) {
            child.setOwnerUuid(parent1.getOwnerUuid());
            child.setTamed(true, true); // Born tamed
        } else {
            child.setTamed(false, false); // Born wild
        }

        // Apply final attributes
        child.applyBonuses();

        // Reduce parent energy
        parent1.updateEnergyLevel(parent1.getEnergyLevel() * 0.5);
        parent2.updateEnergyLevel(parent2.getEnergyLevel() * 0.5);

        parent1.ELvl -= parent1.ELvl * 0.4F;
        parent2.ELvl -= parent2.ELvl * 0.4F;
        this.resetLoveTicks();

        if (!serverWorld.isClient) {
            updateDescription(child);
            updateDescription(parent1);
            updateDescription(parent2);
        }

        return child;
    }

    @Override
    public boolean canBreedWith(AnimalEntity other) {
        if (other == this) {
            return false;
        } else if (!(other instanceof WolfEntity)) {
            return false;
        } else {
            WolfEntity wolfEntity = (WolfEntity)other;
            if (wolfEntity.isInSittingPose()) {
                return false;
            } else {
                return this.isInLove() && wolfEntity.isInLove();
            }
        }
    }

    // --- Custom Hunt Goal ---
    static class HuntGoal extends Goal {
        private final CustomWolfEntity wolf;
        private final double speed;
        private final int energyThreshold;
        private LivingEntity targetPrey;
        private int checkTimer;
        double randomFactor = 0.9F;

        public HuntGoal(CustomWolfEntity wolf, double speed, int energyThreshold) {
            this.wolf = wolf;
            this.speed = speed;
            this.energyThreshold = energyThreshold;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            // Wolf should not start hunting if sitting, angry, or already targeting someone
            if (this.wolf.isSitting() || this.wolf.hasAngerTime() || this.wolf.ELvl >= this.wolf.MaxEnergy * randomFactor || this.wolf.getTarget() != null) {
                return false;
            }
            if (this.checkTimer > 0) {
                this.checkTimer--;
                return false;
            }
            this.checkTimer = 50 + this.wolf.random.nextInt(70);

            // Target custom entities using the predicate
            this.targetPrey = this.wolf.getWorld().getClosestEntity(
                LivingEntity.class,
                TargetPredicate.DEFAULT.setPredicate(HUNT_TARGET_PREDICATE),
                this.wolf,
                this.wolf.getX(), this.wolf.getY(), this.wolf.getZ(),
                this.wolf.getBoundingBox().expand(16.0, 8.0, 16.0)
            );

            randomFactor = 0.7 + (Math.random() * (0.92 - 0.7));

            return this.targetPrey != null;
        }

        @Override
        public boolean shouldContinue() {
            return this.targetPrey != null && this.targetPrey.isAlive()
                && !this.wolf.isSitting() && !this.wolf.hasAngerTime()
                && this.wolf.squaredDistanceTo(this.targetPrey) < 256.0 // 16*16
                && this.wolf.ELvl <= this.energyThreshold;
        }

        @Override
        public void start() {
            this.wolf.setTarget(this.targetPrey);
            this.wolf.setSitting(false);
            this.checkTimer = 0;
        }

        @Override
        public void stop() {
            if (this.wolf.getTarget() == this.targetPrey) {
                this.wolf.setTarget(null);
            }
            this.targetPrey = null;
            this.wolf.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (this.targetPrey == null || !this.targetPrey.isAlive() && !this.shouldContinue()) {
                return;
            }
            this.wolf.getLookControl().lookAt(this.targetPrey, 10.0F, (float)this.wolf.getMaxLookPitchChange());

            // Apply a boost factor to the speed for hunting.

			// Only move if not within attack distance.
            if (this.wolf.squaredDistanceTo(this.targetPrey) > this.getSquaredAttackDistance(this.targetPrey)) {
                this.wolf.getNavigation().startMovingTo(this.targetPrey, this.speed);
            } else {
                this.wolf.getNavigation().stop(); // Stop moving when close.
            }

            // Attempt attack when within range.
            if (this.wolf.squaredDistanceTo(this.targetPrey) < this.getSquaredAttackDistance(this.targetPrey)) {
                this.wolf.tryAttack(this.targetPrey);
            }
        }

        // Helper for attack distance check
        protected double getSquaredAttackDistance(LivingEntity target) {
            // Adjust distance based on entity widths for better reach calculation
            return Math.pow(this.wolf.getWidth() * 1.5 + target.getWidth(), 2);
        }
    }
}