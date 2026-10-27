package dev.scripted.forgotten.gametest;

import dev.scripted.forgotten.logic.boss.StackDepth;
import dev.scripted.forgotten.logic.concept.Concept;
import dev.scripted.forgotten.registry.ModAttachments;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.system.Bosses;
import dev.scripted.forgotten.system.Creatures;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import static dev.scripted.forgotten.gametest.ForgottenGameTests.check;
import static dev.scripted.forgotten.gametest.ForgottenGameTests.player;

/** The creatures and bosses, driven through the same hooks the game uses. */
public final class BossGameTests {
    static Box box(TestContext ctx) {
        return new Box(ctx.getAbsolutePos(BlockPos.ORIGIN)).expand(10);
    }

    @GameTest
    public void recursionSpidersSplitWhenKilled(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        SpiderEntity spider = ctx.spawnEntity(EntityType.SPIDER, new BlockPos(3, 1, 3));
        spider.setAttached(ModAttachments.RECURSION, 0);
        Creatures.shape(spider, 0);
        spider.damage(world, world.getDamageSources().generic(), 1000f);
        long children = world.getEntitiesByClass(SpiderEntity.class, box(ctx),
                s -> s.isAlive() && Integer.valueOf(1).equals(s.getAttached(ModAttachments.RECURSION))).size();
        check(ctx, children == 2, "two first-generation copies, found " + children);
        ctx.killAllEntities();
        ctx.complete();
    }

    @GameTest
    public void theKingDoesNotFallForTheSameTrickTwice(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        MobEntity king = Bosses.summon(Bosses.Kind.KING_OF_YESTERDAY, world, ctx.getAbsolutePos(new BlockPos(3, 1, 3)));
        check(ctx, king != null, "the King appeared");
        FakePlayer p = player(ctx, 0);
        p.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        float start = king.getHealth();
        float last = start;
        float lastDrop = 0f;
        for (int i = 0; i < 8; i++) {
            king.timeUntilRegen = 0;
            king.damage(world, world.getDamageSources().playerAttack(p), 6f);
            lastDrop = last - king.getHealth();
            last = king.getHealth();
        }
        check(ctx, last < start, "the first blows landed");
        check(ctx, lastDrop == 0f, "the eighth identical blow should do nothing, it did " + lastDrop);
        ctx.killAllEntities();
        ctx.complete();
    }

    @GameTest
    public void theCompilerOnlyFeelsErrors(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        MobEntity compiler = Bosses.summon(Bosses.Kind.COMPILER, world, ctx.getAbsolutePos(new BlockPos(3, 1, 3)));
        check(ctx, compiler != null, "the Compiler booted");
        StackDepth stack = Bosses.stackOf(compiler);
        FakePlayer p = player(ctx, 0);
        p.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        float health = compiler.getHealth();
        compiler.damage(world, world.getDamageSources().playerAttack(p), 10f);
        check(ctx, compiler.getHealth() == health && stack.depth() == 0, "a sword is expected behaviour");
        p.setStackInHand(Hand.MAIN_HAND, new ItemStack(ModItems.UNDEFINED_SWORD));
        compiler.timeUntilRegen = 0;
        compiler.damage(world, world.getDamageSources().playerAttack(p), 10f);
        check(ctx, stack.depth() > 0, "an undefined weapon pushes the stack deeper");
        ctx.killAllEntities();
        ctx.complete();
    }

    @GameTest(maxTicks = 100)
    public void theUnwrapperEatsWhatYouThrowIt(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        MobEntity unwrapper = Bosses.summon(Bosses.Kind.UNWRAPPER, world, ctx.getAbsolutePos(new BlockPos(3, 1, 3)));
        check(ctx, unwrapper != null, "the Unwrapper came");
        world.spawnEntity(new ItemEntity(world, unwrapper.getX(), unwrapper.getY() + 0.5, unwrapper.getZ(), new ItemStack(Items.IRON_BLOCK, 4)));
        boolean[] done = {false};
        ctx.runAtEveryTick(() -> {
            if (!done[0] && Bosses.fedOf(unwrapper) >= 4 * 18.0) {
                done[0] = true;
                ctx.killAllEntities();
                ctx.complete();
            }
        });
    }

    @GameTest
    public void theCuratorIsBuiltFromHistory(TestContext ctx) {
        MobEntity curator = Bosses.summon(Bosses.Kind.CURATOR, ctx.getWorld(), ctx.getAbsolutePos(new BlockPos(3, 1, 3)));
        check(ctx, curator != null, "the Curator assembled");
        check(ctx, curator.hasPassengers(), "its body of remembered blocks rides it");
        ctx.killAllEntities();
        ctx.complete();
    }

    @GameTest(maxTicks = 80)
    public void bloomCreepersFlowerInsteadOfExploding(TestContext ctx) {
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
                ctx.setBlockState(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            }
        }
        CreeperEntity creeper = ctx.spawnEntity(EntityType.CREEPER, new BlockPos(3, 2, 3));
        Creatures.bloomify(creeper);
        creeper.ignite();
        boolean[] done = {false};
        ctx.runAtEveryTick(() -> {
            if (done[0] || !creeper.isRemoved()) {
                return;
            }
            done[0] = true;
            check(ctx, ctx.getBlockState(new BlockPos(3, 1, 3)).isOf(Blocks.GRASS_BLOCK), "nothing exploded");
            ctx.complete();
        });
    }

    @GameTest
    public void potentialCreaturesLeavePossibility(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        MobEntity potential = Creatures.potential(world, ctx.getAbsolutePos(new BlockPos(3, 1, 3)), world.getRandom());
        check(ctx, potential != null, "a creature that has not decided what it is");
        potential.damage(world, world.getDamageSources().generic(), 1000f);
        boolean dropped = !world.getEntitiesByClass(ItemEntity.class, box(ctx),
                i -> i.getStack().isOf(ModItems.concept(Concept.POSSIBILITY))).isEmpty();
        check(ctx, dropped, "it leaves Possibility behind");
        ctx.killAllEntities();
        ctx.complete();
    }
}
