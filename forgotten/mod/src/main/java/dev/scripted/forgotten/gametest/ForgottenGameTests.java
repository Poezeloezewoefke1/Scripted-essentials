package dev.scripted.forgotten.gametest;

import dev.scripted.forgotten.Forgotten;
import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.block.entity.ParadoxEngineBlockEntity;
import dev.scripted.forgotten.item.MemoryItem;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.logic.concept.Concept;
import dev.scripted.forgotten.logic.integrity.IntegrityStage;
import dev.scripted.forgotten.logic.language.Word;
import dev.scripted.forgotten.paradox.ParadoxEngineLogic;
import dev.scripted.forgotten.paradox.ParadoxRecipes;
import dev.scripted.forgotten.player.PlayerMemory;
import dev.scripted.forgotten.registry.ModBlocks;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.system.Anomalies;
import dev.scripted.forgotten.system.GlitchKind;
import dev.scripted.forgotten.system.Glitches;
import dev.scripted.forgotten.system.Language;
import dev.scripted.forgotten.system.RealityEdits;
import dev.scripted.forgotten.world.RegionMemory;
import dev.scripted.forgotten.world.WorldMemory;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

/**
 * Game tests run on a real dedicated server with the mod loaded (./gradlew :mod:runGametest).
 * They exercise the systems through the same code paths the game uses.
 */
public final class ForgottenGameTests {
    static void check(TestContext ctx, boolean condition, String message) {
        if (!condition) {
            ctx.throwGameTestException(Text.literal(message));
        }
    }

    static FakePlayer player(TestContext ctx, int insight) {
        FakePlayer p = FakePlayer.get(ctx.getWorld());
        PlayerMemory m = Reality.player(p);
        m.insight = insight;
        m.debt = 0;
        p.getInventory().clear();
        return p;
    }

    @GameTest
    public void everythingIsRegistered(TestContext ctx) {
        check(ctx, Registries.ITEM.getId(ModItems.LENS).equals(Forgotten.id("lens")), "lens");
        check(ctx, ModItems.CONCEPTS.size() == Concept.values().length, "one item per concept");
        for (Concept c : Concept.values()) {
            check(ctx, Registries.ITEM.containsId(Forgotten.id(c.itemId())), "missing " + c.itemId());
        }
        check(ctx, Registries.BLOCK.containsId(Forgotten.id("paradox_engine")), "paradox engine block");
        check(ctx, Registries.ITEM.containsId(Forgotten.id("paradox_engine")), "paradox engine item");
        ctx.complete();
    }

    @GameTest
    public void paradoxRecipesLoadFromData(TestContext ctx) {
        check(ctx, ParadoxRecipes.BOOK.size() >= 15, "expected the shipped Paradox recipes, found " + ParadoxRecipes.BOOK.size());
        ctx.complete();
    }

    @GameTest
    public void engineMakesASingularity(TestContext ctx) {
        BlockPos rel = new BlockPos(1, 1, 1);
        ctx.setBlockState(rel, ModBlocks.PARADOX_ENGINE);
        ParadoxEngineBlockEntity engine = (ParadoxEngineBlockEntity) ctx.getWorld().getBlockEntity(ctx.getAbsolutePos(rel));
        check(ctx, engine != null, "engine block entity");
        engine.inputs.setStack(0, new ItemStack(ModItems.concept(Concept.RECURSION)));
        engine.inputs.setStack(1, new ItemStack(ModItems.concept(Concept.GRAVITY)));
        FakePlayer low = player(ctx, 0);
        check(ctx, !ParadoxEngineLogic.plan(low, engine).ok(), "an Observer should not be able to make a singularity");
        FakePlayer architect = player(ctx, 500);
        check(ctx, ParadoxEngineLogic.contradict(architect, engine), "contradiction should succeed at Paradox Lord rank");
        check(ctx, architect.getInventory().count(ModBlocks.SINGULARITY.asItem()) == 1, "singularity in inventory");
        check(ctx, engine.inputs.getStack(0).isEmpty() && engine.inputs.getStack(1).isEmpty(), "inputs consumed");
        check(ctx, Reality.player(architect).debt > 0, "debt charged");
        ctx.complete();
    }

