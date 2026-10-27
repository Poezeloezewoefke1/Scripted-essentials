package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.chronicle.ChronicleEntry;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.logic.concept.Concept;
import dev.scripted.forgotten.logic.integrity.IntegrityStage;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.util.Displays;
import dev.scripted.forgotten.util.Scheduler;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.RegionMemory;
import dev.scripted.forgotten.world.WorldMemory;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.decoration.MannequinEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

/**
 * The world remembering out loud. Nothing here is a recording: it is the world, half awake,
 * putting back things it thinks should still exist - for a few seconds, or for good.
 */
public final class MemoryBleed {
    public static final int GHOST_LIMIT = 96;

    private MemoryBleed() {
    }

    /** Every five seconds, per player. */
    public static void tick(ServerWorld world, ServerPlayerEntity player, long now) {
        double lucidity = Reality.lucidity(world.getServer());
        IntegrityStage stage = Reality.stage(world, player.getBlockPos());
        if (lucidity < 30 && !stage.atLeast(IntegrityStage.DEJA_VU)) {
            return;
        }
        if (now < Reality.player(player).mercyUntil) {
            return;
        }
        Random random = world.getRandom();
        int roll = random.nextInt(12);
        switch (roll) {
            case 0 -> ruinGhost(world, player, random);
            case 1 -> deathEcho(world, player, random);
            case 2 -> {
                if (world.isNight()) {
                    villageFlicker(world, player);
                }
            }
            case 3 -> namedReturns(world, player, now);
            case 4 -> {
                if (lucidity >= 40) {
                    forgottenThingsReturn(world, player, random, now);
                }
            }
            default -> {
            }
        }
    }

