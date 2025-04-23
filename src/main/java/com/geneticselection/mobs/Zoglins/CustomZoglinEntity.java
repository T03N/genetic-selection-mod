package com.geneticselection.mobs.Zoglins;

import com.geneticselection.attributes.AttributeCarrier;
import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.utils.DescriptionRenderer;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.LookTargetUtil;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ZoglinEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public class CustomZoglinEntity extends ZoglinEntity implements AttributeCarrier {
    private MobAttributes mobAttributes;
    private double MaxHp;
    private double Speed;
    private double ELvl;
    private double MaxEnergy;
    private double MaxMeat;
    private double MaxLeather;

    private int panicTicks = 0;
    private static final int PANIC_DURATION = 100;
    private static final double PANIC_SPEED_MULTIPLIER = 2.0;
    private boolean wasRecentlyHit = false;

    // New breeding-related fields
    private int breedingCooldown;
    private int ticksSinceLastBreeding = 0;
    private int tickAge = 0;
    private static int LIFESPAN = 35000;
    private int happyTicksRemaining = 0;
    private int forcedAge = 0;
    private int loveTicks = 0;

    public CustomZoglinEntity(EntityType<? extends ZoglinEntity> entityType, World world) {
        super(entityType, world);

        if (this.mobAttributes == null) {
            MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
            double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
            double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
            double energy = global.getEnergyLvl() * (0.9 + Math.random() * 0.1);
            double meat = global.getMaxMeat().orElse(0.0) + (0.98 + Math.random() * 0.1);
            double leather = global.getMaxLeather().orElse(0.0) + (0.98 + Math.random() * 0.1);
            this.mobAttributes = new MobAttributes(speed, health, energy, Optional.of(meat), Optional.of(leather), Optional.empty(), Optional.empty(), Optional.empty());
        }

        this.MaxHp = this.mobAttributes.getMaxHealth();
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
        this.setHealth((float)this.MaxHp);
        this.Speed = this.mobAttributes.getMovementSpeed();
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);
        this.ELvl = this.mobAttributes.getEnergyLvl();
        this.MaxEnergy = 100.0;

        this.mobAttributes.getMaxMeat().ifPresent(maxMeat -> {
            this.MaxMeat = maxMeat;
        });
        this.mobAttributes.getMaxLeather().ifPresent(maxLeather -> {
            this.MaxLeather = maxLeather;
        });

        // Initialize breeding cooldown
        this.breedingCooldown = 3000 + (int)((1 - (ELvl / 100.0)) * 2000) + random.nextInt(2001);

        if (!this.getWorld().isClient)
            updateDescription(this);
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

    private void updateDescription(CustomZoglinEntity ent) {
        DescriptionRenderer.setDescription(ent, Text.of("Attributes\n" +
                "Max Hp: " + String.format("%.1f", ent.getHealth()) + "/" + String.format("%.1f", ent.MaxHp) +
                "\nSpeed: " + String.format("%.2f", ent.Speed) +
                "\nEnergy: " + String.format("%.1f", ent.ELvl) + "/" + String.format("%.1f", ent.MaxEnergy) +
                "\nMax Meat: " + String.format("%.1f", ent.MaxMeat) +
                "\nMax Leather: " + String.format("%.1f", ent.MaxLeather) +
                "\nBreeding Cooldown: " + ent.breedingCooldown +
                "\nAge: " + ent.tickAge));
    }

    public double getEnergyLevel() {
        return this.ELvl;
    }

    // New methods for breeding
    public boolean isBaby() {
        return this.tickAge < 4404;
    }

    public void setLoveTicks(int ticks) {
        this.loveTicks = ticks;
    }

    public boolean isInLove() {
        return this.loveTicks > 0;
    }

    public void resetLoveTicks() {
        this.loveTicks = 0;
    }

    public void lovePlayer(PlayerEntity player) {
        this.setLoveTicks(600);
        // Make hearts appear (if implemented)
    }

    public void growUp(int age, boolean overGrow) {
        if (overGrow) {
            this.forcedAge += age;
            if (this.happyTicksRemaining == 0) {
                this.happyTicksRemaining = 40;
                this.MaxEnergy = 100.0;
                this.ELvl = 100.0;
            }
        }

        // Set age to adult stage if it reaches maturity
        if (this.tickAge + age >= 4404) {
            this.tickAge = 4404;
        } else {
            this.tickAge += age;
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        ItemStack offHandStack = player.getOffHandStack();
        boolean isCrimsonNylium = itemStack.isOf(Items.CRIMSON_NYLIUM) || offHandStack.isOf(Items.CRIMSON_NYLIUM);

        if (isCrimsonNylium) {
            // Handle main hand or offhand nylium logic
            Hand usedHand = itemStack.isOf(Items.CRIMSON_NYLIUM) ? hand : Hand.OFF_HAND;
            ItemStack usedItem = itemStack.isOf(Items.CRIMSON_NYLIUM) ? itemStack : offHandStack;

            if (this.isBaby()) {
                return ActionResult.PASS; // Do nothing if the zoglin is a baby
            }

            // If the zoglin is in love mode
            if (this.isInLove()) {
                if (ELvl < MaxEnergy) {
                    updateEnergyLevel(Math.min(MaxEnergy, ELvl + 10.0)); // Gain energy (up to max)
                    player.sendMessage(Text.of("The zoglin has gained energy! Current energy: " + String.format("%.1f", ELvl)), true);

                    if (!player.isCreative()) { // Only consume nylium if not in Creative mode
                        usedItem.decrement(1);
                    }

                    updateDescription(this); // Update description with new energy level
                    return ActionResult.SUCCESS;
                } else {
                    // Zoglin is in love mode and at max energy; do nothing
                    player.sendMessage(Text.of("The zoglin is already at maximum energy!"), true);
                    return ActionResult.PASS;
                }
            }

            if (ELvl < 20.0) {
                updateEnergyLevel(Math.min(MaxEnergy, ELvl + 10.0)); // Gain energy (up to max)
                player.sendMessage(Text.of("This zoglin cannot breed due to low energy. Energy increased to: " + String.format("%.1f", ELvl)), true);

                if (!player.isCreative()) { // Only consume nylium if not in Creative mode
                    usedItem.decrement(1);
                }

                updateDescription(this); // Update description with new energy level
                return ActionResult.SUCCESS;
            } else {
                // If energy is sufficient, trigger breeding
                this.lovePlayer(player);
                player.sendMessage(Text.of("The zoglin is now in breed mode!"), true);

                if (!player.isCreative()) { // Only consume nylium if not in Creative mode
                    usedItem.decrement(1);
                }

                updateDescription(this); // Update description
                return ActionResult.SUCCESS;
            }
        }

        if (itemStack.isEmpty()) {
            if (!this.getWorld().isClient) {
                updateDescription(this);
            }
            return ActionResult.SUCCESS;
        }

        return super.interactMob(player, hand);
    }

    @Override
    public void onDeath(DamageSource source) {
        if (this.isBaby()) {
            return;
        }

        if (ELvl <= 0.0) {
            // Drop minimal resources
            this.dropStack(new ItemStack(Items.LEATHER, 1));
        } else {
            super.onDeath(source);
            if (!this.getWorld().isClient) {
                // Drop leather and meat based on energy
                int leatherAmount = (int) ((MaxLeather) * (ELvl / 100.0));
                this.dropStack(new ItemStack(Items.LEATHER, leatherAmount));

                int meatAmount = (int) ((MaxMeat) * (ELvl / 100.0));
                this.dropStack(new ItemStack(Items.ROTTEN_FLESH, meatAmount));
            }
        }
    }

    public void setAttackTarget(LivingEntity entity) {
        this.brain.forget(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        this.brain.remember(MemoryModuleType.ATTACK_TARGET, entity, 0L);
    }

    // New method for breeding
    public CustomZoglinEntity createChild(ServerWorld serverWorld, CustomZoglinEntity mate) {
        // Create child entity
        CustomZoglinEntity parent1 = this;
        CustomZoglinEntity parent2 = mate;

        // Calculate inheritance factor based on parents' energy levels
        double inheritanceFactor = Math.min(parent1.ELvl, parent2.ELvl) / 100.0;

        // Inherit attributes from parents, scaled by inheritance factor
        double childMaxHp = ((parent1.MaxHp + parent2.MaxHp) / 2) * inheritanceFactor;
        double childMaxMeat = ((parent1.MaxMeat + parent2.MaxMeat) / 2) * inheritanceFactor;
        double childMaxLeather = ((parent1.MaxLeather + parent2.MaxLeather) / 2) * inheritanceFactor;
        int childBreedingCooldown = (int) (((parent1.breedingCooldown + parent2.breedingCooldown) / 2) * (1 / inheritanceFactor));
        double childEnergy = ((parent1.ELvl + parent2.ELvl) / 2) * inheritanceFactor;

        // Create the child entity
        CustomZoglinEntity child = (CustomZoglinEntity) ModEntities.CUSTOM_ZOGLIN.create(serverWorld);

        // Set inherited and calculated attributes
        child.MaxHp = childMaxHp;
        child.MaxMeat = childMaxMeat;
        child.MaxLeather = childMaxLeather;
        child.breedingCooldown = childBreedingCooldown;
        child.ELvl = childEnergy;
        child.tickAge = 0; // Start as a baby

        // Apply stats to the child entity
        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.MaxHp);
        child.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(child.Speed * (child.ELvl / 100.0));

        // Parents lose energy after breeding
        parent1.ELvl -= parent1.ELvl * 0.4F;
        parent2.ELvl -= parent2.ELvl * 0.4F;
        parent1.resetLoveTicks();
        parent2.resetLoveTicks();

        if (!serverWorld.isClient)
            updateDescription(child);

        return child;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient) {
            // Update love ticks if in love
            if (loveTicks > 0) {
                loveTicks--;
            }

            // Max energy is determined by age
            if(tickAge <= 4404){
                MaxEnergy = 10 * Math.log(5 * tickAge + 5);
            } else if (tickAge < LIFESPAN) {
                MaxEnergy = 100;
            } else {
                MaxEnergy = -(tickAge - LIFESPAN) / 16.0 + 100;
            }
            tickAge++;

            // Grow up if reached adult age
            if (tickAge >= 4404 && this.isBaby()) {
                growUp(220, true);
            }

            // Clamp energy level to maximum cap
            if (ELvl > MaxEnergy) {
                updateEnergyLevel(MaxEnergy);
            }

            // Handle panic
            if (panicTicks > 0) {
                panicTicks--;
                if (panicTicks == 0) {
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(Speed * (ELvl / MaxEnergy));
                }
            }

            // Handle energy loss from damage
            if (wasRecentlyHit) {
                ELvl = Math.max(0.0, ELvl * 0.8);
                wasRecentlyHit = false;
            }

            // Energy gain/loss based on environment
            boolean isOnEnergySource = this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.CRIMSON_NYLIUM);

            if (isOnEnergySource) {
                if (Math.random() < 0.3) { // 30% chance to gain energy
                    updateEnergyLevel(Math.min(MaxEnergy, ELvl + (0.1 + Math.random() * 0.75))); // Gain 0.1 to 0.75 energy
                }
            }

            if (Math.random() < 0.5) { // 50% chance to lose energy
                updateEnergyLevel(Math.max(0.0, ELvl - (0.05 + Math.random() * 0.3))); // Lose 0.05 to 0.3 energy
            }

            // Health regeneration at max energy
            if (ELvl == MaxEnergy && this.getHealth() < this.getMaxHealth()) {
                this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + 0.5F));
            }

            // Auto-breeding behavior when energy is high
            if (ELvl >= 90.0 && !isBaby() && ticksSinceLastBreeding >= breedingCooldown) {
                double searchRadius = 32.0;

                List<CustomZoglinEntity> mateCandidates = this.getWorld().getEntitiesByClass(
                        CustomZoglinEntity.class,
                        this.getBoundingBox().expand(searchRadius),
                        candidate -> candidate != this && candidate.getEnergyLevel() >= 90.0 && !candidate.isBaby()
                );

                // Find the nearest candidate
                CustomZoglinEntity nearestMate = null;
                double minDistanceSquared = Double.MAX_VALUE;
                for (CustomZoglinEntity candidate : mateCandidates) {
                    double distSq = this.squaredDistanceTo(candidate);
                    if (distSq < minDistanceSquared) {
                        minDistanceSquared = distSq;
                        nearestMate = candidate;
                    }
                }

                // If we found a mate candidate, move towards it
                if (nearestMate != null) {
                    // Start moving towards the nearest zoglin
                    this.getNavigation().startMovingTo(nearestMate, this.Speed * 5.0F * (this.ELvl / MaxEnergy));

                    // If close enough (within 2 blocks)
                    if (minDistanceSquared < 4.0) {
                        // Only start breeding if both zoglins are not already in love
                        if (!this.isInLove() && !nearestMate.isInLove()) {
                            this.setLoveTicks(500);
                            nearestMate.setLoveTicks(500);
                            ticksSinceLastBreeding = 0;

                            // Create child if on server
                            if (!this.getWorld().isClient && this.getWorld() instanceof ServerWorld) {
                                ServerWorld serverWorld = (ServerWorld) this.getWorld();
                                CustomZoglinEntity child = this.createChild(serverWorld, nearestMate);
                                if (child != null) {
                                    child.setBaby(true);
                                    child.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
                                    serverWorld.spawnEntityAndPassengers(child);
                                }
                            }
                        }
                    }
                }
            }
            ticksSinceLastBreeding++;

            // Kill if energy is 0
            if (ELvl <= 0.0) {
                this.kill();
            } else {
                // Update speed
                if (panicTicks == 0) {
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(Speed * (ELvl / MaxEnergy));
                }
                updateDescription(this);
            }
        }
    }

    @Override
    protected void applyDamage(DamageSource source, float amount) {
        super.applyDamage(source, amount);
        wasRecentlyHit = true;
        panicTicks = PANIC_DURATION;
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                .setBaseValue(Speed * (ELvl / MaxEnergy) * PANIC_SPEED_MULTIPLIER);
        if (!this.getWorld().isClient)
            updateDescription(this);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean bl = super.damage(source, amount);
        if (this.getWorld().isClient) {
            return false;
        } else if (bl && source.getAttacker() instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)source.getAttacker();
            if (this.canTarget(livingEntity) && !LookTargetUtil.isNewTargetTooFar(this, livingEntity, 4.0)) {
                this.setAttackTarget(livingEntity);
            }

            return bl;
        } else {
            return bl;
        }
    }

    // Add method to set baby state
    public void setBaby(boolean baby) {
        if (baby) {
            this.tickAge = 0;
        } else {
            this.tickAge = 4404;
        }
    }

    @Override
    public void applyCustomAttributes(MobAttributes attributes) {
        attributes.getMaxMeat().ifPresent(maxMeat -> this.MaxMeat = maxMeat);
        attributes.getMaxLeather().ifPresent(maxLeather -> this.MaxLeather = maxLeather);
    }
}