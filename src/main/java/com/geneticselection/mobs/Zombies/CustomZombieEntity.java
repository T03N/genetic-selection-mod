package com.geneticselection.mobs.Zombies;

import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * Custom Zombie Entity with genetic attributes and progressive difficulty scaling.
 *
 * Zombies spawn stronger over time based on world age:
 * - Strength scales from 1.0x to 2.5x over 10 Minecraft days (240,000 ticks)
 * - Affects health, attack damage, energy, and loot drops
 * - Speed scales more slowly (using square root) for balance
 * - Individual zombies retain their spawn-time difficulty permanently
 */
public class CustomZombieEntity extends ZombieEntity {

    // Constants
    private static final int LIFESPAN = 50000;
    private static final double MAX_ENERGY_CAP = 100.0;
    private static final int ADULT_AGE_TICKS = 6000;

    // Genetic variance constants
    private static final double SPEED_VARIANCE_MIN = 0.95;
    private static final double SPEED_VARIANCE_RANGE = 0.15;
    private static final double HEALTH_VARIANCE_MIN = 0.95;
    private static final double HEALTH_VARIANCE_RANGE = 0.15;
    private static final double ENERGY_VARIANCE_MIN = 0.9;
    private static final double ENERGY_VARIANCE_RANGE = 0.2;
    private static final double DAMAGE_VARIANCE_MIN = 0.95;
    private static final double DAMAGE_VARIANCE_RANGE = 0.15;

    // Energy change constants
    private static final double SUNLIGHT_ENERGY_LOSS = 0.2;
    private static final double PASSIVE_ENERGY_LOSS_CHANCE = 0.3;
    private static final double PASSIVE_ENERGY_LOSS_MIN = 0.05;
    private static final double PASSIVE_ENERGY_LOSS_MAX = 0.2;
    private static final double NIGHT_ENERGY_GAIN_CHANCE = 0.4;
    private static final double NIGHT_ENERGY_GAIN_MIN = 0.1;
    private static final double NIGHT_ENERGY_GAIN_MAX = 0.5;
    private static final double ENERGY_LOSS_ON_HIT = 0.85;
    private static final float HEALTH_REGEN_RATE = 0.3F;

    // Drop constants
    private static final double MIN_ROTTEN_FLESH_DROP = 0.0;
    private static final double MAX_ROTTEN_FLESH_DROP = 3.0;

    // Progressive difficulty constants
    private static final long DIFFICULTY_SCALE_INTERVAL = 24000; // 1 Minecraft day
    private static final double MAX_DIFFICULTY_MULTIPLIER = 2.5; // Zombies can be up to 2.5x stronger
    private static final long MAX_DIFFICULTY_TIME = 240000; // Reaches max after 10 Minecraft days

    // Attributes
    private MobAttributes mobAttributes;
    private double maxHp;
    private double energyLevel;
    private double maxEnergy;
    private double speed;
    private double attackDamage;
    private double minRottenFlesh;
    private double maxRottenFlesh;

    // Timers and state
    private int tickAge = 0;
    private boolean wasRecentlyHit = false;
    private double difficultyMultiplier = 1.0;

    public CustomZombieEntity(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);

        // Calculate difficulty multiplier based on world time
        this.difficultyMultiplier = calculateDifficultyMultiplier(world);

        initializeAttributes(entityType);
        applyAttributesToEntity();

