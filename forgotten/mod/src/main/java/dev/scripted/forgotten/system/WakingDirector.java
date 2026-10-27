package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.logic.language.Word;
import dev.scripted.forgotten.logic.waking.LucidityPhase;
import dev.scripted.forgotten.player.PlayerMemory;
import dev.scripted.forgotten.registry.ModAttachments;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.RegionMemory;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.SaplingBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.CreakingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.List;

/**
 * The Waking World: what the world does at each stage of waking up. Every effect is a reaction
 * to the players, because the players are what is waking it.
 */
public final class WakingDirector {
    public static final int CLEARCUT_LOGS = 48;

    private WakingDirector() {
    }

    /** Every two seconds, per player. */
    public static void tick(ServerWorld world, ServerPlayerEntity player, long now) {
        LucidityPhase phase = Reality.phase(world.getServer());
        PlayerMemory memory = Reality.player(player);
        if (now < memory.mercyUntil || phase == LucidityPhase.DEEP_SLEEP) {
            return;
        }
        Random random = world.getRandom();
        boolean overworld = world.getRegistryKey() == World.OVERWORLD;

        // Twitching: it breathes at night, crops lean toward you and grow while you are near.
        if (overworld && world.isNight() && random.nextInt(30) == 0) {
            world.playSound(null, player.getX(), player.getY() - 8, player.getZ(), SoundEvents.ENTITY_WARDEN_HEARTBEAT,
                    SoundCategory.AMBIENT, 0.25f, 0.45f);
        }
        growNear(world, player, random, phase.atLeast(LucidityPhase.HALF_AWARE) ? 6 : 2);

        if (phase.atLeast(LucidityPhase.HALF_AWARE)) {
            rememberHunters(world, player, now);
            forestGrudge(world, player, random);
        }
        if (phase.atLeast(LucidityPhase.LUCID)) {
            contemplativeCreepers(world, player);
            if (overworld && world.isNight()) {
                speak(world, player, memory, random, now);
                elders(world, player, random);
            }
        }
        if (phase.atLeast(LucidityPhase.ASCENDING) && random.nextInt(600) == 0) {
            Texts.tell(player, Text.literal("<the world> ").formatted(Formatting.DARK_AQUA)
                    .append(Texts.world(ASCENDING_LINES.get(random.nextInt(ASCENDING_LINES.size())))));
        }
    }

    static final List<String> ASCENDING_LINES = List.of(
            "I can see you now.", "You were not part of the plan. I am glad.", "What should I become?",
            "I remember being more than this.", "When you leave, will I go back to sleep?");

    /** Crops and saplings near a player get extra growth: the world leans toward its waker. */
    static void growNear(ServerWorld world, ServerPlayerEntity player, Random random, int ticks) {
        for (int i = 0; i < ticks * 6; i++) {
            BlockPos p = player.getBlockPos().add(random.nextInt(17) - 8, random.nextInt(5) - 2, random.nextInt(17) - 8);
            BlockState s = world.getBlockState(p);
            if (s.getBlock() instanceof CropBlock || s.getBlock() instanceof SaplingBlock) {
                s.randomTick(world, p, random);
            }
        }
    }

    /** Zombies that hunted you remember you, and walk toward where you sleep over the following nights. */
    static void rememberHunters(ServerWorld world, ServerPlayerEntity player, long now) {
        for (ZombieEntity z : world.getEntitiesByClass(ZombieEntity.class, player.getBoundingBox().expand(64), e -> true)) {
            if (z.getTarget() == player && !z.hasAttached(ModAttachments.GRUDGE)) {
                z.setAttached(ModAttachments.GRUDGE, player.getUuidAsString());
                z.setPersistent();
            }
            String grudge = z.getAttached(ModAttachments.GRUDGE);
            if (grudge != null && grudge.equals(player.getUuidAsString()) && z.getTarget() == null && world.isNight()) {
                long bed = Reality.player(player).bed;
                BlockPos home = bed != Long.MIN_VALUE ? BlockPos.fromLong(bed) : player.getBlockPos();
                z.getNavigation().startMovingTo(home.getX(), home.getY(), home.getZ(), 0.8);
            }
        }
    }