    @GameTest
    public void memoriesDistilIntoConcepts(TestContext ctx) {
        BlockPos rel = new BlockPos(1, 1, 1);
        ctx.setBlockState(rel, ModBlocks.PARADOX_ENGINE);
        ParadoxEngineBlockEntity engine = (ParadoxEngineBlockEntity) ctx.getWorld().getBlockEntity(ctx.getAbsolutePos(rel));
        engine.inputs.setStack(0, MemoryItem.of(ModItems.MEMORY, "minecraft:sand"));
        engine.inputs.setStack(1, new ItemStack(ModItems.PARADOX_FRAGMENT));
        FakePlayer p = player(ctx, 0);
        check(ctx, ParadoxEngineLogic.contradict(p, engine), "distil");
        check(ctx, p.getInventory().count(ModItems.concept(Concept.GRAVITY)) == 1, "sand remembers falling: Gravity");
        ctx.complete();
    }

    @GameTest
    public void anchorsHoldIntegrity(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        BlockPos abs = ctx.getAbsolutePos(new BlockPos(2, 1, 2));
        Reality.setIntegrity(world, abs, 30);
        check(ctx, Reality.stage(world, abs) == IntegrityStage.FRAGMENTATION, "stage from integrity");
        ctx.setBlockState(new BlockPos(2, 1, 2), ModBlocks.REALITY_ANCHOR);
        RegionMemory region = Reality.region(world);
        check(ctx, region.anchored(ChunkPos.toLong(abs)), "anchored");
        check(ctx, Reality.integrity(world, abs) == 100, "anchor restores integrity");
        ctx.setBlockState(new BlockPos(2, 1, 2), Blocks.AIR);
        check(ctx, !region.anchored(ChunkPos.toLong(abs)), "unanchored after removal");
        ctx.complete();
    }

