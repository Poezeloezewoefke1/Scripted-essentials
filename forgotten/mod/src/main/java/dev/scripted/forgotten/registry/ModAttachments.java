package dev.scripted.forgotten.registry;

import com.mojang.serialization.Codec;
import dev.scripted.forgotten.Forgotten;
import dev.scripted.forgotten.player.PlayerMemory;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * Data attached to entities.
 *
 * <p>The mob attachments are how the world rewrites creatures without new entity types: a
 * vanilla cow with a GLITCH of "explosive" is the Herd; a vanilla zombie with a BIO_PROGRAM is a
 * Bio-Node running your code. Identity lives in data, not in the class, which is the point.
 */
public final class ModAttachments {
    public static final AttachmentType<PlayerMemory> PLAYER = AttachmentRegistry.create(Forgotten.id("player_memory"),
            b -> b.persistent(PlayerMemory.CODEC).copyOnDeath().initializer(PlayerMemory::new));

    /** A forgotten identity a vanilla mob has taken on (see GlitchKind). */
    public static final AttachmentType<String> GLITCH = AttachmentRegistry.create(Forgotten.id("glitch"),
            b -> b.persistent(Codec.STRING).copyOnDeath());

    /** A Bio-Node's program, as serialized text. */
    public static final AttachmentType<String> BIO_PROGRAM = AttachmentRegistry.create(Forgotten.id("bio_program"),
            b -> b.persistent(Codec.STRING).copyOnDeath());

    /** A Bio-Node's execution state: "pc,timer,elapsed,failed,halted". */
    public static final AttachmentType<String> BIO_STATE = AttachmentRegistry.create(Forgotten.id("bio_state"),
            b -> b.persistent(Codec.STRING));

    /** A Bio-Node's item buffer, as "item_id*count;..." and its owner's UUID. */
    public static final AttachmentType<String> BIO_BUFFER = AttachmentRegistry.create(Forgotten.id("bio_buffer"),
            b -> b.persistent(Codec.STRING));
    public static final AttachmentType<String> OWNER = AttachmentRegistry.create(Forgotten.id("owner"),
            b -> b.persistent(Codec.STRING).copyOnDeath());

    /** World time until which a mob is frozen by a Breakpoint or lost in thought. */
    public static final AttachmentType<Long> FROZEN_UNTIL = AttachmentRegistry.create(Forgotten.id("frozen_until"),
            b -> b.persistent(Codec.LONG));

    /** A zombie that remembers a player: the UUID it will walk toward over the following nights. */
    public static final AttachmentType<String> GRUDGE = AttachmentRegistry.create(Forgotten.id("grudge"),
            b -> b.persistent(Codec.STRING));

    /** A creeper that has been provoked and will explode as usual. */
    public static final AttachmentType<Boolean> PROVOKED = AttachmentRegistry.create(Forgotten.id("provoked"),
            b -> b.persistent(Codec.BOOL));

    private ModAttachments() {
    }

    public static void init() {
        // Static initialisation registers everything above.
    }
}
