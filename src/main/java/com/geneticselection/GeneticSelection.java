package com.geneticselection;

import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.GlobalAttributesSavedData;
import com.geneticselection.mobs.Axolotl.CustomAxolotlEntity;
import com.geneticselection.mobs.Bee.CustomBeeEntity;
import com.geneticselection.mobs.Camels.CustomCamelEntity;
import com.geneticselection.mobs.Chickens.CustomChickenEntity;
import com.geneticselection.mobs.Cows.CustomCowEntity;
import com.geneticselection.mobs.Donkeys.CustomDonkeyEntity;
import com.geneticselection.mobs.Fox.CustomFoxEntity;
import com.geneticselection.mobs.Goat.CustomGoatEntity;
import com.geneticselection.mobs.Hoglins.CustomHoglinEntity;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.mobs.ModModelLayers;
import com.geneticselection.mobs.Mooshroom.CustomMooshroomEntity;
import com.geneticselection.mobs.Ocelots.CustomOcelotEntity;
import com.geneticselection.mobs.Pigs.CustomPigEntity;
import com.geneticselection.mobs.Rabbit.CustomRabbitEntity;
import com.geneticselection.mobs.Sheep.CustomSheepEntity;
import com.geneticselection.mobs.Wolves.CustomWolfEntity;
import com.geneticselection.mobs.Wolves.CustomWolfRenderer;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.block.Blocks;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SheepEntityModel;
import net.minecraft.entity.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneticSelection implements ModInitializer {
	public static final String MOD_ID = "genetic-selection";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

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

	public void sheepMethod(){
		//Register the default attibutes to your mob
		FabricDefaultAttributeRegistry.register(ModEntities.CUSTOM_SHEEP, CustomSheepEntity.createSheepAttributes());
		//lowers the spawn rate of default vanilla cows
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				EntityType.SHEEP, // Remove the original cow
				0, // Spawn weight (higher = more frequent)
				0, // No minimum group size
				0  // No maximum group size
		);
		//adds custom cow to natural spawn
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				ModEntities.CUSTOM_SHEEP, // Add custom cow
				20, // Spawn weight (higher = more frequent)
				2,  // Minimum group size
				4   // Maximum group size
		);
		//restricts the cow spawn to grass blocks on the ground
		SpawnRestriction.register(
				ModEntities.CUSTOM_SHEEP,
				SpawnLocationTypes.ON_GROUND,
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, spawnReason, pos, random) ->
						world.getBlockState(pos.down()).isOf(Blocks.GRASS_BLOCK)
		);
	}

	public void pigMethod(){
		//Register the default attibutes to your mob
		FabricDefaultAttributeRegistry.register(ModEntities.CUSTOM_PIG, CustomPigEntity.createPigAttributes());
		//lowers the spawn rate of default vanilla pigs
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				EntityType.PIG, // Remove the original pig
				0, // Spawn weight (higher = more frequent)
				0, // No minimum group size
				0  // No maximum group size
		);
		//adds custom pig to natural spawn
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				ModEntities.CUSTOM_PIG, // Add custom pig
				20, // Spawn weight (higher = more frequent)
				5,  // Minimum group size
				10   // Maximum group size
		);
		//restricts the cow spawn to grass blocks on the ground
		SpawnRestriction.register(
				ModEntities.CUSTOM_PIG,
				SpawnLocationTypes.ON_GROUND,
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, spawnReason, pos, random) ->
						world.getBlockState(pos.down()).isOf(Blocks.GRASS_BLOCK)
		);
	}

	public void rabbitMethod() {
		//Register the default attibutes to your mob
		FabricDefaultAttributeRegistry.register(ModEntities.CUSTOM_RABBIT, CustomRabbitEntity.createRabbitAttributes());
		//lowers the spawn rate of default vanilla pigs
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				EntityType.RABBIT, // Remove the original pig
				0, // Spawn weight (higher = more frequent)
				0, // No minimum group size
				0  // No maximum group size
		);
		//adds custom pig to natural spawn
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				ModEntities.CUSTOM_RABBIT, // Add custom pig
				20, // Spawn weight (higher = more frequent)
				5,  // Minimum group size
				10   // Maximum group size
		);
		//restricts the cow spawn to grass blocks on the ground
		SpawnRestriction.register(
				ModEntities.CUSTOM_RABBIT,
				SpawnLocationTypes.ON_GROUND,
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, spawnReason, pos, random) ->
						world.getBlockState(pos.down()).isOf(Blocks.GRASS_BLOCK)
		);
	}

	public void donkeyMethod(){
		//Register the default attibutes to your mob
		FabricDefaultAttributeRegistry.register(ModEntities.CUSTOM_DONKEY, CustomDonkeyEntity.createAbstractDonkeyAttributes());
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				EntityType.DONKEY,
				0, // Spawn weight (higher = more frequent)
				0, // No minimum group size
				0  // No maximum group size
		);
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				ModEntities.CUSTOM_DONKEY,
				20, // Spawn weight (higher = more frequent)
				2,  // Minimum group size
				4   // Maximum group size
		);
		SpawnRestriction.register(
				ModEntities.CUSTOM_DONKEY,
				SpawnLocationTypes.ON_GROUND,
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, spawnReason, pos, random) ->
						world.getBlockState(pos.down()).isOf(Blocks.GRASS_BLOCK)
		);
	}

	public void camelMethod(){
		//Register the default attibutes to your mob
		FabricDefaultAttributeRegistry.register(ModEntities.CUSTOM_CAMEL, CustomCamelEntity.createCamelAttributes());
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				EntityType.CAMEL,
				0, // Spawn weight (higher = more frequent)
				0, // No minimum group size
				0  // No maximum group size
		);
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				ModEntities.CUSTOM_CAMEL,
				20, // Spawn weight (higher = more frequent)
				2,  // Minimum group size
				4   // Maximum group size
		);
		SpawnRestriction.register(
				ModEntities.CUSTOM_CAMEL,
				SpawnLocationTypes.ON_GROUND,
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, spawnReason, pos, random) ->
						world.getBlockState(pos.down()).isOf(Blocks.SAND) // Only spawn on sand blocks (sand or red sand)
		);
	}

	public void chickenMethod() {
		//Register the default attibutes to your mob
		FabricDefaultAttributeRegistry.register(ModEntities.CUSTOM_CHICKEN, CustomChickenEntity.createChickenAttributes());
		//lowers the spawn rate of default vanilla pigs
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				EntityType.CHICKEN, // Remove the original pig
				0, // Spawn weight (higher = more frequent)
				0, // No minimum group size
				0  // No maximum group size
		);
		//adds custom pig to natural spawn
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				ModEntities.CUSTOM_CHICKEN, // Add custom pig
				20, // Spawn weight (higher = more frequent)
				5,  // Minimum group size
				10   // Maximum group size
		);
		//restricts the cow spawn to grass blocks on the ground
		SpawnRestriction.register(
				ModEntities.CUSTOM_CHICKEN,
				SpawnLocationTypes.ON_GROUND,
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, spawnReason, pos, random) ->
						world.getBlockState(pos.down()).isOf(Blocks.GRASS_BLOCK)
		);
	}

	public void hoglinMethod(){
		//Register the default attibutes to your mob
		FabricDefaultAttributeRegistry.register(ModEntities.CUSTOM_HOGLIN, CustomHoglinEntity.createHoglinAttributes());
		//lowers the spawn rate of default vanilla pigs
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInTheNether(),
				SpawnGroup.CREATURE,
				EntityType.HOGLIN, // Remove the original pig
				0, // Spawn weight (higher = more frequent)
				0, // No minimum group size
				0  // No maximum group size
		);
		//adds custom pig to natural spawn
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInTheNether(),
				SpawnGroup.CREATURE,
				ModEntities.CUSTOM_HOGLIN, // Add custom pig
				20, // Spawn weight (higher = more frequent)
				5,  // Minimum group size
				10   // Maximum group size
		);
		//restricts the cow spawn to grass blocks on the ground
		SpawnRestriction.register(
				ModEntities.CUSTOM_HOGLIN,
				SpawnLocationTypes.ON_GROUND,
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, spawnReason, pos, random) ->
						world.getBlockState(pos.down()).isOf(Blocks.CRIMSON_NYLIUM)
		);
	}

	public void wolfMethod(){
		//Register the default attibutes to your mob
		FabricDefaultAttributeRegistry.register(ModEntities.CUSTOM_WOLF, CustomWolfEntity.createWolfAttributes());
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				EntityType.WOLF,
				0, // Spawn weight (higher = more frequent)
				0, // No minimum group size
				0  // No maximum group size
		);
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				ModEntities.CUSTOM_WOLF,
				20, // Spawn weight (higher = more frequent)
				2,  // Minimum group size
				4   // Maximum group size
		);
		SpawnRestriction.register(
				ModEntities.CUSTOM_WOLF,
				SpawnLocationTypes.ON_GROUND,
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, spawnReason, pos, random) ->
						world.getBlockState(pos.down()).isOf(Blocks.GRASS_BLOCK)
		);

	}

	public void beeMethod(){
		//Register the default attibutes to your mob
		FabricDefaultAttributeRegistry.register(ModEntities.CUSTOM_BEE, CustomBeeEntity.createBeeAttributes());
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				EntityType.BEE,
				0, // Spawn weight (higher = more frequent)
				0, // No minimum group size
				0  // No maximum group size
		);
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				ModEntities.CUSTOM_BEE,
				200, // Spawn weight (higher = more frequent)
				2,  // Minimum group size
				4   // Maximum group size
		);
		SpawnRestriction.register(
				ModEntities.CUSTOM_BEE,
				SpawnLocationTypes.UNRESTRICTED,
				Heightmap.Type.MOTION_BLOCKING,
				(entityType, world, spawnReason, pos, random) ->
						world.getBlockState(pos.down()).isOf(Blocks.BEE_NEST)
		);

	}

	public void axolotlMethod() {
		//Register the default attibutes to your mob
		FabricDefaultAttributeRegistry.register(ModEntities.CUSTOM_AXOLOTL, CustomAxolotlEntity.createAxolotlAttributes());
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				EntityType.AXOLOTL,
				0, // Spawn weight (higher = more frequent)
				0, // No minimum group size
				0  // No maximum group size
		);
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				ModEntities.CUSTOM_AXOLOTL,
				200, // Spawn weight (higher = more frequent)
				2,  // Minimum group size
				4   // Maximum group size
		);
		SpawnRestriction.register(
				ModEntities.CUSTOM_AXOLOTL,
				SpawnLocationTypes.IN_WATER,
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, spawnReason, pos, random) ->
						world.getBlockState(pos.down()).isOf(Blocks.CLAY)
		);
	}

	public void ocelotMethod(){
		//Register the default attibutes to your mob
		FabricDefaultAttributeRegistry.register(ModEntities.CUSTOM_OCELOT, CustomOcelotEntity.createOcelotAttributes());
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				EntityType.OCELOT,
				0, // Spawn weight (higher = more frequent)
				0, // No minimum group size
				0  // No maximum group size
		);
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				ModEntities.CUSTOM_OCELOT,
				20, // Spawn weight (higher = more frequent)
				2,  // Minimum group size
				4   // Maximum group size
		);
		SpawnRestriction.register(
				ModEntities.CUSTOM_OCELOT,
				SpawnLocationTypes.ON_GROUND,
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, spawnReason, pos, random) ->
						world.getBlockState(pos.down()).isOf(Blocks.GRASS_BLOCK)
		);
	}

	public void goatMethod(){
		//Register the default attibutes to your mob
		FabricDefaultAttributeRegistry.register(ModEntities.CUSTOM_GOAT, CustomGoatEntity.createGoatAttributes());
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				EntityType.GOAT,
				0, // Spawn weight (higher = more frequent)
				0, // No minimum group size
				0  // No maximum group size
		);
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				ModEntities.CUSTOM_GOAT,
				20, // Spawn weight (higher = more frequent)
				2,  // Minimum group size
				4   // Maximum group size
		);
		SpawnRestriction.register(
				ModEntities.CUSTOM_GOAT,
				SpawnLocationTypes.ON_GROUND,
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, spawnReason, pos, random) ->
						world.getBlockState(pos.down()).isOf(Blocks.SNOW_BLOCK)
		);
	}

	public void foxMethod(){
		//Register the default attibutes to your mob
		FabricDefaultAttributeRegistry.register(ModEntities.CUSTOM_FOX, CustomFoxEntity.createFoxAttributes());
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				EntityType.FOX,
				0, // Spawn weight (higher = more frequent)
				0, // No minimum group size
				0  // No maximum group size
		);
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				ModEntities.CUSTOM_FOX,
				20, // Spawn weight (higher = more frequent)
				2,  // Minimum group size
				4   // Maximum group size
		);
		SpawnRestriction.register(
				ModEntities.CUSTOM_FOX,
				SpawnLocationTypes.ON_GROUND,
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, spawnReason, pos, random) ->
						world.getBlockState(pos.down()).isOf(Blocks.GRASS_BLOCK)
		);
	}

	public void mooshroomMethod(){
		//Register the default attibutes to your mob
		FabricDefaultAttributeRegistry.register(ModEntities.CUSTOM_MOOSHROOM, CustomMooshroomEntity.createCowAttributes());
		//lowers the spawn rate of default vanilla cows
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				EntityType.MOOSHROOM, // Remove the original cow
				0, // Spawn weight (higher = more frequent)
				0, // No minimum group size
				0  // No maximum group size
		);
		//adds custom cow to natural spawn
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				ModEntities.CUSTOM_MOOSHROOM, // Add custom cow
				20, // Spawn weight (higher = more frequent)
				2,  // Minimum group size
				4   // Maximum group size
		);
		//restricts the cow spawn to grass blocks on the ground
		SpawnRestriction.register(
				ModEntities.CUSTOM_MOOSHROOM,
				SpawnLocationTypes.ON_GROUND,
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, spawnReason, pos, random) ->
						world.getBlockState(pos.down()).isOf(Blocks.MYCELIUM)
		);
	}

	@Override
	public void onInitialize() {
		cowMethod();
		sheepMethod();
		pigMethod();
		rabbitMethod();
		donkeyMethod();
		camelMethod();
		chickenMethod();
		wolfMethod();
		hoglinMethod();
		beeMethod();
		axolotlMethod();
		ocelotMethod();
		goatMethod();
		foxMethod();
		mooshroomMethod();
		GlobalAttributesManager.initialize();
		ServerWorldEvents.LOAD.register((server, world) -> {
			GlobalAttributesSavedData.fromWorld(world); // Load your global attributes when the world loads
		});
		LOGGER.info("Hello Fabric world!");
	}
}