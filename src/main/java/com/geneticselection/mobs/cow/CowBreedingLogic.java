package com.geneticselection.mobs.cow;

import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.interfaces.IGeneticEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.server.world.ServerWorld;

import static com.mojang.text2speech.Narrator.LOGGER;

public class CowBreedingLogic {

    public static CowEntity breed(CowEntity parent1, CowEntity parent2, ServerWorld world) {
        MobAttributes attr1 = ((IGeneticEntity) parent1).getMobAttributes();
        MobAttributes attr2 = ((IGeneticEntity) parent2).getMobAttributes();

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

            // Apply inherited attributes
            MobAttributes childAttributes = inheritAttributes(attr1, attr2);
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

    private static MobAttributes inheritAttributes(MobAttributes a, MobAttributes b) {
        double speed = Math.random() < 0.5 ? a.getMovementSpeed() : b.getMovementSpeed();
        double health = Math.random() < 0.5 ? a.getMaxHealth() : b.getMaxHealth();
        if (Math.random() < 0.1) speed *= 1.05;  // Mutation
        if (Math.random() < 0.1) health *= 1.05; // Mutation
        return new MobAttributes(speed, health);
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
                globalAttributes.setMovementSpeed((globalAttributes.getMovementSpeed() + childAttributes.getMovementSpeed()) / 2);
                globalAttributes.setMaxHealth((globalAttributes.getMaxHealth() + childAttributes.getMaxHealth()) / 2);
                GlobalAttributesManager.updateGlobalAttributes(EntityType.COW, globalAttributes);
                LOGGER.info("Updated global cow attributes: Speed={}, Health={}",
                        globalAttributes.getMovementSpeed(), globalAttributes.getMaxHealth());
                // Ensure the global attributes are saved in the save event
            }
        }
    }
}