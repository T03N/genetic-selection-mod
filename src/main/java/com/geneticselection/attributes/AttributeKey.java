package com.geneticselection.attributes;

public enum AttributeKey {
    MOVEMENT_SPEED("movementSpeed"),
    MAX_HEALTH("maxHealth"),
    ENERGY("energyLvl"),
    MAX_MEAT("maxMeat"),
    MAX_LEATHER("maxLeather"),
    MAX_WOOL("maxWool"),
    MAX_RABBIT_HIDE("maxRabbitHide"),
    MAX_FEATHERS("maxFeathers");

    private final String id;

    AttributeKey(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
