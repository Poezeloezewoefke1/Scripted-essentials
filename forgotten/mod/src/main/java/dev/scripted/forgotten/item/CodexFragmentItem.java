package dev.scripted.forgotten.item;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.paradox.ArchitectRank;
import dev.scripted.forgotten.player.PlayerMemory;
import dev.scripted.forgotten.registry.ModComponents;
import dev.scripted.forgotten.util.Texts;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * A page torn from another Architect's Codex when they fall to you. Read it and, for ten
 * minutes, you have their rank and everything it lets them do.
 */
public class CodexFragmentItem extends LoreItem {
    public static final int DURATION = 12000;

    public CodexFragmentItem(Settings settings) {
        super(settings, true);
    }

    public static ItemStack of(net.minecraft.item.Item item, ServerPlayerEntity from) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModComponents.FRAGMENT, Reality.rank(from).level());
        stack.set(ModComponents.HISTORY_OWNER, from.getName().getString());
        return stack;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!(user instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }
        ItemStack stack = user.getStackInHand(hand);
        int level = stack.getOrDefault(ModComponents.FRAGMENT, 1);
        PlayerMemory memory = Reality.player(player);
        memory.borrowedRank = level;
        memory.borrowedUntil = Reality.now(Reality.world(player).getServer()) + DURATION;
        Texts.tell(player, Texts.discovery("For a while, you are " + ArchitectRank.ofLevel(level).title() + "."));
        stack.decrementUnlessCreative(1, player);
        return ActionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent display,
                              Consumer<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, display, tooltip, type);
        String owner = stack.get(ModComponents.HISTORY_OWNER);
        if (owner != null) {
            tooltip.accept(Text.literal("Torn from " + owner + "'s Codex (rank " + stack.getOrDefault(ModComponents.FRAGMENT, 1) + ")")
                    .formatted(Formatting.DARK_PURPLE));
        }
    }
}
