package com.geneticselection.mixin;

import com.geneticselection.attributes.MobAttributes;
import com.geneticselection.interfaces.IGeneticEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnimalEntity.class)
public class AnimalEntityGeneticsMixin implements IGeneticEntity {

    @Unique
    private MobAttributes mobAttributes;

    @Override
    public MobAttributes getMobAttributes() {
        return mobAttributes;
    }

    @Override
    public void setMobAttributes(MobAttributes attributes) {
        this.mobAttributes = attributes;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    private void saveGeneticData(NbtCompound nbt, CallbackInfo ci) {
        if (mobAttributes != null) {
            nbt.putDouble("GeneticSpeed", mobAttributes.getMovementSpeed());
            nbt.putDouble("GeneticHealth", mobAttributes.getMaxHealth());
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    private void loadGeneticData(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("GeneticSpeed") && nbt.contains("GeneticHealth")) {
            double speed = nbt.getDouble("GeneticSpeed");
            double health = nbt.getDouble("GeneticHealth");
            mobAttributes = new MobAttributes(speed, health);
        }
    }
}