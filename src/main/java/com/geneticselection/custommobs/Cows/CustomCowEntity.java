package com.geneticselection.custommobs.Cows;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class CustomCowEntity extends CowEntity {

    public CustomCowEntity(EntityType<? extends CowEntity> entityType, World world) {
        super(entityType, world);
    }

    // Override methods to customize behavior, such as breeding, milk production, etc.

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        if (itemStack.isOf(Items.BUCKET) && !this.isBaby()) {
            player.playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);
            ItemStack itemStack2 = ItemUsage.exchangeStack(itemStack, player, Items.MILK_BUCKET.getDefaultStack());
            player.setStackInHand(hand, itemStack2);
            return ActionResult.success(this.getWorld().isClient);
        }
        else if(itemStack.isEmpty()){
            player.sendMessage(Text.literal("This is a colored message!").styled(style -> style.withColor(TextColor.fromRgb(0xFF0000))), true);
            return ActionResult.success(this.getWorld().isClient);
        }
        else {
            return super.interactMob(player, hand);
        }
    }

    // Add other methods to handle your specific custom features
}
