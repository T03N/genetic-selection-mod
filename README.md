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

### Step 2: Create your Custom Mob Superclass

In your `main/java/com/geneticselection/mobs/newmob` create a file named `CustomMobEntity.java` (replace Mob with your animal name). 
Example below: `main/java/com/geneticselection/mobs/Cows/CustomCowEntity.java`
```java
package com.geneticselection.mobs.Cows;

import com.geneticselection.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import java.util.Random;

public class CustomCowEntity extends CowEntity {
    private int MaxHp;
    private int MinMeat;
    private int MaxMeat;
    private int MinLeather;
    private int MaxLeather;

    public CustomCowEntity(EntityType<? extends CowEntity> entityType, World world) {
        super(entityType, world);
        
        Random random = new Random();
        this.MaxHp = 5 + random.nextInt(11);
        this.MinMeat = 1+ random.nextInt(2);
        this.MaxMeat = MinMeat + random.nextInt(3);
        this.MinLeather = random.nextInt(2);
        this.MaxLeather = MinLeather + random.nextInt(2);
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
    }
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        
        if (itemStack.isOf(Items.BUCKET) && !this.isBaby()) {
            player.playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);
            ItemStack itemStack2 = ItemUsage.exchangeStack(itemStack, player, Items.MILK_BUCKET.getDefaultStack());
            player.setStackInHand(hand, itemStack2);
            return ActionResult.success(this.getWorld().isClient);
        } else if (itemStack.isEmpty()) { // Check if the hand is empty
            // Only display the stats on the server side to avoid duplication
            if (!this.getWorld().isClient) {
                player.sendMessage(Text.literal("Custom Cow Stats:"));
                player.sendMessage(Text.literal("Max Health: " + MaxHp));
                player.sendMessage(Text.literal("Min Meat: " + MinMeat));
                player.sendMessage(Text.literal("Max Meat: " + MaxMeat));
                player.sendMessage(Text.literal("Min Leather: " + MinLeather));
                player.sendMessage(Text.literal("Max Leather: " + MaxLeather));
                player.sendMessage(Text.literal("----------------------------------------------"));
            }
            return ActionResult.success(this.getWorld().isClient);
        } else {
            return super.interactMob(player, hand);
        }
    }
    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        
        if (!this.getWorld().isClient) {
            // Calculate the amount of meat to drop between MinMeat and MaxMeat
            int meatAmount = MinMeat + this.getWorld().random.nextInt((MaxMeat - MinMeat) + 1);
            this.dropStack(new ItemStack(Items.BEEF, meatAmount));

            // Calculate the amount of leather to drop between MinLeather and MaxLeather
            int leatherAmount = MinLeather + this.getWorld().random.nextInt((MaxLeather - MinLeather) + 1);
            this.dropStack(new ItemStack(Items.LEATHER, leatherAmount));
        }
    }
    @Override
    public CustomCowEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
        if (!(mate instanceof CustomCowEntity)) {
            return (CustomCowEntity) EntityType.COW.create(serverWorld);
        }
        
        CustomCowEntity parent1 = this;
        CustomCowEntity parent2 = (CustomCowEntity) mate;

        int childMaxHp = (parent1.MaxHp + parent2.MaxHp) / 2;
        int childMinMeat = (parent1.MinMeat + parent2.MinMeat) / 2;
        int childMaxMeat = (parent1.MaxMeat + parent2.MaxMeat) / 2;
        int childMinLeather = (parent1.MinLeather + parent2.MinLeather) / 2;
        int childMaxLeather = (parent1.MaxLeather + parent2.MaxLeather) / 2;

        CustomCowEntity child = new CustomCowEntity(ModEntities.CUSTOM_COW, serverWorld);

        child.MaxHp = childMaxHp;
        child.MinMeat = childMinMeat;
        child.MaxMeat = childMaxMeat;
        child.MinLeather = childMinLeather;
        child.MaxLeather = childMaxLeather;
        child.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(child.MaxHp);

        return child;
    }
}
```
Your attributes, breeding logic, calculations, and drops will be done mostly in this file. Override default minecraft functions to how you see fit. For now, just create the basic super class and mess with the logic once you got everything set up.

### Step 3: Register your custom mob

Navagate to `main\java\com\geneticselection\mobs\ModEntities.java`, to register your custom mob. Do not create a new file, simply just add onto the commented section:
```java
package com.geneticselection.mobs;

import com.geneticselection.GeneticSelection;
import com.geneticselection.mobs.Cows.CustomCowEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;

public class ModEntities{
    public static final EntityType<CustomCowEntity> CUSTOM_COW = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(GeneticSelection.MOD_ID, "custom_cow"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, CustomCowEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9F, 1.4F))
                    .build()
    );
    //register new mobs here by using the above format
}
```
Copy the format of the provided example and change the variables as you need.

