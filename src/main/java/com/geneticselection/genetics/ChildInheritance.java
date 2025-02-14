package com.geneticselection.genetics;

import com.geneticselection.attributes.AttributeKey;
import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.Camels.CustomCamelEntity;
import com.geneticselection.mobs.Cows.CustomCowEntity;
import com.geneticselection.mobs.Donkeys.CustomDonkeyEntity;
import com.geneticselection.mobs.Pigs.CustomPigEntity;
import com.geneticselection.mobs.Rabbit.CustomRabbitEntity;
import com.geneticselection.mobs.Sheep.CustomSheepEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AnimalEntity;

import java.util.Random;

import static com.mojang.text2speech.Narrator.LOGGER;

public class ChildInheritance {
    public static MobAttributes inheritAttributes(MobAttributes a, MobAttributes b) {
        double heritability = 0.5;
        Random random = new Random();

        double selectionDifferentialSpeed = (a.get(AttributeKey.MOVEMENT_SPEED) - b.get(AttributeKey.MOVEMENT_SPEED)) / 2;
        double selectionDifferentialHealth = (a.get(AttributeKey.MAX_HEALTH) - b.get(AttributeKey.MAX_HEALTH)) / 2;
        double selectionDifferentialEnergy = (a.get(AttributeKey.ENERGY) - b.get(AttributeKey.ENERGY)) / 2;

        double newSpeed = (a.get(AttributeKey.MOVEMENT_SPEED) + b.get(AttributeKey.MOVEMENT_SPEED)) / 2 + heritability * selectionDifferentialSpeed;
        double newHealth = (a.get(AttributeKey.MAX_HEALTH) + b.get(AttributeKey.MAX_HEALTH)) / 2 + heritability * selectionDifferentialHealth;
        double newEnergy = (a.get(AttributeKey.ENERGY) + b.get(AttributeKey.ENERGY)) / 2 + heritability * selectionDifferentialEnergy;

        double mutationFactor = 0.99;
        if (random.nextDouble() < mutationFactor) {
            newSpeed += random.nextGaussian() * 0.09;
            newHealth += random.nextGaussian() * 0.9;
        }

        newSpeed = Math.max(0.1, Math.min(newSpeed, 1.0));
        newHealth = Math.max(1, Math.min(newHealth, 20));
        newEnergy = Math.max(80.0, Math.min(newEnergy, 100));

        double newMaxMeat = inheritOptional(a, b, AttributeKey.MAX_MEAT, random);
        double newMaxLeather = inheritOptional(a, b, AttributeKey.MAX_LEATHER, random);
        double newMaxWool = inheritOptional(a, b, AttributeKey.MAX_WOOL, random);
        double newMaxRabbitHide = inheritOptional(a, b, AttributeKey.MAX_RABBIT_HIDE, random);
        double newMaxFeathers = inheritOptional(a, b, AttributeKey.MAX_FEATHERS, random);

        java.util.Map<AttributeKey, Double> newAttrMap = new java.util.HashMap<>();
        newAttrMap.put(AttributeKey.MOVEMENT_SPEED, newSpeed);
        newAttrMap.put(AttributeKey.MAX_HEALTH, newHealth);
        newAttrMap.put(AttributeKey.ENERGY, newEnergy);
        if (newMaxMeat >= 0) {
            newAttrMap.put(AttributeKey.MAX_MEAT, newMaxMeat);
        }
        if (newMaxLeather >= 0) {
            newAttrMap.put(AttributeKey.MAX_LEATHER, newMaxLeather);
        }
        if (newMaxWool >= 0) {
            newAttrMap.put(AttributeKey.MAX_WOOL, newMaxWool);
        }
        if (newMaxRabbitHide >= 0) {
            newAttrMap.put(AttributeKey.MAX_RABBIT_HIDE, newMaxRabbitHide);
        }
        if (newMaxFeathers >= 0) {
            newAttrMap.put(AttributeKey.MAX_FEATHERS, newMaxFeathers);
        }
        return new MobAttributes(newAttrMap);
    }

