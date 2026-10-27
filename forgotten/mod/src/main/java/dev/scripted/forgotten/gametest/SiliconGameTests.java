package dev.scripted.forgotten.gametest;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.block.entity.CompilerChestBlockEntity;
import dev.scripted.forgotten.item.GitResetItem;
import dev.scripted.forgotten.logic.bio.Program;
import dev.scripted.forgotten.logic.bio.ProgramCompiler;
import dev.scripted.forgotten.registry.ModAttachments;
import dev.scripted.forgotten.registry.ModBlocks;
import dev.scripted.forgotten.registry.ModComponents;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.system.BioNodes;
import dev.scripted.forgotten.system.Draft;
import dev.scripted.forgotten.system.SunDogs;
import dev.scripted.forgotten.system.Unraveling;
import dev.scripted.forgotten.world.CommitStore;
import dev.scripted.forgotten.world.RegionMemory;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.Arrays;

import static dev.scripted.forgotten.gametest.ForgottenGameTests.check;
import static dev.scripted.forgotten.gametest.ForgottenGameTests.player;

/** Silicon and Spores, and the Unmaker's tools, exercised on a real server. */
public final class SiliconGameTests {
    static String[][] rows(String... firstItems) {
        String[][] out = new String[firstItems.length][9];
        for (int r = 0; r < firstItems.length; r++) {
            Arrays.fill(out[r], "");
            out[r][0] = firstItems[r];
        }
        return out;
    }

    static int[][] ones(int rows) {
        int[][] out = new int[rows][9];
        for (int[] r : out) {
            Arrays.fill(r, 1);
        }
        return out;
    }

    @GameTest
    public void compilerChestCompilesACartridge(TestContext ctx) {
        BlockPos rel = new BlockPos(1, 1, 1);
        ctx.setBlockState(rel, ModBlocks.COMPILER_CHEST);
        BlockPos abs = ctx.getAbsolutePos(rel);
        CompilerChestBlockEntity chest = (CompilerChestBlockEntity) ctx.getWorld().getBlockEntity(abs);
        check(ctx, chest != null, "compiler chest block entity");
        chest.code.setStack(0, new ItemStack(Items.COMPASS));
        chest.code.setStack(1, new ItemStack(Items.WHEAT));
        chest.code.setStack(9, new ItemStack(Items.IRON_HOE));
        chest.code.setStack(18, new ItemStack(Items.HOPPER));
        chest.code.setStack(27, new ItemStack(Items.BARRIER));
        chest.code.setStack(CompilerChestBlockEntity.MEDIUM, new ItemStack(ModItems.SILICON_WAFER));
        FakePlayer p = player(ctx, 0);
        BioNodes.compile(ctx.getWorld(), abs, chest, p);
        ItemStack out = chest.code.getStack(CompilerChestBlockEntity.MEDIUM);
        check(ctx, out.isOf(ModItems.CARTRIDGE), "a cartridge in the last slot, found " + out);
        Program program = Program.parse(out.get(ModComponents.PROGRAM));
        check(ctx, program.size() == 4, "four lines, found " + program.size());
        check(ctx, !chest.code.getStack(0).isEmpty(), "source code is kept");
        ctx.complete();
    }

    @GameTest
    public void syntaxErrorsSummonANullPointerException(TestContext ctx) {
        BlockPos rel = new BlockPos(2, 1, 2);
        ctx.setBlockState(rel, ModBlocks.COMPILER_CHEST);
        BlockPos abs = ctx.getAbsolutePos(rel);
        CompilerChestBlockEntity chest = (CompilerChestBlockEntity) ctx.getWorld().getBlockEntity(abs);
        chest.code.setStack(0, new ItemStack(Items.DIRT));
        chest.code.setStack(CompilerChestBlockEntity.MEDIUM, new ItemStack(ModItems.SILICON_WAFER));
        BioNodes.compile(ctx.getWorld(), abs, chest, player(ctx, 0));
        check(ctx, chest.code.getStack(CompilerChestBlockEntity.MEDIUM).isEmpty(), "the wafer is spent");
        check(ctx, !ctx.getWorld().getEntitiesByClass(VexEntity.class, new net.minecraft.util.math.Box(abs).expand(3),
                v -> v.hasCustomName()).isEmpty(), "a NullPointerException appeared");
        ctx.killAllEntities();
        ctx.complete();
    }

    @GameTest
    public void tightLoopsCompileToAnInfiniteLoop(TestContext ctx) {
        ProgramCompiler.Result r = ProgramCompiler.compile(rows("minecraft:note_block", "minecraft:repeater"), ones(2));
        check(ctx, r.ok() && r.program().tightLoop(), "note block then jump-to-1 is a tight loop");
        ctx.complete();
    }

    @GameTest(maxTicks = 200)
    public void bioNodeHarvestsAndDeposits(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        ctx.setBlockState(new BlockPos(3, 1, 2), Blocks.FARMLAND);
        ctx.setBlockState(new BlockPos(3, 2, 2), Blocks.WHEAT.getDefaultState().with(CropBlock.AGE, CropBlock.MAX_AGE));
        ctx.setBlockState(new BlockPos(1, 1, 2), Blocks.CHEST);
        CowEntity cow = ctx.spawnEntity(EntityType.COW, new BlockPos(2, 1, 2));
        FakePlayer p = player(ctx, 0);
        ProgramCompiler.Result compiled = ProgramCompiler.compile(
                rows("minecraft:iron_hoe", "minecraft:hopper", "minecraft:barrier"), ones(3));
        check(ctx, compiled.ok(), "program compiles");
        BioNodes.splice(cow, p);
        BioNodes.install(cow, compiled.program().serialize());
        check(ctx, BioNodes.isNode(cow), "the cow is a Bio-Node");
        BlockPos chestPos = ctx.getAbsolutePos(new BlockPos(1, 1, 2));
        ctx.addFinalTask(() -> {
            ChestBlockEntity chest = (ChestBlockEntity) world.getBlockEntity(chestPos);
            check(ctx, chest != null && chest.count(Items.WHEAT) >= 1, "wheat deposited in the chest");
            check(ctx, world.getBlockState(ctx.getAbsolutePos(new BlockPos(3, 2, 2))).isOf(Blocks.WHEAT), "replanted");
            check(ctx, cow.getAttached(ModAttachments.BIO_STATE).endsWith("true"), "the program halted");
        });
    }

