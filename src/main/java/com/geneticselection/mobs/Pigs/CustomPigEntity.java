package com.geneticselection.mobs.Pigs;

import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.utils.DescriptionRenderer;
import io.netty.buffer.Unpooled;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;

import java.util.List;
import java.util.Optional;

public class CustomPigEntity extends PigEntity {
    private MobAttributes mobAttributes;
    private double MaxHp;
    private double ELvl;
    private double MaxEnergy;
    private double Speed;
    private double MinMeat;
    private double MaxMeat;
    private int breedingCooldown;

    private boolean wasRecentlyHit = false;
    private static final int LIFESPAN = 35000;
    private static final int MAX_AGE = 45000; // After this age, pigs will die naturally
    private int tickAge = 0;
    private int ticksSinceLastBreeding = 0;

    private int panicTicks = 0;
    private static final int PANIC_DURATION = 100; // 5 seconds at 20 ticks per second
    private static final double PANIC_SPEED_MULTIPLIER = 2.0;

    public CustomPigEntity(EntityType<? extends PigEntity> entityType, World world) {
        super(entityType, world);

        if (this.mobAttributes == null) {
            MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
            double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
            double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
            double energy = global.getEnergyLvl() * (0.9 + Math.random() * 0.1);
            double meat = global.getMaxMeat().orElse(0.0) + (0.98 + Math.random() * 0.1);
            this.mobAttributes = new MobAttributes(speed, health, energy, Optional.of(meat), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        }

        this.MaxHp = this.mobAttributes.getMaxHealth();
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
        this.Speed = this.mobAttributes.getMovementSpeed();
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);
        this.ELvl = this.mobAttributes.getEnergyLvl();

        this.mobAttributes.getMaxMeat().ifPresent(maxMeat -> {
            this.MaxMeat = maxMeat;
        });
        this.breedingCooldown = 3000 + (int)((1 - (ELvl / 100.0)) * 2000) + random.nextInt(2001);
        this.setMinMeat(1.0);

        if (!this.getWorld().isClient) {
            updateDescription(this);
        }
    }

