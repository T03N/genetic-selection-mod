package com.geneticselection.utils;

import com.geneticselection.GeneticSelection;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAttachmentType;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class DescriptionRenderer {
    private static final Map<UUID, Text> descriptions = new HashMap<>();

    public static void setDescription(Entity entity, Text text) {
        descriptions.put(entity.getUuid(), text);
    }

    public static Text getDescription(Entity entity) {
        return descriptions.getOrDefault(entity.getUuid(), Text.literal(""));
    }

    public static <T extends Entity> void renderDescription(
            T entity,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            EntityRenderDispatcher dispatcher,
            TextRenderer textRenderer,
            float tickDelta
    ) {
        // Ensure we're on the client side
        if (!entity.getWorld().isClient) return;

        matrices.push();
        double distanceSquared = dispatcher.getSquaredDistanceToCamera(entity);
        if (distanceSquared <= 4096.0) {
            Vec3d attachmentPoint = entity.getAttachments().getPointNullable(EntityAttachmentType.NAME_TAG, 0, entity.getYaw(tickDelta));
            if (attachmentPoint != null) {
                boolean isVisibleThroughBlocks = !entity.isSneaky();
                matrices.translate(attachmentPoint.x, attachmentPoint.y + 0.5, attachmentPoint.z);
                matrices.multiply(dispatcher.getRotation());
                matrices.scale(0.025F, -0.025F, 0.025F);
                Matrix4f matrix4f = matrices.peek().getPositionMatrix();

                float backgroundOpacity = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.125F);
                int backgroundAlpha = (int) (backgroundOpacity * 255.0F) << 24;

                // Draw entity's display name
                Text name = entity.getDisplayName();
                float xOffset = ((float) textRenderer.getWidth(name) / 4); // Add small gap
                float yOffset = -(entity.getHeight() / 2) * 0.025f;

                // Split description into lines
                Text descriptionText = getDescription(entity);
                String[] descriptionLines = descriptionText.getString().split("\n");

                // Render each line below the entity's name
                for (int i = 0; i < descriptionLines.length; i++) {
                    Text lineText = Text.literal(descriptionLines[i]);
                    textRenderer.draw(
                            lineText,
                            xOffset,
                            yOffset + (i * 10f),
                            553648127,
                            false,
                            matrix4f,
                            vertexConsumers,
                            isVisibleThroughBlocks ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL,
                            backgroundAlpha,
                            light
                    );

                    // Render white text overlay if visible through blocks
                    if (isVisibleThroughBlocks) {
                        textRenderer.draw(
                                lineText,
                                xOffset,
                                yOffset + (i * 10f),
                                Colors.WHITE,
                                false,
                                matrix4f,
                                vertexConsumers,
                                TextRenderer.TextLayerType.NORMAL,
                                0,
                                light
                        );
                    }
                }
            }
        }
        matrices.pop();
    }
}
