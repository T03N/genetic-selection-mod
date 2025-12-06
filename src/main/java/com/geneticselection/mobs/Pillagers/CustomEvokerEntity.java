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
import net.minecraft.entity.mob.EvokerEntity;
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

public class CustomEvokerEntity extends EvokerEntity implements AttributeCarrier {
    private MobAttributes mobAttributes;
    private double MaxHp;
    private double Speed;
    private double ELvl;
    private double MaxEnergy;
    private double BaseAttack;
    private double spellPower; // Unique to evokers - enhances spell damage
    private int generation = 0;
    private int tickAge = 0;
    private int breedingCooldown;
    private int ticksSinceLastBreeding = 0;

    // Evolution bonuses
    private double bonusAttack = 0.0;
    private double bonusHealth = 0.0;
    private double bonusSpeed = 0.0;
    private double bonusSpellPower = 0.0; // Unique evolution stat
    private int killCount = 0;

    private boolean wasRecentlyHit = false;

    // Base evoker spell damage (vanilla fangs do 6 damage)
    private static final double VANILLA_EVOKER_ATTACK = 6.0;
    private static final double BASE_SPELL_POWER = 1.0;

    public CustomEvokerEntity(EntityType<? extends EvokerEntity> entityType, World world) {
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
        this.BaseAttack = VANILLA_EVOKER_ATTACK;
        this.spellPower = BASE_SPELL_POWER;
        this.generation = 0;

        this.breedingCooldown = 10000 + (int)((1 - (ELvl / 100.0)) * 5000) + random.nextInt(2001);
        this.ticksSinceLastBreeding = 0;

        this.bonusAttack = 0.0;
        this.bonusHealth = 0.0;
        this.bonusSpeed = 0.0;
        this.bonusSpellPower = 0.0;
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

    private void applyBonuses() {
        if (this.mobAttributes == null) {
            initFromGlobalAttributes(this.getType());
        }

        double inherentMaxHp = this.mobAttributes.getMaxHealth();
        double inherentSpeed = this.mobAttributes.getMovementSpeed();

        double effectiveMaxHp = inherentMaxHp + this.bonusHealth;
        double effectiveSpeed = inherentSpeed * (1.0 + this.bonusSpeed);

        // Evokers get spell power bonus from generation (0.1 per gen, cap at +5)
        double generationSpellBonus = Math.min(this.generation * 0.1, 5.0);
        this.spellPower = BASE_SPELL_POWER + generationSpellBonus + this.bonusSpellPower;

        // Attack damage is enhanced by spell power
        double generationAttackBonus = Math.min(this.generation * 0.12, 6.0);
        double effectiveBaseAttack = (this.BaseAttack + generationAttackBonus + this.bonusAttack) * this.spellPower;

        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(effectiveMaxHp);
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(effectiveSpeed);
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

    private void applyAttackDamageScaling() {
        double generationAttackBonus = Math.min(this.generation * 0.12, 6.0);
        double generationSpellBonus = Math.min(this.generation * 0.1, 5.0);
        double currentSpellPower = BASE_SPELL_POWER + generationSpellBonus + this.bonusSpellPower;

        double baseAttack = (this.BaseAttack + generationAttackBonus + this.bonusAttack) * currentSpellPower;

        // Evokers have excellent energy scaling (80%-100%) - they're magical
        double energyRatio = (this.MaxEnergy > 0) ? (this.ELvl / this.MaxEnergy) : 1.0;
        double energyMultiplier = 0.8 + (energyRatio * 0.2);

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

    public double getSpellPower() {
        return this.spellPower;
    }

    private void updateDescription(CustomEvokerEntity ent) {
        if (ent.getWorld().isClient() || !ent.isAlive()) return;

        double currentMaxHp = ent.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
        double currentSpeed = ent.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        double currentAttack = ent.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        long remainingBreedCDTicks = Math.max(0, ent.breedingCooldown - ent.ticksSinceLastBreeding);

        DescriptionRenderer.setDescription(ent, Text.of(
                "EVOKER\n" +
                        "HP: " + String.format("%.1f", ent.getHealth()) + "/" + String.format("%.1f", currentMaxHp) +
                        " | Atk: " + String.format("%.2f", currentAttack) +
                        "\nSpd: " + String.format("%.3f", currentSpeed) +
                        " | Energy: " + String.format("%.1f", ent.ELvl) + "/" + String.format("%.1f", ent.MaxEnergy) +
                        "\nSpell Power: " + String.format("%.2f", ent.spellPower) + "x" +
                        " | Gen: " + ent.generation +
                        "\nKills: " + ent.killCount +
                        " | Breed CD: " + String.format("%.1f", remainingBreedCDTicks / 20.0) + "s"
        ));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);

        if(player.isSneaking() && itemStack.isEmpty()) {
            if (!this.getWorld().isClient) {
                updateDescription(this);
            }
            return ActionResult.SUCCESS;
        }

        // Feed with emerald blocks for massive energy boost
        if (itemStack.isOf(Items.EMERALD_BLOCK) && this.ELvl < this.MaxEnergy) {
            if (!player.getAbilities().creativeMode) {
                itemStack.decrement(1);
            }
            updateEnergyLevel(this.ELvl + 50.0);
            return ActionResult.SUCCESS;
        }

        // Feed with emeralds
        if (itemStack.isOf(Items.EMERALD) && this.ELvl < this.MaxEnergy) {
            if (!player.getAbilities().creativeMode) {
                itemStack.decrement(1);
            }
            updateEnergyLevel(this.ELvl + 30.0);
            return ActionResult.SUCCESS;
        }

        return super.interactMob(player, hand);
    }

    public void onSuccessfulKill() {
        if (!this.getWorld().isClient) {
            this.killCount++;

            // Substantial energy gain for evokers
            double energyGain = 10.0 + Math.random() * 10.0;
            updateEnergyLevel(this.ELvl + energyGain);

            this.heal(this.getMaxHealth() * 0.2f);

            // Highest evolution chance - evokers are elite
            float evolutionChance = 0.12f + (float)Math.min(this.killCount, 60) / 1200.0f;
            if (this.random.nextFloat() < evolutionChance) {
                int statChoice = this.random.nextInt(4); // 4 options including spell power
                switch(statChoice) {
                    case 0: bonusAttack += 0.12 + random.nextDouble() * 0.20; break;
                    case 1: bonusHealth += 1.0 + random.nextDouble() * 2.0; break;
                    case 2: bonusSpeed += 0.002 + random.nextDouble() * 0.003; break;
                    case 3: bonusSpellPower += 0.08 + random.nextDouble() * 0.12; break; // Unique!
                }

                this.bonusAttack = Math.min(this.bonusAttack, 10.0);
                this.bonusHealth = Math.min(this.bonusHealth, 50.0);
                this.bonusSpeed = Math.min(this.bonusSpeed, 0.20);
                this.bonusSpellPower = Math.min(this.bonusSpellPower, 3.0); // Max 4x spell power total

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

            if (wasRecentlyHit) {
                updateEnergyLevel(this.ELvl * 0.85); // Lose 15% - evokers are fragile
                wasRecentlyHit = false;
            }

            // Evokers drain energy moderately (less than vindicators, more than pillagers)
            if (Math.random() < 0.055) {
                updateEnergyLevel(this.ELvl - 0.09);
            }

            // Good health regen for evokers at high energy
            if (ELvl >= MaxEnergy * 0.90 && this.getHealth() < this.getMaxHealth()) {
                this.heal(0.25F);
            }

            // Breeding
            if (ELvl >= 90.0 && ticksSinceLastBreeding >= breedingCooldown) {
                double searchRadius = 48.0;

                List<CustomEvokerEntity> mateCandidates = this.getWorld().getEntitiesByClass(
                        CustomEvokerEntity.class,
                        this.getBoundingBox().expand(searchRadius),
                        candidate -> candidate != this &&
                                candidate.getEnergyLevel() >= 90.0 &&
                                candidate.ticksSinceLastBreeding >= candidate.breedingCooldown
                );

                if (!mateCandidates.isEmpty()) {
                    CustomEvokerEntity nearestMate = null;
                    double minDistanceSquared = Double.MAX_VALUE;

                    for (CustomEvokerEntity candidate : mateCandidates) {
                        double distSq = this.squaredDistanceTo(candidate);
                        if (distSq < minDistanceSquared) {
                            minDistanceSquared = distSq;
                            nearestMate = candidate;
                        }
                    }

                    if (nearestMate != null && minDistanceSquared < 16.0) {
                        if (this.getWorld() instanceof ServerWorld serverWorld) {
                            CustomEvokerEntity child = createChild(serverWorld, nearestMate);
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

            if (ELvl <= 0.0) {
                this.damage(this.getDamageSources().starve(), 2.5f);
            }

            // Speed and attack scaling
            double currentBaseSpeed = this.Speed * (1.0 + this.bonusSpeed);
            double energyMultiplier = (this.MaxEnergy > 0) ? (this.ELvl / this.MaxEnergy) : 1.0;
            double calculatedSpeed = currentBaseSpeed * energyMultiplier;
            double newSpeed = Math.max(calculatedSpeed, this.Speed * 0.6);

            this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(newSpeed);
            applyAttackDamageScaling();

            if (tickAge % 40 == 0) {
                updateDescription(this);
            }
        }
    }

    @Nullable
    public CustomEvokerEntity createChild(ServerWorld serverWorld, CustomEvokerEntity mate) {
        CustomEvokerEntity parent1 = this;
        CustomEvokerEntity parent2 = mate;
        CustomEvokerEntity child = ModEntities.CUSTOM_EVOKER.create(serverWorld);

        if (child == null) return null;

        if (parent1.mobAttributes == null) parent1.initFromGlobalAttributes(parent1.getType());
        if (parent2.mobAttributes == null) parent2.initFromGlobalAttributes(parent2.getType());

        double p1Speed = parent1.mobAttributes.getMovementSpeed();
        double p2Speed = parent2.mobAttributes.getMovementSpeed();
        double p1MaxHp = parent1.mobAttributes.getMaxHealth();
        double p2MaxHp = parent2.mobAttributes.getMaxHealth();

        double childBaseSpeed = ((p1Speed + p2Speed) / 2.0) * (0.96 + random.nextDouble() * 0.08);
        double childBaseMaxHp = ((p1MaxHp + p2MaxHp) / 2.0) * (0.96 + random.nextDouble() * 0.08);
        double childBaseEnergy = 100.0;

        child.mobAttributes = new MobAttributes(new EnumMap<>(Map.of(
                AttributeKey.MOVEMENT_SPEED, childBaseSpeed,
                AttributeKey.MAX_HEALTH, childBaseMaxHp,
                AttributeKey.ENERGY, childBaseEnergy
        )));

        child.MaxHp = childBaseMaxHp;
        child.Speed = childBaseSpeed;
        child.ELvl = 60.0; // Start with good energy
        child.MaxEnergy = 100.0;
        child.tickAge = 0;

        child.generation = Math.max(parent1.generation, parent2.generation) + 1;

        double inheritanceFactor = Math.max(0.55, Math.min(parent1.getEnergyLevel(), parent2.getEnergyLevel()) / 100.0);
        double randomFactor = 0.93 + random.nextDouble() * 0.14;

        double avgBonusAttack = (parent1.bonusAttack + parent2.bonusAttack) / 2.0;
        double avgBonusHealth = (parent1.bonusHealth + parent2.bonusHealth) / 2.0;
        double avgBonusSpeed = (parent1.bonusSpeed + parent2.bonusSpeed) / 2.0;
        double avgBonusSpellPower = (parent1.bonusSpellPower + parent2.bonusSpellPower) / 2.0;
        int avgKillCount = (parent1.killCount + parent2.killCount) / 2;

        child.bonusAttack = Math.max(0, avgBonusAttack * inheritanceFactor * randomFactor * 1.12); // 12% breeding bonus
        child.bonusHealth = Math.max(0, avgBonusHealth * inheritanceFactor * randomFactor);
        child.bonusSpeed = Math.max(0, avgBonusSpeed * inheritanceFactor * randomFactor);
        child.bonusSpellPower = Math.max(0, avgBonusSpellPower * inheritanceFactor * randomFactor * 1.15); // 15% spell power bonus!
        child.killCount = (int)(avgKillCount * inheritanceFactor * 0.7);

        child.BaseAttack = (parent1.BaseAttack + parent2.BaseAttack) / 2.0 * (0.98 + random.nextDouble() * 0.04);

        double inverseFactor = (inheritanceFactor > 0.1) ? (1 / inheritanceFactor) : 10.0;
        int childBreedingCooldown = (int) (((parent1.breedingCooldown + parent2.breedingCooldown) / 2.0)
                * inverseFactor * (0.9 + random.nextDouble() * 0.2));
        child.breedingCooldown = Math.max(5000, childBreedingCooldown);
        child.ticksSinceLastBreeding = 0;

        child.applyBonuses();

        parent1.updateEnergyLevel(parent1.getEnergyLevel() * 0.50); // Evokers lose more energy
        parent2.updateEnergyLevel(parent2.getEnergyLevel() * 0.50);

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
        nbt.putDouble("SpellPower", this.spellPower);
        nbt.putInt("Generation", this.generation);
        nbt.putInt("TickAge", this.tickAge);
        nbt.putInt("BreedingCooldown", this.breedingCooldown);
        nbt.putInt("TicksSinceLastBreeding", this.ticksSinceLastBreeding);
        nbt.putDouble("BonusAttack", this.bonusAttack);
        nbt.putDouble("BonusHealth", this.bonusHealth);
        nbt.putDouble("BonusSpeed", this.bonusSpeed);
        nbt.putDouble("BonusSpellPower", this.bonusSpellPower);
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
        this.BaseAttack = nbt.contains("BaseAttack") ? nbt.getDouble("BaseAttack") : VANILLA_EVOKER_ATTACK;
        this.spellPower = nbt.contains("SpellPower") ? nbt.getDouble("SpellPower") : BASE_SPELL_POWER;
        this.generation = nbt.getInt("Generation");
        this.tickAge = nbt.getInt("TickAge");
        this.breedingCooldown = nbt.getInt("BreedingCooldown");
        this.ticksSinceLastBreeding = nbt.getInt("TicksSinceLastBreeding");
        this.bonusAttack = nbt.getDouble("BonusAttack");
        this.bonusHealth = nbt.getDouble("BonusHealth");
        this.bonusSpeed = nbt.getDouble("BonusSpeed");
        this.bonusSpellPower = nbt.getDouble("BonusSpellPower");
        this.killCount = nbt.getInt("KillCount");

        applyBonuses();
        applyAttackDamageScaling();

        if (!this.getWorld().isClient) {
            updateDescription(this);
        }
    }

    @Override
    public void applyCustomAttributes(MobAttributes attributes) {
    }

    @Override
    @Nullable
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty,
                                 SpawnReason spawnReason, @Nullable EntityData entityData) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData);

        this.ELvl = 80.0 + random.nextDouble() * 20.0; // 80-100 energy
        applyAttackDamageScaling();

        return data;
    }
}
