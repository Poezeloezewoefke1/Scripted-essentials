package dev.scripted.forgotten.item;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.integrity.IntegrityStage;
import dev.scripted.forgotten.logic.paradox.Insight;
import dev.scripted.forgotten.logic.waking.LucidityPhase;
import dev.scripted.forgotten.system.GlitchKind;
import dev.scripted.forgotten.system.Glitches;
import dev.scripted.forgotten.system.Language;
import dev.scripted.forgotten.util.Texts;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The Lens (amethyst and a spyglass). Look through it and the world shows its seams: the
 * integrity of the place you stand, how awake the world is, which creatures have forgotten what
 * they are. Look at one of the world's formations and you study the word it spells.
 */
public class LensItem extends LoreItem {
    public LensItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!(user instanceof ServerPlayerEntity player) || !(world instanceof ServerWorld server)) {
            return ActionResult.SUCCESS;
        }
        player.getItemCooldownManager().set(user.getStackInHand(hand), 10);
        BlockPos at = player.getBlockPos();
        int integrity = Reality.integrity(server, at);
        IntegrityStage stage = IntegrityStage.of(integrity);
        LucidityPhase phase = Reality.phase(server.getServer());

        HitResult hit = player.raycast(48, 0, false);
        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
            if (Language.study(player, server, bhr.getBlockPos())) {
                return ActionResult.SUCCESS;
            }
        }
        Entity looked = lookedAt(player, server);
        if (looked instanceof LivingEntity living) {
            GlitchKind kind = Glitches.of(living);
            if (kind != null) {
                Texts.bar(player, Texts.world("This " + living.getType().getName().getString().toLowerCase() + " " + kind.reading() + "."));
                if (Reality.player(player).setFlag("seen_glitch_" + kind.id())) {
                    Reality.insight(player, Insight.ANOMALY_WITNESSED);
                }
                return ActionResult.SUCCESS;
            }
        }

        Text line = Text.literal("Integrity " + integrity + " ").formatted(color(stage))
                .append(Text.literal(stage == IntegrityStage.STABLE ? "- the world remembers this place"
                        : "- Stage " + stage.numeral() + ": " + stage.title()).formatted(Formatting.GRAY))
                .append(Text.literal("  |  The world is " + phase.title()).formatted(Formatting.DARK_AQUA));
        Texts.bar(player, line);
        seams(server, player, stage);
        if (stage != IntegrityStage.STABLE && Reality.player(player).setFlag("lens_chunk_" + ChunkPos.toLong(at))) {
            Reality.insight(player, Insight.ANOMALY_WITNESSED);
        }
        Reality.advance(player, "lens");
        return ActionResult.SUCCESS;
    }

    private static Formatting color(IntegrityStage stage) {
        return switch (stage) {
            case STABLE -> Formatting.GREEN;
            case DEJA_VU -> Formatting.YELLOW;
            case CONTRADICTION -> Formatting.GOLD;
            case FRAGMENTATION -> Formatting.RED;
            case AMNESIA -> Formatting.DARK_RED;
            case UNWRITTEN -> Formatting.DARK_PURPLE;
        };
    }

    /** Glowing seams: lines of light where the world is coming apart, denser where it is worse. */
    private static void seams(ServerWorld world, ServerPlayerEntity player, IntegrityStage stage) {
        int lines = stage.severity() * 3;
        var random = world.getRandom();
        for (int i = 0; i < lines; i++) {
            Vec3d a = player.getEntityPos().add(random.nextGaussian() * 6, random.nextDouble() * 3, random.nextGaussian() * 6);
            Vec3d dir = new Vec3d(random.nextGaussian(), random.nextGaussian() * 0.3, random.nextGaussian()).normalize();
            for (int s = 0; s < 8; s++) {
                Vec3d p = a.add(dir.multiply(s * 0.4));
                world.spawnParticles(player, ParticleTypes.END_ROD, true, false, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
        }
    }

    static Entity lookedAt(ServerPlayerEntity player, ServerWorld world) {
        Vec3d eye = player.getEyePos();
        Vec3d dir = player.getRotationVector();
        Entity best = null;
        double bestDot = 0.985;
        for (Entity e : world.getOtherEntities(player, player.getBoundingBox().expand(32))) {
            Vec3d to = e.getEntityPos().add(0, e.getHeight() / 2, 0).subtract(eye);
            double dot = to.normalize().dotProduct(dir);
            if (dot > bestDot && player.canSee(e)) {
                bestDot = dot;
                best = e;
            }
        }
        return best;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return false;
    }
}
