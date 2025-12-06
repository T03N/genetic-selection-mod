package com.geneticselection.mobs.Cows;

import io.netty.buffer.Unpooled;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;

import java.util.List;
import java.util.Optional;

public class CustomCowEntity extends CowEntity {

    // Constants
    private static final int LIFESPAN = 35000;
    private static final int PANIC_DURATION = 100;
    private static final double PANIC_SPEED_MULTIPLIER = 1.25;
    private static final int ADULT_AGE_TICKS = 4404;
    private static final double MAX_ENERGY_CAP = 100.0;
    private static final double ENERGY_REGENERATION_THRESHOLD = 90.0;
    private static final double MATE_SEARCH_RADIUS = 32.0;
    private static final double MATE_PROXIMITY_THRESHOLD = 4.0;
    private static final int LOVE_TICKS_DURATION = 500;

    // Genetic variance constants
    private static final double SPEED_VARIANCE_MIN = 0.98;
    private static final double SPEED_VARIANCE_RANGE = 0.1;
    private static final double HEALTH_VARIANCE_MIN = 0.98;
    private static final double HEALTH_VARIANCE_RANGE = 0.1;
    private static final double ENERGY_VARIANCE_MIN = 0.9;
    private static final double ENERGY_VARIANCE_RANGE = 0.1;
    private static final double MEAT_VARIANCE_MIN = 0.98;
    private static final double MEAT_VARIANCE_RANGE = 0.1;
    private static final double LEATHER_VARIANCE_MIN = 0.98;
    private static final double LEATHER_VARIANCE_RANGE = 0.1;

    // Cooldown calculation constants
    private static final int BASE_COOLDOWN = 3000;
    private static final int ENERGY_COOLDOWN_FACTOR = 2000;
    private static final int COOLDOWN_RANDOM_RANGE = 2001;

    // Energy change constants
    private static final double ENERGY_LOSS_ON_HIT = 0.8;
    private static final double ENERGY_GAIN_WHEAT = 10.0;
    private static final double GRASS_ENERGY_GAIN_CHANCE = 0.3;
    private static final double GRASS_ENERGY_GAIN_MIN = 0.1;
    private static final double GRASS_ENERGY_GAIN_MAX = 0.75;
    private static final double PASSIVE_ENERGY_LOSS_CHANCE = 0.5;
    private static final double PASSIVE_ENERGY_LOSS_MIN = 0.05;
    private static final double PASSIVE_ENERGY_LOSS_MAX = 0.3;
    private static final float HEALTH_REGEN_RATE = 0.5F;
    private static final double BREEDING_ENERGY_COST = 0.4;

    // Attributes
    private MobAttributes mobAttributes;
    private double maxHp;
    private double energyLevel;
    private double maxEnergy;
    private double speed;
    private double minMeat;
    private double maxMeat;
    private double minLeather;
    private double maxLeather;

    // Cooldowns and timers
    private int milkingCooldown;
    private int breedingCooldown;
    private long lastMilkTime = 0;
    private int panicTicks = 0;
    private int tickAge = 0;
    private int ticksSinceLastBreeding = 0;

    // State flags
    private boolean wasRecentlyHit = false;

    public CustomCowEntity(EntityType<? extends CowEntity> entityType, World world) {
        super(entityType, world);
        initializeAttributes(entityType);
        applyAttributesToEntity();
        calculateCooldowns();

        if (!this.getWorld().isClient) {
            updateDescription();
        }
    }

