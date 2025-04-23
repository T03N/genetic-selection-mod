package com.geneticselection.mobs.Axolotl;

import com.geneticselection.attributes.AttributeCarrier;
import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.utils.DescriptionRenderer;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

import static com.geneticselection.genetics.ChildInheritance.*;

public class CustomAxolotlEntity extends AxolotlEntity implements AttributeCarrier {
    private MobAttributes mobAttributes;
    private double MaxHp;
    private double Speed;
    private double ELvl;
    private double MaxEnergy;

    private int panicTicks = 0;
    private static final int PANIC_DURATION = 100;
    private static final double PANIC_SPEED_MULTIPLIER = 2.0;
    private boolean wasRecentlyHit = false;

    // New fields for breeding and age
    private int breedingCooldown;
    private int ticksSinceLastBreeding = 0;
    private int tickAge = 0;
    private static int LIFESPAN = 35000;
    private int happyTicksRemaining = 0;
    private int forcedAge = 0;

    public CustomAxolotlEntity(EntityType<? extends AxolotlEntity> entityType, World world) {
        super(entityType, world);

        if (this.mobAttributes == null) {
            MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
            double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
            double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
            double energy = global.getEnergyLvl() * (0.9 + Math.random() * 0.1);
            this.mobAttributes = new MobAttributes(speed, health, energy, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        }

        this.MaxHp = this.mobAttributes.getMaxHealth();
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
        this.setHealth((float)this.MaxHp);
        this.Speed = this.mobAttributes.getMovementSpeed();
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);
        this.ELvl = this.mobAttributes.getEnergyLvl();
        this.MaxEnergy = 100.0;

        // Initialize breeding cooldown
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

    private void updateDescription(CustomAxolotlEntity ent) {
        DescriptionRenderer.setDescription(ent, Text.of("Attributes\n" +
                "Max Hp: " + String.format("%.1f", ent.getHealth()) + "/" + String.format("%.1f", ent.MaxHp) +
                "\nSpeed: " + String.format("%.2f", ent.Speed) +
                "\nEnergy: " + String.format("%.1f", ent.ELvl) + "/" + String.format("%.1f", ent.MaxEnergy) +
                "\nBreeding Cooldown: " + ent.breedingCooldown +
                "\nAge: " + ent.tickAge));
    }

    public double getEnergyLevel() {
        return this.ELvl;
    }

    // Override isBaby for custom aging logic
    @Override
    public boolean isBaby() {
        return this.tickAge < 4404 || super.isBaby();
    }

    // Add method for custom growth
    public void growUp(int age, boolean overGrow) {
        super.growUp(age, overGrow);

        if (overGrow) {
            this.forcedAge += age;
            if (this.happyTicksRemaining == 0) {
                this.happyTicksRemaining = 40;
                this.MaxEnergy = 100.0;
                this.ELvl = 100.0;
            }
        }

        // Set age to adult stage if it reaches maturity
        if (this.tickAge + age >= 4404) {
            this.tickAge = 4404;
            this.setBaby(false);
        } else {
            this.tickAge += age;
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        ItemStack offHandStack = player.getOffHandStack();
        boolean isTropicalFish = itemStack.isOf(Items.TROPICAL_FISH) || offHandStack.isOf(Items.TROPICAL_FISH);

        if (isTropicalFish) {
            // Handle main hand or offhand fish logic
            Hand usedHand = itemStack.isOf(Items.TROPICAL_FISH) ? hand : Hand.OFF_HAND;
            ItemStack usedItem = itemStack.isOf(Items.TROPICAL_FISH) ? itemStack : offHandStack;

            if (this.isBaby()) {
                if (!player.isCreative()) {
                    usedItem.decrement(1);
                }
                this.growUp((int)(this.getBreedingAge() / 20 * -0.1F), true);
                return ActionResult.SUCCESS;
            }

            // If the axolotl is in love mode
            if (this.isInLove()) {
                if (ELvl < MaxEnergy) {
                    updateEnergyLevel(Math.min(MaxEnergy, ELvl + 10.0)); // Gain energy (up to max)
                    player.sendMessage(Text.of("The axolotl has gained energy! Current energy: " + String.format("%.1f", ELvl)), true);

                    if (!player.isCreative()) { // Only consume fish if not in Creative mode
                        usedItem.decrement(1);
                    }

                    updateDescription(this); // Update description with new energy level
                    return ActionResult.SUCCESS;
                } else {
                    // Axolotl is in love mode and at max energy; do nothing
                    player.sendMessage(Text.of("The axolotl is already at maximum energy!"), true);
                    return ActionResult.PASS;
                }
            }

            if (ELvl < 20.0) {
                updateEnergyLevel(Math.min(MaxEnergy, ELvl + 10.0)); // Gain energy (up to max)
                player.sendMessage(Text.of("This axolotl cannot breed due to low energy. Energy increased to: " + String.format("%.1f", ELvl)), true);

                if (!player.isCreative()) { // Only consume fish if not in Creative mode
                    usedItem.decrement(1);
                }

                updateDescription(this); // Update description with new energy level
                return ActionResult.SUCCESS;
            } else {
                // If energy is sufficient, trigger breeding
                this.lovePlayer(player);
                player.sendMessage(Text.of("The axolotl is now in breed mode!"), true);

                if (!player.isCreative()) { // Only consume fish if not in Creative mode
                    usedItem.decrement(1);
                }

                updateDescription(this); // Update description
                return ActionResult.SUCCESS;
            }
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
    public void onDeath(DamageSource source) {
        if (this.isBaby()) {
            return;
        }

        if (ELvl <= 0.0) {
            // No specific drops for axolotls
        } else {
            super.onDeath(source);
            if (!this.getWorld().isClient) {
                // No specific resource drops
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient) {
            // Max energy is determined by age
            if(tickAge <= 4404){
                MaxEnergy = 10 * Math.log(5 * tickAge + 5);
            } else if (tickAge < LIFESPAN) {
                MaxEnergy = 100;
            } else {
                MaxEnergy = -(tickAge - LIFESPAN) / 16.0 + 100;
            }
            tickAge++;

            // Grow up if reached adult age
            if (tickAge >= 4404 && this.isBaby()) {
                growUp(220, true);
            }

            // Clamp energy level to maximum cap
            if (ELvl > MaxEnergy) {
                updateEnergyLevel(MaxEnergy);
            }

            // Handle panic
            if (panicTicks > 0) {
                panicTicks--;
                if (panicTicks == 0) {
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(Speed * (ELvl / MaxEnergy));
                }
            }

            // Handle energy loss from damage
            if (wasRecentlyHit) {
                ELvl = Math.max(0.0, ELvl * 0.8);
                wasRecentlyHit = false;
            }

            // Energy gain/loss based on environment
            boolean isOnEnergySource = this.submergedInWater;

            if (isOnEnergySource) {
                if (Math.random() < 0.3) { // 30% chance to gain energy when in water
                    updateEnergyLevel(Math.min(MaxEnergy, ELvl + (0.1 + Math.random() * 0.75))); // Gain 0.1 to 0.75 energy
                }
            } else {
                if (Math.random() < 0.5) { // 50% chance to lose energy when not in water
                    updateEnergyLevel(Math.max(0.0, ELvl - (0.05 + Math.random() * 0.3))); // Lose 0.05 to 0.3 energy
                }
            }

            // Health regeneration at max energy
            if (ELvl == MaxEnergy && this.getHealth() < this.getMaxHealth()) {
                this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + 0.5F));
            }

            // Autonomous breeding behavior
            if (ELvl >= 90.0 && !isBaby() && ticksSinceLastBreeding >= breedingCooldown) {
                double searchRadius = 32.0;

                List<CustomAxolotlEntity> mateCandidates = this.getWorld().getEntitiesByClass(
                        CustomAxolotlEntity.class,
                        this.getBoundingBox().expand(searchRadius),
                        candidate -> candidate != this && candidate.getEnergyLevel() >= 90.0 && !candidate.isBaby()
                );

                // Find the nearest candidate
                CustomAxolotlEntity nearestMate = null;
                double minDistanceSquared = Double.MAX_VALUE;
                for (CustomAxolotlEntity candidate : mateCandidates) {
                    double distSq = this.squaredDistanceTo(candidate);
                    if (distSq < minDistanceSquared) {
                        minDistanceSquared = distSq;
                        nearestMate = candidate;
                    }
                }

                // If we found a mate candidate, move towards it
                if (nearestMate != null) {
                    // Start moving towards the nearest axolotl
                    this.getNavigation().startMovingTo(nearestMate, this.Speed * 5.0F * (this.ELvl / MaxEnergy));

                    // If close enough (within 2 blocks)
                    if (minDistanceSquared < 4.0) {
                        // Only start breeding if both axolotls are not already in love
                        if (!this.isInLove() && !nearestMate.isInLove()) {
                            this.setLoveTicks(600);
                            nearestMate.setLoveTicks(600);
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
                            .setBaseValue(Speed * (ELvl / MaxEnergy));
                }
                updateDescription(this);
            }
        }
    }

    @Override
    public CustomAxolotlEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof CustomAxolotlEntity)) {
            return (CustomAxolotlEntity) EntityType.AXOLOTL.create(serverWorld);
        }

        CustomAxolotlEntity parent1 = this;
        CustomAxolotlEntity parent2 = (CustomAxolotlEntity) mate;

        // Calculate the inheritance factor based on the lower energy level of the parents
        double inheritanceFactor = Math.min(parent1.ELvl, parent2.ELvl) / MaxEnergy;

        // Get attributes for inheritance
        MobAttributes attr1 = parent1.mobAttributes;
        MobAttributes attr2 = parent2.mobAttributes;

        // Inherit attributes using existing method, but factor in energy levels
        MobAttributes childAttributes = inheritAttributes(attr1, attr2);

        // Modify inherited attributes based on energy factor
        double adjustedHealth = childAttributes.getMaxHealth() * inheritanceFactor;
        double adjustedSpeed = childAttributes.getMovementSpeed() * inheritanceFactor;
        double childEnergy = ((parent1.ELvl + parent2.ELvl) / 2) * inheritanceFactor;

        // Create adjusted attributes with energy factor
        MobAttributes adjustedAttributes = new MobAttributes(
                adjustedSpeed,
                adjustedHealth,
                childEnergy,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );

        // Create child entity
        CustomAxolotlEntity child = new CustomAxolotlEntity(ModEntities.CUSTOM_AXOLOTL, serverWorld);

        // Set child's variant (color)
        child.setVariant(this.getVariant());

        // Apply attributes
        child.mobAttributes = adjustedAttributes;
        applyAttributes(child, adjustedAttributes);

        // Set specific properties
        child.MaxHp = adjustedAttributes.getMaxHealth();
        child.Speed = adjustedAttributes.getMovementSpeed();
        child.ELvl = childEnergy;
        child.tickAge = 0; // Start as baby

        // Apply breeding cooldown based on parents
        child.breedingCooldown = (int)(((parent1.breedingCooldown + parent2.breedingCooldown) / 2) * (1 / inheritanceFactor));

        // Apply stats to the child entity
        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.MaxHp);
        child.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(child.Speed * (child.ELvl / MaxEnergy));

        // Parents lose energy after breeding
        parent1.ELvl -= parent1.ELvl * 0.4F;
        parent2.ELvl -= parent2.ELvl * 0.4F;
        this.resetLoveTicks();

        // Influence global attributes for evolution
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
                .setBaseValue(Speed * (ELvl / MaxEnergy) * PANIC_SPEED_MULTIPLIER);
        if (!this.getWorld().isClient)
            updateDescription(this);
    }

    @Override
    public void applyCustomAttributes(MobAttributes attributes) {
        // This method can be expanded if needed
    }
}