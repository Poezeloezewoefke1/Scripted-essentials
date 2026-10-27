package dev.scripted.forgotten.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.scripted.forgotten.Forgotten;
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
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;

import java.util.List;

import static dev.scripted.forgotten.gametest.ForgottenGameTests.check;

/** The dimensions and the ways between them. */
public final class WorldsGameTests {
    /**
     * The gametest server only creates the vanilla worlds, so the mod's dimensions cannot be
     * entered here. Instead every dimension file is parsed with the game's own codec against
     * the live registries - the same parse a real server does when it loads them - and the
     * dimension types and noise settings must be present in their registries.
     */
    @GameTest
    public void everyDimensionParses(TestContext ctx) {
        MinecraftServer server = ctx.getWorld().getServer();
        var ops = RegistryOps.of(JsonOps.INSTANCE, server.getRegistryManager());
        var types = server.getRegistryManager().getOrThrow(RegistryKeys.DIMENSION_TYPE);
        var noise = server.getRegistryManager().getOrThrow(RegistryKeys.CHUNK_GENERATOR_SETTINGS);
        List<RegistryKey<World>> keys = new java.util.ArrayList<>(List.of(Dimensions.BACKSIDE, Dimensions.UNWRITTEN,
                Dimensions.STACK, Dimensions.ARCHIVE, Dimensions.UNDERDREAM, Dimensions.FIRST_WORLD));
        for (int d = 1; d <= Depths.MAX; d++) {
            keys.add(Dimensions.depth(d));
        }
        for (RegistryKey<World> key : keys) {
            Identifier id = key.getValue();
            check(ctx, types.containsId(id), "dimension type " + id + " is not registered");
            Identifier file = Identifier.of(id.getNamespace(), "dimension/" + id.getPath() + ".json");
            var resource = server.getResourceManager().getResource(file);
            check(ctx, resource.isPresent(), "missing " + file);
            try (var reader = resource.get().getReader()) {
                var parsed = DimensionOptions.CODEC.parse(ops, JsonParser.parseReader(reader));
                check(ctx, parsed.isSuccess(), id + " does not parse: " + parsed.error().map(e -> e.message()).orElse(""));
            } catch (java.io.IOException e) {
                check(ctx, false, "could not read " + file + ": " + e);
            }
        }
        check(ctx, noise.containsId(Forgotten.id("stack")) && noise.containsId(Forgotten.id("underdream")), "noise settings");
        ctx.complete();
    }

    @GameTest
    public void copyErrorsGrowWithDepth(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        ChunkPos chunk = new ChunkPos(ctx.getAbsolutePos(BlockPos.ORIGIN));
        int changed = Nesting.applyCopyErrors(world, chunk, Depths.MAX);
        check(ctx, changed > 20, "about a third of the Scratch Layer's surface should be wrong, got " + changed);
        ctx.complete();
    }

    @GameTest
    public void riftsAlwaysLeaveSomewhereToStand(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        BlockPos abs = ctx.getAbsolutePos(new BlockPos(3, 1, 3));
        Vec3d at = Rifts.landing(world, abs.getX(), abs.getZ(), abs.getY() + 40, ModBlocks.DATA_BLOCK.getDefaultState());
        check(ctx, !world.getBlockState(BlockPos.ofFloored(at).down()).isAir(), "ground under the arrival point");
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
