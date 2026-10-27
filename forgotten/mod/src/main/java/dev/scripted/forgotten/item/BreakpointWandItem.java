package dev.scripted.forgotten.item;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.paradox.Insight;
import dev.scripted.forgotten.registry.ModAttachments;
import dev.scripted.forgotten.system.BioNodes;
import dev.scripted.forgotten.util.Texts;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Reality debugging. Use to hit a breakpoint: everything within a few blocks stops where it is.
 * While something is paused you can read its internal values, and change some of them - a
 * furnace's burn time, a creeper's explosion radius - at the cost of causal debt.
 */
public class BreakpointWandItem extends LoreItem {
    public static final int RADIUS = 5;
    public static final int PAUSE = 200;
    private static final int[] RADII = {0, 1, 3, 6, 12};

    public BreakpointWandItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!(world instanceof ServerWorld server) || !(user instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }
        int frozen = pause(server, player.getEntityPos(), player);
        Texts.bar(player, Texts.info("Breakpoint hit. " + frozen + " things paused."));
        player.getItemCooldownManager().set(user.getStackInHand(hand), 60);
        return ActionResult.SUCCESS;
    }

    /** Pauses every creature, projectile and dropped item within the radius. */
    public static int pause(ServerWorld world, Vec3d at, Entity except) {
        long until = Reality.now(world.getServer()) + PAUSE;
        int count = 0;
        Box box = new Box(at, at).expand(RADIUS);
        for (Entity e : world.getOtherEntities(except, box, e -> e.isAlive() && !(e instanceof PlayerEntity))) {
            if (e instanceof MobEntity mob) {
                mob.setAttached(ModAttachments.FROZEN_UNTIL, until);
                mob.setAiDisabled(true);
            }
            if (e instanceof MobEntity || e instanceof ProjectileEntity || e instanceof net.minecraft.entity.ItemEntity) {
                e.setVelocity(Vec3d.ZERO);
                e.velocityDirty = true;
                count++;
            }
        }
        world.spawnParticles(ParticleTypes.END_ROD, at.x, at.y + 1, at.z, 40, RADIUS / 2.0, 1.0, RADIUS / 2.0, 0.0);
        world.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.8f, 2f);
        return count;
    }

    static boolean paused(Entity entity) {
        Long until = entity.getAttached(ModAttachments.FROZEN_UNTIL);
        return until != null && entity.getEntityWorld() instanceof ServerWorld w && Reality.now(w.getServer()) < until;
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(user instanceof ServerPlayerEntity player) || !(user.getEntityWorld() instanceof ServerWorld world)) {
            return ActionResult.SUCCESS;
        }
        if (!paused(entity)) {
            pause(world, entity.getEntityPos(), player);
            Texts.bar(player, Texts.info("Breakpoint set. Use again to inspect."));
            return ActionResult.SUCCESS;
        }
        player.sendMessage(Text.literal("[" + entity.getType().getName().getString() + "] health=" + fmt(entity.getHealth())
                + "/" + fmt(entity.getMaxHealth()) + " speed=" + fmt(entity.getAttributeValue(EntityAttributes.MOVEMENT_SPEED))
                + " age=" + entity.age).formatted(Formatting.AQUA), false);
        if (entity instanceof CreeperEntity creeper) {
            int next = RADII[0];
            for (int i = 0; i < RADII.length; i++) {
                if (RADII[i] == creeper.explosionRadius) {
                    next = RADII[(i + 1) % RADII.length];
                }
            }
            if (player.isSneaking()) {
                player.sendMessage(Text.literal("  explosionRadius: " + creeper.explosionRadius + " -> " + next).formatted(Formatting.YELLOW), false);
                creeper.explosionRadius = next;
                Reality.debt(player, 4);
                Reality.insight(player, Insight.REALITY_EDIT);
            } else {
                player.sendMessage(Text.literal("  explosionRadius: " + creeper.explosionRadius + "  (crouch-use to change)").formatted(Formatting.GRAY), false);
            }
        }
        if (entity instanceof MobEntity mob && BioNodes.isNode(mob)) {
            if (player.isSneaking() && BioNodes.restart(mob)) {
                player.sendMessage(Text.literal("  restarted from line 1").formatted(Formatting.YELLOW), false);
            }
            for (String line : BioNodes.inspect(mob)) {
                player.sendMessage(Text.literal("  " + line).formatted(Formatting.DARK_GREEN), false);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!(context.getPlayer() instanceof ServerPlayerEntity player) || !(context.getWorld() instanceof ServerWorld world)) {
            return ActionResult.SUCCESS;
        }
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        StringBuilder sb = new StringBuilder("[").append(state.getBlock().getName().getString()).append("]");
        for (Property<?> property : state.getProperties()) {
            sb.append(' ').append(property.getName()).append('=').append(state.get(property));
        }
        player.sendMessage(Text.literal(sb.toString()).formatted(Formatting.AQUA), false);
        if (world.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace) {
            player.sendMessage(Text.literal("  litTimeRemaining=" + furnace.litTimeRemaining + " litTotalTime=" + furnace.litTotalTime
                    + (player.isSneaking() ? "" : "  (crouch-use to change)")).formatted(Formatting.GRAY), false);
            if (player.isSneaking()) {
                furnace.litTimeRemaining = Integer.MAX_VALUE / 2;
                furnace.litTotalTime = Integer.MAX_VALUE / 2;
                if (state.contains(AbstractFurnaceBlock.LIT)) {
                    world.setBlockState(pos, state.with(AbstractFurnaceBlock.LIT, true), Block.NOTIFY_ALL);
                }
                furnace.markDirty();
                player.sendMessage(Text.literal("  litTimeRemaining -> ∞").formatted(Formatting.YELLOW), false);
                Reality.debt(player, 6);
                Reality.paradox(world, pos, 2);
                Reality.insight(player, Insight.REALITY_EDIT);
            }
        }
        return ActionResult.SUCCESS;
    }

    private static String fmt(double d) {
        return String.format("%.2f", d);
    }
}
