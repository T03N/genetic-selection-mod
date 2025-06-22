package com.geneticselection.utils;

import com.geneticselection.mobs.Cows.CustomCowEntity;
import com.geneticselection.mobs.Wolves.CustomWolfEntity;
import com.geneticselection.mobs.Pigs.CustomPigEntity;
import com.geneticselection.mobs.Sheep.CustomSheepEntity;
import com.geneticselection.mobs.Chickens.CustomChickenEntity;
import com.geneticselection.mobs.Rabbit.CustomRabbitEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAttachmentType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class DescriptionRenderer {

    private static Text buildDescriptionForEntity(Entity entity) {
        MutableText description = Text.literal("");

        if (entity instanceof CustomCowEntity cow) {
            description.append(Text.literal("HP: " + String.format("%.1f", cow.getHealth()) + "/" + String.format("%.1f", cow.getMaxHpTracked()) + "\n"));
            description.append(Text.literal("Spd: " + String.format("%.3f", cow.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)) + "\n"));
            description.append(Text.literal("Energy: " + String.format("%.1f", cow.getEnergyLevel()) + "/" + String.format("%.1f", cow.getMaxEnergy()) + "\n"));
            description.append(Text.literal("Age: " + (cow.getTickAge() / 20) + "s\n"));
            description.append(Text.literal("Meat: " + String.format("%.1f", cow.getMaxMeat()) + " | Leather: " + String.format("%.1f", cow.getMaxLeather())));
            return description;
        }
        if (entity instanceof CustomPigEntity pig) {
            description.append(Text.literal("HP: " + String.format("%.1f", pig.getHealth()) + "/" + String.format("%.1f", pig.getMaxHpTracked()) + "\n"));
            description.append(Text.literal("Spd: " + String.format("%.3f", pig.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)) + "\n"));
            description.append(Text.literal("Energy: " + String.format("%.1f", pig.getEnergyLevel()) + "/" + String.format("%.1f", pig.getMaxEnergy()) + "\n"));
            description.append(Text.literal("Age: " + (pig.getTickAge() / 20) + "s\n"));
            description.append(Text.literal("Meat: " + String.format("%.1f", pig.getMaxMeat())));
            return description;
        }
        if (entity instanceof CustomSheepEntity sheep) {
            description.append(Text.literal("HP: " + String.format("%.1f", sheep.getHealth()) + "/" + String.format("%.1f", sheep.getMaxHpTracked()) + "\n"));
            description.append(Text.literal("Spd: " + String.format("%.3f", sheep.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)) + "\n"));
            description.append(Text.literal("Energy: " + String.format("%.1f", sheep.getEnergyLevel()) + "/" + String.format("%.1f", sheep.getMaxEnergy()) + "\n"));
            description.append(Text.literal("Age: " + (sheep.getTickAge() / 20) + "s\n"));
            description.append(Text.literal("Meat: " + String.format("%.1f", sheep.getMaxMeat()) + " | Wool: " + String.format("%.1f", sheep.getMaxWool())));
            return description;
        }
        if (entity instanceof CustomChickenEntity chicken) {
            description.append(Text.literal("HP: " + String.format("%.1f", chicken.getHealth()) + "/" + String.format("%.1f", chicken.getMaxHpTracked()) + "\n"));
            description.append(Text.literal("Spd: " + String.format("%.3f", chicken.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)) + "\n"));
            description.append(Text.literal("Energy: " + String.format("%.1f", chicken.getEnergyLevel()) + "/" + String.format("%.1f", chicken.getMaxEnergy()) + "\n"));
            description.append(Text.literal("Age: " + (chicken.getTickAge() / 20) + "s\n"));
            description.append(Text.literal("Meat: " + String.format("%.1f", chicken.getMaxMeat()) + " | Feathers: " + String.format("%.1f", chicken.getMaxFeathers())));
            return description;
        }
        if (entity instanceof CustomRabbitEntity rabbit) {
            description.append(Text.literal("HP: " + String.format("%.1f", rabbit.getHealth()) + "/" + String.format("%.1f", rabbit.getMaxHpTracked()) + "\n"));
            description.append(Text.literal("Spd: " + String.format("%.3f", rabbit.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)) + "\n"));
            description.append(Text.literal("Energy: " + String.format("%.1f", rabbit.getEnergyLevel()) + "/" + String.format("%.1f", rabbit.getMaxEnergy()) + "\n"));
            description.append(Text.literal("Age: " + (rabbit.getTickAge() / 20) + "s\n"));
            description.append(Text.literal("Meat: " + String.format("%.1f", rabbit.getMaxMeat()) + " | Hide: " + String.format("%.1f", rabbit.getRabbitHide())));
            return description;
        }
        if (entity instanceof CustomWolfEntity wolf) {
            description.append(Text.literal("HP: " + String.format("%.1f", wolf.getHealth()) + "/" + String.format("%.1f", wolf.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH)) + " | Atk: " + String.format("%.2f", wolf.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)) + "\n"));
            description.append(Text.literal("Spd: " + String.format("%.3f", wolf.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)) + "\n"));
            description.append(Text.literal("Energy: " + String.format("%.1f", wolf.getEnergyLevel()) + "/" + String.format("%.1f", wolf.getMaxEnergy()) + "\n"));
            description.append(Text.literal("Age: " + (wolf.getTickAge() / 20) + "s | Kills: " + wolf.getKillCount() + "\n"));
            description.append(Text.literal("Breed CD: " + String.format("%.1f", wolf.getBreedingCooldown() / 20.0) + "s"));
            return description;
        }
        return null;
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
        if (!MinecraftClient.getInstance().player.isSneaking()) {
            return;
        }

        Text descriptionText = buildDescriptionForEntity(entity);
        if (descriptionText == null) {
            return;
        }

        double distanceSquared = dispatcher.getSquaredDistanceToCamera(entity);
        if (distanceSquared > 144.0) { // Reduced render distance for performance
            return;
        }

        matrices.push();
        Vec3d attachmentPoint = entity.getAttachments().getPointNullable(EntityAttachmentType.NAME_TAG, 0, entity.getYaw(tickDelta));
        if (attachmentPoint != null) {
            matrices.translate(attachmentPoint.x, attachmentPoint.y + 0.75, attachmentPoint.z);
        } else {
             matrices.translate(0.0, entity.getHeight() + 0.75F, 0.0);
        }

        matrices.multiply(dispatcher.getRotation());
        matrices.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        float backgroundOpacity = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.5F);
        int backgroundColor = (int) (backgroundOpacity * 255.0F) << 24;

        float yOffset = 0;
        String[] lines = descriptionText.getString().split("\n");

        for (String line : lines) {
            Text lineText = Text.literal(line);
            float xOffset = -textRenderer.getWidth(lineText) / 2.0f;
            textRenderer.draw(lineText, xOffset, yOffset, 0xFFFFFF, false, matrix4f, vertexConsumers, TextRenderer.TextLayerType.SEE_THROUGH, backgroundColor, light);
            yOffset += textRenderer.fontHeight;
        }

        matrices.pop();
    }
}
