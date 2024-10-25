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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.mojang.text2speech.Narrator.LOGGER;

@Mixin(AnimalEntity.class)
public class AnimalEntityMixin {

    private static final Set<com.geneticselection.util.Pair<UUID, UUID>> breedingPairs = new HashSet<>();

    @Inject(method = "breed", at = @At("HEAD"), cancellable = true)
    private void injectBreedLogic(ServerWorld world, AnimalEntity otherParent, CallbackInfo ci) {
        AnimalEntity parent1 = (AnimalEntity) (Object) this;

        Genetics parent1Genetics = GeneticsRegistry.getGenetics(parent1.getType());
        Genetics parent2Genetics = GeneticsRegistry.getGenetics(otherParent.getType());

        if (parent1Genetics != null && parent2Genetics != null) {
            // Check if this pair has already produced an offspring
            UUID parent1Id = parent1.getUuid();
            UUID parent2Id = otherParent.getUuid();
            com.geneticselection.util.Pair<UUID, UUID> pairId = new com.geneticselection.util.Pair<>(parent1Id, parent2Id);
            com.geneticselection.util.Pair<UUID, UUID> reversePairId = new com.geneticselection.util.Pair<>(parent2Id, parent1Id);

            if (breedingPairs.contains(pairId) || breedingPairs.contains(reversePairId)) {
                // This pair has already produced an offspring, cancel breeding
                ci.cancel();
                return;
            }

            AnimalEntity offspring = parent1Genetics.breed(parent1, otherParent, world);
            if (offspring != null) {
                world.spawnEntity(offspring);
                // Mark this pair as having produced an offspring
                breedingPairs.add(pairId);
                ci.cancel();
            }
        }
    }
}