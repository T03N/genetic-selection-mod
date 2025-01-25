package com.geneticselection.genetics;

import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.Camels.CustomCamelEntity;
import com.geneticselection.mobs.Cows.CustomCowEntity;
import com.geneticselection.mobs.Donkeys.CustomDonkeyEntity;
import com.geneticselection.mobs.Pigs.CustomPigEntity;
import com.geneticselection.mobs.Rabbit.CustomRabbitEntity;
import com.geneticselection.mobs.Rabbit.CustomRabbitRenderer;
import com.geneticselection.mobs.Sheep.CustomSheepEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AnimalEntity;

import java.util.Optional;
import java.util.Random;

import static com.mojang.text2speech.Narrator.LOGGER;

public class ChildInheritance {
    public static MobAttributes inheritAttributes(MobAttributes a, MobAttributes b) {
        double heritability = 0.5;
        Random random = new Random();

        // Calculate selection differential for speed and health
        double selectionDifferentialSpeed = (a.getMovementSpeed() - b.getMovementSpeed()) / 2;
        double selectionDifferentialHealth = (a.getMaxHealth() - b.getMaxHealth()) / 2;
        double selectionDifferentialEnergy = (a.getEnergyLvl() - b.getEnergyLvl()) / 2;

        // Inherit speed and health using the breeder's equation
        double newSpeed = (a.getMovementSpeed() + b.getMovementSpeed()) / 2 + heritability * selectionDifferentialSpeed;
        double newHealth = (a.getMaxHealth() + b.getMaxHealth()) / 2 + heritability * selectionDifferentialHealth;
        double newEnergy = (a.getEnergyLvl() + b.getEnergyLvl()) / 2 + heritability * selectionDifferentialEnergy;

        // Apply mutation (random genetic drift)
        double mutationFactor = 0.99;
        if (random.nextDouble() < mutationFactor) {
            newSpeed += random.nextGaussian() * 0.09;
            newHealth += random.nextGaussian() * 0.9;
        }

        // Ensure speed and health are within reasonable bounds
        newSpeed = Math.max(0.1, Math.min(newSpeed, 1.0));
        newHealth = Math.max(1, Math.min(newHealth, 20));
        newEnergy = Math.max(80.0, Math.min(newEnergy, 100));

        // Calculate and inherit optional attributes (maxMeat, maxLeather, maxWool, etc.)
        Optional<Double> newMaxMeat = inheritOptionalAttribute(a.getMaxMeat(), b.getMaxMeat(), random);
        Optional<Double> newMaxLeather = inheritOptionalAttribute(a.getMaxLeather(), b.getMaxLeather(), random);
        Optional<Double> newMaxWool = inheritOptionalAttribute(a.getMaxWool(), b.getMaxWool(), random);
        Optional<Double> newMaxRabbitHide = inheritOptionalAttribute(a.getMaxRabbitHide(), b.getMaxRabbitHide(), random);
        Optional<Double> newMaxFeathers = inheritOptionalAttribute(a.getMaxFeathers(), b.getMaxFeathers(), random);

        // Return a new MobAttributes object with the inherited values
        return new MobAttributes(newSpeed, newHealth, newEnergy, newMaxMeat, newMaxLeather, newMaxWool, newMaxRabbitHide, newMaxFeathers);
    }

    /**
     * Helper method to handle inheritance and mutation for optional attributes.
     */
    private static Optional<Double> inheritOptionalAttribute(Optional<Double> a, Optional<Double> b, Random random) {
        if (a != null && b != null) {
            if (a.isPresent() && b.isPresent()) {
                // Calculate selection differential
                double selectionDifferential = Math.abs(a.get() - b.get()) / 2;
                // Apply heritability and mutation
                double heritability = 0.5;
                double newValue = (a.get() + b.get()) / 2 + (int)(heritability * selectionDifferential);

                // Apply mutation (random drift)
                double mutationFactor = 0.99;
                if (random.nextDouble() < mutationFactor) {
                    newValue += random.nextInt(3) - 1; // Small random change (-1, 0, +1)
                }

                // Ensure the new value is within reasonable bounds (for example, 0 to 10)
                newValue = Math.max(0, Math.min(newValue, 10));

                return Optional.of(newValue);
            }
        }

        // If either parent doesn't have the attribute, return empty
        return Optional.empty();
    }

    public static void applyAttributes(Entity entity, MobAttributes attributes) {
        EntityAttributeInstance speedAttribute = null;
        EntityAttributeInstance healthAttribute = null;

        if (entity instanceof AnimalEntity animal) {
            speedAttribute = animal.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            healthAttribute = animal.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        }

        if (speedAttribute != null) {
            speedAttribute.setBaseValue(attributes.getMovementSpeed());
        }
        if (healthAttribute != null) {
            healthAttribute.setBaseValue(attributes.getMaxHealth());
            ((AnimalEntity) entity).setHealth((float) attributes.getMaxHealth());
        }

        // Handle Optional attributes for drops
        if (entity instanceof CustomCowEntity cow) {
            attributes.getMaxMeat().ifPresent(cow::setMaxMeat);
            attributes.getMaxLeather().ifPresent(cow::setMaxLeather);
        }

        if (entity instanceof CustomSheepEntity sheep) {
            attributes.getMaxMeat().ifPresent(sheep::setMaxMeat);
            attributes.getMaxWool().ifPresent(sheep::setMaxWool);
        }

        if (entity instanceof CustomDonkeyEntity donkey) {
            attributes.getMaxLeather().ifPresent(donkey::setMaxLeather);
        }

        if (entity instanceof CustomPigEntity pig) {
            attributes.getMaxMeat().ifPresent(pig::setMaxMeat);
        }

        if (entity instanceof CustomRabbitEntity rabbit) {
            attributes.getMaxMeat().ifPresent(rabbit::setMaxMeat);
            attributes.getMaxRabbitHide().ifPresent(rabbit::setRabbitHide);
        }

        if (entity instanceof CustomCamelEntity camel) {
        }
    }

    public static void influenceGlobalAttributes(EntityType<?> offspring) {
        if (offspring != null) {
            MobAttributes childAttributes = GlobalAttributesManager.getAttributes(offspring);

            if (childAttributes != null) {
                MobAttributes globalAttributes = GlobalAttributesManager.getAttributes(offspring);

                // Update global attributes using a moving average
                double updateRate = 0.01;
                double newMovementSpeed = globalAttributes.getMovementSpeed() * (1 - updateRate) + childAttributes.getMovementSpeed() * updateRate;
                double newMaxHealth = globalAttributes.getMaxHealth() * (1 - updateRate) + childAttributes.getMaxHealth() * updateRate;

                globalAttributes.setMovementSpeed(newMovementSpeed);
                globalAttributes.setMaxHealth(newMaxHealth);

                GlobalAttributesManager.updateGlobalAttributes(offspring, globalAttributes);
                LOGGER.info("Updated global attributes: Speed={}, Health={}",
                        globalAttributes.getMovementSpeed(), globalAttributes.getMaxHealth());
            }
        }
    }
}
