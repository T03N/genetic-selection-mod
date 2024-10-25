package com.geneticselection.custommobs.Cows;

import com.geneticselection.GeneticSelection;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class ModModleLayers {
    public static final EntityModelLayer CUSTOM_COW =
            new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID,"custom_cow"), "main");
}
