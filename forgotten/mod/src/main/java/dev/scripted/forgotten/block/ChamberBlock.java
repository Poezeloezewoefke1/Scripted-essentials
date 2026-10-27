package dev.scripted.forgotten.block;

import com.mojang.serialization.MapCodec;
import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.system.Architecture;
import dev.scripted.forgotten.util.Texts;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Reality Architecture: a base that functions as its own law of nature. A Core of Identity holds
 * the structure together; chambers within 32 blocks of one each bend a rule inside their radius.
 */
public class ChamberBlock extends Block {
    public static final IntProperty MODE = IntProperty.of("mode", 0, 2);

    public enum Kind {
        /** Holds the architecture together. Chambers do nothing without one nearby. */
        CORE,
        /** Gravity in a direction you choose: down, up, or none. */
        GRAVITY,
        /** Time runs four times as fast. */
        TEMPORAL,
        /** Two doors joined regardless of the distance between them. Link with an ender pearl. */
        DISTANCE
    }

    private final Kind kind;

    public ChamberBlock(Settings settings, Kind kind) {
        super(settings);
        this.kind = kind;
        setDefaultState(getStateManager().getDefaultState().with(MODE, 0));
    }

    public static ChamberBlock core(Settings s) {
        return new ChamberBlock(s, Kind.CORE);
    }

    public static ChamberBlock gravity(Settings s) {
        return new ChamberBlock(s, Kind.GRAVITY);
    }

    public static ChamberBlock temporal(Settings s) {
        return new ChamberBlock(s, Kind.TEMPORAL);
    }

    public static ChamberBlock distance(Settings s) {
        return new ChamberBlock(s, Kind.DISTANCE);
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return switch (kind) {
            case CORE -> createCodec(ChamberBlock::core);
            case GRAVITY -> createCodec(ChamberBlock::gravity);
            case TEMPORAL -> createCodec(ChamberBlock::temporal);
            case DISTANCE -> createCodec(ChamberBlock::distance);
        };
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (world instanceof ServerWorld server && !oldState.isOf(this)) {
            Reality.region(server).setDevice(pos, kind.ordinal());
        }
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (!world.getBlockState(pos).isOf(this)) {
            Reality.region(world).removeDevice(pos);
            if (kind == Kind.DISTANCE) {
                Architecture.unlink(world, pos);
            }
        }
        super.onStateReplaced(state, world, pos, moved);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!(world instanceof ServerWorld server) || !(player instanceof ServerPlayerEntity sp)) {
            return ActionResult.SUCCESS;
        }
        if (kind == Kind.GRAVITY) {
            int next = (state.get(MODE) + 1) % 3;
            server.setBlockState(pos, state.with(MODE, next), Block.NOTIFY_ALL);
            Texts.bar(sp, Texts.world("Inside this chamber, things fall " + new String[]{"down.", "up.", "nowhere."}[next]));
            return ActionResult.SUCCESS;
        }
        if (kind == Kind.DISTANCE) {
            ItemStack held = sp.getStackInHand(Hand.MAIN_HAND);
            Architecture.link(server, sp, pos, held);
            return ActionResult.SUCCESS;
        }
        Texts.bar(sp, Texts.world(Architecture.describe(server, pos, kind)));
        return ActionResult.SUCCESS;
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        super.onSteppedOn(world, pos, state, entity);
        if (kind == Kind.DISTANCE && world instanceof ServerWorld server && entity instanceof ServerPlayerEntity player && !player.isSneaking()) {
            Architecture.step(server, player, pos);
        }
    }
}
