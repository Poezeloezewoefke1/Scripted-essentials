package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Forgotten;
import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.logic.concept.Concept;
import dev.scripted.forgotten.logic.integrity.IntegrityModel;
import dev.scripted.forgotten.logic.integrity.IntegrityStage;
import dev.scripted.forgotten.logic.paradox.Insight;
import dev.scripted.forgotten.player.PlayerMemory;
import dev.scripted.forgotten.registry.ModAttachments;
import dev.scripted.forgotten.registry.ModBlocks;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.util.Scheduler;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.RegionMemory;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * The world making mistakes. Each stage of forgetting has its own repertoire; a chunk deep in
 * forgetting can also make the milder mistakes of earlier stages. Everything here is temporary
 * or local unless the stage is severe enough that permanence is the point.
 */
public final class Anomalies {
    public static final Identifier GRAVITY_FLIP = Forgotten.id("gravity_flip");
    /** Ticks between anomalies in the same chunk. */
    public static final int SPACING = 300;

    private Anomalies() {
    }

    /** Called periodically for each player. Rolls for an anomaly near them. */
    public static void roll(ServerWorld world, ServerPlayerEntity player) {
        if (player.isSpectator()) {
            return;
        }
        BlockPos at = player.getBlockPos();
        int integrity = Reality.integrity(world, at);
        IntegrityStage stage = IntegrityStage.of(integrity);
        if (stage == IntegrityStage.STABLE) {
            return;
        }
        PlayerMemory memory = Reality.player(player);
        long now = Reality.now(world.getServer());
        if (now < memory.mercyUntil) {
            return;
        }
        RegionMemory.ChunkRecord record = Reality.region(world).chunk(ChunkPos.toLong(at));
        if (now - record.lastAnomaly < SPACING) {
            return;
        }
        Random random = world.getRandom();
        if (random.nextDouble() >= IntegrityModel.anomalyChance(integrity)) {
            return;
        }
        // Mostly the chunk's own stage; sometimes a milder mistake from an earlier one.
        int severity = random.nextInt(4) == 0 ? 1 + random.nextInt(stage.severity()) : stage.severity();
        boolean happened = switch (IntegrityStage.values()[severity]) {
            case DEJA_VU -> dejaVu(world, player, random);
            case CONTRADICTION -> contradiction(world, player, random);
            case FRAGMENTATION -> fragmentation(world, player, random);
            case AMNESIA -> amnesia(world, player, random);
            case UNWRITTEN -> unwritten(world, player, random);
            case STABLE -> false;
        };
        if (happened) {
            record.lastAnomaly = now;
            Reality.count(world, ChronicleEvent.ANOMALY_WITNESSED, IntegrityStage.values()[severity].name());
            Reality.insight(player, Insight.ANOMALY_WITNESSED);
            if (memory.setFlag("first_anomaly")) {
                Reality.advance(player, "something_is_wrong");
            }
            if (severity >= 2 && random.nextInt(6) == 0) {
                Block.dropStack(world, at, new ItemStack(ModItems.PARADOX_FRAGMENT));
            }
        }
    }

    // ---- Stage I: Déjà Vu ----

    static boolean dejaVu(ServerWorld world, ServerPlayerEntity player, Random random) {
        return switch (random.nextInt(5)) {
            case 0 -> staringAnimals(world, player);
            case 1 -> treeBecomesSheep(world, player, random);
            case 2 -> moonMoves(world, player);
            case 3 -> oddCave(world, player, random);
            default -> skyPatch(world, player, random);
        };
    }

    static boolean staringAnimals(ServerWorld world, ServerPlayerEntity player) {
        List<PassiveEntity> animals = world.getEntitiesByClass(PassiveEntity.class, player.getBoundingBox().expand(16), e -> !e.isAiDisabled());
        if (animals.isEmpty()) {
            return false;
        }
        long until = Reality.now(world.getServer()) + 100;
        for (PassiveEntity animal : animals) {
            double dx = player.getX() - animal.getX();
            double dz = player.getZ() - animal.getZ();
            float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90f;
            animal.setYaw(yaw);
            animal.setHeadYaw(yaw);
            animal.setBodyYaw(yaw);
            animal.setAiDisabled(true);
            animal.setAttached(ModAttachments.FROZEN_UNTIL, until);
        }
        return true;
    }

