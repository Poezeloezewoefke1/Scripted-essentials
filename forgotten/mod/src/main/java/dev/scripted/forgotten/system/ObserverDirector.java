package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.logic.concept.Concept;
import dev.scripted.forgotten.logic.observer.ObserverMind;
import dev.scripted.forgotten.logic.paradox.Insight;
import dev.scripted.forgotten.player.PlayerMemory;
import dev.scripted.forgotten.registry.ModAttachments;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.util.Displays;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.WorldMemory;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Observer - also the Witness of the waking world: one entity that is not an entity.
 *
 * <p>It is a black, featureless block display: vanilla, unregistered by this mod, untargetable,
 * so inspecting it finds nothing. It only stands where the player is already looking, at the
 * distance its mind decides, near the places the player keeps returning to. Look away for long
 * enough and it is gone. Stare at it and the world stops for a moment. Leave something it has
 * never seen where it stood and it will take it. Shoot at it and the world resents you.
 */
public final class ObserverDirector {
    private static final Map<UUID, Sighting> ACTIVE = new HashMap<>();
    /** Where each player last saw it: gifts are left here. */
    private static final Map<UUID, Vec3d> LAST_SEEN = new HashMap<>();

    private record Sighting(DisplayEntity.BlockDisplayEntity body, Vec3d pos, long born, int[] unseen) {
    }

    private ObserverDirector() {
    }

    /** Every ten ticks, per player. */
    public static void tick(ServerWorld world, ServerPlayerEntity player, long now) {
        PlayerMemory memory = Reality.player(player);
        ObserverMind mind = memory.observer;
        WorldMemory wm = Reality.memory(world.getServer());
        Sighting s = ACTIVE.get(player.getUuid());
        acceptGifts(world, player, mind);
        if (s == null) {
            double chance = mind.appearanceChance(wm.lucidity.lucidity(), world.getPlayers().size() == 1);
            if (wm.ending() == dev.scripted.forgotten.logic.genesis.Ending.PRESERVE) {
                chance = 0;
            }
            if (world.getRandom().nextDouble() < chance) {
                appear(world, player, mind, now);
            }
            return;
        }
        if (s.body.isRemoved() || s.body.getEntityWorld() != world) {
            ACTIVE.remove(player.getUuid());
            return;
        }
        // Shot at?
        for (ProjectileEntity p : world.getEntitiesByClass(ProjectileEntity.class, new Box(BlockPos.ofFloored(s.pos)).expand(1.5, 3, 1.5), e -> true)) {
            if (p.getOwner() == player) {
                attacked(world, player, mind, now);
                vanish(player.getUuid());
                p.discard();
                return;
            }
        }
        Vec3d eye = player.getEyePos();
        Vec3d to = s.pos.add(0, 1.6, 0).subtract(eye);
        double distance = to.length();
        double dot = to.normalize().dotProduct(player.getRotationVector());
        if (distance < 8) {
            vanish(player.getUuid());
            return;
        }
        if (dot < 0.5) {
            s.unseen[0] += 10;
            if (s.unseen[0] > 40) {
                vanish(player.getUuid());
            }
            return;
        }
        s.unseen[0] = 0;
        boolean staring = dot > 0.995;
        for (int i = 0; i < 10; i++) {
            if (mind.stare(staring)) {
                freeze(world, player, memory, now);
                break;
            }
        }
    }

