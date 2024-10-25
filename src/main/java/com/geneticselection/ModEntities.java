package com.geneticselection;

import com.geneticselection.custommobs.Cows.CustomCowEntity;
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
}