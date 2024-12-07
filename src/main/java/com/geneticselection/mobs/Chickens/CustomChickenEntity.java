package com.geneticselection.mobs.Chickens;

import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.Optional;

import static com.geneticselection.genetics.ChildInheritance.*;

public class CustomChickenEntity extends ChickenEntity {
    private MobAttributes mobAttributes; // Directly store MobAttributes for this entity
    private double MaxHp;
    private double Speed;
    private double MaxMeat;
    private double feathers;

    public CustomChickenEntity(EntityType<? extends ChickenEntity> entityType, World world) {
        super(entityType, world);

        // Initialize mob attributes (directly within the class)
        if (this.mobAttributes == null) {
            MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
            double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
            double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
            double meat = global.getMaxMeat().orElse(0.0) + (0.98 + Math.random() * 0.1);
            double feather = global.getMaxFeathers().orElse(0.0) + (0.98 + Math.random() * 0.1);
            this.mobAttributes = new MobAttributes(speed, health, Optional.of(meat), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(feather));
        }

        // Apply attributes to the entity
        this.MaxHp = this.mobAttributes.getMaxHealth();
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
        this.Speed = this.mobAttributes.getMovementSpeed();
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);
        this.mobAttributes.getMaxMeat().ifPresent(maxMeat -> {
            this.MaxMeat = maxMeat;
        });
        this.mobAttributes.getMaxFeathers().ifPresent(maxRabbitHide -> {
            this.feathers = maxRabbitHide;
        });

        if (!this.getWorld().isClient)
            updateDescription(this);
    }

    private void updateDescription(CustomChickenEntity ent) {
        DescriptionRenderer.setDescription(ent, Text.of("Attributes\n" +
                "Max Hp: " + String.format("%.3f", ent.getHealth()) + "/"+ String.format("%.3f", ent.MaxHp) +
                "\nSpeed: " + String.format("%.3f", ent.Speed) +
                "\nMax Meat: " + String.format("%.3f", ent.MaxMeat) +
                "\nFeathers: " + String.format("%.3f", ent.feathers)));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);

        if (itemStack.isEmpty()) { // Check if the hand is empty
            // Only display the stats on the server side to avoid duplication
            if (!this.getWorld().isClient) {
                updateDescription(this);
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
            int meatAmount = (int) (MaxMeat);
            this.dropStack(new ItemStack(Items.CHICKEN, meatAmount));

            // Calculate the amount of leather to drop between MinLeather and MaxLeather
            int featherAmount = (int)(feathers);
            this.dropStack(new ItemStack(Items.FEATHER, featherAmount));
        }
    }

    @Override
    public CustomChickenEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof CustomChickenEntity)) {
            return (CustomChickenEntity) EntityType.CHICKEN.create(serverWorld);
        }

        CustomChickenEntity parent1 = this;
        CustomChickenEntity parent2 = (CustomChickenEntity) mate;

        MobAttributes attr1 = parent1.mobAttributes;
        MobAttributes attr2 = parent2.mobAttributes;

        // Inherit attributes from both parents
        MobAttributes childAttributes = inheritAttributes(attr1, attr2);

        double childMaxMeat = (parent1.MaxMeat + parent2.MaxMeat) / 2;

        CustomChickenEntity child = new CustomChickenEntity(ModEntities.CUSTOM_CHICKEN, serverWorld);

        // Set the inherited attributes directly
        child.mobAttributes = childAttributes;
        applyAttributes(child, childAttributes);

        child.MaxMeat = childMaxMeat;
        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.MaxHp);

        influenceGlobalAttributes(child.getType());

        if (!this.getWorld().isClient)
            updateDescription(child);

        return child;
    }

    @Override
    protected void applyDamage(DamageSource source, float amount) {
        super.applyDamage(source, amount);

        if (!this.getWorld().isClient)
            updateDescription(this);
    }
}