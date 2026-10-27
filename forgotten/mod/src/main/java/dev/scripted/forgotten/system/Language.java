package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.logic.language.FormationMatcher;
import dev.scripted.forgotten.logic.language.Material;
import dev.scripted.forgotten.logic.language.Word;
import dev.scripted.forgotten.logic.paradox.Insight;
import dev.scripted.forgotten.logic.waking.LucidityPhase;
import dev.scripted.forgotten.player.PlayerMemory;
import dev.scripted.forgotten.registry.ModAttachments;
import dev.scripted.forgotten.registry.ModItems;
import dev.scripted.forgotten.util.Lore;
import dev.scripted.forgotten.util.Texts;
import dev.scripted.forgotten.world.WorldMemory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.Optional;

/**
 * The world's language of shapes. Awake enough, it builds words out of blocks overnight; a
 * player studies them through the Lens until the journal decodes them, and can then answer by
 * building the word and speaking it with the Codex. The world responds.
 */
public final class Language {
    public static final int ANSWER_COOLDOWN = 1200;

    private Language() {
    }

    public static Optional<FormationMatcher.Match> read(ServerWorld world, BlockPos center) {
        FormationMatcher.Grid grid = (x, y, z) -> {
            BlockState state = world.getBlockState(center.add(x, y, z));
            if (state.isAir()) {
                return null;
            }
            return Material.of(Registries.BLOCK.getId(state.getBlock()).toString());
        };
        return FormationMatcher.find(grid, -3, -2, -3, 3, 2, 3);
    }

    /** Studies the formation near a position through the Lens. Returns whether one was there. */
    public static boolean study(ServerPlayerEntity player, ServerWorld world, BlockPos center) {
        Optional<FormationMatcher.Match> match = read(world, center);
        if (match.isEmpty()) {
            return false;
        }
        Word word = match.get().word();
        PlayerMemory memory = Reality.player(player);
        boolean decoded = memory.journal.study(word);
        if (decoded) {
            Texts.tell(player, Texts.discovery("WORD DECODED: " + word.name()));
            Texts.tell(player, Texts.world(word.meaning()));
            Reality.insight(player, Insight.WORD_DECODED);
            Reality.advance(player, "first_word");
        } else if (memory.journal.decoded(word)) {
            Texts.bar(player, Texts.info(word.name() + ": " + word.meaning()));
        } else {
            Texts.bar(player, Texts.world(memory.journal.render(word)));
        }
        world.spawnParticles(ParticleTypes.ENCHANT, center.getX() + 0.5, center.getY() + 1.5, center.getZ() + 0.5,
                30, 1.5, 1.0, 1.5, 0.5);
        return true;
    }

    /** Speaks a formation the player built. Returns whether the world answered. */
    public static boolean speak(ServerPlayerEntity player, ServerWorld world, BlockPos center) {
        Optional<FormationMatcher.Match> match = read(world, center);
        if (match.isEmpty()) {
            Texts.bar(player, Texts.world("There is nothing here the world would recognise as a word."));
            return false;
        }
        Word word = match.get().word();
        PlayerMemory memory = Reality.player(player);
        if (!memory.journal.decoded(word)) {
            Texts.bar(player, Texts.world("You do not know what this says. You cannot say it."));
            return false;
        }
        long now = Reality.now(world.getServer());
        if (now - memory.lastSpoke < ANSWER_COOLDOWN) {
            Texts.bar(player, Texts.world("The world is still thinking about the last thing you said."));
            return false;
        }
        memory.lastSpoke = now;
        respond(player, world, center, word);
        Reality.insight(player, Insight.WORD_SPOKEN);
        Reality.chronicle(world, ChronicleEvent.FORMATION_ANSWERED, center, player.getName().getString(), word.name());
        world.playSound(null, center.getX(), center.getY(), center.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE,
                SoundCategory.AMBIENT, 1.0f, 0.6f);
        return true;
    }

