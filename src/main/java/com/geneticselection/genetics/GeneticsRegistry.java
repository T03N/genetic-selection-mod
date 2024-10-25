package com.geneticselection.genetics;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;

import java.util.HashMap;
import java.util.Map;

public class GeneticsRegistry {
    private static final Map<EntityType<? extends AnimalEntity>, Genetics> registry = new HashMap<>();

    // Register genetics/breeding logic for a specific entity type
    public static void register(EntityType<? extends AnimalEntity> type, Genetics genetics) {
        registry.put(type, genetics);
    }

    // Retrieve the genetics/breeding logic for a specific entity type
    public static Genetics getGenetics(EntityType<?> type) {
        return registry.get(type);
    }
}