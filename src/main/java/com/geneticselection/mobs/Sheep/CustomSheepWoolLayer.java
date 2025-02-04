package com.geneticselection.mobs.Sheep;

import com.geneticselection.GeneticSelection;
import com.geneticselection.mobs.ModModelLayers;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.SheepEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.client.model.ModelPart;

public class CustomSheepWoolLayer<T extends CustomSheepEntity> extends FeatureRenderer<T, SheepEntityModel<T>> {
    private static final Identifier WOOL_TEXTURE = Identifier.of(GeneticSelection.MOD_ID, "textures/entity/sheep/sheep_wool.png");
    private final SheepEntityModel<T> woolModel;

    public CustomSheepWoolLayer(FeatureRendererContext<T, SheepEntityModel<T>> context, ModelPart woolPart) {
        super(context);
        this.woolModel = new SheepEntityModel<>(woolPart);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (!entity.isSheared()) {
            renderWool(matrices, vertexConsumers, light, entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        }
    }

    private void renderWool(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(WOOL_TEXTURE));

        matrices.push();

        // Set the angles based on entity movement
        woolModel.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);

        // Render the wool model
        woolModel.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);

        matrices.pop();
    }
}