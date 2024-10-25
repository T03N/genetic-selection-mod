package com.geneticselection.genetics;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.world.ServerWorld;

public interface Genetics {
    AnimalEntity breed(AnimalEntity parent1, AnimalEntity parent2, ServerWorld world);
}