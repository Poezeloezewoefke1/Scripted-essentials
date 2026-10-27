package dev.scripted.forgotten.gametest;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.dimension.Dimensions;
import dev.scripted.forgotten.logic.nesting.Depths;
import dev.scripted.forgotten.registry.ModBlocks;
import dev.scripted.forgotten.system.Nesting;
import dev.scripted.forgotten.system.Rifts;
import dev.scripted.forgotten.world.WorldMemory;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

import static dev.scripted.forgotten.gametest.ForgottenGameTests.check;

/** The dimensions and the ways between them. */
public final class WorldsGameTests {
    @GameTest
    public void everyDimensionLoads(TestContext ctx) {
        MinecraftServer server = ctx.getWorld().getServer();
        List<RegistryKey<World>> keys = new java.util.ArrayList<>(List.of(Dimensions.BACKSIDE, Dimensions.UNWRITTEN,
                Dimensions.STACK, Dimensions.ARCHIVE, Dimensions.UNDERDREAM, Dimensions.FIRST_WORLD));
        for (int d = 1; d <= Depths.MAX; d++) {
            keys.add(Dimensions.depth(d));
        }
        for (RegistryKey<World> key : keys) {
            check(ctx, server.getWorld(key) != null, "dimension " + key.getValue() + " did not load");
        }
        check(ctx, Dimensions.depthOf(server.getWorld(Dimensions.depth(3))) == 3, "depth_3 knows it is depth 3");
        check(ctx, Dimensions.baselineIntegrity(server.getWorld(Dimensions.UNWRITTEN)) == 0, "the Unwritten starts unwritten");
        ctx.complete();
    }

    @GameTest(maxTicks = 400)
    public void theScratchLayerIsFullOfMistakes(TestContext ctx) {
        ServerWorld scratch = ctx.getWorld().getServer().getWorld(Dimensions.depth(Depths.MAX));
        ChunkPos chunk = new ChunkPos(64, 64);
        scratch.getChunk(chunk.x, chunk.z);
        int changed = Nesting.applyCopyErrors(scratch, chunk, Depths.MAX);
        check(ctx, changed > 20, "about a third of the surface should be wrong, got " + changed);
        ctx.complete();
    }

    @GameTest(maxTicks = 400)
    public void theStackAlwaysHasSomewhereToStand(TestContext ctx) {
        ServerWorld stack = ctx.getWorld().getServer().getWorld(Dimensions.STACK);
        Vec3d at = Rifts.landing(stack, 5000, 5000, 120, ModBlocks.DATA_BLOCK.getDefaultState());
        check(ctx, !stack.getBlockState(BlockPos.ofFloored(at).down()).isAir(), "ground under the arrival point");
        ctx.complete();
    }

    @GameTest
    public void frameAnchorsAreRemembered(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        BlockPos rel = new BlockPos(2, 1, 2);
        ctx.setBlockState(rel, ModBlocks.FRAME_ANCHOR);
        BlockPos abs = ctx.getAbsolutePos(rel);
        WorldMemory.FrameRecord frame = Nesting.register(world, abs);
        WorldMemory memory = Reality.memory(world.getServer());
        check(ctx, memory.frames.containsKey(frame.id()), "registered");
        check(ctx, Nesting.register(world, abs).id() == frame.id(), "registering twice keeps one id");
        ctx.setBlockState(rel, Blocks.AIR);
        check(ctx, !memory.frames.containsKey(frame.id()), "breaking the anchor forgets it");
        ctx.complete();
    }
}
