package dev.scripted.forgotten.block;

import com.mojang.serialization.MapCodec;
import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.menu.GenesisMenu;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.system.Bosses;
import dev.scripted.forgotten.system.Genesis;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.WorldMemory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The Genesis Interface. Feed it the Compiler's Source Code and the Author comes to argue; once
 * the Author has asked its question, the Interface writes the world's rules, its ending, and -
 * for whoever becomes the Author - its laws.
 */
public class GenesisInterfaceBlock extends Block {
    public static final MapCodec<GenesisInterfaceBlock> CODEC = createCodec(GenesisInterfaceBlock::new);

    public GenesisInterfaceBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!(world instanceof ServerWorld server) || !(player instanceof ServerPlayerEntity sp)) {
            return ActionResult.SUCCESS;
        }
        WorldMemory memory = Reality.memory(server.getServer());
        if (!memory.flag(Genesis.AUTHOR_ASKED)) {
            ItemStack held = sp.getMainHandStack();
            if (Bosses.alive(Bosses.Kind.AUTHOR)) {
                Texts.bar(sp, Texts.world("The Author is here. Answer it."));
            } else if (held.isOf(ModItems.SOURCE_CODE)) {
                held.decrementUnlessCreative(1, sp);
                Bosses.summon(Bosses.Kind.AUTHOR, server, pos.add(0, 1, 6));
                Texts.tell(sp, Texts.world("You read the world its own source. Something that wrote it has come to disagree."));
            } else {
                Texts.bar(sp, Texts.world("It reads source code. The Compiler keeps some."));
            }
            return ActionResult.SUCCESS;
        }
        sp.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inventory, p) -> new GenesisMenu(syncId, inventory),
                Text.translatable("container.forgotten.genesis_interface")));
        return ActionResult.SUCCESS;
    }
}
