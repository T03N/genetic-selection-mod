package com.geneticselection.genetics;

import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.mobs.sheep.SheepBreedingLogic;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.NotNull;

public class SheepGenetics implements Genetics {

    @Override
    public AnimalEntity breed(AnimalEntity parent1, AnimalEntity parent2, ServerWorld world) {
        if (world.isClient()) return null; // Server-side only

        // Ensure both parents are instances of SheepEntity
        if (parent1 instanceof SheepEntity sheep1 && parent2 instanceof SheepEntity sheep2) {
            return SheepBreedingLogic.breed(sheep1, sheep2, world);
        }
        return null;
    }

    // Custom logic to inherit mob attributes
    private MobAttributes inheritAttributes(MobAttributes a, MobAttributes b) {
        return getMobAttributes(a, b);
    }

    @NotNull
    public static MobAttributes getMobAttributes(MobAttributes a, MobAttributes b) {
        double speed = Math.random() < 0.5 ? a.getMovementSpeed() : b.getMovementSpeed();
        double health = Math.random() < 0.5 ? a.getMaxHealth() : b.getMaxHealth();
        if (Math.random() < 0.1) speed *= 1.05;  // Mutation
        if (Math.random() < 0.1) health *= 1.05; // Mutation
        return new MobAttributes(speed, health);
    }
}