package com.geneticselection.mobs;

import com.geneticselection.GeneticSelection;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class ModModelLayers {
    public static final EntityModelLayer CUSTOM_COW = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID,"custom_cow"), "main");

    public static final EntityModelLayer CUSTOM_SHEEP = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID,"custom_sheep"), "main");

    //add your new mobs here by using the above format
}
