package com.geneticselection.mobs.Wolves;

import com.geneticselection.GeneticSelection;
import com.geneticselection.mobs.ModModelLayers;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.WolfEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class CustomWolfRenderer extends MobEntityRenderer<CustomWolfEntity, WolfEntityModel<CustomWolfEntity>> {
    private static final Identifier TEXTURE = Identifier.of(GeneticSelection.MOD_ID, "textures/entity/wolf/wolf.png");

    public CustomWolfRenderer(EntityRendererFactory.Context context) {
        super(context, new WolfEntityModel<>(context.getPart(ModModelLayers.CUSTOM_WOLF)), 0.6f);
    }

    @Override
    public Identifier getTexture(CustomWolfEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(CustomWolfEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        // Render custom description using DescriptionRenderer
        DescriptionRenderer.renderDescription(entity, matrices, vertexConsumers, light, this.dispatcher, this.getTextRenderer(), tickDelta);
    }
}