package com.geneticselection.mobs.cow;

import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.interfaces.IGeneticEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.server.world.ServerWorld;
import static com.mojang.text2speech.Narrator.LOGGER;
import java.util.Random;

public class CowBreedingLogic {

    public static CowEntity breed(CowEntity parent1, CowEntity parent2, ServerWorld world) {
        MobAttributes attr1 = ((IGeneticEntity) parent1).getMobAttributes();
        MobAttributes attr2 = ((IGeneticEntity) parent2).getMobAttributes();

        // Calculate heritability (H^2) as a constant, which in real life depends on how much of the variation in a trait is due to genetic differences
        double heritability = 0.5; // This is a chosen value; in real populations it's often less than 1

        // Create the offspring entity
        CowEntity offspring = (CowEntity) EntityType.COW.create(world);

        if (offspring != null) {
            // Calculate position near parents
            double x = (parent1.getX() + parent2.getX()) / 2;
            double y = Math.min(parent1.getY(), parent2.getY()) + 1; // Slight offset to avoid spawn issues
            double z = (parent1.getZ() + parent2.getZ()) / 2;
            offspring.refreshPositionAndAngles(x, y, z, parent1.getYaw(), parent1.getPitch());

            // Optional: Set the offspring as a baby
            offspring.setBaby(true);

            // Apply inherited attributes using the breeder's equation
            MobAttributes childAttributes = inheritAttributes(attr1, attr2, heritability);
            ((IGeneticEntity) offspring).setMobAttributes(childAttributes);
            applyAttributes(offspring, childAttributes);

            LOGGER.info("Breeding cows: Parent1 ID={}, Parent2 ID={}", parent1.getId(), parent2.getId());

            // Spawn the offspring into the world
            world.spawnEntity(offspring);
        }

        // Influence global attributes based on the offspring
        influenceGlobalAttributes(offspring);
        return offspring;
    }

    private static MobAttributes inheritAttributes(MobAttributes a, MobAttributes b, double heritability) {
        // Calculate selection differential as the average difference between the two parent traits
        double selectionDifferentialSpeed = (a.getMovementSpeed() - b.getMovementSpeed()) / 2;
        double selectionDifferentialHealth = (a.getMaxHealth() - b.getMaxHealth()) / 2;

        // Calculate new trait values using the breeder's equation (R = h^2 * S)
        double newSpeed = (a.getMovementSpeed() + b.getMovementSpeed()) / 2 + heritability * selectionDifferentialSpeed;
        double newHealth = (a.getMaxHealth() + b.getMaxHealth()) / 2 + heritability * selectionDifferentialHealth;

        // Add a small mutation factor to simulate genetic drift
        Random random = new Random();
        double mutationFactor = 0.99; // was 0.1
        if (random.nextDouble() < mutationFactor) {
            newSpeed += random.nextGaussian() * 0.09; // was 0.01
            newHealth += random.nextGaussian() * 0.9; // was 0.1
        }

        // Ensure trait values are within reasonable bounds
        newSpeed = Math.max(0.1, Math.min(newSpeed, 1.0));
        newHealth = Math.max(1, Math.min(newHealth, 20));

        return new MobAttributes(newSpeed, newHealth);
    }

    private static void applyAttributes(CowEntity cow, MobAttributes attributes) {
        var speedAttribute = cow.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.setBaseValue(attributes.getMovementSpeed());
        }

        var healthAttribute = cow.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH);
        if (healthAttribute != null) {
            healthAttribute.setBaseValue(attributes.getMaxHealth());
        }

        cow.setHealth((float) attributes.getMaxHealth());
    }

    private static void influenceGlobalAttributes(CowEntity offspring) {
        if (offspring != null) {
            MobAttributes childAttributes = ((IGeneticEntity) offspring).getMobAttributes();
            if (childAttributes != null) {
                MobAttributes globalAttributes = GlobalAttributesManager.getAttributes(EntityType.COW);

                // Update global attributes using a moving average
                double updateRate = 0.01;
                double newMovementSpeed = globalAttributes.getMovementSpeed() * (1 - updateRate) + childAttributes.getMovementSpeed() * updateRate;
                double newMaxHealth = globalAttributes.getMaxHealth() * (1 - updateRate) + childAttributes.getMaxHealth() * updateRate;

                globalAttributes.setMovementSpeed(newMovementSpeed);
                globalAttributes.setMaxHealth(newMaxHealth);

                GlobalAttributesManager.updateGlobalAttributes(EntityType.COW, globalAttributes);
                LOGGER.info("Updated global cow attributes: Speed={}, Health={}",
                        globalAttributes.getMovementSpeed(), globalAttributes.getMaxHealth());
            }
        }
    }
}
