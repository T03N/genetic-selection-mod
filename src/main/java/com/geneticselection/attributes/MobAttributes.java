package com.geneticselection.attributes;
import java.util.Optional;

public class MobAttributes {
    private double movementSpeed;
    private double maxHealth;
    private double energyLvl;
    private Optional<Double> maxMeat = Optional.empty();
    private Optional<Double> maxLeather = Optional.empty();
    private Optional<Double> maxWool = Optional.empty();
    private Optional<Double> maxRabbitHide = Optional.empty();
    private Optional<Double> maxFeathers = Optional.empty();

    // Constructor with optional max values for attributes
    public MobAttributes(double movementSpeed, double maxHealth, double energyLvl, Optional<Double> maxMeat, Optional<Double> maxLeather, Optional<Double> maxWool, Optional<Double> maxRabbitHide, Optional<Double> maxFeathers) {
        this.movementSpeed = movementSpeed;
        this.maxHealth = maxHealth;
        this.energyLvl = energyLvl;
        this.maxMeat = maxMeat;
        this.maxLeather = maxLeather;
        this.maxWool = maxWool;
        this.maxRabbitHide = maxRabbitHide;
        this.maxFeathers = maxFeathers;
    }

    public double getMovementSpeed() {
        return movementSpeed;
    }

    public void setMovementSpeed(double movementSpeed) {
        this.movementSpeed = movementSpeed;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
    }

    public void setEnergyLvl(double energyLvl) {
        this.energyLvl = energyLvl;
    }

    public double getEnergyLvl() {
        return energyLvl;
    }

    // Getter and setter for max values
    public Optional<Double> getMaxMeat() {
        return maxMeat;
    }

    public void setMaxMeat(double maxMeat) {
        this.maxMeat = Optional.of(maxMeat);
    }

    public Optional<Double> getMaxLeather() {
        return maxLeather;
    }

    public void setMaxLeather(double maxLeather) {
        this.maxLeather = Optional.of(maxLeather);
    }

    public Optional<Double> getMaxWool() {
        return maxWool;
    }

    public void setMaxWool(double maxWool) {
        this.maxWool = Optional.of(maxWool);
    }

    public Optional<Double> getMaxRabbitHide() {
        return maxRabbitHide;
    }

    public void setMaxRabbitHide(double maxHide) {
        this.maxRabbitHide = Optional.of(maxHide);
    }

    public Optional<Double> getMaxFeathers() {
        return maxFeathers;
    }

    public void setMaxFeathers(double maxFeathers) {
        this.maxFeathers = Optional.of(maxFeathers);
    }


}