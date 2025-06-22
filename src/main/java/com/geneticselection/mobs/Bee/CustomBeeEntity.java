package com.geneticselection.mobs.Bee;

import com.geneticselection.attributes.AttributeCarrier;
import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.ModEntities;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;

import java.util.List;
import java.util.Optional;
import static com.geneticselection.genetics.ChildInheritance.*;

public class CustomBeeEntity extends BeeEntity implements AttributeCarrier {
    private MobAttributes mobAttributes;
    private double MaxHp;
    private double Speed;
    private static final TrackedData<Float> MAX_HP = DataTracker.registerData(CustomBeeEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> ELVL = DataTracker.registerData(CustomBeeEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> MAX_ENERGY = DataTracker.registerData(CustomBeeEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> TICK_AGE = DataTracker.registerData(CustomBeeEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private int panicTicks = 0;
    private static final int PANIC_DURATION = 100;
    private static final double PANIC_SPEED_MULTIPLIER = 2.0;
    private boolean wasRecentlyHit = false;

    public CustomBeeEntity(EntityType<? extends BeeEntity> entityType, World world) {
        super(entityType, world);
        if (this.mobAttributes == null) {
            MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
            double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
            double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
            double energy = global.getEnergyLvl() * (0.9 + Math.random() * 0.1);
            double meat = global.getMaxMeat().orElse(0.0) + (0.98 + Math.random() * 0.1);
            double feathers = global.getMaxFeathers().orElse(0.0) + (0.98 + Math.random() * 0.1);
            this.mobAttributes = new MobAttributes(speed, health, energy, Optional.of(meat), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(feathers));
        }
        this.MaxHp = this.mobAttributes.getMaxHealth();
        var maxHealthAttr = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) maxHealthAttr.setBaseValue(this.MaxHp);
        this.Speed = this.mobAttributes.getMovementSpeed();
        var speedAttr = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.setBaseValue(this.Speed);
        this.ELvl = this.mobAttributes.getEnergyLvl();
        if (!world.isClient) {
            this.dataTracker.set(MAX_HP, (float)this.mobAttributes.getMaxHealth());
            this.dataTracker.set(ELVL, (float)this.mobAttributes.getEnergyLvl());
            this.dataTracker.set(MAX_ENERGY, 100.0f);
            this.dataTracker.set(TICK_AGE, 0);
        }
    }
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(MAX_HP, 10.0f);
        builder.add(ELVL, 100.0f);
        builder.add(MAX_ENERGY, 100.0f);
        builder.add(TICK_AGE, 0);
    }
    public float getMaxHpTracked() { return this.dataTracker.get(MAX_HP); }
    public double getEnergyLevel() { return this.dataTracker.get(ELVL); }
    public float getMaxEnergy() { return this.dataTracker.get(MAX_ENERGY); }
    public int getTickAge() { return this.dataTracker.get(TICK_AGE); }
    public double getSpeed() { return this.Speed; }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putFloat("MaxHp", this.getMaxHpTracked());
        nbt.putFloat("ELvl", (float)this.getEnergyLevel());
        nbt.putFloat("MaxEnergy", this.getMaxEnergy());
        nbt.putInt("TickAge", this.getTickAge());
    }
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(MAX_HP, nbt.getFloat("MaxHp"));
        this.dataTracker.set(ELVL, nbt.getFloat("ELvl"));
        this.dataTracker.set(MAX_ENERGY, nbt.getFloat("MaxEnergy"));
        this.dataTracker.set(TICK_AGE, nbt.getInt("TickAge"));
    }
    public void updateEnergyLevel(double newEnergyLevel) {
        if (!this.getWorld().isClient) {
            this.dataTracker.set(ELVL, (float)newEnergyLevel);
        }
    }
    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient) {
            int currentTickAge = this.getTickAge();
            float currentMaxEnergy = 100.0f;
            this.dataTracker.set(TICK_AGE, currentTickAge + 1);
            this.dataTracker.set(MAX_ENERGY, currentMaxEnergy);

            // Handle panic
            if (panicTicks > 0) {
                panicTicks--;
                if (panicTicks == 0) {
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(Speed * (ELvl / 100.0));
                }
            }

            // Handle energy loss from damage
            if (wasRecentlyHit) {
                ELvl = Math.max(0.0, ELvl * 0.8);
                wasRecentlyHit = false;
            }

            // Energy gain/loss based on environment
            boolean isOnEnergySource = this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.BEE_NEST);

            if (isOnEnergySource) {
                ELvl = Math.min(100.0, ELvl + 0.1);
            } else {
                ELvl = Math.max(0.0, ELvl - 0.05);
            }

            // Health regeneration at max energy
            if (ELvl == 100.0 && this.getHealth() < this.getMaxHealth()) {
                this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + 0.5F));
            }

            if (ELvl >= 90.0) {
                double searchRadius = 32.0;

                List<CustomBeeEntity> mateCandidates = this.getWorld().getEntitiesByClass(
                    CustomBeeEntity.class,
                    this.getBoundingBox().expand(searchRadius),
                    candidate -> candidate != this && candidate.getEnergyLevel() >= 90.0 && !candidate.isBaby()
                );

                // Find the nearest candidate
                CustomBeeEntity nearestMate = null;
                double minDistanceSquared = Double.MAX_VALUE;
                for (CustomBeeEntity candidate : mateCandidates) {
                    double distSq = this.squaredDistanceTo(candidate);
                    if (distSq < minDistanceSquared) {
                        minDistanceSquared = distSq;
                        nearestMate = candidate;
                    }
                }

                // If we found a mate candidate, move towards it
                if (nearestMate != null) {
                    // Start moving towards the nearest cow; adjust speed as needed
                    this.getNavigation().startMovingTo(nearestMate, this.Speed * 5.0F * (this.ELvl / 100.0));

                    // If close enough (e.g., within 2 blocks; adjust the threshold as needed)
                    if (minDistanceSquared < 4.0) {
                        // Only start breeding if both cows are not already in love
                        if (!this.isInLove() && !nearestMate.isInLove()) {
                            this.setLoveTicks(100);
                            nearestMate.setLoveTicks(100);
                        }
                    }
                }
            }

            // Kill if energy is 0
            if (ELvl <= 0.0) {
                this.kill();
            } else {
                // Update speed
                if (panicTicks == 0) {
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(Speed * (ELvl / 100.0));
                }
            }
        }
        var maxHealthAttr = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) maxHealthAttr.setBaseValue(this.getMaxHpTracked());
    }

    @Override
    public CustomBeeEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof CustomBeeEntity)) {
            return (CustomBeeEntity) EntityType.BEE.create(serverWorld);
        }

        CustomBeeEntity parent1 = this;
        CustomBeeEntity parent2 = (CustomBeeEntity) mate;

        MobAttributes attr1 = parent1.mobAttributes;
        MobAttributes attr2 = parent2.mobAttributes;

        MobAttributes childAttributes = inheritAttributes(attr1, attr2);

        CustomBeeEntity child = new CustomBeeEntity(ModEntities.CUSTOM_BEE, serverWorld);

        child.mobAttributes = childAttributes;
        applyAttributes(child, childAttributes);

        child.MaxHp = childAttributes.getMaxHealth();
        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.MaxHp);
        child.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(child.Speed * (child.ELvl / 100.0));

        parent1.ELvl -= parent1.ELvl * 0.4F;
        parent2.ELvl -= parent2.ELvl * 0.4F;
        this.resetLoveTicks();

        influenceGlobalAttributes(child.getType());

        return child;
    }

    @Override
    protected void applyDamage(DamageSource source, float amount) {
        super.applyDamage(source, amount);
        wasRecentlyHit = true;
        panicTicks = PANIC_DURATION;
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                .setBaseValue(Speed * (ELvl / 100.0) * PANIC_SPEED_MULTIPLIER);
    }

    @Override
    public void applyCustomAttributes(MobAttributes attributes) {
    }
}

