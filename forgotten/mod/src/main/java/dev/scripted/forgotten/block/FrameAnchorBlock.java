package dev.scripted.forgotten.block;

import com.mojang.serialization.MapCodec;
import dev.scripted.forgotten.system.Nesting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The Matryoshka's door. Use it to step one depth down into a copy of the world where time runs
 * faster; crouch-use it inside a depth to climb back up.
 */
public class FrameAnchorBlock extends Block {
    public static final MapCodec<FrameAnchorBlock> CODEC = createCodec(FrameAnchorBlock::new);

    public FrameAnchorBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if (world instanceof ServerWorld server) {
            Nesting.register(server, pos);
        }
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        Nesting.unregister(world, pos);
        super.onStateReplaced(state, world, pos, moved);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (player instanceof ServerPlayerEntity sp) {
            if (sp.isSneaking()) {
                Nesting.ascend(sp);
            } else {
                Nesting.descend(sp, pos);
            }
        }
        return ActionResult.SUCCESS;
    }
}
