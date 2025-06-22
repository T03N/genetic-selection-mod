package com.geneticselection.mobs.Axolotl;

import com.geneticselection.attributes.AttributeCarrier;
import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.utils.DescriptionRenderer;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.passive.FishEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static com.geneticselection.genetics.ChildInheritance.applyAttributes;
import static com.geneticselection.genetics.ChildInheritance.influenceGlobalAttributes;
import static com.geneticselection.genetics.ChildInheritance.inheritAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;

public class CustomAxolotlEntity extends AxolotlEntity implements AttributeCarrier {
    private MobAttributes mobAttributes;
    private double MaxHp;
    private double Speed;
    private double ELvl;
    private double MaxEnergy;
    private int tickAge = 0;
    private int ticksSinceLastBreeding = 0;
    private int breedingCooldown;

    private int panicTicks = 0;
    private static final int PANIC_DURATION = 100;
    private static final double PANIC_SPEED_MULTIPLIER = 1.5;
    private boolean wasRecentlyHit = false;

    private static final int LIFESPAN = 28000; // 2 minecraft days
    private static final int MAX_AGE = 36000;

    // Evolution Attributes
    private double bonusAttack = 0.0;
    private double bonusHealth = 0.0;
    private double bonusSpeed = 0.0;
    private int killCount = 0;