    @GameTest
    public void gitResetRollsTheChunkBack(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        BlockPos a = ctx.getAbsolutePos(new BlockPos(2, 1, 2));
        ctx.setBlockState(new BlockPos(2, 1, 2), Blocks.OAK_PLANKS);
        CommitStore.Commit commit = GitResetItem.commit(world, a);
        ctx.setBlockState(new BlockPos(2, 1, 2), Blocks.AIR);
        ctx.setBlockState(new BlockPos(3, 1, 3), Blocks.TNT);
        int changed = GitResetItem.reset(world, new ChunkPos(a), commit);
        check(ctx, changed >= 2, "at least the two edits rolled back, got " + changed);
        check(ctx, world.getBlockState(a).isOf(Blocks.OAK_PLANKS), "planks restored");
        check(ctx, !world.getBlockState(ctx.getAbsolutePos(new BlockPos(3, 1, 3))).isOf(Blocks.TNT), "tnt reverted");
        ctx.complete();
    }

    @GameTest
    public void commitsSurviveSaving(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        CommitStore store = new CommitStore();
        BlockPos a = ctx.getAbsolutePos(new BlockPos(1, 1, 1));
        ctx.setBlockState(new BlockPos(1, 1, 1), Blocks.GLOWSTONE);
        store.put(42L, GitResetItem.commit(world, a));
        var encoded = CommitStore.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, store).getOrThrow();
        CommitStore back = CommitStore.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, encoded).getOrThrow();
        CommitStore.Commit c = back.get(42L);
        check(ctx, c != null, "commit read back");
        check(ctx, c.palette().stream().anyMatch(s -> s.isOf(Blocks.GLOWSTONE)), "palette kept glowstone");
        ctx.complete();
    }

    @GameTest
    public void editorRewritesASurfaceInsideOneChunk(TestContext ctx) {
        for (int x = 1; x <= 3; x++) {
            ctx.setBlockState(new BlockPos(x, 1, 1), Blocks.STONE_BRICKS);
        }
        int n = Unraveling.rewriteSurface(ctx.getWorld(), ctx.getAbsolutePos(new BlockPos(1, 1, 1)),
                Blocks.GLASS.getDefaultState(), 2);
        check(ctx, n == 2, "limited to two, got " + n);
        check(ctx, ctx.getBlockState(new BlockPos(1, 1, 1)).isOf(Blocks.GLASS), "pointed-at block is glass");
        ctx.complete();
    }

    @GameTest
    public void crashedChunksStayUnmade(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        RegionMemory region = Reality.region(world);
        long far = ChunkPos.toLong(900_001, 900_001);
        region.crash(far);
        check(ctx, region.crashed(far), "crashed");
        region.setIntegrity(far, 100);
        check(ctx, region.integrity(world, far) == 0, "a crashed chunk cannot be restored");
        ctx.complete();
    }

    @GameTest
    public void theDraftSellsImpossibleThings(TestContext ctx) {
        WanderingTraderEntity trader = Draft.spawn(ctx.getWorld(), ctx.getAbsolutePos(new BlockPos(2, 1, 2)));
        check(ctx, trader != null, "trader spawned");
        check(ctx, trader.getOffers().stream().anyMatch(o -> o.getSellItem().isOf(ModItems.BLOCK_OF_AIR)), "sells a block of air");
        check(ctx, trader.getOffers().stream().anyMatch(o -> o.getSellItem().isOf(ModItems.ONE_GRAVITY)), "sells one gravity");
        ctx.killAllEntities();
        ctx.complete();
    }

    @GameTest
    public void sunDogsCannotBeHurt(TestContext ctx) {
        var dog = ctx.spawnEntity(EntityType.BLAZE, new BlockPos(2, 2, 2));
        dog.addCommandTag(SunDogs.TAG);
        ZombieEntity attacker = ctx.spawnEntity(EntityType.ZOMBIE, new BlockPos(4, 1, 4));
        float before = dog.getHealth();
        dog.damage(ctx.getWorld(), ctx.getWorld().getDamageSources().mobAttack(attacker), 5f);
        check(ctx, dog.getHealth() == before, "the Sun Dog took no damage");
        ctx.killAllEntities();
        ctx.complete();
    }

    @GameTest
    public void nanArmorSometimesResolvesToNothing(TestContext ctx) {
        ZombieEntity wearer = ctx.spawnEntity(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        wearer.equipStack(net.minecraft.entity.EquipmentSlot.CHEST, new ItemStack(ModItems.NAN_CHESTPLATE));
        int ignored = 0;
        for (int i = 0; i < 40; i++) {
            float before = wearer.getHealth();
            wearer.timeUntilRegen = 0;
            wearer.damage(ctx.getWorld(), ctx.getWorld().getDamageSources().generic(), 0.01f);
            if (wearer.getHealth() == before) {
                ignored++;
            }
        }
        check(ctx, ignored > 0 && ignored < 40, "some hits resolve to NaN, some do not: " + ignored + "/40");
        ctx.killAllEntities();
        ctx.complete();
    }
}
