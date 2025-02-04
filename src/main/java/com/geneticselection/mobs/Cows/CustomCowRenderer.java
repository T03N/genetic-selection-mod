package com.geneticselection.mobs.Cows;

import com.geneticselection.GeneticSelection;
import com.geneticselection.mobs.ModModelLayers;
import com.geneticselection.utils.DescriptionRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.CowEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CustomCowRenderer extends MobEntityRenderer<CustomCowEntity, CowEntityModel<CustomCowEntity>> {
    private static final Identifier LOW_ENERGY_TEXTURE = Identifier.of(GeneticSelection.MOD_ID, "textures.entity/minecraft-camel.png");
    private static final Identifier MEDIUM_ENERGY_TEXTURE = Identifier.of(GeneticSelection.MOD_ID, "textures.entity/minecraft-sheep.png");
    private static final Identifier HIGH_ENERGY_TEXTURE = Identifier.of(GeneticSelection.MOD_ID, "textures.entity/minecraft-cow.png");

    public CustomCowRenderer(EntityRendererFactory.Context context) {
        super(context, new CowEntityModel<>(context.getPart(ModModelLayers.CUSTOM_COW)), 0.6f);
    }

    @Override
    public Identifier getTexture(CustomCowEntity entity) {
        double energy = entity.getEnergyLevel();  // Access the energy level from the entity

        if (energy <= 33) {
            return LOW_ENERGY_TEXTURE;
        } else if (energy <= 66) {
            return MEDIUM_ENERGY_TEXTURE;
        } else {
            return HIGH_ENERGY_TEXTURE;
        }
    }

    @Override
    public void render(CustomCowEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        // Render custom description using DescriptionRenderer
        DescriptionRenderer.renderDescription(entity, matrices, vertexConsumers, light, this.dispatcher, this.getTextRenderer(), tickDelta);
    }
}