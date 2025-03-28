package com.geneticselection.mobs.Goat;

import com.geneticselection.attributes.AttributeCarrier;
import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.utils.DescriptionRenderer;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
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

public class CustomGoatEntity extends GoatEntity implements AttributeCarrier {
    private MobAttributes mobAttributes;
    private double MaxHp;
    private double Speed;
    private double ELvl;
    private double MaxEnergy = 100.0F;
    private double MinMeat;
    private double MaxMeat;
    private double MinLeather;
    private double MaxLeather;

    private int panicTicks = 0;
    private static final int PANIC_DURATION = 100; // 5 seconds at 20 ticks per second
    private static final double PANIC_SPEED_MULTIPLIER = 1.5;
    private boolean wasRecentlyHit = false;
    private boolean isRamming = false;
    private static final double RAM_ENERGY_COST = 15.0; // Energy cost for ramming

    public CustomGoatEntity(EntityType<? extends GoatEntity> entityType, World world) {
        super(entityType, world);

        if (this.mobAttributes == null) {
            MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
            double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
            double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
            double energy = global.getEnergyLvl() * (0.9 + Math.random() * 0.1);
            double meat = global.getMaxMeat().orElse(0.0) * (0.98 + Math.random() * 0.1);
            double leather = global.getMaxLeather().orElse(0.0) * (0.98 + Math.random() * 0.1);
            this.mobAttributes = new MobAttributes(speed, health, energy, Optional.of(meat), Optional.of(leather), Optional.empty(), Optional.empty(), Optional.empty());
        }

        // Apply attributes to the entity
        this.MaxHp = this.mobAttributes.getMaxHealth();
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
        this.setHealth((float)this.MaxHp);
        this.Speed = this.mobAttributes.getMovementSpeed();
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);
        this.ELvl = this.mobAttributes.getEnergyLvl();

        this.mobAttributes.getMaxMeat().ifPresent(maxMeat -> {
            this.MaxMeat = maxMeat;
        });
        this.mobAttributes.getMaxLeather().ifPresent(maxLeather -> {
            this.MaxLeather = maxLeather;
        });
        this.setMinMeat(1.0);
        this.setMinLeather(0.0);

