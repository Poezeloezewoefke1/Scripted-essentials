package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.dimension.Dimensions;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.logic.concept.Concept;
import dev.scripted.forgotten.logic.waking.LucidityPhase;
import dev.scripted.forgotten.registry.ModBlocks;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.RegionMemory;
import dev.scripted.forgotten.world.WorldMemory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BubbleColumnBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TropicalFishEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradedItem;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockEntityTickInvoker;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Awake world remembers its own past: Anachronisms (ancient water that flows upward, fish
 * made of light) and the Inheritors, an older kind of people who believe the world was promised
 * to them. They are not evil. They are grieving. They trade, accept gifts, and remember harm -
 * and if harmed enough, they go to war.
 *
 * <p>Also the Underdream's two impossible resources: Quiet, grown into its islands, and
 * Tomorrow, which advances everything in a chunk by one day.
 */
public final class Anachronisms {
    public static final String INHERITOR = "forgotten_inheritor";
    public static final String ARRIVED = "inheritors_arrived";

    private Anachronisms() {
    }

    public static void tick(ServerWorld world, ServerPlayerEntity player, long now) {
        if (world.getRegistryKey() == Dimensions.UNDERDREAM) {
            dreamChunks(world, player);
            return;
        }
        if (world.getRegistryKey() != World.OVERWORLD
                || Reality.phase(world.getServer()).ordinal() < LucidityPhase.AWAKE.ordinal()) {
            return;
        }
        Random random = world.getRandom();
        WorldMemory memory = Reality.memory(world.getServer());
        if (!memory.flag(ARRIVED) && random.nextInt(10) == 0) {
            BlockPos at = Anomalies.surfaceNear(world, player.getBlockPos(), random, 96);
            if (at != null && !at.isWithinDistance(player.getBlockPos(), 48)) {
                memory.setFlag(ARRIVED);
                settle(world, at);
                Texts.tell(player, Texts.world("Somewhere near, people who were here before you have come back to grieve."));
            }
        }
        if (random.nextInt(40) == 0) {
            BlockPos at = Anomalies.surfaceNear(world, player.getBlockPos(), random, 64);
            if (at != null && !at.isWithinDistance(player.getBlockPos(), 24)) {
                ancientWater(world, at.down(), random);
            }
        }
        if (relation(world) < -5 && world.isNight() && random.nextInt(20) == 0) {
            war(world, player, random);
        }
        for (WanderingTraderEntity inheritor : world.getEntitiesByClass(WanderingTraderEntity.class, player.getBoundingBox().expand(48),
                e -> e.getCommandTags().contains(INHERITOR))) {
            gifts(world, inheritor);
        }
    }

    // ---- anachronisms ----

