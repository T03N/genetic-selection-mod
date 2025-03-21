package com.geneticselection.utils;

import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.AnimalEntity;
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
        if (getEnergyLevel(animal) >= 40) {
            return false;
        }

        if (targetGrassPos == null) {
            targetGrassPos = findNearestGrass();
        }

        return targetGrassPos != null;
    }

    @Override
    public void start() {
        if (targetGrassPos != null) {
            moveToTarget(targetGrassPos);
        }
    }

    @Override
    public boolean shouldContinue() {
        return getEnergyLevel(animal) < 99;
    }

    private BlockPos findNearestGrass() {
        BlockPos animalPos = animal.getBlockPos();
        int searchRadius = 50;
        BlockPos closestGrass = null;
        double closestDistance = Double.MAX_VALUE;

        for (int y = 2; y >= -2; y--) {
            for (int x = -searchRadius; x <= searchRadius; x++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos checkPos = animalPos.add(x, y, z);

                    if (animal.getWorld().getBlockState(checkPos).isOf(Blocks.TALL_GRASS) ||
                            animal.getWorld().getBlockState(checkPos).isOf(Blocks.SHORT_GRASS)) {
                        return checkPos;
                    }
                }
            }
        }
        return closestGrass;
    }

    private void moveToTarget(BlockPos targetPos) {
        Vec3d moveTarget = new Vec3d(
                targetPos.getX() + 0.5,
                targetPos.getY(),
                targetPos.getZ() + 0.5
        );

        Vec3d animalPos = animal.getPos();
        Vec3d direction = moveTarget.subtract(animalPos).normalize();

        Vec3d overshootTarget = moveTarget.add(direction.multiply(1.5));

        animal.getNavigation().startMovingTo(overshootTarget.x, overshootTarget.y, overshootTarget.z, 1.5);
    }

    private int getEnergyLevel(T entity) {

        return 0; // Default return (modify this to integrate your energy system)
    }
}