    static boolean treeBecomesSheep(ServerWorld world, ServerPlayerEntity player, Random random) {
        BlockPos log = find(world, player.getBlockPos(), 12, 400, random, s -> s.isIn(BlockTags.LOGS));
        if (log == null) {
            return false;
        }
        BlockState state = world.getBlockState(log);
        SheepEntity sheep = EntityType.SHEEP.create(world, SpawnReason.EVENT);
        if (sheep == null) {
            return false;
        }
        world.setBlockState(log, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
        sheep.refreshPositionAndAngles(log.getX() + 0.5, log.getY(), log.getZ() + 0.5, 0, 0);
        sheep.setAiDisabled(true);
        sheep.setNoGravity(true);
        world.spawnEntity(sheep);
        Scheduler.after(60, () -> {
            sheep.discard();
            if (world.getBlockState(log).isAir()) {
                world.setBlockState(log, state, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
            }
        });
        return true;
    }

    static boolean moonMoves(ServerWorld world, ServerPlayerEntity player) {
        if (world.getRegistryKey() != World.OVERWORLD || !world.isNight()) {
            return false;
        }
        world.setTimeOfDay(world.getTimeOfDay() + 24000L);
        return true;
    }

    static boolean oddCave(ServerWorld world, ServerPlayerEntity player, Random random) {
        if (world.isSkyVisible(player.getBlockPos())) {
            return false;
        }
        BlockState[] odd = {Blocks.MOSS_BLOCK.getDefaultState(), Blocks.SCULK.getDefaultState(), Blocks.CALCITE.getDefaultState(),
                Blocks.PACKED_ICE.getDefaultState(), Blocks.HONEYCOMB_BLOCK.getDefaultState(), Blocks.CRYING_OBSIDIAN.getDefaultState()};
        int placed = 0;
        for (int i = 0; i < 40 && placed < 6; i++) {
            BlockPos p = player.getBlockPos().add(random.nextInt(13) - 6, random.nextInt(7) - 3, random.nextInt(13) - 6);
            BlockState s = world.getBlockState(p);
            if (s.isIn(BlockTags.BASE_STONE_OVERWORLD) && exposed(world, p)) {
                world.setBlockState(p, odd[random.nextInt(odd.length)]);
                placed++;
            }
        }
        return placed > 0;
    }

    static boolean skyPatch(ServerWorld world, ServerPlayerEntity player, Random random) {
        if (world.isSkyVisible(player.getBlockPos())) {
            return false;
        }
        BlockPos wall = find(world, player.getBlockPos(), 6, 200, random, s -> s.isIn(BlockTags.BASE_STONE_OVERWORLD));
        if (wall == null || !exposed(world, wall)) {
            return false;
        }
        for (BlockPos p : BlockPos.iterate(wall, wall.add(1, 1, 0))) {
            if (world.getBlockState(p).isIn(BlockTags.BASE_STONE_OVERWORLD)) {
                world.setBlockState(p, ModBlocks.SKY_FRAGMENT.getDefaultState());
            }
        }
        return true;
    }

    // ---- Stage II: Contradiction ----

    static boolean contradiction(ServerWorld world, ServerPlayerEntity player, Random random) {
        return switch (random.nextInt(5)) {
            case 0 -> floatingSand(world, player, random);
            case 1 -> gravityFlip(world, player, random);
            case 2 -> misplacedCreature(world, player, random);
            case 3 -> doubleState(world, player, random);
            default -> risingWater(world, player, random);
        };
    }

    static boolean floatingSand(ServerWorld world, ServerPlayerEntity player, Random random) {
        int lifted = 0;
        for (int i = 0; i < 200 && lifted < 30; i++) {
            BlockPos p = player.getBlockPos().add(random.nextInt(17) - 8, random.nextInt(7) - 3, random.nextInt(17) - 8);
            BlockState s = world.getBlockState(p);
            if (s.getBlock() instanceof FallingBlock && world.getBlockState(p.up()).isAir()) {
                world.setBlockState(p.up(), s, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                world.setBlockState(p, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                lifted++;
            }
        }
        return lifted > 0;
    }

    public static boolean gravityFlip(ServerWorld world, ServerPlayerEntity player, Random random) {
        List<LivingEntity> affected = world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(8), e -> true);
        for (LivingEntity e : affected) {
            EntityAttributeInstance g = e.getAttributeInstance(EntityAttributes.GRAVITY);
            if (g != null && !g.hasModifier(GRAVITY_FLIP)) {
                g.addTemporaryModifier(new EntityAttributeModifier(GRAVITY_FLIP, -0.1, EntityAttributeModifier.Operation.ADD_VALUE));
            }
        }
        Scheduler.after(50, () -> {
            for (LivingEntity e : affected) {
                EntityAttributeInstance g = e.getAttributeInstance(EntityAttributes.GRAVITY);
                if (g != null) {
                    g.removeModifier(GRAVITY_FLIP);
                }
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 200, 0));
            }
        });
        if (random.nextInt(4) == 0) {
            Block.dropStack(world, player.getBlockPos(), new ItemStack(ModItems.concept(Concept.GRAVITY)));
        }
        return true;
    }

    static boolean misplacedCreature(ServerWorld world, ServerPlayerEntity player, Random random) {
        EntityType<?>[] wrong = {EntityType.STRIDER, EntityType.MOOSHROOM, EntityType.PARROT, EntityType.AXOLOTL,
                EntityType.SNIFFER, EntityType.ARMADILLO, EntityType.CAMEL};
        BlockPos p = surfaceNear(world, player.getBlockPos(), random, 10);
        Entity e = wrong[random.nextInt(wrong.length)].create(world, SpawnReason.EVENT);
        if (e == null || p == null) {
            return false;
        }
        e.refreshPositionAndAngles(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, random.nextFloat() * 360, 0);
        world.spawnEntity(e);
        return true;
    }

    static boolean doubleState(ServerWorld world, ServerPlayerEntity player, Random random) {
        BlockPos p = find(world, player.getBlockPos(), 8, 500, random, s -> s.contains(Properties.OPEN) || s.contains(Properties.LIT)
                || s.contains(Properties.POWERED));
        if (p == null) {
            return false;
        }
        for (int i = 1; i <= 8; i++) {
            Scheduler.after(i * 5, () -> {
                BlockState s = world.getBlockState(p);
                if (s.contains(Properties.OPEN)) {
                    world.setBlockState(p, s.cycle(Properties.OPEN), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                } else if (s.contains(Properties.LIT)) {
                    world.setBlockState(p, s.cycle(Properties.LIT), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                } else if (s.contains(Properties.POWERED)) {
                    world.setBlockState(p, s.cycle(Properties.POWERED), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                }
            });
        }
        return true;
    }

    static boolean risingWater(ServerWorld world, ServerPlayerEntity player, Random random) {
        BlockPos water = find(world, player.getBlockPos(), 12, 400, random, s -> s.isOf(Blocks.WATER));
        if (water == null) {
            return false;
        }
        BlockPos bottom = water;
        while (world.getBlockState(bottom.down()).isOf(Blocks.WATER) && bottom.getY() > world.getBottomY() + 1) {
            bottom = bottom.down();
        }
        BlockState floor = world.getBlockState(bottom.down());
        if (floor.isIn(BlockTags.DIRT) || floor.isIn(BlockTags.SAND) || floor.isOf(Blocks.GRAVEL) || floor.isOf(Blocks.CLAY)) {
            world.setBlockState(bottom.down(), Blocks.SOUL_SAND.getDefaultState());
            return true;
        }
        return false;
    }

    // ---- Stage III: Fragmentation ----

    static boolean fragmentation(ServerWorld world, ServerPlayerEntity player, Random random) {
        return switch (random.nextInt(3)) {
            case 0 -> floatingFragment(world, player, random);
            case 1 -> seasonsOverlap(world, player, random);
            default -> skyRiver(world, player, random);
        };
    }

    static boolean floatingFragment(ServerWorld world, ServerPlayerEntity player, Random random) {
        BlockPos center = surfaceNear(world, player.getBlockPos(), random, 18);
        if (center == null || center.isWithinDistance(player.getBlockPos(), 6)) {
            return false;
        }
        int lift = 8 + random.nextInt(8);
        List<BlockPos> moved = new ArrayList<>();
        for (int dx = -2; dx <= 1; dx++) {
            for (int dz = -2; dz <= 1; dz++) {
                for (int dy = -3; dy <= -1; dy++) {
                    BlockPos p = center.add(dx, dy, dz);
                    BlockState s = world.getBlockState(p);
                    if (s.isAir() || s.hasBlockEntity() || s.getHardness(world, p) < 0 || Reality.region(world).wasPlaced(p)) {
                        continue;
                    }
                    if (!world.getBlockState(p.up(lift)).isAir()) {
                        continue;
                    }
                    world.setBlockState(p.up(lift), s, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                    moved.add(p);
                }
            }
        }
        for (BlockPos p : moved) {
            world.setBlockState(p, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
        }
        return !moved.isEmpty();
    }

    static boolean seasonsOverlap(ServerWorld world, ServerPlayerEntity player, Random random) {
        int changed = 0;
        for (int i = 0; i < 120; i++) {
            BlockPos p = player.getBlockPos().add(random.nextInt(15) - 7, random.nextInt(9) - 4, random.nextInt(15) - 7);
            BlockState s = world.getBlockState(p);
            if (s.isOf(Blocks.GRASS_BLOCK) && world.getBlockState(p.up()).isAir()) {
                world.setBlockState(p.up(), Blocks.SNOW.getDefaultState());
                changed++;
            } else if (s.isIn(BlockTags.LEAVES) && random.nextBoolean()) {
                world.setBlockState(p, (random.nextBoolean() ? Blocks.CHERRY_LEAVES : Blocks.AZALEA_LEAVES).getDefaultState()
                        .with(Properties.PERSISTENT, true));
                changed++;
            } else if (s.isIn(BlockTags.LOGS) && random.nextInt(3) == 0) {
                world.setBlockState(p, Blocks.STONE.getDefaultState());
                changed++;
            }
        }
        return changed > 0;
    }

    static boolean skyRiver(ServerWorld world, ServerPlayerEntity player, Random random) {
        if (!world.isSkyVisible(player.getBlockPos())) {
            return false;
        }
        BlockPos start = player.getBlockPos().up(24 + random.nextInt(8));
        Direction dir = Direction.Type.HORIZONTAL.random(random);
        int placed = 0;
        for (int i = -6; i <= 6; i++) {
            BlockPos p = start.offset(dir, i);
            if (world.getBlockState(p).isAir() && p.getY() < world.getTopYInclusive()) {
                world.setBlockState(p, Blocks.WATER.getDefaultState());
                placed++;
            }
        }
        return placed > 0;
    }

    /** Stage III and worse: a door opens onto somewhere the player has been before. */
    public static boolean doorElsewhere(ServerWorld world, ServerPlayerEntity player, BlockPos door) {
        if (!Reality.stage(world, door).atLeast(IntegrityStage.FRAGMENTATION) || world.getRandom().nextInt(10) != 0) {
            return false;
        }
        PlayerMemory memory = Reality.player(player);
        if (memory.places.size() < 3) {
            return false;
        }
        String place = memory.places.get(world.getRandom().nextInt(memory.places.size() - 1));
        String[] p = place.split("\\|");
        ServerWorld target = RealityEdits.worldOf(world, p[0]);
        if (target == null) {
            return false;
        }
        double x = Integer.parseInt(p[1]) + 0.5;
        double y = Integer.parseInt(p[2]);
        double z = Integer.parseInt(p[3]) + 0.5;
        Travel.teleport(player, target, new net.minecraft.util.math.Vec3d(x, y, z), player.getYaw(), player.getPitch());
        Texts.bar(player, Texts.world("This is not where this door goes. This is somewhere you have been."));
        return true;
    }

    // ---- Stage IV: Amnesia ----

    static boolean amnesia(ServerWorld world, ServerPlayerEntity player, Random random) {
        return switch (random.nextInt(4)) {
            case 0 -> identitySwap(world, player, random);
            case 1 -> unrememberedThings(world, player, random);
            case 2 -> creaturesForget(world, player, random);
            default -> inventoryForgets(world, player, random);
        };
    }

    /** A furnace forgets it is a furnace and becomes a chest; a door becomes a window. */
    static boolean identitySwap(ServerWorld world, ServerPlayerEntity player, Random random) {
        BlockPos p = find(world, player.getBlockPos(), 10, 800, random, s ->
                s.getBlock() instanceof AbstractFurnaceBlock || s.getBlock() instanceof DoorBlock
                        || s.isOf(Blocks.TORCH) || s.isOf(Blocks.GLASS) || s.isOf(Blocks.LADDER) || s.isOf(Blocks.CRAFTING_TABLE));
        if (p == null || Reality.region(world).anchored(ChunkPos.toLong(p))) {
            return false;
        }
        swapIdentity(world, p);
        world.spawnParticles(ParticleTypes.WITCH, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.05);
        if (random.nextInt(5) == 0) {
            Block.dropStack(world, p, new ItemStack(ModItems.concept(Concept.IDENTITY)));
        }
        return true;
    }


    /** Makes one block forget what it is. Deterministic, so it can be tested. Returns whether it changed. */
    public static boolean swapIdentity(ServerWorld world, BlockPos p) {
        BlockState s = world.getBlockState(p);
        boolean changed = true;
        if (s.getBlock() instanceof AbstractFurnaceBlock) {
            List<ItemStack> contents = new ArrayList<>();
            BlockEntity be = world.getBlockEntity(p);
            if (be instanceof Inventory inv) {
                for (int i = 0; i < inv.size(); i++) {
                    contents.add(inv.getStack(i).copy());
                    inv.setStack(i, ItemStack.EMPTY);
                }
            }
            world.setBlockState(p, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, s.get(AbstractFurnaceBlock.FACING)));
            if (world.getBlockEntity(p) instanceof Inventory chest) {
                for (int i = 0; i < contents.size() && i < chest.size(); i++) {
                    chest.setStack(i, contents.get(i));
                }
            }
        } else if (s.getBlock() instanceof DoorBlock) {
            BlockPos lower = s.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? p : p.down();
            world.setBlockState(lower.up(), Blocks.GLASS_PANE.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
            world.setBlockState(lower, Blocks.GLASS_PANE.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
        } else if (s.isOf(Blocks.TORCH)) {
            world.setBlockState(p, Blocks.REDSTONE_TORCH.getDefaultState());
        } else if (s.isOf(Blocks.GLASS)) {
            world.setBlockState(p, Blocks.ICE.getDefaultState());
        } else if (s.isOf(Blocks.LADDER)) {
            world.setBlockState(p, Blocks.AIR.getDefaultState());
            Block.dropStack(world, p, new ItemStack(Blocks.VINE));
        } else if (s.isOf(Blocks.CRAFTING_TABLE)) {
            world.setBlockState(p, Blocks.BOOKSHELF.getDefaultState());
        } else {
            changed = false;
        }
        return changed;
    }

    static boolean unrememberedThings(ServerWorld world, ServerPlayerEntity player, Random random) {
        int placed = 0;
        for (int i = 0; i < 60 && placed < 5; i++) {
            BlockPos p = player.getBlockPos().add(random.nextInt(17) - 8, random.nextInt(9) - 4, random.nextInt(17) - 8);
            BlockState s = world.getBlockState(p);
            if ((s.isIn(BlockTags.BASE_STONE_OVERWORLD) || s.isIn(BlockTags.DIRT)) && exposed(world, p)) {
                world.setBlockState(p, ModBlocks.UNREMEMBERED_BLOCK.getDefaultState()
                        .with(dev.scripted.forgotten.block.UnrememberedBlock.IDENTITY,
                                dev.scripted.forgotten.block.UnrememberedBlock.identityAt(p, world.getTime())));
                placed++;
            }
        }
        return placed > 0;
    }

    static boolean creaturesForget(ServerWorld world, ServerPlayerEntity player, Random random) {
        List<MobEntity> mobs = world.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(16), m -> Glitches.of(m) == null);
        if (mobs.isEmpty()) {
            return false;
        }
        GlitchKind[] kinds = GlitchKind.values();
        int n = 0;
        for (MobEntity mob : mobs) {
            if (random.nextInt(3) == 0) {
                GlitchKind kind = kinds[random.nextInt(kinds.length)];
                if (kind == GlitchKind.ARCHIVIST && !(mob instanceof net.minecraft.entity.passive.VillagerEntity)) {
                    kind = GlitchKind.STARER;
                }
                Glitches.apply(mob, kind);
                n++;
            }
        }
        return n > 0;
    }

    static boolean inventoryForgets(ServerWorld world, ServerPlayerEntity player, Random random) {
        if (player.isCreative()) {
            return false;
        }
        var inv = player.getInventory();
        int slot = random.nextInt(inv.size());
        ItemStack stack = inv.getStack(slot);
        if (stack.isEmpty() || stack.isOf(ModItems.LENS) || stack.isOf(ModItems.ARCHITECT_CODEX)) {
            return false;
        }
        stack.decrement(1);
        if (!player.giveItemStack(new ItemStack(ModItems.UNREMEMBERED_THING))) {
            player.dropItem(new ItemStack(ModItems.UNREMEMBERED_THING), false);
        }
        Texts.bar(player, Text.literal("Something in your pack has forgotten what it is.").formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
        return true;
    }

    // ---- Stage V: The Unwritten ----

    static boolean unwritten(ServerWorld world, ServerPlayerEntity player, Random random) {
        return switch (random.nextInt(4)) {
            case 0 -> groundUnwrites(world, player);
            case 1 -> coordinatesFail(player, random);
            case 2 -> {
                gravityFlip(world, player, random);
                yield true;
            }
            default -> unwrittenStone(world, player, random);
        };
    }

    /** The ground under you stops being solid. Underneath is the other side of the world. */
    static boolean groundUnwrites(ServerWorld world, ServerPlayerEntity player) {
        BlockPos under = player.getBlockPos().down();
        List<BlockPos> changed = new ArrayList<>();
        List<BlockState> was = new ArrayList<>();
        for (BlockPos p : BlockPos.iterate(under.add(-1, -2, -1), under.add(1, 0, 1))) {
            BlockState s = world.getBlockState(p);
            if (!s.isAir() && !s.hasBlockEntity() && s.getHardness(world, p) >= 0) {
                changed.add(p.toImmutable());
                was.add(s);
                world.setBlockState(p, ModBlocks.NULL_BLOCK.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
            }
        }
        Scheduler.after(100, () -> {
            for (int i = 0; i < changed.size(); i++) {
                if (world.getBlockState(changed.get(i)).isOf(ModBlocks.NULL_BLOCK)) {
                    world.setBlockState(changed.get(i), was.get(i), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                }
            }
        });
        if (world.getRandom().nextInt(3) == 0) {
            Block.dropStack(world, player.getBlockPos(), new ItemStack(ModItems.concept(Concept.ABSENCE)));
        }
        return !changed.isEmpty();
    }

    static boolean coordinatesFail(ServerPlayerEntity player, Random random) {
        String[] values = {"NaN", "-Infinity", "0x7FFFFFFF", "undefined", "null", "-0", "1.0E308", "?"};
        Texts.bar(player, Text.literal("X: " + values[random.nextInt(values.length)] + "  Y: " + values[random.nextInt(values.length)]
                + "  Z: " + values[random.nextInt(values.length)]).formatted(Formatting.DARK_PURPLE));
        return true;
    }

    static boolean unwrittenStone(ServerWorld world, ServerPlayerEntity player, Random random) {
        BlockPos p = find(world, player.getBlockPos(), 8, 300, random, s -> s.isIn(BlockTags.BASE_STONE_OVERWORLD));
        if (p == null) {
            return false;
        }
        world.setBlockState(p, ModBlocks.UNWRITTEN_STONE.getDefaultState());
        return true;
    }

    // ---- helpers ----

    static BlockPos find(ServerWorld world, BlockPos center, int radius, int tries, Random random,
                         java.util.function.Predicate<BlockState> test) {
        for (int i = 0; i < tries; i++) {
            BlockPos p = center.add(random.nextInt(radius * 2 + 1) - radius, random.nextInt(radius + 1) - radius / 2,
                    random.nextInt(radius * 2 + 1) - radius);
            if (world.isChunkLoaded(p) && test.test(world.getBlockState(p))) {
                return p;
            }
        }
        return null;
    }

    static boolean exposed(ServerWorld world, BlockPos p) {
        for (Direction d : Direction.values()) {
            if (world.getBlockState(p.offset(d)).isAir()) {
                return true;
            }
        }
        return false;
    }

    static BlockPos surfaceNear(ServerWorld world, BlockPos center, Random random, int radius) {
        BlockPos p = center.add(random.nextInt(radius * 2 + 1) - radius, 0, random.nextInt(radius * 2 + 1) - radius);
        if (!world.isChunkLoaded(p)) {
            return null;
        }
        return world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, p);
    }

    /** Lifts the temporary freeze from creatures that stared or were caught by a Breakpoint. */
    public static void thaw(ServerWorld world, long now) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(48),
                    m -> m.hasAttached(ModAttachments.FROZEN_UNTIL))) {
                Long until = mob.getAttached(ModAttachments.FROZEN_UNTIL);
                if (until != null && now >= until) {
                    mob.removeAttached(ModAttachments.FROZEN_UNTIL);
                    mob.setAiDisabled(false);
                }
            }
        }
    }
}
