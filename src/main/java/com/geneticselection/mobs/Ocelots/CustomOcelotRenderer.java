package com.geneticselection.mobs.Ocelots;

import com.geneticselection.GeneticSelection;
import com.geneticselection.mobs.ModModelLayers;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.OcelotEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class CustomOcelotRenderer extends MobEntityRenderer<CustomOcelotEntity, OcelotEntityModel<CustomOcelotEntity>> {
    private static final Identifier TEXTURE = Identifier.of(GeneticSelection.MOD_ID, "textures/entity/ocelot/ocelot.png");

    public CustomOcelotRenderer(EntityRendererFactory.Context context) {
        super(context, new OcelotEntityModel<>(context.getPart(ModModelLayers.CUSTOM_OCELOT)), 0.6f);
    }

    @Override
    public Identifier getTexture(CustomOcelotEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(CustomOcelotEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        // Render custom description using DescriptionRenderer
        DescriptionRenderer.renderDescription(entity, matrices, vertexConsumers, light, this.dispatcher, this.getTextRenderer(), tickDelta);
    }
}