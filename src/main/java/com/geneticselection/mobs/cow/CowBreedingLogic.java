package com.geneticselection.mobs.cow;

import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.interfaces.IGeneticEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.server.world.ServerWorld;

public class CowBreedingLogic {

    public static CowEntity breed(CowEntity parent1, CowEntity parent2, ServerWorld world) {
        MobAttributes attr1 = ((IGeneticEntity) parent1).getMobAttributes();
        MobAttributes attr2 = ((IGeneticEntity) parent2).getMobAttributes();

        CowEntity offspring = (CowEntity) EntityType.COW.create(world);
        if (offspring != null) {
            MobAttributes childAttributes = inheritAttributes(attr1, attr2);
            ((IGeneticEntity) offspring).setMobAttributes(childAttributes);
            applyAttributes(offspring, childAttributes);
        }

        influenceGlobalAttributes(offspring);
        return offspring;
    }

    private static MobAttributes inheritAttributes(MobAttributes a, MobAttributes b) {
        double speed = Math.random() < 0.5 ? a.getMovementSpeed() : b.getMovementSpeed();
        double health = Math.random() < 0.5 ? a.getMaxHealth() : b.getMaxHealth();
        if (Math.random() < 0.1) speed *= 1.05;
        if (Math.random() < 0.1) health *= 1.05;
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
            }
        }
    }
}