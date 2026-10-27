package dev.scripted.forgotten.item;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.logic.concept.Concept;
import dev.scripted.forgotten.logic.paradox.Insight;
import dev.scripted.forgotten.registry.ModBlocks;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.system.Glitches;
import dev.scripted.forgotten.util.Texts;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

/**
 * The Mender's tool. It rewrites something glitched back into what it should be: a creature
 * that forgot its identity becomes itself again; an Unremembered block becomes stone. Each
 * repair gives back Coherence. Using it on things that remember being something else is how
 * Identity is first glimpsed.
 */
public class StitcherItem extends LoreItem {
    public StitcherItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(user instanceof ServerPlayerEntity player) || !(user.getEntityWorld() instanceof ServerWorld world)) {
            return ActionResult.SUCCESS;
        }
        if (entity instanceof MobEntity mob && Glitches.heal(mob)) {
            repaired(world, player, stack, hand, entity.getBlockPos());
            Texts.bar(player, Texts.world("It remembers what it is."));
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!(context.getPlayer() instanceof ServerPlayerEntity player) || !(context.getWorld() instanceof ServerWorld world)) {
            return ActionResult.SUCCESS;
        }
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        BlockState fixed = restore(world, pos, state);
        if (fixed == null) {
            return ActionResult.PASS;
        }
        world.setBlockState(pos, fixed, Block.NOTIFY_ALL);
        repaired(world, player, context.getStack(), context.getHand(), pos);
        return ActionResult.SUCCESS;
    }

    /** What a glitched block should have been, or null if it is not glitched. */
    public static BlockState restore(ServerWorld world, BlockPos pos, BlockState state) {
        if (state.isOf(ModBlocks.UNREMEMBERED_BLOCK) || state.isOf(ModBlocks.NULL_BLOCK) || state.isOf(ModBlocks.SKY_FRAGMENT)) {
            return pos.getY() < 0 ? Blocks.DEEPSLATE.getDefaultState() : Blocks.STONE.getDefaultState();
        }
        if (state.isOf(ModBlocks.VOLATILE_LOG)) {
            return Blocks.OAK_LOG.getDefaultState();
        }
        return null;
    }

    private static void repaired(ServerWorld world, ServerPlayerEntity player, ItemStack stack, Hand hand, BlockPos at) {
        Reality.mend(world, at, 3);
        Block.dropStack(world, at.up(), new ItemStack(ModItems.COHERENCE_SHARD));
        if (world.getRandom().nextInt(12) == 0) {
            Block.dropStack(world, at.up(), new ItemStack(ModItems.concept(Concept.IDENTITY)));
        }
        stack.damage(1, player, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        Reality.insight(player, Insight.ANOMALY_RESOLVED);
        Reality.count(world, ChronicleEvent.ANOMALY_RESOLVED, "stitch");
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.1);
        world.playSound(null, at.getX(), at.getY(), at.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1f, 1.2f);
    }
}
