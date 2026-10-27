package dev.scripted.forgotten.item;

import dev.scripted.forgotten.logic.bio.Program;
import dev.scripted.forgotten.registry.ModComponents;
import dev.scripted.forgotten.system.BioNodes;
import dev.scripted.forgotten.util.Texts;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.List;
import java.util.function.Consumer;

/**
 * A compiled program. Use it on a spliced Bio-Node to load it. The Infinite Loop is a cartridge
 * whose program never lets time pass: no creature can run it, but a portal can.
 */
public class CartridgeItem extends LoreItem {
    private final boolean loop;

    public CartridgeItem(Settings settings, boolean loop) {
        super(settings, loop);
        this.loop = loop;
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(user instanceof ServerPlayerEntity player) || !(user.getEntityWorld() instanceof ServerWorld world)) {
            return ActionResult.SUCCESS;
        }
        if (!(entity instanceof MobEntity mob) || !BioNodes.isNode(mob)) {
            Texts.bar(player, Texts.info("Only a creature spliced with a DNA Strand can run code."));
            return ActionResult.FAIL;
        }
        String program = stack.get(ModComponents.PROGRAM);
        if (program == null || program.isEmpty()) {
            return ActionResult.FAIL;
        }
        if (loop) {
            mob.damage(world, world.getDamageSources().magic(), Float.MAX_VALUE);
            Texts.bar(player, Texts.glitch("It tried to run forever, all at once."));
            return ActionResult.SUCCESS;
        }
        BioNodes.install(mob, program);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, mob.getX(), mob.getBodyY(0.6), mob.getZ(), 16, 0.3, 0.4, 0.3, 0.1);
        world.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.NEUTRAL, 0.6f, 1.6f);
        Texts.bar(player, Texts.discovery("Program loaded. " + Program.parse(program).size() + " lines."));
        if (!player.isCreative()) {
            stack.decrement(1);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent display,
                              Consumer<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, display, tooltip, type);
        String program = stack.get(ModComponents.PROGRAM);
        if (program == null) {
            return;
        }
        List<String> listing = Program.parse(program).listing();
        for (int i = 0; i < Math.min(listing.size(), 8); i++) {
            tooltip.accept(Text.literal(listing.get(i)).formatted(Formatting.DARK_GREEN));
        }
        if (listing.size() > 8) {
            tooltip.accept(Text.literal("... " + (listing.size() - 8) + " more").formatted(Formatting.DARK_GRAY));
        }
    }
}
