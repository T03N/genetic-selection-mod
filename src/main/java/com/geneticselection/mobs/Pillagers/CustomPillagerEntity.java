package com.geneticselection.mobs.Pillagers;

import com.geneticselection.attributes.AttributeCarrier;
import com.geneticselection.attributes.AttributeKey;
import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.utils.DescriptionRenderer;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.geneticselection.genetics.ChildInheritance.*;

public class CustomPillagerEntity extends PillagerEntity implements AttributeCarrier {
    private MobAttributes mobAttributes;
    private double MaxHp;
    private double Speed;
    private double ELvl;
    private double MaxEnergy;
    private double BaseAttack;
    private int generation = 0;
    private int tickAge = 0;
    private int breedingCooldown;
    private int ticksSinceLastBreeding = 0;

    // Evolution bonuses from combat
    private double bonusAttack = 0.0;
    private double bonusHealth = 0.0;
    private double bonusSpeed = 0.0;
    private int killCount = 0;

    private boolean wasRecentlyHit = false;

    // Base pillager attack damage (vanilla crossbow does ~3.5-5.0 depending on difficulty)
    private static final double VANILLA_PILLAGER_ATTACK = 5.0;

    public CustomPillagerEntity(EntityType<? extends PillagerEntity> entityType, World world) {
        super(entityType, world);

        if (this.mobAttributes == null) {
            MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
            double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
            double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
            double energy = global.getEnergyLvl() * (0.9 + Math.random() * 0.1);
            this.mobAttributes = new MobAttributes(speed, health, energy,
                    Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty());
            this.tickAge = 0;
        }

        this.MaxHp = this.mobAttributes.getMaxHealth();
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
        this.setHealth((float)this.MaxHp);

        this.Speed = this.mobAttributes.getMovementSpeed();
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);

        this.ELvl = this.mobAttributes.getEnergyLvl();
        this.MaxEnergy = 100.0;
        this.BaseAttack = VANILLA_PILLAGER_ATTACK;
        this.generation = 0;

        this.breedingCooldown = 6000 + (int)((1 - (ELvl / 100.0)) * 3000) + random.nextInt(2001);
        this.ticksSinceLastBreeding = 0;

        this.bonusAttack = 0.0;
        this.bonusHealth = 0.0;
        this.bonusSpeed = 0.0;
        this.killCount = 0;

        applyBonuses();

