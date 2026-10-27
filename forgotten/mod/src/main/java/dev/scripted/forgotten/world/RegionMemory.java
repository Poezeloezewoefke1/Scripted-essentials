package dev.scripted.forgotten.world;

import com.mojang.serialization.Codec;
import dev.scripted.forgotten.logic.integrity.IntegrityModel;
import dev.scripted.forgotten.logic.integrity.IntegrityStage;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-dimension memory: the integrity of every chunk that has drifted from its baseline, which
 * blocks were placed by players, what was demolished or mined (so it can bleed back, regrow,
 * or be restored), and where anchors and Quiet blocks are.
 */
public final class RegionMemory extends PersistentState {
    public static final Codec<RegionMemory> CODEC = NbtCompound.CODEC.xmap(RegionMemory::fromNbt, RegionMemory::toNbt);
    public static final PersistentStateType<RegionMemory> TYPE =
            new PersistentStateType<>("forgotten_region_memory", RegionMemory::new, CODEC, null);

    public static final int PLACED_CAP = 4096;
    public static final int RUIN_CAP = 768;
    public static final int MINED_CAP = 256;

    private final Long2ObjectOpenHashMap<ChunkRecord> chunks = new Long2ObjectOpenHashMap<>();
    /** Anchor block position -> radius in chunks. */
    private final Long2IntOpenHashMap anchors = new Long2IntOpenHashMap();
    private final LongOpenHashSet quiet = new LongOpenHashSet();
    /** Chunks unmade by the Crash. They stay at zero; nothing can anchor them back. */
    private final LongOpenHashSet crashed = new LongOpenHashSet();
    private final LongOpenHashSet eaters = new LongOpenHashSet();

