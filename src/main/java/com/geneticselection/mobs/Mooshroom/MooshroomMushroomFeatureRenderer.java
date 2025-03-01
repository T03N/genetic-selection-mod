//package com.geneticselection.mobs.Mooshroom;
//
//import com.geneticselection.mobs.Mooshroom.CustomMooshroomEntity;
//import net.minecraft.client.model.ModelPart;
//import net.minecraft.client.render.VertexConsumerProvider;
//import net.minecraft.client.render.entity.feature.FeatureRenderer;
//import net.minecraft.client.render.entity.feature.FeatureRendererContext;
//import net.minecraft.client.render.entity.model.CowEntityModel;
//import net.minecraft.client.util.math.MatrixStack;
//import net.minecraft.util.Identifier;
//
//public class MooshroomMushroomFeatureRenderer<T extends CustomMooshroomEntity>
//        extends FeatureRenderer<T, CowEntityModel<T>> {
//
//    private static final Identifier MUSHROOM_TEXTURE = new Identifier("textures/entity/mooshroom/mushroom.png");
//    private final ModelPart mushroomModel;
//
//    public MooshroomMushroomFeatureRenderer(FeatureRendererContext<T, CowEntityModel<T>> context, ModelPart root) {
//        super(context);
//        this.mushroomModel = root.getChild("mushrooms"); // Ensure your model has this part
//    }
//
//    @Override
//    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
//        if (!entity.isBaby()) {
//            matrices.push();
//            // Position the mushrooms correctly on the cow
//            mushroomModel.render(matrices, vertexConsumers.getBuffer(mushroomModel.getLayer(MUSHROOM_TEXTURE)), light, entity.getOverlay(), 1.0f, 1.0f, 1.0f, 1.0f);
//            matrices.pop();
//        }
//    }
//}