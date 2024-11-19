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

    public CustomCowEntity(EntityType<? extends CowEntity> entityType, World world) {
        super(entityType, world);

        Random random = new Random();
        this.MaxHp = 5 + random.nextInt(11);
        this.MinMeat = 1+ random.nextInt(2);
        this.MaxMeat = MinMeat + random.nextInt(3);
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
        DescriptionRenderer.setDescription(this, Text.of("Attributes\n" + "Max Hp: " + this.MaxHp + "\nMax Meat: " + this.MaxMeat));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);

        if (itemStack.isOf(Items.BUCKET) && !this.isBaby()) {
            player.playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);
            ItemStack itemStack2 = ItemUsage.exchangeStack(itemStack, player, Items.MILK_BUCKET.getDefaultStack());
            player.setStackInHand(hand, itemStack2);
            return ActionResult.success(this.getWorld().isClient);
        } else if (itemStack.isEmpty()) { // Check if the hand is empty
            // Only display the stats on the server side to avoid duplication
            if (!this.getWorld().isClient) {
                DescriptionRenderer.setDescription(this, Text.of("Attributes\n" + "Max Hp: " + this.MaxHp + "\nMax Meat: " + this.MaxMeat));
            }
            return ActionResult.success(this.getWorld().isClient);
        } else {
            return super.interactMob(player, hand);
        }
    }
    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);

        if (!this.getWorld().isClient) {
            // Calculate the amount of meat to drop between MinMeat and MaxMeat
            int meatAmount = MinMeat + this.getWorld().random.nextInt((MaxMeat - MinMeat) + 1);
            this.dropStack(new ItemStack(Items.BEEF, meatAmount));
        }
    }
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

        CustomCowEntity child = new CustomCowEntity(ModEntities.CUSTOM_COW, serverWorld);

        child.MaxHp = childMaxHp;
        child.MinMeat = childMinMeat;
        child.MaxMeat = childMaxMeat;
        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.MaxHp);

        return child;
    }
}
