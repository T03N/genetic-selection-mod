package com.geneticselection.mobs.Mooshroom;

import com.geneticselection.GeneticSelection;
import com.geneticselection.mobs.ModModelLayers;
import com.geneticselection.mobs.Mooshroom.CustomMooshroomEntity;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.MooshroomMushroomFeatureRenderer;
import net.minecraft.client.render.entity.model.CowEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CustomMooshroomRenderer extends MobEntityRenderer<CustomMooshroomEntity, CowEntityModel<CustomMooshroomEntity>> {
    private static final Identifier HIGH_ENERGY_TEXTURE = Identifier.of(GeneticSelection.MOD_ID, "textures/entity/mooshroom/mooshroom.png");

    public CustomMooshroomRenderer(EntityRendererFactory.Context context) {
        super(context, new CowEntityModel<>(context.getPart(ModModelLayers.CUSTOM_MOOSHROOM)), 0.6f);
    }

    @Override
    public Identifier getTexture(CustomMooshroomEntity entity) {
        return HIGH_ENERGY_TEXTURE;
    }

    @Override
    public void render(CustomMooshroomEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        // Render custom description using DescriptionRenderer
        DescriptionRenderer.renderDescription(entity, matrices, vertexConsumers, light, this.dispatcher, this.getTextRenderer(), tickDelta);
    }
}