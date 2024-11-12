package com.geneticselection;

import com.geneticselection.mobs.Cows.CustomCowEntity;
import com.geneticselection.mobs.ModEntities;
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

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		cowMethod();
		LOGGER.info("Hello Fabric world!");
	}
}