    public static RegionMemory get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }

    public ChunkRecord chunk(long key) {
        ChunkRecord r = chunks.get(key);
        if (r == null) {
            r = new ChunkRecord();
            chunks.put(key, r);
        }
        return r;
    }

    public ChunkRecord peek(long key) {
        return chunks.get(key);
    }

    // ---- integrity ----

    public int integrity(ServerWorld world, long chunkKey) {
        if (crashed.contains(chunkKey)) {
            return 0;
        }
        ChunkRecord r = chunks.get(chunkKey);
        if (r != null && r.integrity >= 0) {
            return r.integrity;
        }
        return baseline(world, chunkKey);
    }

    public int integrity(ServerWorld world, BlockPos pos) {
        return integrity(world, ChunkPos.toLong(pos));
    }

    public IntegrityStage stage(ServerWorld world, BlockPos pos) {
        return IntegrityStage.of(integrity(world, pos));
    }

    public void setIntegrity(long chunkKey, int value) {
        chunk(chunkKey).integrity = IntegrityStage.clamp(value);
        markDirty();
    }

    /** Adds paradox to a chunk; it is subtracted from integrity at the next update. */
    public void addParadox(BlockPos pos, int amount) {
        chunk(ChunkPos.toLong(pos)).paradox += amount;
        markDirty();
    }

    public static int baseline(ServerWorld world, long chunkKey) {
        int fixed = dev.scripted.forgotten.dimension.Dimensions.baselineIntegrity(world);
        if (fixed >= 0) {
            return fixed;
        }
        BlockPos spawn = world.getSpawnPoint().getPos();
        int cx = ChunkPos.getPackedX(chunkKey) * 16 + 8;
        int cz = ChunkPos.getPackedZ(chunkKey) * 16 + 8;
        double dx = cx - spawn.getX();
        double dz = cz - spawn.getZ();
        return IntegrityModel.baseline(Math.sqrt(dx * dx + dz * dz));
    }

    // ---- anchors ----

    public void addAnchor(BlockPos pos, int radiusChunks) {
        anchors.put(pos.asLong(), radiusChunks);
        markDirty();
    }

    public void removeAnchor(BlockPos pos) {
        if (anchors.remove(pos.asLong()) != anchors.defaultReturnValue()) {
            markDirty();
        }
    }

    public boolean anchored(long chunkKey) {
        int cx = ChunkPos.getPackedX(chunkKey);
        int cz = ChunkPos.getPackedZ(chunkKey);
        for (var e : anchors.long2IntEntrySet()) {
            BlockPos p = BlockPos.fromLong(e.getLongKey());
            int r = e.getIntValue();
            if (Math.abs((p.getX() >> 4) - cx) <= r && Math.abs((p.getZ() >> 4) - cz) <= r) {
                return true;
            }
        }
        return false;
    }

    public int anchorCount() {
        return anchors.size();
    }

    // ---- the Crash ----

    public void crash(long chunkKey) {
        if (crashed.add(chunkKey)) {
            setIntegrity(chunkKey, 0);
        }
    }

    public boolean crashed(long chunkKey) {
        return crashed.contains(chunkKey);
    }

    // ---- quiet and eaters ----

    public void setQuiet(BlockPos pos, boolean on) {
        if (on ? quiet.add(pos.asLong()) : quiet.remove(pos.asLong())) {
            markDirty();
        }
    }

    public boolean silenced(double x, double y, double z, double radius) {
        double r2 = radius * radius;
        for (long l : quiet) {
            BlockPos p = BlockPos.fromLong(l);
            double dx = p.getX() + 0.5 - x;
            double dy = p.getY() + 0.5 - y;
            double dz = p.getZ() + 0.5 - z;
            if (dx * dx + dy * dy + dz * dz <= r2) {
                return true;
            }
        }
        return false;
    }

    public void setEater(BlockPos pos, boolean on) {
        if (on ? eaters.add(pos.asLong()) : eaters.remove(pos.asLong())) {
            markDirty();
        }
    }

    public boolean eaten(BlockPos at, double radius) {
        double r2 = radius * radius;
        for (long l : eaters) {
            if (BlockPos.fromLong(l).getSquaredDistance(at) <= r2) {
                return true;
            }
        }
        return false;
    }

    // ---- block memories ----

    public void placed(BlockPos pos) {
        ChunkRecord r = chunk(ChunkPos.toLong(pos));
        if (r.placed.size() < PLACED_CAP) {
            r.placed.add(pos.asLong());
            markDirty();
        }
    }

    public boolean wasPlaced(BlockPos pos) {
        ChunkRecord r = chunks.get(ChunkPos.toLong(pos));
        return r != null && r.placed.contains(pos.asLong());
    }

    /** A player-placed block was broken: remember it as part of a ruin. */
    public void demolished(BlockPos pos, BlockState state, long tick) {
        ChunkRecord r = chunk(ChunkPos.toLong(pos));
        r.placed.remove(pos.asLong());
        r.ruins.add(new Remembered(pos.asLong(), state, tick));
        if (r.ruins.size() > RUIN_CAP) {
            r.ruins.remove(0);
        }
        markDirty();
    }

    /** A natural block was mined: remember it so the world can put it back. */
    public void mined(BlockPos pos, BlockState state, long tick) {
        ChunkRecord r = chunk(ChunkPos.toLong(pos));
        r.mined.add(new Remembered(pos.asLong(), state, tick));
        if (r.mined.size() > MINED_CAP) {
            r.mined.remove(0);
        }
        markDirty();
    }

    public List<Long> knownChunks() {
        return new ArrayList<>(chunks.keySet());
    }

    public record Remembered(long pos, BlockState state, long tick) {
        public BlockPos blockPos() {
            return BlockPos.fromLong(pos);
        }
    }

    /** Mutable per-chunk data. Call {@link #markDirty()} on the region after changing it. */
    public static final class ChunkRecord {
        public int integrity = -1;
        public int paradox;
        public final LongOpenHashSet placed = new LongOpenHashSet();
        public final List<Remembered> ruins = new ArrayList<>();
        public final List<Remembered> mined = new ArrayList<>();
        public int logsBroken;
        public int saplingsPlanted;
        public int slept;
        public boolean grudge;
        public boolean memorial;
        /** In a nested depth: whether this chunk's copy errors have been applied. */
        public boolean copied;
        public long lastAnomaly;
    }

    // ---- persistence ----

    private NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        NbtList list = new NbtList();
        for (var e : chunks.long2ObjectEntrySet()) {
            ChunkRecord r = e.getValue();
            NbtCompound c = new NbtCompound();
            c.putLong("k", e.getLongKey());
            c.putInt("i", r.integrity);
            c.putInt("p", r.paradox);
            c.putLongArray("pl", r.placed.toLongArray());
            c.put("ru", writeRemembered(r.ruins));
            c.put("mi", writeRemembered(r.mined));
            c.putInt("lb", r.logsBroken);
            c.putInt("sp", r.saplingsPlanted);
            c.putInt("sl", r.slept);
            c.putBoolean("g", r.grudge);
            c.putBoolean("m", r.memorial);
            c.putBoolean("c", r.copied);
            list.add(c);
        }
        nbt.put("chunks", list);
        NbtList anchorList = new NbtList();
        for (var e : anchors.long2IntEntrySet()) {
            NbtCompound c = new NbtCompound();
            c.putLong("p", e.getLongKey());
            c.putInt("r", e.getIntValue());
            anchorList.add(c);
        }
        nbt.put("anchors", anchorList);
        nbt.putLongArray("quiet", quiet.toLongArray());
        nbt.putLongArray("crashed", crashed.toLongArray());
        nbt.putLongArray("eaters", eaters.toLongArray());
        return nbt;
    }

    private static NbtList writeRemembered(List<Remembered> list) {
        NbtList out = new NbtList();
        for (Remembered m : list) {
            NbtCompound c = new NbtCompound();
            c.putLong("p", m.pos());
            c.put("s", NbtHelper.fromBlockState(m.state()));
            c.putLong("t", m.tick());
            out.add(c);
        }
        return out;
    }

    private static RegionMemory fromNbt(NbtCompound nbt) {
        RegionMemory m = new RegionMemory();
        for (NbtElement el : nbt.getListOrEmpty("chunks")) {
            if (!(el instanceof NbtCompound c)) {
                continue;
            }
            ChunkRecord r = new ChunkRecord();
            r.integrity = c.getInt("i", -1);
            r.paradox = c.getInt("p", 0);
            c.getLongArray("pl").ifPresent(a -> {
                for (long l : a) {
                    r.placed.add(l);
                }
            });
            readRemembered(c.getListOrEmpty("ru"), r.ruins);
            readRemembered(c.getListOrEmpty("mi"), r.mined);
            r.logsBroken = c.getInt("lb", 0);
            r.saplingsPlanted = c.getInt("sp", 0);
            r.slept = c.getInt("sl", 0);
            r.grudge = c.getBoolean("g", false);
            r.memorial = c.getBoolean("m", false);
            r.copied = c.getBoolean("c", false);
            m.chunks.put(c.getLong("k", 0), r);
        }
        for (NbtElement el : nbt.getListOrEmpty("anchors")) {
            if (el instanceof NbtCompound c) {
                m.anchors.put(c.getLong("p", 0), c.getInt("r", 1));
            }
        }
        nbt.getLongArray("crashed").ifPresent(a -> {
            for (long l : a) {
                m.crashed.add(l);
            }
        });
        nbt.getLongArray("quiet").ifPresent(a -> {
            for (long l : a) {
                m.quiet.add(l);
            }
        });
        nbt.getLongArray("eaters").ifPresent(a -> {
            for (long l : a) {
                m.eaters.add(l);
            }
        });
        return m;
    }

    private static void readRemembered(NbtList list, List<Remembered> out) {
        for (NbtElement el : list) {
            if (el instanceof NbtCompound c) {
                BlockState state = BlockStates.read(c.getCompoundOrEmpty("s"));
                if (state != null) {
                    out.add(new Remembered(c.getLong("p", 0), state, c.getLong("t", 0)));
                }
            }
        }
    }

    /** Reads block states from NBT using the static block registry. */
    static final class BlockStates {
        private BlockStates() {
        }

        static BlockState read(NbtCompound nbt) {
            if (nbt.isEmpty()) {
                return null;
            }
            return NbtHelper.toBlockState(net.minecraft.registry.Registries.BLOCK, nbt);
        }
    }
}
