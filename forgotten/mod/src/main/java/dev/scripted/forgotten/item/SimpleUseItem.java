package dev.scripted.forgotten.item;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.player.PlayerMemory;
import dev.scripted.forgotten.player.RealityEdit;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.WorldMemory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Small one-shot items whose whole behaviour is a few lines. Kept together so each is easy to
 * find: the Lullaby, the Block of Air, the Concept of Tuesday and Unwritten Death.
 */
public class SimpleUseItem extends LoreItem {
    public enum Kind {
        /** Forces the world back toward sleep for a while. A temporary mercy. */
        LULLABY,
        /** Bought from the Draft. Turns one block into air. Sometimes it works. */
        BLOCK_OF_AIR,
        /** Also from the Draft. It becomes Tuesday. */
        TUESDAY,
        /** Death does not apply to this player, for five minutes. The debt comes later. */
        UNWRITTEN_DEATH
    }

    private final Kind kind;

    public SimpleUseItem(Settings settings, Kind kind) {
        super(settings, kind == Kind.UNWRITTEN_DEATH);
        this.kind = kind;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (kind != Kind.BLOCK_OF_AIR || !(context.getWorld() instanceof ServerWorld world)) {
            return ActionResult.PASS;
        }
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (state.getHardness(world, pos) < 0) {
            return ActionResult.FAIL;
        }
        world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        context.getStack().decrement(1);
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!(world instanceof ServerWorld server) || !(user instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }
        long now = Reality.now(server.getServer());
        switch (kind) {
            case LULLABY -> {
                WorldMemory memory = Reality.memory(server.getServer());
                memory.lucidity.lull(400);
                memory.markDirty();
                Reality.player(player).mercyUntil = now + 12000;
                Texts.tell(player, Texts.world("You sing to the world, and for a while it sleeps more deeply."));
            }
            case TUESDAY -> {
                long time = server.getTimeOfDay();
                long day = time / 24000L;
                long target = day + Math.floorMod(1 - day, 7);
                if (target * 24000L + 1000 <= time) {
                    target += 7;
                }
                server.setTimeOfDay(target * 24000L + 1000);
                Texts.tell(player, Texts.world("It is Tuesday."));
            }
            case UNWRITTEN_DEATH -> {
                PlayerMemory memory = Reality.player(player);
                memory.deathlessUntil = now + 6000;
                memory.addEdit(new RealityEdit(RealityEdit.Kind.DEATHLESS, server.getRegistryKey().getValue().toString(),
                        player.getBlockPos().asLong(), 0, true));
                Reality.debt(player, 40);
                Texts.tell(player, Texts.discovery("DEATH DOES NOT APPLY TO THIS PLAYER."));
                Texts.tell(player, Texts.world("For a while, the world will reject the events that would end you. It will remember each one."));
            }
            case BLOCK_OF_AIR -> {
                return ActionResult.PASS;
            }
        }
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 1f, 0.7f);
        user.getStackInHand(hand).decrement(1);
        return ActionResult.SUCCESS;
    }
}
