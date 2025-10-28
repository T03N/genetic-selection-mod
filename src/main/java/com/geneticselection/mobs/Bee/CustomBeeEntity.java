package com.geneticselection.mobs.Bee;

import com.geneticselection.attributes.AttributeCarrier;
import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.utils.DescriptionRenderer;
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

import java.util.List;
import java.util.Optional;
import static com.geneticselection.genetics.ChildInheritance.*;

public class CustomBeeEntity extends BeeEntity implements AttributeCarrier {
    private MobAttributes mobAttributes;
    private double MaxHp;
    private double Speed;
    private double ELvl;
    private double BaseAttack;
    private int generation = 0;

    private int panicTicks = 0;
    private static final int PANIC_DURATION = 100;
    private static final double PANIC_SPEED_MULTIPLIER = 2.0;
    private boolean wasRecentlyHit = false;

    // Base bee attack damage (vanilla is 2.0)
    private static final double VANILLA_BEE_ATTACK = 2.0;

    public CustomBeeEntity(EntityType<? extends BeeEntity> entityType, World world) {
        super(entityType, world);

        if (this.mobAttributes == null) {
            MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
            double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
            double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
            double energy = global.getEnergyLvl() * (0.9 + Math.random() * 0.1);
            double meat = global.getMaxMeat().orElse(0.0) + (0.98 + Math.random() * 0.1);
            double feathers = global.getMaxFeathers().orElse(0.0) + (0.98 + Math.random() * 0.1);
            this.mobAttributes = new MobAttributes(speed, health, energy, Optional.of(meat), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(feathers), Optional.empty());
        }

        this.MaxHp = this.mobAttributes.getMaxHealth();
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
        this.setHealth((float)this.MaxHp);
        this.Speed = this.mobAttributes.getMovementSpeed();
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);
        this.ELvl = this.mobAttributes.getEnergyLvl();
        this.BaseAttack = VANILLA_BEE_ATTACK;
        this.generation = 0;

        // Initialize attack damage
        applyAttackDamageScaling();

