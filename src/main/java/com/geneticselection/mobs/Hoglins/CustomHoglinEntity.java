package com.geneticselection.mobs.Hoglins;

import com.geneticselection.attributes.AttributeCarrier;
import com.geneticselection.attributes.AttributeKey;
import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.genetics.ChildInheritance;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.mobs.Zoglins.CustomZoglinEntity;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.Objects;
import java.util.Optional;
import static com.geneticselection.genetics.ChildInheritance.*;

public class CustomHoglinEntity extends HoglinEntity implements AttributeCarrier {
    private MobAttributes mobAttributes;
    private double MaxHp;
    private double Speed;
    private double ELvl;
    private double MaxMeat;
    private double MaxLeather;
    private int transformationTimer = 300; // 15 seconds at 20 ticks per second

    private int panicTicks = 0;
    private static final int PANIC_DURATION = 100;
    private static final double PANIC_SPEED_MULTIPLIER = 2.0;
    private boolean wasRecentlyHit = false;

    public CustomHoglinEntity(EntityType<? extends HoglinEntity> entityType, World world) {
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

        this.mobAttributes.getMaxMeat().ifPresent(maxMeat -> {
            this.MaxMeat = maxMeat;
        });
        this.mobAttributes.getMaxLeather().ifPresent(maxLeather -> {
            this.MaxLeather = maxLeather;
        });

        if (!this.getWorld().isClient)
            updateDescription(this);
    }

    private void updateDescription(CustomHoglinEntity ent) {
        DescriptionRenderer.setDescription(ent, Text.of("Attributes\n" +
                "Max Hp: " + String.format("%.1f", ent.getHealth()) + "/" + String.format("%.1f", ent.MaxHp) +
                "\nSpeed: " + String.format("%.2f", ent.Speed) +
                "\nEnergy: " + String.format("%.1f", ent.ELvl) +
                "\nMax Meat: " + String.format("%.1f", ent.MaxMeat) +
                "\nMax Leather: " + String.format("%.1f", ent.MaxLeather)));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);

        if (itemStack.isOf(Items.CRIMSON_NYLIUM)) {
            if (ELvl < 20.0) {
                player.sendMessage(Text.of("This hoglin cannot breed because it has low energy."), true);
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
            // Drop minimal resources
            this.dropStack(new ItemStack(Items.LEATHER, 1));
        } else {
            super.onDeath(source);
            if (!this.getWorld().isClient) {
                // Drop leather and meat based on energy
                int leatherAmount = (int) ((MaxLeather) * (ELvl / 100.0));
                this.dropStack(new ItemStack(Items.LEATHER, leatherAmount));

                int meatAmount = (int) ((MaxMeat) * (ELvl / 100.0));
                this.dropStack(new ItemStack(Items.PORKCHOP, meatAmount));
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient && this.getWorld().getDimension().bedWorks()) {
            // If not in the Nether, start countdown
            if (transformationTimer > 0) {
                transformationTimer--;
            } else {
                transformIntoZoglin();
            }
        }
    }

    @Override
    public boolean canConvert() {
        return false; // Prevents vanilla transformation into a Zoglin
    }

    private void transformIntoZoglin() {
        CustomZoglinEntity zoglin = new CustomZoglinEntity(ModEntities.CUSTOM_ZOGLIN, this.getWorld());

        // Copy position and rotation
        zoglin.copyPositionAndRotation(this);

        // Transfer attributes
        zoglin.setHealth(this.getHealth());
        Objects.requireNonNull(zoglin.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)).setBaseValue(this.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH));
        Objects.requireNonNull(zoglin.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)).setBaseValue(this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));

        if(this.isBaby()){
            zoglin.setBaby(true);
        }

        // Spawn the new zoglin and remove the hoglin
        this.getWorld().spawnEntity(zoglin);
        this.discard();
    }

    @Override
    public CustomHoglinEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof CustomHoglinEntity)) {
            return (CustomHoglinEntity) EntityType.HOGLIN.create(serverWorld);
        }

        CustomHoglinEntity parent1 = this;
        CustomHoglinEntity parent2 = (CustomHoglinEntity) mate;

        MobAttributes attr1 = parent1.mobAttributes;
        MobAttributes attr2 = parent2.mobAttributes;

        MobAttributes childAttributes = inheritAttributes(attr1, attr2);

        CustomHoglinEntity child = new CustomHoglinEntity(ModEntities.CUSTOM_HOGLIN, serverWorld);

        child.mobAttributes = childAttributes;
        applyAttributes(child, childAttributes);

        child.MaxHp = childAttributes.getMaxHealth();
        child.ELvl = childAttributes.getEnergyLvl();
        child.MaxMeat = childAttributes.get(AttributeKey.MAX_MEAT);
        child.MaxLeather = childAttributes.get(AttributeKey.MAX_LEATHER);
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
        attributes.getMaxMeat().ifPresent(maxMeat -> this.MaxMeat = maxMeat);
        attributes.getMaxLeather().ifPresent(maxLeather -> this.MaxLeather = maxLeather);
    }
}