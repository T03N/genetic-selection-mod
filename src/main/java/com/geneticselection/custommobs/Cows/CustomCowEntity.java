package com.geneticselection.custommobs.Cows;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import java.util.Random;

public class CustomCowEntity extends CowEntity {
    private int MaxHp;
    private double Speed;

    public CustomCowEntity(EntityType<? extends CowEntity> entityType, World world) {
        super(entityType, world);
        Random random = new Random();
        this.MaxHp = 5 + random.nextInt(11);
        this.Speed = 0.15 + (0.1 * random.nextDouble());
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        if (itemStack.isOf(Items.BUCKET) && !this.isBaby()) {
            player.playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);
            ItemStack itemStack2 = ItemUsage.exchangeStack(itemStack, player, Items.MILK_BUCKET.getDefaultStack());
            player.setStackInHand(hand, itemStack2);
            return ActionResult.success(this.getWorld().isClient);
        }
        else if (itemStack.isEmpty()) { // Check if the hand is empty
            // Retrieve the cow's current health and speed attributes
            double currentHealth = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).getValue();
            double currentSpeed = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).getValue();

            // Send the stats to the player
            player.sendMessage(Text.literal("Custom Cow Stats:"), false);
            player.sendMessage(Text.literal("Health: " + currentHealth), false);
            player.sendMessage(Text.literal("Speed: " + currentSpeed), false);

            return ActionResult.success(this.getWorld().isClient);
        }
        else {
            return super.interactMob(player, hand);
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (!this.getWorld().isClient) {
            this.dropStack(new ItemStack(Items.BEEF, 2 + this.getWorld().random.nextInt(3))); // Drop between 2 and 4 beef
        }
    }

    public static DefaultAttributeContainer.Builder createCowAttributes() {
        return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.20000000298023224);
    }
}
