package com.geneticselection;

import com.geneticselection.custommobs.Cows.CustomCowRenderer;
import com.geneticselection.custommobs.Cows.ModModleLayers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.model.CowEntityModel;

public class GeneticSelectionClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.CUSTOM_COW, CustomCowRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(ModModleLayers.CUSTOM_COW, CowEntityModel::getTexturedModelData);
    }
}
