package dev.scripted.forgotten.item;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.util.Texts;
import net.minecraft.block.Block;
import net.minecraft.entity.EquipmentSlot;
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
 * The Unmaker's first tool. Drives a chunk's integrity down on purpose and harvests the Entropy
 * that comes off it. The cost is debt, and a world that is a little less sure of itself.
 */
public class DestabilizerItem extends LoreItem {
    public DestabilizerItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!(context.getPlayer() instanceof ServerPlayerEntity player) || !(context.getWorld() instanceof ServerWorld world)) {
            return ActionResult.SUCCESS;
        }
        BlockPos pos = context.getBlockPos();
        Reality.paradox(world, pos, 15);
        dev.scripted.forgotten.system.Bosses.error(world, net.minecraft.util.math.Vec3d.ofCenter(pos),
                dev.scripted.forgotten.logic.boss.StackDepth.ErrorKind.DESTABILIZE);
        Reality.debt(player, 2);
        Block.dropStack(world, pos.up(), new ItemStack(ModItems.ENTROPY_SHARD, 1 + world.getRandom().nextInt(2)));
        context.getStack().damage(1, player, context.getHand() == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        Reality.count(world, ChronicleEvent.REALITY_EDIT, "destabilize");
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 40, 0.8, 0.8, 0.8, 0.2);
        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 0.8f, 0.6f);
        Texts.bar(player, Texts.world("The ground here is less certain than it was."));
        player.getItemCooldownManager().set(context.getStack(), 20);
        return ActionResult.SUCCESS;
    }
}