    private void initializeAttributes(EntityType<? extends CowEntity> entityType) {
        if (this.mobAttributes == null) {
            MobAttributes globalAttributes = GlobalAttributesManager.getAttributes(entityType);

            double speed = calculateVariance(globalAttributes.getMovementSpeed(),
                    SPEED_VARIANCE_MIN, SPEED_VARIANCE_RANGE);
            double health = calculateVariance(globalAttributes.getMaxHealth(),
                    HEALTH_VARIANCE_MIN, HEALTH_VARIANCE_RANGE);
            double energy = calculateVariance(globalAttributes.getEnergyLvl(),
                    ENERGY_VARIANCE_MIN, ENERGY_VARIANCE_RANGE);
            double meat = globalAttributes.getMaxMeat().orElse(0.0) +
                    (MEAT_VARIANCE_MIN + Math.random() * MEAT_VARIANCE_RANGE);
            double leather = calculateVariance(globalAttributes.getMaxLeather().orElse(0.0),
                    LEATHER_VARIANCE_MIN, LEATHER_VARIANCE_RANGE);

            this.mobAttributes = new MobAttributes(
                    speed, health, energy,
                    Optional.of(meat), Optional.of(leather),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()
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

        this.speed = this.mobAttributes.getMovementSpeed();
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.speed);

        this.energyLevel = this.mobAttributes.getEnergyLvl();

        this.mobAttributes.getMaxMeat().ifPresent(value -> this.maxMeat = value);
        this.mobAttributes.getMaxLeather().ifPresent(value -> this.maxLeather = value);

        this.minMeat = 1.0;
        this.minLeather = 0.0;
    }

    private void calculateCooldowns() {
        int energyPenalty = (int)((1 - (energyLevel / MAX_ENERGY_CAP)) * ENERGY_COOLDOWN_FACTOR);
        int randomOffset = random.nextInt(COOLDOWN_RANDOM_RANGE);

        this.milkingCooldown = BASE_COOLDOWN + energyPenalty + randomOffset;
        this.breedingCooldown = BASE_COOLDOWN + energyPenalty + randomOffset;
    }

    // Energy management
    public double getEnergyLevel() {
        return this.energyLevel;
    }

    public void updateEnergyLevel(double newEnergyLevel) {
        this.energyLevel = newEnergyLevel;

        if (this.getWorld().isClient) {
            markForRenderUpdate();
        } else {
            syncEnergyLevelToClient();
        }
    }

    private void syncEnergyLevelToClient() {
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
        data.writeInt(this.getId());
        data.writeDouble(this.energyLevel);
    }

    public void markForRenderUpdate() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.getEntityRenderDispatcher()
                .getRenderer(this)
                .render(this, 0, 0, new MatrixStack(),
                        client.getBufferBuilders().getEntityVertexConsumers(), 0);
    }

    private void updateDescription() {
        String description = String.format(
                "Attributes\n" +
                        "Max Hp: %.3f/%.3f\n" +
                        "Speed: %.3f\n" +
                        "Energy: %.3f\n" +
                        "Max Meat: %.3f\n" +
                        "Max Leather: %.3f\n" +
                        "Cooldown: %d\n" +
                        "Breeding Cooldown: %d\n" +
                        "Age: %d",
                getHealth(), maxHp, speed, energyLevel,
                maxMeat, maxLeather, milkingCooldown,
                breedingCooldown, tickAge
        );

        DescriptionRenderer.setDescription(this, Text.of(description));
    }

    // Setters
    public void setMinMeat(double minMeat) {
        this.minMeat = minMeat;
    }

    public void setMaxMeat(double maxMeat) {
        this.maxMeat = maxMeat;
    }

    public void setMinLeather(double minLeather) {
        this.minLeather = minLeather;
    }

    public void setMaxLeather(double maxLeather) {
        this.maxLeather = maxLeather;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        ItemStack offHandStack = player.getOffHandStack();

        if (isHoldingWheat(itemStack, offHandStack)) {
            return handleWheatInteraction(player, itemStack, offHandStack, hand);
        }

        if (itemStack.isOf(Items.BUCKET) && !this.isBaby()) {
            return handleMilkingInteraction(player, hand);
        }

        return super.interactMob(player, hand);
    }

    private boolean isHoldingWheat(ItemStack mainHand, ItemStack offHand) {
        return mainHand.isOf(Items.WHEAT) || offHand.isOf(Items.WHEAT);
    }

