package com.geneticselection.mobs.Sheep;

import com.geneticselection.GeneticSelection;
import com.geneticselection.mobs.ModModelLayers;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.SheepEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class CustomSheepRenderer extends MobEntityRenderer<CustomSheepEntity, SheepEntityModel<CustomSheepEntity>> {
    private static final Identifier TEXTURE = Identifier.of(GeneticSelection.MOD_ID, "textures/entity/sheep/minecraft-sheep.png");

    public CustomSheepRenderer(EntityRendererFactory.Context context) {
        super(context, new SheepEntityModel<>(context.getPart(ModModelLayers.CUSTOM_SHEEP)), 0.6f);
        this.addFeature(new CustomSheepWoolLayer<>(this, context.getPart(ModModelLayers.CUSTOM_SHEEP_FUR)));
    }

    @Override
    public Identifier getTexture(CustomSheepEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(CustomSheepEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        // Render custom description
        DescriptionRenderer.renderDescription(entity, matrices, vertexConsumers, light, this.dispatcher, this.getTextRenderer(), tickDelta);
    }
}