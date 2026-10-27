package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.paradox.CausalDebt;
import dev.scripted.forgotten.logic.paradox.DebtTier;
import dev.scripted.forgotten.player.PlayerMemory;
import dev.scripted.forgotten.util.Scheduler;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.RegionMemory;
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.Map;

/**
 * The world remembers what you refuse to. Causal Debt is not a number that makes enemies
 * hit harder; each tier is a different way history starts collecting.
 */
public final class DebtDirector {
    private static final Map<Item, Item> SMELTS = Map.of(Items.RAW_IRON, Items.IRON_INGOT, Items.RAW_GOLD, Items.GOLD_INGOT,
            Items.RAW_COPPER, Items.COPPER_INGOT, Items.SAND, Items.GLASS, Items.COBBLESTONE, Items.STONE,
            Items.BEEF, Items.COOKED_BEEF, Items.PORKCHOP, Items.COOKED_PORKCHOP);

    private DebtDirector() {
    }

    public static void init() {
        // Reality Bleed: your name, and what you say, start to scramble.
        ServerMessageDecoratorEvent.EVENT.register(ServerMessageDecoratorEvent.CONTENT_PHASE, (sender, message) -> {
            if (sender != null && Reality.player(sender).tier() == DebtTier.BLEED) {
                return Text.literal(Texts.scramble(message.getString(), sender.getRandom(), 0.12f));
            }
            return message;
        });
    }

    /** Every two seconds, per player. */
    public static void tick(ServerWorld world, ServerPlayerEntity player, long now) {
        PlayerMemory memory = Reality.player(player);
        DebtTier tier = memory.tier();
        if (tier == DebtTier.NONE || player.isSpectator()) {
            return;
        }
        Random random = world.getRandom();
        whispers(world, player, random);
        if (tier.atLeast(DebtTier.REARRANGEMENT) && random.nextInt(40) == 0) {
            rearrange(world, player, random, now);
        }
        if (tier.atLeast(DebtTier.CORRECTIONS) && Puppets.count(Puppets.Kind.SECOND_YOU) == 0
                && random.nextDouble() < CausalDebt.correctionChance(tier) / 30.0) {
            correction(world, player, random);
        }
        if (tier == DebtTier.BLEED) {
            bleed(world, player, random);
        }
    }

    /** Low debt: the world gets ahead of itself around you. */
    static void whispers(ServerWorld world, ServerPlayerEntity player, Random random) {
        if (random.nextInt(25) == 0) {
            // A furnace near you smelts something before you have put it in.
            BlockPos f = Anomalies.find(world, player.getBlockPos(), 6, 200, random, s -> s.getBlock() instanceof AbstractFurnaceBlock);
            if (f != null && world.getBlockEntity(f) instanceof AbstractFurnaceBlockEntity furnace) {
                var inv = player.getInventory();
                for (int i = 0; i < inv.size(); i++) {
                    ItemStack st = inv.getStack(i);
                    Item out = SMELTS.get(st.getItem());
                    if (out != null) {
                        ItemStack existing = furnace.getStack(2);
                        if (existing.isEmpty() || (existing.isOf(out) && existing.getCount() < existing.getMaxCount())) {
                            st.decrement(1);
                            if (existing.isEmpty()) {
                                furnace.setStack(2, new ItemStack(out));
                            } else {
                                existing.increment(1);
                            }
                            furnace.markDirty();
                            break;
                        }
                    }
                }
            }
        }
        if (random.nextInt(20) == 0) {
            // Doors open a moment before you reach them.
            Vec3d ahead = player.getEntityPos().add(player.getRotationVector().multiply(2.5));
            BlockPos p = BlockPos.ofFloored(ahead);
            for (BlockPos q : new BlockPos[]{p, p.up(), p.down()}) {
                BlockState s = world.getBlockState(q);
                if (s.getBlock() instanceof DoorBlock && !s.get(Properties.OPEN)) {
                    ((DoorBlock) s.getBlock()).setOpen(player, world, s, q, true);
                    break;
                }
            }
        }
        if (random.nextInt(120) == 0 && world.isNight()) {
            // A sound that has not happened yet. Then it happens.
            Vec3d behind = player.getEntityPos().subtract(player.getRotationVector().multiply(6));
            world.playSound(null, behind.x, behind.y, behind.z, SoundEvents.ENTITY_CREEPER_PRIMED, SoundCategory.HOSTILE, 0.8f, 1f);
            Scheduler.after(100, () -> {
                CreeperEntity creeper = EntityType.CREEPER.create(world, SpawnReason.EVENT);
                if (creeper != null) {
                    creeper.refreshPositionAndAngles(behind.x, behind.y, behind.z, 0, 0);
                    world.spawnEntity(creeper);
                }
            });
        }
    }

