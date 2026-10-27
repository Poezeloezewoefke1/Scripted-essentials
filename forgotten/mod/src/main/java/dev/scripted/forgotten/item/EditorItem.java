package dev.scripted.forgotten.item;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.paradox.Insight;
import dev.scripted.forgotten.system.Unraveling;
import dev.scripted.forgotten.util.Texts;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The Unmaker's Editor. Hold the declaration in your other hand and point: at a wall, and for a
 * minute it is made of whatever block you hold; at a creature while holding a chest, and it has
 * the properties of a chest; at nothing while holding a slime ball, and you jump like a slime.
 */
public class EditorItem extends LoreItem {
    public static final int SURFACE_LIMIT = 96;

    public EditorItem(Settings settings) {
        super(settings);
    }

    private static ItemStack declaration(PlayerEntity player, Hand hand) {
        return player.getStackInHand(hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!(world instanceof ServerWorld server) || !(user instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }
        ItemStack held = declaration(player, hand);
        StatusEffectInstance effect;
        String said;
        if (held.isOf(Items.SLIME_BALL)) {
            effect = new StatusEffectInstance(StatusEffects.JUMP_BOOST, Unraveling.EDIT_TICKS, 2);
            said = "the jump height of a slime";
        } else if (held.isOf(Items.FEATHER)) {
            effect = new StatusEffectInstance(StatusEffects.SLOW_FALLING, Unraveling.EDIT_TICKS, 0);
            said = "the weight of a feather";
        } else if (held.isOf(Items.SUGAR)) {
            effect = new StatusEffectInstance(StatusEffects.SPEED, Unraveling.EDIT_TICKS, 1);
            said = "the speed of sugar";
        } else if (held.isOf(Items.GLASS) || held.isOf(Items.GLASS_PANE)) {
            effect = new StatusEffectInstance(StatusEffects.INVISIBILITY, Unraveling.EDIT_TICKS / 2, 0);
            said = "the visibility of glass";
        } else if (held.isOf(Items.OBSIDIAN)) {
            effect = new StatusEffectInstance(StatusEffects.RESISTANCE, Unraveling.EDIT_TICKS / 2, 1);
            said = "the hardness of obsidian";
        } else {
            Texts.bar(player, Texts.info("Hold a declaration in your other hand: slime ball, feather, sugar, glass, obsidian, a block, a chest."));
            return ActionResult.FAIL;
        }
        player.addStatusEffect(effect);
        edited(server, player, user.getStackInHand(hand), hand, player.getBlockPos(), 1);
        Texts.bar(player, Texts.glitch("You have " + said + "."));
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!(context.getPlayer() instanceof ServerPlayerEntity player) || !(context.getWorld() instanceof ServerWorld world)) {
            return ActionResult.SUCCESS;
        }
        ItemStack held = declaration(player, context.getHand());
        if (!(held.getItem() instanceof BlockItem blockItem)) {
            return use(world, player, context.getHand());
        }
        BlockState into = blockItem.getBlock().getDefaultState();
        if (into.hasBlockEntity()) {
            Texts.bar(player, Texts.info("Things that hold things cannot be declared into walls."));
            return ActionResult.FAIL;
        }
        BlockPos pos = context.getBlockPos();
        int changed = Unraveling.rewriteSurface(world, pos, into, SURFACE_LIMIT);
        if (changed == 0) {
            return ActionResult.FAIL;
        }
        edited(world, player, context.getStack(), context.getHand(), pos, 1 + changed / 16);
        Texts.bar(player, Texts.glitch("It is now " + into.getBlock().getName().getString() + ". For a minute."));
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(user instanceof ServerPlayerEntity player) || !(user.getEntityWorld() instanceof ServerWorld world)) {
            return ActionResult.SUCCESS;
        }
        ItemStack held = declaration(player, hand);
        if (!(entity instanceof MobEntity mob)) {
            return ActionResult.PASS;
        }
        if (held.isOf(Items.CHEST) || held.isOf(Items.BARREL)) {
            boolean fresh = !Unraveling.isChest(mob);
            var inventory = Unraveling.chestOf(mob);
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, playerInventory, p) -> GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, inventory),
                    Text.literal(mob.getName().getString() + " (chest)")));
            if (fresh) {
                edited(world, player, stack, hand, mob.getBlockPos(), 1);
            }
            return ActionResult.SUCCESS;
        }
        if (held.isOf(Items.SLIME_BALL)) {
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, Unraveling.EDIT_TICKS, 3));
        } else if (held.isOf(Items.FEATHER)) {
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 100, 0));
        } else if (held.isOf(Items.COBWEB)) {
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, Unraveling.EDIT_TICKS, 4));
        } else {
            return ActionResult.PASS;
        }
        edited(world, player, stack, hand, mob.getBlockPos(), 1);
        return ActionResult.SUCCESS;
    }

    private static void edited(ServerWorld world, ServerPlayerEntity player, ItemStack stack, Hand hand, BlockPos at, int cost) {
        Reality.setIntegrity(world, at, Math.max(0, Reality.integrity(world, at) - cost));
        Reality.debt(player, cost);
        Reality.insight(player, Insight.REALITY_EDIT);
        stack.damage(1, player, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        world.spawnParticles(ParticleTypes.ENCHANT, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 30, 0.6, 0.6, 0.6, 0.5);
        world.playSound(null, at, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.8f, 1.5f);
    }
}
