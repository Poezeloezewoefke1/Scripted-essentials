package dev.scripted.forgotten.gametest;

import com.mojang.authlib.GameProfile;
import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.block.RuleAnchorBlock;
import dev.scripted.forgotten.logic.paradox.ArchitectRank;
import dev.scripted.forgotten.registry.ModBlocks;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.system.Anachronisms;
import dev.scripted.forgotten.system.Architecture;
import dev.scripted.forgotten.system.Guilds;
import dev.scripted.forgotten.world.Politics;
import dev.scripted.forgotten.world.RegionMemory;
import dev.scripted.forgotten.world.WorldMemory;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.UUID;

import static dev.scripted.forgotten.gametest.ForgottenGameTests.check;
import static dev.scripted.forgotten.gametest.ForgottenGameTests.player;

/** Reality Architecture, the Possibility Eater, politics and the Underdream's Tomorrow. */
public final class ArchitectureGameTests {
    static FakePlayer other(TestContext ctx, String name) {
        return FakePlayer.get(ctx.getWorld(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
    }

    @GameTest
    public void ruleAnchorsForbidHarmBetweenPlayers(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        ctx.setBlockState(new BlockPos(2, 1, 2), ModBlocks.RULE_ANCHOR);
        BlockPos at = ctx.getAbsolutePos(new BlockPos(3, 1, 3));
        FakePlayer victim = other(ctx, "victim");
        victim.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        victim.setHealth(victim.getMaxHealth());
        FakePlayer attacker = other(ctx, "attacker");
        check(ctx, RuleAnchorBlock.enforced(world, victim.getX(), victim.getY(), victim.getZ(), RuleAnchorBlock.Rule.NO_PVP), "no-PvP in force");
        check(ctx, !Guilds.allowDamage(victim, world.getDamageSources().playerAttack(attacker), 5f), "the blow is refused");
        ctx.setBlockState(new BlockPos(2, 1, 2), Blocks.AIR);
        check(ctx, !RuleAnchorBlock.enforced(world, victim.getX(), victim.getY(), victim.getZ(), RuleAnchorBlock.Rule.NO_PVP), "lifted with the anchor");
        ctx.complete();
    }

    @GameTest(maxTicks = 60)
    public void possibilityEatersEatExplosions(TestContext ctx) {
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
                ctx.setBlockState(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        ctx.setBlockState(new BlockPos(0, 2, 0), ModBlocks.POSSIBILITY_EATER);
        CreeperEntity creeper = ctx.spawnEntity(EntityType.CREEPER, new BlockPos(4, 2, 4));
        creeper.ignite();
        boolean[] done = {false};
        ctx.runAtEveryTick(() -> {
            if (done[0] || !creeper.isRemoved()) {
                return;
            }
            done[0] = true;
            check(ctx, ctx.getBlockState(new BlockPos(4, 1, 4)).isOf(Blocks.STONE), "the explosion never happened");
            ctx.complete();
        });
    }

    @GameTest
    public void theMemoryVaultRebuildsWhatWasLost(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        BlockPos vault = ctx.getAbsolutePos(new BlockPos(1, 1, 1));
        ctx.setBlockState(new BlockPos(1, 1, 1), ModBlocks.MEMORY_VAULT);
        BlockEntity be = world.getBlockEntity(vault);
        check(ctx, be instanceof dev.scripted.forgotten.block.entity.MemoryVaultBlockEntity, "vault entity");
        ((dev.scripted.forgotten.block.entity.MemoryVaultBlockEntity) be).memories.setStack(0, new ItemStack(ModItems.MEMORY, 2));
        BlockPos lost = ctx.getAbsolutePos(new BlockPos(4, 1, 4));
        RegionMemory region = Reality.region(world);
        region.demolished(lost, Blocks.BRICKS.getDefaultState(), Reality.now(world.getServer()));
        Architecture.reconstruct(world, region, vault);
        check(ctx, world.getBlockState(lost).isOf(Blocks.BRICKS), "the demolished brick is back");
        ctx.complete();
    }

    @GameTest
    public void distanceChambersJoinWithAPearl(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        FakePlayer p = player(ctx, 0);
        ctx.setBlockState(new BlockPos(1, 1, 1), ModBlocks.DISTANCE_CHAMBER);
        ctx.setBlockState(new BlockPos(5, 1, 5), ModBlocks.DISTANCE_CHAMBER);
        ItemStack pearl = new ItemStack(Items.ENDER_PEARL, 2);
        Architecture.link(world, p, ctx.getAbsolutePos(new BlockPos(1, 1, 1)), pearl);
        Architecture.link(world, p, ctx.getAbsolutePos(new BlockPos(5, 1, 5)), pearl);
        WorldMemory memory = Reality.memory(world.getServer());
        String a = dev.scripted.forgotten.system.Rifts.point(world, net.minecraft.util.math.Vec3d.of(ctx.getAbsolutePos(new BlockPos(1, 1, 1))));
        check(ctx, memory.links.containsKey(a), "the doors are joined");
        ctx.setBlockState(new BlockPos(1, 1, 1), Blocks.AIR);
        check(ctx, !memory.links.containsKey(a), "and unjoined when one is broken");
        ctx.complete();
    }

    @GameTest
    public void tomorrowGrowsEverythingInTheChunk(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
                ctx.setBlockState(new BlockPos(x, 1, z), Blocks.FARMLAND);
                ctx.setBlockState(new BlockPos(x, 2, z), Blocks.WHEAT);
            }
        }
        Anachronisms.tomorrow(world, new ChunkPos(ctx.getAbsolutePos(new BlockPos(3, 2, 3))));
        int grown = 0;
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
                var s = ctx.getBlockState(new BlockPos(x, 2, z));
                if (s.isOf(Blocks.WHEAT) && s.get(CropBlock.AGE) > 0) {
                    grown++;
                }
            }
        }
        check(ctx, grown > 0, "a day passed for the wheat");
        ctx.complete();
    }

    @GameTest
    public void sealedGuildTerritoryKeepsOutsidersOut(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        Politics politics = Politics.get(world.getServer());
        FakePlayer member = other(ctx, "member");
        FakePlayer outsider = other(ctx, "outsider");
        Politics.Guild guild = politics.create("gametest_guild", member.getUuid());
        check(ctx, guild != null, "guild founded");
        BlockPos at = ctx.getAbsolutePos(new BlockPos(2, 1, 2));
        guild.claims.add(world.getRegistryKey().getValue() + "|" + ChunkPos.toLong(at));
        guild.rules.add(Politics.GuildRule.SEALED);
        boolean outsiderBlocked = !Guilds.allowBreak(world, outsider, at);
        boolean memberAllowed = Guilds.allowBreak(world, member, at);
        politics.leave(member.getUuid());
        check(ctx, outsiderBlocked, "outsiders cannot break sealed ground");
        check(ctx, memberAllowed, "members can");
        check(ctx, !politics.guilds.containsKey("gametest_guild"), "an empty guild dissolves");
        ctx.complete();
    }

    @GameTest
    public void aCodexFragmentLendsItsRank(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        FakePlayer master = other(ctx, "master");
        Reality.player(master).insight = 100000;
        FakePlayer novice = other(ctx, "novice");
        Reality.player(novice).insight = 0;
        ItemStack page = dev.scripted.forgotten.item.CodexFragmentItem.of(ModItems.CODEX_FRAGMENT, master);
        novice.setStackInHand(Hand.MAIN_HAND, page);
        ModItems.CODEX_FRAGMENT.use(world, novice, Hand.MAIN_HAND);
        check(ctx, Reality.rank(novice).level() == Reality.rank(master).level(), "the novice borrows the master's rank");
        check(ctx, Reality.rank(novice).level() > ArchitectRank.of(0).level(), "which is higher than their own");
        ctx.complete();
    }
}