    private static final TrackedData<Float> MAX_HP = DataTracker.registerData(CustomAxolotlEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> ELVL = DataTracker.registerData(CustomAxolotlEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> MAX_ENERGY = DataTracker.registerData(CustomAxolotlEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> TICK_AGE = DataTracker.registerData(CustomAxolotlEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public CustomAxolotlEntity(EntityType<? extends AxolotlEntity> entityType, World world) {
        super(entityType, world);
        if (this.mobAttributes == null) {
            initFromGlobalAttributes(entityType);
        }
        this.MaxEnergy = 10.0;
        this.ELvl = this.mobAttributes.getEnergyLvl();
        this.breedingCooldown = 3000 + random.nextInt(3000);
        applyBonuses();
        if (!world.isClient) {
            this.dataTracker.set(MAX_HP, (float)this.mobAttributes.getMaxHealth());
            this.dataTracker.set(ELVL, (float)this.mobAttributes.getEnergyLvl());
            this.dataTracker.set(MAX_ENERGY, (float)this.MaxEnergy);
            this.dataTracker.set(TICK_AGE, 0);
        }
    }

    private void initFromGlobalAttributes(EntityType<?> entityType) {
        this.mobAttributes = GlobalAttributesManager.getAttributes(entityType);
        this.MaxHp = this.mobAttributes.getMaxHealth();
        this.Speed = this.mobAttributes.getMovementSpeed();
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(1, new HuntFishGoal(this, 1.2, 80));
        this.goalSelector.add(2, new FleeEntityGoal<>(this, PlayerEntity.class, 8.0F, 1.6, 1.4, (livingEntity) -> true));
        this.goalSelector.add(3, new MeleeAttackGoal(this, 1.2, true));
    }

    private void applyBonuses() {
        if (this.mobAttributes == null) {
            initFromGlobalAttributes(this.getType());
        }

        double inherentMaxHp = this.mobAttributes.getMaxHealth();
        double inherentSpeed = this.mobAttributes.getMovementSpeed();
        double inherentAttack = 2.0; // Axolotls have a base attack damage

        double effectiveMaxHp = Math.max(1.0, inherentMaxHp + this.bonusHealth);
        double speedMultiplier = Math.max(0.1, 1.0 + this.bonusSpeed);
        double effectiveSpeed = inherentSpeed * speedMultiplier;
        double effectiveAttack = inherentAttack + this.bonusAttack;

        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(effectiveMaxHp);
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(effectiveSpeed);
        this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(effectiveAttack);

        if(this.getHealth() > this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
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
    public int getBreedingCooldown() { return this.breedingCooldown; }

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

    public void onSuccessfulKill(LivingEntity killedEntity) {
        if (!this.getWorld().isClient) {
            this.killCount++;
            double energyGain = Math.min(15.0, killedEntity.getMaxHealth() * 2.0);
            updateEnergyLevel(this.ELvl + energyGain);
            this.heal(this.getMaxHealth() * 0.25f);

            float evolutionChance = 0.08f + (float)Math.min(this.killCount, 150) / 2500.0f;
            if (this.random.nextFloat() < evolutionChance) {
                int statChoice = this.random.nextInt(3);
                switch(statChoice) {
                    case 0: bonusAttack += 0.06 + random.nextDouble() * 0.1; break;
                    case 1: bonusHealth += 0.1 + random.nextDouble() * 0.4; break;
                    case 2: bonusSpeed += 0.002 + random.nextDouble() * 0.003; break;
                }
                this.bonusAttack = Math.min(this.bonusAttack, 5.0);
                this.bonusHealth = Math.min(this.bonusHealth, 12.0);
                this.bonusSpeed = Math.min(this.bonusSpeed, 0.25);

                applyBonuses();
            }
        }
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

            LivingEntity currentTarget = this.getTarget();
            if (currentTarget != null && currentTarget.isDead()) {
                if (currentTarget instanceof FishEntity) {
                    onSuccessfulKill(currentTarget);
                }
                this.setTarget(null);
            }

            if (tickAge >= MAX_AGE) {
                this.damage(this.getDamageSources().starve(), this.getHealth() + 1.0f);
                return;
            }

            if (tickAge <= 12000) { MaxEnergy = 10.0 + 90.0 * (tickAge / 12000.0); }
            else if (tickAge <= LIFESPAN) { MaxEnergy = 100.0; }
            else { MaxEnergy = Math.max(20.0, 100.0 - (tickAge - LIFESPAN) / 240.0); }
            tickAge++;

            if (tickAge >= 12000 && this.isBaby()) {
                this.growUp(this.getBreedingAge() * -1, false);
            }

            if (panicTicks > 0) {
                panicTicks--;
            }

            if (wasRecentlyHit) {
                updateEnergyLevel(this.ELvl * 0.8);
                wasRecentlyHit = false;
            }

            boolean inWater = this.isSubmergedInWater();
            if (inWater) {
                updateEnergyLevel(this.ELvl + 0.1);
            } else {
                updateEnergyLevel(this.ELvl - 0.1);
            }

            if (ELvl >= MaxEnergy * 0.95 && this.getHealth() < this.getMaxHealth()) {
                this.heal(0.25F);
            }

            if (canAutoBreed()) {
                findMateAndBreed();
            }

            if (!this.isInLove()) {
                ticksSinceLastBreeding++;
            }

            if (ELvl <= 0.0) {
                this.damage(this.getDamageSources().starve(), this.getHealth() + 1.0f);
            } else {
                double speedMultiplier = wasRecentlyHit ? PANIC_SPEED_MULTIPLIER : 1.0;
                double energyFactor = Math.max(0.5, this.ELvl / this.MaxEnergy);
                this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed * (1.0 + this.bonusSpeed) * energyFactor * speedMultiplier);
            }

            if (tickAge % 40 == 0) updateDescription(this);
        }
        var maxHealthAttr = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) maxHealthAttr.setBaseValue(this.getMaxHpTracked());
    }

    private boolean canAutoBreed() {
        return !this.isBaby() && this.ELvl >= this.MaxEnergy * 0.9 && ticksSinceLastBreeding >= breedingCooldown && !this.isInLove();
    }

    private void findMateAndBreed() {
        double searchRadius = 24.0;
        List<CustomAxolotlEntity> mateCandidates = this.getWorld().getEntitiesByClass(
            CustomAxolotlEntity.class, this.getBoundingBox().expand(searchRadius),
            candidate -> candidate != this && candidate.canAutoBreed());

        if (!mateCandidates.isEmpty()) {
            CustomAxolotlEntity nearestMate = mateCandidates.get(0); // Simple selection
            this.getNavigation().startMovingTo(nearestMate, 1.2);
            if (this.squaredDistanceTo(nearestMate) < 9.0) {
                this.setLoveTicks(600);
                nearestMate.setLoveTicks(600);
                this.ticksSinceLastBreeding = 0;
                nearestMate.ticksSinceLastBreeding = 0;
            }
        }
    }

    @Override
    public CustomAxolotlEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof CustomAxolotlEntity parent2)) {
            return null;
        }

        CustomAxolotlEntity child = ModEntities.CUSTOM_AXOLOTL.create(serverWorld);
        if (child == null) return null;

        CustomAxolotlEntity parent1 = this;

        // Inherit Base Attributes
        MobAttributes childBaseAttributes = inheritAttributes(parent1.mobAttributes, parent2.mobAttributes);
        child.mobAttributes = childBaseAttributes;
        child.Speed = childBaseAttributes.getMovementSpeed();
        child.MaxHp = childBaseAttributes.getMaxHealth();

        // Inherit Bonus Stats
        double inheritanceFactor = Math.max(0.2, Math.min(parent1.getEnergyLevel(), parent2.getEnergyLevel()) / 100.0);
        double randomFactor = 0.9 + random.nextDouble() * 0.2;
        child.bonusAttack = Math.max(0, ((parent1.bonusAttack + parent2.bonusAttack) / 2.0) * inheritanceFactor * randomFactor);
        child.bonusHealth = Math.max(0, ((parent1.bonusHealth + parent2.bonusHealth) / 2.0) * inheritanceFactor * randomFactor);
        child.bonusSpeed = Math.max(0, ((parent1.bonusSpeed + parent2.bonusSpeed) / 2.0) * inheritanceFactor * randomFactor);
        child.killCount = (int)(((parent1.killCount + parent2.killCount) / 2.0) * inheritanceFactor);

        // Apply final attributes
        child.applyBonuses();

        // Reduce parent energy
        parent1.updateEnergyLevel(parent1.getEnergyLevel() * 0.5);
        parent2.updateEnergyLevel(parent2.getEnergyLevel() * 0.5);
        this.resetLoveTicks();
        parent2.resetLoveTicks();

        influenceGlobalAttributes(child.getType());

        return child;
    }

