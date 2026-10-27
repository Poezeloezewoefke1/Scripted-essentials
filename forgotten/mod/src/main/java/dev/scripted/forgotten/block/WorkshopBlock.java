package dev.scripted.forgotten.block;

import com.mojang.serialization.MapCodec;
import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.menu.GenesisMenu;
import dev.scripted.forgotten.player.PlayerMemory;
import dev.scripted.forgotten.system.Rifts;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * On the Workshop's one workbench: a grass block. The real one. Your Overworld - you have been
 * inside someone else's Frame Anchor the whole time. Use it to rewrite the world's laws;
 * crouch-use it to climb back out.
 */
public class WorkshopBlock extends Block {
    public static final MapCodec<WorkshopBlock> CODEC = createCodec(WorkshopBlock::new);

    public WorkshopBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!(player instanceof ServerPlayerEntity sp)) {
            return ActionResult.SUCCESS;
        }
        if (sp.isSneaking()) {
            PlayerMemory memory = Reality.player(sp);
            String back = memory.depthStack.isEmpty() ? "" : memory.depthStack.remove(memory.depthStack.size() - 1);
            Rifts.goTo(sp, back);
            return ActionResult.SUCCESS;
        }
        sp.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inventory, p) -> new GenesisMenu(syncId, inventory, true),
                Text.translatable("container.forgotten.workshop")));
        return ActionResult.SUCCESS;
    }
}