        if (!this.getWorld().isClient) {
            updateDescription(this);
        }
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
    }

    // Apply all bonuses and scaling
    private void applyBonuses() {
        if (this.mobAttributes == null) {
            initFromGlobalAttributes(this.getType());
        }

        double inherentMaxHp = this.mobAttributes.getMaxHealth();
        double inherentSpeed = this.mobAttributes.getMovementSpeed();

        double effectiveMaxHp = inherentMaxHp + this.bonusHealth;
        double effectiveSpeed = inherentSpeed * (1.0 + this.bonusSpeed);

        // Base attack includes generation bonus and evolution bonus
        double generationBonus = Math.min(this.generation * 0.15, 8.0); // 0.15 per gen, cap at +8
        double effectiveBaseAttack = this.BaseAttack + generationBonus + this.bonusAttack;

        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(effectiveMaxHp);
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(effectiveSpeed);

        // Attack will be energy-scaled in tick(), set base here
        this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(effectiveBaseAttack);

        if(this.getHealth() > this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
    }

    private void initFromGlobalAttributes(EntityType<?> entityType) {
        this.mobAttributes = GlobalAttributesManager.getAttributes(entityType);
        this.MaxHp = this.mobAttributes.getMaxHealth();
        this.Speed = this.mobAttributes.getMovementSpeed();
        this.MaxEnergy = 100.0;
        this.ELvl = this.mobAttributes.getEnergyLvl();
    }

    // Apply attack damage scaling based on energy
    private void applyAttackDamageScaling() {
        double generationBonus = Math.min(this.generation * 0.15, 8.0);
        double baseAttack = this.BaseAttack + generationBonus + this.bonusAttack;

        // Energy scaling: 60% to 100% of base damage
        double energyRatio = (this.MaxEnergy > 0) ? (this.ELvl / this.MaxEnergy) : 1.0;
        double energyMultiplier = 0.6 + (energyRatio * 0.4); // Ranges from 0.6 to 1.0

        double scaledAttack = baseAttack * energyMultiplier;

        this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(scaledAttack);
    }

    public void updateEnergyLevel(double newEnergyLevel) {
        this.ELvl = Math.max(0.0, Math.min(this.MaxEnergy, newEnergyLevel));
        applyAttackDamageScaling();

        if (!this.getWorld().isClient) {
            this.syncEnergyLevelToClient();
        }
    }

    private void syncEnergyLevelToClient() {
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
        data.writeInt(this.getId());
        data.writeDouble(this.ELvl);
    }

    public double getEnergyLevel() {
        return this.ELvl;
    }

    private void updateDescription(CustomPillagerEntity ent) {
        if (ent.getWorld().isClient() || !ent.isAlive()) return;

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
                        " | Gen: " + ent.generation +
                        "\nBreed CD: " + String.format("%.1f", remainingBreedCDTicks / 20.0) + "s"
        ));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);

        // Allow stat check with sneak + empty hand
        if(player.isSneaking() && itemStack.isEmpty()) {
            if (!this.getWorld().isClient) {
                updateDescription(this);
            }
            return ActionResult.SUCCESS;
        }

        // Feed with emeralds to boost energy
        if (itemStack.isOf(Items.EMERALD) && this.ELvl < this.MaxEnergy) {
            if (!player.getAbilities().creativeMode) {
                itemStack.decrement(1);
            }
            updateEnergyLevel(this.ELvl + 20.0);
            return ActionResult.SUCCESS;
        }

        return super.interactMob(player, hand);
    }

    // Called when this pillager kills something
    public void onSuccessfulKill() {
        if (!this.getWorld().isClient) {
            this.killCount++;

            // Energy gain from kills
            double energyGain = 5.0 + Math.random() * 5.0; // 5-10 energy
            updateEnergyLevel(this.ELvl + energyGain);

            // Small heal
            this.heal(this.getMaxHealth() * 0.1f);

            // Evolution chance (slightly higher for pillagers)
            float evolutionChance = 0.08f + (float)Math.min(this.killCount, 100) / 2000.0f;
            if (this.random.nextFloat() < evolutionChance) {
                int statChoice = this.random.nextInt(3);
                switch(statChoice) {
                    case 0: bonusAttack += 0.1 + random.nextDouble() * 0.2; break;
                    case 1: bonusHealth += 0.5 + random.nextDouble() * 1.0; break;
                    case 2: bonusSpeed += 0.002 + random.nextDouble() * 0.003; break;
                }

                // Cap bonuses
                this.bonusAttack = Math.min(this.bonusAttack, 12.0);
                this.bonusHealth = Math.min(this.bonusHealth, 30.0);
                this.bonusSpeed = Math.min(this.bonusSpeed, 0.20);

                applyBonuses();
                updateDescription(this);
            }
        }
    }

    @Override
    protected void applyDamage(DamageSource source, float amount) {
        super.applyDamage(source, amount);
        if (!this.getWorld().isClient && this.isAlive()) {
            this.wasRecentlyHit = true;
            updateDescription(this);
        }
    }

    @Override
    public void tick() {
        if (this.mobAttributes == null && !this.getWorld().isClient) {
            initFromGlobalAttributes(this.getType());
            applyBonuses();
        }

        super.tick();

        if (!this.getWorld().isClient) {
            tickAge++;

            // Energy loss from being hit
            if (wasRecentlyHit) {
                updateEnergyLevel(this.ELvl * 0.90); // Lose 10%
                wasRecentlyHit = false;
            }

            // Passive energy drain (pillagers need to raid/fight to survive)
            if (Math.random() < 0.05) { // 5% chance per tick
                updateEnergyLevel(this.ELvl - 0.08);
            }

            // Small health regen at high energy
            if (ELvl >= MaxEnergy * 0.95 && this.getHealth() < this.getMaxHealth()) {
                this.heal(0.15F);
            }

            // Automated breeding (pillagers breed when well-fed and successful)
            if (ELvl >= 80.0 && ticksSinceLastBreeding >= breedingCooldown) {
                double searchRadius = 48.0;

                List<CustomPillagerEntity> mateCandidates = this.getWorld().getEntitiesByClass(
                        CustomPillagerEntity.class,
                        this.getBoundingBox().expand(searchRadius),
                        candidate -> candidate != this &&
                                candidate.getEnergyLevel() >= 80.0 &&
                                candidate.ticksSinceLastBreeding >= candidate.breedingCooldown
                );

                if (!mateCandidates.isEmpty()) {
                    CustomPillagerEntity nearestMate = null;
                    double minDistanceSquared = Double.MAX_VALUE;

                    for (CustomPillagerEntity candidate : mateCandidates) {
                        double distSq = this.squaredDistanceTo(candidate);
                        if (distSq < minDistanceSquared) {
                            minDistanceSquared = distSq;
                            nearestMate = candidate;
                        }
                    }

                    if (nearestMate != null && minDistanceSquared < 16.0) { // Within 4 blocks
                        // Breed!
                        if (this.getWorld() instanceof ServerWorld serverWorld) {
                            CustomPillagerEntity child = createChild(serverWorld, nearestMate);
                            if (child != null) {
                                child.refreshPositionAndAngles(
                                        this.getX(), this.getY(), this.getZ(),
                                        this.getYaw(), 0.0F
                                );
                                serverWorld.spawnEntity(child);

                                this.ticksSinceLastBreeding = 0;
                                nearestMate.ticksSinceLastBreeding = 0;
                            }
                        }
                    }
                }
            }

            if (ticksSinceLastBreeding < breedingCooldown) {
                ticksSinceLastBreeding++;
            }

            // Starvation damage
            if (ELvl <= 0.0) {
                this.damage(this.getDamageSources().starve(), 1.5f);
            }

            // Update speed based on energy
            double currentBaseSpeed = this.Speed * (1.0 + this.bonusSpeed);
            double energyMultiplier = (this.MaxEnergy > 0) ? (this.ELvl / this.MaxEnergy) : 1.0;
            double calculatedSpeed = currentBaseSpeed * energyMultiplier;
            double newSpeed = Math.max(calculatedSpeed, this.Speed * 0.4); // Min 40% speed

            this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(newSpeed);

            // Update attack damage scaling
            applyAttackDamageScaling();

            // Periodic description update
            if (tickAge % 40 == 0) {
                updateDescription(this);
            }
        }
    }

    @Nullable
    public CustomPillagerEntity createChild(ServerWorld serverWorld, CustomPillagerEntity mate) {
        CustomPillagerEntity parent1 = this;
        CustomPillagerEntity parent2 = mate;
        CustomPillagerEntity child = ModEntities.CUSTOM_PILLAGER.create(serverWorld);

        if (child == null) return null;

        // Ensure parents have mobAttributes
        if (parent1.mobAttributes == null) parent1.initFromGlobalAttributes(parent1.getType());
        if (parent2.mobAttributes == null) parent2.initFromGlobalAttributes(parent2.getType());

        // Inherit base attributes
        double p1Speed = parent1.mobAttributes.getMovementSpeed();
        double p2Speed = parent2.mobAttributes.getMovementSpeed();
        double p1MaxHp = parent1.mobAttributes.getMaxHealth();
        double p2MaxHp = parent2.mobAttributes.getMaxHealth();

        double childBaseSpeed = ((p1Speed + p2Speed) / 2.0) * (0.95 + random.nextDouble() * 0.1);
        double childBaseMaxHp = ((p1MaxHp + p2MaxHp) / 2.0) * (0.95 + random.nextDouble() * 0.1);
        double childBaseEnergy = 100.0;

        child.mobAttributes = new MobAttributes(new EnumMap<>(Map.of(
                AttributeKey.MOVEMENT_SPEED, childBaseSpeed,
                AttributeKey.MAX_HEALTH, childBaseMaxHp,
                AttributeKey.ENERGY, childBaseEnergy
        )));

        child.MaxHp = childBaseMaxHp;
        child.Speed = childBaseSpeed;
        child.ELvl = 40.0; // Start with moderate energy
        child.MaxEnergy = 100.0;
        child.tickAge = 0;

        // Inherit generation
        child.generation = Math.max(parent1.generation, parent2.generation) + 1;

        // Inherit bonuses
        double inheritanceFactor = Math.max(0.4, Math.min(parent1.getEnergyLevel(), parent2.getEnergyLevel()) / 100.0);
        double randomFactor = 0.90 + random.nextDouble() * 0.2;

        double avgBonusAttack = (parent1.bonusAttack + parent2.bonusAttack) / 2.0;
        double avgBonusHealth = (parent1.bonusHealth + parent2.bonusHealth) / 2.0;
        double avgBonusSpeed = (parent1.bonusSpeed + parent2.bonusSpeed) / 2.0;
        int avgKillCount = (parent1.killCount + parent2.killCount) / 2;

        child.bonusAttack = Math.max(0, avgBonusAttack * inheritanceFactor * randomFactor * 1.08); // 8% breeding bonus
        child.bonusHealth = Math.max(0, avgBonusHealth * inheritanceFactor * randomFactor);
        child.bonusSpeed = Math.max(0, avgBonusSpeed * inheritanceFactor * randomFactor);
        child.killCount = (int)(avgKillCount * inheritanceFactor * 0.5);

        // Inherit base attack
        child.BaseAttack = (parent1.BaseAttack + parent2.BaseAttack) / 2.0 * (0.98 + random.nextDouble() * 0.04);

        // Calculate breeding cooldown
        double inverseFactor = (inheritanceFactor > 0.1) ? (1 / inheritanceFactor) : 10.0;
        int childBreedingCooldown = (int) (((parent1.breedingCooldown + parent2.breedingCooldown) / 2.0)
                * inverseFactor * (0.9 + random.nextDouble() * 0.2));
        child.breedingCooldown = Math.max(3000, childBreedingCooldown); // Min 2.5 min cooldown
        child.ticksSinceLastBreeding = 0;

        child.applyBonuses();

        // Reduce parent energy
        parent1.updateEnergyLevel(parent1.getEnergyLevel() * 0.6);
        parent2.updateEnergyLevel(parent2.getEnergyLevel() * 0.6);

        if (!serverWorld.isClient) {
            updateDescription(child);
            updateDescription(parent1);
            updateDescription(parent2);
        }

        return child;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putDouble("ELvl", this.ELvl);
        nbt.putDouble("MaxEnergy", this.MaxEnergy);
        nbt.putDouble("BaseAttack", this.BaseAttack);
        nbt.putInt("Generation", this.generation);
        nbt.putInt("TickAge", this.tickAge);
        nbt.putInt("BreedingCooldown", this.breedingCooldown);
        nbt.putInt("TicksSinceLastBreeding", this.ticksSinceLastBreeding);
        nbt.putDouble("BonusAttack", this.bonusAttack);
        nbt.putDouble("BonusHealth", this.bonusHealth);
        nbt.putDouble("BonusSpeed", this.bonusSpeed);
        nbt.putInt("KillCount", this.killCount);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        if (this.mobAttributes == null) {
            initFromGlobalAttributes(this.getType());
        }

        super.readCustomDataFromNbt(nbt);

        this.ELvl = nbt.contains("ELvl") ? nbt.getDouble("ELvl") : this.mobAttributes.getEnergyLvl();
        this.MaxEnergy = nbt.getDouble("MaxEnergy");
        this.BaseAttack = nbt.contains("BaseAttack") ? nbt.getDouble("BaseAttack") : VANILLA_PILLAGER_ATTACK;
        this.generation = nbt.getInt("Generation");
        this.tickAge = nbt.getInt("TickAge");
        this.breedingCooldown = nbt.getInt("BreedingCooldown");
        this.ticksSinceLastBreeding = nbt.getInt("TicksSinceLastBreeding");
        this.bonusAttack = nbt.getDouble("BonusAttack");
        this.bonusHealth = nbt.getDouble("BonusHealth");
        this.bonusSpeed = nbt.getDouble("BonusSpeed");
        this.killCount = nbt.getInt("KillCount");

        applyBonuses();
        applyAttackDamageScaling();

        if (!this.getWorld().isClient) {
            updateDescription(this);
        }
    }

    @Override
    public void applyCustomAttributes(MobAttributes attributes) {
        // Implemented from AttributeCarrier interface
    }

    @Override
    @Nullable
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty,
                                 SpawnReason spawnReason, @Nullable EntityData entityData) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData);

        // Give spawned pillagers some initial energy
        this.ELvl = 60.0 + random.nextDouble() * 40.0; // 60-100 energy
        applyAttackDamageScaling();

        return data;
    }
}