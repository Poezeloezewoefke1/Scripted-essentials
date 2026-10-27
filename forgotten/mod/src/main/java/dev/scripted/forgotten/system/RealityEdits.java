package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.logic.paradox.Insight;
import dev.scripted.forgotten.player.PlayerMemory;
import dev.scripted.forgotten.player.RealityEdit;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.RegionMemory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Large reality edits applied with Rule Scrolls, and how the world takes them back when a
 * Correction wins.
 */
public final class RealityEdits {
    public static final int LIFT_HEIGHT = 12;
    public static final int LIFT_LIMIT = 2048;
    public static final int NATURAL_LIFT_LIMIT = 384;
    public static final int WATER_LIMIT = 3000;
    public static final int SKY_HEIGHT = 32;

    private RealityEdits() {
    }

    public static boolean apply(String rule, ServerPlayerEntity player, ServerWorld world, BlockPos target) {
        return switch (rule) {
            case "lift" -> lift(player, world, target);
            case "sky_water" -> skyWater(player, world, target);
            default -> false;
        };
    }

    /**
     * Stone remembers falling upward. Lifts the structure the target block belongs to: the
     * connected player-built blocks if it is a build, or a small natural outcrop otherwise.
     */
    public static boolean lift(ServerPlayerEntity player, ServerWorld world, BlockPos origin) {
        RegionMemory region = Reality.region(world);
        boolean built = region.wasPlaced(origin);
        int limit = built ? LIFT_LIMIT : NATURAL_LIFT_LIMIT;
        List<BlockPos> body = flood(world, origin, limit, p -> {
            BlockState s = world.getBlockState(p);
            if (s.isAir() || s.hasBlockEntity() || s.getHardness(world, p) < 0 || !s.getFluidState().isEmpty()) {
                return false;
            }
            return !built || region.wasPlaced(p);
        });
        if (body == null) {
            Texts.tell(player, Texts.world("It is too much for one memory. The ground does not want to let go."));
            return false;
        }
        for (BlockPos p : body) {
            if (!world.getBlockState(p.up(LIFT_HEIGHT)).isAir() && !body.contains(p.up(LIFT_HEIGHT))) {
                Texts.tell(player, Texts.world("There is something in the way above it."));
                return false;
            }
        }
        body.sort(Comparator.comparingInt(BlockPos::getY).reversed());
        List<BlockState> states = new ArrayList<>();
        for (BlockPos p : body) {
            states.add(world.getBlockState(p));
        }
        for (int i = 0; i < body.size(); i++) {
            BlockPos to = body.get(i).up(LIFT_HEIGHT);
            world.setBlockState(to, states.get(i), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
            if (built) {
                region.placed(to);
            }
        }
        Set<BlockPos> lifted = new HashSet<>();
        for (BlockPos p : body) {
            lifted.add(p.up(LIFT_HEIGHT));
        }
        for (BlockPos p : body) {
            if (!lifted.contains(p)) {
                world.setBlockState(p, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
            }
        }
        record(player, world, RealityEdit.Kind.LIFT, origin, LIFT_HEIGHT, body.size());
        world.playSound(null, origin.getX(), origin.getY(), origin.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE,
                SoundCategory.BLOCKS, 1.0f, 0.5f);
        Texts.tell(player, Texts.world("The stone remembers falling upward. " + body.size() + " blocks forget the ground."));
        return true;
    }

    /** Water remembers being a cloud: the whole lake rises into a floating sphere. */
    public static boolean skyWater(ServerPlayerEntity player, ServerWorld world, BlockPos origin) {
        BlockPos start = origin;
        if (!world.getFluidState(start).isOf(Fluids.WATER)) {
            start = origin.up();
            if (!world.getFluidState(start).isOf(Fluids.WATER)) {
                Texts.tell(player, Texts.world("This is not water that remembers anything."));
                return false;
            }
        }
        List<BlockPos> water = flood(world, start, WATER_LIMIT, p -> world.getFluidState(p).isOf(Fluids.WATER)
                && world.getFluidState(p).isStill() && world.getBlockState(p).isOf(Blocks.WATER));
        if (water == null) {
            Texts.tell(player, Texts.world("It is an ocean. An ocean will not rise for you."));
            return false;
        }
        int radius = (int) Math.max(2, Math.min(10, Math.round(Math.cbrt(3.0 * water.size() / (4.0 * Math.PI)))));
        int topY = water.stream().mapToInt(BlockPos::getY).max().orElse(origin.getY());
        BlockPos center = new BlockPos(origin.getX(), Math.min(world.getTopYInclusive() - radius - 2, topY + SKY_HEIGHT), origin.getZ());
        Box lake = new Box(start).expand(24, 8, 24);
        List<Entity> fish = world.getEntitiesByClass(WaterCreatureEntity.class, lake, e -> true).stream().map(e -> (Entity) e).toList();
        for (BlockPos p : water) {
            world.setBlockState(p, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
        for (BlockPos p : BlockPos.iterate(center.add(-radius, -radius, -radius), center.add(radius, radius, radius))) {
            if (p.getSquaredDistance(center) <= radius * radius && world.getBlockState(p).isAir()) {
                world.setBlockState(p, Blocks.WATER.getDefaultState(), Block.NOTIFY_ALL);
            }
        }
        for (Entity e : fish) {
            e.refreshPositionAndAngles(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, e.getYaw(), e.getPitch());
        }
        record(player, world, RealityEdit.Kind.SKY_WATER, center, radius, water.size());
        world.spawnParticles(ParticleTypes.SPLASH, center.getX(), center.getY(), center.getZ(), 200, radius, radius, radius, 0.2);
        Texts.tell(player, Texts.world("The water remembers being a cloud. It goes home."));
        return true;
    }

    /**
     * Takes back an edit. Called when a Correction defeats its player, and by the Preserve
     * ending. Lifted structures fall; floating water collapses into rain.
     */
    public static void revoke(ServerWorld world, RealityEdit edit) {
        BlockPos pos = BlockPos.fromLong(edit.pos());
        switch (edit.kind()) {
            case LIFT -> {
                BlockPos base = pos.up(edit.data());
                for (BlockPos p : BlockPos.iterate(base.add(-12, -2, -12), base.add(12, 24, 12))) {
                    BlockState s = world.getBlockState(p);
                    if (!s.isAir() && Reality.region(world).wasPlaced(p) && world.getBlockState(p.down()).isAir()) {
                        FallingBlockEntity.spawnFromBlock(world, p.toImmutable(), s);
                    }
                }
            }
            case SKY_WATER -> {
                int r = edit.data();
                for (BlockPos p : BlockPos.iterate(pos.add(-r, -r, -r), pos.add(r, r, r))) {
                    if (world.getBlockState(p).isOf(Blocks.WATER)) {
                        world.setBlockState(p, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                    }
                }
                world.setWeather(0, 2400, true, false);
            }
            case ANCHOR, CHAMBER -> world.breakBlock(pos, true);
            case REWRITE -> world.setBlockState(pos, Blocks.STONE.getDefaultState());
            case DEATHLESS -> {
                // Handled by the Correction: it takes the immunity for itself.
            }
        }
    }

    public static ServerWorld worldOf(ServerWorld any, String dimension) {
        RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(dimension));
        return any.getServer().getWorld(key);
    }

    static void record(ServerPlayerEntity player, ServerWorld world, RealityEdit.Kind kind, BlockPos pos, int data, int size) {
        PlayerMemory memory = Reality.player(player);
        memory.addEdit(new RealityEdit(kind, world.getRegistryKey().getValue().toString(), pos.asLong(), data, true));
        Reality.paradox(world, pos, Math.min(40, 5 + size / 50));
        Reality.insight(player, Insight.REALITY_EDIT);
        Reality.chronicle(world, ChronicleEvent.REALITY_EDIT, pos, player.getName().getString(), kind.name());
    }

    /** Breadth-first fill of connected positions matching a test. Null if it exceeds the limit. */
    static List<BlockPos> flood(ServerWorld world, BlockPos start, int limit, java.util.function.Predicate<BlockPos> test) {
        if (!test.test(start)) {
            return List.of();
        }
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.toImmutable());
        seen.add(start.toImmutable());
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            out.add(p);
            if (out.size() > limit) {
                return null;
            }
            for (Direction d : Direction.values()) {
                BlockPos n = p.offset(d);
                if (seen.add(n) && world.isChunkLoaded(n) && test.test(n)) {
                    queue.add(n);
                }
            }
        }
        return out;
    }
}
