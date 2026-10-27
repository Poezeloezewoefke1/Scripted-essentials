package dev.scripted.forgotten.mixin;

import dev.scripted.forgotten.system.Hooks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric has no "block placed by a player" event; this is it. The world needs to know which
 * blocks were built, so it can remember them when they are destroyed.
 */
@Mixin(BlockItem.class)
abstract class BlockItemMixin {
    @Inject(method = "postPlacement", at = @At("HEAD"))
    private void forgotten$placed(BlockPos pos, World world, PlayerEntity player, ItemStack stack, BlockState state,
                                  CallbackInfoReturnable<Boolean> cir) {
        if (world instanceof ServerWorld server && player != null) {
            Hooks.placed(server, player, pos.toImmutable(), state);
        }
    }
}
