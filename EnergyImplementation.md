# Energy System Implementation Guide for Custom Mobs

## Implementation Details

This implementation is based on a genetic selection system where mobs have variable attributes that can be inherited and affected by their energy levels.

### Key Features
- Energy-based breeding restrictions
- Dynamic energy gain/loss based on environment
- Energy impact on movement speed and resource drops
- Health regeneration at maximum energy
- Energy-based attribute inheritance
- Damage causes immediate energy loss

### Energy System Rules
- Energy Range: 0.0 to 100.0
- Breeding Threshold: 20.0
- Energy gain from breeding item: +10.0
- Energy loss from damage: 20% of current energy
- Health regeneration at 100 energy: +0.5 HP per tick
- Movement speed scales linearly with energy percentage

## Core Components Overview

### 1. Base Attributes
```java
// These attributes are consistent across all mobs
private MobAttributes mobAttributes;
private double MaxHp;
private double Speed;
private double ELvl;

// [MOB-SPECIFIC] Resource drops - customize based on mob type
private double MaxMeat;     // For meat-dropping mobs
private double MinMeat;     // For meat-dropping mobs
private double MaxLeather;  // For leather-dropping mobs
private double MinLeather; // For leather-dropping mobs
// Add other resource attributes as needed
```

### 2. Constructor Setup
```java
public CustomMobEntity(EntityType entityType, World world) {
    super(entityType, world);

    // Standard attribute initialization
    if(this.mobAttributes == null){
        MobAttributes global = GlobalAttributesManager.getAttributes(entityType);
        double speed = global.getMovementSpeed() * (0.98 + Math.random() * 0.1);
        double health = global.getMaxHealth() * (0.98 + Math.random() * 0.1);
        double energy = global.getEnergyLvl() * (0.9 + Math.random() * 0.1);
        
        // [MOB-SPECIFIC] Resource initialization
        double meat = global.getMaxMeat().orElse(0.0) + (0.98 + Math.random() * 0.1);
        // Add other resources as needed
        
        this.mobAttributes = new MobAttributes(/* Add required parameters */);
    }

    // Apply base attributes
    this.MaxHp = this.mobAttributes.getMaxHealth();
    this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(this.MaxHp);
    this.Speed = this.mobAttributes.getMovementSpeed();
    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(this.Speed);
    this.ELvl = this.mobAttributes.getEnergyLvl();
}
```

### 3. Energy-Based Breeding System
```java
@Override
public ActionResult interactMob(PlayerEntity player, Hand hand) {
    ItemStack itemStack = player.getStackInHand(hand);
    ItemStack offHandStack = player.getOffHandStack();

    // [MOB-SPECIFIC] Modify breeding item check
    if (itemStack.isOf(Items.WHEAT) || offHandStack.isOf(Items.WHEAT)) { // Replace WHEAT with mob-specific breeding item
        // Standard energy check for breeding
        if (ELvl < 20.0) {
            player.sendMessage(Text.of("This mob cannot breed because it has low energy."), true);
            return ActionResult.FAIL;
        }
        return super.interactMob(player, itemStack.isOf(Items.WHEAT) ? hand : Hand.OFF_HAND);
    }
    
    // [MOB-SPECIFIC] Add other mob-specific interactions here
    
    return super.interactMob(player, hand);
}
```

### 4. Energy-Based Resource Drops
```java
@Override
public void onDeath(DamageSource source) {
    if (this.isBaby()) {
        return;
    }

    if (ELvl <= 0.0) {
        // [MOB-SPECIFIC] Zero energy drops
        this.dropStack(new ItemStack(Items.BONE, 1));
    } else {
        super.onDeath(source);
        if (!this.getWorld().isClient) {
            // [MOB-SPECIFIC] Resource drops
            // Example for meat drops:
            int scaledMeatAmount = (int) ((MinMeat + this.getWorld().random.nextInt((int) (MaxMeat - MinMeat) + 1)) 
                * (ELvl / 100.0));
            this.dropStack(new ItemStack(Items.BEEF, Math.max(0, scaledMeatAmount))); // Replace BEEF with mob-specific meat
            
            // Add other resource drops as needed
        }
    }
}
```

### 5. Energy Tick System
```java
@Override
public void tick() {
    super.tick();

    if (!this.getWorld().isClient) {
        // [MOB-SPECIFIC] Energy gain condition
        boolean isOnEnergySource = this.getWorld().getBlockState(this.getBlockPos().down())
            .isOf(Blocks.GRASS_BLOCK); // Replace with mob-specific energy source block

        // Standard energy adjustment
        if (isOnEnergySource) {
            ELvl = Math.min(100.0, ELvl + 0.1);
        } else {
            ELvl = Math.max(0.0, ELvl - 0.05);
        }

        // Standard energy death check
        if (ELvl <= 0.0) {
            this.kill();
        } else {
            this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                .setBaseValue(Speed * (ELvl / 100.0));
            updateDescription(this);
        }
    }
}
```

