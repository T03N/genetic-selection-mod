package com.geneticselection;

import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.individual.MobIndividualAttributes;
import com.geneticselection.mobs.cow.CowGeneticsInitializer;
import net.fabricmc.api.ModInitializer;

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

		// Register individual attribute handlers
		MobIndividualAttributes.register();

		LOGGER.info("Genetic Selection Mod Initialized");
	}
}