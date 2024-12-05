package com.geneticselection.mobs.Sheep;

import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.SheepEntity;
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

public class CustomSheepEntity extends SheepEntity {
    private MobAttributes mobAttributes; // Directly store MobAttributes for this entity
    private double MaxHp;
    private double Speed;
    private double MaxMeat;
    private double MaxWool;

    public CustomSheepEntity(EntityType<? extends SheepEntity> entityType, World world) {
        super(entityType, world);

        // Initialize mob attributes (directly within the class)
        if (this.mobAttributes == null) {
            MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
            double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
            double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
            double meat = global.getMaxMeat().orElse(0.0) + (0.98 + Math.random() * 0.1);
            double wool = global.getMaxRabbitHide().orElse(0.0) + (0.98 + Math.random() * 0.1);
            this.mobAttributes = new MobAttributes(speed, health, Optional.of(meat), Optional.empty(), Optional.of(wool), Optional.empty());
        }

        // Apply attributes to the entity
        this.MaxHp = this.mobAttributes.getMaxHealth();
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
        this.Speed = this.mobAttributes.getMovementSpeed();
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);
        this.mobAttributes.getMaxMeat().ifPresent(maxMeat -> {
            this.MaxMeat = maxMeat;
        });
        this.mobAttributes.getMaxWool().ifPresent(MaxWool -> {
            this.MaxWool = MaxWool;
        });
    }

    public void setMaxMeat(double maxMeat)
    {
        this.MaxMeat = maxMeat;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);

        if (itemStack.isEmpty()) { // Check if the hand is empty
            // Only display the stats on the server side to avoid duplication
            if (this.getWorld().isClient) {
                DescriptionRenderer.setDescription(this, Text.of("Attributes\n" +
                        "Max Hp: " + String.format("%.4f", this.MaxHp) + "\n" +
                        "Speed: " + String.format("%.4f", this.Speed) + "\n" +
                        "Max Meat: " + String.format("%.4f", this.MaxMeat) + "\n" +
                        "Wool: " + String.format("%.4f", this.MaxWool)));
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
            this.dropStack(new ItemStack(Items.RABBIT, meatAmount));

            // Calculate the amount of leather to drop between MinLeather and MaxLeather
            int woolAmount = (int)(MaxWool);
            this.dropStack(new ItemStack(Items.WHITE_WOOL, woolAmount));
        }
    }

    @Override
    public CustomSheepEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof CustomSheepEntity)) {
            return (CustomSheepEntity) EntityType.SHEEP.create(serverWorld);
        }

        CustomSheepEntity parent1 = this;
        CustomSheepEntity parent2 = (CustomSheepEntity) mate;

        MobAttributes attr1 = parent1.mobAttributes;
        MobAttributes attr2 = parent2.mobAttributes;

        // Inherit attributes from both parents
        MobAttributes childAttributes = inheritAttributes(attr1, attr2);

        double childMaxMeat = (parent1.MaxMeat + parent2.MaxMeat) / 2;
        double childMaxWool = (parent1.MaxWool + parent2.MaxWool) / 2;

        CustomSheepEntity child = new CustomSheepEntity(ModEntities.CUSTOM_SHEEP, serverWorld);

        // Set the inherited attributes directly
        child.mobAttributes = childAttributes;
        applyAttributes(child, childAttributes);

        child.MaxMeat = childMaxMeat;
        child.MaxWool = childMaxWool;
        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.MaxHp);

        influenceGlobalAttributes(child.getType());

        return child;
    }

    // Getter and Setter for mobAttributes if needed
    public MobAttributes getMobAttributes() {
        return this.mobAttributes;
    }

    public void setMobAttributes(MobAttributes attributes) {
        this.mobAttributes = attributes;
    }
}
