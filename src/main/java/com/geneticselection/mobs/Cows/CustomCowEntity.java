package com.geneticselection.mobs.Cows;

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
import java.util.Random;

public class CustomCowEntity extends CowEntity {
    private int MaxHp;
    private int MinMeat;
    private int MaxMeat;
    private int MinLeather;
    private int MaxLeather;
    private int milkingCooldown;
    private long lastMilkTime = 0;

    public CustomCowEntity(EntityType<? extends CowEntity> entityType, World world) {
        super(entityType, world);

        Random random = new Random();
        this.MaxHp = 5 + random.nextInt(11);
        this.MinMeat = 1 + random.nextInt(2);
        this.MaxMeat = this.MinMeat + random.nextInt(3);
        this.MinLeather = random.nextInt(2);
        this.MaxLeather = this.MinLeather + random.nextInt(2);
        this.milkingCooldown = 3000 + random.nextInt(2001);
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
        this.setHealth(this.MaxHp);

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
            int meatAmount = MinMeat + this.getWorld().random.nextInt((MaxMeat - MinMeat) + 1);
            this.dropStack(new ItemStack(Items.BEEF, meatAmount));

            int leatherAmount = MinLeather + this.getWorld().random.nextInt((MaxLeather - MinLeather) + 1);
            this.dropStack(new ItemStack(Items.LEATHER, leatherAmount));
        }
    }

    // Custom breeding from your original code
    @Override
    public CustomCowEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof CustomCowEntity)) {
            return (CustomCowEntity) EntityType.COW.create(serverWorld);
        }

        CustomCowEntity parent1 = this;
        CustomCowEntity parent2 = (CustomCowEntity) mate;

        int childMaxHp = (parent1.MaxHp + parent2.MaxHp) / 2;
        int childMinMeat = (parent1.MinMeat + parent2.MinMeat) / 2;
        int childMaxMeat = (parent1.MaxMeat + parent2.MaxMeat) / 2;
        int childMinLeather = (parent1.MinLeather + parent2.MinLeather) / 2;
        int childMaxLeather = (parent1.MaxLeather + parent2.MaxLeather) / 2;
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