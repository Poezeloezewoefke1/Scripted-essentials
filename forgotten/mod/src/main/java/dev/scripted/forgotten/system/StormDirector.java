package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Forgotten;
import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.logic.storm.StormKind;
import dev.scripted.forgotten.logic.storm.StormScheduler;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.RegionMemory;
import dev.scripted.forgotten.world.WorldMemory;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Reality Storms: every so often, once the world is half awake, random rules fail across the
 * whole server at once. They are disasters and opportunities both: Paradox Fragments only fall
 * during storms, and Unwritten Stone can only be broken while one rages.
 */
public final class StormDirector {
    private static final Identifier STORM_GRAVITY = Forgotten.id("storm_gravity");
    private static final Identifier FUSED = Forgotten.id("fused");
    private static final SplittableRandom RANDOM = new SplittableRandom();
    private static ServerBossBar bar;

    private StormDirector() {
    }

    public static void tick(MinecraftServer server, long now) {
        WorldMemory memory = Reality.memory(server);
        double lucidity = memory.lucidity.lucidity();
        if (memory.storming(now)) {
            during(server, memory, now);
            return;
        }
        if (!memory.storm.isEmpty()) {
            end(server, memory);
        }
        if (lucidity < StormScheduler.MIN_LUCIDITY || memory.ending() == dev.scripted.forgotten.logic.genesis.Ending.PRESERVE) {
            return;
        }
        if (memory.nextStorm == 0) {
            memory.nextStorm = now + StormScheduler.interval(lucidity, RANDOM);
            memory.markDirty();
        } else if (now >= memory.nextStorm) {
            begin(server, memory, StormScheduler.pick(RANDOM, server.getPlayerManager().getCurrentPlayerCount(), lucidity), now);
        }
    }

