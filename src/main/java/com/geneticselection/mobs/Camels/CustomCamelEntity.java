package com.geneticselection.mobs.Camels;

import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.Optional;

import static com.geneticselection.genetics.ChildInheritance.*;

public class CustomCamelEntity extends CamelEntity {
    private MobAttributes mobAttributes; // Directly store MobAttributes for this entity
    private double MaxHp;
    private double Speed;

    public CustomCamelEntity(EntityType<? extends CamelEntity> entityType, World world) {
        super(entityType, world);

        // Initialize mob attributes (directly within the class)
        if (this.mobAttributes == null) {
            MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
            double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
            double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
            this.mobAttributes = new MobAttributes(speed, health, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        }

        // Apply attributes to the entity
        this.MaxHp = this.mobAttributes.getMaxHealth();
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
        this.Speed = this.mobAttributes.getMovementSpeed();
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        // If player has an empty hand
        if (itemStack.isEmpty()) {
            player.playSound(SoundEvents.ENTITY_CAMEL_AMBIENT, 1.0F, 1.0F);

            // Only display the stats on the server side to avoid duplication
            if (!this.getWorld().isClient) {
                DescriptionRenderer.setDescription(this, Text.of("Attributes\n" +
                        "Max Hp: " + this.MaxHp));
            }
            return ActionResult.success(this.getWorld().isClient);
        } else {
            return super.interactMob(player, hand);
        }
    }


    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
    }

    @Override
    public CustomCamelEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof CustomCamelEntity)) {
            return (CustomCamelEntity) EntityType.CAMEL.create(serverWorld);
        }

        CustomCamelEntity parent1 = this;
        CustomCamelEntity parent2 = (CustomCamelEntity) mate;

        MobAttributes attr1 = parent1.mobAttributes;
        MobAttributes attr2 = parent2.mobAttributes;

        // Inherit attributes from both parents
        MobAttributes childAttributes = inheritAttributes(attr1, attr2);

        CustomCamelEntity child = new CustomCamelEntity(ModEntities.CUSTOM_CAMEL, serverWorld);

        // Set the inherited attributes directly
        child.mobAttributes = childAttributes;
        applyAttributes(child, childAttributes);

        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.MaxHp);

        influenceGlobalAttributes(child.getType());

        return child;
    }
}