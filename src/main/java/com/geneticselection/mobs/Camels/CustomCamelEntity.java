package com.geneticselection.mobs.Camels;

import com.geneticselection.attributes.AttributeCarrier;
import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.utils.DescriptionRenderer;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public class CustomCamelEntity extends CamelEntity implements AttributeCarrier {
    private MobAttributes mobAttributes;
    private double MaxHp;
    private double ELvl;
    private double MaxEnergy = 100.0; // Maximum energy level
    private double Speed;
    private double MinMeat;
    private double MaxMeat;
    private double MinLeather;
    private double MaxLeather;
    private int breedingCooldown;
    private static final int LIFESPAN = 50000; // Camels live longer than cows
    private static final int MAX_AGE = 65000; // After this age, camels will die naturally

    private int panicTicks = 0;
    private static final int PANIC_DURATION = 100;
    private static final double PANIC_SPEED_MULTIPLIER = 1.15; // Camels don't panic as much as cows
    private boolean wasRecentlyHit = false;
    private int tickAge = 0;
    private int ticksSinceLastBreeding = 0;
    private int waterReserve = 0; // Camels store water
    private static final int MAX_WATER_RESERVE = 10000; // Maximum water storage
    private boolean isSitting = false;
    private int sittingTicks = 0;
    private int humpSize = 50; // Camel hump size (affects water storage and energy retention)

    public CustomCamelEntity(EntityType<? extends CamelEntity> entityType, World world) {
        super(entityType, world);

        // Initialize mob attributes
        if (this.mobAttributes == null) {
            MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
            double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
            double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
            double energy = global.getEnergyLvl() * (0.9 + Math.random() * 0.1);
            double meat = global.getMaxMeat().orElse(0.0) + (0.98 + Math.random() * 0.1);
            double leather = global.getMaxLeather().orElse(0.0) * (0.98 + Math.random() * 0.1);
            this.mobAttributes = new MobAttributes(speed, health, energy, Optional.of(meat), Optional.of(leather), Optional.empty(), Optional.empty(), Optional.empty());
            this.tickAge = 0;
            this.waterReserve = (int)(MAX_WATER_RESERVE * 0.5); // Start with half water reserve
            this.humpSize = 50 + (int)(Math.random() * 50); // Random initial hump size
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
        this.breedingCooldown = 5000 + (int)((1 - (ELvl / 100.0)) * 3000) + random.nextInt(2001);

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

    private void updateDescription(CustomCamelEntity ent) {
        DescriptionRenderer.setDescription(ent, Text.of("Attributes\n" +
                "Max Hp: " + String.format("%.1f", ent.getHealth()) + "/" + String.format("%.1f", ent.MaxHp) +
                "\nSpeed: " + String.format("%.2f", ent.Speed) +
                "\nEnergy: " + String.format("%.1f", ent.ELvl) + "/" + String.format("%.1f", ent.MaxEnergy) +
                "\nWater Reserve: " + waterReserve + "/" + MAX_WATER_RESERVE +
                "\nHump Size: " + humpSize + "/100" +
                "\nMax Meat: " + String.format("%.1f", ent.MaxMeat) +
                "\nMax Leather: " + String.format("%.1f", ent.MaxLeather) +
                "\nBreeding Cooldown: " + ent.breedingCooldown +
                "\nAge: " + ent.tickAge + "/" + MAX_AGE +
                (isSitting ? "\nSitting" : "")));
    }

    // Add this getter for energy level
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

    // Toggle sitting state
    public void setSitting(boolean sitting) {
        this.isSitting = sitting;
        if (isSitting) {
            sittingTicks = 0;
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        ItemStack offHandStack = player.getOffHandStack();
        boolean isHayBlock = itemStack.isOf(Items.HAY_BLOCK) || offHandStack.isOf(Items.HAY_BLOCK);
        boolean isWaterBottle = itemStack.isOf(Items.WATER_BUCKET) || offHandStack.isOf(Items.WATER_BUCKET);
        boolean isCactus = itemStack.isOf(Items.CACTUS) || offHandStack.isOf(Items.CACTUS);
        boolean isEmptyHand = itemStack.isEmpty() && offHandStack.isEmpty();

        // Handle water bottle for hydration
        if (isWaterBottle) {
            Hand usedHand = itemStack.isOf(Items.WATER_BUCKET) ? hand : Hand.OFF_HAND;
            ItemStack usedItem = itemStack.isOf(Items.WATER_BUCKET) ? itemStack : offHandStack;

            if (waterReserve < MAX_WATER_RESERVE) {
                waterReserve = Math.min(MAX_WATER_RESERVE, waterReserve + 1000);
                player.sendMessage(Text.of("The camel's water reserve has been replenished! Current: " + waterReserve + "/" + MAX_WATER_RESERVE), true);

                if (!player.isCreative()) {
                    // Replace water bottle with glass bottle
                    ItemStack glassBottle = new ItemStack(Items.GLASS_BOTTLE);
                    if (usedHand == Hand.MAIN_HAND) {
                        player.setStackInHand(Hand.MAIN_HAND, glassBottle);
                    } else {
                        player.setStackInHand(Hand.OFF_HAND, glassBottle);
                    }
                }

                updateDescription(this);
                return ActionResult.SUCCESS;
            } else {
                player.sendMessage(Text.of("The camel's water reserve is already full!"), true);
                return ActionResult.PASS;
            }
        }

        // Handle cactus for special camel treat
        if (isCactus) {
            Hand usedHand = itemStack.isOf(Items.CACTUS) ? hand : Hand.OFF_HAND;
            ItemStack usedItem = itemStack.isOf(Items.CACTUS) ? itemStack : offHandStack;

            // Camels can eat cacti for a temporary boost to hump size
            if (humpSize < 100) {
                humpSize = Math.min(100, humpSize + 5 + random.nextInt(6));
                player.sendMessage(Text.of("The camel's hump has grown! Current size: " + humpSize + "/100"), true);

                if (!player.isCreative()) {
                    usedItem.decrement(1);
                }

                updateDescription(this);
                return ActionResult.SUCCESS;
            } else {
                player.sendMessage(Text.of("The camel's hump is already at maximum size!"), true);
                return ActionResult.PASS;
            }
        }

        // Handle hay block for feeding/breeding
        if (isHayBlock) {
            Hand usedHand = itemStack.isOf(Items.HAY_BLOCK) ? hand : Hand.OFF_HAND;
            ItemStack usedItem = itemStack.isOf(Items.HAY_BLOCK) ? itemStack : offHandStack;

            if (this.isBaby()) {
                return ActionResult.PASS; // Do nothing if the camel is a baby
            }

            // If the camel is in love mode
            if (this.isInLove()) {
                if (ELvl < MaxEnergy) {
                    updateEnergyLevel(Math.min(MaxEnergy, ELvl + 10.0)); // Gain energy (up to max)
                    player.sendMessage(Text.of("The camel has gained energy! Current energy: " + String.format("%.1f", ELvl)), true);

                    if (!player.isCreative()) { // Only consume hay if the player is NOT in Creative mode
                        usedItem.decrement(1);
                    }

                    updateDescription(this);
                    return ActionResult.SUCCESS;
                } else {
                    // Camel is in love mode and at max energy; do nothing
                    player.sendMessage(Text.of("The camel is already at maximum energy!"), true);
                    return ActionResult.PASS;
                }
            }

            if (ELvl < 20.0 || waterReserve < 1000 || humpSize < 30) {
                if (ELvl < 20.0) {
                    updateEnergyLevel(Math.min(MaxEnergy, ELvl + 10.0)); // Gain energy (up to max)
                    player.sendMessage(Text.of("This camel cannot breed due to low energy. Energy increased to: " + String.format("%.1f", ELvl)), true);
                } else if (waterReserve < 1000) {
                    player.sendMessage(Text.of("This camel cannot breed due to low water reserves."), true);
                } else {
                    player.sendMessage(Text.of("This camel cannot breed due to small hump size."), true);
                    humpSize = Math.min(100, humpSize + 5);
                }

                if (!player.isCreative()) { // Only consume hay if the player is NOT in Creative mode
                    usedItem.decrement(1);
                }

                updateDescription(this);
                return ActionResult.SUCCESS;
            } else {
                // If energy, water, and hump size are sufficient, trigger breeding
                this.lovePlayer(player);
                player.sendMessage(Text.of("The camel is now in breed mode!"), true);

                if (!player.isCreative()) { // Only consume hay if the player is NOT in Creative mode
                    usedItem.decrement(1);
                }

                updateDescription(this);
                return ActionResult.SUCCESS;
            }
        }

        // Toggle sitting with empty hand (right-click) if camel is not a baby
        if (isEmptyHand && !this.isBaby() && !this.hasPassengers()) {
            if (!this.getWorld().isClient) {
                // Toggle sitting state
                setSitting(!isSitting);
                player.sendMessage(Text.of("The camel is now " + (isSitting ? "sitting" : "standing") + "."), true);
                updateDescription(this);
            }
            return ActionResult.SUCCESS;
        }

        // Other interactions (e.g., mounting, etc.)
        return super.interactMob(player, hand);
    }

    @Override
    protected void applyDamage(DamageSource source, float amount) {
        super.applyDamage(source, amount);

        // Mark the camel as recently hit
        wasRecentlyHit = true;

        // Start panic mode
        panicTicks = PANIC_DURATION;

        // Increase speed temporarily
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                .setBaseValue(Speed * (ELvl / MaxEnergy) * PANIC_SPEED_MULTIPLIER);

        // Force camel to stand if it was sitting
        if (isSitting) {
            setSitting(false);
        }

        if (!this.getWorld().isClient) {
            updateDescription(this);
        }
    }

    @Override
    public void growUp(int age, boolean overGrow) {
        int currentAge = this.getBreedingAge();
        int newAge = currentAge + age; // Increment age by provided value

        // Ensure camel reaches adulthood when age hits 0 (negative age counting)
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
            super.onDeath(source);
            return;
        }

        super.onDeath(source);

        if (!this.getWorld().isClient) {
            // Determine drop quantities based on energy level and age
            int meatAmount = (int) (MaxMeat * (ELvl / MaxEnergy));
            int leatherAmount = (int) (MaxLeather * (ELvl / MaxEnergy));

            // Reduce drops if the camel died of old age
            if (tickAge >= MAX_AGE) {
                meatAmount = Math.max(1, meatAmount / 2);
                leatherAmount = Math.max(1, leatherAmount / 2);

                // Always drop bones for old age death
                this.dropStack(new ItemStack(Items.BONE, 2 + random.nextInt(3)));
            }

            boolean shouldDropCooked = false;

            // Check if the entity died from fire, lava, or burning
            if (source.getName().equals("onFire") || source.getName().equals("inFire") || source.getName().equals("lava")) {
                shouldDropCooked = true;
            }

            // Check if the attacker has Fire Aspect
            if (source.getAttacker() instanceof LivingEntity attacker) {
                ItemStack weapon = attacker.getMainHandStack();
                RegistryEntry<Enchantment> fireAspectEntry = this.getWorld().getServer().getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Enchantments.FIRE_ASPECT).get();
                if (EnchantmentHelper.getLevel(fireAspectEntry, weapon) >= 1) {
                    shouldDropCooked = true;
                }
            }

            // Drop meat and leather
            if (shouldDropCooked) {
                this.dropStack(new ItemStack(Items.COOKED_BEEF, meatAmount)); // No camel meat in Minecraft, so using beef
            } else {
                this.dropStack(new ItemStack(Items.BEEF, meatAmount)); // No camel meat in Minecraft, so using beef
            }

            this.dropStack(new ItemStack(Items.LEATHER, leatherAmount));

            // Special drop: saddle if camel was high quality and didn't die of old age
            if (humpSize > 80 && ELvl > 80 && tickAge < MAX_AGE && Math.random() < 0.2) {
                this.dropStack(new ItemStack(Items.SADDLE, 1));
            }
        }
    }

    @Override
    public CustomCamelEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof CustomCamelEntity))
            return (CustomCamelEntity) EntityType.CAMEL.create(serverWorld);

        CustomCamelEntity parent1 = this;
        CustomCamelEntity parent2 = (CustomCamelEntity) mate;

        // Calculate the inheritance factor based on energy, water reserves, and hump size
        double parent1Factor = (parent1.ELvl / MaxEnergy) * (parent1.waterReserve / (double)MAX_WATER_RESERVE) * (parent1.humpSize / 100.0);
        double parent2Factor = (parent2.ELvl / MaxEnergy) * (parent2.waterReserve / (double)MAX_WATER_RESERVE) * (parent2.humpSize / 100.0);
        double inheritanceFactor = Math.min(1.0, (parent1Factor + parent2Factor) / 2.0);

        // Inherit attributes from parents with some randomness, scaled by the inheritance factor
        double childMaxHp = ((parent1.MaxHp + parent2.MaxHp) / 2) * (inheritanceFactor * 0.9 + Math.random() * 0.2);
        double childSpeed = ((parent1.Speed + parent2.Speed) / 2) * (inheritanceFactor * 0.9 + Math.random() * 0.2);
        double childMinMeat = ((parent1.MinMeat + parent2.MinMeat) / 2) * inheritanceFactor;
        double childMaxMeat = ((parent1.MaxMeat + parent2.MaxMeat) / 2) * (inheritanceFactor * 0.9 + Math.random() * 0.2);
        double childMinLeather = ((parent1.MinLeather + parent2.MinLeather) / 2) * inheritanceFactor;
        double childMaxLeather = ((parent1.MaxLeather + parent2.MaxLeather) / 2) * (inheritanceFactor * 0.9 + Math.random() * 0.2);
        int childBreedingCooldown = (int) (((parent1.breedingCooldown + parent2.breedingCooldown) / 2) * (1 / inheritanceFactor));
        double childEnergy = ((parent1.ELvl + parent2.ELvl) / 2) * inheritanceFactor;
        int childHumpSize = (int)(((parent1.humpSize + parent2.humpSize) / 2) * inheritanceFactor);

        // Create the child entity
        CustomCamelEntity child = new CustomCamelEntity(ModEntities.CUSTOM_CAMEL, serverWorld);

        // Set inherited attributes
        child.MaxHp = childMaxHp;
        child.Speed = childSpeed;
        child.MinMeat = childMinMeat;
        child.MaxMeat = childMaxMeat;
        child.MinLeather = childMinLeather;
        child.MaxLeather = childMaxLeather;
        child.breedingCooldown = childBreedingCooldown;
        child.ELvl = childEnergy;
        child.waterReserve = (int)(Math.min(parent1.waterReserve, parent2.waterReserve) * 0.5); // Child starts with 50% of the lower parent's water
        child.humpSize = childHumpSize;

        // Apply stats to the child entity
        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.MaxHp);
        child.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(child.Speed * (child.ELvl / MaxEnergy));

        // Parents lose energy and water after breeding
        parent1.ELvl -= parent1.ELvl * 0.4F;
        parent2.ELvl -= parent2.ELvl * 0.4F;
        parent1.waterReserve -= parent1.waterReserve * 0.3;
        parent2.waterReserve -= parent2.waterReserve * 0.3;
        parent1.humpSize = Math.max(30, parent1.humpSize - 15);
        parent2.humpSize = Math.max(30, parent2.humpSize - 15);

        this.resetLoveTicks();

        if (!this.getWorld().isClient)
            updateDescription(child);

        return child;
    }

    @Override
    public void tick() {
        super.tick();

        // Only perform energy and water adjustments on the server side
        if (!this.getWorld().isClient) {
            // Increment age
            tickAge++;

            // Age-based death: die of old age when MAX_AGE is reached
            if (tickAge >= MAX_AGE) {
                // Die of old age
                this.damage(this.getDamageSources().starve(), 1.0F);
                return;
            }

            // Max energy is determined by age
            if (tickAge <= 6000) {
                MaxEnergy = 10 * Math.log(5 * tickAge + 5);
            } else if (tickAge > 6000 && tickAge < LIFESPAN) {
                MaxEnergy = 100;
            } else {
                MaxEnergy = -(tickAge - LIFESPAN) / 16.0 + 100;
                // Ensure MaxEnergy doesn't go below 0
                MaxEnergy = Math.max(0, MaxEnergy);
            }

            // Grow up baby camel after certain age
            if (tickAge >= 6000 && this.isBaby()) {
                growUp(220, true);
            }

            // Clamp energy level to maximum
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

            // Handle energy loss if the camel was recently hit
            if (wasRecentlyHit) {
                // Reduce energy by 15% of its current level (camels are more resilient than cows)
                updateEnergyLevel(Math.max(0.0, ELvl * 0.85));
                wasRecentlyHit = false; // Reset the flag after applying the energy loss
            }

            // Special camel mechanics for sitting
            if (isSitting) {
                sittingTicks++;

                // While sitting, regenerate energy faster (based on hump size)
                if (sittingTicks % 20 == 0) { // Every second
                    double energyGain = 0.3 + (0.5 * (humpSize / 100.0)); // Larger humps provide more energy while sitting
                    updateEnergyLevel(Math.min(MaxEnergy, ELvl + energyGain));
                }

                // Camels conserve water while sitting (based on hump size)
                if (sittingTicks % 100 == 0) { // Every 5 seconds
                    int waterConservation = 5 + (int)((humpSize / 100.0) * 10); // Larger humps conserve more water
                    waterReserve = Math.min(MAX_WATER_RESERVE, waterReserve + waterConservation);
                }

                // Slowly regenerate hump size while sitting
                if (sittingTicks % 1200 == 0 && humpSize < 100) { // Every minute
                    humpSize = Math.min(100, humpSize + 1);
                }
            } else {
                // Regular water consumption while active (affected by hump size)
                if (tickAge % 200 == 0) { // Every 10 seconds
                    int waterLoss = 10 - (int)((humpSize / 100.0) * 5); // Larger humps reduce water loss
                    waterReserve = Math.max(0, waterReserve - Math.max(1, waterLoss));
                }

                // Hump size slowly decreases when not sitting
                if (tickAge % 3600 == 0 && humpSize > 30) { // Every 3 minutes
                    humpSize--;
                }
            }

            // Aging effects - modify attributes as camel gets older
            if (tickAge > LIFESPAN) {
                // Calculate aging factor (0 to 1) based on how far past LIFESPAN
                double agingFactor = (double)(tickAge - LIFESPAN) / (MAX_AGE - LIFESPAN);

                // Decrease energy faster with age
                if (Math.random() < 0.3 * agingFactor) {
                    updateEnergyLevel(Math.max(0.0, ELvl - (0.1 + 0.2 * agingFactor)));
                }

                // Decrease hump size with age (less efficient metabolism)
                if (tickAge % 2400 == 0 && humpSize > 20) { // Every 2 minutes for older camels
                    humpSize--;
                }

                // Water reserves deplete faster with age
                if (tickAge % (200 - (int)(100 * agingFactor)) == 0) {
                    waterReserve = Math.max(0, waterReserve - (int)(3 * agingFactor));
                }

                // Occasional health deterioration with old age
                if (Math.random() < 0.05 * agingFactor) {
                    this.damage(this.getDamageSources().generic(), 0.5f * (float)agingFactor);
                }
            }

            // Check if the camel is standing on sand or sandstone (desert biome blocks)
            boolean isOnSand = this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.SAND) ||
                    this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.SANDSTONE) ||
                    this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.RED_SAND);

            // Check if the camel is in water
            boolean isInWater = this.isSubmergedInWater();

            // Adjust energy level based on environment
            if (isOnSand && !isSitting) {
                if (Math.random() < 0.3) { // 30% chance to gain energy on sand
                    updateEnergyLevel(Math.min(MaxEnergy, ELvl + (0.2 + Math.random() * 0.5))); // Gain 0.2 to 0.7 energy
                }
            }

            if (isInWater) {
                // Replenish water reserves when in water
                waterReserve = Math.min(MAX_WATER_RESERVE, waterReserve + 100);

                // But reduce energy slightly while in water (camels don't like swimming)
                if (Math.random() < 0.5) {
                    updateEnergyLevel(Math.max(0.0, ELvl - 0.5));
                }
            }

            // Regular energy consumption if not sitting
            if (!isSitting && Math.random() < 0.3) { // 30% chance to lose energy
                double energyLoss = (0.03 + Math.random() * 0.2) * (1.0 - (humpSize / 200.0)); // Larger humps reduce energy loss
                updateEnergyLevel(Math.max(0.0, ELvl - energyLoss));
            }

            // Check if energy and water are high for health regeneration
            if (ELvl > 90.0 && waterReserve > MAX_WATER_RESERVE * 0.8 && tickAge < LIFESPAN) {
                if (this.getHealth() < this.getMaxHealth()) {
                    this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + 0.5F)); // Regenerate 0.5 HP per tick
                }
            }

            // Auto-breeding behavior when optimal conditions are met
            if (ELvl >= 90.0 && waterReserve >= MAX_WATER_RESERVE * 0.7 && humpSize >= 70 && !isBaby() && ticksSinceLastBreeding >= breedingCooldown && tickAge < LIFESPAN) {
                double searchRadius = 32.0;

                List<CustomCamelEntity> mateCandidates = this.getWorld().getEntitiesByClass(
                        CustomCamelEntity.class,
                        this.getBoundingBox().expand(searchRadius),
                        candidate -> candidate != this && candidate.getEnergyLevel() >= 90.0 && !candidate.isBaby()
                );

                // Find the nearest candidate
                CustomCamelEntity nearestMate = null;
                double minDistanceSquared = Double.MAX_VALUE;
                for (CustomCamelEntity candidate : mateCandidates) {
                    double distSq = this.squaredDistanceTo(candidate);
                    if (distSq < minDistanceSquared) {
                        minDistanceSquared = distSq;
                        nearestMate = candidate;
                    }
                }

                // If we found a mate candidate, move towards it
                if (nearestMate != null) {
                    // Stop sitting if we're going to breed
                    if (isSitting) {
                        setSitting(false);
                    }

                    // Start moving towards the nearest camel
                    this.getNavigation().startMovingTo(nearestMate, this.Speed * 3.0F * (this.ELvl / MaxEnergy));

                    // If close enough (within 3 blocks)
                    if (minDistanceSquared < 9.0) {
                        // Only start breeding if both camels are not already in love
                        if (!this.isInLove() && !nearestMate.isInLove()) {
                            this.setLoveTicks(600);
                            nearestMate.setLoveTicks(600);
                            ticksSinceLastBreeding = 0;
                        }
                    }
                }
            }
            ticksSinceLastBreeding++;

            // If energy or water reaches critical levels, kill the camel
            if (ELvl <= 0.0 || (waterReserve <= 0 && tickAge % 1000 == 0)) {
                this.damage(this.getDamageSources().starve(), 1.0F); // Use starve damage instead of instant kill
            } else {
                // Update movement speed based on energy if not in panic mode
                if (panicTicks == 0 && !isSitting) {
                    double energyFactor = ELvl / MaxEnergy;
                    double waterFactor = Math.max(0.5, waterReserve / (double)MAX_WATER_RESERVE);
                    double humpFactor = 0.8 + (0.4 * (humpSize / 100.0)); // Larger humps provide better movement efficiency

                    // Apply age factor to reduce movement speed for older camels
                    double ageFactor = 1.0;
                    if (tickAge > LIFESPAN) {
                        ageFactor = 1.0 - (0.5 * (tickAge - LIFESPAN) / (MAX_AGE - LIFESPAN));
                    }

                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(Speed * energyFactor * waterFactor * humpFactor * ageFactor);
                } else if (isSitting) {
                    // Completely stop movement while sitting
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(0);
                }

                // Update the description with the new stats
                updateDescription(this);
            }
        }
    }

    @Override
    public void applyCustomAttributes(MobAttributes attributes) {
        // This is from the AttributeCarrier interface
        // We can optionally add custom implementation here
    }
}