package com.geneticselection.mobs.sheep;

import com.geneticselection.genetics.SheepGenetics;
import com.geneticselection.genetics.GeneticsRegistry;
import net.minecraft.entity.EntityType;

public class SheepGeneticsInitializer {
    public static void initialize() {
        GeneticsRegistry.register(EntityType.SHEEP, new SheepGenetics());
    }
}