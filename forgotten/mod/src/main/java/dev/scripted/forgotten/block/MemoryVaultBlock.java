package dev.scripted.forgotten.block;

import com.mojang.serialization.MapCodec;
import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.block.entity.MemoryVaultBlockEntity;
import dev.scripted.forgotten.system.Architecture;
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
 * The Memory Vault keeps memories extracted from the world. Within sixteen blocks, it spends them
 * to reconstruct what was lost: demolished buildings and mined-out stone come back, one memory at
 * a time.
 */
public class MemoryVaultBlock extends BlockWithEntity {
    public static final MapCodec<MemoryVaultBlock> CODEC = createCodec(MemoryVaultBlock::new);

    public MemoryVaultBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MemoryVaultBlockEntity(pos, state);
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (world instanceof ServerWorld server && !oldState.isOf(this)) {
            Reality.region(server).setDevice(pos, Architecture.VAULT);
        }
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient() && world.getBlockEntity(pos) instanceof MemoryVaultBlockEntity vault) {
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inventory, p) -> GenericContainerScreenHandler.createGeneric9x3(syncId, inventory, vault.memories),
                    Text.translatable("container.forgotten.memory_vault")));
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (world.getBlockEntity(pos) instanceof MemoryVaultBlockEntity vault) {
            ItemScatterer.spawn(world, pos, vault.memories);
        }
        Reality.region(world).removeDevice(pos);
        super.onStateReplaced(state, world, pos, moved);
    }
}
