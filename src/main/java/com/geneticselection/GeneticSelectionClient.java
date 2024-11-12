package com.geneticselection;

import com.geneticselection.mobs.Cows.CustomCowRenderer;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.mobs.ModModleLayers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.model.CowEntityModel;

public class GeneticSelectionClient implements ClientModInitializer {
    public void cowMethod(){
        //register your cow
        EntityRendererRegistry.register(ModEntities.CUSTOM_COW, CustomCowRenderer::new);
        //register your model layer for your cow
        EntityModelLayerRegistry.registerModelLayer(ModModleLayers.CUSTOM_COW, CowEntityModel::getTexturedModelData);
    }

    @Override
    public void onInitializeClient() {
        cowMethod();
    }
}
