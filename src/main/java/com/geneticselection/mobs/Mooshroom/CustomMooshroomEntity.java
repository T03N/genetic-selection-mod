package com.geneticselection.mobs.Mooshroom;
import com.geneticselection.mobs.Cows.CustomCowEntity;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;

import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;


import java.util.Optional;

import static com.geneticselection.genetics.ChildInheritance.*;

public class CustomMooshroomEntity extends MooshroomEntity {
    private MobAttributes mobAttributes; // Directly store MobAttributes for this entity
    private double MaxHp;
    private double ELvl;
    private double Speed;
    private double MinMeat;
    private double MaxMeat;
    private double MinLeather;
    private double MaxLeather;
    private int milkingCooldown;
    private long lastMilkTime = 0;
    private int panicTicks = 0;
    private static final int PANIC_DURATION = 100; // 5 seconds at 20 ticks per second
    private static final double PANIC_SPEED_MULTIPLIER = 1.25;
    private boolean wasRecentlyHit = false;

    public CustomMooshroomEntity(EntityType<? extends MooshroomEntity> entityType, World world) {
        super(entityType, world);

        // Initialize mob attributes (directly within the class)
        if (this.mobAttributes == null) {
            MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
            double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
            double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
            double energy = global.getEnergyLvl() * (0.9 + Math.random() * 0.1);
            double meat = global.getMaxMeat().orElse(0.0) + (0.98 + Math.random() * 0.1);
            double leather = global.getMaxLeather().orElse(0.0) * (0.98 + Math.random() * 0.1);
            this.mobAttributes = new MobAttributes(speed, health, energy, Optional.of(meat), Optional.of(leather),Optional.empty(),Optional.empty(), Optional.empty());
        }

        // Apply attributes to the entity
        this.MaxHp = this.mobAttributes.getMaxHealth();
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
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
        this.milkingCooldown = 3000 + (int)((1 - (ELvl / 100.0)) * 2000) + random.nextInt(2001);
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

    private void updateDescription(CustomMooshroomEntity ent) {
        DescriptionRenderer.setDescription(ent, Text.of("Attributes\n" +
                "Max Hp: " + String.format("%.3f", ent.getHealth()) + "/"+ String.format("%.3f", ent.MaxHp) +
                "\nSpeed: " + String.format("%.3f", ent.Speed) +
                "\nEnergy: " + String.format("%.3f", ent.ELvl) +
                "\nMax Meat: " + String.format("%.3f", ent.MaxMeat) +
                "\nMax Leather: " + String.format("%.3f", ent.MaxLeather)+
                "\nCooldown: " + ent.milkingCooldown));
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
    if (itemStack.isOf(Items.SHEARS)) {
        if (!this.getWorld().isClient) {
            // Create a new CustomCowEntity and transfer relevant attributes
            CustomCowEntity newCow = new CustomCowEntity(ModEntities.CUSTOM_COW, this.getWorld());
            newCow.copyPositionAndRotation(this);
            newCow.setHealth(this.getHealth());
            newCow.updateEnergyLevel(this.getEnergyLevel());
            newCow.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
            newCow.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);

            // Spawn the new cow
            this.getWorld().spawnEntity(newCow);

            // Drop mushrooms
            this.dropStack(new ItemStack(this.getVariant() == MooshroomEntity.Type.BROWN ? Items.BROWN_MUSHROOM : Items.RED_MUSHROOM, 5));

            // Remove the original Mooshroom
            this.discard();
        }
        return ActionResult.SUCCESS;
    }
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
                if (ELvl < 100.0) {
                    updateEnergyLevel(Math.min(100.0, ELvl + 10.0)); // Gain energy (up to max 100)
                    player.sendMessage(Text.of("The mooshroom has gained energy! Current energy: " + String.format("%.1f", ELvl)), true);

                    if (!player.isCreative()) { // Only consume wheat if the player is NOT in Creative mode
                        usedItem.decrement(1);
                    }

                    updateDescription(this); // Update description with new energy level
                    return ActionResult.SUCCESS;
                } else {
                    // Cow is in love mode and at max energy; do nothing
                    player.sendMessage(Text.of("The mooshroom is already at maximum energy!"), true);
                    return ActionResult.PASS;
                }
            }

