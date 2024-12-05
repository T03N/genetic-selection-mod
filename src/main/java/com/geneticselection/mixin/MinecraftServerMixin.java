package com.geneticselection.mixin;

import com.geneticselection.attributes.GlobalAttributesSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Inject(at = @At("HEAD"), method = "shutdown")
    private void onShutdown(CallbackInfo info) {
        for (ServerWorld world : ((MinecraftServer) (Object) this).getWorlds()) {
            GlobalAttributesSavedData.save(world);
        }
    }
}