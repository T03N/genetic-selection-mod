package com.geneticselection.mobs.cow;

import com.geneticselection.genetics.CowGenetics;
import com.geneticselection.genetics.GeneticsRegistry;
import net.minecraft.entity.EntityType;

public class CowGeneticsInitializer {
    public static void initialize() {
        GeneticsRegistry.register(EntityType.COW, new CowGenetics());
    }
}