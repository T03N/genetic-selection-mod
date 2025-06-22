package com.geneticselection.mobs.Cows;

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
    private MobAttributes mobAttributes; // Directly store MobAttributes for this entity
    private double MaxHp;
    private double Speed;
    private double MinMeat;
    private double MaxMeat;
    private double MinLeather;
    private double MaxLeather;
    private int milkingCooldown;
    private int breedingCooldown;
    private long lastMilkTime = 0;

    private int panicTicks = 0;
    private static int LIFESPAN = 35000;
    private static final int PANIC_DURATION = 100; // 5 seconds at 20 ticks per second
    private static final double PANIC_SPEED_MULTIPLIER = 1.25;
    private boolean wasRecentlyHit = false;
    private int tickAge = 0;
    private int ticksSinceLastBreeding = 0;

    private static final TrackedData<Float> MAX_HP = DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> ELVL = DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> MAX_ENERGY = DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> TICK_AGE = DataTracker.registerData(CustomCowEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public CustomCowEntity(EntityType<? extends CowEntity> entityType, World world) {
        super(entityType, world);

        // Initialize mob attributes (directly within the class)
        if (this.mobAttributes == null) {
            MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
            double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
            double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
            double energy = global.getEnergyLvl() * (0.9 + Math.random() * 0.1);
            double meat = global.getMaxMeat().orElse(0.0) + (0.98 + Math.random() * 0.1);
            double leather = global.getMaxLeather().orElse(0.0) + (0.98 + Math.random() * 0.1);
            this.mobAttributes = new MobAttributes(speed, health, energy, Optional.of(meat), Optional.of(leather),Optional.empty(),Optional.empty(), Optional.empty());
            if (!world.isClient) {
                this.dataTracker.set(TICK_AGE, 0);
            }
        }
        this.Speed = this.mobAttributes.getMovementSpeed();
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);
        if (!world.isClient) {
            this.dataTracker.set(MAX_HP, (float)this.mobAttributes.getMaxHealth());
            this.dataTracker.set(ELVL, (float)this.mobAttributes.getEnergyLvl());
        }
        this.mobAttributes.getMaxMeat().ifPresent(maxMeat -> {
            this.MaxMeat = maxMeat;
        });
        this.mobAttributes.getMaxLeather().ifPresent(maxLeather -> {
            this.MaxLeather = maxLeather;
        });
        this.setMinMeat(1.0);
        this.setMinLeather(0.0);
        this.milkingCooldown = 3000 + (int)((1 - (getEnergyLevel() / 100.0)) * 2000) + random.nextInt(2001);
        this.breedingCooldown = 3000 + (int)((1 - (getEnergyLevel() / 100.0)) * 2000) + random.nextInt(2001);
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
    public float getEnergyLevel() { return this.dataTracker.get(ELVL); }
    public float getMaxEnergy() { return this.dataTracker.get(MAX_ENERGY); }
    public int getTickAge() { return this.dataTracker.get(TICK_AGE); }
    public double getSpeed() { return this.Speed; }
    public double getMaxMeat() { return this.MaxMeat; }
    public double getMaxLeather() { return this.MaxLeather; }
    public int getBreedingCooldown() { return this.breedingCooldown; }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putDouble("MaxMeat", this.MaxMeat);
        nbt.putDouble("MaxLeather", this.MaxLeather);
        nbt.putFloat("MaxHp", this.getMaxHpTracked());
        nbt.putFloat("ELvl", this.getEnergyLevel());
        nbt.putFloat("MaxEnergy", this.getMaxEnergy());
        nbt.putInt("TickAge", this.getTickAge());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.MaxMeat = nbt.getDouble("MaxMeat");
        this.MaxLeather = nbt.getDouble("MaxLeather");
        this.dataTracker.set(MAX_HP, nbt.getFloat("MaxHp"));
        this.dataTracker.set(ELVL, nbt.getFloat("ELvl"));
        this.dataTracker.set(MAX_ENERGY, nbt.getFloat("MaxEnergy"));
        this.dataTracker.set(TICK_AGE, nbt.getInt("TickAge"));
    }

    public void updateEnergyLevel(double newEnergyLevel) {
        if (!this.getWorld().isClient) {
            this.dataTracker.set(ELVL, (float)newEnergyLevel);
        }
    }

    private void updateDescription(CustomCowEntity ent) {
        DescriptionRenderer.setDescription(ent, Text.of("Attributes\n" +
                "Max Hp: " + String.format("%.3f", ent.getHealth()) + "/"+ String.format("%.3f", ent.MaxHp) +
                "\nSpeed: " + String.format("%.3f", ent.Speed) +
                "\nEnergy: " + String.format("%.3f", ent.ELvl) +
                "\nMax Meat: " + String.format("%.3f", ent.MaxMeat) +
                "\nMax Leather: " + String.format("%.3f", ent.MaxLeather)+
                "\nCooldown: " + ent.milkingCooldown+
                        "\nBreeding Cooldown: " + ent.breedingCooldown+
                        "\nAge: " + ent.tickAge)
                );
    }

    // Add this getter for energy level
    public double getEnergyLevel() {
        return this.ELvl;
    }

    public void setMinMeat(double minMeat)
    {
        this.MinMeat = minMeat;
    }

    public void setMaxMeat(double maxMeat)
    {
        this.MaxMeat = maxMeat;
    }

    public void setMinLeather(double minLeather)
    {
        this.MinLeather = minLeather;
    }

    public void setMaxLeather(double maxLeather)
    {
        this.MaxLeather = maxLeather;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        ItemStack offHandStack = player.getOffHandStack();  // Get the item in the offhand
        boolean isWheat = itemStack.isOf(Items.WHEAT) || offHandStack.isOf(Items.WHEAT); // Check both hands for wheat

        if (isWheat) {
            // Handle main hand or offhand wheat logic
            Hand usedHand = itemStack.isOf(Items.WHEAT) ? hand : Hand.OFF_HAND;
            ItemStack usedItem = itemStack.isOf(Items.WHEAT) ? itemStack : offHandStack;

            if (this.isBaby()) {
                return ActionResult.PASS; // Do nothing if the cow is a baby
            }

            // If the cow is in love mode
            if (this.isInLove()) {
                if (ELvl < MaxEnergy) {
                    updateEnergyLevel(Math.min(MaxEnergy, ELvl + 10.0)); // Gain energy (up to max 100)
                    player.sendMessage(Text.of("The cow has gained energy! Current energy: " + String.format("%.1f", ELvl)), true);

                    if (!player.isCreative()) { // Only consume wheat if the player is NOT in Creative mode
                        usedItem.decrement(1);
                    }

                    updateDescription(this); // Update description with new energy level
                    return ActionResult.SUCCESS;
                } else {
                    // Cow is in love mode and at max energy; do nothing
                    player.sendMessage(Text.of("The cow is already at maximum energy!"), true);
                    return ActionResult.PASS;
                }
            }

            if (ELvl < 20.0) {
                updateEnergyLevel(Math.min(MaxEnergy, ELvl + 10.0)); // Gain energy (up to max 100)
                player.sendMessage(Text.of("This cow cannot breed due to low energy. Energy increased to: " + String.format("%.1f", ELvl)), true);

                if (!player.isCreative()) { // Only consume wheat if the player is NOT in Creative mode
                    usedItem.decrement(1);
                }

                updateDescription(this); // Update description with new energy level
                return ActionResult.SUCCESS;
            } else {
                // If energy is sufficient, trigger breeding
                this.lovePlayer(player);
                player.sendMessage(Text.of("The cow is now in breed mode!"), true);

                if (!player.isCreative()) { // Only consume wheat if the player is NOT in Creative mode
                    usedItem.decrement(1);
                }

                updateDescription(this); // Update description
                return ActionResult.SUCCESS;
            }
        }

        // Handle other interactions (e.g., milking, empty hand, etc.)
        return super.interactMob(player, hand);
    }

    @Override
    protected void applyDamage(DamageSource source, float amount) {
        super.applyDamage(source, amount);

        // Mark the cow as recently hit
        wasRecentlyHit = true;

        // Start panic mode
        panicTicks = PANIC_DURATION;

        // Increase speed temporarily
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                .setBaseValue(Speed * (ELvl / MaxEnergy) * PANIC_SPEED_MULTIPLIER);

        if (!this.getWorld().isClient) {
            updateDescription(this);
        }
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
                this.MaxEnergy = 100.0F;
                this.ELvl = 100.0F;
            }
        }

        // Prevent resetting forcedAge unless we are an adult
        if (this.getBreedingAge() == 0 && this.forcedAge > 0) {
            this.setBreedingAge(this.forcedAge);
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);

        if (!this.getWorld().isClient) {
            int meatAmount = (int) (MaxMeat);

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
                this.dropStack(new ItemStack(Items.COOKED_BEEF, meatAmount));
            }
            else{
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

        // Calculate the inheritance factor based on the lower energy level of the parents
        double inheritanceFactor = Math.min(parent1.ELvl, parent2.ELvl) / MaxEnergy;

        // Inherit attributes from parents, scaled by the inheritance factor
        double childMaxHp = ((parent1.MaxHp + parent2.MaxHp) / 2) * inheritanceFactor;
        double childMinMeat = ((parent1.MinMeat + parent2.MinMeat) / 2) * inheritanceFactor;
        double childMaxMeat = ((parent1.MaxMeat + parent2.MaxMeat) / 2) * inheritanceFactor;
        double childMinLeather = ((parent1.MinLeather + parent2.MinLeather) / 2) * inheritanceFactor;
        double childMaxLeather = ((parent1.MaxLeather + parent2.MaxLeather) / 2) * inheritanceFactor;
        int childMilkingCooldown = (int) (((parent1.milkingCooldown + parent2.milkingCooldown) / 2) * (1 / inheritanceFactor));
        int childBreedingCooldown = (int) (((parent1.breedingCooldown + parent2.breedingCooldown) / 2) * (1 / inheritanceFactor));
        double childEnergy = ((parent1.ELvl + parent2.ELvl) / 2) * inheritanceFactor;

        // Create the child entity
        CustomCowEntity child = new CustomCowEntity(ModEntities.CUSTOM_COW, serverWorld);

        // Set inherited and calculated attributes
        child.MaxHp = childMaxHp;
        child.MinMeat = childMinMeat;
        child.MaxMeat = childMaxMeat;
        child.MinLeather = childMinLeather;
        child.MaxLeather = childMaxLeather;
        child.milkingCooldown = childMilkingCooldown;
        child.breedingCooldown = childBreedingCooldown;
        child.ELvl = childEnergy;

        // Apply stats to the child entity
        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.MaxHp);
        child.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(child.Speed * (child.ELvl / MaxEnergy));

        parent1.ELvl -= parent1.ELvl * 0.4F;
        parent2.ELvl -= parent2.ELvl * 0.4F;
        this.resetLoveTicks();

        if (!this.getWorld().isClient)
            updateDescription(child);

        return child;
    }

    @Override
    public void tick() {
        super.tick();

        // Only perform energy adjustments on the server side
        if (!this.getWorld().isClient) {
            int currentTickAge = this.getTickAge();
            float currentMaxEnergy;
            if(currentTickAge <= 4404){
                currentMaxEnergy = (float)(10 * Math.log(5 * currentTickAge + 5));
            } else if (currentTickAge < LIFESPAN) {
                currentMaxEnergy = 100;
            } else {
                currentMaxEnergy = (float)(-(currentTickAge - LIFESPAN) / 16.0 + 100);
            }
            this.dataTracker.set(TICK_AGE, currentTickAge + 1);
            this.dataTracker.set(MAX_ENERGY, currentMaxEnergy);
            if (currentTickAge >= 4404 && this.isBaby()) {
                growUp(220, true);
            }
            if (getEnergyLevel() > getMaxEnergy()) {
                updateEnergyLevel(getMaxEnergy());
            }
            if (panicTicks > 0) {
                panicTicks--;
                if (panicTicks == 0) {
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(Speed * (getEnergyLevel() / getMaxEnergy()));
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
                    this.getNavigation().startMovingTo(nearestMate, this.Speed * 5.0F * (this.getEnergyLevel() / this.getMaxEnergy()));
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
                            .setBaseValue(Speed * (getEnergyLevel() / getMaxEnergy()));
                }
            }
        }
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.getMaxHpTracked());
    }
}
