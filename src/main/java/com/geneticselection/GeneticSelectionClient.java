package com.geneticselection;

import com.geneticselection.mobs.Axolotl.CustomAxolotlEntity;
import com.geneticselection.mobs.Axolotl.CustomAxolotlRenderer;
import com.geneticselection.mobs.Bee.CustomBeeEntity;
import com.geneticselection.mobs.Bee.CustomBeeRenderer;
import com.geneticselection.mobs.Camels.CustomCamelRenderer;
import com.geneticselection.mobs.Chickens.CustomChickenRenderer;
import com.geneticselection.mobs.Cows.CustomCowRenderer;
import com.geneticselection.mobs.Donkeys.CustomDonkeyRenderer;
import com.geneticselection.mobs.Fox.CustomFoxRenderer;
import com.geneticselection.mobs.Goat.CustomGoatEntity;
import com.geneticselection.mobs.Goat.CustomGoatRenderer;
import com.geneticselection.mobs.Hoglins.CustomHoglinEntity;
import com.geneticselection.mobs.Hoglins.CustomHoglinRenderer;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.mobs.ModModelLayers;
import com.geneticselection.mobs.Mooshroom.CustomMooshroomRenderer;
import com.geneticselection.mobs.Ocelots.CustomOcelotRenderer;
import com.geneticselection.mobs.Rabbit.CustomRabbitRenderer;
import com.geneticselection.mobs.Sheep.CustomSheepRenderer;
import com.geneticselection.mobs.Pigs.CustomPigRenderer;
import com.geneticselection.mobs.Wolves.CustomWolfRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.block.MushroomBlock;
import net.minecraft.block.MushroomPlantBlock;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.*;
import net.minecraft.client.model.Dilation;
import net.minecraft.entity.passive.MooshroomEntity;

public class GeneticSelectionClient implements ClientModInitializer {
    public void cowMethod(){
        //register your cow
        EntityRendererRegistry.register(ModEntities.CUSTOM_COW, CustomCowRenderer::new);
        //register your model layer for your cow
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.CUSTOM_COW, CowEntityModel::getTexturedModelData);
    }
    public void sheepMethod(){
        EntityRendererRegistry.register(ModEntities.CUSTOM_SHEEP, CustomSheepRenderer::new);

        // Register both the base model and wool model layers
        EntityModelLayerRegistry.registerModelLayer(
                ModModelLayers.CUSTOM_SHEEP,
                SheepEntityModel::getTexturedModelData
        );

        EntityModelLayerRegistry.registerModelLayer(
                ModModelLayers.CUSTOM_SHEEP_FUR,
                SheepWoolEntityModel::getTexturedModelData
        );
    }

    public void pigMethod() {
        // Register custom pig renderer
        EntityRendererRegistry.register(ModEntities.CUSTOM_PIG, CustomPigRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.CUSTOM_PIG,
                () -> PigEntityModel.getTexturedModelData(new Dilation(0.2F))); // Apply dilation here
    }

    public void rabbitMethod() {
        EntityRendererRegistry.register(ModEntities.CUSTOM_RABBIT, CustomRabbitRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.CUSTOM_RABBIT, RabbitEntityModel::getTexturedModelData);
    }

    public void donkeyMethod() {
        EntityRendererRegistry.register(ModEntities.CUSTOM_DONKEY, CustomDonkeyRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.CUSTOM_DONKEY, DonkeyEntityModel::getTexturedModelData);
    }

    public void camelMethod() {
        EntityRendererRegistry.register(ModEntities.CUSTOM_CAMEL, CustomCamelRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.CUSTOM_CAMEL, CamelEntityModel::getTexturedModelData);
    }

    public void chickenMethod() {
        EntityRendererRegistry.register(ModEntities.CUSTOM_CHICKEN, CustomChickenRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.CUSTOM_CHICKEN, ChickenEntityModel::getTexturedModelData);
    }

    public void wolfMethod() {
        EntityRendererRegistry.register(ModEntities.CUSTOM_WOLF, CustomWolfRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.CUSTOM_WOLF,
                () -> TexturedModelData.of(WolfEntityModel.getTexturedModelData(new Dilation(0.2F)), 64, 32)); // Apply dilation here
    }
 
    public void hoglinMethod() {
        EntityRendererRegistry.register(ModEntities.CUSTOM_HOGLIN, CustomHoglinRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.CUSTOM_HOGLIN, HoglinEntityModel::getTexturedModelData);
    }

    public void beeMethod() {
        EntityRendererRegistry.register(ModEntities.CUSTOM_BEE, CustomBeeRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.CUSTOM_BEE, BeeEntityModel::getTexturedModelData);
    }

    public void axolotlMethod() {
        EntityRendererRegistry.register(ModEntities.CUSTOM_AXOLOTL, CustomAxolotlRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.CUSTOM_AXOLOTL, AxolotlEntityModel::getTexturedModelData);
    }

    public void ocelotMethod() {
        EntityRendererRegistry.register(ModEntities.CUSTOM_OCELOT, CustomOcelotRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.CUSTOM_OCELOT,
                () -> TexturedModelData.of(OcelotEntityModel.getModelData(new Dilation(0.2F)), 64, 32)); // Apply dilation here
    }

    public void goatMethod() {
        EntityRendererRegistry.register(ModEntities.CUSTOM_GOAT, CustomGoatRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.CUSTOM_GOAT, GoatEntityModel::getTexturedModelData);
    }

    public void foxMethod() {
        EntityRendererRegistry.register(ModEntities.CUSTOM_FOX, CustomFoxRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.CUSTOM_FOX, FoxEntityModel::getTexturedModelData);
    }

    public void mooshroomMethod() {
        EntityRendererRegistry.register(ModEntities.CUSTOM_MOOSHROOM, CustomMooshroomRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.CUSTOM_MOOSHROOM, CowEntityModel::getTexturedModelData);
    }

    @Override
    public void onInitializeClient() {
        cowMethod();
        sheepMethod();
        pigMethod();
        rabbitMethod();
        donkeyMethod();
        camelMethod();
        chickenMethod();
        wolfMethod();
        hoglinMethod();
        beeMethod();
        axolotlMethod();
        ocelotMethod();
        goatMethod();
        foxMethod();
        mooshroomMethod();
    }
}