    /** Moderate debt: history rearranges. Felled trees come back holding their own logs. */
    static void rearrange(ServerWorld world, ServerPlayerEntity player, Random random, long now) {
        RegionMemory region = Reality.region(world);
        RegionMemory.ChunkRecord r = region.peek(ChunkPos.toLong(player.getBlockPos()));
        if (r != null && !r.mined.isEmpty()) {
            List<RegionMemory.Remembered> logs = r.mined.stream().filter(m -> m.state().isIn(BlockTags.LOGS) && now - m.tick() > 2400).toList();
            if (logs.size() >= 3) {
                int restored = 0;
                for (RegionMemory.Remembered m : logs) {
                    if (world.getBlockState(m.blockPos()).isAir()) {
                        world.setBlockState(m.blockPos(), m.state());
                        restored++;
                    }
                }
                RegionMemory.Remembered first = logs.get(0);
                BlockPos chestPos = first.blockPos();
                world.setBlockState(chestPos, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.Type.HORIZONTAL.random(random)));
                if (world.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
                    chest.setStack(13, new ItemStack(first.state().getBlock().asItem(), Math.min(64, logs.size())));
                }
                r.mined.removeAll(logs);
                region.markDirty();
                if (restored > 0) {
                    Texts.bar(player, Texts.world("A tree you cut down is standing again. There is a chest inside it."));
                }
                return;
            }
            List<RegionMemory.Remembered> ores = r.mined.stream().filter(m -> !m.state().isIn(BlockTags.LOGS) && now - m.tick() > 4800).toList();
            for (RegionMemory.Remembered m : ores) {
                if (world.getBlockState(m.blockPos()).isAir()) {
                    world.setBlockState(m.blockPos(), m.state());
                }
            }
            if (!ores.isEmpty()) {
                r.mined.removeAll(ores);
                region.markDirty();
                Texts.bar(player, Texts.world("The rock you mined has remembered itself."));
            }
        }
        for (VillagerEntity v : world.getEntitiesByClass(VillagerEntity.class, player.getBoundingBox().expand(24), e -> !e.hasCustomName())) {
            if (random.nextInt(4) == 0) {
                v.setCustomName(Text.literal("Remembers what you did").formatted(Formatting.GRAY, Formatting.ITALIC));
            }
        }
    }

    /** High debt: reality sends a Correction. */
    static void correction(ServerWorld world, ServerPlayerEntity player, Random random) {
        Vec3d behind = player.getEntityPos().subtract(player.getRotationVector().multiply(new Vec3d(10, 0, 10)));
        Vec3d at = Vec3d.ofBottomCenter(world.getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.ofFloored(behind)));
        if (Puppets.spawn(world, player, Puppets.Kind.SECOND_YOU, at) != null) {
            Texts.tell(player, Text.literal("Someone is in your base.").formatted(Formatting.DARK_RED, Formatting.ITALIC));
        }
    }

    /** Reality Bleed: your footsteps leave holes in the world, and creatures near you become echoes. */
    static void bleed(ServerWorld world, ServerPlayerEntity player, Random random) {
        if (player.isOnGround() && random.nextInt(3) == 0) {
            BlockPos under = player.getBlockPos().down();
            BlockState s = world.getBlockState(under);
            if (!s.isAir() && !s.hasBlockEntity() && s.getHardness(world, under) >= 0
                    && !Reality.region(world).wasPlaced(under) && s.isIn(BlockTags.DIRT)) {
                world.setBlockState(under, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                Scheduler.after(60, () -> {
                    if (world.getBlockState(under).isAir()) {
                        world.setBlockState(under, s, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                    }
                });
            }
        }
        if (random.nextInt(30) == 0) {
            Anomalies.creaturesForget(world, player, random);
        }
    }
}
