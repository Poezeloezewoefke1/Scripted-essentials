package dev.scripted.forgotten.paradox;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.block.entity.ParadoxEngineBlockEntity;
import dev.scripted.forgotten.item.FoldedMemoryItem;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.logic.concept.Concept;
import dev.scripted.forgotten.logic.concept.ConceptDistiller;
import dev.scripted.forgotten.logic.paradox.Insight;
import dev.scripted.forgotten.logic.paradox.ParadoxRecipe;
import dev.scripted.forgotten.logic.paradox.ParadoxRecipeBook;
import dev.scripted.forgotten.logic.paradox.ParadoxStack;
import dev.scripted.forgotten.registry.ModComponents;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.dimension.Dimensions;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * What the Paradox Engine does with its inputs. Two contradictions are built in because they
 * apply to anything rather than to specific items:
 * <ul>
 *   <li>Folding: at depth 1 or deeper, any stack contradicted by Absence becomes a weightless
 *       Memory of itself, to be carried up through the nesting and unfolded at home.</li>
 *   <li>Distilling: any Memory contradicted by a Paradox Fragment yields the Concept it is
 *       closest to. This is the first reliable way to get Concepts.</li>
 * </ul>
 * Everything else comes from data-driven recipes.
 */
public final class ParadoxEngineLogic {
    private ParadoxEngineLogic() {
    }

    /** One planned outcome, before it is made real. */
    public record Plan(ItemStack result, double debt, int[] consume, boolean consumeWholeSubject, String reason) {
        public boolean ok() {
            return reason == null;
        }

        static Plan fail(String reason) {
            return new Plan(ItemStack.EMPTY, 0, new int[0], false, reason);
        }
    }

    public static ParadoxStack view(ItemStack stack) {
        if (stack.isEmpty()) {
            return ParadoxStack.EMPTY;
        }
        String memory = stack.get(ModComponents.MEMORY_SOURCE);
        return new ParadoxStack(Registries.ITEM.getId(stack.getItem()).toString(), memory == null ? "" : memory, stack.getCount());
    }

    public static Plan plan(ServerPlayerEntity player, ParadoxEngineBlockEntity engine) {
        ItemStack s = engine.inputs.getStack(ParadoxEngineBlockEntity.SUBJECT);
        ItemStack c = engine.inputs.getStack(ParadoxEngineBlockEntity.CONTRADICTION);
        ItemStack k = engine.inputs.getStack(ParadoxEngineBlockEntity.CATALYST);
        if (s.isEmpty() || c.isEmpty()) {
            return Plan.fail("Place a Subject and a Contradiction.");
        }
        int depth = engine.getWorld() instanceof ServerWorld w ? Dimensions.depthOf(w) : 0;

        if (c.isOf(ModItems.concept(Concept.ABSENCE)) && k.isEmpty() && depth >= 1
                && !s.isOf(ModItems.MEMORY) && !s.isOf(ModItems.FOLDED_MEMORY) && !(s.getItem() instanceof dev.scripted.forgotten.item.ConceptItem)) {
            return new Plan(FoldedMemoryItem.fold(ModItems.FOLDED_MEMORY, s), 2, new int[]{1}, true, null);
        }
        if (s.isOf(ModItems.MEMORY) && c.isOf(ModItems.PARADOX_FRAGMENT) && k.isEmpty()) {
            String source = s.get(ModComponents.MEMORY_SOURCE);
            if (source != null) {
                Concept concept = ConceptDistiller.conceptFor(source);
                return new Plan(new ItemStack(ModItems.concept(concept)), 3, new int[]{0, 1}, false, null);
            }
        }

        int rank = Reality.rank(player).level();
        ParadoxRecipeBook.Lookup lookup = ParadoxRecipes.BOOK.find(view(s), view(c), view(k), rank, depth);
        return switch (lookup.outcome()) {
            case NO_RECIPE -> Plan.fail("Reality does not believe this. Nothing would come of it.");
            case RANK_TOO_LOW -> Plan.fail("Reality would believe this from an Architect of rank " + lookup.recipe().rank()
                    + ". You are rank " + rank + ".");
            case TOO_SHALLOW -> Plan.fail("This only holds " + lookup.recipe().minDepth() + " layers deep. The Engine is at depth " + depth + ".");
            case OK -> {
                ParadoxRecipe r = lookup.recipe();
                int[] consume = r.catalyst() == null ? new int[]{0, 1} : new int[]{0, 1, 2};
                yield new Plan(build(r), r.debt(), consume, false, null);
            }
        };
    }

    static ItemStack build(ParadoxRecipe recipe) {
        Item item = Registries.ITEM.get(Identifier.of(recipe.result()));
        ItemStack out = new ItemStack(item, recipe.resultCount());
        if (!recipe.resultData().isEmpty()) {
            if (item == ModItems.RULE_SCROLL) {
                out.set(ModComponents.RULE, recipe.resultData());
            } else if (item == ModItems.MEMORY) {
                out.set(ModComponents.MEMORY_SOURCE, recipe.resultData());
            }
        }
        return out;
    }

    public static ItemStack preview(ServerPlayerEntity player, ParadoxEngineBlockEntity engine) {
        Plan plan = plan(player, engine);
        if (!plan.ok()) {
            ItemStack barrier = new ItemStack(Items.BARRIER);
            barrier.set(DataComponentTypes.CUSTOM_NAME, Text.literal(plan.reason()).formatted(Formatting.GRAY));
            return barrier;
        }
        ItemStack shown = plan.result().copy();
        shown.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("Click to contradict reality.").formatted(Formatting.LIGHT_PURPLE),
                Text.literal(String.format("Causal debt: %.0f", plan.debt())).formatted(Formatting.GOLD))));
        return shown;
    }

    /** Makes the planned result real. Returns whether anything happened. */
    public static boolean contradict(ServerPlayerEntity player, ParadoxEngineBlockEntity engine) {
        Plan plan = plan(player, engine);
        if (!plan.ok()) {
            Texts.bar(player, Texts.world(plan.reason()));
            return false;
        }
        for (int slot : plan.consume()) {
            engine.inputs.removeStack(slot, 1);
        }
        if (plan.consumeWholeSubject()) {
            engine.inputs.setStack(ParadoxEngineBlockEntity.SUBJECT, ItemStack.EMPTY);
        }
        ItemStack result = plan.result().copy();
        if (!player.giveItemStack(result)) {
            player.dropItem(result, false);
        }
        Reality.debt(player, plan.debt());
        Reality.insight(player, Insight.REALITY_EDIT);
        ServerWorld world = Reality.world(player);
        BlockPos pos = engine.getPos();
        Reality.paradox(world, pos, (int) Math.ceil(plan.debt() / 2));
        if (plan.debt() >= 10) {
            Reality.chronicle(world, ChronicleEvent.REALITY_EDIT, pos, player.getName().getString(),
                    Registries.ITEM.getId(plan.result().getItem()).toString());
        }
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 60, 0.4, 0.6, 0.4, 0.3);
        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.BLOCKS, 1f, 0.6f);
        Reality.advance(player, "paradox_engine");
        return true;
    }
}