    @Override
    protected void applyDamage(DamageSource source, float amount) {
        super.applyDamage(source, amount);
        wasRecentlyHit = true;
        panicTicks = PANIC_DURATION;
    }

    @Override
    public void applyCustomAttributes(MobAttributes attributes) {
    }

    static class HuntFishGoal extends Goal {
        private final CustomAxolotlEntity axolotl;
        private final double speed;
        private final int energyThreshold;
        private FishEntity targetFish;

        public HuntFishGoal(CustomAxolotlEntity axolotl, double speed, int energyThreshold) {
            this.axolotl = axolotl;
            this.speed = speed;
            this.energyThreshold = energyThreshold;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (this.axolotl.getEnergyLevel() < (this.axolotl.MaxEnergy * (this.energyThreshold / 100.0))) {
                List<FishEntity> list = this.axolotl.getWorld().getEntitiesByClass(FishEntity.class, this.axolotl.getBoundingBox().expand(12.0D), (fish) -> true);
                if (!list.isEmpty()) {
                    this.targetFish = list.get(this.axolotl.random.nextInt(list.size()));
                    return true;
                }
            }
            return false;
        }

        @Override
        public void start() {
            if (this.targetFish != null) {
                this.axolotl.getNavigation().startMovingTo(this.targetFish, this.speed);
                this.axolotl.setTarget(this.targetFish);
            }
        }

        @Override
        public boolean shouldContinue() {
            return this.targetFish != null && this.targetFish.isAlive() && this.axolotl.getEnergyLevel() < this.axolotl.MaxEnergy * 0.95 && !this.axolotl.getNavigation().isIdle();
        }

        @Override
        public void stop() {
            this.axolotl.setTarget(null);
            this.targetFish = null;
        }

        @Override
        public void tick() {
            if (this.targetFish != null) {
                this.axolotl.getLookControl().lookAt(this.targetFish);
                if (this.axolotl.getBoundingBox().intersects(this.targetFish.getBoundingBox())) {
                    this.axolotl.tryAttack(this.targetFish);
                    this.targetFish.damage(this.axolotl.getDamageSources().mobAttack(this.axolotl), (float)this.axolotl.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
                    if (this.targetFish.isDead()) {
                        this.axolotl.onSuccessfulKill(this.targetFish);
                        this.stop();
                    }
                }
            }
        }
    }
}

