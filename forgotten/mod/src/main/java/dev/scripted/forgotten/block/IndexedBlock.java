package dev.scripted.forgotten.block;

import com.mojang.serialization.MapCodec;
import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.world.RegionMemory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A block whose presence changes the rules around it, so it registers itself in its
 * dimension's region memory while it exists: Reality Anchors, Quiet, Possibility Eaters.
 * Registration happens in onBlockAdded/onStateReplaced, so it holds however the block got there.
 */
public class IndexedBlock extends Block {
    public enum Kind {
        /** Pins integrity at 100 for chunks within the radius. */
        ANCHOR,
        /** Silences sounds within 16 blocks. */
        QUIET,
        /** Eats unrealised outcomes: hostile spawns and explosions within 12 blocks never happen. */
        EATER
    }

    private final Kind kind;
    private final int radius;

    public IndexedBlock(Settings settings, Kind kind, int radius) {
        super(settings);
        this.kind = kind;
        this.radius = radius;
    }

    public static IndexedBlock anchor(Settings s) {
        return new IndexedBlock(s, Kind.ANCHOR, 2);
    }

    public static IndexedBlock quiet(Settings s) {
        return new IndexedBlock(s, Kind.QUIET, 16);
    }

    public static IndexedBlock eater(Settings s) {
        return new IndexedBlock(s, Kind.EATER, 12);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return switch (kind) {
            case ANCHOR -> createCodec(IndexedBlock::anchor);
            case QUIET -> createCodec(IndexedBlock::quiet);
            case EATER -> createCodec(IndexedBlock::eater);
        };
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (world instanceof ServerWorld server && !oldState.isOf(this)) {
            RegionMemory region = Reality.region(server);
            switch (kind) {
                case ANCHOR -> {
                    region.addAnchor(pos, radius);
                    Reality.setIntegrity(server, pos, 100);
                }
                case QUIET -> region.setQuiet(pos, true);
                case EATER -> region.setEater(pos, true);
            }
        }
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (!world.getBlockState(pos).isOf(this)) {
            RegionMemory region = Reality.region(world);
            switch (kind) {
                case ANCHOR -> region.removeAnchor(pos);
                case QUIET -> region.setQuiet(pos, false);
                case EATER -> region.setEater(pos, false);
            }
        }
        super.onStateReplaced(state, world, pos, moved);
    }
}
