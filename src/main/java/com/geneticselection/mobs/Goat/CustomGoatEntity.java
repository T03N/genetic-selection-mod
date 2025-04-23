package com.geneticselection.mobs.Goat;

import com.geneticselection.attributes.AttributeCarrier;
import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.utils.DescriptionRenderer;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;
import static com.geneticselection.genetics.ChildInheritance.*;

public class CustomGoatEntity extends GoatEntity implements AttributeCarrier {
    private MobAttributes mobAttributes;
    private double MaxHp;
    private double Speed;
    private double ELvl;
    private double MaxEnergy;
    private int breedingCooldown;

    private int panicTicks = 0;
    private static final int LIFESPAN = 35000;
    private static final int MAX_AGE = 45000; // After this age, goats will die naturally
    private static final int PANIC_DURATION = 100;
    private static final double PANIC_SPEED_MULTIPLIER = 2.0;
    private boolean wasRecentlyHit = false;
    private int tickAge = 0;
    private int ticksSinceLastBreeding = 0;

    public CustomGoatEntity(EntityType<? extends GoatEntity> entityType, World world) {
        super(entityType, world);

        if (this.mobAttributes == null) {
            MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
            double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
            double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
            double energy = global.getEnergyLvl() * (0.9 + Math.random() * 0.1);
            double meat = global.getMaxMeat().orElse(0.0) + (0.98 + Math.random() * 0.1);
            double leather = global.getMaxLeather().orElse(0.0) + (0.98 + Math.random() * 0.1);
            this.mobAttributes = new MobAttributes(speed, health, energy, Optional.of(meat), Optional.of(leather), Optional.empty(), Optional.empty(), Optional.empty());
        }

        this.MaxHp = this.mobAttributes.getMaxHealth();
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
        this.setHealth((float)this.MaxHp);
        this.Speed = this.mobAttributes.getMovementSpeed();
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);
        this.ELvl = this.mobAttributes.getEnergyLvl();

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

    public double getEnergyLevel(){
        return this.ELvl;
    }

    private void syncEnergyLevelToClient() {
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
        data.writeInt(this.getId());  // Send entity ID
        data.writeDouble(this.ELvl);  // Send the updated energy level
    }

    private void updateDescription(CustomGoatEntity ent) {
        DescriptionRenderer.setDescription(ent, Text.of("Attributes\n" +
                "Max Hp: " + String.format("%.1f", ent.getHealth()) + "/" + String.format("%.1f", ent.MaxHp) +
                "\nSpeed: " + String.format("%.2f", ent.Speed) +
                "\nEnergy: " + String.format("%.1f", ent.ELvl) + "/" + String.format("%.1f", ent.MaxEnergy) +
                "\nBreeding Cooldown: " + ent.breedingCooldown+
                "\nAge: " + ent.tickAge + "/" + MAX_AGE)
        );
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);

