package dev.scripted.forgotten.item;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.registry.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Draws a Memory out of whatever it touches: a block, a creature, water, or - pointed at open
 * sky high above the world - the sky itself. Memories are what the Paradox Engine runs on.
 */
public class MemorySiphonItem extends LoreItem {
    public static final int SKY_HEIGHT = 140;

    public MemorySiphonItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!(context.getWorld() instanceof ServerWorld world) || !(context.getPlayer() instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        String source = Registries.BLOCK.getId(state.getBlock()).toString();
        extract(world, player, context.getStack(), context.getHand(), source, pos);
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(user.getEntityWorld() instanceof ServerWorld world) || !(user instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }
        String source = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
        entity.damage(world, world.getDamageSources().magic(), 2.0f);
        extract(world, player, stack, hand, source, entity.getBlockPos());
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!(world instanceof ServerWorld server) || !(user instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }
        HitResult fluidHit = player.raycast(6, 0, true);
        if (fluidHit instanceof BlockHitResult bhr && fluidHit.getType() == HitResult.Type.BLOCK
                && !server.getFluidState(bhr.getBlockPos()).isEmpty()) {
            String source = Registries.FLUID.getId(server.getFluidState(bhr.getBlockPos()).getFluid()).toString()
                    .replace("flowing_", "");
            extract(server, player, user.getStackInHand(hand), hand, source, bhr.getBlockPos());
            return ActionResult.SUCCESS;
        }
        HitResult hit = player.raycast(64, 0, false);
        if (hit.getType() == HitResult.Type.MISS && player.getY() >= SKY_HEIGHT && player.getPitch() < -30) {
            extract(server, player, user.getStackInHand(hand), hand, "forgotten:sky", player.getBlockPos());
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    private static void extract(ServerWorld world, ServerPlayerEntity player, ItemStack siphon, Hand hand, String source, BlockPos at) {
        ItemStack memory = MemoryItem.of(ModItems.MEMORY, source);
        if (!player.giveItemStack(memory)) {
            player.dropItem(memory, false);
        }
        siphon.damage(1, player, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        Reality.paradox(world, at, 1);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.02);
        world.playSound(null, at.getX(), at.getY(), at.getZ(), SoundEvents.BLOCK_SCULK_CATALYST_BLOOM, SoundCategory.PLAYERS, 0.8f, 1.6f);
        player.getItemCooldownManager().set(siphon, 20);
    }
}
