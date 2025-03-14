package com.geneticselection.utils;

import com.geneticselection.mobs.Chickens.CustomChickenEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.EnumSet;

public class EatGrassGoal extends Goal {
    private final CustomChickenEntity chicken;
    private int eatTimer;
    private BlockPos targetGrassPos;

    public EatGrassGoal(CustomChickenEntity chicken) {
        this.chicken = chicken;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        // Only eat when energy is low (adjust condition to check for low energy)
        if (chicken.getEnergyLevel() >= 60.0) {
            return false; // Chicken won't eat if its energy is above the threshold
        }

        // Only search for a new target if no valid target exists yet
        if (targetGrassPos == null) {
            targetGrassPos = findNearestGrass();
        }

        // Return true if a valid target has been found
        return targetGrassPos != null;
    }

    @Override
    public void start() {
        if (targetGrassPos != null) {
            System.out.println("Moving to: " + targetGrassPos);
            chicken.getNavigation().startMovingTo(
                    targetGrassPos.getX() + 0.5,
                    targetGrassPos.getY() + 0.5,
                    targetGrassPos.getZ() + 0.5,
                    1.2
            );
        }
    }

    @Override
    public boolean shouldContinue() {
        return targetGrassPos != null &&
                chicken.squaredDistanceTo(targetGrassPos.getX() + 0.5, targetGrassPos.getY() + 0.5, targetGrassPos.getZ() + 0.5) > 1.0;
    }

    private BlockPos findNearestGrass() {
        BlockPos chickenPos = chicken.getBlockPos();
        int searchRadius = 10;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                BlockPos checkPos = chickenPos.add(x, -1, z);
                if (chicken.getWorld().getBlockState(checkPos).isOf(Blocks.GRASS_BLOCK)) {
                    return checkPos;
                }
            }
        }
        return null; // No grass found
    }
}
