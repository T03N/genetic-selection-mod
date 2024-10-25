package com.geneticselection.mixin;

import com.geneticselection.genetics.GeneticsRegistry;
import com.geneticselection.genetics.Genetics;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(AnimalEntity.class)
public class AnimalEntityMixin {

    @Inject(method = "breed", at = @At("HEAD"), cancellable = true)
    private void injectBreedLogic(ServerWorld world, AnimalEntity otherParent, CallbackInfo ci) {
        AnimalEntity parent1 = (AnimalEntity) (Object) this;

        Genetics parent1Genetics = GeneticsRegistry.getGenetics(parent1.getType());
        Genetics parent2Genetics = GeneticsRegistry.getGenetics(otherParent.getType());

        if (parent1Genetics != null && parent2Genetics != null) {
            AnimalEntity offspring = parent1Genetics.breed(parent1, otherParent, world);

            if (offspring != null) {
                world.spawnEntity(offspring);
                ci.cancel();  // Prevent default spawning
            }
        }
    }
}