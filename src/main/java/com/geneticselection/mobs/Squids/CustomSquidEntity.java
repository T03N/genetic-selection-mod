package com.geneticselection.mobs.Squids;

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
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
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

public class CustomSquidEntity extends SquidEntity implements AttributeCarrier {
    private MobAttributes mobAttributes;
    private double MaxHp;
    private double Speed;
    private double ELvl;
    private double MaxEnergy;
    private int generation = 0;
    private int tickAge = 0;
    private int breedingCooldown;
    private int ticksSinceLastBreeding = 0;

    // Squid-specific attributes
    private double inkProduction = 1.0; // Multiplier for ink sac drops
    private int inkRegenTicks = 0;
    private static final int INK_REGEN_COOLDOWN = 600; // 30 seconds

    // Evolution bonuses
    private double bonusHealth = 0.0;
    private double bonusSpeed = 0.0;
    private double bonusInkProduction = 0.0;

    private boolean wasRecentlyHit = false;
    private boolean isInWater = false;

    public CustomSquidEntity(EntityType<? extends SquidEntity> entityType, World world) {
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
        this.generation = 0;
        this.inkProduction = 1.0;

        this.breedingCooldown = 4000 + (int)((1 - (ELvl / 100.0)) * 2000) + random.nextInt(1001);
        this.ticksSinceLastBreeding = 0;

        this.bonusHealth = 0.0;
        this.bonusSpeed = 0.0;
        this.bonusInkProduction = 0.0;

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

        // Generation bonus for health and ink production
        double generationHealthBonus = Math.min(this.generation * 0.5, 10.0);
        double generationInkBonus = Math.min(this.generation * 0.05, 2.0);

        this.inkProduction = 1.0 + generationInkBonus + this.bonusInkProduction;

        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(effectiveMaxHp + generationHealthBonus);
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(effectiveSpeed);

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

    public void updateEnergyLevel(double newEnergyLevel) {
        this.ELvl = Math.max(0.0, Math.min(this.MaxEnergy, newEnergyLevel));

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

    public double getInkProduction() {
        return this.inkProduction;
    }

    private void updateDescription(CustomSquidEntity ent) {
        if (ent.getWorld().isClient() || !ent.isAlive()) return;

        double currentMaxHp = ent.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
        double currentSpeed = ent.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        long remainingBreedCDTicks = Math.max(0, ent.breedingCooldown - ent.ticksSinceLastBreeding);
        int inkRegenRemaining = Math.max(0, INK_REGEN_COOLDOWN - ent.inkRegenTicks);

        DescriptionRenderer.setDescription(ent, Text.of(
                "SQUID\n" +
                        "HP: " + String.format("%.1f", ent.getHealth()) + "/" + String.format("%.1f", currentMaxHp) +
                        " | Spd: " + String.format("%.3f", currentSpeed) +
                        "\nEnergy: " + String.format("%.1f", ent.ELvl) + "/" + String.format("%.1f", ent.MaxEnergy) +
                        " | Ink: " + String.format("%.2f", ent.inkProduction) + "x" +
                        "\nGen: " + ent.generation +
                        " | Age: " + ent.tickAge +
                        "\nBreed CD: " + String.format("%.1f", remainingBreedCDTicks / 20.0) + "s" +
                        (inkRegenRemaining > 0 ? "\nInk Regen: " + String.format("%.1f", inkRegenRemaining / 20.0) + "s" : "\n✓ Ink Ready")
        ));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);

        // Check stats with sneak + empty hand
        if(player.isSneaking() && itemStack.isEmpty()) {
            if (!this.getWorld().isClient) {
                updateDescription(this);
            }
            return ActionResult.SUCCESS;
        }

        // Feed with kelp or seagrass for energy
        if ((itemStack.isOf(Items.KELP) || itemStack.isOf(Items.SEAGRASS)) && this.ELvl < this.MaxEnergy) {
            if (!player.getAbilities().creativeMode) {
                itemStack.decrement(1);
            }
            updateEnergyLevel(this.ELvl + 20.0);

            // Spawn particles
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(
                        ParticleTypes.BUBBLE,
                        this.getX(), this.getY() + 0.5, this.getZ(),
                        10, 0.3, 0.3, 0.3, 0.02
                );
            }
            return ActionResult.SUCCESS;
        }

        // Collect ink sac if available
        if (itemStack.isEmpty() && this.inkRegenTicks >= INK_REGEN_COOLDOWN && this.ELvl >= 30.0) {
            if (!this.getWorld().isClient) {
                int inkAmount = (int)Math.ceil(this.inkProduction);
                this.dropStack(new ItemStack(Items.INK_SAC, inkAmount));
                this.inkRegenTicks = 0;
                updateEnergyLevel(this.ELvl - 30.0);

                // Spawn ink particles
                if (this.getWorld() instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(
                            ParticleTypes.SQUID_INK,
                            this.getX(), this.getY() + 0.5, this.getZ(),
                            20, 0.3, 0.3, 0.3, 0.05
                    );
                }
            }
            return ActionResult.SUCCESS;
        }

        return super.interactMob(player, hand);
    }

