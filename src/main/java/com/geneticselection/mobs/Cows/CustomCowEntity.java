package com.geneticselection.mobs.Cows;
import com.geneticselection.mobs.Camels.CustomCamelEntity;
import io.netty.buffer.Unpooled;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
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
    private static final TrackedData<Float>
        MAX_HP = DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> E_LVL = DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> MAX_ENERGY = DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> MAX_MEAT = DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> MAX_LEATHER = DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> TICK_AGE = DataTracker.registerData(CustomCamelEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private MobAttributes mobAttributes;
    private static final TrackedData<Float> SPEED = DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private double MinMeat;
    private double MinLeather;
    private int milkingCooldown;
    private int breedingCooldown;
    private long lastMilkTime = 0;

    private int panicTicks = 0;
    private static int LIFESPAN = 35000;
    private static final int PANIC_DURATION = 100;
    private static final double PANIC_SPEED_MULTIPLIER = 1.25;
    private boolean wasRecentlyHit = false;
    private int ticksSinceLastBreeding = 0;

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
        this.dataTracker.set(MAX_MEAT, this.mobAttributes.getMaxMeat().map(Double::floatValue).orElse(0.0f));
        this.dataTracker.set(MAX_LEATHER, this.mobAttributes.getMaxLeather().map(Double::floatValue).orElse(0.0f));
        this.setMinMeat(1.0);
        this.setMinLeather(0.0);
        this.milkingCooldown = 3000 + (int)((1 - (getEnergyLevel() / 100.0)) * 2000) + random.nextInt(2001);
        this.breedingCooldown = 3000 + (int)((1 - (getEnergyLevel() / 100.0)) * 2000) + random.nextInt(2001);
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
            "\nCooldown: " + ent.milkingCooldown+
            "\nBreeding Cooldown: " + ent.breedingCooldown+
            "\nAge: " + ent.getTickAge())
        );
    }

    public double getMaxHP() {
        return this.dataTracker.get(MAX_HP).doubleValue();
    }

    public double getSpeed() {
        return this.dataTracker.get(SPEED).doubleValue();
    }

    public void updateSpeed(double newSpeed) {
        this.dataTracker.set(SPEED, (float)newSpeed);
    }

    public double getEnergyLevel() {
        return this.dataTracker.get(E_LVL).doubleValue();
    }

    public int getTickAge() {
        return this.dataTracker.get(TICK_AGE).intValue();
    }

    public void updateTickAge(int age) {
        this.dataTracker.set(TICK_AGE, age);
    }

    public void setMinMeat(double minMeat)
    {
        this.MinMeat = minMeat;
    }

    public void setMaxMeat(float maxMeat)
    {
        this.dataTracker.set(MAX_MEAT, maxMeat);
    }

    public float getMaxMeat()
    {
        return this.dataTracker.get(MAX_MEAT);
    }

    public void setMinLeather(double minLeather)
    {
        this.MinLeather = minLeather;
    }

    public void setMaxLeather(float maxLeather)
    {
        this.dataTracker.set(MAX_LEATHER, maxLeather);
    }

    public float getMaxLeather()
    {
        return this.dataTracker.get(MAX_LEATHER);
    }

    public float getMaxEnergy() {
        return this.dataTracker.get(MAX_ENERGY);
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

        wasRecentlyHit = true;
        panicTicks = PANIC_DURATION;

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
        double childMinMeat = ((parent1.MinMeat + parent2.MinMeat) / 2) * inheritanceFactor;
        double childMaxMeat = ((parent1.dataTracker.get(MAX_MEAT) + parent2.dataTracker.get(MAX_MEAT)) / 2) * inheritanceFactor;
        double childMinLeather = ((parent1.MinLeather + parent2.MinLeather) / 2) * inheritanceFactor;
        double childMaxLeather = ((parent1.dataTracker.get(MAX_LEATHER) + parent2.dataTracker.get(MAX_LEATHER)) / 2) * inheritanceFactor;
        int childMilkingCooldown = (int) (((parent1.milkingCooldown + parent2.milkingCooldown) / 2) * (1 / inheritanceFactor));
        int childBreedingCooldown = (int) (((parent1.breedingCooldown + parent2.breedingCooldown) / 2) * (1 / inheritanceFactor));
        double childEnergy = ((parent1.getEnergyLevel() + parent2.getEnergyLevel()) / 2) * inheritanceFactor;

        CustomCowEntity child = new CustomCowEntity(ModEntities.CUSTOM_COW, serverWorld);

        child.dataTracker.set(MAX_HP, (float)childMaxHp);
        child.MinMeat = childMinMeat;
        child.dataTracker.set(MAX_MEAT, (float)childMaxMeat);
        child.MinLeather = childMinLeather;
        child.dataTracker.set(MAX_LEATHER, (float)childMaxLeather);
        child.milkingCooldown = childMilkingCooldown;
        child.breedingCooldown = childBreedingCooldown;
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
            } else if (getTickAge() < LIFESPAN) {
                updateMaxEnergy(100.0f);
            } else {
                updateMaxEnergy((float)(-(getTickAge() - LIFESPAN) / 16.0 + 100));
            }
            updateTickAge(this.getTickAge() + 1);

            if (getTickAge() >= 4404 && this.isBaby()) {
                growUp(220, true);
            }

            if (getEnergyLevel() > getMaxEnergy()) {
                updateEnergyLevel(getMaxEnergy());
            }

            if (panicTicks > 0) {
                panicTicks--;
                if (panicTicks == 0) {
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                        .setBaseValue(getSpeed() * (getEnergyLevel() / getMaxEnergy()));
                }
            }

            if (wasRecentlyHit) {
                updateEnergyLevel(Math.max(0.0, getEnergyLevel() * 0.8));
                wasRecentlyHit = false;
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

            if (getEnergyLevel() >= 90.0 && !isBaby() && ticksSinceLastBreeding >= breedingCooldown) {
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
                            ticksSinceLastBreeding = 0;
                        }
                    }
                }
            }
            ticksSinceLastBreeding++;

            if (getEnergyLevel() <= 0.0) {
                this.kill();
            } else {
                if (panicTicks == 0) {
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                        .setBaseValue(getSpeed() * (getEnergyLevel() / getMaxEnergy()));
                }
                updateDescription(this);
            }
        }
    }
}