package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Forgotten;
import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.dimension.Dimensions;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.logic.integrity.IntegrityStage;
import dev.scripted.forgotten.logic.paradox.Insight;
import dev.scripted.forgotten.player.PlayerMemory;
import dev.scripted.forgotten.registry.ModBlocks;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.util.Texts;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The ways between worlds. Every dimension is reached by doing something the world does not
 * expect: falling through ground that has stopped being solid (the Backside, and below it the
 * Unwritten), feeding a portal a loop that never ends (the Stack), sleeping through a storm
 * (the Underdream), holding a memory older than the world (the First World).
 */
public final class Rifts {
    public static final Identifier STACK_GRAVITY = Forgotten.id("stack_gravity");
    private static final Map<UUID, Long> COOLDOWN = new HashMap<>();

    private Rifts() {
    }

    public static ServerWorld world(MinecraftServer server, RegistryKey<World> key) {
        return server.getWorld(key);
    }

    // ---- return points ----

    public static String point(ServerWorld world, Vec3d pos) {
        return world.getRegistryKey().getValue() + "|" + (int) Math.floor(pos.x) + "|" + (int) Math.floor(pos.y) + "|" + (int) Math.floor(pos.z);
    }

    /** Goes back to an encoded point; falls back to the Overworld spawn if it no longer exists. */
    public static void goTo(ServerPlayerEntity player, String point) {
        MinecraftServer server = Reality.world(player).getServer();
        String[] f = point == null ? new String[0] : point.split("\\|");
        if (f.length == 4) {
            Identifier id = Identifier.tryParse(f[0]);
            ServerWorld target = id == null ? null : server.getWorld(RegistryKey.of(RegistryKeys.WORLD, id));
            if (target != null) {
                try {
                    Vec3d at = new Vec3d(Integer.parseInt(f[1]) + 0.5, Integer.parseInt(f[2]), Integer.parseInt(f[3]) + 0.5);
                    target.getChunk(BlockPos.ofFloored(at));
                    Travel.teleport(player, target, at, player.getYaw(), player.getPitch());
                    return;
                } catch (NumberFormatException ignored) {
                    // fall through to spawn
                }
            }
        }
        home(player);
    }

    /** The Overworld, at the player's bed if they have one there, else world spawn. */
    public static void home(ServerPlayerEntity player) {
        ServerWorld overworld = Reality.world(player).getServer().getOverworld();
        PlayerMemory memory = Reality.player(player);
        BlockPos at = memory.bed != Long.MIN_VALUE ? BlockPos.fromLong(memory.bed) : overworld.getSpawnPoint().getPos();
        Vec3d surface = Travel.surface(overworld, at.getX(), at.getZ());
        Travel.teleport(player, overworld, surface, player.getYaw(), player.getPitch());
    }

    static boolean cooling(Entity entity, long now) {
        Long last = COOLDOWN.get(entity.getUuid());
        if (last != null && now - last < 100) {
            return true;
        }
        COOLDOWN.put(entity.getUuid(), now);
        return false;
    }

