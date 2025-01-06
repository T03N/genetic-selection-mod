package com.geneticselection;

import com.geneticselection.attributes.GlobalAttributesManager;
import com.geneticselection.attributes.GlobalAttributesSavedData;
import com.geneticselection.individual.MobIndividualAttributes;
import com.geneticselection.mobs.cow.CowGeneticsInitializer;
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

	}
}
