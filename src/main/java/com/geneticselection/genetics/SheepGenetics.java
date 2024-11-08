package com.geneticselection.genetics;

import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.individual.MobIndividualAttributes;
import com.geneticselection.mobs.sheep.SheepBreedingLogic;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.NotNull;

public class SheepGenetics implements Genetics {
    @Override
    public AnimalEntity breed(AnimalEntity parent1, AnimalEntity parent2, ServerWorld world) {
        if (isValidBreedingPair(parent1, parent2)) {
            return SheepBreedingLogic.breed((SheepEntity) parent1, (SheepEntity) parent2, world);
        }
        return null;
    }

    private boolean isValidBreedingPair(AnimalEntity parent1, AnimalEntity parent2) {
        return parent1 instanceof SheepEntity && parent2 instanceof SheepEntity;
    }

}