    private ActionResult handleWheatInteraction(PlayerEntity player, ItemStack mainHand,
                                                ItemStack offHand, Hand originalHand) {
        Hand usedHand = mainHand.isOf(Items.WHEAT) ? originalHand : Hand.OFF_HAND;
        ItemStack usedItem = mainHand.isOf(Items.WHEAT) ? mainHand : offHand;

        if (this.isBaby()) {
            return ActionResult.PASS;
        }

        if (this.isInLove()) {
            return handleWheatFeedingInLove(player, usedItem);
        } else {
            return handleWheatFeedingDefault(player, usedItem);
        }
    }

    private ActionResult handleWheatFeedingInLove(PlayerEntity player, ItemStack wheatStack) {
        if (energyLevel < maxEnergy) {
            updateEnergyLevel(Math.min(maxEnergy, energyLevel + ENERGY_GAIN_WHEAT));
            player.sendMessage(
                    Text.of(String.format("The cow has gained energy! Current energy: %.1f", energyLevel)),
                    true
            );

            if (!player.isCreative()) {
                wheatStack.decrement(1);
            }
            return ActionResult.SUCCESS;
        } else {
            player.sendMessage(Text.of("The cow is already at maximum energy."), true);
            return ActionResult.PASS;
        }
    }

    private ActionResult handleWheatFeedingDefault(PlayerEntity player, ItemStack wheatStack) {
        if (energyLevel >= ENERGY_REGENERATION_THRESHOLD) {
            player.sendMessage(
                    Text.of(String.format("The cow's energy is high (%.1f). It is ready to breed!", energyLevel)),
                    true
            );
            return ActionResult.PASS;
        } else {
            updateEnergyLevel(Math.min(maxEnergy, energyLevel + ENERGY_GAIN_WHEAT));
            player.sendMessage(
                    Text.of(String.format("The cow has gained energy! Current energy: %.1f", energyLevel)),
                    true
            );

            if (!player.isCreative()) {
                wheatStack.decrement(1);
            }
            return ActionResult.SUCCESS;
        }
    }

    private ActionResult handleMilkingInteraction(PlayerEntity player, Hand hand) {
        long currentTime = this.getWorld().getTime();
        long timeSinceLastMilk = currentTime - lastMilkTime;

        if (timeSinceLastMilk >= milkingCooldown) {
            player.setStackInHand(hand, new ItemStack(Items.MILK_BUCKET));
            lastMilkTime = currentTime;

            if (energyLevel > 0) {
                updateEnergyLevel(Math.max(0.0, energyLevel - ENERGY_GAIN_WHEAT));
            }

            updateDescription();
            return ActionResult.SUCCESS;
        } else {
            long ticksRemaining = milkingCooldown - timeSinceLastMilk;
            player.sendMessage(
                    Text.of(String.format("The cow cannot be milked yet. Wait %d more ticks.", ticksRemaining)),
                    true
            );
            return ActionResult.PASS;
        }
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
                .setBaseValue(speed * PANIC_SPEED_MULTIPLIER * (energyLevel / maxEnergy));
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        super.dropLoot(source, causedByPlayer);

        if (!this.getWorld().isClient) {
            dropMeatAndLeather(source);
        }
    }

    private void dropMeatAndLeather(DamageSource source) {
        int meatAmount = (int) (minMeat + Math.random() * (maxMeat - minMeat + 1));
        int leatherAmount = (int) (minLeather + Math.random() * (maxLeather - minLeather + 1));

        this.dropStack(new ItemStack(Items.LEATHER, leatherAmount));

        boolean shouldDropCooked = source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_FIRE);

        if (!shouldDropCooked && source.getAttacker() instanceof LivingEntity attacker) {
            ItemStack weapon = attacker.getMainHandStack();
            RegistryEntry<Enchantment> fireAspectEntry = attacker.getWorld()
                    .getRegistryManager()
                    .get(RegistryKeys.ENCHANTMENT)
                    .getEntry(Enchantments.FIRE_ASPECT)
                    .orElse(null);

            if (fireAspectEntry != null && EnchantmentHelper.getLevel(fireAspectEntry, weapon) >= 1) {
                shouldDropCooked = true;
            }
        }

