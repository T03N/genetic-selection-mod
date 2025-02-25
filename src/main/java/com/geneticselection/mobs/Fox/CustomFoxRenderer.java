package com.geneticselection.mobs.Fox;

import com.geneticselection.GeneticSelection;
import com.geneticselection.mobs.ModModelLayers;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.FoxEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class CustomFoxRenderer extends MobEntityRenderer<CustomFoxEntity, FoxEntityModel<CustomFoxEntity>> {
    private static final Identifier TEXTURE = Identifier.of(GeneticSelection.MOD_ID, "textures/entity/fox/fox.png");

    public CustomFoxRenderer(EntityRendererFactory.Context context) {
        super(context, new FoxEntityModel<>(context.getPart(ModModelLayers.CUSTOM_FOX)), 0.6f);
    }

    @Override
    public Identifier getTexture(CustomFoxEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(CustomFoxEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        // Render custom description using DescriptionRenderer
        DescriptionRenderer.renderDescription(entity, matrices, vertexConsumers, light, this.dispatcher, this.getTextRenderer(), tickDelta);
    }
}