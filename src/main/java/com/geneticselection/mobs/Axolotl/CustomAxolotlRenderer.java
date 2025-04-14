package com.geneticselection.mobs.Axolotl;

import com.geneticselection.GeneticSelection;
import com.geneticselection.mobs.ModModelLayers;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.AxolotlEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class CustomAxolotlRenderer extends MobEntityRenderer<CustomAxolotlEntity, AxolotlEntityModel<CustomAxolotlEntity>> {
    private static final Identifier TEXTURE = Identifier.of(GeneticSelection.MOD_ID, "textures/entity/axolotl/axolotl.png");

    public CustomAxolotlRenderer(EntityRendererFactory.Context context) {
        super(context, new AxolotlEntityModel<>(context.getPart(ModModelLayers.CUSTOM_AXOLOTL)), 0.6f);
    }

    @Override
    public Identifier getTexture(CustomAxolotlEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(CustomAxolotlEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        // Render custom description using DescriptionRenderer
        DescriptionRenderer.renderDescription(entity, matrices, vertexConsumers, light, this.dispatcher, this.getTextRenderer(), tickDelta);
    }
}