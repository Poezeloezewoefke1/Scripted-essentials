package dev.scripted.forgotten.item;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.registry.ModComponents;
import dev.scripted.forgotten.util.Texts;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * A stack refined into a memory of itself in a deeper layer. It weighs nothing, so it can be
 * carried up through the nesting, and unfolds back into the stack only in the original world.
 */
public class FoldedMemoryItem extends Item {
    public FoldedMemoryItem(Settings settings) {
        super(settings);
    }

    public static ItemStack fold(Item folded, ItemStack contents) {
        ItemStack out = new ItemStack(folded);
        out.set(ModComponents.FOLDED, Registries.ITEM.getId(contents.getItem()) + "|" + contents.getCount());
        return out;
    }

    @Override
    public Text getName(ItemStack stack) {
        String f = stack.get(ModComponents.FOLDED);
        if (f == null) {
            return super.getName(stack);
        }
        String[] p = f.split("\\|");
        Identifier id = Identifier.tryParse(p[0]);
        Text inner = id != null && Registries.ITEM.containsId(id) ? Registries.ITEM.get(id).getName() : Text.literal(p[0]);
        return Text.translatable("item.forgotten.folded_memory.of", p.length > 1 ? p[1] : "?", inner);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        String f = stack.get(ModComponents.FOLDED);
        if (f == null || !(user instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }
        if (Reality.player(player).depth() > 0) {
            Texts.bar(player, Texts.world("It will only unfold in the world it was copied from."));
            return ActionResult.FAIL;
        }
        String[] p = f.split("\\|");
        Identifier id = Identifier.tryParse(p[0]);
        if (id == null || !Registries.ITEM.containsId(id)) {
            return ActionResult.FAIL;
        }
        int count = p.length > 1 ? Integer.parseInt(p[1]) : 1;
        Item item = Registries.ITEM.get(id);
        stack.decrement(1);
        while (count > 0) {
            int n = Math.min(count, item.getMaxCount());
            ItemStack give = new ItemStack(item, n);
            if (!player.giveItemStack(give)) {
                player.dropItem(give, false);
            }
            count -= n;
        }
        return ActionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent display,
                              Consumer<Text> tooltip, TooltipType type) {
        tooltip.accept(Text.translatable("item.forgotten.folded_memory.desc").formatted(Formatting.GRAY));
    }
}
