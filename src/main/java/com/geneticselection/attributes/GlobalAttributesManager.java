package com.geneticselection.attributes;

import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class GlobalAttributesManager {
    private static final Map<EntityType<?>, MobAttributes> globalAttributes = new HashMap<>();

    public static void initialize() {
        // Initialize default attributes for cows
        globalAttributes.put(EntityType.COW, new MobAttributes(0.2, 10.0));
        globalAttributes.put(EntityType.SHEEP, new MobAttributes(0.2, 10.0));

        // Initialize other passive mobs similarly when added
    }

    public static MobAttributes getAttributes(EntityType<?> type) {
        return globalAttributes.getOrDefault(type, new MobAttributes(0.2, 10.0));
    }

    public static void updateGlobalAttributes(EntityType<?> type, MobAttributes newAttributes) {
        globalAttributes.put(type, newAttributes);
        // Optionally, mark data as dirty for saving
    }

    // Implement save and load methods using NBT for persistence
    public static void save(NbtCompound tag) {
        for (Map.Entry<EntityType<?>, MobAttributes> entry : globalAttributes.entrySet()) {
            NbtCompound mobTag = new NbtCompound();
            mobTag.putDouble("movementSpeed", entry.getValue().getMovementSpeed());
            mobTag.putDouble("maxHealth", entry.getValue().getMaxHealth());
            // Add other attributes
            Identifier id = Registries.ENTITY_TYPE.getId(entry.getKey());
            if (id != null) {
                tag.put(id.toString(), mobTag);
            }
        }
    }

    public static void load(NbtCompound tag) {
        globalAttributes.clear(); // Clear existing data before loading
        for (String key : tag.getKeys()) {
            Identifier id = Identifier.tryParse(key);
            if (id == null) {
                continue; // Skip invalid identifiers
            }
            EntityType<?> type = Registries.ENTITY_TYPE.get(id);
            if (type != null) {
                NbtCompound mobTag = tag.getCompound(key);
                MobAttributes attributes = new MobAttributes(
                        mobTag.getDouble("movementSpeed"),
                        mobTag.getDouble("maxHealth")
                );
                // Load other attributes
                globalAttributes.put(type, attributes);
            }
        }
    }

    // Add methods to hook into world save/load events for persistence
}