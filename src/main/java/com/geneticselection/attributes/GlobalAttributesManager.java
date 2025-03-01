package com.geneticselection.attributes;

import com.geneticselection.mobs.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

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
        }});

        ENTITY_ATTRIBUTE_DEFAULTS.put(ModEntities.CUSTOM_DONKEY, new HashMap<>() {{
            put(AttributeKey.MOVEMENT_SPEED, 0.2);
            put(AttributeKey.MAX_HEALTH, 15.0);
            put(AttributeKey.ENERGY, 100.0);
        }});

        ENTITY_ATTRIBUTE_DEFAULTS.put(ModEntities.CUSTOM_CAMEL, new HashMap<>() {{
            put(AttributeKey.MOVEMENT_SPEED, 0.175);
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
            put(AttributeKey.MOVEMENT_SPEED, 0.175);
            put(AttributeKey.MAX_HEALTH, 3.0);
            put(AttributeKey.ENERGY, 100.0);
            put(AttributeKey.MAX_MEAT, 3.0);
            put(AttributeKey.MAX_FEATHERS, 2.0);
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
    }

    public static void initialize() {
        if (ENTITY_ATTRIBUTE_DEFAULTS.isEmpty()) {
            registerDefaults();
        }
        for (Map.Entry<EntityType<?>, Map<AttributeKey, Double>> entry : ENTITY_ATTRIBUTE_DEFAULTS.entrySet()) {
            globalAttributes.put(entry.getKey(), new MobAttributes(entry.getValue()));
        }
    }

    public static MobAttributes getAttributes(EntityType<?> type) {
        return globalAttributes.getOrDefault(type,
                new MobAttributes(new HashMap<>() {{
                    put(AttributeKey.MOVEMENT_SPEED, 0.2);
                    put(AttributeKey.MAX_HEALTH, 10.0);
                    put(AttributeKey.ENERGY, 100.0);
                }}));
    }

    public static void updateGlobalAttributes(EntityType<?> type, MobAttributes newAttributes) {
        globalAttributes.put(type, newAttributes);
    }

    public static void save(NbtCompound tag) {
        for (Map.Entry<EntityType<?>, MobAttributes> entry : globalAttributes.entrySet()) {
            NbtCompound mobTag = new NbtCompound();
            for (Map.Entry<AttributeKey, Double> attr : entry.getValue().getAllAttributes().entrySet()) {
                mobTag.putDouble(attr.getKey().getId(), attr.getValue());
            }
            Identifier id = Registries.ENTITY_TYPE.getId(entry.getKey());
            if (id != null) {
                tag.put(id.toString(), mobTag);
            }
        }
    }

    public static void load(NbtCompound tag) {
        globalAttributes.clear();
        for (String key : tag.getKeys()) {
            Identifier id = Identifier.tryParse(key);
            if (id == null) continue;
            EntityType<?> type = Registries.ENTITY_TYPE.get(id);
            if (type != null) {
                NbtCompound mobTag = tag.getCompound(key);
                Map<AttributeKey, Double> loadedAttributes = new HashMap<>();
                for (AttributeKey attributeKey : AttributeKey.values()) {
                    if (mobTag.contains(attributeKey.getId())) {
                        loadedAttributes.put(attributeKey, mobTag.getDouble(attributeKey.getId()));
                    }
                }
                globalAttributes.put(type, new MobAttributes(loadedAttributes));
            }
        }
    }
}
