package dev.scripted.forgotten.item;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.language.Word;
import dev.scripted.forgotten.logic.paradox.ArchitectRank;
import dev.scripted.forgotten.player.PlayerMemory;
import dev.scripted.forgotten.player.RealityEdit;
import dev.scripted.forgotten.system.Language;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.WorldMemory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * The Architect Codex. Use it to read where you stand: your rank and what it lets you do, what
 * you owe reality, how awake the world is, the words you have decoded, the edits you are
 * keeping alive. Use it on a formation you built, crouching, to speak it to the world.
 */
public class CodexItem extends LoreItem {
    public CodexItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() instanceof ServerPlayerEntity player && context.getWorld() instanceof ServerWorld world
                && player.isSneaking()) {
            Language.speak(player, world, context.getBlockPos());
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (user instanceof ServerPlayerEntity player && world instanceof ServerWorld server) {
            report(player, server);
        }
        return ActionResult.SUCCESS;
    }

    public static void report(ServerPlayerEntity player, ServerWorld world) {
        PlayerMemory m = Reality.player(player);
        WorldMemory w = Reality.memory(world.getServer());
        long now = Reality.now(world.getServer());
        ArchitectRank rank = m.rank(now);
        Texts.tell(player, Text.literal("=== Architect Codex ===").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
        Texts.tell(player, Text.literal("Rank " + rank.level() + ": " + rank.title() + " (" + m.insight + " insight"
                + (rank.toNext(m.insight) > 0 ? ", " + rank.toNext(m.insight) + " to next" : "") + ")").formatted(Formatting.AQUA));
        Texts.tell(player, Texts.world(rank.power()));
        Texts.tell(player, Text.literal(String.format("Causal debt: %.0f (%s)", m.debt, m.tier().name().toLowerCase()))
                .formatted(m.debt > 120 ? Formatting.RED : Formatting.GOLD));
        Texts.tell(player, Text.literal(String.format("The world is %s (lucidity %.1f)", w.lucidity.phase().title(),
                w.lucidity.lucidity())).formatted(Formatting.DARK_AQUA));
        if (w.storming(now)) {
            Texts.tell(player, Text.literal("A Reality Storm is in progress: " + w.storm).formatted(Formatting.DARK_PURPLE));
        }
        StringBuilder words = new StringBuilder();
        for (Word word : Word.values()) {
            if (m.journal.decoded(word)) {
                words.append(words.isEmpty() ? "" : ", ").append(word.name());
            }
        }
        Texts.tell(player, Text.literal("Words decoded (" + m.journal.decodedCount() + "/" + Word.values().length + "): "
                + (words.isEmpty() ? "none yet. Study the world's formations through the Lens." : words)).formatted(Formatting.GRAY));
        long active = m.edits.stream().filter(RealityEdit::active).count();
        Texts.tell(player, Text.literal("Edits holding: " + active + "   Depth: " + m.depth() + "   Nesting instability: " + m.instability)
                .formatted(Formatting.GRAY));
        if (m.observer.appearances() > 0) {
            Texts.tell(player, Text.literal("Something has been watching you (" + m.observer.appearances() + " times). Trust: "
                    + m.observer.trust() + (m.observer.hostile() ? " - it no longer comes." : "")).formatted(Formatting.DARK_GRAY));
        }
    }
}
