package com.geneticselection.mobs.Sheep;

import com.geneticselection.attributes.AttributeCarrier;
import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.Cows.CustomCowEntity;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.utils.DescriptionRenderer;
import com.geneticselection.utils.EatGrassGoal;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

import static com.geneticselection.genetics.ChildInheritance.*;

public class CustomSheepEntity extends SheepEntity implements AttributeCarrier {
    private MobAttributes mobAttributes; // Directly store MobAttributes for this entity
    private double MaxHp;
    private double Speed;
    private double ELvl;
    private double MaxEnergy = 100.0F;
    private double MaxMeat;
    private double MaxWool;
    private int breedingCooldown;

    private static int LIFESPAN = 35000;
    private static final int PANIC_DURATION = 100; // 5 seconds at 20 ticks per second
    private static final double PANIC_SPEED_MULTIPLIER = 1.25;
    private int panicTicks = 0;
    private boolean wasRecentlyHit = false;
    private int tickAge = 0;
    private int ticksSinceLastBreeding = 0;

    public CustomSheepEntity(EntityType<? extends SheepEntity> entityType, World world) {
        super(entityType, world);
        this.setSheared(false);

        // Initialize mob attributes (directly within the class)
        if (this.mobAttributes == null) {
            MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
            double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
            double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
            double energy = global.getEnergyLvl() * (0.9 + Math.random() * 0.1);
            double meat = global.getMaxMeat().orElse(0.0) + (0.98 + Math.random() * 0.1);
            double wool = global.getMaxRabbitHide().orElse(0.0) + (0.98 + Math.random() * 0.1);
            this.mobAttributes = new MobAttributes(speed, health, energy, Optional.of(meat), Optional.empty(), Optional.of(wool), Optional.empty(), Optional.empty());
        }

        // Apply attributes to the entity
        this.MaxHp = this.mobAttributes.getMaxHealth();
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
        this.setHealth((float)this.MaxHp);
        this.Speed = this.mobAttributes.getMovementSpeed();
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);
        this.ELvl = this.mobAttributes.getEnergyLvl();

        this.mobAttributes.getMaxMeat().ifPresent(maxMeat -> {
            this.MaxMeat = maxMeat;
        });
        this.mobAttributes.getMaxWool().ifPresent(MaxWool -> {
            this.MaxWool = MaxWool;
        });

