package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.integrity.IntegrityStage;
import dev.scripted.forgotten.registry.ModBlocks;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.util.Displays;
import dev.scripted.forgotten.util.Texts;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.TraderLlamaEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradedItem;

import java.util.Optional;

/**
 * The Draft: a wandering trader from a version of the world that was never finished. Its llamas
 * are untextured polygons. It sells impossible things - a block of air, one (1) gravity, the
 * concept of Tuesday - for absurd prices. Sometimes the trades work.
 */
public final class Draft {
    public static final String TAG = "forgotten_draft";

    private Draft() {
    }

    /** Rolled every two seconds per player; appears rarely, only where the world is contradicting itself. */
    public static void roll(ServerWorld world, ServerPlayerEntity player) {
        if (world.getRandom().nextInt(2400) != 0 || !Reality.stage(world, player.getBlockPos()).atLeast(IntegrityStage.CONTRADICTION)) {
            return;
        }
        if (!world.getEntitiesByClass(WanderingTraderEntity.class, player.getBoundingBox().expand(96),
                t -> t.getCommandTags().contains(TAG)).isEmpty()) {
            return;
        }
        BlockPos near = Anomalies.surfaceNear(world, player.getBlockPos(), world.getRandom(), 12);
        if (near != null) {
            spawn(world, near);
        }
    }

    public static WanderingTraderEntity spawn(ServerWorld world, BlockPos at) {
        WanderingTraderEntity trader = EntityType.WANDERING_TRADER.create(world, SpawnReason.EVENT);
        if (trader == null) {
            return null;
        }
        trader.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        trader.setCustomName(Text.literal("The Draft").formatted(Formatting.LIGHT_PURPLE));
        trader.setDespawnDelay(12000);
        trader.addCommandTag(TAG);
        world.spawnEntity(trader);
        TradeOfferList offers = trader.getOffers();
        offers.clear();
        Random random = world.getRandom();
        offers.add(new TradeOffer(new TradedItem(Items.EMERALD, 37 + random.nextInt(27)), Optional.empty(),
                new ItemStack(ModItems.BLOCK_OF_AIR), 3, 5, 0f));
        offers.add(new TradeOffer(new TradedItem(Items.DIAMOND, 1 + random.nextInt(3)), Optional.of(new TradedItem(Items.FEATHER, 64)),
                new ItemStack(ModItems.ONE_GRAVITY), 2, 5, 0f));
        offers.add(new TradeOffer(new TradedItem(Items.CLOCK, 7), Optional.empty(),
                new ItemStack(ModItems.TUESDAY), 1, 5, 0f));
        offers.add(new TradeOffer(new TradedItem(ModItems.ENTROPY_SHARD, 16), Optional.of(new TradedItem(Items.TOTEM_OF_UNDYING, 1)),
                new ItemStack(ModItems.UNWRITTEN_DEATH), 1, 10, 0f));
        offers.add(new TradeOffer(new TradedItem(Items.DIRT, 1), Optional.empty(),
                new ItemStack(ModItems.UNREMEMBERED_THING, 1), 12, 1, 0f));
        for (int i = 0; i < 2; i++) {
            TraderLlamaEntity llama = EntityType.TRADER_LLAMA.create(world, SpawnReason.EVENT);
            if (llama == null) {
                continue;
            }
            llama.refreshPositionAndAngles(at.getX() + 1.5 + i, at.getY(), at.getZ() + 0.5, 0f, 0f);
            llama.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 12000, 0, false, false));
            llama.addCommandTag(TAG);
            world.spawnEntity(llama);
            llama.attachLeash(trader, true);
            var polygon = Displays.block(world, new Vec3d(llama.getX(), llama.getY(), llama.getZ()),
                    ModBlocks.NULL_BLOCK.getDefaultState(), 0.9f, 1.4f, 1.3f, false);
            if (polygon != null) {
                polygon.startRiding(llama, true, false);
            }
        }
        return trader;
    }

    /** What an impossible item does when it does not work: the inventory forgets its order. */
    public static void corrupt(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        Random random = player.getRandom();
        for (int i = 0; i < 4; i++) {
            int a = random.nextInt(PlayerInventory.MAIN_SIZE);
            int b = random.nextInt(PlayerInventory.MAIN_SIZE);
            ItemStack sa = inventory.getStack(a);
            inventory.setStack(a, inventory.getStack(b));
            inventory.setStack(b, sa);
        }
        int victim = random.nextInt(PlayerInventory.MAIN_SIZE);
        ItemStack stack = inventory.getStack(victim);
        if (!stack.isEmpty() && stack.getRarity() == net.minecraft.util.Rarity.COMMON) {
            inventory.setStack(victim, new ItemStack(ModItems.UNREMEMBERED_THING, Math.min(stack.getCount(), 16)));
        }
        Texts.bar(player, Texts.glitch("The trade did not finish compiling. Your inventory has been rearranged."));
    }
}
