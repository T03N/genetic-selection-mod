# Genetic Selection Mod
This Minecraft mod enhances the game by providing a more lifelike simulation of animal interaction and breeding. It introduces additional attributes and characteristics to existing Minecraft animals, enabling them to exhibit more realistic behaviors and interactions. Currently, in Minecraft 1.21.1, Horses can be bred to produce more desirable foals, with higher jump strength and speed. This mod expands on this feature, allowing players to breed other animals, in a similar way, to create offspring with specific traits and characteristics. 

## Getting Started
This mod uses Minecraft version 1.21.1\
If you want to contribute to the mod, use method 1.

### **Method 1: Running through Intellij IDEA**
1) Download a [JDK](https://adoptium.net/temurin/releases/?package=jdk&arch=any) of version 21-LT that matches your system.

2) Use [IntelliJ IDEA](https://www.jetbrains.com/idea/), the community edition is free to use.

3) Clone the repository
4) Open the repository in IntelliJ
5) Ensure the settings are correct
6) Run through the Minecraft Client

##### *Video tutorial to run a Minecraft fabric mod in Intellij IDEA:*
[Fabric Modding Tutorial - Minecraft 1.21: Getting Started | #1](https://youtu.be/oU8-qV-ZtUY?list=PLKGarocXCE1H_HxOYihQMq0mlpqiUJj4L)

### **Method 2: Running through Fabric**
If you already have Minecraft and/or you just want to play with this mod, you can install fabric and run mods through it.

##### *Installation Guide for Windows and MacOS:*
[Installing Fabric Mods](https://minecrafthopper.net/help/guides/installing-mods/fabric/#:~:text=Installing%20Mods,-In%20this%20tutorial&text=First%2C%20launch%20the%20Fabric%20loader,minecraft%20folder)


## How to Implement Your Own Mobs

Currently, the mod includes an implementation for cows. To add a new mob, which we'll refer to as "NewMob", follow these steps:

### Step 1: Create a New Mob Directory

1. **Navigate to** the `mobs` directory.
2. **Create a new directory** for "NewMob":
	***main/java/com/geneticselection/mobs/newmob***

### Step 2: Implement NewMob Genetics Logic

#### 1. Create Genetics Initializer

Create a file named `NewMobGeneticsInitializer.java` in `com/geneticselection/mobs/newmob`:
```java
package com.geneticselection.mobs.newmob;

public class NewMobGeneticsInitializer {
 public static void initialize() {
     GeneticsRegistry.register(EntityType.NEWMOB, new NewMobGenetics());
 }
}

```
#### 2. Create Genetics Logic
Create a file named `NewMobGenetics.java` in the `genetics` directory:
```java
package com.geneticselection.mobs.newmob;

    @Override
    public AnimalEntity breed(AnimalEntity parent1, AnimalEntity parent2, ServerWorld world) {
        if (world.isClient()) return null; // Server-side only

        // Ensure both parents are instances of CowEntity
        if (parent1 instanceof NewMobEntity mob1 && parent2 instanceof NewMobEntity mob2) {
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
```
#### 3. Implement Breeding Logic

Create a file named `NewMobBreedingLogic.java`:
```java
package com.geneticselection.mobs.newmob;

public class NewMobBreedingLogic {
    public static NewMobEntity breed(NewMobEntity parent1, NewMobEntity parent2, ServerWorld world) {
        MobAttributes attr1 = ((IGeneticEntity) parent1).getMobAttributes();
        MobAttributes attr2 = ((IGeneticEntity) parent2).getMobAttributes();

        // Calculate heritability (H^2) as a constant, which in real life depends on how much of the variation in a trait is due to genetic differences
        double heritability = 0.5; // This is a chosen value; in real populations it's often less than 1

        // Create the offspring entity
        NewMobEntity offspring = (NewMobEntity) EntityType.NewMob.create(world);

        if (offspring != null) {
            // Calculate position near parents
            double x = (parent1.getX() + parent2.getX()) / 2;
            double y = Math.min(parent1.getY(), parent2.getY()) + 1; // Slight offset to avoid spawn issues
            double z = (parent1.getZ() + parent2.getZ()) / 2;
            offspring.refreshPositionAndAngles(x, y, z, parent1.getYaw(), parent1.getPitch());

            // Optional: Set the offspring as a baby
            offspring.setBaby(true);

            // Apply inherited attributes using the breeder's equation
            MobAttributes childAttributes = inheritAttributes(attr1, attr2, heritability);
            ((IGeneticEntity) offspring).setMobAttributes(childAttributes);
            applyAttributes(offspring, childAttributes);

            LOGGER.info("Breeding NewMobs: Parent1 ID={}, Parent2 ID={}", parent1.getId(), parent2.getId());

            // Spawn the offspring into the world
            world.spawnEntity(offspring);
        }

        // Influence global attributes based on the offspring
        influenceGlobalAttributes(offspring);
        return offspring;
    }

    private static MobAttributes inheritAttributes(MobAttributes a, MobAttributes b, double heritability) {
        // Calculate selection differential as the average difference between the two parent traits
        double selectionDifferentialSpeed = (a.getMovementSpeed() - b.getMovementSpeed()) / 2;
        double selectionDifferentialHealth = (a.getMaxHealth() - b.getMaxHealth()) / 2;

        // Calculate new trait values using the breeder's equation (R = h^2 * S)
        double newSpeed = (a.getMovementSpeed() + b.getMovementSpeed()) / 2 + heritability * selectionDifferentialSpeed;
        double newHealth = (a.getMaxHealth() + b.getMaxHealth()) / 2 + heritability * selectionDifferentialHealth;

        // Add a small mutation factor to simulate genetic drift
        Random random = new Random();
        double mutationFactor = 0.99; // was 0.1
        if (random.nextDouble() < mutationFactor) {
            newSpeed += random.nextGaussian() * 0.09; // was 0.01
            newHealth += random.nextGaussian() * 0.9; // was 0.1
        }

        // Ensure trait values are within reasonable bounds
        newSpeed = Math.max(0.1, Math.min(newSpeed, 1.0));
        newHealth = Math.max(1, Math.min(newHealth, 20));

        return new MobAttributes(newSpeed, newHealth);
    }

    private static void applyAttributes(NewMobEntity NewMob, MobAttributes attributes) {
        var speedAttribute = NewMob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.setBaseValue(attributes.getMovementSpeed());
        }

        var healthAttribute = NewMob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH);
        if (healthAttribute != null) {
            healthAttribute.setBaseValue(attributes.getMaxHealth());
        }

        NewMob.setHealth((float) attributes.getMaxHealth());
    }

    private static void influenceGlobalAttributes(NewMobEntity offspring) {
        if (offspring != null) {
            MobAttributes childAttributes = ((IGeneticEntity) offspring).getMobAttributes();
            if (childAttributes != null) {
                MobAttributes globalAttributes = GlobalAttributesManager.getAttributes(EntityType.NewMob);

                // Update global attributes using a moving average
                double updateRate = 0.01;
                double newMovementSpeed = globalAttributes.getMovementSpeed() * (1 - updateRate) + childAttributes.getMovementSpeed() * updateRate;
                double newMaxHealth = globalAttributes.getMaxHealth() * (1 - updateRate) + childAttributes.getMaxHealth() * updateRate;

                globalAttributes.setMovementSpeed(newMovementSpeed);
                globalAttributes.setMaxHealth(newMaxHealth);

                GlobalAttributesManager.updateGlobalAttributes(EntityType.NewMob, globalAttributes);
                LOGGER.info("Updated global NewMob attributes: Speed={}, Health={}",
                        globalAttributes.getMovementSpeed(), globalAttributes.getMaxHealth());
            }
        }
    }
}
```
### Step 3: Modify Main Mod to Initialize NewMob

In `GeneticSelection.java`, modify the `onInitialize` method:
```java
@Override
public void onInitialize() {
    LOGGER.info("Initializing Genetic Selection Mod");
    
    // Initialize global attributes
    GlobalAttributesManager.initialize();

    // Register genetics for cows
    CowGeneticsInitializer.initialize();
    
    // Register genetics for NewMob
    NewMobGeneticsInitializer.initialize();

    // Register individual attribute handlers
    MobIndividualAttributes.register();
}
```
### Step 4: Update Global Attributes Management

In **`GlobalAttributesManager.java`**, ensure to initialize attributes for NewMob:
```java
public static void initialize() {
    globalAttributes.put(EntityType.COW, new MobAttributes(0.2, 10.0));
    
    // Initialize attributes for NewMob
    globalAttributes.put(EntityType.NEWMOB, new MobAttributes(0.3, 8.0)); // Example attributes
}
```
### Step 5: Test the Implementation

1. **Compile and run the mod** in Minecraft.
2. **Test breeding NewMob** to verify that the attributes are inherited correctly.
3. **Check that global attributes** update as expected.
   

## Credits
<a href="https://github.com/T03N/genetic-selection-mod/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=T03N/genetic-selection-mod" />
</a> 

###### *Made with [contrib.rocks](https://contrib.rocks).*

TonyJRPI https://github.com/TonyJRPI \
winsonlin21 https://github.com/winsonlin21 \
corxgi https://github.com/corxgi \
AnthonyLi88 https://github.com/AnthonyLi88 
###### thanks for the extra help!
@samsthenerd (discord)