        ItemStack meatStack = shouldDropCooked ?
                new ItemStack(Items.COOKED_BEEF, meatAmount) :
                new ItemStack(Items.BEEF, meatAmount);
        this.dropStack(meatStack);
    }

    @Override
    public CustomCowEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof CustomCowEntity customMate)) {
            return (CustomCowEntity) EntityType.COW.create(serverWorld);
        }

        double inheritanceFactor = calculateInheritanceFactor(customMate);
        CustomCowEntity child = createChildWithInheritedTraits(serverWorld, customMate, inheritanceFactor);

        applyBreedingCosts(customMate);
        this.resetLoveTicks();

        if (!this.getWorld().isClient) {
            child.updateDescription();
        }

        return child;
    }

    private double calculateInheritanceFactor(CustomCowEntity mate) {
        return Math.min(this.energyLevel, mate.energyLevel) / maxEnergy;
    }

    private CustomCowEntity createChildWithInheritedTraits(ServerWorld world,
                                                           CustomCowEntity parent2,
                                                           double inheritanceFactor) {
        CustomCowEntity child = new CustomCowEntity(ModEntities.CUSTOM_COW, world);

        child.maxHp = inheritAttribute(this.maxHp, parent2.maxHp, inheritanceFactor);
        child.minMeat = inheritAttribute(this.minMeat, parent2.minMeat, inheritanceFactor);
        child.maxMeat = inheritAttribute(this.maxMeat, parent2.maxMeat, inheritanceFactor);
        child.minLeather = inheritAttribute(this.minLeather, parent2.minLeather, inheritanceFactor);
        child.maxLeather = inheritAttribute(this.maxLeather, parent2.maxLeather, inheritanceFactor);
        child.energyLevel = inheritAttribute(this.energyLevel, parent2.energyLevel, inheritanceFactor);

        child.milkingCooldown = inheritCooldown(this.milkingCooldown, parent2.milkingCooldown, inheritanceFactor);
        child.breedingCooldown = inheritCooldown(this.breedingCooldown, parent2.breedingCooldown, inheritanceFactor);

        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.maxHp);
        child.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                .setBaseValue(child.speed * (child.energyLevel / maxEnergy));

        return child;
    }

    private double inheritAttribute(double parent1Value, double parent2Value, double factor) {
        return ((parent1Value + parent2Value) / 2) * factor;
    }

    private int inheritCooldown(int parent1Cooldown, int parent2Cooldown, double factor) {
        return (int) (((parent1Cooldown + parent2Cooldown) / 2) * (1 / factor));
    }

    private void applyBreedingCosts(CustomCowEntity mate) {
        this.energyLevel -= this.energyLevel * BREEDING_ENERGY_COST;
        mate.energyLevel -= mate.energyLevel * BREEDING_ENERGY_COST;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient) {
            updateMaxEnergy();
            updateAge();
            handlePanicState();
            handleHitEnergyLoss();
            handleEnergyChanges();
            handleHealthRegeneration();
            handleAutomaticBreeding();
            handleDeathByStarvation();
            updateMovementSpeed();
            updateDescription();
        }
    }

    private void updateMaxEnergy() {
        if (tickAge <= ADULT_AGE_TICKS) {
            maxEnergy = 10 * Math.log(5 * tickAge + 5);
        } else if (tickAge < LIFESPAN) {
            maxEnergy = MAX_ENERGY_CAP;
        } else {
            maxEnergy = -(tickAge - LIFESPAN) / 16.0 + MAX_ENERGY_CAP;
        }

        if (energyLevel > maxEnergy) {
            updateEnergyLevel(maxEnergy);
        }
    }

    private void updateAge() {
        tickAge++;

        if (tickAge >= ADULT_AGE_TICKS && this.isBaby()) {
            growUp(220, true);
        }
    }

    private void handlePanicState() {
        if (panicTicks > 0) {
            panicTicks--;
            if (panicTicks == 0) {
                resetSpeedAfterPanic();
            }
        }
    }

    private void resetSpeedAfterPanic() {
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                .setBaseValue(speed * (energyLevel / maxEnergy));
    }

    private void handleHitEnergyLoss() {
        if (wasRecentlyHit) {
            updateEnergyLevel(Math.max(0.0, energyLevel * ENERGY_LOSS_ON_HIT));
            wasRecentlyHit = false;
        }
    }

    private void handleEnergyChanges() {
        boolean isOnGrass = this.getWorld()
                .getBlockState(this.getBlockPos().down())
                .isOf(Blocks.GRASS_BLOCK);

        if (isOnGrass && Math.random() < GRASS_ENERGY_GAIN_CHANCE) {
            double energyGain = GRASS_ENERGY_GAIN_MIN +
                    Math.random() * (GRASS_ENERGY_GAIN_MAX - GRASS_ENERGY_GAIN_MIN);
            updateEnergyLevel(Math.min(MAX_ENERGY_CAP, energyLevel + energyGain));
        }

        if (Math.random() < PASSIVE_ENERGY_LOSS_CHANCE) {
            double energyLoss = PASSIVE_ENERGY_LOSS_MIN +
                    Math.random() * (PASSIVE_ENERGY_LOSS_MAX - PASSIVE_ENERGY_LOSS_MIN);
            updateEnergyLevel(Math.max(0.0, energyLevel - energyLoss));
        }
    }

    private void handleHealthRegeneration() {
        if (energyLevel == maxEnergy && this.getHealth() < this.getMaxHealth()) {
            this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + HEALTH_REGEN_RATE));
        }
    }

    private void handleAutomaticBreeding() {
        if (energyLevel >= ENERGY_REGENERATION_THRESHOLD &&
                !isBaby() &&
                ticksSinceLastBreeding >= breedingCooldown) {

            CustomCowEntity nearestMate = findNearestMate();

            if (nearestMate != null) {
                approachAndBreedWithMate(nearestMate);
            }
        }

        ticksSinceLastBreeding++;
    }

    private CustomCowEntity findNearestMate() {
        List<CustomCowEntity> candidates = this.getWorld().getEntitiesByClass(
                CustomCowEntity.class,
                this.getBoundingBox().expand(MATE_SEARCH_RADIUS),
                candidate -> candidate != this &&
                        candidate.getEnergyLevel() >= ENERGY_REGENERATION_THRESHOLD &&
                        !candidate.isBaby()
        );

        CustomCowEntity nearest = null;
        double minDistanceSquared = Double.MAX_VALUE;

        for (CustomCowEntity candidate : candidates) {
            double distSq = this.squaredDistanceTo(candidate);
            if (distSq < minDistanceSquared) {
                minDistanceSquared = distSq;
                nearest = candidate;
            }
        }

        return nearest;
    }

    private void approachAndBreedWithMate(CustomCowEntity mate) {
        double distanceSquared = this.squaredDistanceTo(mate);

        this.getNavigation().startMovingTo(mate,
                this.speed * 5.0F * (this.energyLevel / maxEnergy));

        if (distanceSquared < MATE_PROXIMITY_THRESHOLD &&
                !this.isInLove() &&
                !mate.isInLove()) {
            this.setLoveTicks(LOVE_TICKS_DURATION);
            mate.setLoveTicks(LOVE_TICKS_DURATION);
            ticksSinceLastBreeding = 0;
        }
    }

    private void handleDeathByStarvation() {
        if (energyLevel <= 0.0) {
            this.kill();
        }
    }

    private void updateMovementSpeed() {
        if (panicTicks == 0) {
            this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                    .setBaseValue(speed * (energyLevel / maxEnergy));
        }
    }
}