    private static void arrive(ServerPlayerEntity player, ServerWorld world, String title, String line) {
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.PLAYERS, 0.4f, 1.6f);
        Texts.tell(player, Texts.discovery(title));
        Texts.tell(player, Texts.world(line));
        Reality.insight(player, Insight.DIMENSION_DISCOVERED);
        Reality.chronicle(world, ChronicleEvent.DIMENSION_ENTERED, player.getBlockPos(), player.getName().getString(),
                world.getRegistryKey().getValue().toString());
    }

    /** A safe place to stand: the surface if there is one, otherwise a small platform at {@code y}. */
    public static Vec3d landing(ServerWorld world, int x, int z, int y, BlockState platform) {
        Vec3d surface = Travel.surface(world, x, z);
        if (world.getBlockState(BlockPos.ofFloored(surface).down()).isAir()) {
            BlockPos base = new BlockPos(x, y, z);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (world.getBlockState(base.add(dx, -1, dz)).isAir()) {
                        world.setBlockState(base.add(dx, -1, dz), platform, Block.NOTIFY_ALL);
                    }
                }
            }
            return Vec3d.ofBottomCenter(base);
        }
        return surface;
    }

    // ---- falling through ----

    /**
     * Called every tick per player. Below the bottom of a world that has stopped being solid
     * is the Backside; below the Backside is the Unwritten; below the Unwritten is home.
     */
    public static void tickPlayer(ServerPlayerEntity player, long now) {
        ServerWorld world = Reality.world(player);
        if (player.getY() > world.getBottomY() - 8 || player.isSpectator()) {
            return;
        }
        RegistryKey<World> key = world.getRegistryKey();
        MinecraftServer server = world.getServer();
        if (key == Dimensions.UNWRITTEN) {
            home(player);
            player.fallDistance = 0;
            Texts.tell(player, Texts.world("You fall out of the bottom of nothing and land somewhere you remember."));
            return;
        }
        if (key == Dimensions.BACKSIDE) {
            ServerWorld unwritten = world(server, Dimensions.UNWRITTEN);
            if (unwritten != null) {
                Vec3d at = landing(unwritten, player.getBlockX(), player.getBlockZ(), 100, ModBlocks.UNWRITTEN_STONE.getDefaultState());
                Travel.teleport(player, unwritten, at, player.getYaw(), player.getPitch());
                player.fallDistance = 0;
                arrive(player, unwritten, "THE UNWRITTEN",
                        "Nothing here has been decided yet. Ground exists only where you stand on it.");
            }
            return;
        }
        if (Reality.stage(world, player.getBlockPos().withY(world.getBottomY() + 1)).atLeast(IntegrityStage.AMNESIA)) {
            fallThrough(player);
        }
    }

    /**
     * Through ground that has stopped being solid, into the Backside: from the Overworld, the
     * First World or a depth. Null blocks in forgotten chunks lead here too.
     */
    public static boolean fallThrough(ServerPlayerEntity player) {
        ServerWorld world = Reality.world(player);
        RegistryKey<World> key = world.getRegistryKey();
        if (!(key == World.OVERWORLD || key == Dimensions.FIRST_WORLD || Dimensions.isDepth(world))) {
            return false;
        }
        ServerWorld backside = world(world.getServer(), Dimensions.BACKSIDE);
        if (backside == null || cooling(player, Reality.now(world.getServer()))) {
            return false;
        }
        Vec3d at = Travel.surface(backside, player.getBlockX(), player.getBlockZ());
        Travel.teleport(player, backside, at, player.getYaw(), player.getPitch());
        player.fallDistance = 0;
        arrive(player, backside, "THE BACKSIDE",
                "The same world, seen from behind. Find a bed to wake up; fall again and you go further.");
        Reality.advance(player, "the_backside");
        return true;
    }

    /** In the Backside, any bed wakes you at home. */
    public static boolean useBedInBackside(ServerPlayerEntity player) {
        if (Reality.world(player).getRegistryKey() != Dimensions.BACKSIDE) {
            return false;
        }
        home(player);
        Texts.tell(player, Texts.world("You were only dreaming that you fell."));
        return true;
    }

    // ---- the Stack ----

    /**
     * The Stack opens for an Infinite Loop thrown onto obsidian ringed by bookshelves: the
     * obsidian becomes a rift. Checked once a second for loop items near players.
     */
    public static void checkLoops(ServerWorld world, ServerPlayerEntity player) {
        for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, player.getBoundingBox().expand(16),
                e -> e.getStack().isOf(ModItems.INFINITE_LOOP) && e.isOnGround())) {
            BlockPos under = item.getBlockPos().down();
            if (!world.getBlockState(under).isOf(Blocks.OBSIDIAN) || bookshelves(world, under) < 8) {
                continue;
            }
            item.discard();
            world.setBlockState(under, ModBlocks.RIFT.getDefaultState(), Block.NOTIFY_ALL);
            world.playSound(null, under, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 1f, 1.8f);
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, under.getX() + 0.5, under.getY() + 1, under.getZ() + 0.5, 80, 0.5, 1, 0.5, 0.1);
            Texts.bar(player, Texts.glitch("StackOverflowError"));
        }
    }

    static int bookshelves(ServerWorld world, BlockPos center) {
        int n = 0;
        for (BlockPos p : BlockPos.iterate(center.add(-2, 0, -2), center.add(2, 2, 2))) {
            if (world.getBlockState(p).isOf(Blocks.BOOKSHELF) || world.getBlockState(p).isOf(Blocks.CHISELED_BOOKSHELF)) {
                n++;
            }
        }
        return n;
    }

    /** Stepping into a rift: into the Stack from anywhere, or back out of it. */
    public static void enterRift(ServerPlayerEntity player, BlockPos rift) {
        ServerWorld world = Reality.world(player);
        MinecraftServer server = world.getServer();
        if (cooling(player, Reality.now(server))) {
            return;
        }
        if (world.getRegistryKey() == Dimensions.STACK) {
            goTo(player, Reality.player(player).returnPoint);
            return;
        }
        ServerWorld stack = world(server, Dimensions.STACK);
        if (stack == null) {
            return;
        }
        Reality.player(player).returnPoint = point(world, Vec3d.ofBottomCenter(rift.up()).add(1.5, 0, 0));
        Vec3d at = landing(stack, rift.getX(), rift.getZ(), 120, ModBlocks.DATA_BLOCK.getDefaultState());
        Travel.teleport(player, stack, at, player.getYaw(), player.getPitch());
        BlockPos back = BlockPos.ofFloored(at).add(2, -1, 0);
        stack.setBlockState(back, ModBlocks.RIFT.getDefaultState(), Block.NOTIFY_ALL);
        arrive(player, stack, "THE STACK", "Memory nobody is reading. Gravity here depends on where you are. The rift beside you leads back.");
        Reality.advance(player, "the_stack");
    }

    /** In the Stack, gravity is a function of your coordinates, recomputed as you move. */
    public static void stackGravity(ServerPlayerEntity player) {
        EntityAttributeInstance gravity = player.getAttributeInstance(EntityAttributes.GRAVITY);
        if (gravity == null) {
            return;
        }
        boolean inStack = Reality.world(player).getRegistryKey() == Dimensions.STACK;
        double wanted = inStack ? switch (Math.floorMod((player.getBlockX() >> 4) + (player.getBlockZ() >> 4), 4)) {
            case 0 -> 0.0;
            case 1 -> -0.04;
            case 2 -> -0.07;
            default -> 0.04;
        } : 0.0;
        EntityAttributeModifier current = gravity.getModifier(STACK_GRAVITY);
        double have = current == null ? 0.0 : current.value();
        if (have == wanted) {
            return;
        }
        gravity.removeModifier(STACK_GRAVITY);
        if (wanted != 0.0) {
            gravity.addTemporaryModifier(new EntityAttributeModifier(STACK_GRAVITY, wanted, EntityAttributeModifier.Operation.ADD_VALUE));
            Texts.bar(player, Texts.info("gravity = " + String.format("%.2f", 0.08 + wanted)));
        }
    }

    // ---- the Underdream ----

    /**
     * Waking from sleep during a thunderstorm - or at all, once the Observer trusts you - takes
     * you down instead of up.
     */
    public static boolean dreamDown(ServerPlayerEntity player) {
        ServerWorld world = Reality.world(player);
        if (world.getRegistryKey() == Dimensions.UNDERDREAM) {
            goTo(player, Reality.player(player).returnPoint);
            Texts.tell(player, Texts.world("You wake. Something of the Underdream is still on your hands."));
            return true;
        }
        boolean invited = world.isThundering() || Reality.player(player).observer.grantsPassage()
                || Reality.memory(world.getServer()).rule(dev.scripted.forgotten.logic.genesis.GenesisRule.WORLD_DREAMS);
        if (world.getRegistryKey() != World.OVERWORLD || !invited) {
            return false;
        }
        ServerWorld dream = world(world.getServer(), Dimensions.UNDERDREAM);
        if (dream == null) {
            return false;
        }
        Reality.player(player).returnPoint = point(world, player.getEntityPos());
        Vec3d at = landing(dream, player.getBlockX(), player.getBlockZ(), 140, ModBlocks.MARROWGLASS.getDefaultState());
        Travel.teleport(player, dream, at, player.getYaw(), player.getPitch());
        arrive(player, dream, "THE UNDERDREAM", "Beneath the world's sleep. The bones of it are glass. Sleep again to wake.");
        Reality.advance(player, "the_underdream");
        return true;
    }

    // ---- the First World ----

    /** The Memory of the First World, used: go there, or come back. */
    public static void firstWorld(ServerPlayerEntity player) {
        ServerWorld world = Reality.world(player);
        if (world.getRegistryKey() == Dimensions.FIRST_WORLD) {
            goTo(player, Reality.player(player).returnPoint);
            return;
        }
        ServerWorld first = world(world.getServer(), Dimensions.FIRST_WORLD);
        if (first == null) {
            return;
        }
        Reality.player(player).returnPoint = point(world, player.getEntityPos());
        Vec3d at = Travel.surface(first, player.getBlockX(), player.getBlockZ());
        Travel.teleport(player, first, at, player.getYaw(), player.getPitch());
        Genesis.revealBeginning(player);
        arrive(player, first, "THE FIRST WORLD", "Your world, before anything happened to it. Nothing you built is here. Nothing you broke is broken.");
        Reality.advance(player, "the_first_world");
    }

    public static void clear() {
        COOLDOWN.clear();
    }

}
