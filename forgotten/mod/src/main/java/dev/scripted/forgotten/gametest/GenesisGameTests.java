package dev.scripted.forgotten.gametest;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.boss.AuthorTrial;
import dev.scripted.forgotten.logic.concept.Concept;
import dev.scripted.forgotten.logic.genesis.Ending;
import dev.scripted.forgotten.logic.genesis.GenesisRule;
import dev.scripted.forgotten.logic.genesis.Law;
import dev.scripted.forgotten.logic.genesis.LawSubject;
import dev.scripted.forgotten.menu.GenesisMenu;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.system.Bosses;
import dev.scripted.forgotten.system.Genesis;
import dev.scripted.forgotten.world.WorldMemory;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import static dev.scripted.forgotten.gametest.ForgottenGameTests.check;
import static dev.scripted.forgotten.gametest.ForgottenGameTests.player;

/** The endgame: the Author's question, the rules, the laws and the interface that writes them. */
public final class GenesisGameTests {
    @GameTest
    public void theRightConceptsMakeTheAuthorAsk(TestContext ctx) {
        MobEntity author = Bosses.summon(Bosses.Kind.AUTHOR, ctx.getWorld(), ctx.getAbsolutePos(new BlockPos(3, 1, 3)));
        check(ctx, author != null, "the Author came");
        FakePlayer p = player(ctx, 0);
        AuthorTrial.Step[] steps = {AuthorTrial.Step.UNMAKE_INVENTORY, AuthorTrial.Step.UNMAKE_ENCHANTMENTS, AuthorTrial.Step.UNMAKE_EDITS,
                AuthorTrial.Step.UNMAKE_CAUSES, AuthorTrial.Step.UNMAKE_RETURN};
        for (int round = 0; round < 12; round++) {
            for (AuthorTrial.Step step : steps) {
                Bosses.answer(author, p, step.answer());
            }
        }
        WorldMemory memory = Reality.memory(ctx.getWorld().getServer());
        check(ctx, memory.flag(Genesis.AUTHOR_ASKED), "answered enough, the Author asks its question");
        Bosses.dismissAuthor(ctx.getWorld().getServer());
        check(ctx, author.isRemoved(), "the Author can be dismissed");
        ctx.complete();
    }

    @GameTest
    public void rulesWaitForTheQuestionAndTheBeginning(TestContext ctx) {
        WorldMemory memory = Reality.memory(ctx.getWorld().getServer());
        FakePlayer p = player(ctx, 0);
        memory.setFlag(Genesis.AUTHOR_ASKED);
        Genesis.toggleRule(p, GenesisRule.ITEMS_REMEMBER);
        check(ctx, memory.rule(GenesisRule.ITEMS_REMEMBER), "a rule can be written once the Author has asked");
        Genesis.toggleRule(p, GenesisRule.ITEMS_REMEMBER);
        check(ctx, !memory.rule(GenesisRule.ITEMS_REMEMBER), "and unwritten");
        boolean revealed = memory.flag(Genesis.BEGINNING_REVEALED);
        if (!revealed) {
            Genesis.toggleRule(p, GenesisRule.REWRITE_THE_BEGINNING);
            check(ctx, !memory.rule(GenesisRule.REWRITE_THE_BEGINNING), "the beginning cannot be rewritten unseen");
        }
        ctx.complete();
    }

    @GameTest
    public void onlyTheAuthorWritesLaws(TestContext ctx) {
        WorldMemory memory = Reality.memory(ctx.getWorld().getServer());
        FakePlayer p = player(ctx, 0);
        Law law = new Law(LawSubject.WORLD, Concept.WITNESS);
        check(ctx, memory.ending() == null, "the test world has not ended");
        Genesis.toggleLaw(p, law);
        check(ctx, !memory.law(law), "nobody else can write laws");
        memory.end(Ending.AUTHOR, p.getUuid());
        Genesis.toggleLaw(p, law);
        boolean written = memory.law(law);
        Genesis.toggleLaw(p, law);
        memory.resetEnding();
        check(ctx, written, "the Author writes laws");
        check(ctx, !memory.law(law), "and repeals them");
        ctx.complete();
    }

    @GameTest
    public void theInterfaceShowsEveryRuleAndLaw(TestContext ctx) {
        FakePlayer p = player(ctx, 0);
        GenesisMenu menu = new GenesisMenu(1, p.getInventory());
        check(ctx, !menu.getSlot(GenesisMenu.RULES).getStack().isEmpty(), "rule buttons");
        check(ctx, !menu.getSlot(GenesisMenu.ENDINGS).getStack().isEmpty(), "ending buttons");
        for (int i = 0; i < 36; i++) {
            check(ctx, menu.getSlot(GenesisMenu.LAWS + i).getStack().isOf(ModItems.concept(Concept.values()[i % 12])), "law " + i);
        }
        ctx.complete();
    }

    @GameTest(maxTicks = 40)
    public void sandHangsWhenGravityIsOptional(TestContext ctx) {
        WorldMemory memory = Reality.memory(ctx.getWorld().getServer());
        memory.setRule(GenesisRule.GRAVITY_OPTIONAL, true);
        BlockPos sand = new BlockPos(2, 4, 2);
        ctx.setBlockState(sand, Blocks.SAND);
        ctx.runAtTick(20, () -> {
            memory.setRule(GenesisRule.GRAVITY_OPTIONAL, false);
            check(ctx, ctx.getBlockState(sand).isOf(Blocks.SAND), "the sand stayed where it was put");
            ctx.complete();
        });
    }
}
