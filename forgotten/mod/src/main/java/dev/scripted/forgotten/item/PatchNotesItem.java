package dev.scripted.forgotten.item;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.logic.paradox.CausalDebt;
import dev.scripted.forgotten.logic.paradox.Insight;
import dev.scripted.forgotten.logic.unraveling.PatchNotes;
import dev.scripted.forgotten.player.PlayerMemory;
import dev.scripted.forgotten.util.Texts;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.SplittableRandom;

/**
 * The Mender's consumable. Reading it applies a fix the world wrote for itself: integrity
 * returns around you, and the fix has a version number and a line in a changelog.
 */
public class PatchNotesItem extends LoreItem {
    public PatchNotesItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!(world instanceof ServerWorld server) || !(user instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }
        PatchNotes.Note note = PatchNotes.generate(new SplittableRandom(world.getTime() ^ player.getUuid().getLeastSignificantBits()));
        BlockPos at = player.getBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Reality.mend(server, at.add(dx * 16, 0, dz * 16), note.strength());
            }
        }
        PlayerMemory memory = Reality.player(player);
        memory.debt = CausalDebt.afterRepayment(memory.debt, 1);
        Texts.tell(player, Texts.discovery(note.title()));
        Texts.tell(player, Texts.world(note.line()));
        Reality.insight(player, Insight.ANOMALY_RESOLVED);
        Reality.chronicle(server, ChronicleEvent.ANOMALY_RESOLVED, at, player.getName().getString(), note.line());
        world.playSound(null, at.getX(), at.getY(), at.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1f, 1.8f);
        user.getStackInHand(hand).decrement(1);
        return ActionResult.SUCCESS;
    }
}