    private static double inheritOptional(MobAttributes a, MobAttributes b, AttributeKey key, Random random) {
        double valA = a.get(key);
        double valB = b.get(key);
        if (valA == 0 && valB == 0) {
            return -1;
        }
        double selectionDifferential = Math.abs(valA - valB) / 2;
        double heritability = 0.5;
        double newValue = (valA + valB) / 2 + (heritability * selectionDifferential);
        double mutationFactor = 0.99;
        if (random.nextDouble() < mutationFactor) {
            newValue += random.nextInt(3) - 1;
        }
        newValue = Math.max(0, Math.min(newValue, 10));
        return newValue;
    }

    public static void applyAttributes(Entity entity, MobAttributes attributes) {
        EntityAttributeInstance speedAttribute = null;
        EntityAttributeInstance healthAttribute = null;

        if (entity instanceof AnimalEntity animal) {
            speedAttribute = animal.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            healthAttribute = animal.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        }

        if (speedAttribute != null) {
            speedAttribute.setBaseValue(attributes.get(AttributeKey.MOVEMENT_SPEED));
        }
        if (healthAttribute != null) {
            healthAttribute.setBaseValue(attributes.get(AttributeKey.MAX_HEALTH));
            ((AnimalEntity) entity).setHealth((float) attributes.get(AttributeKey.MAX_HEALTH));
        }

        if (entity instanceof CustomCowEntity cow) {
            if(attributes.get(AttributeKey.MAX_MEAT) > 0) cow.setMaxMeat(attributes.get(AttributeKey.MAX_MEAT));
            if(attributes.get(AttributeKey.MAX_LEATHER) > 0) cow.setMaxLeather(attributes.get(AttributeKey.MAX_LEATHER));
        }
        if (entity instanceof CustomSheepEntity sheep) {
            if(attributes.get(AttributeKey.MAX_MEAT) > 0) sheep.setMaxMeat(attributes.get(AttributeKey.MAX_MEAT));
            if(attributes.get(AttributeKey.MAX_WOOL) > 0) sheep.setMaxWool(attributes.get(AttributeKey.MAX_WOOL));
        }
        if (entity instanceof CustomDonkeyEntity donkey) {
            if(attributes.get(AttributeKey.MAX_LEATHER) > 0) donkey.setMaxLeather(attributes.get(AttributeKey.MAX_LEATHER));
        }
        if (entity instanceof CustomPigEntity pig) {
            if(attributes.get(AttributeKey.MAX_MEAT) > 0) pig.setMaxMeat(attributes.get(AttributeKey.MAX_MEAT));
        }
        if (entity instanceof CustomRabbitEntity rabbit) {
            if(attributes.get(AttributeKey.MAX_MEAT) > 0) rabbit.setMaxMeat(attributes.get(AttributeKey.MAX_MEAT));
            if(attributes.get(AttributeKey.MAX_RABBIT_HIDE) > 0) rabbit.setRabbitHide(attributes.get(AttributeKey.MAX_RABBIT_HIDE));
        }
        if (entity instanceof CustomCamelEntity camel) {
            // Camel-specific attributes can be applied here if needed.
        }
    }

    public static void influenceGlobalAttributes(EntityType<?> offspring) {
        if (offspring != null) {
            MobAttributes childAttributes = GlobalAttributesManager.getAttributes(offspring);
            if (childAttributes != null) {
                MobAttributes globalAttr = GlobalAttributesManager.getAttributes(offspring);
                double updateRate = 0.01;
                double newMovementSpeed = globalAttr.get(AttributeKey.MOVEMENT_SPEED) * (1 - updateRate) + childAttributes.get(AttributeKey.MOVEMENT_SPEED) * updateRate;
                double newMaxHealth = globalAttr.get(AttributeKey.MAX_HEALTH) * (1 - updateRate) + childAttributes.get(AttributeKey.MAX_HEALTH) * updateRate;
                globalAttr.set(AttributeKey.MOVEMENT_SPEED, newMovementSpeed);
                globalAttr.set(AttributeKey.MAX_HEALTH, newMaxHealth);
                GlobalAttributesManager.updateGlobalAttributes(offspring, globalAttr);
                LOGGER.info("Updated global attributes: Speed={}, Health={}", newMovementSpeed, newMaxHealth);
            }
        }
    }
}
