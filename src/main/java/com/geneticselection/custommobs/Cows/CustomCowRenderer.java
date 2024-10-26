package com.geneticselection.custommobs.Cows;


import com.geneticselection.GeneticSelection;
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.CowEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class CustomCowRenderer extends MobEntityRenderer<CustomCowEntity, CowEntityModel<CustomCowEntity>> {
    private static final Identifier TEXTURE = Identifier.of(GeneticSelection.MOD_ID, "textures/entity/minecraft-cow.png");

    public CustomCowRenderer(EntityRendererFactory.Context context) {
        super(context, new CowEntityModel<>(context.getPart(ModModleLayers.CUSTOM_COW)), 0.6f);
    }

    @Override
    public Identifier getTexture(CustomCowEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(CustomCowEntity livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        super.render(livingEntity, f, g, matrixStack, vertexConsumerProvider, i);
    }
}