    @Override
    protected void applyDamage(DamageSource source, float amount) {
        super.applyDamage(source, amount);
        if (!this.getWorld().isClient && this.isAlive()) {
            this.wasRecentlyHit = true;

            // Release ink cloud when hit (vanilla behavior)
            if (this.inkRegenTicks >= INK_REGEN_COOLDOWN / 2) {
                this.inkRegenTicks = 0; // Reset ink cooldown
            }

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

            // Check if in water
            this.isInWater = this.isSubmergedInWater();

            // Energy loss from being hit
            if (wasRecentlyHit) {
                updateEnergyLevel(this.ELvl * 0.92); // Lose 8%
                wasRecentlyHit = false;
            }

            // Energy management based on water
            if (isInWater) {
                // Gain energy while in water
                if (Math.random() < 0.1 && ELvl < MaxEnergy) {
                    updateEnergyLevel(this.ELvl + 0.15);
                }
            } else {
                // Lose energy rapidly out of water
                if (Math.random() < 0.3) {
                    updateEnergyLevel(this.ELvl - 0.5);
                }
            }

            // Health regeneration at high energy
            if (ELvl >= MaxEnergy * 0.95 && this.getHealth() < this.getMaxHealth() && isInWater) {
                this.heal(0.10F);
            }

            // Ink regeneration
            if (inkRegenTicks < INK_REGEN_COOLDOWN) {
                inkRegenTicks++;
            }

            // Automated breeding in water
            if (ELvl >= 80.0 && ticksSinceLastBreeding >= breedingCooldown && isInWater) {
                double searchRadius = 32.0;

                List<CustomSquidEntity> mateCandidates = this.getWorld().getEntitiesByClass(
                        CustomSquidEntity.class,
                        this.getBoundingBox().expand(searchRadius),
                        candidate -> candidate != this &&
                                candidate.getEnergyLevel() >= 80.0 &&
                                candidate.ticksSinceLastBreeding >= candidate.breedingCooldown &&
                                candidate.isSubmergedInWater()
                );

                if (!mateCandidates.isEmpty()) {
                    CustomSquidEntity nearestMate = null;
                    double minDistanceSquared = Double.MAX_VALUE;

                    for (CustomSquidEntity candidate : mateCandidates) {
                        double distSq = this.squaredDistanceTo(candidate);
                        if (distSq < minDistanceSquared) {
                            minDistanceSquared = distSq;
                            nearestMate = candidate;
                        }
                    }

                    if (nearestMate != null && minDistanceSquared < 9.0) { // Within 3 blocks
                        if (this.getWorld() instanceof ServerWorld serverWorld) {
                            CustomSquidEntity child = createChild(serverWorld, nearestMate);
                            if (child != null) {
                                child.refreshPositionAndAngles(
                                        this.getX(), this.getY(), this.getZ(),
                                        this.getYaw(), 0.0F
                                );
                                serverWorld.spawnEntity(child);

                                // Spawn breeding particles
                                serverWorld.spawnParticles(
                                        ParticleTypes.HEART,
                                        this.getX(), this.getY() + 1.0, this.getZ(),
                                        5, 0.5, 0.5, 0.5, 0.0
                                );

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

            // Damage out of water (vanilla squid behavior)
            if (!isInWater && ELvl <= 0.0) {
                this.damage(this.getDamageSources().dryOut(), 1.0f);
            }

            // Speed scaling with energy (swim speed)
            double currentBaseSpeed = this.Speed * (1.0 + this.bonusSpeed);
            double energyMultiplier = (this.MaxEnergy > 0) ? (this.ELvl / this.MaxEnergy) : 1.0;
            double calculatedSpeed = currentBaseSpeed * energyMultiplier;
            double newSpeed = Math.max(calculatedSpeed, this.Speed * 0.6); // Min 60% speed

            this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(newSpeed);

            // Periodic description update
            if (tickAge % 40 == 0) {
                updateDescription(this);
            }
        }
    }

    @Nullable
    public CustomSquidEntity createChild(ServerWorld serverWorld, CustomSquidEntity mate) {
        CustomSquidEntity parent1 = this;
        CustomSquidEntity parent2 = mate;
        CustomSquidEntity child = ModEntities.CUSTOM_SQUID.create(serverWorld);

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
        child.ELvl = 60.0; // Baby squids start with moderate energy
        child.MaxEnergy = 100.0;
        child.tickAge = 0;

        child.generation = Math.max(parent1.generation, parent2.generation) + 1;

        double inheritanceFactor = Math.max(0.4, Math.min(parent1.getEnergyLevel(), parent2.getEnergyLevel()) / 100.0);
        double randomFactor = 0.90 + random.nextDouble() * 0.2;

        double avgBonusHealth = (parent1.bonusHealth + parent2.bonusHealth) / 2.0;
        double avgBonusSpeed = (parent1.bonusSpeed + parent2.bonusSpeed) / 2.0;
        double avgBonusInkProduction = (parent1.bonusInkProduction + parent2.bonusInkProduction) / 2.0;

        child.bonusHealth = Math.max(0, avgBonusHealth * inheritanceFactor * randomFactor);
        child.bonusSpeed = Math.max(0, avgBonusSpeed * inheritanceFactor * randomFactor);
        child.bonusInkProduction = Math.max(0, avgBonusInkProduction * inheritanceFactor * randomFactor * 1.05); // 5% breeding bonus

        double inverseFactor = (inheritanceFactor > 0.1) ? (1 / inheritanceFactor) : 10.0;
        int childBreedingCooldown = (int) (((parent1.breedingCooldown + parent2.breedingCooldown) / 2.0)
                * inverseFactor * (0.9 + random.nextDouble() * 0.2));
        child.breedingCooldown = Math.max(2000, childBreedingCooldown);
        child.ticksSinceLastBreeding = 0;

        child.applyBonuses();

        parent1.updateEnergyLevel(parent1.getEnergyLevel() * 0.65);
        parent2.updateEnergyLevel(parent2.getEnergyLevel() * 0.65);

        if (!serverWorld.isClient) {
            updateDescription(child);
            updateDescription(parent1);
            updateDescription(parent2);
        }

        return child;
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);

        if (!this.getWorld().isClient) {
            // Drop ink sacs based on ink production
            int baseInkAmount = 1 + random.nextInt(3); // 1-3 vanilla
            int inkAmount = (int)(baseInkAmount * this.inkProduction);

            for (int i = 0; i < inkAmount; i++) {
                this.dropItem(Items.INK_SAC);
            }
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putDouble("ELvl", this.ELvl);
        nbt.putDouble("MaxEnergy", this.MaxEnergy);
        nbt.putInt("Generation", this.generation);
        nbt.putInt("TickAge", this.tickAge);
        nbt.putInt("BreedingCooldown", this.breedingCooldown);
        nbt.putInt("TicksSinceLastBreeding", this.ticksSinceLastBreeding);
        nbt.putDouble("InkProduction", this.inkProduction);
        nbt.putInt("InkRegenTicks", this.inkRegenTicks);
        nbt.putDouble("BonusHealth", this.bonusHealth);
        nbt.putDouble("BonusSpeed", this.bonusSpeed);
        nbt.putDouble("BonusInkProduction", this.bonusInkProduction);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        if (this.mobAttributes == null) {
            initFromGlobalAttributes(this.getType());
        }

        super.readCustomDataFromNbt(nbt);

        this.ELvl = nbt.contains("ELvl") ? nbt.getDouble("ELvl") : this.mobAttributes.getEnergyLvl();
        this.MaxEnergy = nbt.getDouble("MaxEnergy");
        this.generation = nbt.getInt("Generation");
        this.tickAge = nbt.getInt("TickAge");
        this.breedingCooldown = nbt.getInt("BreedingCooldown");
        this.ticksSinceLastBreeding = nbt.getInt("TicksSinceLastBreeding");
        this.inkProduction = nbt.contains("InkProduction") ? nbt.getDouble("InkProduction") : 1.0;
        this.inkRegenTicks = nbt.getInt("InkRegenTicks");
        this.bonusHealth = nbt.getDouble("BonusHealth");
        this.bonusSpeed = nbt.getDouble("BonusSpeed");
        this.bonusInkProduction = nbt.getDouble("BonusInkProduction");

        applyBonuses();

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

        this.ELvl = 70.0 + random.nextDouble() * 30.0; // 70-100 energy

        return data;
    }

}