            if (ELvl < 20.0) {
                updateEnergyLevel(Math.min(100.0, ELvl + 10.0)); // Gain energy (up to max 100)
                player.sendMessage(Text.of("This mooshroom cannot breed due to low energy. Energy increased to: " + String.format("%.1f", ELvl)), true);

                if (!player.isCreative()) { // Only consume wheat if the player is NOT in Creative mode
                    usedItem.decrement(1);
                }

                updateDescription(this); // Update description with new energy level
                return ActionResult.SUCCESS;
            } else {
                // If energy is sufficient, trigger breeding
                this.lovePlayer(player);
                player.sendMessage(Text.of("The mooshroom is now in breed mode!"), true);

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
                .setBaseValue(Speed * (ELvl / 100.0) * PANIC_SPEED_MULTIPLIER);

        if (!this.getWorld().isClient) {
            updateDescription(this);
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
    public CustomMooshroomEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof CustomMooshroomEntity))
            return (CustomMooshroomEntity) EntityType.MOOSHROOM.create(serverWorld);

        CustomMooshroomEntity parent1 = this;
        CustomMooshroomEntity parent2 = (CustomMooshroomEntity) mate;

        // Calculate the inheritance factor based on the lower energy level of the parents
        double inheritanceFactor = Math.min(parent1.ELvl, parent2.ELvl) / 100.0;

        // Inherit attributes from parents, scaled by the inheritance factor
        double childMaxHp = ((parent1.MaxHp + parent2.MaxHp) / 2) * inheritanceFactor;
        double childMinMeat = ((parent1.MinMeat + parent2.MinMeat) / 2) * inheritanceFactor;
        double childMaxMeat = ((parent1.MaxMeat + parent2.MaxMeat) / 2) * inheritanceFactor;
        double childMinLeather = ((parent1.MinLeather + parent2.MinLeather) / 2) * inheritanceFactor;
        double childMaxLeather = ((parent1.MaxLeather + parent2.MaxLeather) / 2) * inheritanceFactor;
        int childMilkingCooldown = (int) (((parent1.milkingCooldown + parent2.milkingCooldown) / 2) * (1 / inheritanceFactor));
        double childEnergy = ((parent1.ELvl + parent2.ELvl) / 2) * inheritanceFactor;

        // Create the child entity
        CustomMooshroomEntity child = new CustomMooshroomEntity(ModEntities.CUSTOM_MOOSHROOM, serverWorld);

        // Set inherited and calculated attributes
        child.MaxHp = childMaxHp;
        child.MinMeat = childMinMeat;
        child.MaxMeat = childMaxMeat;
        child.MinLeather = childMinLeather;
        child.MaxLeather = childMaxLeather;
        child.milkingCooldown = childMilkingCooldown;
        child.ELvl = childEnergy;

        // Apply stats to the child entity
        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.MaxHp);
        child.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(child.Speed * (child.ELvl / 100.0));

        if (!this.getWorld().isClient)
            updateDescription(child);

        return child;
    }

    @Override
    public void tick() {
        super.tick();

        // Only perform energy adjustments on the server side
        if (!this.getWorld().isClient) {
            // Handle panic state
            if (panicTicks > 0) {
                panicTicks--;
                if (panicTicks == 0) {
                    // Reset speed back to normal when panic ends
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(Speed * (ELvl / 100.0));
                }
            }

            // Handle energy loss if the cow was recently hit
            if (wasRecentlyHit) {
                // Reduce energy by 20% of its current level
                updateEnergyLevel(Math.max(0.0, ELvl * 0.8));
                wasRecentlyHit = false; // Reset the flag after applying the energy loss
            }

            // Check if the cow is standing on grass
            boolean isOnGrass = this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.GRASS_BLOCK);

            // Adjust energy level randomly based on whether the cow is on grass
            if (isOnGrass) {
                if (Math.random() < 0.2) { // 20% chance to gain energy
                    updateEnergyLevel(Math.min(100.0, ELvl + (0.01 + Math.random() * 0.19))); // Gain 0.01 to 0.2 energy
                }
            } else {
                if (Math.random() < 0.5) { // 50% chance to lose energy
                    updateEnergyLevel(Math.max(0.0, ELvl - (0.01 + Math.random() * 0.19))); // Lose 0.01 to 0.2 energy
                }
            }

            // Check if energy is 100 and regenerate health if not at max
            if (ELvl == 100.0) {
                if (this.getHealth() < this.getMaxHealth()) {
                    this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + 0.5F)); // Regenerate 0.5 HP per second
                }
            }

            // If energy reaches 0, kill the cow
            if (ELvl <= 0.0) {
                this.kill(); // This makes the cow die
            } else {
                // Update attributes dynamically if energy is greater than 0
                if (panicTicks == 0) { // Only update speed if not in panic mode
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(Speed * (ELvl / 100.0));
                }

                // Update the description with the new energy level
                updateDescription(this);
            }
        }
    }
}
