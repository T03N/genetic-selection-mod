package com.geneticselection.attributes;

import net.minecraft.entity.LivingEntity;

public interface AttributeCarrier {
    // Handle ram-related energy costs through other methods
    // We'll use the isCharging method to check when the goat is about to ram
    boolean isCharging();

    /**
     * Apply all custom (non-basic) attributes stored in the given MobAttributes.
     * For example, a cow might look up MAX_MEAT and MAX_LEATHER and assign those
     * to its internal drop values.
     */
    void applyCustomAttributes(MobAttributes attributes);

    double getEnergyLevel();
}
