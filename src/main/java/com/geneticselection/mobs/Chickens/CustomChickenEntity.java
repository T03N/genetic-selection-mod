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

import java.util.List;
import java.util.Optional;
import static com.geneticselection.genetics.ChildInheritance.*;

public class CustomChickenEntity extends ChickenEntity implements AttributeCarrier {
    private MobAttributes mobAttributes;
    private double MaxHp;
    private double Speed;
    private double MaxEnergy = 100.0F;
    private double ELvl;
    private double MaxMeat;
    private double MaxFeathers;
    private int breedingCooldown;

    private int panicTicks = 0;
    private static int LIFESPAN = 30000;
    private static final int PANIC_DURATION = 100;
    private static final double PANIC_SPEED_MULTIPLIER = 1.5;
    private boolean wasRecentlyHit = false;
    private int tickAge = 0;
    private int ticksSinceLastBreeding = 0;

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

        this.MaxHp = this.mobAttributes.getMaxHealth();
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
        this.setHealth((float)this.MaxHp);
        this.Speed = this.mobAttributes.getMovementSpeed();
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);
        this.ELvl = this.mobAttributes.getEnergyLvl();

        this.mobAttributes.getMaxMeat().ifPresent(maxMeat -> {
            this.MaxMeat = maxMeat;
        });
        this.mobAttributes.getMaxFeathers().ifPresent(maxFeathers -> {
            this.MaxFeathers = maxFeathers;
        });
        this.breedingCooldown = 3000 + (int)((1 - (ELvl / 100.0)) * 2000) + random.nextInt(2001);


        if (!this.getWorld().isClient)
            updateDescription(this);
    }

    public void updateEnergyLevel(double newEnergyLevel) {
        this.ELvl = newEnergyLevel;

        // Sync energy level with server if needed
        if (!this.getWorld().isClient) {
            this.syncEnergyLevelToClient();
        }
    }

    private void syncEnergyLevelToClient() {
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
        data.writeInt(this.getId());  // Send entity ID
        data.writeDouble(this.ELvl);  // Send the updated energy level
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
                "Max Hp: " + String.format("%.1f", ent.getHealth()) + "/" + String.format("%.1f", ent.MaxHp) +
                "\nSpeed: " + String.format("%.2f", ent.Speed) +
                "\nEnergy: " + String.format("%.1f", ent.ELvl) +
                "\nMax Meat: " + String.format("%.1f", ent.MaxMeat) +
                "\nFeathers: " + String.format("%.1f", ent.MaxFeathers) +
                        "\nBreeding Cooldown: " + ent.breedingCooldown+
                        "\nAge: " + ent.tickAge
                ));
    }

    public double getEnergyLevel(){
        return this.ELvl;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);

        if (itemStack.isOf(Items.WHEAT_SEEDS)) {
            if (ELvl < 20.0) {
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
                if (ELvl <= 0.0) {
                    // Drop minimal resources or nothing
                    this.dropStack(new ItemStack(Items.FEATHER, 1));
                } else {
                    super.onDeath(source);
                    if (!this.getWorld().isClient) {
                        // Drop feathers based on energy
                        int feathersAmount = (int) ((MaxFeathers) * (ELvl / 100.0));
                        this.dropStack(new ItemStack(Items.FEATHER, feathersAmount));

                        // Drop chicken meat based on energy
                        int meatAmount = (int) ((MaxMeat) * (ELvl / 100.0));
                        this.dropStack(new ItemStack(Items.COOKED_CHICKEN, meatAmount));
                    }
                }
            }
            else{
                if (ELvl <= 0.0) {
                    // Drop minimal resources or nothing
                    this.dropStack(new ItemStack(Items.FEATHER, 1));
                } else {
                    super.onDeath(source);
                    if (!this.getWorld().isClient) {
                        // Drop feathers based on energy
                        int feathersAmount = (int) ((MaxFeathers) * (ELvl / 100.0));
                        this.dropStack(new ItemStack(Items.FEATHER, feathersAmount));

                        // Drop chicken meat based on energy
                        int meatAmount = (int) ((MaxMeat) * (ELvl / 100.0));
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

            // Max energy is determined by age
            if(tickAge <= 4404){
                MaxEnergy = 10 * Math.log(5 * tickAge + 5);
            } else if (tickAge > 4404 && tickAge < LIFESPAN) {
                MaxEnergy = 100;
            } else {
                MaxEnergy = -(tickAge - LIFESPAN) / 16.0 + 100;
            }
            tickAge++;

            if (tickAge >= 4404 && this.isBaby()) {
                growUp(220, true);
            }

            // Clamp the current energy level to the maximum cap
            if (ELvl > MaxEnergy) {
                updateEnergyLevel(MaxEnergy);
            }

            // Handle panic
            if (panicTicks > 0) {
                panicTicks--;
                if (panicTicks == 0) {
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(Speed * (ELvl / 100.0));
                }
            }

            // Handle energy loss from damage
            if (wasRecentlyHit) {
                ELvl = Math.max(0.0, ELvl * 0.8);
                wasRecentlyHit = false;
            }

            // Energy gain/loss based on environment
            boolean isOnEnergySource = this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.GRASS_BLOCK);

            if (isOnEnergySource) {
                if (Math.random() < 0.2) { // 20% chance to gain energy
                    updateEnergyLevel(Math.min(MaxEnergy, ELvl + (0.01 + Math.random() * 0.19))); // Gain 0.01 to 0.2 energy
                }
            } else {
                if (Math.random() < 0.5) { // 50% chance to lose energy
                    updateEnergyLevel(Math.max(0.0, ELvl - (0.01 + Math.random() * 0.19))); // Lose 0.01 to 0.2 energy
                }
            }

            // Health regeneration at max energy
            if (ELvl == 100.0 && this.getHealth() < this.getMaxHealth()) {
                this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + 0.5F));
            }

            if (ELvl >= 90.0 && !isBaby() && ticksSinceLastBreeding >= breedingCooldown) {
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
                    // Start moving towards the nearest cow; adjust speed as needed
                    this.getNavigation().startMovingTo(nearestMate, this.Speed * 5.0F * (this.ELvl / MaxEnergy));

                    // If close enough (e.g., within 2 blocks; adjust the threshold as needed)
                    if (minDistanceSquared < 4.0) {
                        // Only start breeding if both cows are not already in love
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
            if (ELvl <= 0.0) {
                this.kill();
            } else {
                // Update speed
                if (panicTicks == 0) {
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(Speed * (ELvl / 100.0));
                }
                updateDescription(this);
            }
        }
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
        child.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(child.Speed * (child.ELvl / 100.0));

        parent1.ELvl -= parent1.ELvl * 0.4F;
        parent2.ELvl -= parent2.ELvl * 0.4F;
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
                .setBaseValue(Speed * (ELvl / 100.0) * PANIC_SPEED_MULTIPLIER);
        if (!this.getWorld().isClient)
            updateDescription(this);
    }

    @Override
    public void applyCustomAttributes(MobAttributes attributes) {
        attributes.getMaxMeat().ifPresent(val -> this.MaxMeat = val);
        attributes.getMaxFeathers().ifPresent(val -> this.MaxFeathers = val);
    }
}