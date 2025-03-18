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
                    3
            );
        }
    }

    @Override
    public boolean shouldContinue() {
        if (targetGrassPos == null) return false;

        // Check if the chicken is on the grass block
        BlockPos chickenBlockPos = chicken.getBlockPos();
        if (chickenBlockPos.equals(targetGrassPos)) {
            System.out.println("[DEBUG] Standing on grass. Eating...");
            return false;
        }

        return true;
    }

    private BlockPos findNearestGrass() {
        BlockPos chickenPos = chicken.getBlockPos();
        int searchRadius = 10;
        BlockPos closestGrass = null;
        double closestDistance = Double.MAX_VALUE;

        System.out.println("[DEBUG] Searching for nearest grass...");

        for (int y = 2; y >= -10; y--) {
            for (int x = -searchRadius; x <= searchRadius; x++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos checkPos = chickenPos.add(x, y, z);

                    if (chicken.getWorld().getBlockState(checkPos).isOf(Blocks.GRASS_BLOCK)) {

                        double distance = chickenPos.getSquaredDistance(checkPos);

                        if (distance < closestDistance) {
                            closestGrass = checkPos;
                            closestDistance = distance;
                        }
                    }
                }
            }
        }

        if (closestGrass != null) {
            System.out.println("[DEBUG] Found grass at: " + closestGrass);
        } else {
            System.out.println("[DEBUG] No grass found within search radius.");
        }

        return closestGrass;
    }


}