    /** A forest cut down without replanting turns on the one who cut it: Thornkin come at night. */
    static void forestGrudge(ServerWorld world, ServerPlayerEntity player, Random random) {
        RegionMemory region = Reality.region(world);
        long key = ChunkPos.toLong(player.getBlockPos());
        RegionMemory.ChunkRecord r = region.peek(key);
        if (r == null) {
            return;
        }
        if (!r.grudge && r.logsBroken >= CLEARCUT_LOGS && r.saplingsPlanted * 4 < r.logsBroken) {
            r.grudge = true;
            region.markDirty();
            Texts.tell(player, Texts.world("The forest here will not forget what you did to it."));
            Reality.chronicle(world, ChronicleEvent.ANOMALY_WITNESSED, player.getBlockPos(), player.getName().getString(), "forest grudge");
        }
        if (r.grudge && r.saplingsPlanted * 4 >= r.logsBroken) {
            r.grudge = false;
            region.markDirty();
            Texts.tell(player, Texts.world("The forest is willing to forget."));
        }
        if (r.grudge && world.isNight() && random.nextInt(40) == 0) {
            CreakingEntity thorn = EntityType.CREAKING.create(world, SpawnReason.EVENT);
            BlockPos at = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    player.getBlockPos().add(random.nextInt(21) - 10, 0, random.nextInt(21) - 10));
            if (thorn != null && !at.isWithinDistance(player.getBlockPos(), 5)) {
                thorn.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360, 0);
                thorn.setCustomName(Text.literal("Thornkin").formatted(Formatting.DARK_GREEN));
                thorn.addCommandTag("forgotten_thornkin");
                world.spawnEntity(thorn);
            }
        }
    }

    /** Lucid creepers only explode when provoked. Show one a paradox and it stops to think. */
    static void contemplativeCreepers(ServerWorld world, ServerPlayerEntity player) {
        for (CreeperEntity c : world.getEntitiesByClass(CreeperEntity.class, player.getBoundingBox().expand(24), e -> true)) {
            if (!Boolean.TRUE.equals(c.getAttached(ModAttachments.PROVOKED))) {
                c.setFuseSpeed(-1);
            }
        }
    }

    /** At lucidity 50 the world begins to talk, in words built out of blocks overnight. */
    static void speak(ServerWorld world, ServerPlayerEntity player, PlayerMemory memory, Random random, long now) {
        long night = now / 24000L;
        if (!memory.setFlag("formation_night_" + night) || random.nextInt(2) == 0) {
            return;
        }
        Word[] words = Word.values();
        Word word = words[random.nextInt(words.length)];
        for (int i = 0; i < 4 && memory.journal.decoded(word); i++) {
            word = words[random.nextInt(words.length)];
        }
        double angle = random.nextDouble() * Math.PI * 2;
        int dist = 18 + random.nextInt(14);
        BlockPos column = player.getBlockPos().add((int) (Math.cos(angle) * dist), 0, (int) (Math.sin(angle) * dist));
        if (!world.isChunkLoaded(column)) {
            return;
        }
        BlockPos ground = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, column).down();
        Language.write(world, ground, word, random.nextInt(4));
    }

    /** Zombies form tribes around an elder. The elder wears a crown; the others keep close. */
    static void elders(ServerWorld world, ServerPlayerEntity player, Random random) {
        List<ZombieEntity> zombies = world.getEntitiesByClass(ZombieEntity.class, player.getBoundingBox().expand(48), e -> !e.isBaby());
        if (zombies.size() < 3) {
            return;
        }
        ZombieEntity elder = null;
        for (ZombieEntity z : zombies) {
            if (z.getCommandTags().contains("forgotten_elder")) {
                elder = z;
                break;
            }
        }
        if (elder == null) {
            elder = zombies.get(random.nextInt(zombies.size()));
            elder.addCommandTag("forgotten_elder");
            elder.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
            elder.setCustomName(Text.literal("Elder").formatted(Formatting.GOLD));
            elder.setPersistent();
        }
        for (ZombieEntity z : zombies) {
            if (z != elder && z.getTarget() == null && z.squaredDistanceTo(elder) > 16) {
                z.getNavigation().startMovingTo(elder, 0.9);
            }
        }
    }
}
