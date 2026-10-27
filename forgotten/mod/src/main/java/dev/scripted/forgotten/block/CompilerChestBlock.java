package dev.scripted.forgotten.block;

import com.mojang.serialization.MapCodec;
import dev.scripted.forgotten.block.entity.CompilerChestBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Silicon and Spores: instead of crafting, you write. Lay items out one instruction per row,
 * put a silicon wafer in the last slot, and close the chest. If the code is valid you get a
 * Cartridge to load into a Bio-Node; if not, the chest finds out the hard way.
 */
public class CompilerChestBlock extends BlockWithEntity {
    public static final MapCodec<CompilerChestBlock> CODEC = createCodec(CompilerChestBlock::new);

    public CompilerChestBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CompilerChestBlockEntity(pos, state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient() && world.getBlockEntity(pos) instanceof CompilerChestBlockEntity chest) {
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inventory, p) -> GenericContainerScreenHandler.createGeneric9x6(syncId, inventory, chest.code),
                    Text.translatable("container.forgotten.compiler_chest")));
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (world.getBlockEntity(pos) instanceof CompilerChestBlockEntity chest) {
            ItemScatterer.spawn(world, pos, chest.code);
        }
        super.onStateReplaced(state, world, pos, moved);
    }
}