    static void appear(ServerWorld world, ServerPlayerEntity player, ObserverMind mind, long now) {
        Vec3d look = player.getRotationVector().multiply(1, 0, 1);
        if (look.lengthSquared() < 1e-4) {
            return;
        }
        look = look.normalize();
        double distance = mind.distance();
        // Bias toward a place the player keeps returning to, if one is roughly ahead.
        long haunt = mind.chooseHaunt(new java.util.SplittableRandom(world.getRandom().nextLong()), ChunkPos.toLong(player.getBlockPos()));
        ChunkPos hc = new ChunkPos(haunt);
        Vec3d toHaunt = new Vec3d(hc.getCenterX() - player.getX(), 0, hc.getCenterZ() - player.getZ());
        Vec3d dir = look;
        if (toHaunt.lengthSquared() > 1 && toHaunt.normalize().dotProduct(look) > 0.85) {
            dir = toHaunt.normalize();
            distance = Math.max(distance, Math.min(toHaunt.length(), 96));
        }
        Vec3d flat = player.getEntityPos().add(dir.multiply(distance));
        BlockPos column = BlockPos.ofFloored(flat);
        if (!world.isChunkLoaded(column)) {
            return;
        }
        BlockPos ground = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, column);
        Vec3d pos = Vec3d.ofBottomCenter(ground);
        DisplayEntity.BlockDisplayEntity body = Displays.block(world, pos, Blocks.BLACK_CONCRETE.getDefaultState(), 0.55f, 2.9f, 0.55f, true);
        if (body == null) {
            return;
        }
        body.setViewRange(6.0f);
        ACTIVE.put(player.getUuid(), new Sighting(body, pos, now, new int[1]));
        LAST_SEEN.put(player.getUuid(), pos);
        mind.appeared();
        if (Reality.player(player).setFlag("observer_seen")) {
            Reality.chronicle(world, ChronicleEvent.ANOMALY_WITNESSED, ground, player.getName().getString(), "observer");
        }
    }

    static void vanish(UUID player) {
        Sighting s = ACTIVE.remove(player);
        if (s != null) {
            s.body.discard();
        }
    }

    /** Staring long enough stops the world: creatures freeze, sounds stop, and you see something that is not an item. */
    static void freeze(ServerWorld world, ServerPlayerEntity player, PlayerMemory memory, long now) {
        for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(24), m -> !m.isAiDisabled())) {
            mob.setAiDisabled(true);
            mob.setAttached(ModAttachments.FROZEN_UNTIL, now + 60);
        }
        player.networkHandler.sendPacket(new StopSoundS2CPacket(null, null));
        Texts.bar(player, Text.literal("[").formatted(Formatting.WHITE).append(Texts.glitch("an item that does not exist"))
                .append(Text.literal("]").formatted(Formatting.WHITE)));
        world.spawnParticles(player, ParticleTypes.WHITE_ASH, true, false, player.getX(), player.getY() + 1, player.getZ(), 200, 6, 3, 6, 0);
        Reality.insight(player, Insight.OBSERVER_STARED);
        if (memory.setFlag("observer_stare") || world.getRandom().nextInt(3) == 0) {
            ItemStack witness = new ItemStack(ModItems.concept(Concept.WITNESS));
            if (!player.giveItemStack(witness)) {
                player.dropItem(witness, false);
            }
        }
        if (world.getRandom().nextInt(4) == 0) {
            ItemStack silence = new ItemStack(ModItems.concept(Concept.SILENCE));
            if (!player.giveItemStack(silence)) {
                player.dropItem(silence, false);
            }
        }
    }

    static void attacked(ServerWorld world, ServerPlayerEntity player, ObserverMind mind, long now) {
        mind.attacked();
        WorldMemory wm = Reality.memory(world.getServer());
        wm.lucidity.resent(now);
        wm.markDirty();
        Reality.chronicle(world, ChronicleEvent.ANOMALY_WITNESSED, player.getBlockPos(), player.getName().getString(), "attacked the observer");
        Texts.tell(player, Texts.world("Nothing was hit. Everything noticed. The world will not wake any further for a long time."));
    }

    /** Things the player leaves where it stood are taken, if it has never seen them before. */
    static void acceptGifts(ServerWorld world, ServerPlayerEntity player, ObserverMind mind) {
        Vec3d spot = LAST_SEEN.get(player.getUuid());
        if (spot == null || mind.hostile()) {
            return;
        }
        for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, new Box(BlockPos.ofFloored(spot)).expand(2), e -> true)) {
            String id = Registries.ITEM.getId(item.getStack().getItem()).toString();
            item.discard();
            world.spawnParticles(ParticleTypes.SCULK_SOUL, item.getX(), item.getY() + 0.3, item.getZ(), 8, 0.2, 0.2, 0.2, 0.02);
            if (Reality.player(player).setFlag("gifted_" + id)) {
                int gain = mind.gift(id, world.getSeed());
                Texts.bar(player, Texts.world("It is gone. Something accepted it."));
                if (mind.grantsPassage() && Reality.player(player).setFlag("witness_trust")) {
                    Texts.tell(player, Texts.discovery("THE WITNESS TRUSTS YOU."));
                    Texts.tell(player, Texts.world("Sleep through a storm. It will show you where the world dreams."));
                    Reality.advance(player, "witness");
                }
                if (gain > 0 && world.getRandom().nextInt(5) == 0) {
                    Block.dropStack(world, BlockPos.ofFloored(spot), new ItemStack(ModItems.concept(Concept.WITNESS)));
                }
            }
        }
    }

    public static void clear() {
        for (Sighting s : ACTIVE.values()) {
            s.body.discard();
        }
        ACTIVE.clear();
        LAST_SEEN.clear();
    }
}
