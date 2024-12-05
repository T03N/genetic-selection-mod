package com.geneticselection.attributes;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

public class GlobalAttributesSavedData extends PersistentState {
    private static final String KEY = "genetic-selection";

    public GlobalAttributesSavedData() {
        super();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        return toTag(nbt);
    }

    public void fromTag(NbtCompound tag) {
        GlobalAttributesManager.load(tag);
    }

    public NbtCompound toTag(NbtCompound tag) {
        GlobalAttributesManager.save(tag);
        return tag;
    }

    public static GlobalAttributesSavedData fromWorld(ServerWorld world) {
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(new Type<>(
                GlobalAttributesSavedData::new,
                (nbt, registryLookup) -> {
                    GlobalAttributesSavedData data = new GlobalAttributesSavedData();
                    data.fromTag(nbt);
                    return data;
                },
                null
        ), KEY);
    }

    public static void save(ServerWorld world) {
        PersistentStateManager manager = world.getPersistentStateManager();
        GlobalAttributesSavedData data = fromWorld(world);
        data.markDirty();
    }
}