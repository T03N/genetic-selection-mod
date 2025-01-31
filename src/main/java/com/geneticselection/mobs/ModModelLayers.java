package com.geneticselection.mobs;

import com.geneticselection.GeneticSelection;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class ModModelLayers {
    // Mob Model Initializers

    public static final EntityModelLayer CUSTOM_COW = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID,"custom_cow"), "main");

    public static final EntityModelLayer CUSTOM_SHEEP = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID,"custom_sheep"), "main");
    public static final EntityModelLayer CUSTOM_SHEEP_FUR = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID, "custom_sheep_fur"), "fur");

    public static final EntityModelLayer CUSTOM_PIG = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID, "custom_pig"), "main");

    public static final EntityModelLayer CUSTOM_RABBIT = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID, "custom_rabbit"), "main");

    public static final EntityModelLayer CUSTOM_DONKEY = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID,"custom_donkey"), "main");

    public static final EntityModelLayer CUSTOM_CAMEL = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID,"custom_camel"), "main");

    public static final EntityModelLayer CUSTOM_CHICKEN = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID,"custom_chicken"), "main");

}
