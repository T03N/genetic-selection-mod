package com.geneticselection.mobs.Sheep;

import com.geneticselection.GeneticSelection;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.SheepEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.DyeColor;
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
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity,
                       float limbAngle, float limbDistance, float tickDelta,
                       float animationProgress, float headYaw, float headPitch) {
        if (entity.isSheared()) {
            return; // Don't render wool if sheep is sheared
        }

        // Get the sheep color
        DyeColor dyeColor = entity.getColor();
        // Get the correct vertex consumer with the wool texture
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(
                RenderLayer.getEntityCutoutNoCull(WOOL_TEXTURE));

        // Important: Save the current state of the matrices
        matrices.push();

        // Copy the model state but explicitly handle child scaling
        this.woolModel.child = entity.isBaby();

        // Make sure animation parameters are set correctly
        this.woolModel.animateModel(entity, limbAngle, limbDistance, tickDelta);
        this.woolModel.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);

        // Render the wool with color
        this.woolModel.render(matrices, vertexConsumer, light,
                OverlayTexture.DEFAULT_UV
        );

        // Restore the matrices
        matrices.pop();
    }
}