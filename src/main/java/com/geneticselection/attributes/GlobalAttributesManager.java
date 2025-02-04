package com.geneticselection.attributes;

import com.geneticselection.mobs.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GlobalAttributesManager {
    // Stores all the attributes for all mobs
    private static final Map<EntityType<?>, MobAttributes> globalAttributes = new HashMap<>();

    // Initialize default attributes
    public static void initialize() {
        globalAttributes.put(ModEntities.CUSTOM_COW, new MobAttributes(0.2, 10.0, 100.0,  Optional.of(3.0), Optional.of(2.0), null, null, Optional.empty()));
        globalAttributes.put(ModEntities.CUSTOM_SHEEP, new MobAttributes(0.2, 10.0, 100.0, Optional.of(3.0), Optional.empty(), Optional.of(2.0), Optional.empty(), Optional.empty()));
        globalAttributes.put(ModEntities.CUSTOM_RABBIT, new MobAttributes(0.2, 10.0, 100.0, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(2.0), Optional.empty()));
        globalAttributes.put(ModEntities.CUSTOM_PIG, new MobAttributes(0.2, 8.0, 100.0, Optional.empty(), null, null, null, Optional.empty()));
        globalAttributes.put(ModEntities.CUSTOM_DONKEY, new MobAttributes(0.2, 15.0, 100.0, null, Optional.empty(), null, null, Optional.empty()));
        globalAttributes.put(ModEntities.CUSTOM_CAMEL, new MobAttributes(0.2, 30.0, 100.0, null, null, null, null, Optional.empty()));
        globalAttributes.put(ModEntities.CUSTOM_CHICKEN, new MobAttributes(0.2, 3, 100.0, Optional.of(3.0), null, null, null, Optional.of(2.0)));

    }

    // Default values for all mobs (Safety load values)
    public static MobAttributes getAttributes(EntityType<?> type) {
        return globalAttributes.getOrDefault(type, new MobAttributes(0.2, 10.0, 100.0, Optional.of(3.0), Optional.of(2.0), Optional.of(1.0), Optional.of(2.0), Optional.of(2.0)));
    }

    public static void updateGlobalAttributes(EntityType<?> type, MobAttributes newAttributes) {
        globalAttributes.put(type, newAttributes);
    }

    // Implement save and load methods using NBT for persistence.
    // This is used to save and load the attributes from the world data, only keeping everything within the world and saving the data.
    public static void save(NbtCompound tag) {
        for (Map.Entry<EntityType<?>, MobAttributes> entry : globalAttributes.entrySet()) {
            NbtCompound mobTag = new NbtCompound();
            // Save basic attributes
            mobTag.putDouble("movementSpeed", entry.getValue().getMovementSpeed());
            mobTag.putDouble("maxHealth", entry.getValue().getMaxHealth());

            // Save optional attributes if present
            entry.getValue().getMaxMeat().ifPresent(maxMeat -> mobTag.putDouble("maxMeat", maxMeat));
            entry.getValue().getMaxLeather().ifPresent(maxLeather -> mobTag.putDouble("maxLeather", maxLeather));
            entry.getValue().getMaxWool().ifPresent(maxWool -> mobTag.putDouble("maxWool", maxWool));
            entry.getValue().getMaxRabbitHide().ifPresent(maxRabbitHide -> mobTag.putDouble("maxRabbitHide", maxRabbitHide));
            entry.getValue().getMaxFeathers().ifPresent(maxFeathers -> mobTag.putDouble("maxFeathers", maxFeathers));

            // Save entity type id
            Identifier id = Registries.ENTITY_TYPE.getId(entry.getKey());
            if (id != null)
                tag.put(id.toString(), mobTag);
        }
    }

    public static void load(NbtCompound tag) {
        globalAttributes.clear();
        for (String key : tag.getKeys()) {
            Identifier id = Identifier.tryParse(key);
            if (id == null)
                continue;
            EntityType<?> type = Registries.ENTITY_TYPE.get(id);
            if (type != null) {
                NbtCompound mobTag = tag.getCompound(key);

                // Load basic attributes
                double movementSpeed = mobTag.getDouble("movementSpeed");
                double maxHealth = mobTag.getDouble("maxHealth");
                double energyLvl = mobTag.getDouble("energyLvl");

                // Load optional attributes, or use Optional.empty() if they are not set
                Optional<Double> maxMeat = mobTag.contains("maxMeat") ? Optional.of(mobTag.getDouble("maxMeat")) : Optional.empty();
                Optional<Double> maxLeather = mobTag.contains("maxLeather") ? Optional.of(mobTag.getDouble("maxLeather")) : Optional.empty();
                Optional<Double> maxWool = mobTag.contains("maxWool") ? Optional.of(mobTag.getDouble("maxWool")) : Optional.empty();
                Optional<Double> maxRabbitHide = mobTag.contains("maxRabbitHide") ? Optional.of(mobTag.getDouble("maxRabbitHide")) : Optional.empty();
                Optional<Double> maxFeathers = mobTag.contains("maxFeathers") ? Optional.of(mobTag.getDouble("maxFeathers")) : Optional.empty();

                // Create MobAttributes with loaded data
                MobAttributes attributes = new MobAttributes(
                        movementSpeed,
                        maxHealth,
                        energyLvl,
                        maxMeat,
                        maxLeather,
                        maxWool,
                        maxRabbitHide,
                        maxFeathers
                );

                // Store loaded attributes in globalAttributes
                globalAttributes.put(type, attributes);
            }
        }
    }
}