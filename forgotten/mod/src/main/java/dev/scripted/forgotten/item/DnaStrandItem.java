package dev.scripted.forgotten.item;

import dev.scripted.forgotten.system.BioNodes;
import dev.scripted.forgotten.util.Texts;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

/** Splices a creature into a Bio-Node: its own instincts are cut away and it waits for code. */
public class DnaStrandItem extends LoreItem {
    public DnaStrandItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(user instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }
        if (!(entity instanceof MobEntity mob) || entity instanceof Monster || BioNodes.isNode(mob)) {
            return ActionResult.PASS;
        }
        BioNodes.splice(mob, player);
        Texts.bar(player, Texts.discovery("Spliced. It has stopped wanting anything. Give it a program."));
        if (!player.isCreative()) {
            stack.decrement(1);
        }
        return ActionResult.SUCCESS;
    }
}
