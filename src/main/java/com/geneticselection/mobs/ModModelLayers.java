package com.geneticselection.mobs;

import com.geneticselection.GeneticSelection;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class ModModelLayers {
    // Model layer for the custom cow, identified by "custom_cow"
    public static final EntityModelLayer CUSTOM_COW = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID, "custom_cow"), "main");

    // Model layer for the custom pig, identified by "custom_pig"
    public static final EntityModelLayer CUSTOM_PIG = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID, "custom_pig"), "main");
}