        if (!this.getWorld().isClient) {
            updateDescription();
        }
    }

    private double calculateDifficultyMultiplier(World world) {
        long worldTime = world.getTimeOfDay();

        // Calculate how many difficulty intervals have passed
        double progress = Math.min(1.0, (double) worldTime / MAX_DIFFICULTY_TIME);

        // Linear scaling from 1.0 to MAX_DIFFICULTY_MULTIPLIER
        return 1.0 + (progress * (MAX_DIFFICULTY_MULTIPLIER - 1.0));
    }

    private void initializeAttributes(EntityType<? extends ZombieEntity> entityType) {
        if (this.mobAttributes == null) {
            MobAttributes globalAttributes = GlobalAttributesManager.getAttributes(entityType);

            double speed = calculateVariance(globalAttributes.getMovementSpeed(),
                    SPEED_VARIANCE_MIN, SPEED_VARIANCE_RANGE);
            double health = calculateVariance(globalAttributes.getMaxHealth(),
                    HEALTH_VARIANCE_MIN, HEALTH_VARIANCE_RANGE);
            double energy = calculateVariance(globalAttributes.getEnergyLvl(),
                    ENERGY_VARIANCE_MIN, ENERGY_VARIANCE_RANGE);
            double damage = calculateVariance(globalAttributes.getAttackDamage().orElse(3.0),
                    DAMAGE_VARIANCE_MIN, DAMAGE_VARIANCE_RANGE);

            this.mobAttributes = new MobAttributes(
                    speed, health, energy,
                    Optional.empty(), Optional.empty(),
                    Optional.of(damage), Optional.empty(), Optional.empty(), Optional.empty()
            );
            this.tickAge = 0;
        }
    }

    private double calculateVariance(double baseValue, double min, double range) {
        return baseValue * (min + Math.random() * range);
    }

    private void applyAttributesToEntity() {
        // Apply difficulty multiplier to base stats
        this.maxHp = this.mobAttributes.getMaxHealth() * difficultyMultiplier;
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.maxHp);
        this.setHealth((float) this.maxHp); // Spawn with full health

        this.speed = this.mobAttributes.getMovementSpeed() * Math.sqrt(difficultyMultiplier); // Slower scaling for speed
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.speed);

        this.energyLevel = this.mobAttributes.getEnergyLvl() * difficultyMultiplier;

        this.attackDamage = this.mobAttributes.getAttackDamage().orElse(3.0) * difficultyMultiplier;
        this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(this.attackDamage);

        this.minRottenFlesh = MIN_ROTTEN_FLESH_DROP;
        this.maxRottenFlesh = MAX_ROTTEN_FLESH_DROP * difficultyMultiplier; // More drops from stronger zombies
    }

    private void updateDescription() {
        String description = String.format(
                "Zombie Attributes\n" +
                        "Max Hp: %.3f/%.3f\n" +
                        "Speed: %.3f\n" +
                        "Energy: %.3f\n" +
                        "Attack Damage: %.3f\n" +
                        "Difficulty: %.2fx\n" +
                        "Age: %d",
                getHealth(), maxHp, speed, energyLevel,
                attackDamage, difficultyMultiplier, tickAge
        );

        DescriptionRenderer.setDescription(this, Text.of(description));
    }

    // Energy management
    public double getEnergyLevel() {
        return this.energyLevel;
    }

    public void updateEnergyLevel(double newEnergyLevel) {
        this.energyLevel = Math.max(0.0, Math.min(maxEnergy, newEnergyLevel));

        if (!this.getWorld().isClient) {
            updateDescription();
        }
    }

    // Setters
    public void setMinRottenFlesh(double minRottenFlesh) {
        this.minRottenFlesh = minRottenFlesh;
    }

    public void setMaxRottenFlesh(double maxRottenFlesh) {
        this.maxRottenFlesh = maxRottenFlesh;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);

        // Example: Feed zombie rotten flesh to restore energy
        if (itemStack.isOf(Items.ROTTEN_FLESH) && !this.getWorld().isClient) {
            if (energyLevel < maxEnergy) {
                updateEnergyLevel(energyLevel + 15.0);
                player.sendMessage(
                        Text.of(String.format("Zombie energy restored! Current: %.1f", energyLevel)),
                        true
                );

                if (!player.isCreative()) {
                    itemStack.decrement(1);
                }
                return ActionResult.SUCCESS;
            } else {
                player.sendMessage(Text.of("Zombie is at maximum energy."), true);
                return ActionResult.PASS;
            }
        }

        return super.interactMob(player, hand);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean damaged = super.damage(source, amount);

        if (damaged && !this.getWorld().isClient) {
            wasRecentlyHit = true;
        }

        return damaged;
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        super.dropLoot(source, causedByPlayer);

        if (!this.getWorld().isClient) {
            dropRottenFlesh();
        }
    }

    private void dropRottenFlesh() {
        int fleshAmount = (int) (minRottenFlesh +
                Math.random() * (maxRottenFlesh - minRottenFlesh + 1));

        if (fleshAmount > 0) {
            this.dropStack(new ItemStack(Items.ROTTEN_FLESH, fleshAmount));
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient) {
            updateMaxEnergy();
            updateAge();
            handleHitEnergyLoss();
            handleSunlightEnergyLoss();
            handleEnergyChanges();
            handleHealthRegeneration();
            handleDeathByStarvation();
            updateAttributesBasedOnEnergy();
            updateDescription();
        }
    }

    private void updateMaxEnergy() {
        if (tickAge <= ADULT_AGE_TICKS) {
            maxEnergy = 10 * Math.log(5 * tickAge + 5);
        } else if (tickAge < LIFESPAN) {
            maxEnergy = MAX_ENERGY_CAP;
        } else {
            maxEnergy = -(tickAge - LIFESPAN) / 20.0 + MAX_ENERGY_CAP;
        }

        if (energyLevel > maxEnergy) {
            updateEnergyLevel(maxEnergy);
        }
    }

    private void updateAge() {
        tickAge++;

        if (tickAge >= ADULT_AGE_TICKS && this.isBaby()) {
            setBaby(false);
        }
    }

    private void handleHitEnergyLoss() {
        if (wasRecentlyHit) {
            updateEnergyLevel(energyLevel * ENERGY_LOSS_ON_HIT);
            wasRecentlyHit = false;
        }
    }

    private void handleSunlightEnergyLoss() {
        // Zombies lose energy in daylight
        if (this.getWorld().isDay() &&
                this.getWorld().isSkyVisible(this.getBlockPos()) &&
                !this.isInvisible()) {
            updateEnergyLevel(energyLevel - SUNLIGHT_ENERGY_LOSS);
        }
    }

    private void handleEnergyChanges() {
        // Zombies gain energy at night
        if (!this.getWorld().isDay() && Math.random() < NIGHT_ENERGY_GAIN_CHANCE) {
            double energyGain = NIGHT_ENERGY_GAIN_MIN +
                    Math.random() * (NIGHT_ENERGY_GAIN_MAX - NIGHT_ENERGY_GAIN_MIN);
            updateEnergyLevel(energyLevel + energyGain);
        }

        // Passive energy loss
        if (Math.random() < PASSIVE_ENERGY_LOSS_CHANCE) {
            double energyLoss = PASSIVE_ENERGY_LOSS_MIN +
                    Math.random() * (PASSIVE_ENERGY_LOSS_MAX - PASSIVE_ENERGY_LOSS_MIN);
            updateEnergyLevel(energyLevel - energyLoss);
        }
    }

    private void handleHealthRegeneration() {
        // Zombies regenerate health when at max energy and at night
        if (energyLevel >= maxEnergy * 0.9 &&
                !this.getWorld().isDay() &&
                this.getHealth() < this.getMaxHealth()) {
            this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + HEALTH_REGEN_RATE));
        }
    }

    private void handleDeathByStarvation() {
        if (energyLevel <= 0.0) {
            this.kill();
        }
    }

    private void updateAttributesBasedOnEnergy() {
        // Zombie stats scale with energy level
        double energyRatio = energyLevel / maxEnergy;

        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                .setBaseValue(speed * energyRatio);

        this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE)
                .setBaseValue(attackDamage * energyRatio);
    }
}