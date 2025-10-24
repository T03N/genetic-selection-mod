package com.geneticselection.mobs.Piglins;

import com.geneticselection.mobs.ModModelLayers;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.PiglinEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class CustomPiglinRenderer extends MobEntityRenderer<CustomPiglinEntity, PiglinEntityModel<CustomPiglinEntity>> {
    // Using vanilla piglin texture
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/piglin/piglin.png");

    public CustomPiglinRenderer(EntityRendererFactory.Context context) {
        super(context, new PiglinEntityModel<>(context.getPart(ModModelLayers.CUSTOM_PIGLIN)), 0.5f);
    }

    @Override
    public Identifier getTexture(CustomPiglinEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(CustomPiglinEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        // Render custom description using DescriptionRenderer
        DescriptionRenderer.renderDescription(entity, matrices, vertexConsumers, light, this.dispatcher, this.getTextRenderer(), tickDelta);
    }
}