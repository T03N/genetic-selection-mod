package com.geneticselection.mobs.Hoglins;

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

public class CustomHoglinRenderer extends MobEntityRenderer<CustomHoglinEntity, HoglinEntityModel<CustomHoglinEntity>> {
    private static final Identifier TEXTURE = Identifier.of(GeneticSelection.MOD_ID, "textures/entity/hoglin/hoglin.png");

    public CustomHoglinRenderer(EntityRendererFactory.Context context) {
        super(context, new HoglinEntityModel<>(context.getPart(ModModelLayers.CUSTOM_HOGLIN)), 0.6f);
    }

    @Override
    public Identifier getTexture(CustomHoglinEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(CustomHoglinEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        // Render custom description using DescriptionRenderer
        DescriptionRenderer.renderDescription(entity, matrices, vertexConsumers, light, this.dispatcher, this.getTextRenderer(), tickDelta);
    }
}