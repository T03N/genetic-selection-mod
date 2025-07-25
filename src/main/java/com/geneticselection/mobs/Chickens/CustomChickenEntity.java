package com.geneticselection.mobs.Chickens;

import com.geneticselection.attributes.AttributeCarrier;
import com.geneticselection.attributes.AttributeKey;
import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.Cows.CustomCowEntity;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.utils.DescriptionRenderer;
import com.geneticselection.utils.EatGrassGoal;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;

import java.util.List;
import java.util.Optional;
import static com.geneticselection.genetics.ChildInheritance.*;

public class CustomChickenEntity extends ChickenEntity implements AttributeCarrier {
    private MobAttributes mobAttributes;
    private double Speed;
    private int breedingCooldown;

    private int panicTicks = 0;
    private static final int LIFESPAN = 20000;
    private static final int MAX_AGE = 27000; // After this age, chickens will die naturally
    private static final int PANIC_DURATION = 100;
    private static final double PANIC_SPEED_MULTIPLIER = 1.5;
    private boolean wasRecentlyHit = false;
    private int tickAge = 0;
    private int ticksSinceLastBreeding = 0;

    private static final TrackedData<Float> MAX_HP = DataTracker.registerData(CustomChickenEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> E_LVL = DataTracker.registerData(CustomChickenEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> MAX_ENERGY = DataTracker.registerData(CustomChickenEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> TICK_AGE = DataTracker.registerData(CustomChickenEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> MAX_MEAT = DataTracker.registerData(CustomChickenEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> MAX_FEATHERS = DataTracker.registerData(CustomChickenEntity.class, TrackedDataHandlerRegistry.FLOAT);


    public CustomChickenEntity(EntityType<? extends ChickenEntity> entityType, World world) {
        super(entityType, world);

        if (this.mobAttributes == null) {
            MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
            double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
            double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
            double energy = global.getEnergyLvl() * (0.9 + Math.random() * 0.1);
            double meat = global.getMaxMeat().orElse(0.0) + (0.98 + Math.random() * 0.1);
            double feathers = global.getMaxFeathers().orElse(0.0) + (0.98 + Math.random() * 0.1);
            this.mobAttributes = new MobAttributes(speed, health, energy, Optional.of(meat), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(feathers));
            this.tickAge = 0;
        }

        updateMaxHP(this.mobAttributes.getMaxHealth());
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(getMaxHP());
        this.setHealth(getMaxHealth());
        this.Speed = this.mobAttributes.getMovementSpeed();
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);
        updateEnergyLevel(this.mobAttributes.getEnergyLvl());

        this.dataTracker.set(MAX_MEAT, this.mobAttributes.getMaxMeat().map(Double::floatValue).orElse(0.0f));
        this.dataTracker.set(MAX_FEATHERS, this.mobAttributes.getMaxFeather().map(Double::floatValue).orElse(0.0f));
        this.breedingCooldown = 3000 + (int)((1 - (getEnergyLevel() / 100.0)) * 2000) + random.nextInt(2001);


        if (!this.getWorld().isClient)
            updateDescription(this);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(MAX_HP, 6.0f);
        builder.add(E_LVL, 100.0f);
        builder.add(MAX_ENERGY, 100.0f);
        builder.add(TICK_AGE, 0);
    }

    public int getBreedingCooldown() { return this.breedingCooldown; }

    private void updateDescription(CustomCowEntity ent) {
        DescriptionRenderer.setDescription(ent, Text.of("Attributes\n" +
            "Max Hp: " + String.format("%.3f", ent.getHealth()) + "/"+ String.format("%.3f", getMaxHP()) +
            "\nSpeed: " + String.format("%.3f", getSpeed()) +
            "\nEnergy: " + String.format("%.3f", ent.getEnergyLevel()) +
            "\nMax Meat: " + String.format("%.3f", getMaxMeat()) +
            "\nMax Feather: " + String.format("%.3f", getMaxFeather())+
            // "\nBreeding Cooldown: " + ent.breedingCooldown+
            "\nAge: " + getTickAge())
        );
    }

    public double getSpeed() { return this.Speed; }
    public double getMaxFeathers() { return this.dataTracker.get(MAX_FEATHERS); }

    public double getMaxHP() {
        return this.dataTracker.get(MAX_HP).doubleValue();
    }

    public double getEnergyLevel() {
        return this.dataTracker.get(E_LVL).doubleValue();
    }

    public void updateEnergyLevel(float newEnergyLevel) {
        this.dataTracker.set(E_LVL, newEnergyLevel);
    }

    public float getMaxEnergy() {
        return this.dataTracker.get(MAX_ENERGY);
    }

    public int getTickAge() {
        return this.dataTracker.get(TICK_AGE).intValue();
    }

    public void setMaxMeat(float maxMeat)
    {
        this.dataTracker.set(MAX_MEAT, maxMeat);
    }

    public float getMaxMeat()
    {
        return this.dataTracker.get(MAX_MEAT);
    }

    public void setMaxFeather(float maxFeather)
    {
        this.dataTracker.set(MAX_FEATHERS, maxFeather);
    }

    public float getMaxFeather()
    {
        return this.dataTracker.get(MAX_FEATHERS);
    }

    public void updateMaxHP(double newMaxHP) {
        this.dataTracker.set(MAX_HP, (float)newMaxHP);
    }

    public void updateEnergyLevel(double newEnergyLevel) {
        this.dataTracker.set(E_LVL, (float)newEnergyLevel);
    }

    public void updateMaxEnergy(float newMaxEnergy)
    {
        this.dataTracker.set(MAX_ENERGY, newMaxEnergy);
    }

    public void updateMaxMeat(float newMaxMeat)
    {
        this.dataTracker.set(MAX_MEAT, newMaxMeat);
    }

    public void updateMaxFeathers(float newMaxFeather)
    {
        this.dataTracker.set(MAX_FEATHERS, newMaxFeather);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putDouble("MaxMeat", this.getMaxMeat());
        nbt.putDouble("MaxFeathers", this.getMaxFeather());
        nbt.putDouble("MaxHp", this.getMaxHP());
        nbt.putDouble("ELvl", this.getEnergyLevel());
        nbt.putFloat("MaxEnergy", getMaxEnergy());
        nbt.putInt("TickAge", this.getTickAge());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.updateMaxMeat(nbt.getFloat("MaxMeat"));
        this.updateMaxFeathers(nbt.getFloat("MaxFeathers"));
        this.dataTracker.set(MAX_HP, nbt.getFloat("MaxHp"));
        this.dataTracker.set(E_LVL, nbt.getFloat("ELvl"));
        this.dataTracker.set(MAX_ENERGY, nbt.getFloat("MaxEnergy"));
        this.dataTracker.set(TICK_AGE, nbt.getInt("TickAge"));
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.4));
        this.goalSelector.add(2, new EatGrassGoal(this));
        this.goalSelector.add(3, new AnimalMateGoal(this, 1.0));
        this.goalSelector.add(4, new TemptGoal(this, 1.0, (stack) -> {
            return stack.isIn(ItemTags.CHICKEN_FOOD);
        }, false));
        this.goalSelector.add(5, new FollowParentGoal(this, 1.1));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
    }

    private void updateDescription(CustomChickenEntity ent) {
        DescriptionRenderer.setDescription(ent, Text.of("Attributes\n" +
                "Max Hp: " + String.format("%.1f", ent.getHealth()) + "/" + String.format("%.1f", ent.getMaxHP()) +
                "\nSpeed: " + String.format("%.2f", ent.Speed) +
                "\nEnergy: " + String.format("%.1f", getEnergyLevel()) + "/" + String.format("%.1f", ent.getMaxEnergy()) +
                "\nMax Meat: " + String.format("%.1f", ent.getMaxMeat()) +
                "\nFeathers: " + String.format("%.1f", ent.getMaxFeathers()) +
                "\nBreeding Cooldown: " + ent.breedingCooldown +
                "\nAge: " + ent.tickAge + "/" + MAX_AGE
        ));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);

        if (itemStack.isOf(Items.WHEAT_SEEDS)) {
            if (getEnergyLevel() < 20.0) {
                player.sendMessage(Text.of("This chicken cannot breed because it has low energy."), true);
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
    public void growUp(int age, boolean overGrow) {
        int currentAge = this.getBreedingAge();
        int newAge = currentAge + age; // Increment age by provided value

        // Ensure cow reaches adulthood when age hits 0 (negative age counting)
        if (newAge > 0) {
            newAge = 0; // Reaches adulthood at age 0 (negative -> 0 for babies)
        }

        int delta = newAge - currentAge;
        this.setBreedingAge(newAge);

        // Apply forcedAge for overgrowth if necessary
        if (overGrow) {
            this.forcedAge += delta;
            if (this.happyTicksRemaining == 0) {
                this.happyTicksRemaining = 40;

                updateMaxEnergy(100.0F);
                updateEnergyLevel(100.0F);
            }
        }

        // Prevent resetting forcedAge unless we are an adult
        if (this.getBreedingAge() == 0 && this.forcedAge > 0) {
            this.setBreedingAge(this.forcedAge);
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        if (this.isBaby()) {
            return;
        }

        if (!this.getWorld().isClient) {
            boolean shouldDropCooked = false;

            // Check if the entity died from fire, lava, or burning
            if (source.getName().equals("onFire") || source.getName().equals("inFire") || source.getName().equals("lava")) {
                shouldDropCooked = true;
            }

            // Check if the attacker has Fire Aspect
            if (source.getAttacker() instanceof LivingEntity attacker) {
                ItemStack weapon = attacker.getMainHandStack();
                RegistryEntry<Enchantment> fireAspectEntry = this.getWorld().getServer().getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Enchantments.FIRE_ASPECT).get();
                if (EnchantmentHelper.getLevel(fireAspectEntry ,weapon) >= 1) {
                    shouldDropCooked = true;
                }
            }

            // Drop cooked or raw chicken based on conditions
            if(shouldDropCooked) {
                if (getEnergyLevel() <= 0.0 || tickAge >= MAX_AGE) {
                    // Drop minimal resources for old or low energy chickens
                    this.dropStack(new ItemStack(Items.FEATHER, 1));
                    this.dropStack(new ItemStack(Items.BONE, 1)); // Add bone drop for old age death
                } else {
                    super.onDeath(source);
                    if (!this.getWorld().isClient) {
                        // Drop feathers based on energy
                        int feathersAmount = (int) ((getMaxFeather()) * (getEnergyLevel() / 100.0));
                        this.dropStack(new ItemStack(Items.FEATHER, feathersAmount));

                        // Drop chicken meat based on energy
                        int meatAmount = (int) ((getMaxMeat()) * (getEnergyLevel() / 100.0));
                        this.dropStack(new ItemStack(Items.COOKED_CHICKEN, meatAmount));
                    }
                }
            }
            else{
                if (getEnergyLevel() <= 0.0 || tickAge >= MAX_AGE) {
                    // Drop minimal resources for old or low energy chickens
                    this.dropStack(new ItemStack(Items.FEATHER, 1));
                    this.dropStack(new ItemStack(Items.BONE, 1)); // Add bone drop for old age death
                } else {
                    super.onDeath(source);
                    if (!this.getWorld().isClient) {
                        // Drop feathers based on energy
                        int feathersAmount = (int) ((MaxFeathers) * (getEnergyLevel() / 100.0));
                        this.dropStack(new ItemStack(Items.FEATHER, feathersAmount));

                        // Drop chicken meat based on energy
                        int meatAmount = (int) ((MaxMeat) * (getEnergyLevel() / 100.0));
                        this.dropStack(new ItemStack(Items.CHICKEN, meatAmount));
                    }
                }
            }
        }
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return true;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient) {
            int currentTickAge = this.getTickAge();
            float currentMaxEnergy = 100.0f;
            this.dataTracker.set(TICK_AGE, currentTickAge + 1);
            this.dataTracker.set(MAX_ENERGY, currentMaxEnergy);

            // Increment age
            tickAge++;

            // Age-based death: die of old age when MAX_AGE is reached
            if (tickAge >= MAX_AGE) {
                // Die of old age
                this.kill();
                return;
            }

            // Max energy is determined by age
            if(tickAge <= 957){
                MaxEnergy = 11.8 * Math.log(5 * tickAge + 5);
            } else if (tickAge > 957 && tickAge < LIFESPAN) {
                MaxEnergy = 100;
            } else {
                MaxEnergy = -(tickAge - LIFESPAN) / 16.0 + 100;
                // Ensure MaxEnergy doesn't go below 0
                MaxEnergy = Math.max(0, MaxEnergy);
            }

            if (tickAge >= 957 && this.isBaby()) {
                growUp(47, true);
            }

            // Clamp the current energy level to the maximum cap
            if (getEnergyLevel() > MaxEnergy) {
                updateEnergyLevel(MaxEnergy);
            }

            // Handle panic
            if (panicTicks > 0) {
                panicTicks--;
                if (panicTicks == 0) {
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(Speed * (getEnergyLevel() / MaxEnergy));
                }
            }

            // Handle energy loss from damage
            if (wasRecentlyHit) {
                ELvl = Math.max(0.0, getEnergyLevel() * 0.8);
                wasRecentlyHit = false;
            }

            // Energy gain/loss based on environment
            boolean isOnEnergySource = this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.GRASS_BLOCK);

            if (isOnEnergySource) {
                if (Math.random() < 0.2) { // 20% chance to gain energy
                    updateEnergyLevel(Math.min(MaxEnergy, getEnergyLevel() + (0.01 + Math.random() * 0.19))); // Gain 0.01 to 0.2 energy
                }
            } else {
                if (Math.random() < 0.5) { // 50% chance to lose energy
                    updateEnergyLevel(Math.max(0.0, getEnergyLevel() - (0.01 + Math.random() * 0.19))); // Lose 0.01 to 0.2 energy
                }
            }

            // Aging effects - decreasing energy and health as the chicken gets very old
            if (tickAge > LIFESPAN) {
                // Additional energy drain for old age
                double ageFactor = (tickAge - LIFESPAN) / (double)(MAX_AGE - LIFESPAN);
                updateEnergyLevel(Math.max(0.0, getEnergyLevel() - (0.05 * ageFactor))); // More energy loss based on age

                // Health deterioration with old age
                if (Math.random() < 0.1 * ageFactor) {
                    this.damage(this.getDamageSources().generic(), 0.5f * (float)ageFactor);
                }
            }

            // Health regeneration at max energy (only for chickens not in old age)
            if (getEnergyLevel() == MaxEnergy && this.getHealth() < this.getMaxHealth() && tickAge < LIFESPAN) {
                this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + 0.5F));
            }

            // Autonomous breeding behavior (only for chickens not in old age)
            if (getEnergyLevel() >= 90.0 && !isBaby() && ticksSinceLastBreeding >= breedingCooldown && tickAge < LIFESPAN) {
                double searchRadius = 32.0;

                List<CustomChickenEntity> mateCandidates = this.getWorld().getEntitiesByClass(
                        CustomChickenEntity.class,
                        this.getBoundingBox().expand(searchRadius),
                        candidate -> candidate != this && candidate.getEnergyLevel() >= 90.0 && !candidate.isBaby()
                );

                // Find the nearest candidate
                CustomChickenEntity nearestMate = null;
                double minDistanceSquared = Double.MAX_VALUE;
                for (CustomChickenEntity candidate : mateCandidates) {
                    double distSq = this.squaredDistanceTo(candidate);
                    if (distSq < minDistanceSquared) {
                        minDistanceSquared = distSq;
                        nearestMate = candidate;
                    }
                }

                // If we found a mate candidate, move towards it
                if (nearestMate != null) {
                    // Start moving towards the nearest chicken
                    this.getNavigation().startMovingTo(nearestMate, this.Speed * 5.0F * (this.getEnergyLevel() / MaxEnergy));

                    // If close enough (within 2 blocks)
                    if (minDistanceSquared < 4.0) {
                        // Only start breeding if both chickens are not already in love
                        if (!this.isInLove() && !nearestMate.isInLove()) {
                            this.setLoveTicks(500);
                            nearestMate.setLoveTicks(500);
                            ticksSinceLastBreeding = 0;
                        }
                    }
                }
            }
            ticksSinceLastBreeding++;

            // Kill if energy is 0
            if (getEnergyLevel() <= 0.0) {
                this.kill();
            } else {
                // Update speed
                if (panicTicks == 0) {
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(Speed * (getEnergyLevel() / MaxEnergy));
                }
                updateDescription(this);
            }
        }
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.getMaxHpTracked());
    }

    @Override
    public CustomChickenEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof CustomChickenEntity)) {
            return (CustomChickenEntity) EntityType.CHICKEN.create(serverWorld);
        }

        CustomChickenEntity parent1 = this;
        CustomChickenEntity parent2 = (CustomChickenEntity) mate;

        MobAttributes attr1 = parent1.mobAttributes;
        MobAttributes attr2 = parent2.mobAttributes;

        MobAttributes childAttributes = inheritAttributes(attr1, attr2);

        CustomChickenEntity child = new CustomChickenEntity(ModEntities.CUSTOM_CHICKEN, serverWorld);

        child.mobAttributes = childAttributes;
        applyAttributes(child, childAttributes);

        child.MaxHp = childAttributes.getMaxHealth();
        child.ELvl = childAttributes.getEnergyLvl();
        child.MaxMeat = childAttributes.get(AttributeKey.MAX_MEAT);
        child.MaxFeathers = childAttributes.get(AttributeKey.MAX_FEATHERS);
        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.MaxHp);
        child.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(child.Speed * (child.getEnergyLevel() / 100.0));

        parent1.ELvl -= parent1.getEnergyLevel() * 0.4F;
        parent2.ELvl -= parent2.getEnergyLevel() * 0.4F;
        this.resetLoveTicks();

        influenceGlobalAttributes(child.getType());

        if (!this.getWorld().isClient)
            updateDescription(child);

        return child;
    }

    @Override
    protected void applyDamage(DamageSource source, float amount) {
        super.applyDamage(source, amount);
        wasRecentlyHit = true;
        panicTicks = PANIC_DURATION;
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                .setBaseValue(Speed * (getEnergyLevel() / MaxEnergy) * PANIC_SPEED_MULTIPLIER);
        if (!this.getWorld().isClient)
            updateDescription(this);
    }

    @Override
    public void applyCustomAttributes(MobAttributes attributes) {
        attributes.getMaxMeat().ifPresent(val -> this.MaxMeat = val);
        attributes.getMaxFeathers().ifPresent(val -> this.MaxFeathers = val);
    }
}