        if (!this.getWorld().isClient) {
            updateDescription(this);
        }
    }

    public void updateEnergyLevel(double newEnergyLevel) {
        this.ELvl = newEnergyLevel;

        // If the energy level changes, notify the renderer to update.
        if (this.getWorld().isClient) {
            // In case this is client-side, trigger a re-render.
            this.markForRenderUpdate();
        }

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

    public void markForRenderUpdate() {
        // This triggers the renderer to update the texture the next time it's rendered
        MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(this).render(this, 0, 0, new MatrixStack(), MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers(), 0);
    }

    private void updateDescription(CustomGoatEntity ent) {
        DescriptionRenderer.setDescription(ent, Text.of("Attributes\n" +
                "Max Hp: " + String.format("%.1f", ent.getHealth()) + "/" + String.format("%.1f", ent.MaxHp) +
                "\nSpeed: " + String.format("%.2f", ent.Speed) +
                "\nEnergy: " + String.format("%.1f", ent.ELvl) +
                "\nMax Meat: " + String.format("%.1f", ent.MaxMeat) +
                "\nMax Leather: " + String.format("%.1f", ent.MaxLeather)));
    }

    public double getEnergyLevel() {
        return this.ELvl;
    }

    public void setMinMeat(double minMeat) {
        this.MinMeat = minMeat;
    }

    public void setMaxMeat(double maxMeat) {
        this.MaxMeat = maxMeat;
    }

    public void setMinLeather(double minLeather) {
        this.MinLeather = minLeather;
    }

    public void setMaxLeather(double maxLeather) {
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
                return ActionResult.PASS; // Do nothing if the goat is a baby
            }

            // If the goat is in love mode
            if (this.isInLove()) {
                if (ELvl < MaxEnergy) {
                    updateEnergyLevel(Math.min(MaxEnergy, ELvl + 10.0)); // Gain energy (up to max 100)
                    player.sendMessage(Text.of("The goat has gained energy! Current energy: " + String.format("%.1f", ELvl)), true);

                    if (!player.isCreative()) { // Only consume wheat if the player is NOT in Creative mode
                        usedItem.decrement(1);
                    }

                    updateDescription(this); // Update description with new energy level
                    return ActionResult.SUCCESS;
                } else {
                    // Goat is in love mode and at max energy; do nothing
                    player.sendMessage(Text.of("The goat is already at maximum energy!"), true);
                    return ActionResult.PASS;
                }
            }

            if (ELvl < 20.0) {
                updateEnergyLevel(Math.min(MaxEnergy, ELvl + 10.0)); // Gain energy (up to max 100)
                player.sendMessage(Text.of("This goat cannot breed due to low energy. Energy increased to: " + String.format("%.1f", ELvl)), true);

                if (!player.isCreative()) { // Only consume wheat if the player is NOT in Creative mode
                    usedItem.decrement(1);
                }

                updateDescription(this); // Update description with new energy level
                return ActionResult.SUCCESS;
            } else {
                // If energy is sufficient, trigger breeding
                this.lovePlayer(player);
                player.sendMessage(Text.of("The goat is now in breed mode!"), true);

                if (!player.isCreative()) { // Only consume wheat if the player is NOT in Creative mode
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

        // Handle other interactions
        return super.interactMob(player, hand);
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);

        if (!this.getWorld().isClient && !this.isBaby()) {
            int meatAmount = (int) (MaxMeat);
            int leatherAmount = (int) (MaxLeather);

            boolean shouldDropCooked = false;

            // Check if the entity died from fire, lava, or burning
            if (source.getName().equals("onFire") || source.getName().equals("inFire") || source.getName().equals("lava")) {
                shouldDropCooked = true;
            }

            // Drop cooked or raw meat based on conditions
            if (shouldDropCooked) {
                this.dropStack(new ItemStack(Items.COOKED_MUTTON, meatAmount));
            } else {
                this.dropStack(new ItemStack(Items.MUTTON, meatAmount));
            }

            // Drop leather
            this.dropStack(new ItemStack(Items.LEATHER, leatherAmount));
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Only perform energy adjustments on the server side
        if (!this.getWorld().isClient) {
            if (MaxEnergy > 0.0) {
                double t = MaxEnergy / 100.0; // Normalized progress (1 -> 0)
                MaxEnergy -= (100.0 / 10000.0) * t * t; // Quadratic decay (ease-in)
                if (MaxEnergy < 0.0) MaxEnergy = 0.0; // Ensure it never goes negative
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

            // Check if ramming and apply energy cost
            if (isRamming) {
                // Reduce energy when ramming
                updateEnergyLevel(Math.max(0.0, ELvl - RAM_ENERGY_COST));
                isRamming = false; // Reset ramming flag after applying cost
            }

            // Check if the goat is standing on stone or dirt (modified from grass for cow)
            boolean isOnStone = this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.STONE) ||
                    this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.COBBLESTONE) ||
                    this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.ANDESITE) ||
                    this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.GRANITE) ||
                    this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.DIORITE);

            boolean isOnDirt = this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.DIRT) ||
                    this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.COARSE_DIRT) ||
                    this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.DIRT_PATH);

            // Adjust energy level randomly based on whether the goat is on stone or dirt
            if (isOnStone || isOnDirt) {
                if (Math.random() < 0.2) { // 20% chance to gain energy
                    updateEnergyLevel(Math.min(MaxEnergy, ELvl + (0.01 + Math.random() * 0.19))); // Gain 0.01 to 0.2 energy
                }
            } else {
                if (Math.random() < 0.5) { // 50% chance to lose energy
                    updateEnergyLevel(Math.max(0.0, ELvl - (0.01 + Math.random() * 0.19))); // Lose 0.01 to 0.2 energy
                }
            }

            // Check if energy is 100 and regenerate health if not at max
            if (ELvl == MaxEnergy) {
                if (this.getHealth() < this.getMaxHealth()) {
                    this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + 0.5F)); // Regenerate 0.5 HP per second
                }
            }

            // Automatic breeding at high energy levels
            if (ELvl >= 90.0) {
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
                    // Start moving towards the nearest goat; adjust speed as needed
                    this.getNavigation().startMovingTo(nearestMate, this.Speed * 5.0F * (this.ELvl / MaxEnergy));

                    // If close enough (e.g., within 2 blocks; adjust the threshold as needed)
                    if (minDistanceSquared < 4.0) {
                        // Only start breeding if both goats are not already in love
                        if (!this.isInLove() && !nearestMate.isInLove()) {
                            this.setLoveTicks(100);
                            nearestMate.setLoveTicks(100);
                        }
                    }
                }
            }

            // If energy reaches 0, kill the goat
            if (ELvl <= 0.0) {
                this.kill(); // This makes the goat die
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

    @Override
    public CustomGoatEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof CustomGoatEntity)) {
            return (CustomGoatEntity) EntityType.GOAT.create(serverWorld);
        }

        CustomGoatEntity parent1 = this;
        CustomGoatEntity parent2 = (CustomGoatEntity) mate;

        // Calculate the inheritance factor based on the lower energy level of the parents
        double inheritanceFactor = Math.min(parent1.ELvl, parent2.ELvl) / MaxEnergy;

        // Inherit attributes from parents, scaled by the inheritance factor
        double childMaxHp = ((parent1.MaxHp + parent2.MaxHp) / 2) * inheritanceFactor;
        double childMinMeat = ((parent1.MinMeat + parent2.MinMeat) / 2) * inheritanceFactor;
        double childMaxMeat = ((parent1.MaxMeat + parent2.MaxMeat) / 2) * inheritanceFactor;
        double childMinLeather = ((parent1.MinLeather + parent2.MinLeather) / 2) * inheritanceFactor;
        double childMaxLeather = ((parent1.MaxLeather + parent2.MaxLeather) / 2) * inheritanceFactor;
        double childEnergy = ((parent1.ELvl + parent2.ELvl) / 2) * inheritanceFactor;

        // Create the child entity
        CustomGoatEntity child = new CustomGoatEntity(ModEntities.CUSTOM_GOAT, serverWorld);

        // Set inherited and calculated attributes
        child.MaxHp = childMaxHp;
        child.MinMeat = childMinMeat;
        child.MaxMeat = childMaxMeat;
        child.MinLeather = childMinLeather;
        child.MaxLeather = childMaxLeather;
        child.ELvl = childEnergy;

        // Apply stats to the child entity
        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.MaxHp);
        child.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(child.Speed * (child.ELvl / MaxEnergy));

        parent1.ELvl -= parent1.ELvl * 0.4F;
        parent2.ELvl -= parent2.ELvl * 0.4F;
        this.resetLoveTicks();

        if (!this.getWorld().isClient) {
            updateDescription(child);
        }

        return child;
    }

    @Override
    protected void applyDamage(DamageSource source, float amount) {
        super.applyDamage(source, amount);

        // Mark the goat as recently hit
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

    // Override the attack method to consume energy during rams
    @Override
    public boolean tryAttack(net.minecraft.entity.Entity target) {
        boolean didAttack = super.tryAttack(target);

        // If the attack was successful and goat is charging (ramming), mark for energy consumption
        if (didAttack && this.isCharging()) {
            updateEnergyLevel(Math.max(0.0, ELvl - RAM_ENERGY_COST));
            if (!this.getWorld().isClient) {
                updateDescription(this);
            }
        }

        return didAttack;
    }

    // Handle ram-related energy costs through other methods
    // We'll use the isCharging method to check when the goat is about to ram
    @Override
    public boolean isCharging() {
        boolean charging = super.isAttacking();

        // If the goat is charging but doesn't have enough energy, cancel the charging
        if (charging && ELvl < RAM_ENERGY_COST) {
            // We can't directly cancel charging, but we'll handle the energy deduction in tick()
            return false;
        }

        return charging;
    }

    @Override
    public void applyCustomAttributes(MobAttributes attributes) {
        // Required by AttributeCarrier interface, but we handle attributes directly
    }
}