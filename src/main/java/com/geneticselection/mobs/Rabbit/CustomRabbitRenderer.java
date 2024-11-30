package com.geneticselection.mobs.Rabbit;

import com.geneticselection.GeneticSelection;
import com.geneticselection.mobs.ModModelLayers;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.CowEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.RabbitEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class CustomRabbitRenderer extends MobEntityRenderer<CustomRabbitEntity, RabbitEntityModel<CustomRabbitEntity>> {
    private static final Identifier TEXTURE = Identifier.of(GeneticSelection.MOD_ID, "textures/entity/brown-rabbit.png");

    public CustomRabbitRenderer(EntityRendererFactory.Context context) {
        super(context, new RabbitEntityModel<>(context.getPart(ModModelLayers.CUSTOM_RABBIT)), 0.6f);
    }

    @Override
    public Identifier getTexture(CustomRabbitEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(CustomRabbitEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        // Render custom description using DescriptionRenderer
        DescriptionRenderer.renderDescription(entity, matrices, vertexConsumers, light, this.dispatcher, this.getTextRenderer(), tickDelta);
    }
}