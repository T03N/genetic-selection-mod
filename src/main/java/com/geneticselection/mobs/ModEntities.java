package com.geneticselection.mobs;

import com.geneticselection.GeneticSelection;
import com.geneticselection.mobs.Cows.CustomCowEntity;
import com.geneticselection.mobs.Sheep.CustomSheepEntity;
import com.geneticselection.mobs.Pigs.CustomPigEntity;
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
    public static final EntityType<CustomSheepEntity> CUSTOM_SHEEP = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(GeneticSelection.MOD_ID, "custom_sheep"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, CustomSheepEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9F, 1.4F))
                    .build()
    );

    public static final EntityType<CustomPigEntity> CUSTOM_PIG = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(GeneticSelection.MOD_ID, "custom_pig"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, CustomPigEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9F, 1.4F))
                    .build()
    );
    //register new mobs here by using the above format
}