    public static void begin(MinecraftServer server, WorldMemory memory, List<StormKind> kinds, long now) {
        memory.storm.clear();
        memory.storm.addAll(kinds);
        memory.stormEnds = now + StormScheduler.duration(RANDOM);
        memory.nextStorm = 0;
        memory.markDirty();
        String names = String.join(" / ", kinds.stream().map(StormKind::title).toList());
        bar = new ServerBossBar(Text.literal("REALITY STORM: " + names).formatted(Formatting.LIGHT_PURPLE),
                BossBar.Color.PURPLE, BossBar.Style.NOTCHED_10);
        ServerWorld overworld = server.getOverworld();
        overworld.setWeather(0, (int) (memory.stormEnds - now), true, true);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            bar.addPlayer(p);
            p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("REALITY STORM").formatted(Formatting.DARK_PURPLE)));
            p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(names).formatted(Formatting.GRAY)));
            Texts.tell(p, Texts.world(String.join(" ", kinds.stream().map(StormKind::description).toList())));
        }
        Reality.chronicle(overworld, ChronicleEvent.STORM, overworld.getSpawnPoint().getPos(), "", names);
        if (kinds.contains(StormKind.INVERTED_GRAVITY)) {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                invert(p, true);
            }
        }
    }

    static void during(MinecraftServer server, WorldMemory memory, long now) {
        if (bar != null) {
            bar.setPercent(Math.max(0f, Math.min(1f, (memory.stormEnds - now) / 4800f)));
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (bar != null) {
                bar.addPlayer(player);
            }
            ServerWorld world = Reality.world(player);
            Random random = world.getRandom();
            if (now % 600 == 0) {
                // Fragments of broken rules fall out of the sky.
                BlockPos at = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING,
                        player.getBlockPos().add(random.nextInt(17) - 8, 0, random.nextInt(17) - 8)).up(6);
                ItemEntity fragment = new ItemEntity(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, new ItemStack(ModItems.PARADOX_FRAGMENT));
                fragment.setGlowing(true);
                world.spawnEntity(fragment);
            }
            for (StormKind kind : memory.storm) {
                apply(world, player, kind, random, now);
            }
        }
    }

    static void apply(ServerWorld world, ServerPlayerEntity player, StormKind kind, Random random, long now) {
        switch (kind) {
            case INVERTED_GRAVITY -> {
                if (now % 40 == 0) {
                    invert(player, true);
                    for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(24), x -> !(x instanceof ServerPlayerEntity))) {
                        invert(e, true);
                    }
                }
            }
            case BLOCK_AMNESIA -> {
                if (now % 100 == 0) {
                    blockAmnesia(world, player, random);
                }
            }
            case MOB_FUSION -> {
                if (now % 200 == 0) {
                    fuse(world, player);
                }
            }
            case TIME_STUTTER -> {
                if (now % 400 == 0) {
                    Vec3d back = Hooks.rewindPoint(player);
                    player.requestTeleport(back.x, back.y, back.z);
                    Texts.bar(player, Texts.world("The last five seconds happen again."));
                }
            }
            case CHUNK_MUTATION -> {
                if (now % 300 == 0) {
                    mutateChunk(world, player, random);
                }
            }
            case PLAYER_SWAP -> {
                if (now % 600 == 0) {
                    swap(world, player, random);
                }
            }
            case ADMIN_GLITCH -> {
                if (now % 500 == 0 && player == world.getServer().getPlayerManager().getPlayerList().get(0)) {
                    adminGlitch(world.getServer(), random);
                }
            }
        }
        if (now % 200 == 0 && random.nextInt(3) == 0) {
            Unrendered.stir(world, player, random);
        }
    }

    static void invert(LivingEntity e, boolean on) {
        EntityAttributeInstance g = e.getAttributeInstance(EntityAttributes.GRAVITY);
        if (g == null) {
            return;
        }
        if (on && !g.hasModifier(STORM_GRAVITY)) {
            g.addTemporaryModifier(new EntityAttributeModifier(STORM_GRAVITY, -0.095, EntityAttributeModifier.Operation.ADD_VALUE));
        } else if (!on && g.hasModifier(STORM_GRAVITY)) {
            g.removeModifier(STORM_GRAVITY);
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 300, 0));
        }
    }

    /** Placed blocks forget what they are. Anchored chunks are spared: this is what anchors are for. */
    static void blockAmnesia(ServerWorld world, ServerPlayerEntity player, Random random) {
        RegionMemory region = Reality.region(world);
        long key = ChunkPos.toLong(player.getBlockPos());
        if (region.anchored(key)) {
            return;
        }
        RegionMemory.ChunkRecord r = region.peek(key);
        if (r == null || r.placed.isEmpty()) {
            return;
        }
        BlockState[] forgotten = {Blocks.MOSSY_COBBLESTONE.getDefaultState(), Blocks.CRACKED_STONE_BRICKS.getDefaultState(),
                Blocks.TUFF.getDefaultState(), Blocks.CALCITE.getDefaultState(), Blocks.MUD_BRICKS.getDefaultState(),
                Blocks.PACKED_MUD.getDefaultState(), Blocks.DRIPSTONE_BLOCK.getDefaultState()};
        long[] placed = r.placed.toLongArray();
        for (int i = 0; i < 3; i++) {
            BlockPos p = BlockPos.fromLong(placed[random.nextInt(placed.length)]);
            BlockState s = world.getBlockState(p);
            if (!s.isAir() && !s.hasBlockEntity() && s.isFullCube(world, p)) {
                world.setBlockState(p, forgotten[random.nextInt(forgotten.length)]);
            }
        }
    }

    /** Two creatures that stand too close merge into one that remembers being both. */
    static void fuse(ServerWorld world, ServerPlayerEntity player) {
        List<MobEntity> mobs = world.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(16), m -> !m.hasCustomName());
        for (MobEntity a : mobs) {
            for (MobEntity b : mobs) {
                if (a != b && a.isAlive() && b.isAlive() && a.squaredDistanceTo(b) < 9 && a.getType() != b.getType()) {
                    String name = cap(b.getType().getName().getString()) + "-" + cap(a.getType().getName().getString());
                    b.discard();
                    EntityAttributeInstance scale = a.getAttributeInstance(EntityAttributes.SCALE);
                    if (scale != null && !scale.hasModifier(FUSED)) {
                        scale.addPersistentModifier(new EntityAttributeModifier(FUSED, 0.4, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                    }
                    a.setCustomName(Text.literal(name).formatted(Formatting.LIGHT_PURPLE));
                    a.setHealth(a.getMaxHealth());
                    GlitchKind[] kinds = GlitchKind.values();
                    Glitches.apply(a, kinds[world.getRandom().nextInt(kinds.length)]);
                    world.spawnParticles(ParticleTypes.EXPLOSION, a.getX(), a.getBodyY(0.5), a.getZ(), 2, 0.2, 0.2, 0.2, 0);
                    return;
                }
            }
        }
    }

    static String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static void mutateChunk(ServerWorld world, ServerPlayerEntity player, Random random) {
        ChunkPos c = new ChunkPos(player.getBlockPos().add(random.nextInt(49) - 24, 0, random.nextInt(49) - 24));
        if (Reality.region(world).anchored(c.toLong())) {
            return;
        }
        Reality.paradox(world, c.getStartPos(), 20);
        for (int i = 0; i < 40; i++) {
            BlockPos column = new BlockPos(c.getStartX() + random.nextInt(16), 0, c.getStartZ() + random.nextInt(16));
            BlockPos top = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, column).down();
            BlockState s = world.getBlockState(top);
            if (s.isAir() || s.hasBlockEntity() || Reality.region(world).wasPlaced(top)) {
                continue;
            }
            world.setBlockState(top, switch (random.nextInt(4)) {
                case 0 -> dev.scripted.forgotten.registry.ModBlocks.UNREMEMBERED_BLOCK.getDefaultState();
                case 1 -> Blocks.CRYING_OBSIDIAN.getDefaultState();
                case 2 -> Blocks.SCULK.getDefaultState();
                default -> dev.scripted.forgotten.registry.ModBlocks.SKY_FRAGMENT.getDefaultState();
            });
        }
    }

    static void swap(ServerWorld world, ServerPlayerEntity player, Random random) {
        List<ServerPlayerEntity> others = new ArrayList<>(world.getPlayers(p -> p != player && !p.isSpectator()));
        if (others.isEmpty()) {
            return;
        }
        ServerPlayerEntity other = others.get(random.nextInt(others.size()));
        Vec3d a = player.getEntityPos();
        Vec3d b = other.getEntityPos();
        player.requestTeleport(b.x, b.y, b.z);
        other.requestTeleport(a.x, a.y, a.z);
        Texts.bar(player, Texts.world("You are standing where " + other.getName().getString() + " was."));
        Texts.bar(other, Texts.world("You are standing where " + player.getName().getString() + " was."));
    }

    static void adminGlitch(MinecraftServer server, Random random) {
        String[] commands = {"/gamerule doGravity false", "/time set yesterday", "/kill @e[type=sun]", "/tp @a ~ ~-1 ~",
                "/forget @p", "/give @a minecraft:the_concept_of_tuesday", "/execute as @world run sleep"};
        String cmd = commands[random.nextInt(commands.length)];
        server.getPlayerManager().broadcast(Text.literal("[Server: an operator who is not an operator] ").formatted(Formatting.GRAY, Formatting.ITALIC)
                .append(Text.literal(cmd).formatted(Formatting.WHITE)), false);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            Anomalies.gravityFlip(Reality.world(p), p, random);
        }
    }

    static void end(MinecraftServer server, WorldMemory memory) {
        boolean gravity = memory.storm.contains(StormKind.INVERTED_GRAVITY);
        memory.storm.clear();
        memory.markDirty();
        if (bar != null) {
            bar.clearPlayers();
            bar = null;
        }
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (gravity) {
                invert(p, false);
                for (LivingEntity e : Reality.world(p).getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(48), x -> true)) {
                    invert(e, false);
                }
            }
            Texts.bar(p, Texts.world("The storm passes. The rules settle, mostly."));
        }
        server.getOverworld().setWeather(6000, 0, false, false);
    }
}