        if (!this.getWorld().isClient)
            updateDescription(this);
    }

    // Apply attack damage scaling based on energy and generation
    private void applyAttackDamageScaling() {
        // Generation bonus: +0.05 attack per generation (capped at +2.0)
        double generationBonus = Math.min(this.generation * 0.05, 2.0);

        // Energy scaling: 40% to 100% of base damage based on energy
        double energyRatio = (this.ELvl / 100.0);
        double energyMultiplier = 0.4 + (energyRatio * 0.6); // Ranges from 0.4 to 1.0

        // Calculate final attack damage
        double scaledAttack = (this.BaseAttack + generationBonus) * energyMultiplier;

        // Apply to entity attribute
        if (this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE) != null) {
            this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(scaledAttack);
        }
    }

    public void updateEnergyLevel(double newEnergyLevel) {
        this.ELvl = newEnergyLevel;

        // Update attack damage when energy changes
        applyAttackDamageScaling();

        // Sync energy level with server if needed
        if (!this.getWorld().isClient) {
            this.syncEnergyLevelToClient();
        }
    }

    public double getEnergyLevel(){
        return this.ELvl;
    }

    private void syncEnergyLevelToClient() {
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
        data.writeInt(this.getId());  // Send entity ID
        data.writeDouble(this.ELvl);  // Send the updated energy level
    }

    private void updateDescription(CustomBeeEntity ent) {
        double currentAttack = 0.0;
        if (ent.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE) != null) {
            currentAttack = ent.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        }

        DescriptionRenderer.setDescription(ent, Text.of("Attributes\n" +
                "Max Hp: " + String.format("%.1f", ent.getHealth()) + "/" + String.format("%.1f", ent.MaxHp) +
                "\nSpeed: " + String.format("%.2f", ent.Speed) +
                "\nEnergy: " + String.format("%.1f", ent.ELvl) +
                "\nAttack: " + String.format("%.2f", currentAttack) +
                "\nGen: " + ent.generation));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);

        if (itemStack.isOf(Items.POPPY)) {
            if (ELvl < 20.0) {
                player.sendMessage(Text.of("This bee cannot breed because it has low energy."), true);
                return ActionResult.FAIL;
            }
            return super.interactMob(player, hand);
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
            // Zero energy drops (e.g., bone or honeycomb as default)
            this.dropStack(new ItemStack(Items.HONEYCOMB, 1));
        } else {
            super.onDeath(source);
            if (!this.getWorld().isClient) {
                // Drop honey based on energy
                this.dropStack(new ItemStack(Items.HONEY_BOTTLE, 1));
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient) {
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
                applyAttackDamageScaling(); // Update attack after energy loss
                wasRecentlyHit = false;
            }

            // Energy gain/loss based on environment
            boolean isOnEnergySource = this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.BEE_NEST);

            if (isOnEnergySource) {
                ELvl = Math.min(100.0, ELvl + 0.1);
                applyAttackDamageScaling(); // Update attack as energy increases
            } else {
                ELvl = Math.max(0.0, ELvl - 0.05);
                applyAttackDamageScaling(); // Update attack as energy decreases
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
                    // Start moving towards the nearest bee; adjust speed as needed
                    this.getNavigation().startMovingTo(nearestMate, this.Speed * 5.0F * (this.ELvl / 100.0));

                    // If close enough (e.g., within 2 blocks; adjust the threshold as needed)
                    if (minDistanceSquared < 4.0) {
                        // Only start breeding if both bees are not already in love
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
                updateDescription(this);
            }
        }
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
        child.ELvl = childAttributes.getEnergyLvl();
        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.MaxHp);
        child.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(child.Speed * (child.ELvl / 100.0));

        // Inherit generation (higher of parents + 1)
        child.generation = Math.max(parent1.generation, parent2.generation) + 1;

        // Inherit base attack with slight improvement
        double avgBaseAttack = (parent1.BaseAttack + parent2.BaseAttack) / 2.0;
        double inheritanceFactor = Math.max(0.3, Math.min(parent1.getEnergyLevel(), parent2.getEnergyLevel()) / 100.0);
        child.BaseAttack = avgBaseAttack * (0.98 + Math.random() * 0.04); // Slight variation

        // Apply attack damage scaling for child
        child.applyAttackDamageScaling();

        parent1.ELvl -= parent1.ELvl * 0.4F;
        parent2.ELvl -= parent2.ELvl * 0.4F;

        // Update attack damage for parents after energy loss
        parent1.applyAttackDamageScaling();
        parent2.applyAttackDamageScaling();

        this.resetLoveTicks();

        influenceGlobalAttributes(child.getType());

        if (!this.getWorld().isClient) {
            updateDescription(child);
            updateDescription(parent1);
            updateDescription(parent2);
        }

        return child;
    }

    @Override
    protected void applyDamage(DamageSource source, float amount) {
        super.applyDamage(source, amount);
        wasRecentlyHit = true;
        panicTicks = PANIC_DURATION;
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                .setBaseValue(Speed * (ELvl / 100.0) * PANIC_SPEED_MULTIPLIER);
        if (!this.getWorld().isClient)
            updateDescription(this);
    }

    @Override
    public void applyCustomAttributes(MobAttributes attributes) {
    }

    // NBT data saving
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putDouble("ELvl", this.ELvl);
        nbt.putDouble("BaseAttack", this.BaseAttack);
        nbt.putInt("Generation", this.generation);
        nbt.putInt("PanicTicks", this.panicTicks);
    }

    // NBT data loading
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("ELvl")) {
            this.ELvl = nbt.getDouble("ELvl");
        }
        if (nbt.contains("BaseAttack")) {
            this.BaseAttack = nbt.getDouble("BaseAttack");
        } else {
            this.BaseAttack = VANILLA_BEE_ATTACK;
        }
        if (nbt.contains("Generation")) {
            this.generation = nbt.getInt("Generation");
        }
        if (nbt.contains("PanicTicks")) {
            this.panicTicks = nbt.getInt("PanicTicks");
        }

        // Reapply attack damage scaling after loading
        applyAttackDamageScaling();
    }
}