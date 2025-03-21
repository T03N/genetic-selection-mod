package com.geneticselection.utils;

import com.geneticselection.mobs.Chickens.CustomChickenEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.EnumSet;

public class EatGrassGoal extends Goal {
    private final CustomChickenEntity chicken;
    private BlockPos targetGrassPos;

    public EatGrassGoal(CustomChickenEntity chicken) {
        this.chicken = chicken;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (chicken.getEnergyLevel() >= 40) {
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

        if (chicken.getEnergyLevel()>=99) {
            return false;
        }

        return true;
    }

    private BlockPos findNearestGrass() {
        BlockPos chickenPos = chicken.getBlockPos();
        int searchRadius = 50;
        BlockPos closestGrass = null;
        double closestDistance = Double.MAX_VALUE;


        for (int y = 2; y >= -2; y--) {  // Only checking reasonable height differences
            for (int x = -searchRadius; x <= searchRadius; x++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos checkPos = chickenPos.add(x, y, z);

                    if (chicken.getWorld().getBlockState(checkPos).isOf(Blocks.TALL_GRASS) || chicken.getWorld().getBlockState(checkPos).isOf(Blocks.SHORT_GRASS)) {
                        return checkPos;
                    }
                }
            }
        }

        return closestGrass;
    }

    private void moveToTarget(BlockPos targetPos) {
        // Calculate the center of the target block
        Vec3d moveTarget = new Vec3d(
                targetPos.getX() + 0.5,
                targetPos.getY(),
                targetPos.getZ() + 0.5
        );

        Vec3d chickenPos = chicken.getPos();
        Vec3d direction = moveTarget.subtract(chickenPos).normalize();

        Vec3d overshootTarget = moveTarget.add(direction.multiply(1.5));

        chicken.getNavigation().startMovingTo(overshootTarget.x, overshootTarget.y, overshootTarget.z, 2);
    }
}
