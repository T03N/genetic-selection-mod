package com.geneticselection.attributes;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class MobAttributes {
    private final Map<AttributeKey, Double> attributes = new EnumMap<>(AttributeKey.class);

    // New constructor that takes a map (used by GlobalAttributesManager)
    public MobAttributes(Map<AttributeKey, Double> defaults) {
        attributes.putAll(defaults);
    }

    // Backward compatibility constructor used by mob classes
    public MobAttributes(double movementSpeed, double maxHealth, double energy, Optional<Double> maxMeat, Optional<Double> maxLeather, Optional<Double> maxWool, Optional<Double> maxRabbitHide, Optional<Double> maxFeathers) {
        attributes.put(AttributeKey.MOVEMENT_SPEED, movementSpeed);
        attributes.put(AttributeKey.MAX_HEALTH, maxHealth);
        attributes.put(AttributeKey.ENERGY, energy);
        if (maxMeat != null && maxMeat.isPresent()) {
            attributes.put(AttributeKey.MAX_MEAT, maxMeat.get());
        }
        if (maxLeather != null && maxLeather.isPresent()) {
            attributes.put(AttributeKey.MAX_LEATHER, maxLeather.get());
        }
        if (maxWool != null && maxWool.isPresent()) {
            attributes.put(AttributeKey.MAX_WOOL, maxWool.get());
        }
        if (maxRabbitHide != null && maxRabbitHide.isPresent()) {
            attributes.put(AttributeKey.MAX_RABBIT_HIDE, maxRabbitHide.get());
        }
        if (maxFeathers != null && maxFeathers.isPresent()) {
            attributes.put(AttributeKey.MAX_FEATHERS, maxFeathers.get());
        }
    }

    public double get(AttributeKey key) {
        return attributes.getOrDefault(key, 0.0);
    }

    public void set(AttributeKey key, double value) {
        attributes.put(key, value);
    }

    public double getMovementSpeed() {
        return get(AttributeKey.MOVEMENT_SPEED);
    }

    public double getMaxHealth() {
        return get(AttributeKey.MAX_HEALTH);
    }

    public double getEnergyLvl() {
        return get(AttributeKey.ENERGY);
    }

    public Map<AttributeKey, Double> getAllAttributes() {
        return attributes;
    }

    // Backward compatibility getters
    public Optional<Double> getMaxMeat() {
        return Optional.ofNullable(attributes.get(AttributeKey.MAX_MEAT));
    }

    public Optional<Double> getMaxLeather() {
        return Optional.ofNullable(attributes.get(AttributeKey.MAX_LEATHER));
    }

    public Optional<Double> getMaxWool() {
        return Optional.ofNullable(attributes.get(AttributeKey.MAX_WOOL));
    }

    public Optional<Double> getMaxRabbitHide() {
        return Optional.ofNullable(attributes.get(AttributeKey.MAX_RABBIT_HIDE));
    }

    public Optional<Double> getMaxFeathers() {
        return Optional.ofNullable(attributes.get(AttributeKey.MAX_FEATHERS));
    }
}
