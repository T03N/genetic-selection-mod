package com.geneticselection.mobs.Camels;

import com.geneticselection.GeneticSelection;
import com.geneticselection.mobs.ModModelLayers;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.CamelEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class CustomCamelRenderer extends MobEntityRenderer<CustomCamelEntity, CamelEntityModel<CustomCamelEntity>> {
    private static final Identifier TEXTURE = Identifier.of(GeneticSelection.MOD_ID, "textures/entity/camel/minecraft-camel.png");

    public CustomCamelRenderer(EntityRendererFactory.Context context) {
        super(context, new CamelEntityModel<>(context.getPart(ModModelLayers.CUSTOM_CAMEL)), 0.6f);
    }

    @Override
    public Identifier getTexture(CustomCamelEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(CustomCamelEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        // Render custom description using DescriptionRenderer
        DescriptionRenderer.renderDescription(entity, matrices, vertexConsumers, light, this.dispatcher, this.getTextRenderer(), tickDelta);
    }
}