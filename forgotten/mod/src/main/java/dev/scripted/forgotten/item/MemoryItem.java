package dev.scripted.forgotten.item;

import dev.scripted.forgotten.registry.ModComponents;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

/**
 * A Memory extracted from something: what a creeper remembers about exploding, what water
 * remembers about flowing. The source is a data component, so one item covers everything in
 * the game, and Paradox recipes can ask for a specific one.
 */
public class MemoryItem extends Item {
    public MemoryItem(Settings settings) {
        super(settings);
    }

    public static ItemStack of(Item memoryItem, String source) {
        ItemStack stack = new ItemStack(memoryItem);
        stack.set(ModComponents.MEMORY_SOURCE, source);
        return stack;
    }

    /** A readable name for whatever a memory came from. */
    public static Text sourceName(String source) {
        Identifier id = Identifier.tryParse(source);
        if (id == null) {
            return Text.literal(source);
        }
        if (Registries.ENTITY_TYPE.containsId(id)) {
            return Registries.ENTITY_TYPE.get(id).getName();
        }
        if (Registries.BLOCK.containsId(id)) {
            return Registries.BLOCK.get(id).getName();
        }
        if (Registries.ITEM.containsId(id)) {
            return Registries.ITEM.get(id).getName();
        }
        return Text.translatable("memory.forgotten." + id.getPath());
    }

    @Override
    public Text getName(ItemStack stack) {
        String source = stack.get(ModComponents.MEMORY_SOURCE);
        if (source == null || source.isEmpty()) {
            return super.getName(stack);
        }
        return Text.translatable("item.forgotten.memory.of", sourceName(source));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent display,
                              Consumer<Text> tooltip, TooltipType type) {
        String source = stack.get(ModComponents.MEMORY_SOURCE);
        if (source != null) {
            tooltip.accept(Text.literal(source).formatted(Formatting.DARK_GRAY));
        }
        tooltip.accept(Text.translatable("item.forgotten.memory.desc").formatted(Formatting.GRAY));
    }
}
