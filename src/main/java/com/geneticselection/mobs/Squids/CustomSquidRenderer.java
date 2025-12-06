package com.geneticselection.mobs.Squids;

import com.geneticselection.GeneticSelection;
import com.geneticselection.mobs.ModModelLayers;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.SquidEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class CustomSquidRenderer extends MobEntityRenderer<CustomSquidEntity, SquidEntityModel<CustomSquidEntity>> {
    private static final Identifier TEXTURE = Identifier.of(GeneticSelection.MOD_ID, "textures/entity/squid/squid.png");

    public CustomSquidRenderer(EntityRendererFactory.Context context) {
        super(context, new SquidEntityModel<>(context.getPart(ModModelLayers.CUSTOM_SQUID)), 0.7f);
    }

    @Override
    public Identifier getTexture(CustomSquidEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(CustomSquidEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        // Render custom description using DescriptionRenderer
        DescriptionRenderer.renderDescription(entity, matrices, vertexConsumers, light, this.dispatcher, this.getTextRenderer(), tickDelta);
    }
}