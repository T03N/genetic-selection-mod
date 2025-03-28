package com.geneticselection.utils;

import com.geneticselection.attributes.AttributeCarrier;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.EnumSet;

public class EatGrassGoal<T extends AnimalEntity> extends Goal {
    private final T animal;
    private BlockPos targetGrassPos;

    public EatGrassGoal(T animal) {
        this.animal = animal;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        double energy = getEnergyLevel(animal);

        if (targetGrassPos == null) {
            targetGrassPos = findNearestGrass(); // Always attempt to find grass
        }

        boolean canStart = (energy < 50) && (targetGrassPos != null);

        return canStart;
    }

    @Override
    public void start() {
        if (targetGrassPos != null) {
            moveToTarget(targetGrassPos);
        }
    }

    @Override
    public boolean shouldContinue() {
        boolean hasEnergyNeed = getEnergyLevel(animal) < 90;

        boolean isNavigating = !animal.getNavigation().isIdle();

        if (!isNavigating) {
            targetGrassPos = null;
            return false;
        }

        return hasEnergyNeed;
    }

    private BlockPos findNearestGrass() {
        BlockPos animalPos = animal.getBlockPos();
        int searchRadius = 50;

        for (int r = 0; r <= searchRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) < r && Math.abs(dz) < r) continue;

                    for (int dy = 2; dy >= -2; dy--) {
                        BlockPos checkPos = animalPos.add(dx, dy, dz);

                        if (animal.getWorld().getBlockState(checkPos).isOf(Blocks.GRASS_BLOCK)) {
                            return checkPos;
                        }
                    }
                }
            }
        }

        return null;
    }

    private void moveToTarget(BlockPos targetPos) {
        Vec3d moveTarget = new Vec3d(
                targetPos.getX() + 0.5,
                targetPos.getY(),
                targetPos.getZ() + 0.5
        );

        Vec3d animalPos = animal.getPos();
        Vec3d direction = moveTarget.subtract(animalPos).normalize();

        Vec3d overshootTarget = moveTarget.add(direction.multiply(1));

        animal.getNavigation().startMovingTo(overshootTarget.x, overshootTarget.y, overshootTarget.z, 2);
    }

    private double getEnergyLevel(T entity) {
        if (entity instanceof AttributeCarrier) {
            return ((AttributeCarrier) entity).getEnergyLevel();
        }
        return 0;
    }

    private void sendDebugMessage(String message) {
        // Send message to the nearest player
        if (animal.getWorld().isClient()) return; // Prevent client-side execution

        animal.getWorld().getPlayers().stream()
                .filter(player -> player.squaredDistanceTo(animal) < 100) // Only nearby players
                .forEach(player -> player.sendMessage(Text.of("[" + animal.getName().getString() + "] " + message), false));
    }
}
