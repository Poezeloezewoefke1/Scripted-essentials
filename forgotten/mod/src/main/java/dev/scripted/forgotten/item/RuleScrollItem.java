package dev.scripted.forgotten.item;

import dev.scripted.forgotten.registry.ModComponents;
import dev.scripted.forgotten.system.RealityEdits;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

/**
 * A rule the Paradox Engine has convinced reality of, waiting to be applied somewhere: "stone
 * remembers falling upward", "water remembers being a cloud".
 */
public class RuleScrollItem extends LoreItem {
    public RuleScrollItem(Settings settings) {
        super(settings, true);
    }

    public static ItemStack of(net.minecraft.item.Item item, String rule) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModComponents.RULE, rule);
        return stack;
    }

    @Override
    public Text getName(ItemStack stack) {
        String rule = stack.get(ModComponents.RULE);
        return rule == null ? super.getName(stack) : Text.translatable("item.forgotten.rule_scroll." + rule);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        String rule = context.getStack().get(ModComponents.RULE);
        if (rule == null || !(context.getPlayer() instanceof ServerPlayerEntity player) || !(context.getWorld() instanceof ServerWorld world)) {
            return ActionResult.PASS;
        }
        if (RealityEdits.apply(rule, player, world, context.getBlockPos())) {
            context.getStack().decrement(1);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent display,
                              Consumer<Text> tooltip, TooltipType type) {
        String rule = stack.get(ModComponents.RULE);
        if (rule != null) {
            tooltip.accept(Text.translatable("item.forgotten.rule_scroll." + rule + ".desc").formatted(Formatting.GRAY));
        }
    }
}
