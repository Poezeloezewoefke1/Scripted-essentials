package dev.scripted.forgotten.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

/**
 * An item whose tooltip explains what it is for, from the language file entry
 * "item.forgotten.&lt;id&gt;.desc". Most of the mod's items extend this so every one of them
 * tells the player how it fits into the rest.
 */
public class LoreItem extends Item {
    private final boolean glint;

    public LoreItem(Settings settings) {
        this(settings, false);
    }

    public LoreItem(Settings settings, boolean glint) {
        super(settings);
        this.glint = glint;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return glint || super.hasGlint(stack);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent display,
                              Consumer<Text> tooltip, TooltipType type) {
        tooltip.accept(Text.translatable(getTranslationKey() + ".desc").formatted(Formatting.GRAY));
        super.appendTooltip(stack, context, display, tooltip, type);
    }
}