    @GameTest
    public void furnaceForgetsItIsAFurnace(TestContext ctx) {
        BlockPos rel = new BlockPos(1, 1, 1);
        ctx.setBlockState(rel, Blocks.FURNACE);
        BlockPos abs = ctx.getAbsolutePos(rel);
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) ctx.getWorld().getBlockEntity(abs);
        furnace.setStack(0, new ItemStack(Items.RAW_IRON, 5));
        check(ctx, Anomalies.swapIdentity(ctx.getWorld(), abs), "swap happened");
        check(ctx, ctx.getWorld().getBlockState(abs).isOf(Blocks.CHEST), "now a chest");
        ChestBlockEntity chest = (ChestBlockEntity) ctx.getWorld().getBlockEntity(abs);
        check(ctx, chest.count(Items.RAW_IRON) == 5, "kept its contents");
        ctx.complete();
    }

    @GameTest
    public void creaturesForgetAndAreStitched(TestContext ctx) {
        CowEntity cow = ctx.spawnEntity(EntityType.COW, new BlockPos(2, 1, 2));
        Glitches.apply(cow, GlitchKind.HERD);
        check(ctx, Glitches.of(cow) == GlitchKind.HERD, "cow remembers being a creeper");
        check(ctx, Glitches.heal(cow), "stitched");
        check(ctx, Glitches.of(cow) == null, "cow is a cow again");
        ctx.complete();
    }

    @GameTest
    public void worldWritesAndReadsWords(TestContext ctx) {
        BlockPos ground = ctx.getAbsolutePos(new BlockPos(3, 0, 3));
        Language.write(ctx.getWorld(), ground, Word.WITNESS, 1);
        var match = Language.read(ctx.getWorld(), ground.up());
        check(ctx, match.isPresent() && match.get().word() == Word.WITNESS, "read back WITNESS, got " + match.map(m -> m.word().name()).orElse("nothing"));
        ctx.complete();
    }

    @GameTest
    public void stoneRemembersFallingUpward(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        FakePlayer p = player(ctx, 500);
        BlockPos a = ctx.getAbsolutePos(new BlockPos(2, 1, 2));
        for (int i = 0; i < 3; i++) {
            world.setBlockState(a.east(i), Blocks.STONE_BRICKS.getDefaultState());
            Reality.region(world).placed(a.east(i));
        }
        check(ctx, RealityEdits.lift(p, world, a), "lift succeeded");
        for (int i = 0; i < 3; i++) {
            check(ctx, world.getBlockState(a.east(i)).isAir(), "original position cleared");
            check(ctx, world.getBlockState(a.east(i).up(RealityEdits.LIFT_HEIGHT)).isOf(Blocks.STONE_BRICKS), "lifted block present");
        }
        check(ctx, !Reality.player(p).activeEdits().isEmpty(), "edit recorded");
        ctx.complete();
    }

    @GameTest(maxTicks = 80)
    public void singularityPullsThingsIn(TestContext ctx) {
        ctx.setBlockState(new BlockPos(1, 1, 1), ModBlocks.SINGULARITY);
        ItemEntity item = ctx.spawnItem(Items.DIRT, 6f, 2f, 6f);
        double start = item.squaredDistanceTo(ctx.getAbsolute(new net.minecraft.util.math.Vec3d(1.5, 1.5, 1.5)));
        ctx.runAtTick(60, () -> {
            double now = item.squaredDistanceTo(ctx.getAbsolute(new net.minecraft.util.math.Vec3d(1.5, 1.5, 1.5)));
            check(ctx, now < start, "item should be drawn toward the singularity (" + start + " -> " + now + ")");
            ctx.complete();
        });
    }

    @GameTest(maxTicks = 200)
    public void marrowglassClosesGaps(TestContext ctx) {
        BlockPos a = new BlockPos(1, 1, 1);
        BlockPos gap = new BlockPos(2, 1, 1);
        BlockPos b = new BlockPos(3, 1, 1);
        ctx.setBlockState(a, ModBlocks.MARROWGLASS);
        ctx.setBlockState(b, ModBlocks.MARROWGLASS);
        ctx.runAtEveryTick(() -> {
            ctx.forceRandomTick(a);
            ctx.forceRandomTick(b);
        });
        ctx.runAtTick(150, () -> {
            check(ctx, ctx.getBlockState(gap).isOf(ModBlocks.MARROWGLASS), "marrowglass should grow into the gap");
            ctx.complete();
        });
    }

    @GameTest
    public void worldMemoryRoundTrips(TestContext ctx) {
        WorldMemory memory = new WorldMemory();
        memory.lucidity.setLucidity(42);
        memory.setFlag("first_cube");
        memory.chronicle.record(new dev.scripted.forgotten.logic.chronicle.ChronicleEntry(ChronicleEvent.BOSS_DEFEATED, 5,
                "minecraft:overworld", 1, 2, 3, "Alex", "cartographer"));
        NbtElement nbt = WorldMemory.CODEC.encodeStart(NbtOps.INSTANCE, memory).getOrThrow();
        WorldMemory back = WorldMemory.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow();
        check(ctx, Math.abs(back.lucidity.lucidity() - 42) < 0.01, "lucidity survives");
        check(ctx, back.flag("first_cube"), "flags survive");
        check(ctx, back.chronicle.size() == 1, "chronicle survives");
        ctx.complete();
    }

    @GameTest
    public void playerMemoryRoundTrips(TestContext ctx) {
        PlayerMemory m = new PlayerMemory();
        m.insight = 77;
        m.debt = 13.5;
        m.journal.study(Word.RAIN);
        m.observer.spokenTo();
        m.visit("minecraft:overworld|1|2|3");
        NbtElement nbt = PlayerMemory.CODEC.encodeStart(NbtOps.INSTANCE, m).getOrThrow();
        PlayerMemory back = PlayerMemory.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow();
        check(ctx, back.insight == 77 && Math.abs(back.debt - 13.5) < 1e-9, "numbers survive");
        check(ctx, back.journal.progress(Word.RAIN) == 1, "journal survives");
        check(ctx, back.observer.trust() == m.observer.trust(), "observer survives");
        check(ctx, back.places.equals(m.places), "places survive");
        ctx.complete();
    }
}
