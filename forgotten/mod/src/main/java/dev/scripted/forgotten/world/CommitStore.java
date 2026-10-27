package dev.scripted.forgotten.world;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Git Reset commits: for each committed chunk of a dimension, the blocks of a 32-block-tall
 * slice of it, stored as a palette and indices.
 */
public final class CommitStore extends PersistentState {
    public static final int HEIGHT = 32;
    public static final Codec<CommitStore> CODEC = NbtCompound.CODEC.xmap(CommitStore::fromNbt, CommitStore::toNbt);
    public static final PersistentStateType<CommitStore> TYPE =
            new PersistentStateType<>("forgotten_commits", CommitStore::new, CODEC, null);

    private final Map<Long, Commit> commits = new HashMap<>();

    public static CommitStore get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }

    public void put(long chunkKey, Commit commit) {
        commits.put(chunkKey, commit);
        markDirty();
    }

    public Commit get(long chunkKey) {
        return commits.get(chunkKey);
    }

    /**
     * @param baseY   lowest y of the slice
     * @param time    world time the commit was made
     * @param palette distinct states
     * @param blocks  palette index per position, ordered y, then z, then x
     */
    public record Commit(int baseY, long time, List<BlockState> palette, int[] blocks) {
        public static int index(int x, int y, int z) {
            return (y * 16 + z) * 16 + x;
        }
    }

    private NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        NbtList list = new NbtList();
        for (Map.Entry<Long, Commit> e : commits.entrySet()) {
            Commit c = e.getValue();
            NbtCompound tag = new NbtCompound();
            tag.putLong("k", e.getKey());
            tag.putInt("y", c.baseY());
            tag.putLong("t", c.time());
            NbtList palette = new NbtList();
            for (BlockState s : c.palette()) {
                BlockState.CODEC.encodeStart(NbtOps.INSTANCE, s).result().ifPresent(palette::add);
            }
            tag.put("p", palette);
            tag.putIntArray("b", c.blocks());
            list.add(tag);
        }
        nbt.put("commits", list);
        return nbt;
    }

    private static CommitStore fromNbt(NbtCompound nbt) {
        CommitStore store = new CommitStore();
        for (NbtElement el : nbt.getListOrEmpty("commits")) {
            if (!(el instanceof NbtCompound tag)) {
                continue;
            }
            List<BlockState> palette = new ArrayList<>();
            for (NbtElement p : tag.getListOrEmpty("p")) {
                palette.add(BlockState.CODEC.parse(NbtOps.INSTANCE, p).result()
                        .orElse(net.minecraft.block.Blocks.AIR.getDefaultState()));
            }
            int[] blocks = tag.getIntArray("b").orElse(new int[0]);
            if (blocks.length != 16 * 16 * HEIGHT || palette.isEmpty()) {
                continue;
            }
            store.commits.put(tag.getLong("k", 0), new Commit(tag.getInt("y", 0), tag.getLong("t", 0), palette, blocks));
        }
        return store;
    }
}
