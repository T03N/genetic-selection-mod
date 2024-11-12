package com.geneticselection.genetics;
package com.geneticselection.mobs.pig;

@Override
public AnimalEntity breed(AnimalEntity parent1, AnimalEntity parent2, ServerWorld world) {
    if (world.isClient()) return null; // Server-side only

    // Ensure both parents are instances of CowEntity
    if (parent1 instanceof PigEntity mob1 && parent2 instanceof PigEntity mob2) {
        return CowBreedingLogic.breed(mob1, mob2, world);
    }
    return null;
}

// Custom logic to inherit mob attributes
private MobAttributes inheritAttributes(MobAttributes a, MobAttributes b) {
    return getMobAttributes(a, b);
}

@NotNull
public static MobAttributes getMobAttributes(MobAttributes a, MobAttributes b) {
    double speed = Math.random() < 0.5 ? a.getMovementSpeed() : b.getMovementSpeed();
    double health = Math.random() < 0.5 ? a.getMaxHealth() : b.getMaxHealth();
    if (Math.random() < 0.1) speed *= 1.05;  // Mutation
    if (Math.random() < 0.1) health *= 1.05; // Mutation
    return new MobAttributes(speed, health);
}
