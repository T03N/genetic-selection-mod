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
Create a file named `NewMobGenetics.java` in the `newmob` directory:
```java
package com.geneticselection.mobs.newmob;

public class NewMobGenetics implements Genetics {
    @Override
    public AnimalEntity breed(AnimalEntity parent1, AnimalEntity parent2, ServerWorld world) {
        if (isValidBreedingPair(parent1, parent2)) {
            return NewMobBreedingLogic.breed((NewMobEntity) parent1, (NewMobEntity) parent2, world);
        }
        return null;
    }

    private boolean isValidBreedingPair(AnimalEntity parent1, AnimalEntity parent2) {
        return parent1 instanceof NewMobEntity && parent2 instanceof NewMobEntity;
    }
}
```
#### 3. Implement Breeding Logic

Create a file named `NewMobBreedingLogic.java`:
```java
package com.geneticselection.mobs.newmob;

public class NewMobBreedingLogic {
    public static NewMobEntity breed(NewMobEntity parent1, NewMobEntity parent2, ServerWorld world) {
        NewMobEntity offspring = (NewMobEntity) EntityType.NEWMOB.create(world);
        if (offspring != null) {
            // Calculate position for the offspring and set its traits...
            applyInheritedAttributes(offspring, parent1.getAttributes(), parent2.getAttributes());
        }
        return offspring;
    }

    private static void applyInheritedAttributes(NewMobEntity offspring, MobAttributes attr1, MobAttributes attr2) {
        MobAttributes childAttributes = NewMobGenetics.getMobAttributes(attr1, attr2);
        offspring.setAttributes(childAttributes);
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
