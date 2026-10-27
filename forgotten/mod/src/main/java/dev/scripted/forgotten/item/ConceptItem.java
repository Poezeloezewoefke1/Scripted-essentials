package dev.scripted.forgotten.item;

import dev.scripted.forgotten.logic.concept.Concept;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

/** One of the twelve fundamental ideas. Combined in the Paradox Engine they make phenomena. */
public class ConceptItem extends Item {
    private final Concept concept;

    public ConceptItem(Settings settings, Concept concept) {
        super(settings);
        this.concept = concept;
    }

    public Concept concept() {
        return concept;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent display,
                              Consumer<Text> tooltip, TooltipType type) {
        tooltip.accept(Text.literal(concept.meaning()).formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC));
        tooltip.accept(Text.literal("Found in: " + concept.source()).formatted(Formatting.DARK_GRAY));
    }
}
