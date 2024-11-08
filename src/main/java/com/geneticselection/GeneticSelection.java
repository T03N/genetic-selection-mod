package com.geneticselection;

import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.GlobalAttributesSavedData;
import com.geneticselection.individual.MobIndividualAttributes;
import com.geneticselection.mobs.cow.CowGeneticsInitializer;
import com.geneticselection.mobs.sheep.SheepGeneticsInitializer;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneticSelection implements ModInitializer {
	public static final String MOD_ID = "genetic-selection";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Genetic Selection Mod");

		// Initialize global attributes
		GlobalAttributesManager.initialize();

		// Register genetics for mobs
		CowGeneticsInitializer.initialize();
		// TODO: Initialize other mob genetics similarly

		SheepGeneticsInitializer.initialize();

		// Register individual attribute handlers
		MobIndividualAttributes.register();

		// Register world load and save events for PersistentState
		ServerWorldEvents.LOAD.register(this::onWorldLoad);
		ServerWorldEvents.UNLOAD.register(this::onWorldUnload);

		LOGGER.info("Genetic Selection Mod Initialized");
	}

	private void onWorldLoad(MinecraftServer minecraftServer, ServerWorld world) {
		if (!world.isClient()) {
			LOGGER.info("Loading Global Attributes for world: {}", world.getRegistryKey().getValue());
			GlobalAttributesSavedData data = GlobalAttributesSavedData.fromWorld(world);
			if (data != null) {
				data.markDirty(); // Ensure data is flagged for saving
			}
		}
	}

	private void onWorldUnload(MinecraftServer minecraftServer, ServerWorld world) {
		if (!world.isClient()) {
			LOGGER.info("Saving Global Attributes for world: {}", world.getRegistryKey().getValue());
			GlobalAttributesSavedData.save(world);
		}
	}
}