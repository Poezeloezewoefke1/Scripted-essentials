package dev.scripted.forgotten.item;

import dev.scripted.forgotten.system.Unraveling;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;

/**
 * The Unmaker's endgame. Set it into ground that has already fallen to Fragmentation and the
 * Crash begins: a five-by-five chunk region is unmade, permanently. It is survivable, and it is
 * the only place Null Materials can be found.
 */
public class CrashCoreItem extends LoreItem {
    public CrashCoreItem(Settings settings) {
        super(settings, true);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!(context.getPlayer() instanceof ServerPlayerEntity player) || !(context.getWorld() instanceof ServerWorld world)) {
            return ActionResult.SUCCESS;
        }
        if (!Unraveling.crash(world, context.getBlockPos(), player)) {
            return ActionResult.FAIL;
        }
        context.getStack().decrementUnlessCreative(1, player);
        return ActionResult.SUCCESS;
    }
}
