package com.geneticselection.mobs.Goat;

import com.geneticselection.GeneticSelection;
import com.geneticselection.mobs.ModModelLayers;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.GoatEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class CustomGoatRenderer extends MobEntityRenderer<CustomGoatEntity, GoatEntityModel<CustomGoatEntity>> {
    private static final Identifier TEXTURE = Identifier.of(GeneticSelection.MOD_ID, "textures/entity/goat/minecraft-goat.png");

    public CustomGoatRenderer(EntityRendererFactory.Context context) {
        super(context, new GoatEntityModel<>(context.getPart(ModModelLayers.CUSTOM_GOAT)), 0.6f);
    }

    @Override
    public Identifier getTexture(CustomGoatEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(CustomGoatEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        // Render custom description using DescriptionRenderer
        DescriptionRenderer.renderDescription(entity, matrices, vertexConsumers, light, this.dispatcher, this.getTextRenderer(), tickDelta);
    }
}