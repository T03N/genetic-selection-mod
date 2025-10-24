package com.geneticselection.mobs.Piglins;

import com.geneticselection.attributes.AttributeCarrier;
import com.geneticselection.attributes.AttributeKey;
import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.utils.DescriptionRenderer;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PiglinEntity;
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

import java.util.List;
import java.util.Optional;
import static com.geneticselection.genetics.ChildInheritance.*;

public class CustomPiglinEntity extends PiglinEntity implements AttributeCarrier {

    // Constants
    private static final int PANIC_DURATION = 100;
    private static final double PANIC_SPEED_MULTIPLIER = 1.5;
    private static final double ENERGY_LOSS_ON_HIT = 0.85;
    private static final double ENERGY_BREEDING_COST = 0.4;
    private static final double MAX_ATTACK_DAMAGE_CAP = 10.0; // Increased cap for piglins
    private static final double BREEDING_DAMAGE_BONUS = 0.08; // 8% improvement per generation

    // Attributes
    private MobAttributes mobAttributes;
    private double maxHp;
    private double speed;
    private double energyLevel;
    private double baseAttackDamage;
    private double maxGoldNuggets;

    // State
    private int panicTicks = 0;
    private int tickAge = 0;
    private int ticksSinceLastBreeding = 0;
    private int breedingCooldown;
    private boolean wasRecentlyHit = false;

    public CustomPiglinEntity(EntityType<? extends PiglinEntity> entityType, World world) {
        super(entityType, world);
        initializeAttributes(entityType);
        applyAttributesToEntity();
        calculateCooldowns();

        if (!this.getWorld().isClient) {
            updateDescription();
        }
    }

    private void initializeAttributes(EntityType<? extends PiglinEntity> entityType) {
        if (this.mobAttributes == null) {
            MobAttributes globalAttributes = GlobalAttributesManager.getAttributes(entityType);

            double speed = calculateVariance(globalAttributes.getMovementSpeed(), 0.95, 0.15);
            double health = calculateVariance(globalAttributes.getMaxHealth(), 0.95, 0.15);
            double energy = calculateVariance(globalAttributes.getEnergyLvl(), 0.9, 0.2);
            double attackDamage = calculateVariance(globalAttributes.getAttackDamage().orElse(5.0), 0.95, 0.15);

            this.mobAttributes = new MobAttributes(
                    speed, health, energy,
                    Optional.empty(), Optional.empty(),
                    Optional.of(attackDamage), Optional.empty(), Optional.empty(), Optional.empty()
            );
            this.tickAge = 0;
        }
    }

    private double calculateVariance(double baseValue, double min, double range) {
        return baseValue * (min + Math.random() * range);
    }

    private void applyAttributesToEntity() {
        this.maxHp = this.mobAttributes.getMaxHealth();
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.maxHp);
        this.setHealth((float) this.maxHp);

