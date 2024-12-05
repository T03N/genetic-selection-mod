package com.geneticselection.mobs.Cows;

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

import java.util.Optional;

import static com.geneticselection.genetics.ChildInheritance.*;

public class CustomCowEntity extends CowEntity {
    private MobAttributes mobAttributes; // Directly store MobAttributes for this entity
    private double MaxHp;
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
            double meat = global.getMaxMeat().orElse(0.0) + (0.98 + Math.random() * 0.1);
            double leather = global.getMaxLeather().orElse(0.0) * (0.98 + Math.random() * 0.1);
            this.mobAttributes = new MobAttributes(speed, health, Optional.of(meat), Optional.of(leather),Optional.empty(),Optional.empty());
        }

        // Apply attributes to the entity
        this.MaxHp = this.mobAttributes.getMaxHealth();
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
        this.Speed = this.mobAttributes.getMovementSpeed();
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);
        this.mobAttributes.getMaxMeat().ifPresent(maxMeat -> {
            this.MaxMeat = maxMeat;
        });
        this.mobAttributes.getMaxLeather().ifPresent(maxLeather -> {
            this.MaxLeather = maxLeather;
        });
        this.setMinMeat(1.0);
        this.setMinLeather(0.0);
        this.milkingCooldown = 3000 + random.nextInt(2001);
        if (!this.getWorld().isClient) {
            updateDescription(this);
        }
    }

    private void updateDescription(CustomCowEntity ent) {
        DescriptionRenderer.setDescription(ent, Text.of("Attributes\n" +
                "Max Hp: " + ent.getHealth() + "/" + ent.MaxHp + "\n" +
                "Meat: " + ent.MinMeat + "-" + ent.MaxMeat + "\n" +
                "Leather: " + ent.MinLeather + "-" + ent.MaxLeather + "\n" +
                "Cooldown: " + ent.milkingCooldown));
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

        if (itemStack.isOf(Items.BUCKET) && !this.isBaby()) {
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
        }
        else if (itemStack.isEmpty()) { // Check if the hand is empty
            // Only display the stats on the server side to avoid duplication
            if (!this.getWorld().isClient) {
                updateDescription(this);
            }
            return ActionResult.success(this.getWorld().isClient);
        }
        else {
            return super.interactMob(player, hand);
        }
    }

    @Override
    protected void applyDamage(DamageSource source, float amount) {
        super.applyDamage(source, amount);

        if (!this.getWorld().isClient) {
            updateDescription(this);
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);

        if (!this.getWorld().isClient) {
            // Calculate the amount of meat to drop between MinMeat and MaxMeat
            int meatAmount = (int) (MinMeat + this.getWorld().random.nextInt(((int)(MaxMeat - MinMeat)) + 1));
            this.dropStack(new ItemStack(Items.BEEF, meatAmount));

            // Calculate the amount of leather to drop between MinLeather and MaxLeather
            int leatherAmount = (int)(MinLeather + this.getWorld().random.nextInt((int)(MaxLeather - MinLeather) + 1));
            this.dropStack(new ItemStack(Items.LEATHER, leatherAmount));
        }
    }

    @Override
    public CustomCowEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof CustomCowEntity)) {
            return (CustomCowEntity) EntityType.COW.create(serverWorld);
        }

        CustomCowEntity parent1 = this;
        CustomCowEntity parent2 = (CustomCowEntity) mate;

        double childMaxHp = (parent1.MaxHp + parent2.MaxHp) / 2;
        double childMinMeat = (parent1.MinMeat + parent2.MinMeat) / 2;
        double childMaxMeat = (parent1.MaxMeat + parent2.MaxMeat) / 2;
        double childMinLeather = (parent1.MinLeather + parent2.MinLeather) / 2;
        double childMaxLeather = (parent1.MaxLeather + parent2.MaxLeather) / 2;
        int childMilkingCooldown = (parent1.milkingCooldown + parent2.milkingCooldown) / 2;

        CustomCowEntity child = new CustomCowEntity(ModEntities.CUSTOM_COW, serverWorld);

        child.MaxHp = childMaxHp;
        child.MinMeat = childMinMeat;
        child.MaxMeat = childMaxMeat;
        child.MinLeather = childMinLeather;
        child.MaxLeather = childMaxLeather;
        child.milkingCooldown = childMilkingCooldown;
        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.MaxHp);

        if (!this.getWorld().isClient) {
            updateDescription(child);
        }
        return child;
    }

}
