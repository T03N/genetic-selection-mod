package com.geneticselection.mobs.Camels;

import com.geneticselection.attributes.AttributeCarrier;
import com.geneticselection.attributes.AttributeKey;
import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.genetics.ChildInheritance;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.Optional;
import static com.geneticselection.genetics.ChildInheritance.*;

public class CustomCamelEntity extends CamelEntity implements AttributeCarrier {
    private MobAttributes mobAttributes;
    private double MaxHp;
    private double Speed;
    private double ELvl;

    private int panicTicks = 0;
    private static final int PANIC_DURATION = 100;
    private static final double PANIC_SPEED_MULTIPLIER = 1.25;
    private boolean wasRecentlyHit = false;

    public CustomCamelEntity(EntityType<? extends CamelEntity> entityType, World world) {
        super(entityType, world);

        if (this.mobAttributes == null) {
            MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
            double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
            double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
            double energy = global.getEnergyLvl() * (0.9 + Math.random() * 0.1);
            double meat = global.getMaxMeat().orElse(0.0) + (0.98 + Math.random() * 0.1);
            double leather = global.getMaxLeather().orElse(0.0) + (0.98 + Math.random() * 0.1);
            this.mobAttributes = new MobAttributes(speed, health, energy, Optional.of(meat), Optional.of(leather), Optional.empty(), Optional.empty(), Optional.empty());
        }

        this.MaxHp = this.mobAttributes.getMaxHealth();
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
        this.setHealth((float)this.MaxHp);
        this.Speed = this.mobAttributes.getMovementSpeed();
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);
        this.ELvl = this.mobAttributes.getEnergyLvl();

        if (!this.getWorld().isClient)
            updateDescription(this);
    }

    private void updateDescription(CustomCamelEntity ent) {
        DescriptionRenderer.setDescription(ent, Text.of("Attributes\n" +
                "Max Hp: " + String.format("%.1f", ent.getHealth()) + "/" + String.format("%.1f", ent.MaxHp) +
                "\nSpeed: " + String.format("%.2f", ent.Speed) +
                "\nEnergy: " + String.format("%.1f", ent.ELvl)));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);

        if (itemStack.isOf(Items.HAY_BLOCK)) {
            if (ELvl < 20.0) {
                player.sendMessage(Text.of("This camel cannot breed because it has low energy."), true);
                return ActionResult.FAIL;
            }
            return super.interactMob(player, hand);
        }

        if (itemStack.isEmpty()) {
            if (!this.getWorld().isClient) {
                updateDescription(this);
            }
            return ActionResult.SUCCESS;
        }

        return super.interactMob(player, hand);
    }

    @Override
    public void onDeath(DamageSource source) {
        if (this.isBaby()) {
            return;
        }

        if (ELvl <= 0.0) {
            // Zero energy drops (e.g., minimal items)
            this.dropStack(new ItemStack(Items.LEATHER, 1));
        } else {
            super.onDeath(source);
            if (!this.getWorld().isClient) {
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient) {
            // Handle panic
            if (panicTicks > 0) {
                panicTicks--;
                if (panicTicks == 0) {
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(Speed * (ELvl / 100.0));
                }
            }

            // Handle energy loss from damage
            if (wasRecentlyHit) {
                ELvl = Math.max(0.0, ELvl * 0.8);
                wasRecentlyHit = false;
            }

            // Energy gain/loss based on environment
            boolean isOnEnergySource = this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.SAND);

            if (isOnEnergySource) {
                ELvl = Math.min(100.0, ELvl + 0.1);
            } else {
                ELvl = Math.max(0.0, ELvl - 0.05);
            }

            // Health regeneration at max energy
            if (ELvl == 100.0 && this.getHealth() < this.getMaxHealth()) {
                this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + 0.5F));
            }

            // Kill if energy is 0
            if (ELvl <= 0.0) {
                this.kill();
            } else {
                // Update speed
                if (panicTicks == 0) {
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(Speed * (ELvl / 100.0));
                }
                updateDescription(this);
            }
        }
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

        MobAttributes childAttributes = inheritAttributes(attr1, attr2);

        CustomCamelEntity child = new CustomCamelEntity(ModEntities.CUSTOM_CAMEL, serverWorld);

        child.mobAttributes = childAttributes;
        applyAttributes(child, childAttributes);

        child.MaxHp = childAttributes.getMaxHealth();
        child.ELvl = childAttributes.getEnergyLvl();
        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.MaxHp);
        child.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(child.Speed * (child.ELvl / 100.0));

        influenceGlobalAttributes(child.getType());

        if (!this.getWorld().isClient)
            updateDescription(child);

        return child;
    }

    @Override
    protected void applyDamage(DamageSource source, float amount) {
        super.applyDamage(source, amount);
        wasRecentlyHit = true;
        panicTicks = PANIC_DURATION;
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                .setBaseValue(Speed * (ELvl / 100.0) * PANIC_SPEED_MULTIPLIER);
        if (!this.getWorld().isClient)
            updateDescription(this);
    }

    @Override
    public void applyCustomAttributes(MobAttributes attributes) {
    }
}