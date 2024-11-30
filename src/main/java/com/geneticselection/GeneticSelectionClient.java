package com.geneticselection;

import com.geneticselection.mobs.Cows.CustomCowRenderer;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.mobs.ModModelLayers;
import com.geneticselection.mobs.Rabbit.CustomRabbitRenderer;
import com.geneticselection.mobs.Sheep.CustomSheepRenderer;
import com.geneticselection.mobs.Pigs.CustomPigRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.model.CowEntityModel;
import net.minecraft.client.render.entity.model.PigEntityModel;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.render.entity.model.RabbitEntityModel;
import net.minecraft.client.render.entity.model.SheepEntityModel;

public class GeneticSelectionClient implements ClientModInitializer {
    public void cowMethod(){
        //register your cow
        EntityRendererRegistry.register(ModEntities.CUSTOM_COW, CustomCowRenderer::new);
        //register your model layer for your cow
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.CUSTOM_COW, CowEntityModel::getTexturedModelData);
    }
    public void sheepMethod(){
        //register your cow
        EntityRendererRegistry.register(ModEntities.CUSTOM_SHEEP, CustomSheepRenderer::new);
        //register your model layer for your cow
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.CUSTOM_SHEEP, SheepEntityModel::getTexturedModelData);
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

    @Override
    public void onInitializeClient() {
        cowMethod();
        sheepMethod();
        pigMethod();
        rabbitMethod();
    }
}