    /** An ancient ocean where there is none: a pool over soul sand whose water flows upward, and fish made of light. */
    public static void ancientWater(ServerWorld world, BlockPos center, Random random) {
        BlockState column = Blocks.BUBBLE_COLUMN.getDefaultState().with(BubbleColumnBlock.DRAG, false);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx * dx + dz * dz > 10) {
                    continue;
                }
                BlockPos top = center.add(dx, 0, dz);
                if (world.getBlockState(top).hasBlockEntity()) {
                    continue;
                }
                world.setBlockState(top.down(3), Blocks.SOUL_SAND.getDefaultState(), Block.NOTIFY_LISTENERS);
                for (int y = 2; y >= 0; y--) {
                    world.setBlockState(top.down(y), column, Block.NOTIFY_LISTENERS);
                }
            }
        }
        for (int i = 0; i < 3; i++) {
            TropicalFishEntity fish = EntityType.TROPICAL_FISH.create(world, SpawnReason.EVENT);
            if (fish != null) {
                fish.refreshPositionAndAngles(center.getX() + 0.5, center.getY() - 1, center.getZ() + 0.5, random.nextFloat() * 360, 0f);
                fish.setGlowing(true);
                fish.setCustomName(Text.literal("light").formatted(Formatting.YELLOW));
                world.spawnEntity(fish);
            }
        }
        Reality.chronicle(world, ChronicleEvent.ANOMALY_WITNESSED, center, "", "anachronism");
    }

    // ---- the Inheritors ----

    static void settle(ServerWorld world, BlockPos center) {
        Random random = world.getRandom();
        for (int h = 0; h < 3; h++) {
            BlockPos base = Anomalies.surfaceNear(world, center, random, 10);
            if (base == null) {
                continue;
            }
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = 0; dy <= 4; dy++) {
                        BlockPos p = base.add(dx, dy, dz);
                        boolean wall = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                        BlockState s = dy == 0 ? Blocks.SMOOTH_QUARTZ.getDefaultState()
                                : dy == 4 ? Blocks.QUARTZ_SLAB.getDefaultState()
                                : wall && !(dz == -2 && dx == 0 && dy < 3) ? Blocks.CALCITE.getDefaultState()
                                : Blocks.AIR.getDefaultState();
                        if (!world.getBlockState(p).hasBlockEntity()) {
                            world.setBlockState(p, s, Block.NOTIFY_LISTENERS);
                        }
                    }
                }
            }
            inheritor(world, base.up());
        }
        inheritor(world, center.up());
    }

    public static WanderingTraderEntity inheritor(ServerWorld world, BlockPos at) {
        WanderingTraderEntity inheritor = EntityType.WANDERING_TRADER.create(world, SpawnReason.EVENT);
        if (inheritor == null) {
            return null;
        }
        inheritor.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        inheritor.setCustomName(Text.literal("Inheritor").formatted(Formatting.AQUA));
        inheritor.setDespawnDelay(0);
        inheritor.setPersistent();
        inheritor.setWanderTarget(at);
        inheritor.addCommandTag(INHERITOR);
        world.spawnEntity(inheritor);
        TradeOfferList offers = inheritor.getOffers();
        offers.clear();
        offers.add(new TradeOffer(new TradedItem(ModItems.MEMORY, 2), Optional.empty(),
                new ItemStack(ModItems.concept(Concept.MEMORY)), 6, 5, 0f));
        offers.add(new TradeOffer(new TradedItem(ModItems.LULLABY_SHARD, 8), Optional.empty(),
                new ItemStack(ModBlocks.MARROWGLASS, 8), 6, 5, 0f));
        offers.add(new TradeOffer(new TradedItem(Items.EMERALD, 24), Optional.of(new TradedItem(ModItems.COHERENCE_SHARD, 8)),
                new ItemStack(ModItems.concept(Concept.WITNESS)), 2, 10, 0f));
        offers.add(new TradeOffer(new TradedItem(ModItems.PARADOX_FRAGMENT, 12), Optional.empty(),
                new ItemStack(ModItems.TOMORROW), 2, 10, 0f));
        return inheritor;
    }

    /** How the Inheritors feel about the living: gifts raise it, every harm lowers it three times as far. */
    public static long relation(ServerWorld world) {
        var chronicle = Reality.memory(world.getServer()).chronicle;
        return chronicle.countOf(ChronicleEvent.ANOMALY_RESOLVED, "inheritor_gift")
                - 3 * chronicle.countOf(ChronicleEvent.ANOMALY_WITNESSED, "inheritor_harm");
    }

    /** Items left at an Inheritor's feet are taken as gifts. */
    static void gifts(ServerWorld world, WanderingTraderEntity inheritor) {
        for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, inheritor.getBoundingBox().expand(1.5), Entity::isAlive)) {
            item.discard();
            Reality.count(world, ChronicleEvent.ANOMALY_RESOLVED, "inheritor_gift");
            world.spawnParticles(net.minecraft.particle.ParticleTypes.HEART, inheritor.getX(), inheritor.getBodyY(1.1), inheritor.getZ(),
                    3, 0.3, 0.2, 0.3, 0.0);
        }
    }

    /** Hooked on damage: harming an Inheritor is remembered. */
    public static void harmed(LivingEntity entity, DamageSource source) {
        if (entity.getCommandTags().contains(INHERITOR) && source.getAttacker() instanceof PlayerEntity
                && entity.getEntityWorld() instanceof ServerWorld world) {
            Reality.count(world, ChronicleEvent.ANOMALY_WITNESSED, "inheritor_harm");
        }
    }

    static void war(ServerWorld world, ServerPlayerEntity player, Random random) {
        BlockPos at = Anomalies.surfaceNear(world, player.getBlockPos(), random, 24);
        if (at == null || at.isWithinDistance(player.getBlockPos(), 10)) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            PillagerEntity raider = EntityType.PILLAGER.create(world, SpawnReason.EVENT);
            if (raider != null) {
                raider.refreshPositionAndAngles(at.getX() + 0.5 + i, at.getY(), at.getZ() + 0.5, 0f, 0f);
                raider.setCustomName(Text.literal("Grieving Inheritor").formatted(Formatting.DARK_AQUA));
                raider.equipStack(net.minecraft.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
                raider.setTarget(player);
                world.spawnEntity(raider);
            }
        }
        Texts.tell(player, Texts.world("The Inheritors have not forgiven you."));
    }

    // ---- the Underdream ----

    /** The first time someone stands in an Underdream chunk, Quiet grows into it. */
    static void dreamChunks(ServerWorld world, ServerPlayerEntity player) {
        RegionMemory region = Reality.region(world);
        ChunkPos chunk = player.getChunkPos();
        RegionMemory.ChunkRecord record = region.chunk(chunk.toLong());
        if (record.copied) {
            return;
        }
        record.copied = true;
        region.markDirty();
        Random random = Random.create(chunk.toLong());
        for (int i = 0; i < 2; i++) {
            BlockPos top = world.getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(chunk.getStartX() + random.nextInt(16), 0, chunk.getStartZ() + random.nextInt(16)));
            if (top.getY() > world.getBottomY() + 1 && world.getBlockState(top).isAir()) {
                world.setBlockState(top, ModBlocks.QUIET.getDefaultState(), Block.NOTIFY_ALL);
            }
        }
    }

    /** Breaking Marrowglass in the Underdream sometimes leaves Tomorrow behind. */
    public static void broken(ServerWorld world, BlockPos pos, BlockState state) {
        if (world.getRegistryKey() == Dimensions.UNDERDREAM && state.isOf(ModBlocks.MARROWGLASS) && world.getRandom().nextInt(10) == 0) {
            Block.dropStack(world, pos, new ItemStack(ModItems.TOMORROW));
        }
    }

    /**
     * Tomorrow: everything in the chunk advances one day. Every block that grows is given the
     * random ticks a day would give it, machines run (up to a minute of work), the young grow up.
     */
    public static int tomorrow(ServerWorld world, ChunkPos chunk) {
        Random random = world.getRandom();
        int grown = 0;
        BlockPos.Mutable p = new BlockPos.Mutable();
        for (int y = world.getBottomY(); y <= world.getTopYInclusive(); y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    p.set(chunk.getStartX() + x, y, chunk.getStartZ() + z);
                    BlockState s = world.getBlockState(p);
                    if (!s.hasRandomTicks() || s.isIn(net.minecraft.registry.tag.BlockTags.LEAVES)) {
                        continue;
                    }
                    // A day is about eighteen random ticks per block.
                    for (int i = 0; i < 18; i++) {
                        BlockState now = world.getBlockState(p);
                        if (!now.hasRandomTicks()) {
                            break;
                        }
                        now.randomTick(world, p.toImmutable(), random);
                    }
                    grown++;
                }
            }
        }
        List<BlockEntityTickInvoker> tickers = new ArrayList<>(world.blockEntityTickers);
        for (BlockEntityTickInvoker ticker : tickers) {
            if (!ticker.isRemoved() && new ChunkPos(ticker.getPos()).equals(chunk)) {
                for (int i = 0; i < 1200; i++) {
                    ticker.tick();
                }
            }
        }
        for (PassiveEntity young : world.getEntitiesByClass(PassiveEntity.class,
                new net.minecraft.util.math.Box(chunk.getStartX(), world.getBottomY(), chunk.getStartZ(),
                        chunk.getEndX() + 1, world.getTopYInclusive(), chunk.getEndZ() + 1), PassiveEntity::isBaby)) {
            young.setBreedingAge(0);
        }
        return grown;
    }
}