### Step 4: Add a mod model layer for your custom mob

Navagate to `main\java\com\geneticselection\mobs\ModModleLayers.java`, to add your custom mobs model layer. Do not create a new file, simply just add onto the commented section:
```java
package com.geneticselection.mobs;

import com.geneticselection.GeneticSelection;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class ModModleLayers {
    public static final EntityModelLayer CUSTOM_COW = new EntityModelLayer(Identifier.of(GeneticSelection.MOD_ID,"custom_cow"), "main");
    //add your new mobs here by using the above format
}
```
Copy the format of the provided example and change the variables as you need.


### Step 5: Create your renderer

Navagate to `main\java\com\geneticselection\mobs\newmob` and create a file named `NewmobRenderer.java`.
example below: `main\java\com\geneticselection\mobs\Cows\CustomCowRenderer.java`
```java
package com.geneticselection.mobs.Cows;

import com.geneticselection.GeneticSelection;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.CowEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class CustomCowRenderer extends MobEntityRenderer<CustomCowEntity, CowEntityModel<CustomCowEntity>> {
    private static final Identifier TEXTURE = Identifier.of(GeneticSelection.MOD_ID, "textures/entity/minecraft-cow.png");

    public CustomCowRenderer(EntityRendererFactory.Context context) {
        super(context, new CowEntityModel<>(context.getPart(ModModleLayers.CUSTOM_COW)), 0.6f);
    }
    @Override
    public Identifier getTexture(CustomCowEntity entity) {
        return TEXTURE;
    }
    @Override
    public void render(CustomCowEntity livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        super.render(livingEntity, f, g, matrixStack, vertexConsumerProvider, i);
    }
}
```
For your textures in "textures/entity/minecraft-cow.png", navagate to your `main\resources\assets\genetic-selection\textures\entity` and drop a png file of a texture you want for your mob. For example, the cow one `main\resources\assets\genetic-selection\textures\entity\minecraft-cow.png`. For the rest, copy the format of the provided example and change the variables as you need.

### Step 6: Edit the server side

1. Navagate to `main\java\com\geneticselection\GeneticSelection.java`
2. Create a method that pertains to your mob
3. Register the default attibutes to your mob
4. Lower the spawn rate of the vanilla mobs and allow your custom mobs to spawn
5. Set the spawning requirements for your mob if neede
6. Call your method in onInitialize
   
Copy the format of the provided example and change the variables as you need.
ex: `main\java\com\geneticselection\GeneticSelection.java`
```java
public void cowMethod(){
	//Register the default attibutes to your mob
	FabricDefaultAttributeRegistry.register(ModEntities.CUSTOM_COW, CustomCowEntity.createCowAttributes());
	//lowers the spawn rate of default vanilla cows
	BiomeModifications.addSpawn(
		BiomeSelectors.foundInOverworld(),
		SpawnGroup.CREATURE,
		EntityType.COW, // Remove the original cow
		0, // Spawn weight (higher = more frequent)
		0, // No minimum group size
		0  // No maximum group size
	);
	//adds custom cow to natural spawn
	BiomeModifications.addSpawn(
		BiomeSelectors.foundInOverworld(),
		SpawnGroup.CREATURE,
		ModEntities.CUSTOM_COW, // Add custom cow
		20, // Spawn weight (higher = more frequent)
		2,  // Minimum group size
		4   // Maximum group size
	);
	//restricts the cow spawn to grass blocks on the ground
	SpawnRestriction.register(
		ModEntities.CUSTOM_COW,
		SpawnLocationTypes.ON_GROUND,
		Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
		(entityType, world, spawnReason, pos, random) ->
			world.getBlockState(pos.down()).isOf(Blocks.GRASS_BLOCK)
	);
}
```

### Step 7: Edit the client side
1. Navagate to `main\java\com\geneticselection\GeneticSelectionClient.java`
2. Create a method that pertains to your mob
3. Register your mob
4. register the model layer for your mob
5. Call your method in onInitializeClient

Copy the format of the provided example and change the variables as you need.
ex: `main\java\com\geneticselection\GeneticSelectionClient.java`
```java
public void cowMethod(){
        //register your cow
        EntityRendererRegistry.register(ModEntities.CUSTOM_COW, CustomCowRenderer::new);
        //register your model layer for your cow
        EntityModelLayerRegistry.registerModelLayer(ModModleLayers.CUSTOM_COW, CowEntityModel::getTexturedModelData);
}
```

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
