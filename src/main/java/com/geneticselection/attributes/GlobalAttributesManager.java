package com.geneticselection.attributes;

import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GlobalAttributesManager {
    private static final Map<EntityType<?>, MobAttributes> globalAttributes = new HashMap<>();

    public static void initialize() {
        // Initialize default attributes for cows
        globalAttributes.put(EntityType.COW, new MobAttributes(0.2, 10.0, Optional.of(3.0), Optional.of(2.0), null, null));
        globalAttributes.put(EntityType.RABBIT, new MobAttributes(2.2, 3.0, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(2.0)));
        globalAttributes.put(EntityType.PIG, new MobAttributes(0.2, 8.0, Optional.empty(), null, null, null));

    }

    public static MobAttributes getAttributes(EntityType<?> type) {
        return globalAttributes.getOrDefault(type, new MobAttributes(0.2, 10.0, Optional.of(3.0), Optional.of(2.0), Optional.of(1.0), Optional.of(2.0)));
    }

    public static void updateGlobalAttributes(EntityType<?> type, MobAttributes newAttributes) {
        globalAttributes.put(type, newAttributes);
        // Optionally, mark data as dirty for saving
    }

    // Implement save and load methods using NBT for persistence
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

            // Save entity type id
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

                // Load basic attributes
                double movementSpeed = mobTag.getDouble("movementSpeed");
                double maxHealth = mobTag.getDouble("maxHealth");

                // Load optional attributes, or use Optional.empty() if they are not set
                Optional<Double> maxMeat = mobTag.contains("maxMeat") ? Optional.of(mobTag.getDouble("maxMeat")) : Optional.empty();
                Optional<Double> maxLeather = mobTag.contains("maxLeather") ? Optional.of(mobTag.getDouble("maxLeather")) : Optional.empty();
                Optional<Double> maxWool = mobTag.contains("maxWool") ? Optional.of(mobTag.getDouble("maxWool")) : Optional.empty();
                Optional<Double> maxRabbitHide = mobTag.contains("maxRabbitHide") ? Optional.of(mobTag.getDouble("maxRabbitHide")) : Optional.empty();

                // Create MobAttributes with loaded data
                MobAttributes attributes = new MobAttributes(
                        movementSpeed,
                        maxHealth,
                        maxMeat,
                        maxLeather,
                        maxWool,
                        maxRabbitHide
                );

                // Store loaded attributes in globalAttributes
                globalAttributes.put(type, attributes);
            }
        }
    }
}