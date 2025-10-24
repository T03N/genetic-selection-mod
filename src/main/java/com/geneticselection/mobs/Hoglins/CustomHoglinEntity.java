package com.geneticselection.mobs.Hoglins;

import com.geneticselection.attributes.AttributeCarrier;
import com.geneticselection.attributes.AttributeKey;
import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.mobs.Zoglins.CustomZoglinEntity;
import com.geneticselection.utils.DescriptionRenderer;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.List;
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
    private double baseAttackDamage; // Base damage that improves through breeding
    private int transformationTimer = 300; // 15 seconds at 20 ticks per second

    private int panicTicks = 0;
    private static final int PANIC_DURATION = 100;
    private static final double PANIC_SPEED_MULTIPLIER = 2.0;
    private boolean wasRecentlyHit = false;

    // Damage scaling constants
    private static final double MAX_ATTACK_DAMAGE_CAP = 12.0; // 2x vanilla (6.0 is vanilla)
    private static final double BREEDING_DAMAGE_BONUS = 0.1; // 10% improvement per generation

    public CustomHoglinEntity(EntityType<? extends HoglinEntity> entityType, World world) {
        super(entityType, world);

        if (this.mobAttributes == null) {
            MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
            double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
            double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
            double energy = global.getEnergyLvl() * (0.9 + Math.random() * 0.1);
            double meat = global.getMaxMeat().orElse(0.0) + (0.98 + Math.random() * 0.1);
            double leather = global.getMaxLeather().orElse(0.0) + (0.98 + Math.random() * 0.1);
            double attackDamage = global.getAttackDamage().orElse(6.0) * (0.98 + Math.random() * 0.1);

            this.mobAttributes = new MobAttributes(
                    speed, health, energy,
                    Optional.of(meat), Optional.of(leather),
                    Optional.of(attackDamage), Optional.empty(), Optional.empty(), Optional.empty()
            );
        }

        this.MaxHp = this.mobAttributes.getMaxHealth();
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
        this.setHealth((float)this.MaxHp);

        this.Speed = this.mobAttributes.getMovementSpeed();
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);

        this.ELvl = this.mobAttributes.getEnergyLvl();

        // Initialize base attack damage
        this.baseAttackDamage = this.mobAttributes.getAttackDamage().orElse(6.0);
        this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(this.baseAttackDamage);

        this.mobAttributes.getMaxMeat().ifPresent(maxMeat -> {
            this.MaxMeat = maxMeat;
        });
        this.mobAttributes.getMaxLeather().ifPresent(maxLeather -> {
            this.MaxLeather = maxLeather;
        });

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

    public double getEnergyLevel(){
        return this.ELvl;
    }

    private void syncEnergyLevelToClient() {
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
        data.writeInt(this.getId());  // Send entity ID
        data.writeDouble(this.ELvl);  // Send the updated energy level
    }

    private void updateDescription(CustomHoglinEntity ent) {
        // Calculate current effective damage (scaled by energy)
        double effectiveDamage = ent.baseAttackDamage * (ent.ELvl / 100.0);

        DescriptionRenderer.setDescription(ent, Text.of("Attributes\n" +
                "Max Hp: " + String.format("%.1f", ent.getHealth()) + "/" + String.format("%.1f", ent.MaxHp) +
                "\nSpeed: " + String.format("%.2f", ent.Speed) +
                "\nEnergy: " + String.format("%.1f", ent.ELvl) +
                "\nBase Damage: " + String.format("%.1f", ent.baseAttackDamage) +
                "\nCurrent Damage: " + String.format("%.1f", effectiveDamage) +
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

        if (!this.getWorld().isClient) {
            boolean shouldDropCooked = false;

            // Check if the entity died from fire, lava, or burning
            if (source.getName().equals("onFire") || source.getName().equals("inFire") || source.getName().equals("lava")) {
                shouldDropCooked = true;
            }

            // Check if the attacker has Fire Aspect
            if (source.getAttacker() instanceof LivingEntity attacker) {
                ItemStack weapon = attacker.getMainHandStack();
                RegistryEntry<Enchantment> fireAspectEntry = this.getWorld().getServer().getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Enchantments.FIRE_ASPECT).get();
                if (EnchantmentHelper.getLevel(fireAspectEntry ,weapon) >= 1) {
                    shouldDropCooked = true;
                }
            }

            // Drop cooked or raw chicken based on conditions
            if(shouldDropCooked) {
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
                        this.dropStack(new ItemStack(Items.COOKED_PORKCHOP, meatAmount));
                    }
                }
            }
            else{
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
            boolean isOnEnergySource = this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.CRIMSON_NYLIUM);

            if (isOnEnergySource) {
                ELvl = Math.min(100.0, ELvl + 0.1);
            } else {
                ELvl = Math.max(0.0, ELvl - 0.05);
            }

            // Health regeneration at max energy
            if (ELvl == 100.0 && this.getHealth() < this.getMaxHealth()) {
                this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + 0.5F));
            }

            if (ELvl >= 90.0) {
                double searchRadius = 32.0;

                List<CustomHoglinEntity> mateCandidates = this.getWorld().getEntitiesByClass(
                        CustomHoglinEntity.class,
                        this.getBoundingBox().expand(searchRadius),
                        candidate -> candidate != this && candidate.getEnergyLevel() >= 90.0 && !candidate.isBaby()
                );

                // Find the nearest candidate
                CustomHoglinEntity nearestMate = null;
                double minDistanceSquared = Double.MAX_VALUE;
                for (CustomHoglinEntity candidate : mateCandidates) {
                    double distSq = this.squaredDistanceTo(candidate);
                    if (distSq < minDistanceSquared) {
                        minDistanceSquared = distSq;
                        nearestMate = candidate;
                    }
                }

                // If we found a mate candidate, move towards it
                if (nearestMate != null) {
                    // Start moving towards the nearest cow; adjust speed as needed
                    this.getNavigation().startMovingTo(nearestMate, this.Speed * 5.0F * (this.ELvl / 100.0));

                    // If close enough (e.g., within 2 blocks; adjust the threshold as needed)
                    if (minDistanceSquared < 4.0) {
                        // Only start breeding if both cows are not already in love
                        if (!this.isInLove() && !nearestMate.isInLove()) {
                            this.setLoveTicks(100);
                            nearestMate.setLoveTicks(100);
                        }
                    }
                }
            }

            // Kill if energy is 0
            if (ELvl <= 0.0) {
                this.kill();
            } else {
                // Update speed based on energy
                if (panicTicks == 0) {
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(Speed * (ELvl / 100.0));
                }

                // Update attack damage based on energy
                updateDamageBasedOnEnergy();

                updateDescription(this);
            }
        }
    }

    /**
     * Scales attack damage based on current energy level.
     * Higher energy = full damage, lower energy = reduced damage.
     */
    private void updateDamageBasedOnEnergy() {
        double energyRatio = ELvl / 100.0;
        double effectiveDamage = baseAttackDamage * energyRatio;
        this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(effectiveDamage);
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

        // Apply breeding bonus to attack damage (incremental improvement)
        double parentAvgDamage = (parent1.baseAttackDamage + parent2.baseAttackDamage) / 2.0;
        double improvedDamage = parentAvgDamage * (1.0 + BREEDING_DAMAGE_BONUS);

        // Cap at maximum to prevent infinite scaling
        improvedDamage = Math.min(improvedDamage, MAX_ATTACK_DAMAGE_CAP);

        // Update the child's attack damage attribute
        childAttributes.set(AttributeKey.ATTACK_DAMAGE, improvedDamage);

        CustomHoglinEntity child = new CustomHoglinEntity(ModEntities.CUSTOM_HOGLIN, serverWorld);

        child.mobAttributes = childAttributes;
        applyAttributes(child, childAttributes);

        child.MaxHp = childAttributes.getMaxHealth();
        child.ELvl = childAttributes.getEnergyLvl();
        child.MaxMeat = childAttributes.get(AttributeKey.MAX_MEAT);
        child.MaxLeather = childAttributes.get(AttributeKey.MAX_LEATHER);
        child.baseAttackDamage = improvedDamage; // Set the improved damage

        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.MaxHp);
        child.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(child.Speed * (child.ELvl / 100.0));
        child.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(child.baseAttackDamage * (child.ELvl / 100.0));

        parent1.ELvl -= parent1.ELvl * 0.4F;
        parent2.ELvl -= parent2.ELvl * 0.4F;
        this.resetLoveTicks();

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
        attributes.getAttackDamage().ifPresent(attackDamage -> this.baseAttackDamage = attackDamage);
    }
}