        if (itemStack.isOf(Items.HAY_BLOCK)) {
            if (ELvl < 20.0) {
                player.sendMessage(Text.of("This goat cannot breed because it has low energy."), true);
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
    public void onDeath(DamageSource source) {
        if (this.isBaby()) {
            return;
        }

        if (ELvl <= 0.0 || tickAge >= MAX_AGE) {
            // Drop minimal resources or bones for old age
            this.dropStack(new ItemStack(Items.LEATHER, 1));

            if (tickAge >= MAX_AGE) {
                // Drop bones if died of old age
                this.dropStack(new ItemStack(Items.BONE, 1 + random.nextInt(2)));
            }
        } else {
            super.onDeath(source);
            if (!this.getWorld().isClient) {
                // Drop mutton based on energy
                if (this.isOnFire()) {
                    int meatAmount = (int) (3 * (ELvl / 100.0));
                    this.dropStack(new ItemStack(Items.COOKED_MUTTON, Math.max(1, meatAmount)));
                } else {
                    int meatAmount = (int) (3 * (ELvl / 100.0));
                    this.dropStack(new ItemStack(Items.MUTTON, Math.max(1, meatAmount)));
                }

                // Drop wool
                this.dropStack(new ItemStack(Items.WHITE_WOOL, 1));
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Only perform energy adjustments on the server side
        if (!this.getWorld().isClient) {
            // Increment age
            tickAge++;

            // Age-based death: die of old age when MAX_AGE is reached
            if (tickAge >= MAX_AGE) {
                // Die of old age
                this.kill();
                return;
            }

            // Max energy is determined by age
            if(tickAge <= 4404){
                MaxEnergy = 10 * Math.log(5 * tickAge + 5);
            } else if (tickAge < LIFESPAN) {
                MaxEnergy = 100;
            } else {
                MaxEnergy = -(tickAge - LIFESPAN) / 16.0 + 100;
                // Ensure MaxEnergy doesn't go below 0
                MaxEnergy = Math.max(0, MaxEnergy);
            }

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

            // Handle energy loss if the goat was recently hit
            if (wasRecentlyHit) {
                // Reduce energy by 20% of its current level
                updateEnergyLevel(Math.max(0.0, ELvl * 0.8));
                wasRecentlyHit = false; // Reset the flag after applying the energy loss
            }

            // Check if the goat is standing on grass
            boolean isOnGrass = this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.GRASS_BLOCK);

            // Adjust energy level randomly based on whether the goat is on grass
            if (isOnGrass) {
                if (Math.random() < 0.3) { // 30% chance to gain energy
                    updateEnergyLevel(Math.min(MaxEnergy, ELvl + (0.1 + Math.random() * 0.75))); // Gain 0.1 to 0.75 energy
                }
            }

            // Standard energy loss over time
            if (Math.random() < 0.5) { // 50% chance to lose energy
                updateEnergyLevel(Math.max(0.0, ELvl - (0.05 + Math.random() * 0.3))); // Lose 0.05 to 0.3 energy
            }

            // Aging effects - decreasing energy and health as the goat gets very old
            if (tickAge > LIFESPAN) {
                // Additional energy drain for old age
                double ageFactor = (tickAge - LIFESPAN) / (double)(MAX_AGE - LIFESPAN);
                updateEnergyLevel(Math.max(0.0, ELvl - (0.05 * ageFactor))); // More energy loss based on age

                // Health deterioration with old age
                if (Math.random() < 0.1 * ageFactor) {
                    this.damage(this.getDamageSources().generic(), 0.5f * (float)ageFactor);
                }
            }

            // Check if energy is at max and regenerate health if not at max (only if not too old)
            if (ELvl == MaxEnergy && tickAge < LIFESPAN) {
                if (this.getHealth() < this.getMaxHealth()) {
                    this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + 0.5F)); // Regenerate 0.5 HP per second
                }
            }

            // Auto-breeding behavior (only for goats not too old)
            if (ELvl >= 90.0 && !isBaby() && ticksSinceLastBreeding >= breedingCooldown && tickAge < LIFESPAN) {
                double searchRadius = 32.0;

                List<CustomGoatEntity> mateCandidates = this.getWorld().getEntitiesByClass(
                        CustomGoatEntity.class,
                        this.getBoundingBox().expand(searchRadius),
                        candidate -> candidate != this && candidate.getEnergyLevel() >= 90.0 && !candidate.isBaby()
                );

                // Find the nearest candidate
                CustomGoatEntity nearestMate = null;
                double minDistanceSquared = Double.MAX_VALUE;
                for (CustomGoatEntity candidate : mateCandidates) {
                    double distSq = this.squaredDistanceTo(candidate);
                    if (distSq < minDistanceSquared) {
                        minDistanceSquared = distSq;
                        nearestMate = candidate;
                    }
                }

                // If we found a mate candidate, move towards it
                if (nearestMate != null) {
                    // Start moving towards the nearest goat
                    this.getNavigation().startMovingTo(nearestMate, this.Speed * 5.0F * (this.ELvl / MaxEnergy));

                    // If close enough (within 2 blocks)
                    if (minDistanceSquared < 4.0) {
                        // Only start breeding if both goats are not already in love
                        if (!this.isInLove() && !nearestMate.isInLove()) {
                            this.setLoveTicks(500);
                            nearestMate.setLoveTicks(500);
                            ticksSinceLastBreeding = 0;
                        }
                    }
                }
            }
            ticksSinceLastBreeding++;

            // If energy reaches 0, kill the goat
            if (ELvl <= 0.0) {
                this.kill(); // This makes the goat die
            } else {
                // Update attributes dynamically if energy is greater than 0
                if (panicTicks == 0) { // Only update speed if not in panic mode
                    double speedModifier = ELvl / MaxEnergy;

                    // Reduce speed for older goats
                    if (tickAge > LIFESPAN) {
                        double ageFactor = 1.0 - (0.5 * (tickAge - LIFESPAN) / (MAX_AGE - LIFESPAN));
                        speedModifier *= ageFactor;
                    }

                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(Speed * speedModifier);
                }

                // Update the description with the new energy level
                updateDescription(this);
            }
        }
    }

    @Override
    public CustomGoatEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof CustomGoatEntity)) {
            return (CustomGoatEntity) EntityType.GOAT.create(serverWorld);
        }

        CustomGoatEntity parent1 = this;
        CustomGoatEntity parent2 = (CustomGoatEntity) mate;

        // Calculate inheritance factor based on parents' energy levels
        double inheritanceFactor = Math.min(parent1.ELvl, parent2.ELvl) / MaxEnergy;

        MobAttributes attr1 = parent1.mobAttributes;
        MobAttributes attr2 = parent2.mobAttributes;

        MobAttributes childAttributes = inheritAttributes(attr1, attr2);

        CustomGoatEntity child = new CustomGoatEntity(ModEntities.CUSTOM_GOAT, serverWorld);

        child.mobAttributes = childAttributes;
        applyAttributes(child, childAttributes);

        // Apply energy-based inheritance factor
        child.MaxHp = childAttributes.getMaxHealth() * inheritanceFactor;
        child.ELvl = childAttributes.getEnergyLvl() * inheritanceFactor;
        child.breedingCooldown = (int)(((parent1.breedingCooldown + parent2.breedingCooldown) / 2) * (1 / inheritanceFactor));

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
                .setBaseValue(Speed * (ELvl / MaxEnergy) * PANIC_SPEED_MULTIPLIER);
        if (!this.getWorld().isClient)
            updateDescription(this);
    }

    @Override
    public void applyCustomAttributes(MobAttributes attributes) {
    }
}