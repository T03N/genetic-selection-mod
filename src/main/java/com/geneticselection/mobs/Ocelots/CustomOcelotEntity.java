package com.geneticselection.mobs.Ocelots;

import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.utils.DescriptionRenderer;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.Optional;

import static com.geneticselection.genetics.ChildInheritance.*;

public class CustomOcelotEntity extends OcelotEntity {
    private MobAttributes mobAttributes;
    private double MaxHp;
    private double Speed;
    private double ELvl;

    public CustomOcelotEntity(EntityType<? extends OcelotEntity> entityType, World world) {
        super(entityType, world);

        if (this.mobAttributes == null) {
            MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
            double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
            double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
            double energy = global.getEnergyLvl() * (0.9 + Math.random() * 0.1);
            this.mobAttributes = new MobAttributes(speed, health, energy, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        }
        this.MaxHp = this.mobAttributes.getMaxHealth();
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
        this.setHealth((float) this.MaxHp);
        this.Speed = this.mobAttributes.getMovementSpeed();
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);
        this.ELvl = this.mobAttributes.getEnergyLvl();

        if (!this.getWorld().isClient)
            updateDescription(this);
    }

    public void updateEnergyLevel(double newEnergyLevel) {
        this.ELvl = newEnergyLevel;

        // Sync energy level with server if needed
        if (!this.getWorld().isClient) {
            this.syncEnergyLevelToClient();
        }
    }

    private void syncEnergyLevelToClient() {
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
        data.writeInt(this.getId());  // Send entity ID
        data.writeDouble(this.ELvl);  // Send the updated energy level
    }

    private void updateDescription(com.geneticselection.mobs.Ocelots.CustomOcelotEntity ent) {
        DescriptionRenderer.setDescription(ent, Text.of("Attributes\n" +
                "Max Hp: " + String.format("%.3f", ent.getHealth()) + "/" + String.format("%.3f", ent.MaxHp) +
                "\nSpeed: " + String.format("%.3f", ent.Speed) +
                "\nEnergy: " + String.format("%.3f", ent.ELvl)));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        // If player has an empty hand
        if (itemStack.isEmpty()) {
            player.playSound(SoundEvents.ENTITY_OCELOT_AMBIENT, 1.0F, 1.0F);

            // Only display the stats on the server side to avoid duplication
            if (!this.getWorld().isClient) {
                updateDescription(this);
            }
            return ActionResult.success(this.getWorld().isClient);
        } else {
            // If the player is holding something else (like food or another item), you can handle that here.
            // You can check itemStack for specific items, and create custom behavior for them.
            return super.interactMob(player, hand); // fallback to the default interaction
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
    }

    @Override
    public com.geneticselection.mobs.Ocelots.CustomOcelotEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof com.geneticselection.mobs.Ocelots.CustomOcelotEntity)) {
            return (com.geneticselection.mobs.Ocelots.CustomOcelotEntity) EntityType.OCELOT.create(serverWorld);
        }

        com.geneticselection.mobs.Ocelots.CustomOcelotEntity parent1 = this;
        com.geneticselection.mobs.Ocelots.CustomOcelotEntity parent2 = (com.geneticselection.mobs.Ocelots.CustomOcelotEntity) mate;

        MobAttributes attr1 = parent1.mobAttributes;
        MobAttributes attr2 = parent2.mobAttributes;

        MobAttributes childAttributes = inheritAttributes(attr1, attr2);


        com.geneticselection.mobs.Ocelots.CustomOcelotEntity child = new com.geneticselection.mobs.Ocelots.CustomOcelotEntity(ModEntities.CUSTOM_OCELOT, serverWorld);

        child.mobAttributes = childAttributes;
        applyAttributes(child, childAttributes);

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