        if (!this.getWorld().isClient)
            updateDescription(this);
    }

    @Override
    protected void initGoals() {
        super.initGoals();

        this.goalSelector.add(9, new EatGrassGoal(this));
    }

    public void setMaxMeat(double maxMeat)
    {
        this.MaxMeat = maxMeat;
    }

    public void setMaxWool(double maxWool)
    {
        this.MaxWool = maxWool;
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

    public double getEnergyLevel(){
        return this.ELvl;
    }

    @Override
    public void growUp(int age, boolean overGrow) {
        int i = this.getBreedingAge();
        int j = i;
        i += age * 20;
        if (i > 0) {
            i = 0;
        }

        int k = i - j;
        this.setBreedingAge(i);
        if (overGrow) {
            this.forcedAge += k;
            if (this.happyTicksRemaining == 0) {
                this.happyTicksRemaining = 40;
                this.MaxEnergy = 100.0F;
            }
        }

        if (this.getBreedingAge() == 0) {
            this.setBreedingAge(this.forcedAge);
        }
    }

    private void updateDescription(CustomSheepEntity ent) {

        DescriptionRenderer.setDescription(ent, Text.of("Attributes\n" +
                "Max Hp: " + String.format("%.3f", ent.getHealth()) + "/"+ String.format("%.3f", ent.MaxHp) +
                "\nSpeed: " + String.format("%.3f", ent.Speed) +
                "\nEnergy: " + String.format("%.3f", ent.ELvl) +
                "\nMax Meat: " + String.format("%.3f", ent.MaxMeat) +
                "\nMax Wool: " + String.format("%.3f", ent.MaxWool)));
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
            this.dropStack(new ItemStack(Items.MUTTON, meatAmount));

            // Calculate the amount of leather to drop between MinLeather and MaxLeather
            int woolAmount = (int)(MaxWool);
            this.dropStack(new ItemStack(Items.WHITE_WOOL, woolAmount));
        }
    }

    @Override
    protected void applyDamage(DamageSource source, float amount) {
        super.applyDamage(source, amount);

        // Mark the cow as recently hit
        wasRecentlyHit = true;

        // Start panic mode
        panicTicks = PANIC_DURATION;

        // Increase speed temporarily
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
            .setBaseValue(Speed * (ELvl / MaxEnergy) * PANIC_SPEED_MULTIPLIER);

        if (!this.getWorld().isClient) {
            updateDescription(this);
        }
    }

    @Override
    public CustomSheepEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof CustomSheepEntity))
            return (CustomSheepEntity) EntityType.SHEEP.create(serverWorld);

        CustomSheepEntity parent1 = this;
        CustomSheepEntity parent2 = (CustomSheepEntity) mate;

        // Calculate the inheritance factor based on the lower energy level of the parents
        double inheritanceFactor = Math.min(parent1.ELvl, parent2.ELvl) / MaxEnergy;

        // Inherit attributes from parents, scaled by the inheritance factor
        double childMaxHp = ((parent1.MaxHp + parent2.MaxHp) / 2) * inheritanceFactor;
        double childMaxMeat = ((parent1.MaxMeat + parent2.MaxMeat) / 2) * inheritanceFactor;
        int childBreedingCooldown = (int) (((parent1.breedingCooldown + parent2.breedingCooldown) / 2) * (1 / inheritanceFactor));
        double childEnergy = ((parent1.ELvl + parent2.ELvl) / 2) * inheritanceFactor;

        // Create the child entity
        CustomSheepEntity child = new CustomSheepEntity(ModEntities.CUSTOM_SHEEP, serverWorld);

        // Set inherited and calculated attributes
        child.MaxHp = childMaxHp;
        child.MaxMeat = childMaxMeat;
        child.breedingCooldown = childBreedingCooldown;
        child.ELvl = childEnergy;

        // Apply stats to the child entity
        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.MaxHp);
        child.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(child.Speed * (child.ELvl / MaxEnergy));

        parent1.ELvl -= parent1.ELvl * 0.4F;
        parent2.ELvl -= parent2.ELvl * 0.4F;
        this.resetLoveTicks();

        if (!this.getWorld().isClient)
            updateDescription(child);

        return child;
    }

    @Override
    public void tick()
    {
        super.tick();

        // Only perform energy adjustments on the server side
        if (!this.getWorld().isClient)
        {

            // Max energy is determined by age
            if (tickAge <= 4404)
            {
                MaxEnergy = 10 * Math.log(5 * tickAge + 5);
            } else if (tickAge < LIFESPAN)
            {
                MaxEnergy = 100;
            } else
            {
                MaxEnergy = -(tickAge - LIFESPAN) / 16.0 + 100;
            }
            tickAge++;

            if (tickAge >= 4404 && this.isBaby())
            {
                growUp(220, true);
            }

            // Clamp the current energy level to the maximum cap
            if (ELvl > MaxEnergy)
            {
                updateEnergyLevel(MaxEnergy);
            }

            // Handle panic state
            if (panicTicks > 0)
            {
                panicTicks--;
                if (panicTicks == 0)
                {
                    // Reset speed back to normal when panic ends
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                        .setBaseValue(Speed * (ELvl / MaxEnergy));
                }
            }

            // Handle energy loss if the cow was recently hit
            if (wasRecentlyHit)
            {
                // Reduce energy by 20% of its current level
                updateEnergyLevel(Math.max(0.0, ELvl * 0.8));
                wasRecentlyHit = false; // Reset the flag after applying the energy loss
            }

            // Check if the cow is standing on grass
            boolean isOnGrass =
                this.getWorld().getBlockState(this.getBlockPos().down()).isOf(Blocks.GRASS_BLOCK);

            // Adjust energy level randomly based on whether the cow is on grass
            if (isOnGrass)
            {
                if (Math.random() < 0.3)
                { // 30% chance to gain energy
                    updateEnergyLevel(Math.min(100.0,
                        ELvl + (0.1 + Math.random() * 0.75))); // Gain 0.1 to 0.75 energy
                }
            }

            if (Math.random() < 0.5)
            { // 50% chance to lose energy
                updateEnergyLevel(
                    Math.max(0.0, ELvl - (0.05 + Math.random() * 0.3))); // Lose 0.05 to 0.3 energy
            }

            // Check if energy is 100 and regenerate health if not at max
            if (ELvl == MaxEnergy)
            {
                if (this.getHealth() < this.getMaxHealth())
                {
                    this.setHealth(Math.min(this.getMaxHealth(),
                        this.getHealth() + 0.5F)); // Regenerate 0.5 HP per second
                }
            }

            if (ELvl >= 90.0 && !isBaby() && ticksSinceLastBreeding >= breedingCooldown)
            {
                double searchRadius = 32.0;

                List<CustomSheepEntity> mateCandidates = this.getWorld()
                    .getEntitiesByClass(CustomSheepEntity.class,
                        this.getBoundingBox().expand(searchRadius),
                        candidate -> candidate != this && candidate.getEnergyLevel() >= 90.0
                            && !candidate.isBaby());

                // Find the nearest candidate
                CustomSheepEntity nearestMate = null;
                double minDistanceSquared = Double.MAX_VALUE;
                for (CustomSheepEntity candidate : mateCandidates)
                {
                    double distSq = this.squaredDistanceTo(candidate);
                    if (distSq < minDistanceSquared)
                    {
                        minDistanceSquared = distSq;
                        nearestMate = candidate;
                    }
                }

                // If we found a mate candidate, move towards it
                if (nearestMate != null)
                {
                    // Start moving towards the nearest cow; adjust speed as needed
                    this.getNavigation()
                        .startMovingTo(nearestMate, this.Speed * 5.0F * (this.ELvl / MaxEnergy));

                    // If close enough (e.g., within 2 blocks; adjust the threshold as needed)
                    if (minDistanceSquared < 4.0)
                    {
                        // Only start breeding if both cows are not already in love
                        if (!this.isInLove() && !nearestMate.isInLove())
                        {
                            this.setLoveTicks(500);
                            nearestMate.setLoveTicks(500);
                            ticksSinceLastBreeding = 0;
                        }
                    }
                }
            }
            ticksSinceLastBreeding++;

            // If energy reaches 0, kill the cow
            if (ELvl <= 0.0)
            {
                this.kill(); // This makes the cow die
            } else
            {
                // Update attributes dynamically if energy is greater than 0
                if (panicTicks == 0)
                { // Only update speed if not in panic mode
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                        .setBaseValue(Speed * (ELvl / MaxEnergy));
                }

                // Update the description with the new energy level
                updateDescription(this);
            }
        }
    }

    @Override
    public void applyCustomAttributes(MobAttributes attributes) {
        attributes.getMaxMeat().ifPresent(this::setMaxMeat);
        attributes.getMaxWool().ifPresent(this::setMaxWool);
    }
}
