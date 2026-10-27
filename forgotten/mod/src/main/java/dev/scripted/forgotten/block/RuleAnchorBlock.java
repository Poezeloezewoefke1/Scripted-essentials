package dev.scripted.forgotten.block;

import com.mojang.serialization.MapCodec;
import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.util.Texts;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A Rule Anchor enforces one local law within sixteen blocks. Use it to choose which. The
 * Paradox Engine always finds a way to break the rules: during a Reality Storm they are
 * suspended.
 */
public class RuleAnchorBlock extends Block {
    public static final MapCodec<RuleAnchorBlock> CODEC = createCodec(RuleAnchorBlock::new);
    public static final IntProperty RULE = IntProperty.of("rule", 0, 3);
    public static final double RADIUS = 16;

    public enum Rule {
        NO_PVP("No harm between players."),
        NO_EXPLOSIONS("Nothing explodes."),
        NO_MOBS("Monsters cannot enter."),
        GRAVITY_OPTIONAL("Gravity is optional.");

        final String text;

        Rule(String text) {
            this.text = text;
        }

        public String text() {
            return text;
        }
    }

    public RuleAnchorBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(RULE, 0));
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(RULE);
    }

    /** Whether a law is in force at a place right now. */
    public static boolean enforced(ServerWorld world, double x, double y, double z, Rule rule) {
        return !Reality.storming(world.getServer()) && Reality.region(world).ruled(x, y, z, rule.ordinal(), RADIUS);
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (world instanceof ServerWorld server) {
            Reality.region(server).setRuleAnchor(pos, state.get(RULE));
        }
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (!world.getBlockState(pos).isOf(this)) {
            Reality.region(world).removeRuleAnchor(pos);
        }
        super.onStateReplaced(state, world, pos, moved);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world instanceof ServerWorld server && player instanceof ServerPlayerEntity sp) {
            int next = (state.get(RULE) + 1) % Rule.values().length;
            server.setBlockState(pos, state.with(RULE, next), Block.NOTIFY_ALL);
            Reality.region(server).setRuleAnchor(pos, next);
            server.playSound(null, pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.BLOCKS, 0.8f, 0.8f + next * 0.2f);
            Texts.bar(sp, Texts.world("Here: " + Rule.values()[next].text()));
        }
        return ActionResult.SUCCESS;
    }
}
