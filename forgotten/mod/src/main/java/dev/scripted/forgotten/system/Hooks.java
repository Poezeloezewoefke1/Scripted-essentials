package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.command.ForgottenCommand;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.logic.concept.Concept;
import dev.scripted.forgotten.logic.integrity.IntegrityStage;
import dev.scripted.forgotten.logic.paradox.CausalDebt;
import dev.scripted.forgotten.player.PlayerMemory;
import dev.scripted.forgotten.registry.ModBlocks;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.util.Scheduler;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.RegionMemory;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.SaplingBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Wires the mod into the game: every event that feeds the world's memory, and the tick loop.
 * The handlers are thin; each forwards to the system that owns the behaviour.
 */
public final class Hooks {
    /** Recent positions per player, for rewinding a rejected death. Transient. */
    private static final Map<UUID, ArrayDeque<Vec3d>> TRAIL = new HashMap<>();

    private Hooks() {
    }

    public static void init() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (state.isOf(ModBlocks.UNWRITTEN_STONE) && world instanceof ServerWorld server && !player.isCreative()
                    && !Reality.storming(server.getServer())) {
                Texts.bar((ServerPlayerEntity) player, Texts.world("It does not exist firmly enough to break. Not until the rules loosen."));
                return false;
            }
            return true;
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerWorld server && player instanceof ServerPlayerEntity sp) {
                broken(server, sp, pos.toImmutable(), state);
            }
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world instanceof ServerWorld server && player instanceof ServerPlayerEntity sp) {
                BlockState state = world.getBlockState(hit.getBlockPos());
                if (state.getBlock() instanceof DoorBlock && Anomalies.doorElsewhere(server, sp, hit.getBlockPos())) {
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });
        ServerLivingEntityEvents.ALLOW_DEATH.register(Hooks::allowDeath);
        ServerLivingEntityEvents.AFTER_DEATH.register(Hooks::died);
        EntitySleepEvents.STOP_SLEEPING.register((entity, sleepingPos) -> {
            if (entity instanceof ServerPlayerEntity player) {
                woke(player, sleepingPos);
            }
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> Glitches.track(entity));
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            Glitches.untrack(entity);
            if (entity instanceof ItemEntity item && entity.getRemovalReason() == Entity.RemovalReason.KILLED) {
                ItemStack stack = item.getStack();
                if (!stack.isEmpty() && (stack.getRarity().ordinal() > 0 || stack.hasEnchantments() || stack.getCount() >= 16)) {
                    Reality.chronicle(world, ChronicleEvent.ITEM_DESTROYED, entity.getBlockPos(), "",
                            Registries.ITEM.getId(stack.getItem()) + "*" + stack.getCount());
                }
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(Directors::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            Scheduler.clear();
            TRAIL.clear();
            ObserverDirector.clear();
            FirstSigns.reset();
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ForgottenCommand.register(dispatcher));
    }

    // ---- building and breaking ----

    public static void placed(ServerWorld world, PlayerEntity player, BlockPos pos, BlockState state) {
        RegionMemory region = Reality.region(world);
        region.placed(pos);
        Reality.count(world, ChronicleEvent.BLOCK_PLACED, Registries.BLOCK.getId(state.getBlock()).toString());
        if (state.getBlock() instanceof SaplingBlock) {
            region.chunk(ChunkPos.toLong(pos)).saplingsPlanted++;
            region.markDirty();
        }
    }

    static void broken(ServerWorld world, ServerPlayerEntity player, BlockPos pos, BlockState state) {
        RegionMemory region = Reality.region(world);
        long now = world.getTime();
        Reality.count(world, ChronicleEvent.BLOCK_BROKEN, Registries.BLOCK.getId(state.getBlock()).toString());
        if (region.wasPlaced(pos)) {
            region.demolished(pos, state, now);
        } else if (state.isIn(BlockTags.LOGS)) {
            region.chunk(ChunkPos.toLong(pos)).logsBroken++;
            region.mined(pos, state, now);
        } else if (state.isIn(BlockTags.BASE_STONE_OVERWORLD) || state.isIn(BlockTags.DIRT)
                || Registries.BLOCK.getId(state.getBlock()).getPath().endsWith("_ore")) {
            region.mined(pos, state, now);
        }
        // Stage I: the block drops something that does not exist.
        IntegrityStage stage = Reality.stage(world, pos);
        if (stage.atLeast(IntegrityStage.DEJA_VU) && world.getRandom().nextFloat() < 0.01f * stage.severity()) {
            Block.dropStack(world, pos, new ItemStack(ModItems.UNREMEMBERED_THING));
            Reality.count(world, ChronicleEvent.ANOMALY_WITNESSED, "odd_drop");
        }
    }

    // ---- life and death ----

    /** Remembers where players have been for the last few seconds. Called every tick. */
    static void trail(ServerPlayerEntity player) {
        ArrayDeque<Vec3d> trail = TRAIL.computeIfAbsent(player.getUuid(), k -> new ArrayDeque<>());
        trail.addLast(player.getEntityPos());
        while (trail.size() > 60) {
            trail.removeFirst();
        }
    }

    public static Vec3d rewindPoint(ServerPlayerEntity player) {
        ArrayDeque<Vec3d> trail = TRAIL.get(player.getUuid());
        return trail == null || trail.isEmpty() ? player.getEntityPos() : trail.peekFirst();
    }

    static boolean allowDeath(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayerEntity player)) {
            return true;
        }
        PlayerMemory memory = Reality.player(player);
        long now = Reality.now(Reality.world(player).getServer());
        if (now < memory.deathlessUntil) {
            Vec3d back = rewindPoint(player);
            player.requestTeleport(back.x, back.y, back.z);
            player.setHealth(Math.max(6f, player.getMaxHealth() / 2));
            player.fallDistance = 0;
            player.extinguish();
            Reality.debt(player, 20);
            Texts.bar(player, Texts.glitch("REJECTED") .append(Texts.world(" The world refuses the event that would have killed you.")));
            Reality.chronicle(Reality.world(player), ChronicleEvent.REALITY_EDIT, player.getBlockPos(),
                    player.getName().getString(), "death rejected: " + source.getName());
            return false;
        }
        return true;
    }

    static void died(LivingEntity entity, DamageSource source) {
        if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        BlockPos pos = entity.getBlockPos();
        if (entity instanceof ServerPlayerEntity player) {
            PlayerMemory memory = Reality.player(player);
            memory.deaths++;
            memory.debt = CausalDebt.afterDeath(memory.debt);
            memory.observer.died(ChunkPos.toLong(pos));
            String held = Registries.ITEM.getId(player.getMainHandStack().getItem()).toString();
            Reality.chronicle(world, ChronicleEvent.PLAYER_DIED, pos, player.getName().getString(), source.getName() + "|" + held);
            Echoes.onPlayerDeath(world, player, source);
            return;
        }
        if (entity instanceof EnderDragonEntity) {
            Reality.chronicle(world, ChronicleEvent.DRAGON_DEFEATED, pos, attacker(source), "the lock");
            Block.dropStack(world, pos, new ItemStack(ModItems.concept(Concept.FINALITY), 3));
            return;
        }
        if (entity instanceof WitherEntity) {
            Reality.chronicle(world, ChronicleEvent.WITHER_DEFEATED, pos, attacker(source), "wither");
            return;
        }
        if (entity instanceof VillagerEntity) {
            Reality.chronicle(world, ChronicleEvent.VILLAGER_DIED, pos, attacker(source), "villager");
        } else if (entity.hasCustomName() && entity instanceof MobEntity) {
            Reality.chronicle(world, ChronicleEvent.NAMED_MOB_DIED, pos, attacker(source),
                    entity.getCustomName().getString() + "|" + Registries.ENTITY_TYPE.getId(entity.getType()));
        }
        if (source.getAttacker() instanceof ServerPlayerEntity) {
            Reality.count(world, ChronicleEvent.MOB_KILLED, Registries.ENTITY_TYPE.getId(entity.getType()).toString());
        }
    }

    private static String attacker(DamageSource source) {
        Entity a = source.getAttacker();
        return a == null ? "" : a.getName().getString();
    }

    static void woke(ServerPlayerEntity player, BlockPos bed) {
        ServerWorld world = Reality.world(player);
        Reality.chronicle(world, ChronicleEvent.SLEPT, bed, player.getName().getString(), "");
        PlayerMemory memory = Reality.player(player);
        memory.bed = bed.asLong();
        if (memory.hollowCursed) {
            memory.hollowCursed = false;
            Texts.tell(player, Texts.world("You wake, and your hands remember how to hold things."));
        }
        Waking.onWake(world, player, bed);
    }
}
