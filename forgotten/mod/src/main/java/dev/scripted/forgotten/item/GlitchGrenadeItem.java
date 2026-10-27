package dev.scripted.forgotten.item;

import dev.scripted.forgotten.entity.GlitchGrenadeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** Throw it. Mobs swap minds, blocks swap places, players' controls swap directions. */
public class GlitchGrenadeItem extends LoreItem {
    public GlitchGrenadeItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS,
                0.5f, 0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f));
        if (world instanceof ServerWorld) {
            GlitchGrenadeEntity grenade = new GlitchGrenadeEntity(world, user, stack.copyWithCount(1));
            grenade.setVelocity(user, user.getPitch(), user.getYaw(), 0f, 1.2f, 1f);
            world.spawnEntity(grenade);
        }
        user.getItemCooldownManager().set(stack, 20);
        stack.decrementUnlessCreative(1, user);
        return ActionResult.SUCCESS;
    }
}
