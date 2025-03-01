package com.geneticselection.mobs;

import com.geneticselection.GeneticSelection;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class ModModelLayers {
    // Mob Model Initializers

    public static final EntityModelLayer CUSTOM_COW = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID,"custom_cow"), "main");

    public static final EntityModelLayer CUSTOM_SHEEP = new EntityModelLayer(
            Identifier.of(GeneticSelection.MOD_ID, "custom_sheep"), "main");

    public static final EntityModelLayer CUSTOM_SHEEP_FUR = new EntityModelLayer(
            Identifier.of(GeneticSelection.MOD_ID, "custom_sheep"), "wool");

    public static final EntityModelLayer CUSTOM_PIG = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID, "custom_pig"), "main");

    public static final EntityModelLayer CUSTOM_RABBIT = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID, "custom_rabbit"), "main");

    public static final EntityModelLayer CUSTOM_DONKEY = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID,"custom_donkey"), "main");

    public static final EntityModelLayer CUSTOM_CAMEL = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID,"custom_camel"), "main");

    public static final EntityModelLayer CUSTOM_CHICKEN = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID,"custom_chicken"), "main");

    public static final EntityModelLayer CUSTOM_WOLF = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID,"custom_wolf"), "main");
    public static final EntityModelLayer CUSTOM_HOGLIN = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID,"custom_hoglin"), "main");

    public static final EntityModelLayer CUSTOM_OCELOT = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID,"custom_ocelot"), "main");
    public static final EntityModelLayer CUSTOM_BEE = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID,"custom_bee"), "main");
    public static final EntityModelLayer CUSTOM_AXOLOTL = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID,"custom_axolotl"), "main");
    public static final EntityModelLayer CUSTOM_GOAT = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID,"custom_goat"), "main");
    public static final EntityModelLayer CUSTOM_FOX = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID,"custom_fox"), "main");
    public static final EntityModelLayer CUSTOM_MOOSHROOM = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID,"custom_mooshroom"), "main");
}
