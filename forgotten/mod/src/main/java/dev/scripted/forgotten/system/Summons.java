package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.dimension.Dimensions;
import dev.scripted.forgotten.logic.chronicle.ChronicleEntry;
import dev.scripted.forgotten.logic.integrity.IntegrityStage;
import dev.scripted.forgotten.util.Lore;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.WorldMemory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * How the bosses that are not ambushes are called: a clock turned at midnight in a broken place
 * calls the King of Yesterday, a Lens held to a cartography table where the map no longer agrees
 * with the land calls the Cartographer, and a Lens held to a written book on a lectern opens the
 * Archive, where the Curator keeps everything.
 */
public final class Summons {
    /** The Archive hall: centre, half-width, height. */
    public static final BlockPos HALL = new BlockPos(0, 100, 0);
    public static final int HALF = 12;
    public static final int HEIGHT = 8;

    private Summons() {
    }

    public static boolean useItem(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
        if (stack.isOf(Items.CLOCK) && world.getRegistryKey() == net.minecraft.world.World.OVERWORLD) {
            long time = world.getTimeOfDay() % 24000L;
            boolean midnight = time >= 17500 && time <= 18500;
            if (midnight && Reality.stage(world, player.getBlockPos()).atLeast(IntegrityStage.FRAGMENTATION)
                    && !Bosses.alive(Bosses.Kind.KING_OF_YESTERDAY)) {
                BlockPos at = Anomalies.surfaceNear(world, player.getBlockPos(), world.getRandom(), 10);
                if (at != null && Bosses.summon(Bosses.Kind.KING_OF_YESTERDAY, world, at) != null) {
                    stack.decrementUnlessCreative(1, player);
                    Texts.tell(player, Texts.world("You turned the hands back. Yesterday has come to collect."));
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean lensOn(ServerWorld world, ServerPlayerEntity player, BlockPos pos, BlockState state) {
        if (state.isOf(Blocks.CARTOGRAPHY_TABLE) && Reality.stage(world, pos).atLeast(IntegrityStage.CONTRADICTION)
                && !Bosses.alive(Bosses.Kind.CARTOGRAPHER)) {
            BlockPos at = Anomalies.surfaceNear(world, pos, world.getRandom(), 8);
            if (at != null && Bosses.summon(Bosses.Kind.CARTOGRAPHER, world, at) != null) {
                Texts.tell(player, Texts.world("The map on the table disagrees with the land. Someone has come to settle it."));
                return true;
            }
        }
        if (state.isOf(Blocks.LECTERN) && state.get(LecternBlock.HAS_BOOK)) {
            enterArchive(player);
            return true;
        }
        return false;
    }

    // ---- the Archive ----

    public static void enterArchive(ServerPlayerEntity player) {
        ServerWorld from = Reality.world(player);
        if (from.getRegistryKey() == Dimensions.ARCHIVE) {
            Rifts.goTo(player, Reality.player(player).returnPoint);
            return;
        }
        ServerWorld archive = from.getServer().getWorld(Dimensions.ARCHIVE);
        if (archive == null) {
            return;
        }
        Reality.player(player).returnPoint = Rifts.point(from, player.getEntityPos());
        WorldMemory memory = Reality.memory(from.getServer());
        if (memory.archivePos == Long.MIN_VALUE) {
            buildHall(archive, memory);
            memory.archivePos = HALL.asLong();
            memory.archiveDimension = Dimensions.ARCHIVE.getValue().toString();
            memory.markDirty();
        }
        Travel.teleport(player, archive, Vec3d.ofBottomCenter(HALL.add(0, 1, -HALF + 2)), 0f, 0f);
        Texts.tell(player, Texts.discovery("THE ARCHIVE"));
        Texts.tell(player, Texts.world("Everything that happened is kept here. The lecterns remember. Read one to leave."));
        Reality.advance(player, "the_archive");
        if (!memory.flag("defeated_curator") && !Bosses.alive(Bosses.Kind.CURATOR)) {
            Bosses.summon(Bosses.Kind.CURATOR, archive, HALL.up());
        }
    }

    /** A white hall with a lectern for each of the world's most significant events. */
    static void buildHall(ServerWorld world, WorldMemory memory) {
        BlockState floor = Blocks.WHITE_CONCRETE.getDefaultState();
        BlockState wall = Blocks.QUARTZ_BRICKS.getDefaultState();
        BlockState light = Blocks.SEA_LANTERN.getDefaultState();
        for (int x = -HALF; x <= HALF; x++) {
            for (int z = -HALF; z <= HALF; z++) {
                world.setBlockState(HALL.add(x, 0, z), (x % 6 == 0 && z % 6 == 0) ? light : floor, Block.NOTIFY_LISTENERS);
                world.setBlockState(HALL.add(x, HEIGHT, z), floor, Block.NOTIFY_LISTENERS);
                if (Math.abs(x) == HALF || Math.abs(z) == HALF) {
                    for (int y = 1; y < HEIGHT; y++) {
                        world.setBlockState(HALL.add(x, y, z), wall, Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }
        List<ChronicleEntry> events = memory.chronicle.highlights(12);
        List<ItemStack> books = new ArrayList<>();
        for (ChronicleEntry e : events) {
            String text = e.event().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ') + "\n\n"
                    + (e.actor().isEmpty() ? "" : e.actor() + "\n") + e.detail() + "\n\nat " + e.x() + ", " + e.y() + ", " + e.z()
                    + "\nin " + e.dimension() + "\nday " + (e.tick() / 24000L);
            books.add(Lore.book("Record " + (books.size() + 1), "The Archive", List.of(Text.literal(text))));
        }
        books.add(Lore.firstWorld());
        int i = 0;
        for (int x = -HALF + 3; x <= HALF - 3 && i < books.size(); x += 3) {
            for (int z : new int[]{-4, 4}) {
                if (i >= books.size()) {
                    break;
                }
                BlockPos p = HALL.add(x, 1, z);
                world.setBlockState(p, Blocks.LECTERN.getDefaultState().with(LecternBlock.FACING, z < 0 ? Direction.SOUTH : Direction.NORTH),
                        Block.NOTIFY_LISTENERS);
                if (world.getBlockEntity(p) instanceof LecternBlockEntity lectern) {
                    lectern.setBook(books.get(i++));
                    world.setBlockState(p, world.getBlockState(p).with(LecternBlock.HAS_BOOK, true), Block.NOTIFY_LISTENERS);
                }
            }
        }
    }
}
