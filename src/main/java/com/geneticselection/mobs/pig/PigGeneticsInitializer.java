package com.geneticselection.mobs.pig;

import net.minecraft.entity.EntityType;

public class PigGeneticsInitializer {
    public static void initialize() {
        GeneticsRegistry.register(EntityType.PIG, new PigGenetics());
    }
}