    private static void respond(ServerPlayerEntity player, ServerWorld world, BlockPos at, Word word) {
        WorldMemory memory = Reality.memory(world.getServer());
        long now = Reality.now(world.getServer());
        LucidityPhase phase = memory.lucidity.phase();
        switch (word) {
            case YOU -> {
                memory.lucidity.add(30, now);
                memory.markDirty();
                Texts.tell(player, Texts.world("The world turns toward you. Something vast has noticed it is being spoken to."));
            }
            case WORLD -> Texts.tell(player, Texts.world(Lore.ARCHIVE.get(world.getRandom().nextInt(Lore.ARCHIVE.size()))));
            case SLEEP -> {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        Reality.mend(world, at.add(dx * 16, 0, dz * 16), 10);
                    }
                }
                Texts.tell(player, Texts.world("The ground around you settles, like something turning over in its sleep."));
            }
            case RAIN -> {
                world.setWeather(0, 6000, true, false);
                Texts.tell(player, Texts.world("The sky agrees."));
            }
            case CLEAR -> {
                world.setWeather(6000, 0, false, false);
                Texts.tell(player, Texts.world("The clouds part, reluctantly."));
            }
            case DAY, NIGHT -> {
                if (!phase.atLeast(LucidityPhase.AWAKE)) {
                    Texts.tell(player, Texts.world("The world hears you, but it is not awake enough to move the sky."));
                    return;
                }
                long time = world.getTimeOfDay();
                long day = time / 24000L;
                long target = word == Word.DAY ? 1000 : 13000;
                long next = day * 24000L + target;
                if (next <= time) {
                    next += 24000L;
                }
                world.setTimeOfDay(next);
                Texts.tell(player, Texts.world(word == Word.DAY ? "The sky negotiates. It is morning." : "The sky negotiates. It is night."));
            }
            case GIFT -> {
                if (!Reality.player(player).setFlag("gift_" + (now / 24000L))) {
                    Texts.tell(player, Texts.world("The world has already given you something today."));
                    return;
                }
                List<ItemStack> gifts = List.of(new ItemStack(ModItems.PARADOX_FRAGMENT, 2),
                        new ItemStack(Items.AMETHYST_SHARD, 6), new ItemStack(Items.ECHO_SHARD, 1),
                        new ItemStack(ModItems.CONCEPTS.get(world.getRandom().nextInt(ModItems.CONCEPTS.size()))));
                for (ItemStack gift : gifts) {
                    Block.dropStack(world, at.up(), gift);
                }
                Texts.tell(player, Texts.world("Things you did not see arrive are lying at your feet."));
            }
            case WITNESS -> {
                Reality.player(player).observer.spokenTo();
                Texts.tell(player, Texts.world("Far away, something tall shifts its weight."));
            }
            case PARADOX -> {
                int thinkers = 0;
                for (CreeperEntity creeper : world.getEntitiesByClass(CreeperEntity.class, new Box(at).expand(16), c -> true)) {
                    creeper.setAttached(ModAttachments.FROZEN_UNTIL, now + 7L * 24000L);
                    creeper.setAiDisabled(true);
                    thinkers++;
                }
                Block.dropStack(world, at.up(), new ItemStack(ModItems.PARADOX_FRAGMENT));
                Texts.tell(player, Texts.world(thinkers > 0
                        ? thinkers + " creeper(s) stop to consider what you have built. They will be some time."
                        : "The world turns the contradiction over and leaves a piece of it behind."));
            }
            case HOME -> {
                Reality.mend(world, at, 25);
                Texts.tell(player, Texts.world("This place will remember you a little better."));
            }
            case FORGET -> {
                Reality.paradox(world, at, 25);
                Block.dropStack(world, at.up(), new ItemStack(ModItems.ENTROPY_SHARD, 2));
                Texts.tell(player, Texts.world("The world lets go of something here."));
            }
        }
    }

    /** The world writes a word itself, out of canonical blocks, standing on the ground at a position. */
    public static void write(ServerWorld world, BlockPos ground, Word word, int rotation) {
        for (Word.Cell c : word.rotated(rotation)) {
            BlockPos p = ground.add(c.x(), c.y() + 1, c.z());
            if (!world.getBlockState(p).isAir() && !world.getBlockState(p).isReplaceable()) {
                continue;
            }
            Block block = Registries.BLOCK.get(net.minecraft.util.Identifier.of(c.material().canonicalBlock()));
            world.setBlockState(p, block.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
    }
}
