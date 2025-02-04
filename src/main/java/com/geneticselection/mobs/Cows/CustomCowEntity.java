package com.geneticselection.mobs.Cows;

import com.geneticselection.attributes.AttributeCarrier;
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

public class CustomCowEntity extends CowEntity implements AttributeCarrier {
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

    public CustomCowEntity(EntityType<? extends CowEntity> entityType, World world) {
        super(entityType, world);

        // Initialize mob attributes (directly within the class)
        if (this.mobAttributes == null) {
            MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
            double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
            double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
            double energy = global.getEnergyLvl() * (0.9 + Math.random() * 0.1);
            double meat = global.getMaxMeat().orElse(0.0) + (0.98 + Math.random() * 0.1);
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
        this.milkingCooldown = 3000 + (int)((1 - (ELvl / 100.0)) * 2000) + random.nextInt(2001);
        if (!this.getWorld().isClient) {
            updateDescription(this);
        }
    }

    private void updateDescription(CustomCowEntity ent) {
        DescriptionRenderer.setDescription(ent, Text.of("Attributes\n" +
                "Max Hp: " + String.format("%.3f", ent.getHealth()) + "/" + String.format("%.3f", ent.MaxHp) +
                "\nSpeed: " + String.format("%.3f", ent.Speed) +
                "\nEnergy: " + String.format("%.3f", ent.ELvl) +
                "\nMax Meat: " + String.format("%.3f", ent.MaxMeat) +
                "\nMax Leather: " + String.format("%.3f", ent.MaxLeather) +
                "\nCooldown: " + ent.milkingCooldown));
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
        ItemStack offHandStack = player.getOffHandStack();

        if (itemStack.isOf(Items.WHEAT)) {
            if (ELvl < 20.0) {
                player.sendMessage(Text.of("This cow cannot breed because it has low energy."), true);
                return ActionResult.FAIL;
            }
            return super.interactMob(player, hand);
        } else if (offHandStack.isOf(Items.WHEAT)) {
            if (ELvl < 30.0) {
                player.sendMessage(Text.of("This cow cannot breed because it has low energy."), true);
                return ActionResult.FAIL;
            }
            return super.interactMob(player, Hand.OFF_HAND);
        } else if (itemStack.isOf(Items.BUCKET) && !this.isBaby()) {
            if (!this.getWorld().isClient) {
                long currentTime = this.getWorld().getTime();
                if (currentTime - lastMilkTime >= milkingCooldown) {
                    player.playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);
                    ItemStack itemStack2 = ItemUsage.exchangeStack(itemStack, player, Items.MILK_BUCKET.getDefaultStack());
                    player.setStackInHand(hand, itemStack2);
                    lastMilkTime = currentTime;
                    return ActionResult.SUCCESS;
                } else {
                    player.sendMessage(Text.of("This cow needs " + (milkingCooldown - (currentTime - lastMilkTime)) + " more ticks"), true);
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.CONSUME;
        } else if (itemStack.isEmpty()) {
            if (!this.getWorld().isClient)
                updateDescription(this);
            return ActionResult.success(this.getWorld().isClient);
        } else {
            return super.interactMob(player, hand);
        }
    }

    @Override
    protected void applyDamage(DamageSource source, float amount) {
        super.applyDamage(source, amount);
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                .setBaseValue(Speed * (ELvl / 100.0));
        if (!this.getWorld().isClient)
            updateDescription(this);
    }

    @Override
    public void onDeath(DamageSource source) {
        if (this.isBaby()) {
            return;
        }
        if (ELvl <= 0.0) {
            this.dropStack(new ItemStack(Items.BONE));
        } else {
            super.onDeath(source);
            if (!this.getWorld().isClient) {
                int scaledMeatAmount = (int) ((MinMeat + this.getWorld().random.nextInt((int) (MaxMeat - MinMeat) + 1)) * (ELvl / 100.0));
                this.dropStack(new ItemStack(Items.BEEF, Math.max(0, scaledMeatAmount)));
                int scaledLeatherAmount = (int) ((MinLeather + this.getWorld().random.nextInt((int) (MaxLeather - MinLeather) + 1)) * (ELvl / 100.0));
                this.dropStack(new ItemStack(Items.LEATHER, Math.max(0, scaledLeatherAmount)));
            }
        }
    }

    @Override
    public CustomCowEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof CustomCowEntity))
            return (CustomCowEntity) EntityType.COW.create(serverWorld);

        CustomCowEntity parent1 = this;
        CustomCowEntity parent2 = (CustomCowEntity) mate;

        double childMaxHp = (parent1.MaxHp + parent2.MaxHp) / 2;
        double childMinMeat = (parent1.MinMeat + parent2.MinMeat) / 2;
        double childMaxMeat = (parent1.MaxMeat + parent2.MaxMeat) / 2;
        double childMinLeather = (parent1.MinLeather + parent2.MinLeather) / 2;
        double childMaxLeather = (parent1.MaxLeather + parent2.MaxLeather) / 2;
        int childMilkingCooldown = (parent1.milkingCooldown + parent2.milkingCooldown) / 2;
        double childEnergy = (parent1.ELvl + parent2.ELvl) / 2;

        CustomCowEntity child = new CustomCowEntity(ModEntities.CUSTOM_COW, serverWorld);
        child.MaxHp = childMaxHp;
        child.MinMeat = childMinMeat;
        child.MaxMeat = childMaxMeat;
        child.MinLeather = childMinLeather;
        child.MaxLeather = childMaxLeather;
        child.milkingCooldown = childMilkingCooldown;
        child.ELvl = childEnergy;

        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.MaxHp);
        child.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(child.Speed * (child.ELvl / 100.0));

        // Apply custom attributes to update drop values, etc.
        child.applyCustomAttributes(child.mobAttributes);

        if (!this.getWorld().isClient)
            updateDescription(child);

        return child;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient) {
            boolean isOnGrass = this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.GRASS_BLOCK);
            if (isOnGrass) {
                ELvl = Math.min(100.0, ELvl + 0.1);
            } else {
                ELvl = Math.max(0.0, ELvl - 0.05);
            }
            if (ELvl <= 0.0) {
                this.kill();
            } else {
                this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(Speed * (ELvl / 100.0));
                updateDescription(this);
            }
        }
    }

    @Override
    public void applyCustomAttributes(MobAttributes attributes) {
        attributes.getMaxMeat().ifPresent(this::setMaxMeat);
        attributes.getMaxLeather().ifPresent(this::setMaxLeather);
    }
}
