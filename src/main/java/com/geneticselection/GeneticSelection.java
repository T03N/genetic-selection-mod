package com.geneticselection;

import com.geneticselection.custommobs.Cows.CustomCowEntity;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneticSelection implements ModInitializer {
	public static final String MOD_ID = "genetic-selection";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution
		FabricDefaultAttributeRegistry.register(ModEntities.CUSTOM_COW, CustomCowEntity.createCowAttributes());

//		BiomeModifications.addSpawn(
//				BiomeSelectors.foundInOverworld(),
//				SpawnGroup.CREATURE,
//				EntityType.COW, // Remove the original cow
//				-1, // Negative value means removal
//				0, // No minimum group size
//				0  // No maximum group size
//		);
		BiomeModifications.addSpawn(
				BiomeSelectors.foundInOverworld(),
				SpawnGroup.CREATURE,
				ModEntities.CUSTOM_COW, // Add custom cow
				100, // Spawn weight (higher = more frequent)
				2,  // Minimum group size
				4   // Maximum group size
		);

		LOGGER.info("Hello Fabric world!");
	}
}