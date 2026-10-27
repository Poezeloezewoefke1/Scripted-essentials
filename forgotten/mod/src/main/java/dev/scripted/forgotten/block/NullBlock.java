package dev.scripted.forgotten.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * What the world becomes past the addresses it can hold: no collision, no texture, and anything
 * that passes through it may stop existing. Items dropped into it are gone; a player wading
 * through it loses an item every second.
 */
public class NullBlock extends Block {
    public static final MapCodec<NullBlock> CODEC = createCodec(NullBlock::new);

    public NullBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity,
                                     EntityCollisionHandler handler, boolean intersects) {
        if (!(world instanceof ServerWorld)) {
            return;
        }
        if (entity instanceof ItemEntity item) {
            item.discard();
            return;
        }
        if (entity instanceof net.minecraft.server.network.ServerPlayerEntity sp && world.getTime() % 60 == 0
                && dev.scripted.forgotten.Reality.stage((ServerWorld) world, pos)
                        .atLeast(dev.scripted.forgotten.logic.integrity.IntegrityStage.AMNESIA)
                && dev.scripted.forgotten.system.Rifts.fallThrough(sp)) {
            return;
        }
        if (entity instanceof PlayerEntity player && !player.isCreative() && world.getTime() % 20 == 0) {
            var inv = player.getInventory();
            int size = inv.size();
            int start = world.getRandom().nextInt(size);
            for (int i = 0; i < size; i++) {
                int slot = (start + i) % size;
                ItemStack stack = inv.getStack(slot);
                if (!stack.isEmpty()) {
                    stack.decrement(1);
                    break;
                }
            }
        }
    }
}
