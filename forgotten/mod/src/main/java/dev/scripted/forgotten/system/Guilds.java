package dev.scripted.forgotten.system;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.block.RuleAnchorBlock;
import dev.scripted.forgotten.logic.storm.StormScheduler;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.Politics;
import dev.scripted.forgotten.world.WorldMemory;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.UUID;

/**
 * Multiplayer politics. Architect Guilds claim chunks and set rules in them; Rule Anchors
 * enforce local laws; elections choose a temporary World Admin who can trigger a few reality
 * events. No one controls the world for long: Reality Storms suspend every Rule Anchor.
 */
public final class Guilds {
    public static final int ELECTION_TICKS = 2400;
    public static final int ADMIN_TICKS = 24000;
    public static final int ADMIN_COOLDOWN = 6000;
    private static final Map<UUID, Long> ADMIN_USED = new HashMap<>();

    private Guilds() {
    }

    static String dim(World world) {
        return world.getRegistryKey().getValue().toString();
    }

    public static Politics.Guild territory(ServerWorld world, BlockPos pos) {
        return Politics.get(world.getServer()).owner(dim(world), ChunkPos.toLong(pos));
    }

    // ---- per second ----

    public static void tick(MinecraftServer server, long now) {
        if (now % 20 != 0) {
            return;
        }
        Politics politics = Politics.get(server);
        if (politics.electionEnds > 0 && now >= politics.electionEnds) {
            count(server, politics, now);
        }
        if (politics.admin != null && now >= politics.adminUntil) {
            politics.admin = null;
            politics.markDirty();
            broadcast(server, "The World Admin's term has ended.");
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerWorld world = Reality.world(player);
            Politics.Guild guild = territory(world, player.getBlockPos());
            if (guild != null && guild.rules.contains(Politics.GuildRule.NIGHT)) {
                if (!guild.members.contains(player.getUuid())) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 60, 0, true, false));
                }
                for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(32),
                        m -> m.getType().isIn(EntityTypeTags.UNDEAD) && m.isOnFire())) {
                    if (territory(world, mob.getBlockPos()) == guild) {
                        mob.extinguish();
                    }
                }
            }
            for (HostileEntity mob : world.getEntitiesByClass(HostileEntity.class, player.getBoundingBox().expand(32), Entity::isAlive)) {
                if (RuleAnchorBlock.enforced(world, mob.getX(), mob.getY(), mob.getZ(), RuleAnchorBlock.Rule.NO_MOBS)
                        && !mob.hasAttached(dev.scripted.forgotten.registry.ModAttachments.BOSS)) {
                    Vec3d away = mob.getEntityPos().subtract(player.getEntityPos()).normalize().multiply(0.9);
                    mob.setVelocity(away.x, 0.3, away.z);
                    mob.velocityDirty = true;
                    mob.setTarget(null);
                }
            }
            if (player.isSneaking() && !player.isOnGround()
                    && RuleAnchorBlock.enforced(world, player.getX(), player.getY(), player.getZ(), RuleAnchorBlock.Rule.GRAVITY_OPTIONAL)) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 30, 0, false, false));
            }
        }
    }

    /** Explosions are caught a tick before they happen; checked every tick, not every second. */
    public static void explosions(MinecraftServer server) {
        for (Entity e : Eaters.aboutToExplode()) {
            if (e.getEntityWorld() instanceof ServerWorld world
                    && RuleAnchorBlock.enforced(world, e.getX(), e.getY(), e.getZ(), RuleAnchorBlock.Rule.NO_EXPLOSIONS)) {
                e.discard();
            }
        }
    }

    // ---- hooks ----

    public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (entity instanceof ServerPlayerEntity victim && source.getAttacker() instanceof PlayerEntity attacker && attacker != victim) {
            return !RuleAnchorBlock.enforced(Reality.world(victim), victim.getX(), victim.getY(), victim.getZ(), RuleAnchorBlock.Rule.NO_PVP);
        }
        return true;
    }

    /** A sealed guild's territory cannot be broken by outsiders. */
    public static boolean allowBreak(ServerWorld world, ServerPlayerEntity player, BlockPos pos) {
        Politics.Guild guild = territory(world, pos);
        if (guild != null && guild.rules.contains(Politics.GuildRule.SEALED) && !guild.members.contains(player.getUuid())
                && !Reality.storming(world.getServer())) {
            Texts.bar(player, Texts.world("This place belongs to " + guild.name + ". It does not let you."));
            return false;
        }
        return true;
    }

    /** Falling blocks under a gravity-optional Rule Anchor stay where they were. */
    public static boolean gravityOptional(ServerWorld world, BlockPos pos) {
        return RuleAnchorBlock.enforced(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, RuleAnchorBlock.Rule.GRAVITY_OPTIONAL);
    }

    // ---- elections ----

    static void count(MinecraftServer server, Politics politics, long now) {
        politics.electionEnds = 0;
        Map<UUID, Integer> tally = new HashMap<>();
        politics.votes.values().forEach(c -> tally.merge(c, 1, Integer::sum));
        politics.votes.clear();
        UUID winner = tally.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        if (winner == null) {
            broadcast(server, "Nobody voted. The world remains ungoverned.");
            politics.markDirty();
            return;
        }
        politics.admin = winner;
        politics.adminUntil = now + ADMIN_TICKS;
        politics.markDirty();
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(winner);
        broadcast(server, (p != null ? p.getName().getString() : "Someone") + " is the World Admin for one day.");
    }

    static void broadcast(MinecraftServer server, String text) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            Texts.tell(p, Texts.world(text));
        }
    }

    // ---- commands ----

    static ServerPlayerEntity self(CommandContext<ServerCommandSource> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return ctx.getSource().getPlayerOrThrow();
    }

    static int reply(CommandContext<ServerCommandSource> ctx, String text) {
        ctx.getSource().sendFeedback(() -> Text.literal(text), false);
        return 1;
    }

    public static void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        LiteralArgumentBuilder<ServerCommandSource> guild = CommandManager.literal("guild");
        guild.then(CommandManager.literal("create").then(CommandManager.argument("name", StringArgumentType.word()).executes(ctx -> {
            ServerPlayerEntity p = self(ctx);
            Politics.Guild g = Politics.get(p.getEntityWorld().getServer()).create(StringArgumentType.getString(ctx, "name"), p.getUuid());
            return reply(ctx, g == null ? "That name is taken, or you already belong to a guild." : "The " + g.name + " guild is founded.");
        })));
        guild.then(CommandManager.literal("join").then(CommandManager.argument("name", StringArgumentType.word()).executes(ctx -> {
            ServerPlayerEntity p = self(ctx);
            Politics politics = Politics.get(p.getEntityWorld().getServer());
            Politics.Guild g = politics.guilds.get(StringArgumentType.getString(ctx, "name"));
            if (g == null || politics.guildOf(p.getUuid()) != null) {
                return reply(ctx, "No such guild, or you already belong to one.");
            }
            g.members.add(p.getUuid());
            politics.markDirty();
            return reply(ctx, "You join " + g.name + ".");
        })));
        guild.then(CommandManager.literal("leave").executes(ctx -> {
            ServerPlayerEntity p = self(ctx);
            Politics.get(p.getEntityWorld().getServer()).leave(p.getUuid());
            return reply(ctx, "You belong to no guild.");
        }));
        guild.then(CommandManager.literal("claim").executes(ctx -> {
            ServerPlayerEntity p = self(ctx);
            Politics politics = Politics.get(p.getEntityWorld().getServer());
            Politics.Guild g = politics.guildOf(p.getUuid());
            ServerWorld world = Reality.world(p);
            if (g == null) {
                return reply(ctx, "You belong to no guild.");
            }
            if (territory(world, p.getBlockPos()) != null) {
                return reply(ctx, "This chunk is already claimed.");
            }
            g.claims.add(dim(world) + "|" + ChunkPos.toLong(p.getBlockPos()));
            politics.markDirty();
            return reply(ctx, g.name + " now controls reality in this chunk.");
        }));
        guild.then(CommandManager.literal("unclaim").executes(ctx -> {
            ServerPlayerEntity p = self(ctx);
            Politics politics = Politics.get(p.getEntityWorld().getServer());
            Politics.Guild g = politics.guildOf(p.getUuid());
            if (g != null && g.claims.remove(dim(Reality.world(p)) + "|" + ChunkPos.toLong(p.getBlockPos()))) {
                politics.markDirty();
                return reply(ctx, "Unclaimed.");
            }
            return reply(ctx, "Your guild does not hold this chunk.");
        }));
        for (Politics.GuildRule rule : Politics.GuildRule.values()) {
            guild.then(CommandManager.literal("rule").then(CommandManager.literal(rule.name().toLowerCase(java.util.Locale.ROOT)).executes(ctx -> {
                ServerPlayerEntity p = self(ctx);
                Politics politics = Politics.get(p.getEntityWorld().getServer());
                Politics.Guild g = politics.guildOf(p.getUuid());
                if (g == null || !p.getUuid().equals(g.leader)) {
                    return reply(ctx, "Only a guild's leader sets its rules.");
                }
                boolean on = g.rules.add(rule) || !g.rules.remove(rule);
                politics.markDirty();
                if (on && rule == Politics.GuildRule.FLOAT && territory(Reality.world(p), p.getBlockPos()) == g) {
                    RealityEdits.lift(p, Reality.world(p), p.getBlockPos().down());
                }
                return reply(ctx, g.name + (on ? " now has: " : " no longer has: ") + rule.name().toLowerCase(java.util.Locale.ROOT));
            })));
        }
        guild.then(CommandManager.literal("info").executes(ctx -> {
            ServerPlayerEntity p = self(ctx);
            Politics.Guild here = territory(Reality.world(p), p.getBlockPos());
            Politics.Guild mine = Politics.get(p.getEntityWorld().getServer()).guildOf(p.getUuid());
            return reply(ctx, "Here: " + (here == null ? "unclaimed" : here.name + " " + here.rules) + ". You: "
                    + (mine == null ? "no guild" : mine.name + " (" + mine.members.size() + " members, " + mine.claims.size() + " chunks)"));
        }));
        root.then(guild);

        root.then(CommandManager.literal("election").executes(ctx -> {
            ServerPlayerEntity p = self(ctx);
            MinecraftServer server = p.getEntityWorld().getServer();
            Politics politics = Politics.get(server);
            if (politics.electionEnds > 0 || politics.admin != null) {
                return reply(ctx, "An election is running, or the World Admin's term has not ended.");
            }
            politics.electionEnds = Reality.now(server) + ELECTION_TICKS;
            politics.markDirty();
            broadcast(server, "An election for World Admin has begun. Two minutes: /forgotten vote <player>.");
            return 1;
        }));
        root.then(CommandManager.literal("vote").then(CommandManager.argument("candidate", EntityArgumentType.player()).executes(ctx -> {
            ServerPlayerEntity p = self(ctx);
            ServerPlayerEntity candidate = EntityArgumentType.getPlayer(ctx, "candidate");
            Politics politics = Politics.get(p.getEntityWorld().getServer());
            if (politics.electionEnds == 0) {
                return reply(ctx, "No election is running.");
            }
            politics.votes.put(p.getUuid(), candidate.getUuid());
            return reply(ctx, "You voted for " + candidate.getName().getString() + ".");
        })));
        LiteralArgumentBuilder<ServerCommandSource> worldadmin = CommandManager.literal("worldadmin");
        for (String event : new String[]{"storm", "calm", "mend", "lull"}) {
            worldadmin.then(CommandManager.literal(event).executes(ctx -> adminEvent(ctx, event)));
        }
        root.then(worldadmin);
    }

    /** The World Admin's limited powers. The world lets them do a few things, once in a while. */
    static int adminEvent(CommandContext<ServerCommandSource> ctx, String event) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity p = self(ctx);
        MinecraftServer server = p.getEntityWorld().getServer();
        Politics politics = Politics.get(server);
        long now = Reality.now(server);
        if (!p.getUuid().equals(politics.admin)) {
            return reply(ctx, "You are not the World Admin.");
        }
        Long used = ADMIN_USED.get(p.getUuid());
        if (used != null && now - used < ADMIN_COOLDOWN) {
            return reply(ctx, "The world needs time before it listens again.");
        }
        ADMIN_USED.put(p.getUuid(), now);
        WorldMemory memory = Reality.memory(server);
        switch (event) {
            case "storm" -> StormDirector.begin(server, memory, StormScheduler.pick(new SplittableRandom(),
                    server.getPlayerManager().getCurrentPlayerCount(), memory.lucidity.lucidity()), now);
            case "calm" -> {
                memory.stormEnds = now;
                memory.markDirty();
            }
            case "mend" -> {
                ServerWorld world = Reality.world(p);
                ChunkPos c = p.getChunkPos();
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        Reality.mend(world, new BlockPos((c.x + dx) * 16, p.getBlockY(), (c.z + dz) * 16), 30);
                    }
                }
            }
            case "lull" -> {
                memory.lucidity.lull(600);
                memory.markDirty();
            }
            default -> {
                return 0;
            }
        }
        broadcast(server, "The World Admin has decreed: " + event + ".");
        return 1;
    }

    public static void clear() {
        ADMIN_USED.clear();
    }
}
