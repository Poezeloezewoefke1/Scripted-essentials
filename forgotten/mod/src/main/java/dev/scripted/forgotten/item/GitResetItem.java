package dev.scripted.forgotten.item;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.CommitStore;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Git Reset Tool. Crouch-use on a block to commit the chunk around it (a 32-block-tall
 * slice). Use it later to roll the chunk back to that commit - every block exactly as it was,
 * at the cost of thirty experience levels. Containers and other blocks with contents are left
 * alone so a reset can never duplicate or delete what they hold.
 */
public class GitResetItem extends LoreItem {
    public static final int COST_LEVELS = 30;

    public GitResetItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!(context.getPlayer() instanceof ServerPlayerEntity player) || !(context.getWorld() instanceof ServerWorld world)) {
            return ActionResult.SUCCESS;
        }
        BlockPos pos = context.getBlockPos();
        if (player.isSneaking()) {
            CommitStore.Commit commit = commit(world, pos);
            CommitStore.get(world).put(ChunkPos.toLong(pos), commit);
            Texts.bar(player, Texts.info("Committed chunk " + new ChunkPos(pos) + ", y " + commit.baseY() + " to "
                    + (commit.baseY() + CommitStore.HEIGHT - 1) + "."));
            world.playSound(null, pos, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundCategory.PLAYERS, 1f, 1.2f);
            return ActionResult.SUCCESS;
        }
        CommitStore.Commit commit = CommitStore.get(world).get(ChunkPos.toLong(pos));
        if (commit == null) {
            Texts.bar(player, Texts.info("Nothing committed here. Crouch and use to commit this chunk."));
            return ActionResult.FAIL;
        }
        if (!player.isCreative() && player.experienceLevel < COST_LEVELS) {
            Texts.bar(player, Texts.warn("A reset costs " + COST_LEVELS + " levels."));
            return ActionResult.FAIL;
        }
        int changed = reset(world, new ChunkPos(pos), commit);
        if (!player.isCreative()) {
            player.addExperienceLevels(-COST_LEVELS);
        }
        Reality.debt(player, 5);
        Reality.chronicle(world, ChronicleEvent.ANOMALY_RESOLVED, pos, player.getName().getString(), "git_reset:" + changed);
        Texts.bar(player, Texts.discovery("HEAD is now at the commit. " + changed + " blocks restored."));
        world.playSound(null, pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1f, 1.4f);
        return ActionResult.SUCCESS;
    }

    public static CommitStore.Commit commit(ServerWorld world, BlockPos at) {
        int base = Math.max(world.getBottomY(), Math.min(at.getY() - 8, world.getTopYInclusive() - CommitStore.HEIGHT + 1));
        ChunkPos chunk = new ChunkPos(at);
        List<BlockState> palette = new ArrayList<>();
        Map<BlockState, Integer> ids = new HashMap<>();
        int[] blocks = new int[16 * 16 * CommitStore.HEIGHT];
        BlockPos.Mutable p = new BlockPos.Mutable();
        for (int y = 0; y < CommitStore.HEIGHT; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState s = world.getBlockState(p.set(chunk.getStartX() + x, base + y, chunk.getStartZ() + z));
                    Integer id = ids.get(s);
                    if (id == null) {
                        id = palette.size();
                        palette.add(s);
                        ids.put(s, id);
                    }
                    blocks[CommitStore.Commit.index(x, y, z)] = id;
                }
            }
        }
        return new CommitStore.Commit(base, world.getTime(), palette, blocks);
    }

    /** Rolls a chunk back. Returns how many blocks changed. */
    public static int reset(ServerWorld world, ChunkPos chunk, CommitStore.Commit commit) {
        int changed = 0;
        BlockPos.Mutable p = new BlockPos.Mutable();
        for (int y = 0; y < CommitStore.HEIGHT; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState target = commit.palette().get(commit.blocks()[CommitStore.Commit.index(x, y, z)]);
                    p.set(chunk.getStartX() + x, commit.baseY() + y, chunk.getStartZ() + z);
                    BlockState current = world.getBlockState(p);
                    if (current.equals(target) || current.hasBlockEntity() || target.hasBlockEntity()) {
                        continue;
                    }
                    world.setBlockState(p, target, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                    changed++;
                    if (changed % 16 == 0) {
                        world.spawnParticles(ParticleTypes.END_ROD, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 1, 0, 0, 0, 0);
                    }
                }
            }
        }
        return changed;
    }
}
