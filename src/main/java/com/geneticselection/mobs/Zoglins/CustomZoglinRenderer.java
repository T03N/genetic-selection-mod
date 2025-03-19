package com.geneticselection.mobs.Zoglins;

import com.geneticselection.GeneticSelection;
import com.geneticselection.mobs.ModModelLayers;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.DonkeyEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.HoglinEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class CustomZoglinRenderer extends MobEntityRenderer<CustomZoglinEntity, HoglinEntityModel<CustomZoglinEntity>> {
    private static final Identifier TEXTURE = Identifier.of(GeneticSelection.MOD_ID, "textures/entity/zoglin/zoglin.png");

    public CustomZoglinRenderer(EntityRendererFactory.Context context) {
        super(context, new HoglinEntityModel<>(context.getPart(ModModelLayers.CUSTOM_ZOGLIN)), 0.6f);
    }

    @Override
    public Identifier getTexture(CustomZoglinEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(CustomZoglinEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        // Render custom description using DescriptionRenderer
        DescriptionRenderer.renderDescription(entity, matrices, vertexConsumers, light, this.dispatcher, this.getTextRenderer(), tickDelta);
    }
}