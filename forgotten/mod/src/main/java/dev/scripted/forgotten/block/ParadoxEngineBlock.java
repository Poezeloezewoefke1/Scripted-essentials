package dev.scripted.forgotten.block;

import com.mojang.serialization.MapCodec;
import dev.scripted.forgotten.block.entity.ParadoxEngineBlockEntity;
import dev.scripted.forgotten.menu.ParadoxEngineMenu;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The Paradox Engine: a crafting table fused with a clock, a beacon and something organic. It
 * does not craft. Put a Subject and a Contradiction (and for the hardest edits, a Catalyst) in
 * it, and it convinces reality that the result should exist.
 */
public class ParadoxEngineBlock extends BlockWithEntity {
    public static final MapCodec<ParadoxEngineBlock> CODEC = createCodec(ParadoxEngineBlock::new);

    public ParadoxEngineBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ParadoxEngineBlockEntity(pos, state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient() && world.getBlockEntity(pos) instanceof ParadoxEngineBlockEntity engine) {
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inventory, p) -> new ParadoxEngineMenu(syncId, inventory, engine),
                    Text.translatable("container.forgotten.paradox_engine")));
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (world.getBlockEntity(pos) instanceof ParadoxEngineBlockEntity engine) {
            ItemScatterer.spawn(world, pos, engine.inputs);
        }
        super.onStateReplaced(state, world, pos, moved);
    }
}