    /** A demolished building flickers back for three seconds, glowing, exactly as it was. */
    static void ruinGhost(ServerWorld world, ServerPlayerEntity player, Random random) {
        RegionMemory region = Reality.region(world);
        ChunkPos c = player.getChunkPos();
        List<RegionMemory.Remembered> ruins = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                RegionMemory.ChunkRecord r = region.peek(ChunkPos.toLong(c.x + dx, c.z + dz));
                if (r != null) {
                    ruins.addAll(r.ruins);
                }
            }
        }
        if (ruins.size() < 6) {
            return;
        }
        List<DisplayEntity.BlockDisplayEntity> ghosts = new ArrayList<>();
        for (RegionMemory.Remembered m : ruins.subList(Math.max(0, ruins.size() - GHOST_LIMIT), ruins.size())) {
            if (world.getBlockState(m.blockPos()).isAir()) {
                DisplayEntity.BlockDisplayEntity g = Displays.ghost(world, m.blockPos(), m.state());
                if (g != null) {
                    ghosts.add(g);
                }
            }
        }
        if (ghosts.isEmpty()) {
            return;
        }
        Scheduler.after(60, () -> ghosts.forEach(Entity::discard));
        if (Reality.player(player).setFlag("seen_ruin_ghost")) {
            Texts.tell(player, Texts.world("For three seconds, something you tore down is standing again."));
        }
        if (random.nextInt(5) == 0) {
            net.minecraft.block.Block.dropStack(world, player.getBlockPos(), new ItemStack(ModItems.concept(Concept.MEMORY)));
        }
    }

    /** Someone who died here dies here again, silently, for a moment. */
    static void deathEcho(ServerWorld world, ServerPlayerEntity player, Random random) {
        List<ChronicleEntry> deaths = Reality.memory(world.getServer()).chronicle.near(world.getRegistryKey().getValue().toString(),
                player.getBlockX(), player.getBlockY(), player.getBlockZ(), 32, ChronicleEvent.PLAYER_DIED);
        if (deaths.isEmpty()) {
            return;
        }
        ChronicleEntry death = deaths.get(random.nextInt(deaths.size()));
        MannequinEntity ghost = EntityType.MANNEQUIN.create(world, SpawnReason.EVENT);
        if (ghost == null) {
            return;
        }
        ghost.setMannequinProfile(ProfileComponent.ofDynamic(death.actor()));
        ghost.refreshPositionAndAngles(death.x() + 0.5, death.y(), death.z() + 0.5, random.nextFloat() * 360, 0);
        ghost.setInvulnerable(true);
        ghost.setNoGravity(true);
        ghost.setGlowing(true);
        ghost.setCustomName(Text.literal(death.actor() + " - " + death.detail().split("\\|")[0]).formatted(Formatting.GRAY, Formatting.ITALIC));
        world.spawnEntity(ghost);
        Scheduler.after(50, () -> {
            world.spawnParticles(ParticleTypes.SOUL, ghost.getX(), ghost.getY() + 1, ghost.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
            ghost.discard();
        });
    }

    /** Villages that were lost flicker back into existence at night. */
    static void villageFlicker(ServerWorld world, ServerPlayerEntity player) {
        List<ChronicleEntry> lost = Reality.memory(world.getServer()).chronicle.near(world.getRegistryKey().getValue().toString(),
                player.getBlockX(), player.getBlockY(), player.getBlockZ(), 48, ChronicleEvent.VILLAGER_DIED);
        List<Entity> ghosts = new ArrayList<>();
        for (ChronicleEntry e : lost.subList(0, Math.min(6, lost.size()))) {
            VillagerEntity v = EntityType.VILLAGER.create(world, SpawnReason.EVENT);
            if (v == null) {
                continue;
            }
            v.refreshPositionAndAngles(e.x() + 0.5, e.y(), e.z() + 0.5, 0, 0);
            v.setAiDisabled(true);
            v.setInvulnerable(true);
            v.setSilent(true);
            v.setGlowing(true);
            world.spawnEntity(v);
            ghosts.add(v);
        }
        if (!ghosts.isEmpty()) {
            Scheduler.after(100, () -> ghosts.forEach(Entity::discard));
        }
    }

    /** The world does not understand deletion. Named creatures that died come back. */
    static void namedReturns(ServerWorld world, ServerPlayerEntity player, long now) {
        WorldMemory memory = Reality.memory(world.getServer());
        for (ChronicleEntry e : memory.chronicle.near(world.getRegistryKey().getValue().toString(),
                player.getBlockX(), player.getBlockY(), player.getBlockZ(), 64, ChronicleEvent.NAMED_MOB_DIED)) {
            if (now - e.tick() < 72000 || !memory.setFlag("returned_" + e.tick() + "_" + e.x() + "_" + e.z())) {
                continue;
            }
            String[] p = e.detail().split("\\|");
            if (p.length < 2) {
                continue;
            }
            Identifier type = Identifier.tryParse(p[1]);
            if (type == null || !Registries.ENTITY_TYPE.containsId(type)) {
                continue;
            }
            Entity back = Registries.ENTITY_TYPE.get(type).create(world, SpawnReason.EVENT);
            if (back instanceof MobEntity mob) {
                mob.refreshPositionAndAngles(e.x() + 0.5, e.y(), e.z() + 0.5, 0, 0);
                mob.setCustomName(Text.literal(p[0]));
                mob.setPersistent();
                world.spawnEntity(mob);
                world.playSound(null, e.x(), e.y(), e.z(), SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.NEUTRAL, 0.6f, 1.4f);
                Texts.tell(player, Texts.world(p[0] + " is back. The world never understood that they were gone."));
                return;
            }
        }
    }

    /** Items destroyed long ago turn up again, in a chest in a cave nearby. */
    static void forgottenThingsReturn(ServerWorld world, ServerPlayerEntity player, Random random, long now) {
        WorldMemory memory = Reality.memory(world.getServer());
        List<ChronicleEntry> lost = memory.chronicle.find(e -> e.event() == ChronicleEvent.ITEM_DESTROYED && now - e.tick() > 48000);
        if (lost.isEmpty()) {
            return;
        }
        ChronicleEntry e = lost.get(random.nextInt(lost.size()));
        if (!memory.setFlag("restored_item_" + e.tick())) {
            return;
        }
        BlockPos cave = Anomalies.find(world, player.getBlockPos().down(12), 10, 400, random, s -> s.isAir());
        if (cave == null || world.isSkyVisible(cave) || !world.getBlockState(cave.down()).isIn(BlockTags.BASE_STONE_OVERWORLD)) {
            return;
        }
        String[] p = e.detail().split("\\*");
        Identifier id = Identifier.tryParse(p[0]);
        if (id == null || !Registries.ITEM.containsId(id)) {
            return;
        }
        Item item = Registries.ITEM.get(id);
        world.setBlockState(cave, Blocks.CHEST.getDefaultState());
        if (world.getBlockEntity(cave) instanceof ChestBlockEntity chest) {
            chest.setStack(13, new ItemStack(item, Math.min(item.getMaxCount(), p.length > 1 ? Integer.parseInt(p[1]) : 1)));
        }
        Texts.bar(player, Texts.world("Somewhere below you, something that was destroyed has been put back."));
    }
}
