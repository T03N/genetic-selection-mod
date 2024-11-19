package com.geneticselection.mobs.Pigs;

import com.geneticselection.GeneticSelection;
import com.geneticselection.mobs.Pigs.CustomPigEntity;
import com.geneticselection.mobs.ModModelLayers;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.CowEntityModel;
import net.minecraft.client.render.entity.model.PigEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class CustomPigRenderer extends MobEntityRenderer<CustomPigEntity, PigEntityModel<CustomPigEntity>> {
    private static final Identifier TEXTURE = Identifier.of(GeneticSelection.MOD_ID, "textures/entity/minecraft-pig.png");

    public CustomPigRenderer(EntityRendererFactory.Context context) {
        super(context, new PigEntityModel<>(context.getPart(ModModelLayers.CUSTOM_PIG)), 0.6f);
    }

    @Override
    public Identifier getTexture(CustomPigEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(CustomPigEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        // Render custom description using DescriptionRenderer
        DescriptionRenderer.renderDescription(entity, matrices, vertexConsumers, light, this.dispatcher, this.getTextRenderer(), tickDelta);
    }
}