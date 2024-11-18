package com.geneticselection;

import com.geneticselection.mobs.Cows.CustomCowRenderer;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.mobs.ModModelLayers;
import com.geneticselection.mobs.Pigs.CustomPigRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.model.CowEntityModel;
import net.minecraft.client.render.entity.model.PigEntityModel;

public class GeneticSelectionClient implements ClientModInitializer {
    public void cowMethod(){
        //register your cow
        EntityRendererRegistry.register(ModEntities.CUSTOM_COW, CustomCowRenderer::new);
        //register your model layer for your cow
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.CUSTOM_COW, CowEntityModel::getTexturedModelData);
    }

    public void pigMethod(){
        EntityRendererRegistry.register(ModEntities.CUSTOM_PIG, CustomPigRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.CUSTOM_PIG, PigEntityModel::getTexturedModelData);
    }

    @Override
    public void onInitializeClient() {
        cowMethod();
        pigMethod();
    }
}