### 6. Breeding and Child Creation
```java
@Override
public CustomMobEntity createChild(ServerWorld serverWorld, PassiveEntity mate) {
    if (!(mate instanceof CustomMobEntity)) {
        return (CustomMobEntity) EntityType.MOB.create(serverWorld); // Replace MOB with specific entity type
    }

    CustomMobEntity parent1 = this;
    CustomMobEntity parent2 = (CustomMobEntity) mate;

    // Standard attribute inheritance
    double childMaxHp = (parent1.MaxHp + parent2.MaxHp) / 2;
    double childEnergy = (parent1.ELvl + parent2.ELvl) / 2;

    // [MOB-SPECIFIC] Resource inheritance
    double childMaxMeat = (parent1.MaxMeat + parent2.MaxMeat) / 2;
    // Add other resource inheritance as needed

    CustomMobEntity child = new CustomMobEntity(ModEntities.CUSTOM_MOB, serverWorld); // Replace CUSTOM_MOB

    // Apply inherited attributes
    child.MaxHp = childMaxHp;
    child.ELvl = childEnergy;
    // Apply other inherited attributes

    return child;
}
```

## Customization Checklist

When implementing a new mob, modify these key elements:

1. **Resource Attributes**
    - [ ] Add relevant resource fields (meat, leather, wool, etc.)
    - [ ] Initialize resource values in constructor
    - [ ] Add getters/setters for resources

2. **Breeding System**
    - [ ] Set appropriate breeding item (wheat, carrot, seeds, etc.)
    - [ ] Define energy threshold for breeding
    - [ ] Add mob-specific breeding behaviors

3. **Energy Source**
    - [ ] Define appropriate block(s) for energy gain
    - [ ] Set energy gain/loss rates
    - [ ] Add any special energy gain conditions

4. **Resource Drops**
    - [ ] Define appropriate items to drop
    - [ ] Set drop quantities and scaling
    - [ ] Add special drop conditions

5. **Description Updates**
    - [ ] Customize description format
    - [ ] Add mob-specific attributes to display

## Environment Energy Mechanics

```java
// In tick() method
if (!this.getWorld().isClient) {
    // Energy loss from damage
    if (wasRecentlyHit) {
        ELvl = Math.max(0.0, ELvl * 0.8);  // Lose 20% of current energy
        wasRecentlyHit = false;
    }

    // Environmental energy changes
    boolean isOnEnergySource = this.getWorld().getBlockState(this.getBlockPos().down())
        .isOf(Blocks.GRASS_BLOCK);

    if (isOnEnergySource) {
        if (Math.random() < 0.2) {  // 20% chance per tick
            ELvl = Math.min(100.0, ELvl + (0.01 + Math.random() * 0.19));
        }
    } else {
        if (Math.random() < 0.5) {  // 50% chance per tick
            ELvl = Math.max(0.0, ELvl - (0.01 + Math.random() * 0.19));
        }
    }

    // Health regeneration at max energy
    if (ELvl == 100.0 && this.getHealth() < this.getMaxHealth()) {
        this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + 0.5F));
    }
}
```

## Breeding System Details

```java
// Energy threshold check
if (ELvl < 20.0) {
    player.sendMessage(Text.of("This mob cannot breed due to low energy."), true);
    return ActionResult.FAIL;
}

// Energy gain from breeding item
if (this.isInLove() && ELvl < 100.0) {
    ELvl = Math.min(100.0, ELvl + 10.0);
    player.sendMessage(Text.of("Energy increased to: " + String.format("%.1f", ELvl)), true);
}
```

## Inheritance System
```java
// Calculate inheritance factor based on parent energy
double inheritanceFactor = Math.min(parent1.ELvl, parent2.ELvl) / 100.0;

// Inherit and scale attributes
double childMaxHp = ((parent1.MaxHp + parent2.MaxHp) / 2) * inheritanceFactor;
double childEnergy = ((parent1.ELvl + parent2.ELvl) / 2) * inheritanceFactor;

// Apply inherited attributes
child.MaxHp = childMaxHp;
child.ELvl = childEnergy;
child.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
    .setBaseValue(child.Speed * (child.ELvl / 100.0));
```

## Panic Response System

Mobs implement a panic response when damaged:

```java
// Fields for panic system
private int panicTicks = 0;
private static final int PANIC_DURATION = 100; // 5 seconds at 20 ticks per second
private static final double PANIC_SPEED_MULTIPLIER = 2.0;

// In applyDamage method
@Override
protected void applyDamage(DamageSource source, float amount) {
    super.applyDamage(source, amount);
    wasRecentlyHit = true;
    
    // Start panic mode
    panicTicks = PANIC_DURATION;
    
    // Increase speed temporarily
    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
            .setBaseValue(Speed * (ELvl / 100.0) * PANIC_SPEED_MULTIPLIER);
}

// In tick method
@Override
public void tick() {
    super.tick();
    
    //Keep old stuff
    
    
    if (panicTicks > 0) {
        panicTicks--;
        if (panicTicks == 0) {
            // Reset speed back to normal when panic ends
            this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                    .setBaseValue(Speed * (ELvl / 100.0));
        }
    }
}
```

### Panic System Features
- Triggers when mob takes damage
- Doubles movement speed for 5 seconds
- Speed multiplier still affected by energy level
- Automatically returns to normal speed after duration
- Can be customized per mob type

- **Energy Thresholds**
    - Maximum Energy: 100.0
    - Minimum Energy: 0.0
    - Breeding Threshold: 20.0

- **Energy Rates**
    - Environmental Gain: 0.01-0.2 per tick (20% chance)
    - Environmental Loss: 0.01-0.2 per tick (50% chance)
    - Breeding Item Gain: +10.0
    - Damage Loss: 20% of current energy

- **Attribute Variation**
    - Health/Speed Variation: ±2% (0.98 to 1.08)
    - Energy Variation: ±10% (0.9 to 1.1)