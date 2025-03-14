package com.geneticselection.mobs.Pigs;

import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;

import java.util.Optional;

public class CustomPigEntity extends PigEntity {
    private MobAttributes mobAttributes;
    private double MaxHp;
    private double ELvl;
    private double Speed;
    private double MinMeat;
    private double MaxMeat;
    private boolean wasRecentlyHit = false;

    public CustomPigEntity(EntityType<? extends PigEntity> entityType, World world) {
        super(entityType, world);

        if (this.mobAttributes == null) {
            MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
            double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
            double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
            double energy = global.getEnergyLvl() * (0.9 + Math.random() * 0.1);
            double meat = global.getMaxMeat().orElse(0.0) + (0.98 + Math.random() * 0.1);
            this.mobAttributes = new MobAttributes(speed, health, energy, Optional.of(meat), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        }

        this.MaxHp = this.mobAttributes.getMaxHealth();
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
        this.Speed = this.mobAttributes.getMovementSpeed();
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);
        this.ELvl = this.mobAttributes.getEnergyLvl();

        this.mobAttributes.getMaxMeat().ifPresent(maxMeat -> {
            this.MaxMeat = maxMeat;
        });
        this.setMinMeat(1.0);

        if (!this.getWorld().isClient) {
            updateDescription(this);
        }
    }

    private void updateDescription(CustomPigEntity ent) {
        DescriptionRenderer.setDescription(ent, Text.of("Attributes\n" +
                "Max Hp: " + String.format("%.3f", ent.getHealth()) + "/"+ String.format("%.3f", ent.MaxHp) +
                "\nSpeed: " + String.format("%.3f", ent.Speed) +
                "\nEnergy: " + String.format("%.3f", ent.ELvl) +
                "\nMax Meat: " + String.format("%.3f", ent.MaxMeat)));
    }

    public void setMinMeat(double minMeat) {
        this.MinMeat = minMeat;
    }

    public void setMaxMeat(double maxMeat) {
        this.MaxMeat = maxMeat;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        ItemStack offHandStack = player.getOffHandStack();
        boolean isCarrot = itemStack.isOf(Items.CARROT) || offHandStack.isOf(Items.CARROT);

        if (isCarrot) {
            Hand usedHand = itemStack.isOf(Items.CARROT) ? hand : Hand.OFF_HAND;
            ItemStack usedItem = itemStack.isOf(Items.CARROT) ? itemStack : offHandStack;

            if (this.isBaby()) {
                return ActionResult.PASS;
            }

            if (this.isInLove()) {
                if (ELvl < 100.0) {
                    ELvl = Math.min(100.0, ELvl + 10.0);
                    player.sendMessage(Text.of("The pig has gained energy! Current energy: " + String.format("%.1f", ELvl)), true);
                    usedItem.decrement(1);
                    updateDescription(this);
                    return ActionResult.SUCCESS;
                } else {
                    player.sendMessage(Text.of("The pig is already at maximum energy!"), true);
                    return ActionResult.PASS;
                }
            }

            if (ELvl < 20.0) {
                ELvl = Math.min(100.0, ELvl + 10.0);
                player.sendMessage(Text.of("This pig cannot breed due to low energy. Energy increased to: " + String.format("%.1f", ELvl)), true);
                usedItem.decrement(1);
                updateDescription(this);
                return ActionResult.SUCCESS;
            } else {
                this.lovePlayer(player);
                player.sendMessage(Text.of("The pig is now in breed mode!"), true);
                usedItem.decrement(1);
                updateDescription(this);
                return ActionResult.SUCCESS;
            }
        }

        if (itemStack.isEmpty()) {
            if (!this.getWorld().isClient) {
                updateDescription(this);
            }
            return ActionResult.success(this.getWorld().isClient);
        }

        return super.interactMob(player, hand);
    }

    private int panicTicks = 0;
    private static final int PANIC_DURATION = 100; // 5 seconds at 20 ticks per second
    private static final double PANIC_SPEED_MULTIPLIER = 2.0;

    @Override
    protected void applyDamage(DamageSource source, float amount) {
        super.applyDamage(source, amount);
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
        if (this.isBaby()) {
            return;
        }

        if (ELvl <= 0.0) {
            this.dropStack(new ItemStack(Items.BONE, 1));
        } else {
            super.onDeath(source);
            if (!this.getWorld().isClient) {
                int scaledMeatAmount = (int) ((MinMeat + this.getWorld().random.nextInt((int) (MaxMeat - MinMeat) + 1)) * (ELvl / 100.0));
                this.dropStack(new ItemStack(Items.PORKCHOP, Math.max(0, scaledMeatAmount)));
            }
        }
    }

    @Override
    public CustomPigEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof CustomPigEntity))
            return (CustomPigEntity) EntityType.PIG.create(serverWorld);

        CustomPigEntity parent1 = this;
        CustomPigEntity parent2 = (CustomPigEntity) mate;

        double inheritanceFactor = Math.min(parent1.ELvl, parent2.ELvl) / 100.0;

        double childMaxHp = ((parent1.MaxHp + parent2.MaxHp) / 2) * inheritanceFactor;
        double childMinMeat = ((parent1.MinMeat + parent2.MinMeat) / 2) * inheritanceFactor;
        double childMaxMeat = ((parent1.MaxMeat + parent2.MaxMeat) / 2) * inheritanceFactor;
        double childEnergy = ((parent1.ELvl + parent2.ELvl) / 2) * inheritanceFactor;

        CustomPigEntity child = new CustomPigEntity(ModEntities.CUSTOM_PIG, serverWorld);

        child.MaxHp = childMaxHp;
        child.MinMeat = childMinMeat;
        child.MaxMeat = childMaxMeat;
        child.ELvl = childEnergy;

        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.MaxHp);
        child.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(child.Speed * (child.ELvl / 100.0));

        if (!this.getWorld().isClient)
            updateDescription(child);

        return child;
    }

    @Override
    public void tick() {
        super.tick();

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

            if (wasRecentlyHit) {
                ELvl = Math.max(0.0, ELvl * 0.8);
                wasRecentlyHit = false;
            }

            boolean isOnGrass = this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.GRASS_BLOCK);

            if (isOnGrass) {
                if (Math.random() < 0.2) {
                    ELvl = Math.min(100.0, ELvl + (0.01 + Math.random() * 0.19));
                }
            } else {
                if (Math.random() < 0.5) {
                    ELvl = Math.max(0.0, ELvl - (0.01 + Math.random() * 0.19));
                }
            }

            if (ELvl == 100.0) {
                if (this.getHealth() < this.getMaxHealth()) {
                    this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + 0.5F));
                }
            }

            if (ELvl <= 0.0) {
                this.kill();
            } else {
                this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                        .setBaseValue(Speed * (ELvl / 100.0));
                updateDescription(this);
            }
        }
    }
}