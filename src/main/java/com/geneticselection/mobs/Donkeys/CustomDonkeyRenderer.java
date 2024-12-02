package com.geneticselection.mobs.Donkeys;

import com.geneticselection.GeneticSelection;
import com.geneticselection.mobs.Donkeys.CustomDonkeyEntity;
import com.geneticselection.mobs.ModModelLayers;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.DonkeyEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class CustomDonkeyRenderer extends MobEntityRenderer<CustomDonkeyEntity, DonkeyEntityModel<CustomDonkeyEntity>> {
    private static final Identifier TEXTURE = Identifier.of(GeneticSelection.MOD_ID, "textures/entity/minecraft-donkey.png");

    public CustomDonkeyRenderer(EntityRendererFactory.Context context) {
        super(context, new DonkeyEntityModel<>(context.getPart(ModModelLayers.CUSTOM_DONKEY)), 0.6f);
    }

    @Override
    public Identifier getTexture(CustomDonkeyEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(CustomDonkeyEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        // Render custom description using DescriptionRenderer
        DescriptionRenderer.renderDescription(entity, matrices, vertexConsumers, light, this.dispatcher, this.getTextRenderer(), tickDelta);
    }
}