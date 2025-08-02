package com.geneticselection.mobs.Cows;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
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
    private static TrackedData<Float>
        MAX_HP = DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static TrackedData<Float> E_LVL = DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static TrackedData<Float> MAX_ENERGY = DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static TrackedData<Float> MAX_MEAT = DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static TrackedData<Float> MAX_LEATHER = DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static TrackedData<Integer> TICK_AGE = DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private MobAttributes mobAttributes;
    private static TrackedData<Float> SPEED = DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static TrackedData<Integer> MILKING_COOLDOWN = DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static TrackedData<Integer> BREEDING_COOLDOWN = DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.INTEGER);
    // private long lastMilkTime = 0;

    private static TrackedData<Integer> PANIC_TICKS = DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static TrackedData<Integer> LIFE_SPAN = DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static TrackedData<Boolean> WAS_RECENTLY_HIT = DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static TrackedData<Integer> TICKS_SINCE_LAST_BREEDING = DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private static final int PANIC_DURATION = 100;
    private static final double PANIC_SPEED_MULTIPLIER = 1.25;

    public CustomCowEntity(EntityType<? extends CowEntity> entityType, World world) {
        super(entityType, world);

        if (this.mobAttributes == null) {
            MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
            double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
            double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
            double energy = global.getEnergyLvl() * (0.9 + Math.random() * 0.1);
            double meat = global.getMaxMeat().orElse(0.0) + (0.98 + Math.random() * 0.1);
            double leather = global.getMaxLeather().orElse(0.0) * (0.98 + Math.random() * 0.1);
            this.mobAttributes = new MobAttributes(speed, health, energy, Optional.of(meat), Optional.of(leather), Optional.empty(), Optional.empty(), Optional.empty());
            updateTickAge(0);
        }

        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.mobAttributes.getMaxHealth());
        this.updateSpeed(this.mobAttributes.getMovementSpeed());
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.getSpeed());
        updateEnergyLevel(this.mobAttributes.getEnergyLvl());
        this.updateMaxEnergy(this.mobAttributes.getMaxMeat().map(Double::floatValue).orElse(0.0f));
        this.updateLeather(this.mobAttributes.getMaxLeather().map(Double::floatValue).orElse(0.0f));
        this.updateMilkingCooldown(3000 + (int)((1 - (getEnergyLevel() / 100.0)) * 2000) + random.nextInt(2001));
        this.updateBreedingCooldown(3000 + (int)((1 - (getEnergyLevel() / 100.0)) * 2000) + random.nextInt(2001));
        if (!this.getWorld().isClient) {
            updateDescription(this);
        }
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(MAX_HP, 10.0f);
        builder.add(E_LVL, 100.0f);
        builder.add(MAX_ENERGY, 100.0f);
        builder.add(MAX_MEAT, 3.0f);
        builder.add(MAX_LEATHER, 2.0f);
        builder.add(SPEED, 0.0f);
        builder.add(PANIC_TICKS, 0);
        builder.add(MILKING_COOLDOWN, 0);
        builder.add(BREEDING_COOLDOWN, 0);
        builder.add(LIFE_SPAN, 35000);
        builder.add(WAS_RECENTLY_HIT, false);
        builder.add(TICKS_SINCE_LAST_BREEDING, 0);
        builder.add(DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.INTEGER), 0);
        builder.add(DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.INTEGER), 0);
        System.out.println("Builder Info: " + builder);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putFloat("MaxHp", this.dataTracker.get(MAX_HP));
        nbt.putFloat("ELvl", this.dataTracker.get(E_LVL));
        nbt.putFloat("MaxEnergy", this.dataTracker.get(MAX_ENERGY));
        nbt.putFloat("MaxMeat", this.dataTracker.get(MAX_MEAT));
        nbt.putFloat("MaxLeather", this.dataTracker.get(MAX_LEATHER));
        nbt.putInt("tickAge", this.dataTracker.get(TICK_AGE));
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(MAX_HP, nbt.getFloat("MaxHp"));
        this.dataTracker.set(E_LVL, nbt.getFloat("ELvl"));
        this.dataTracker.set(MAX_ENERGY, nbt.getFloat("MaxEnergy"));
        this.dataTracker.set(MAX_MEAT, nbt.getFloat("MaxMeat"));
        this.dataTracker.set(MAX_LEATHER, nbt.getFloat("MaxLeather"));
        this.dataTracker.set(TICK_AGE, nbt.getInt("tickAge"));
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(getMaxHP());
    }

    private void updateDescription(CustomCowEntity ent) {
        DescriptionRenderer.setDescription(ent, Text.of("Attributes\n" +
            "Max Hp: " + String.format("%.3f", ent.getHealth()) + "/"+ String.format("%.3f", ent.getMaxHP()) +
            "\nSpeed: " + String.format("%.3f", ent.getSpeed()) +
            "\nEnergy: " + String.format("%.3f", ent.getEnergyLevel()) +
            "\nMax Meat: " + String.format("%.3f", ent.getMaxMeat()) +
            "\nMax Leather: " + String.format("%.3f", ent.getMaxLeather())+
            "\nCooldown: " + ent.getMilkingCooldown() +
            "\nBreeding Cooldown: " + ent.getBreedingCooldown() +
            "\nAge: " + ent.getTickAge())
        );
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        ItemStack offHandStack = player.getOffHandStack();
        boolean isWheat = itemStack.isOf(Items.WHEAT) || offHandStack.isOf(Items.WHEAT);

        if (isWheat) {
            Hand usedHand = itemStack.isOf(Items.WHEAT) ? hand : Hand.OFF_HAND;
            ItemStack usedItem = itemStack.isOf(Items.WHEAT) ? itemStack : offHandStack;

            if (this.isBaby()) {
                return ActionResult.PASS;
            }

            if (this.isInLove()) {
                if (getEnergyLevel() < getMaxEnergy()) {
                    updateEnergyLevel(Math.min(getMaxEnergy(), getEnergyLevel() + 10.0));
                    player.sendMessage(Text.of("The cow has gained energy! Current energy: " + String.format("%.1f", getEnergyLevel())), true);

                    if (!player.isCreative()) {
                        usedItem.decrement(1);
                    }

                    updateDescription(this);
                    return ActionResult.SUCCESS;
                } else {
                    player.sendMessage(Text.of("The cow is already at maximum energy!"), true);
                    return ActionResult.PASS;
                }
            }

            if (getEnergyLevel() < 20.0) {
                updateEnergyLevel(Math.min(getMaxEnergy(), getEnergyLevel() + 10.0));
                player.sendMessage(Text.of("This cow cannot breed due to low energy. Energy increased to: " + String.format("%.1f", getEnergyLevel())), true);

                if (!player.isCreative()) {
                    usedItem.decrement(1);
                }

                updateDescription(this);
                return ActionResult.SUCCESS;
            } else {
                this.lovePlayer(player);
                player.sendMessage(Text.of("The cow is now in breed mode!"), true);

                if (!player.isCreative()) {
                    usedItem.decrement(1);
                }

                updateDescription(this);
                return ActionResult.SUCCESS;
            }
        }

        return super.interactMob(player, hand);
    }

    @Override
    protected void applyDamage(DamageSource source, float amount) {
        super.applyDamage(source, amount);

        setWasRecentlyHit(true);
        updatePanicTicks(PANIC_DURATION);

        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
            .setBaseValue(getSpeed() * (getEnergyLevel() / getMaxEnergy()) * PANIC_SPEED_MULTIPLIER);

        if (!this.getWorld().isClient) {
            updateDescription(this);
        }
    }

    @Override
    public void growUp(int age, boolean overGrow) {
        int currentAge = this.getBreedingAge();
        int newAge = currentAge + age;

        if (newAge > 0) {
            newAge = 0;
        }

        int delta = newAge - currentAge;
        this.setBreedingAge(newAge);

        if (overGrow) {
            this.forcedAge += delta;
            if (this.happyTicksRemaining == 0) {
                this.happyTicksRemaining = 40;
                updateMaxEnergy(100.0f);
                updateEnergyLevel(100.0f);
            }
        }

        if (this.getBreedingAge() == 0 && this.forcedAge > 0) {
            this.setBreedingAge(this.forcedAge);
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);

        if (!this.getWorld().isClient) {
            int meatAmount = this.dataTracker.get(MAX_MEAT).intValue();

            boolean shouldDropCooked = false;

            if (source.getName().equals("onFire") || source.getName().equals("inFire") || source.getName().equals("lava")) {
                shouldDropCooked = true;
            }

            if (source.getAttacker() instanceof LivingEntity attacker) {
                ItemStack weapon = attacker.getMainHandStack();
                RegistryEntry<Enchantment> fireAspectEntry = this.getWorld().getServer().getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Enchantments.FIRE_ASPECT).get();
                if (EnchantmentHelper.getLevel(fireAspectEntry, weapon) >= 1) {
                    shouldDropCooked = true;
                }
            }

            if (shouldDropCooked) {
                this.dropStack(new ItemStack(Items.COOKED_BEEF, meatAmount));
            } else {
                this.dropStack(new ItemStack(Items.BEEF, meatAmount));
            }
        }
    }

    @Override
    public CustomCowEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof CustomCowEntity))
            return (CustomCowEntity) EntityType.COW.create(serverWorld);

        CustomCowEntity parent1 = this;
        CustomCowEntity parent2 = (CustomCowEntity) mate;

        double inheritanceFactor = Math.min(parent1.getEnergyLevel(), parent2.getEnergyLevel()) / getMaxEnergy();

        double childMaxHp = ((parent1.dataTracker.get(MAX_HP) + parent2.dataTracker.get(MAX_HP)) / 2) * inheritanceFactor;
        double childMaxMeat = ((parent1.dataTracker.get(MAX_MEAT) + parent2.dataTracker.get(MAX_MEAT)) / 2) * inheritanceFactor;
        double childMaxLeather = ((parent1.dataTracker.get(MAX_LEATHER) + parent2.dataTracker.get(MAX_LEATHER)) / 2) * inheritanceFactor;
        int childMilkingCooldown = (int) (((parent1.getMilkingCooldown() + parent2.getMilkingCooldown()) / 2) * (1 / inheritanceFactor));
        int childBreedingCooldown = (int) (((parent1.getBreedingCooldown() + parent2.getBreedingCooldown()) / 2) * (1 / inheritanceFactor));
        double childEnergy = ((parent1.getEnergyLevel() + parent2.getEnergyLevel()) / 2) * inheritanceFactor;

        CustomCowEntity child = new CustomCowEntity(ModEntities.CUSTOM_COW, serverWorld);

        child.updateMaxHP((float)childMaxHp);
        child.dataTracker.set(MAX_MEAT, (float)childMaxMeat);
        child.dataTracker.set(MAX_LEATHER, (float)childMaxLeather);
        child.updateMilkingCooldown(childMilkingCooldown);
        child.updateBreedingCooldown(childBreedingCooldown);
        child.updateEnergyLevel(childEnergy);

        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.dataTracker.get(MAX_HP));
        child.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(child.getSpeed() * (child.getEnergyLevel() / getMaxEnergy()));

        parent1.updateEnergyLevel(parent1.getEnergyLevel() - parent1.getEnergyLevel() * 0.4F);
        parent2.updateEnergyLevel(parent2.getEnergyLevel() - parent2.getEnergyLevel() * 0.4F);
        this.resetLoveTicks();

        if (!this.getWorld().isClient)
            updateDescription(child);

        return child;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient) {

            if(getTickAge() <= 4404){
                updateMaxEnergy((float)(10 * Math.log(5 * getTickAge() + 5)));
            } else if (getTickAge() < getLifeSpan()) {
                updateMaxEnergy(100.0f);
            } else {
                updateMaxEnergy((float)(-(getTickAge() - getLifeSpan()) / 16.0 + 100));
            }
            updateTickAge(this.getTickAge() + 1);

            if (getTickAge() >= 4404 && this.isBaby()) {
                growUp(220, true);
            }

            if (getEnergyLevel() > getMaxEnergy()) {
                updateEnergyLevel(getMaxEnergy());
            }

            if (getPanicTicks() > 0) {
                updatePanicTicks(getPanicTicks() - 1);
                if (getPanicTicks() == 0) {
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                        .setBaseValue(getSpeed() * (getEnergyLevel() / getMaxEnergy()));
                }
            }

            if (wasRecentlyHit()) {
                updateEnergyLevel(Math.max(0.0, getEnergyLevel() * 0.8));
                setWasRecentlyHit(false);
            }

            boolean isOnGrass = this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.GRASS_BLOCK);

            if (isOnGrass) {
                if (Math.random() < 0.3) {
                    updateEnergyLevel(Math.min(100.0, getEnergyLevel() + (0.1 + Math.random() * 0.75)));
                }
            }

            if (Math.random() < 0.5) {
                updateEnergyLevel(Math.max(0.0, getEnergyLevel() - (0.05 + Math.random() * 0.3)));
            }

            if (getEnergyLevel() == getMaxEnergy()) {
                if (this.getHealth() < this.getMaxHealth()) {
                    this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + 0.5F));
                }
            }

            if (getEnergyLevel() >= 90.0 && !isBaby() && getTicksSinceLastBreeding() >= getBreedingCooldown()) {
                double searchRadius = 32.0;

                List<CustomCowEntity> mateCandidates = this.getWorld().getEntitiesByClass(
                    CustomCowEntity.class,
                    this.getBoundingBox().expand(searchRadius),
                    candidate -> candidate != this && candidate.getEnergyLevel() >= 90.0 && !candidate.isBaby()
                );

                CustomCowEntity nearestMate = null;
                double minDistanceSquared = Double.MAX_VALUE;
                for (CustomCowEntity candidate : mateCandidates) {
                    double distSq = this.squaredDistanceTo(candidate);
                    if (distSq < minDistanceSquared) {
                        minDistanceSquared = distSq;
                        nearestMate = candidate;
                    }
                }

                if (nearestMate != null) {
                    this.getNavigation().startMovingTo(nearestMate, this.getSpeed() * 5.0F * (this.getEnergyLevel() / getMaxEnergy()));

                    if (minDistanceSquared < 4.0) {
                        if (!this.isInLove() && !nearestMate.isInLove()) {
                            this.setLoveTicks(500);
                            nearestMate.setLoveTicks(500);
                            updateTicksSinceLastBreeding(0);
                        }
                    }
                }
            }

            updateTicksSinceLastBreeding(getTicksSinceLastBreeding() + 1);
            if (getEnergyLevel() <= 0.0) {
                this.kill();
            } else {
                if (getPanicTicks() == 0) {
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                        .setBaseValue(getSpeed() * (getEnergyLevel() / getMaxEnergy()));
                }
                updateDescription(this);
            }
        }
    }


    public double getMaxHP() {
        return this.dataTracker.get(MAX_HP).doubleValue();
    }

    public double getSpeed() {
        return this.dataTracker.get(SPEED).doubleValue();
    }

    public double getEnergyLevel() {
        return this.dataTracker.get(E_LVL).doubleValue();
    }

    public int getTickAge() {
        return this.dataTracker.get(TICK_AGE).intValue();
    }

    public float getMaxMeat()
    {
        return this.dataTracker.get(MAX_MEAT);
    }

    public float getMaxLeather()
    {
        return this.dataTracker.get(MAX_LEATHER);
    }

    public float getMilkingCooldown()
    {
        return this.dataTracker.get(MILKING_COOLDOWN);
    }

    public float getBreedingCooldown()
    {
        return this.dataTracker.get(BREEDING_COOLDOWN);
    }

    public int getPanicTicks()
    {
        return this.dataTracker.get(PANIC_TICKS);
    }

    public float getLifeSpan()
    {
        return this.dataTracker.get(LIFE_SPAN);
    }

    public float getMaxEnergy() {
        return this.dataTracker.get(MAX_ENERGY);
    }

    public boolean wasRecentlyHit() {
        return this.dataTracker.get(WAS_RECENTLY_HIT);
    }

    public int getTicksSinceLastBreeding() {
        return this.dataTracker.get(TICKS_SINCE_LAST_BREEDING);
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

    public void updateLeather(float newMaxLeather) {
        this.dataTracker.set(MAX_LEATHER, newMaxLeather);
    }

    public void updateMeat(float newMaxMeat) {
        this.dataTracker.set(MAX_MEAT, newMaxMeat);
    }

    public void updateSpeed(double newSpeed) {
        this.dataTracker.set(SPEED, (float)newSpeed);
    }

    public void updateTickAge(int age) {
        this.dataTracker.set(TICK_AGE, age);
    }

    public void updateMilkingCooldown(int cooldown) {
        this.dataTracker.set(MILKING_COOLDOWN, cooldown);
    }

    public void updateBreedingCooldown(int cooldown) {
        this.dataTracker.set(BREEDING_COOLDOWN, cooldown);
    }

    public void updatePanicTicks(int ticks) {
        this.dataTracker.set(PANIC_TICKS, ticks);
    }

    public void updateLifeSpan(int lifespan) {
        this.dataTracker.set(LIFE_SPAN, lifespan);
    }

    public void setWasRecentlyHit(boolean wasHit) {
        this.dataTracker.set(WAS_RECENTLY_HIT, wasHit);
    }

    public void updateTicksSinceLastBreeding(int ticks) {
        this.dataTracker.set(TICKS_SINCE_LAST_BREEDING, ticks);
    }
}