        this.speed = this.mobAttributes.getMovementSpeed();
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.speed);

        this.energyLevel = this.mobAttributes.getEnergyLvl();

        this.baseAttackDamage = this.mobAttributes.getAttackDamage().orElse(5.0);
        this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(this.baseAttackDamage);

        this.maxGoldNuggets = 4.0; // Base gold nugget drops
    }

    private void calculateCooldowns() {
        int energyPenalty = (int)((1 - (energyLevel / 100.0)) * 2000);
        this.breedingCooldown = 3000 + energyPenalty + random.nextInt(2001);
    }

    private void updateDescription() {
        double effectiveDamage = baseAttackDamage * (energyLevel / 100.0);

        String description = String.format(
                "Piglin Attributes\n" +
                        "Max Hp: %.1f/%.1f\n" +
                        "Speed: %.2f\n" +
                        "Energy: %.1f\n" +
                        "Base Damage: %.1f\n" +
                        "Current Damage: %.1f\n" +
                        "Age: %d",
                getHealth(), maxHp, speed, energyLevel,
                baseAttackDamage, effectiveDamage, tickAge
        );

        DescriptionRenderer.setDescription(this, Text.of(description));
    }

    // Energy management
    public double getEnergyLevel() {
        return this.energyLevel;
    }

    public void updateEnergyLevel(double newEnergyLevel) {
        this.energyLevel = Math.max(0.0, Math.min(100.0, newEnergyLevel));

        if (!this.getWorld().isClient) {
            syncEnergyLevelToClient();
            updateDescription();
        }
    }

    private void syncEnergyLevelToClient() {
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
        data.writeInt(this.getId());
        data.writeDouble(this.energyLevel);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);

        // Feed gold to restore energy
        if (itemStack.isOf(Items.GOLD_INGOT) && !this.getWorld().isClient) {
            if (energyLevel < 100.0) {
                updateEnergyLevel(Math.min(100.0, energyLevel + 15.0));
                player.sendMessage(
                        Text.of(String.format("Piglin energy restored! Current: %.1f", energyLevel)),
                        true
                );

                if (!player.isCreative()) {
                    itemStack.decrement(1);
                }
                return ActionResult.SUCCESS;
            } else {
                player.sendMessage(Text.of("Piglin is at maximum energy."), true);
                return ActionResult.PASS;
            }
        }

        // Display stats on empty hand
        if (itemStack.isEmpty() && !this.getWorld().isClient) {
            updateDescription();
            return ActionResult.SUCCESS;
        }

        return super.interactMob(player, hand);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean damaged = super.damage(source, amount);

        if (damaged && !this.getWorld().isClient) {
            wasRecentlyHit = true;
            enterPanicMode();
        }

        return damaged;
    }

    private void enterPanicMode() {
        panicTicks = PANIC_DURATION;
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                .setBaseValue(speed * PANIC_SPEED_MULTIPLIER * (energyLevel / 100.0));
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        super.dropLoot(source, causedByPlayer);

        if (!this.getWorld().isClient) {
            dropGoldNuggets(source);
        }
    }

    private void dropGoldNuggets(DamageSource source) {
        // Gold nugget drops scale with energy
        int nuggetAmount = (int) (Math.random() * maxGoldNuggets * (energyLevel / 100.0));

        if (nuggetAmount > 0) {
            this.dropStack(new ItemStack(Items.GOLD_NUGGET, nuggetAmount));
        }
    }

    public CustomPiglinEntity createChild(ServerWorld serverWorld, CustomPiglinEntity mate) {
        MobAttributes attr1 = this.mobAttributes;
        MobAttributes attr2 = mate.mobAttributes;

        MobAttributes childAttributes = inheritAttributes(attr1, attr2);

        // Apply breeding bonus to attack damage
        double parentAvgDamage = (this.baseAttackDamage + mate.baseAttackDamage) / 2.0;
        double improvedDamage = parentAvgDamage * (1.0 + BREEDING_DAMAGE_BONUS);
        improvedDamage = Math.min(improvedDamage, MAX_ATTACK_DAMAGE_CAP);

        childAttributes.set(AttributeKey.ATTACK_DAMAGE, improvedDamage);

        CustomPiglinEntity child = new CustomPiglinEntity(ModEntities.CUSTOM_PIGLIN, serverWorld);

        child.mobAttributes = childAttributes;
        applyAttributes(child, childAttributes);

        child.maxHp = childAttributes.getMaxHealth();
        child.energyLevel = childAttributes.getEnergyLvl();
        child.baseAttackDamage = improvedDamage;
        child.speed = childAttributes.getMovementSpeed();

        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.maxHp);
        child.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                .setBaseValue(child.speed * (child.energyLevel / 100.0));
        child.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE)
                .setBaseValue(child.baseAttackDamage * (child.energyLevel / 100.0));

        this.energyLevel -= this.energyLevel * ENERGY_BREEDING_COST;
        mate.energyLevel -= mate.energyLevel * ENERGY_BREEDING_COST;

        influenceGlobalAttributes(child.getType());

        if (!serverWorld.isClient) {
            child.updateDescription();
        }

        return child;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient) {
            tickAge++;
            updatePanicState();
            handleHitEnergyLoss();
            handleEnergyChanges();
            handleHealthRegeneration();
            handleAutomaticBreeding();
            handleDeathByStarvation();
            updateAttributesBasedOnEnergy();
            updateDescription();
        }
    }

    private void updatePanicState() {
        if (panicTicks > 0) {
            panicTicks--;
            if (panicTicks == 0) {
                resetSpeedAfterPanic();
            }
        }
    }

    private void resetSpeedAfterPanic() {
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                .setBaseValue(speed * (energyLevel / 100.0));
    }

    private void handleHitEnergyLoss() {
        if (wasRecentlyHit) {
            updateEnergyLevel(energyLevel * ENERGY_LOSS_ON_HIT);
            wasRecentlyHit = false;
        }
    }

    private void handleEnergyChanges() {
        // Piglins gain energy when standing on nether-related blocks
        boolean isOnNetherBlock = this.getWorld()
                .getBlockState(this.getBlockPos().down())
                .isIn(net.minecraft.registry.tag.BlockTags.BASE_STONE_NETHER) ||
                this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.SOUL_SAND) ||
                this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.SOUL_SOIL);

        if (isOnNetherBlock && Math.random() < 0.3) {
            updateEnergyLevel(energyLevel + (0.1 + Math.random() * 0.5));
        }

        // Passive energy loss
        if (Math.random() < 0.4) {
            updateEnergyLevel(energyLevel - (0.05 + Math.random() * 0.2));
        }
    }

    private void handleHealthRegeneration() {
        if (energyLevel >= 90.0 && this.getHealth() < this.getMaxHealth()) {
            this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + 0.3F));
        }
    }

    private void handleAutomaticBreeding() {
        if (energyLevel >= 90.0 &&
                !this.isBaby() &&
                ticksSinceLastBreeding >= breedingCooldown) {

            CustomPiglinEntity nearestMate = findNearestMate();

            if (nearestMate != null) {
                approachAndBreedWithMate(nearestMate);
            }
        }

        ticksSinceLastBreeding++;
    }

    private CustomPiglinEntity findNearestMate() {
        List<CustomPiglinEntity> candidates = this.getWorld().getEntitiesByClass(
                CustomPiglinEntity.class,
                this.getBoundingBox().expand(32.0),
                candidate -> candidate != this &&
                        candidate.getEnergyLevel() >= 90.0 &&
                        !candidate.isBaby()
        );

        CustomPiglinEntity nearest = null;
        double minDistanceSquared = Double.MAX_VALUE;

        for (CustomPiglinEntity candidate : candidates) {
            double distSq = this.squaredDistanceTo(candidate);
            if (distSq < minDistanceSquared) {
                minDistanceSquared = distSq;
                nearest = candidate;
            }
        }

        return nearest;
    }

    private void approachAndBreedWithMate(CustomPiglinEntity mate) {
        double distanceSquared = this.squaredDistanceTo(mate);

        this.getNavigation().startMovingTo(mate,
                this.speed * 5.0F * (this.energyLevel / 100.0));

        if (distanceSquared < 4.0) {
            // Create child directly since piglins don't use love mode
            CustomPiglinEntity child = createChild((ServerWorld) this.getWorld(), mate);
            child.setBaby(true);
            child.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
            this.getWorld().spawnEntity(child);
            ticksSinceLastBreeding = 0;
        }
    }

    private void handleDeathByStarvation() {
        if (energyLevel <= 0.0) {
            this.kill();
        }
    }

    private void updateAttributesBasedOnEnergy() {
        double energyRatio = energyLevel / 100.0;

        if (panicTicks == 0) {
            this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                    .setBaseValue(speed * energyRatio);
        }

        this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE)
                .setBaseValue(baseAttackDamage * energyRatio);
    }

    @Override
    public void applyCustomAttributes(MobAttributes attributes) {
        attributes.getAttackDamage().ifPresent(attackDamage -> this.baseAttackDamage = attackDamage);
    }
}