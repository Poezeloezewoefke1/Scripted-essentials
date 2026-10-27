package dev.scripted.forgotten.mixin;

import dev.scripted.forgotten.Reality;
import net.minecraft.entity.Entity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Quiet is a harvested absence of sound: nothing within sixteen blocks of one makes a noise.
 * Every sound the server broadcasts passes through these two methods.
 */
@Mixin(ServerWorld.class)
abstract class ServerWorldMixin {
    @Inject(method = "playSound(Lnet/minecraft/entity/Entity;DDDLnet/minecraft/registry/entry/RegistryEntry;Lnet/minecraft/sound/SoundCategory;FFJ)V",
            at = @At("HEAD"), cancellable = true)
    private void forgotten$quietAt(Entity source, double x, double y, double z, RegistryEntry<SoundEvent> sound,
                                   SoundCategory category, float volume, float pitch, long seed, CallbackInfo ci) {
        if (Reality.region((ServerWorld) (Object) this).silenced(x, y, z, 16)) {
            ci.cancel();
        }
    }

    @Inject(method = "playSoundFromEntity(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;Lnet/minecraft/registry/entry/RegistryEntry;Lnet/minecraft/sound/SoundCategory;FFJ)V",
            at = @At("HEAD"), cancellable = true)
    private void forgotten$quietFrom(Entity source, Entity entity, RegistryEntry<SoundEvent> sound, SoundCategory category,
                                     float volume, float pitch, long seed, CallbackInfo ci) {
        if (Reality.region((ServerWorld) (Object) this).silenced(entity.getX(), entity.getY(), entity.getZ(), 16)) {
            ci.cancel();
        }
    }
}
