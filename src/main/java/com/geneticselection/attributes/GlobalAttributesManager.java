package com.geneticselection.attributes;

import com.geneticselection.mobs.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class GlobalAttributesManager {
    private static final Map<EntityType<?>, MobAttributes> globalAttributes = new HashMap<>();
    private static final Map<EntityType<?>, Map<AttributeKey, Double>> ENTITY_ATTRIBUTE_DEFAULTS = new HashMap<>();

    public static void registerDefaults() {
        ENTITY_ATTRIBUTE_DEFAULTS.put(ModEntities.CUSTOM_COW, new HashMap<>() {{
            put(AttributeKey.MOVEMENT_SPEED, 0.2);
            put(AttributeKey.MAX_HEALTH, 10.0);
            put(AttributeKey.ENERGY, 100.0);
            put(AttributeKey.MAX_MEAT, 3.0);
            put(AttributeKey.MAX_LEATHER, 2.0);
        }});

        ENTITY_ATTRIBUTE_DEFAULTS.put(ModEntities.CUSTOM_SHEEP, new HashMap<>() {{
            put(AttributeKey.MOVEMENT_SPEED, 0.2);
            put(AttributeKey.MAX_HEALTH, 10.0);
            put(AttributeKey.ENERGY, 100.0);
            put(AttributeKey.MAX_MEAT, 3.0);
            put(AttributeKey.MAX_WOOL, 2.0);
        }});

        ENTITY_ATTRIBUTE_DEFAULTS.put(ModEntities.CUSTOM_RABBIT, new HashMap<>() {{
            put(AttributeKey.MOVEMENT_SPEED, 2.2);
            put(AttributeKey.MAX_HEALTH, 10.0);
            put(AttributeKey.ENERGY, 100.0);
            put(AttributeKey.MAX_RABBIT_HIDE, 2.0);
        }});

        ENTITY_ATTRIBUTE_DEFAULTS.put(ModEntities.CUSTOM_PIG, new HashMap<>() {{
            put(AttributeKey.MOVEMENT_SPEED, 0.2);
            put(AttributeKey.MAX_HEALTH, 8.0);
            put(AttributeKey.ENERGY, 100.0);
            put(AttributeKey.MAX_MEAT, 3.0);
        }});

        ENTITY_ATTRIBUTE_DEFAULTS.put(ModEntities.CUSTOM_DONKEY, new HashMap<>() {{
            put(AttributeKey.MOVEMENT_SPEED, 0.2);
            put(AttributeKey.MAX_HEALTH, 15.0);
            put(AttributeKey.ENERGY, 100.0);
        }});

        ENTITY_ATTRIBUTE_DEFAULTS.put(ModEntities.CUSTOM_CAMEL, new HashMap<>() {{
            put(AttributeKey.MOVEMENT_SPEED, 0.125);
            put(AttributeKey.MAX_HEALTH, 30.0);
            put(AttributeKey.ENERGY, 100.0);
        }});

        ENTITY_ATTRIBUTE_DEFAULTS.put(ModEntities.CUSTOM_CHICKEN, new HashMap<>() {{
            put(AttributeKey.MOVEMENT_SPEED, 0.175);
            put(AttributeKey.MAX_HEALTH, 3.0);
            put(AttributeKey.ENERGY, 100.0);
            put(AttributeKey.MAX_MEAT, 3.0);
            put(AttributeKey.MAX_FEATHERS, 2.0);
        }});

        ENTITY_ATTRIBUTE_DEFAULTS.put(ModEntities.CUSTOM_WOLF, new HashMap<>() {{
            put(AttributeKey.MOVEMENT_SPEED, 0.3);
            put(AttributeKey.MAX_HEALTH, 8.0);
            put(AttributeKey.ENERGY, 95.0);
        }});

        ENTITY_ATTRIBUTE_DEFAULTS.put(ModEntities.CUSTOM_OCELOT, new HashMap<>() {{
            put(AttributeKey.MOVEMENT_SPEED, 0.175);
            put(AttributeKey.MAX_HEALTH, 3.0);
            put(AttributeKey.ENERGY, 100.0);
            put(AttributeKey.MAX_MEAT, 3.0);
            put(AttributeKey.MAX_FEATHERS, 2.0);
        }});

        ENTITY_ATTRIBUTE_DEFAULTS.put(ModEntities.CUSTOM_HOGLIN, new HashMap<>() {{
            put(AttributeKey.MOVEMENT_SPEED, 0.3);
            put(AttributeKey.MAX_HEALTH, 40.0);
            put(AttributeKey.ENERGY, 100.0);
        }});
        ENTITY_ATTRIBUTE_DEFAULTS.put(ModEntities.CUSTOM_ZOGLIN, new HashMap<>() {{
            put(AttributeKey.MOVEMENT_SPEED, 0.3);
            put(AttributeKey.MAX_HEALTH, 40.0);
            put(AttributeKey.ENERGY, 100.0);
        }});

        ENTITY_ATTRIBUTE_DEFAULTS.put(ModEntities.CUSTOM_BEE, new HashMap<>() {{
            put(AttributeKey.MOVEMENT_SPEED, 0.3);
            put(AttributeKey.MAX_HEALTH, 10.0);
            put(AttributeKey.ENERGY, 100.0);
        }});

        ENTITY_ATTRIBUTE_DEFAULTS.put(ModEntities.CUSTOM_AXOLOTL, new HashMap<>() {{
            put(AttributeKey.MOVEMENT_SPEED, 0.3);
            put(AttributeKey.MAX_HEALTH, 10.0);
            put(AttributeKey.ENERGY, 100.0);
        }});

        ENTITY_ATTRIBUTE_DEFAULTS.put(ModEntities.CUSTOM_GOAT, new HashMap<>() {{
            put(AttributeKey.MOVEMENT_SPEED, 0.2);
            put(AttributeKey.MAX_HEALTH, 10.0);
            put(AttributeKey.ENERGY, 100.0);
        }});

        ENTITY_ATTRIBUTE_DEFAULTS.put(ModEntities.CUSTOM_FOX, new HashMap<>() {{
            put(AttributeKey.MOVEMENT_SPEED, 0.2);
            put(AttributeKey.MAX_HEALTH, 10.0);
            put(AttributeKey.ENERGY, 100.0);
        }});

        ENTITY_ATTRIBUTE_DEFAULTS.put(ModEntities.CUSTOM_MOOSHROOM, new HashMap<>() {{
            put(AttributeKey.MOVEMENT_SPEED, 0.2);
            put(AttributeKey.MAX_HEALTH, 10.0);
            put(AttributeKey.ENERGY, 100.0);
            put(AttributeKey.MAX_MEAT, 3.0);
            put(AttributeKey.MAX_LEATHER, 2.0);
        }});

        // Initialize the globalAttributes map AFTER registering defaults
        initializeGlobalMap();
    }

    // Separated initialization of the runtime map from default registration
    public static void initializeGlobalMap() {
        globalAttributes.clear(); // Clear existing runtime attributes
        // Populate the runtime map from the registered defaults
        for (Map.Entry<EntityType<?>, Map<AttributeKey, Double>> entry : ENTITY_ATTRIBUTE_DEFAULTS.entrySet()) {
            globalAttributes.put(entry.getKey(), new MobAttributes(new EnumMap<>(entry.getValue()))); // Use EnumMap copy
        }
    }

    // Get attributes, falling back to registered defaults if not loaded/present at runtime
    public static MobAttributes getAttributes(EntityType<?> type) {
        // Prefer loaded/runtime global attribute
        MobAttributes loadedAttrs = globalAttributes.get(type);
        if (loadedAttrs != null) {
            return loadedAttrs;
        }
        // Fallback to registered defaults if not found in runtime map
        Map<AttributeKey, Double> defaults = ENTITY_ATTRIBUTE_DEFAULTS.get(type);
        if (defaults != null) {
            // Return a new instance based on defaults so the default map isn't modified
            return new MobAttributes(new EnumMap<>(defaults));
        }
        // Absolute fallback if no defaults registered (should not happen if registerDefaults is called)
        System.err.println("WARN: No default attributes registered or loaded for entity type: " + Registries.ENTITY_TYPE.getId(type));
        return new MobAttributes(new EnumMap<>(Map.of(
            AttributeKey.MOVEMENT_SPEED, 0.2,
            AttributeKey.MAX_HEALTH, 10.0,
            AttributeKey.ENERGY, 100.0
        )));
    }

    // Update the runtime global attributes (e.g., for slow species evolution)
    public static void updateGlobalAttributes(EntityType<?> type, MobAttributes newAttributes) {
        if (type != null && newAttributes != null) {
            globalAttributes.put(type, newAttributes);
        }
    }

    // Save the current runtime globalAttributes map to NBT
    public static void save(NbtCompound tag) {
        NbtCompound attributesTag = new NbtCompound(); // Use a nested tag for clarity
        for (Map.Entry<EntityType<?>, MobAttributes> entry : globalAttributes.entrySet()) {
            NbtCompound mobTag = new NbtCompound();
            // Save all attributes present in the MobAttributes object
            entry.getValue().getAllAttributes().forEach((key, value) -> {
                mobTag.putDouble(key.getId(), value);
            });

            Identifier id = Registries.ENTITY_TYPE.getId(entry.getKey());
            if (id != null && !mobTag.isEmpty()) {
                attributesTag.put(id.toString(), mobTag);
            }
        }
        tag.put("GlobalAttributes", attributesTag); // Save under a specific key
    }

    // Load attributes from NBT into the runtime globalAttributes map
    public static void load(NbtCompound tag) {
        initializeGlobalMap(); // Start with defaults in the runtime map

        if (tag.contains("GlobalAttributes", NbtCompound.COMPOUND_TYPE)) {
            NbtCompound attributesTag = tag.getCompound("GlobalAttributes");
            for (String key : attributesTag.getKeys()) {
                Identifier id = Identifier.tryParse(key);
                if (id == null) continue;

                EntityType<?> type = Registries.ENTITY_TYPE.get(id);
                if (type != null) {
                    NbtCompound mobTag = attributesTag.getCompound(key);
                    Map<AttributeKey, Double> loadedAttributes = new EnumMap<>(AttributeKey.class);
                    // Load attributes defined in the AttributeKey enum
                    for (AttributeKey attributeKey : AttributeKey.values()) {
                        if (mobTag.contains(attributeKey.getId())) {
                            loadedAttributes.put(attributeKey, mobTag.getDouble(attributeKey.getId()));
                        }
                    }
                    // If we loaded any attributes, update the global map for this type
                    if (!loadedAttributes.isEmpty()) {
                        // Create a new MobAttributes object, potentially merging with defaults
                        // Get existing defaults for this type
                        Map<AttributeKey, Double> defaults = ENTITY_ATTRIBUTE_DEFAULTS.getOrDefault(type, new HashMap<>());
                        // Create a merged map, preferring loaded values over defaults
                        Map<AttributeKey, Double> mergedAttributes = new EnumMap<>(defaults);
                        mergedAttributes.putAll(loadedAttributes); // Loaded values overwrite defaults

                        globalAttributes.put(type, new MobAttributes(mergedAttributes));
                    }
                }
            }
        }
        System.out.println("Loaded Global Attributes: " + globalAttributes.size() + " entity types.");
    }
}
