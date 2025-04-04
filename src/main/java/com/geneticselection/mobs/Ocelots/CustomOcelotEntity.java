package com.geneticselection.mobs.Ocelots;

import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.mobs.Mooshroom.CustomMooshroomEntity;
import com.geneticselection.utils.DescriptionRenderer;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

import static com.geneticselection.genetics.ChildInheritance.*;

public class CustomOcelotEntity extends OcelotEntity {
    private MobAttributes mobAttributes;
    private double MaxHp;
    private double Speed;
    private double MaxEnergy;
    private double ELvl;
    private int breedingCooldown;

    private int panicTicks = 0;
    private static int LIFESPAN = 35000;
    private static final int PANIC_DURATION = 100; // 5 seconds at 20 ticks per second
    private static final double PANIC_SPEED_MULTIPLIER = 1.25;
    private boolean wasRecentlyHit = false;
    private int tickAge = 0;
    private int ticksSinceLastBreeding = 0;

    public CustomOcelotEntity(EntityType<? extends OcelotEntity> entityType, World world) {
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
        this.setHealth((float) this.MaxHp);
        this.Speed = this.mobAttributes.getMovementSpeed();
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);
        this.ELvl = this.mobAttributes.getEnergyLvl();

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

    private void updateDescription(com.geneticselection.mobs.Ocelots.CustomOcelotEntity ent) {
        DescriptionRenderer.setDescription(ent, Text.of("Attributes\n" +
                "Max Hp: " + String.format("%.3f", ent.getHealth()) + "/" + String.format("%.3f", ent.MaxHp) +
                "\nSpeed: " + String.format("%.3f", ent.Speed) +
                "\nEnergy: " + String.format("%.3f", ent.ELvl)));
    }

    public double getEnergyLevel() {
        return this.ELvl;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        // If player has an empty hand
        if (itemStack.isEmpty()) {
            player.playSound(SoundEvents.ENTITY_OCELOT_AMBIENT, 1.0F, 1.0F);

            // Only display the stats on the server side to avoid duplication
            if (!this.getWorld().isClient) {
                updateDescription(this);
            }
            return ActionResult.success(this.getWorld().isClient);
        } else {
            // If the player is holding something else (like food or another item), you can handle that here.
            // You can check itemStack for specific items, and create custom behavior for them.
            return super.interactMob(player, hand); // fallback to the default interaction
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
    }

    @Override
    public CustomOcelotEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof com.geneticselection.mobs.Ocelots.CustomOcelotEntity)) {
            return (com.geneticselection.mobs.Ocelots.CustomOcelotEntity) EntityType.OCELOT.create(serverWorld);
        }

        com.geneticselection.mobs.Ocelots.CustomOcelotEntity parent1 = this;
        com.geneticselection.mobs.Ocelots.CustomOcelotEntity parent2 = (com.geneticselection.mobs.Ocelots.CustomOcelotEntity) mate;

        MobAttributes attr1 = parent1.mobAttributes;
        MobAttributes attr2 = parent2.mobAttributes;

        MobAttributes childAttributes = inheritAttributes(attr1, attr2);


        com.geneticselection.mobs.Ocelots.CustomOcelotEntity child = new com.geneticselection.mobs.Ocelots.CustomOcelotEntity(ModEntities.CUSTOM_OCELOT, serverWorld);

        child.mobAttributes = childAttributes;
        applyAttributes(child, childAttributes);

        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.MaxHp);

        influenceGlobalAttributes(child.getType());

        if (!this.getWorld().isClient)
            updateDescription(child);

        return child;
    }

    @Override
    protected void applyDamage(DamageSource source, float amount) {
        super.applyDamage(source, amount);

        if (!this.getWorld().isClient)
            updateDescription(this);
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
    public void tick() {
        super.tick();

        // Only perform energy adjustments on the server side
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

            // Handle panic state
            if (panicTicks > 0) {
                panicTicks--;
                if (panicTicks == 0) {
                    // Reset speed back to normal when panic ends
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(Speed * (ELvl / MaxEnergy));
                }
            }

            // Handle energy loss if the cow was recently hit
            if (wasRecentlyHit) {
                // Reduce energy by 20% of its current level
                updateEnergyLevel(Math.max(0.0, ELvl * 0.8));
                wasRecentlyHit = false; // Reset the flag after applying the energy loss
            }

            // Check if the ocelot is standing on grass
            boolean isOnGrass = this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.GRASS_BLOCK);

            // Adjust energy level randomly based on whether the cow is on grass
            if (isOnGrass) {
                if (Math.random() < 0.3) { // 30% chance to gain energy
                    updateEnergyLevel(Math.min(100.0, ELvl + (0.1 + Math.random() * 0.75))); // Gain 0.1 to 0.75 energy
                }
            }

            if (Math.random() < 0.5) { // 50% chance to lose energy
                updateEnergyLevel(Math.max(0.0, ELvl - (0.05 + Math.random() * 0.3))); // Lose 0.05 to 0.3 energy
            }

            // Check if energy is 100 and regenerate health if not at max
            if (ELvl == MaxEnergy) {
                if (this.getHealth() < this.getMaxHealth()) {
                    this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + 0.5F)); // Regenerate 0.5 HP per second
                }
            }

            if (ELvl >= 90.0 && !isBaby() && ticksSinceLastBreeding >= breedingCooldown) {
                double searchRadius = 32.0;

                List<CustomOcelotEntity> mateCandidates = this.getWorld().getEntitiesByClass(
                        CustomOcelotEntity.class,
                        this.getBoundingBox().expand(searchRadius),
                        candidate -> candidate != this && candidate.getEnergyLevel() >= 90.0 && !candidate.isBaby()
                );

                // Find the nearest candidate
                CustomOcelotEntity nearestMate = null;
                double minDistanceSquared = Double.MAX_VALUE;
                for (CustomOcelotEntity candidate : mateCandidates) {
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

            // If energy reaches 0, kill the cow
            if (ELvl <= 0.0) {
                this.kill(); // This makes the cow die
            } else {
                // Update attributes dynamically if energy is greater than 0
                if (panicTicks == 0) { // Only update speed if not in panic mode
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(Speed * (ELvl / MaxEnergy));
                }

                // Update the description with the new energy level
                updateDescription(this);
            }
        }
    }
}