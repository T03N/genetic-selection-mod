package com.geneticselection;

import com.geneticselection.mobs.Camels.CustomCamelEntity;
import com.geneticselection.mobs.Cows.CustomCowEntity;
import com.geneticselection.mobs.Donkeys.CustomDonkeyEntity;
import com.geneticselection.mobs.ModEntities;
import com.geneticselection.mobs.Pigs.CustomPigEntity;
import com.geneticselection.mobs.Rabbit.CustomRabbitEntity;
import com.geneticselection.mobs.Sheep.CustomSheepEntity;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
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


	@Override
	public void onInitialize() {
		cowMethod();
		sheepMethod();
		pigMethod();
		rabbitMethod();
		donkeyMethod();
		camelMethod();
		LOGGER.info("Hello Fabric world!");
	}
}