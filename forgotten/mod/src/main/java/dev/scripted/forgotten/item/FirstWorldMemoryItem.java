package dev.scripted.forgotten.item;

import dev.scripted.forgotten.system.Rifts;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** A memory older than the world. Crouch and use it to remember the First World - and to come back. */
public class FirstWorldMemoryItem extends LoreItem {
    public FirstWorldMemoryItem(Settings settings) {
        super(settings, true);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!user.isSneaking()) {
            return ActionResult.PASS;
        }
        if (user instanceof ServerPlayerEntity player) {
            Rifts.firstWorld(player);
        }
        return ActionResult.SUCCESS;
    }
}