    public void updateEnergyLevel(double newEnergyLevel) {
        this.ELvl = newEnergyLevel;

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

    private void updateDescription(CustomPigEntity ent) {
        DescriptionRenderer.setDescription(ent, Text.of("Attributes\n" +
                "Max Hp: " + String.format("%.3f", ent.getHealth()) + "/"+ String.format("%.3f", ent.MaxHp) +
                "\nSpeed: " + String.format("%.3f", ent.Speed) +
                "\nEnergy: " + String.format("%.3f", ent.ELvl) + "/" + String.format("%.3f", ent.MaxEnergy) +
                "\nMax Meat: " + String.format("%.3f", ent.MaxMeat)+
                "\nBreeding Cooldown: " + ent.breedingCooldown+
                "\nAge: " + ent.tickAge + "/" + MAX_AGE)
        );
    }

    public void setMinMeat(double minMeat) {
        this.MinMeat = minMeat;
    }

    public void setMaxMeat(double maxMeat) {
        this.MaxMeat = maxMeat;
    }

    public double getEnergyLevel() {
        return this.ELvl;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        ItemStack offHandStack = player.getOffHandStack();
        boolean isCarrot = itemStack.isOf(Items.CARROT) || offHandStack.isOf(Items.CARROT);

        if (isCarrot) {
            Hand usedHand = itemStack.isOf(Items.CARROT) ? hand : Hand.OFF_HAND;
            ItemStack usedItem = itemStack.isOf(Items.CARROT) ? itemStack : offHandStack;

            if (this.isBaby()) {
                if (!player.isCreative()) {
                    usedItem.decrement(1);
                }
                this.growUp((int)(this.getBreedingAge() / 20 * -0.1F), true);
                return ActionResult.SUCCESS;
            }

            if (this.isInLove()) {
                if (ELvl < MaxEnergy) {
                    updateEnergyLevel(Math.min(MaxEnergy, ELvl + 10.0));
                    player.sendMessage(Text.of("The pig has gained energy! Current energy: " + String.format("%.1f", ELvl)), true);

                    if (!player.isCreative()) {
                        usedItem.decrement(1);
                    }

                    updateDescription(this);
                    return ActionResult.SUCCESS;
                } else {
                    player.sendMessage(Text.of("The pig is already at maximum energy!"), true);
                    return ActionResult.PASS;
                }
            }

            if (ELvl < 20.0) {
                updateEnergyLevel(Math.min(MaxEnergy, ELvl + 10.0));
                player.sendMessage(Text.of("This pig cannot breed due to low energy. Energy increased to: " + String.format("%.1f", ELvl)), true);

                if (!player.isCreative()) {
                    usedItem.decrement(1);
                }

                updateDescription(this);
                return ActionResult.SUCCESS;
            } else {
                this.lovePlayer(player);
                player.sendMessage(Text.of("The pig is now in breed mode!"), true);

                if (!player.isCreative()) {
                    usedItem.decrement(1);
                }

                updateDescription(this);
                return ActionResult.SUCCESS;
            }
        }

        if (itemStack.isEmpty()) {
            if (!this.getWorld().isClient) {
                updateDescription(this);
            }
            return ActionResult.success(this.getWorld().isClient);
        }

        return super.interactMob(player, hand);
    }

    // Add method for custom growth
    public void growUp(int age, boolean overGrow) {
        int currentAge = this.getBreedingAge();
        int newAge = currentAge + age; // Increment age by provided value

        // Ensure pig reaches adulthood when age hits 0 (negative age counting)
        if (newAge > 0) {
            newAge = 0; // Reaches adulthood at age 0 (negative -> 0 for babies)
        }

        int delta = newAge - currentAge;
        this.setBreedingAge(newAge);

        // Apply forcedAge for overgrowth if necessary
        if (overGrow) {
            this.forcedAge += delta;
            if (this.happyTicksRemaining == 0) {
                this.happyTicksRemaining = 40;
                this.MaxEnergy = 100.0;
                this.ELvl = 100.0;
            }
        }
    }

    @Override
    protected void applyDamage(DamageSource source, float amount) {
        super.applyDamage(source, amount);
        wasRecentlyHit = true;

        // Start panic mode
        panicTicks = PANIC_DURATION;

        // Increase speed temporarily
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                .setBaseValue(Speed * (ELvl / MaxEnergy) * PANIC_SPEED_MULTIPLIER);

        if (!this.getWorld().isClient) {
            updateDescription(this);
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        if (this.isBaby()) {
            return;
        }

        if (!this.getWorld().isClient) {
            boolean shouldDropCooked = false;

            // Check if the entity died from fire, lava, or burning
            if (source.getName().equals("onFire") || source.getName().equals("inFire") || source.getName().equals("lava")) {
                shouldDropCooked = true;
            }

            // Check if the attacker has Fire Aspect
            if (source.getAttacker() instanceof LivingEntity attacker) {
                ItemStack weapon = attacker.getMainHandStack();
                RegistryEntry<Enchantment> fireAspectEntry = this.getWorld().getServer().getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Enchantments.FIRE_ASPECT).get();
                if (EnchantmentHelper.getLevel(fireAspectEntry, weapon) >= 1) {
                    shouldDropCooked = true;
                }
            }

            // Calculate meat amount based on energy level
            int meatAmount = 0;
            if (ELvl > 0.0) {
                meatAmount = (int) ((MinMeat + this.getWorld().random.nextInt((int) (MaxMeat - MinMeat) + 1)) * (ELvl / 100.0));
                meatAmount = Math.max(0, meatAmount);
            }

            // Drop meat based on conditions
            if (shouldDropCooked) {
                if (meatAmount > 0) {
                    this.dropStack(new ItemStack(Items.COOKED_PORKCHOP, meatAmount));
                }
            } else {
                if (meatAmount > 0) {
                    this.dropStack(new ItemStack(Items.PORKCHOP, meatAmount));
                }
            }

            // Drop bones if energy is <= 0 or the pig died of old age
            if (ELvl <= 0.0 || tickAge >= MAX_AGE) {
                this.dropStack(new ItemStack(Items.BONE, 1));
            }

            super.onDeath(source);
        }
    }

    @Override
    public CustomPigEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof CustomPigEntity))
            return (CustomPigEntity) EntityType.PIG.create(serverWorld);

        CustomPigEntity parent1 = this;
        CustomPigEntity parent2 = (CustomPigEntity) mate;

        // Calculate inheritance factor based on energy levels
        double inheritanceFactor = Math.min(parent1.ELvl, parent2.ELvl) / MaxEnergy;

        // Inherit attributes from parents, scaled by the inheritance factor
        double childMaxHp = ((parent1.MaxHp + parent2.MaxHp) / 2) * inheritanceFactor;
        double childMinMeat = ((parent1.MinMeat + parent2.MinMeat) / 2) * inheritanceFactor;
        double childMaxMeat = ((parent1.MaxMeat + parent2.MaxMeat) / 2) * inheritanceFactor;
        int childBreedingCooldown = (int) (((parent1.breedingCooldown + parent2.breedingCooldown) / 2) * (1 / inheritanceFactor));
        double childEnergy = ((parent1.ELvl + parent2.ELvl) / 2) * inheritanceFactor;

        // Create the child entity
        CustomPigEntity child = new CustomPigEntity(ModEntities.CUSTOM_PIG, serverWorld);

        // Set inherited and calculated attributes
        child.MaxHp = childMaxHp;
        child.MinMeat = childMinMeat;
        child.MaxMeat = childMaxMeat;
        child.breedingCooldown = childBreedingCooldown;
        child.ELvl = childEnergy;
        child.tickAge = 0; // Start as a baby

        // Apply stats to the child entity
        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.MaxHp);
        child.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(child.Speed * (child.ELvl / MaxEnergy));

        // Parents lose energy after breeding
        parent1.ELvl -= parent1.ELvl * 0.4F;
        parent2.ELvl -= parent2.ELvl * 0.4F;
        this.resetLoveTicks();

        if (!this.getWorld().isClient)
            updateDescription(child);

        return child;
    }

    @Override
    public void tick() {
        super.tick();

        // Only perform energy adjustments on the server side
        if (!this.getWorld().isClient) {
            // Increment age
            tickAge++;

            // Max energy is determined by age
            if(tickAge <= 4404){
                MaxEnergy = 10 * Math.log(5 * tickAge + 5);
            } else if (tickAge < LIFESPAN) {
                MaxEnergy = 100;
            } else {
                // Energy starts decreasing after LIFESPAN
                MaxEnergy = -(tickAge - LIFESPAN) / 16.0 + 100;

                // Ensure MaxEnergy doesn't go below 0
                MaxEnergy = Math.max(0, MaxEnergy);
            }

            // Age-based death: die of old age when MAX_AGE is reached
            if (tickAge >= MAX_AGE) {
                // Die of old age
                this.kill();
                return;
            }

            // Grow up if reached adult age
            if (tickAge >= 4404 && this.isBaby()) {
                growUp(220, true);
            }

            // Clamp the current energy level to the maximum cap
            if (ELvl > MaxEnergy) {
                updateEnergyLevel(MaxEnergy);
            }

            // Handle panic state
            if (panicTicks > 0) {
                panicTicks--;
                if (panicTicks == 0) {
                    // Reset speed back to normal when panic ends
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(Speed * (ELvl / MaxEnergy));
                }
            }

            // Handle energy loss if the pig was recently hit
            if (wasRecentlyHit) {
                // Reduce energy by 20% of its current level
                updateEnergyLevel(Math.max(0.0, ELvl * 0.8));
                wasRecentlyHit = false; // Reset the flag after applying the energy loss
            }

            // Energy based on environment
            boolean isOnGrass = this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.GRASS_BLOCK);

            // Adjust energy level randomly based on environment
            if (isOnGrass) {
                if (Math.random() < 0.3) { // 30% chance to gain energy
                    updateEnergyLevel(Math.min(MaxEnergy, ELvl + (0.1 + Math.random() * 0.75))); // Gain 0.1 to 0.75 energy
                }
            }

            if (Math.random() < 0.5) { // 50% chance to lose energy
                updateEnergyLevel(Math.max(0.0, ELvl - (0.05 + Math.random() * 0.3))); // Lose 0.05 to 0.3 energy
            }

            // Aging effects - decreasing energy and health as the pig gets very old
            if (tickAge > LIFESPAN) {
                // Additional energy drain for old age
                double ageFactor = (tickAge - LIFESPAN) / (double)(MAX_AGE - LIFESPAN);
                updateEnergyLevel(Math.max(0.0, ELvl - (0.05 * ageFactor))); // More energy loss based on age

                // Health deterioration with old age
                if (Math.random() < 0.1 * ageFactor) {
                    this.damage(this.getDamageSources().generic(), 0.5f * (float)ageFactor);
                }
            }

            // Health regeneration at max energy (only for pigs not in old age)
            if (ELvl == MaxEnergy && this.getHealth() < this.getMaxHealth() && tickAge < LIFESPAN) {
                this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + 0.5F)); // Regenerate 0.5 HP per second
            }

            // Autonomous breeding behavior
            if (ELvl >= 90.0 && !isBaby() && ticksSinceLastBreeding >= breedingCooldown && tickAge < LIFESPAN) {
                double searchRadius = 32.0;

                List<CustomPigEntity> mateCandidates = this.getWorld().getEntitiesByClass(
                        CustomPigEntity.class,
                        this.getBoundingBox().expand(searchRadius),
                        candidate -> candidate != this && candidate.getEnergyLevel() >= 90.0 && !candidate.isBaby()
                );

                // Find the nearest candidate
                CustomPigEntity nearestMate = null;
                double minDistanceSquared = Double.MAX_VALUE;
                for (CustomPigEntity candidate : mateCandidates) {
                    double distSq = this.squaredDistanceTo(candidate);
                    if (distSq < minDistanceSquared) {
                        minDistanceSquared = distSq;
                        nearestMate = candidate;
                    }
                }

                // If we found a mate candidate, move towards it
                if (nearestMate != null) {
                    // Start moving towards the nearest pig
                    this.getNavigation().startMovingTo(nearestMate, this.Speed * 5.0F * (this.ELvl / MaxEnergy));

                    // If close enough (within 2 blocks)
                    if (minDistanceSquared < 4.0) {
                        // Only start breeding if both pigs are not already in love
                        if (!this.isInLove() && !nearestMate.isInLove()) {
                            this.setLoveTicks(500);
                            nearestMate.setLoveTicks(500);
                            ticksSinceLastBreeding = 0;
                        }
                    }
                }
            }
            ticksSinceLastBreeding++;

            // If energy reaches 0, kill the pig
            if (ELvl <= 0.0) {
                this.kill();
            } else {
                // Update attributes dynamically if energy is greater than 0
                if (panicTicks == 0) { // Only update speed if not in panic mode
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(Speed * (ELvl / MaxEnergy));
                }

                // Update the description with the new energy level
                updateDescription(this);
            }
        }
    }
}