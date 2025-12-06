package com.geneticselection.mobs;

import com.geneticselection.GeneticSelection;
import com.geneticselection.mobs.Axolotl.CustomAxolotlEntity;
import com.geneticselection.mobs.Bee.CustomBeeEntity;
import com.geneticselection.mobs.Camels.CustomCamelEntity;
import com.geneticselection.mobs.Cows.CustomCowEntity;
import com.geneticselection.mobs.Donkeys.CustomDonkeyEntity;
import com.geneticselection.mobs.Fox.CustomFoxEntity;
import com.geneticselection.mobs.Goat.CustomGoatEntity;
import com.geneticselection.mobs.Hoglins.CustomHoglinEntity;
import com.geneticselection.mobs.Piglins.CustomPiglinEntity;
import com.geneticselection.mobs.Pillagers.CustomEvokerEntity;
import com.geneticselection.mobs.Pillagers.CustomVindicatorEntity;
import com.geneticselection.mobs.Zoglins.CustomZoglinEntity;
import com.geneticselection.mobs.Mooshroom.CustomMooshroomEntity;
import com.geneticselection.mobs.Ocelots.CustomOcelotEntity;
import com.geneticselection.mobs.Rabbit.CustomRabbitEntity;
import com.geneticselection.mobs.Sheep.CustomSheepEntity;
import com.geneticselection.mobs.Pigs.CustomPigEntity;
import com.geneticselection.mobs.Chickens.CustomChickenEntity;
import com.geneticselection.mobs.Wolves.CustomWolfEntity;
import com.geneticselection.mobs.Zombies.CustomZombieEntity;
import com.geneticselection.mobs.Pillagers.CustomPillagerEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;

public class ModEntities{
    // Register the custom cow entity
    public static final EntityType<CustomCowEntity> CUSTOM_COW = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(GeneticSelection.MOD_ID, "custom_cow"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, CustomCowEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9F, 1.4F))
                    .build()
    );

    // Register the custom sheep entity
    public static final EntityType<CustomSheepEntity> CUSTOM_SHEEP = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(GeneticSelection.MOD_ID, "custom_sheep"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, CustomSheepEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9F, 1.4F))
                    .build()
    );

    // Register the custom pig entity
    public static final EntityType<CustomPigEntity> CUSTOM_PIG = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(GeneticSelection.MOD_ID, "custom_pig"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, CustomPigEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9F, 1.4F))
                    .build()
    );

    // Register the custom rabbit entity
    public static final EntityType<CustomRabbitEntity> CUSTOM_RABBIT = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(GeneticSelection.MOD_ID, "custom_rabbit"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, CustomRabbitEntity::new)
                    .dimensions(EntityDimensions.fixed(0.5F, 0.4F))
                    .build()
    );

    // Register the custom donkey entity
    public static final EntityType<CustomDonkeyEntity> CUSTOM_DONKEY = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(GeneticSelection.MOD_ID, "custom_donkey"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, CustomDonkeyEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9F, 1.95F))
                    .build()
    );

    // Register the custom camel entity
    public static final EntityType<CustomCamelEntity> CUSTOM_CAMEL = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(GeneticSelection.MOD_ID, "custom_camel"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, CustomCamelEntity::new)
                    .dimensions(EntityDimensions.fixed(1.0F, 2.0F))
                    .build()
    );

    // Register the custom chicken entity
    public static final EntityType<CustomChickenEntity> CUSTOM_CHICKEN = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(GeneticSelection.MOD_ID, "custom_chicken"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, CustomChickenEntity::new)
                    .dimensions(EntityDimensions.fixed(1.0F, 2.0F))
                    .build()
    );

    // Register the custom wolf entity
    public static final EntityType<CustomWolfEntity> CUSTOM_WOLF = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(GeneticSelection.MOD_ID, "custom_wolf"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, CustomWolfEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9F, 1.4F))
                    .build()
    );

    // Register the custom ocelot entity
    public static final EntityType<CustomOcelotEntity> CUSTOM_OCELOT = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(GeneticSelection.MOD_ID, "custom_ocelot"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, CustomOcelotEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9F, 1.4F))
                    .build()
    );

    // Register the custom hoglin entity
    public static final EntityType<CustomHoglinEntity> CUSTOM_HOGLIN = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(GeneticSelection.MOD_ID, "custom_hoglin"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, CustomHoglinEntity::new)
                    .dimensions(EntityDimensions.fixed(1.4F, 1.4F))
                    .build()
    );

    // Register the custom zoglin entity
    public static final EntityType<CustomZoglinEntity> CUSTOM_ZOGLIN = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(GeneticSelection.MOD_ID, "custom_zoglin"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, CustomZoglinEntity::new)
                    .dimensions(EntityDimensions.fixed(1.4F, 1.4F))
                    .build()
    );

    // Register the custom bee entity
    public static final EntityType<CustomBeeEntity> CUSTOM_BEE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(GeneticSelection.MOD_ID, "custom_bee"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, CustomBeeEntity::new)
                    .dimensions(EntityDimensions.fixed(0.7F, 0.6F))
                    .build()
    );

    // Register the custom axolotl entity
    public static final EntityType<CustomAxolotlEntity> CUSTOM_AXOLOTL = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(GeneticSelection.MOD_ID, "custom_axolotl"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, CustomAxolotlEntity::new)
                    .dimensions(EntityDimensions.fixed(0.75F, 0.42F))
                    .build()
    );

    // Register the custom goat entity
    public static final EntityType<CustomGoatEntity> CUSTOM_GOAT = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(GeneticSelection.MOD_ID, "custom_goat"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, CustomGoatEntity::new)
                    .dimensions(EntityDimensions.fixed(1.3F, 0.9F))
                    .build()
    );

    // Register the custom fox entity
    public static final EntityType<CustomFoxEntity> CUSTOM_FOX = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(GeneticSelection.MOD_ID, "custom_fox"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, CustomFoxEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6F, 0.7F))
                    .build()
    );

    // Register the custom mooshroom entity
    public static final EntityType<CustomMooshroomEntity> CUSTOM_MOOSHROOM = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(GeneticSelection.MOD_ID, "custom_mooshroom"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, CustomMooshroomEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9F, 1.4F))
                    .build()
    );

    // Register the custom zombie entity
    public static final EntityType<CustomZombieEntity> CUSTOM_ZOMBIE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(GeneticSelection.MOD_ID, "custom_zombie"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, CustomZombieEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6F, 1.95F))
                    .build()
    );
    // Register the custom piglin entity
    public static final EntityType<CustomPiglinEntity> CUSTOM_PIGLIN = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(GeneticSelection.MOD_ID, "custom_piglin"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, CustomPiglinEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6F, 1.95F))
                    .build()
    );

    public static final EntityType<CustomPillagerEntity> CUSTOM_PILLAGER = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(GeneticSelection.MOD_ID, "custom_pillager"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, CustomPillagerEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6F, 1.95F))
                    .build()
    );

    public static final EntityType<CustomVindicatorEntity> CUSTOM_VINDICATOR = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(GeneticSelection.MOD_ID, "custom_vindicator"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, CustomVindicatorEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6F, 1.95F))
                    .build()
    );

    public static final EntityType<CustomEvokerEntity> CUSTOM_EVOKER = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(GeneticSelection.MOD_ID, "custom_evoker"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, CustomEvokerEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6F, 1.95F))
                    .build()
    );
}
