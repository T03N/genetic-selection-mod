package com.geneticselection.mobs.Donkeys;

import com.geneticselection.attributes.AttributeCarrier;
import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.DonkeyEntity;
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

import java.util.Optional;

import static com.geneticselection.genetics.ChildInheritance.*;

public class CustomDonkeyEntity extends DonkeyEntity implements AttributeCarrier {
    private MobAttributes mobAttributes; // Directly store MobAttributes for this entity
    private double MaxHp;
    private double Speed;
    private double ELvl;
    private double MaxLeather;

    public CustomDonkeyEntity(EntityType<? extends DonkeyEntity> entityType, World world) {
        super(entityType, world);

        // Initialize mob attributes (directly within the class)
        if (this.mobAttributes == null) {
            MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
            double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
            double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
            double energy = global.getEnergyLvl() * (0.9 + Math.random() * 0.1);
            double leather = global.getMaxLeather().orElse(0.0) * (0.98 + Math.random() * 0.1);
            this.mobAttributes = new MobAttributes(speed, health,  energy,Optional.empty(), Optional.of(leather), Optional.empty(), Optional.empty(),Optional.empty());
        }

        // Apply attributes to the entity
        this.MaxHp = this.mobAttributes.getMaxHealth();
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
        this.setHealth((float)this.MaxHp);
        this.Speed = this.mobAttributes.getMovementSpeed();
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);
        this.ELvl = this.mobAttributes.getEnergyLvl();

        this.mobAttributes.getMaxLeather().ifPresent(maxLeather -> {
            this.MaxLeather = maxLeather;
        });

        if (!this.getWorld().isClient)
            updateDescription(this);
    }

    public void setMaxLeather(double maxLeather)
    {
        this.MaxLeather = maxLeather;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        // If player has an empty hand
        if (itemStack.isEmpty()) {
            player.playSound(SoundEvents.ENTITY_DONKEY_AMBIENT, 1.0F, 1.0F);

            // Only display the stats on the server side to avoid duplication
            if (!this.getWorld().isClient)
                updateDescription(this);

            return ActionResult.success(this.getWorld().isClient);
        } else {
            return super.interactMob(player, hand);
        }
    }

    private void updateDescription(CustomDonkeyEntity ent) {
        DescriptionRenderer.setDescription(ent, Text.of("Attributes\n" +
                "Max Hp: " + String.format("%.3f", ent.getHealth()) + "/"+ String.format("%.3f", ent.MaxHp) +
                "\nSpeed: " + String.format("%.3f", ent.Speed) +
                "\nEnergy: " + String.format("%.3f", ent.ELvl) +
                "\nMax Leather: " + String.format("%.3f", ent.MaxLeather)));
    }


    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);

        if (!this.getWorld().isClient) {
            int leatherAmount = (int)(MaxLeather);
            this.dropStack(new ItemStack(Items.LEATHER, leatherAmount));
        }
    }

    @Override
    public CustomDonkeyEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof CustomDonkeyEntity)) {
            return (CustomDonkeyEntity) EntityType.DONKEY.create(serverWorld);
        }

        CustomDonkeyEntity parent1 = this;
        CustomDonkeyEntity parent2 = (CustomDonkeyEntity) mate;

        MobAttributes attr1 = parent1.mobAttributes;
        MobAttributes attr2 = parent2.mobAttributes;

        // Inherit attributes from both parents
        MobAttributes childAttributes = inheritAttributes(attr1, attr2);

        double childMaxLeather = (parent1.MaxLeather + parent2.MaxLeather) / 2;

        CustomDonkeyEntity child = new CustomDonkeyEntity(ModEntities.CUSTOM_DONKEY, serverWorld);

        // Set the inherited attributes directly
        child.mobAttributes = childAttributes;
        applyAttributes(child, childAttributes);

        child.MaxLeather = childMaxLeather;
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

    @Override
    public void applyCustomAttributes(MobAttributes attributes) {
        attributes.getMaxLeather().ifPresent(this::setMaxLeather);
    }
}