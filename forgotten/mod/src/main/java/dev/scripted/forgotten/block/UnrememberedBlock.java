package dev.scripted.forgotten.block;

import com.mojang.serialization.MapCodec;
import dev.scripted.forgotten.logic.util.Hashing;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * An Unremembered Thing: a block the world made without remembering what it is. Its identity
 * is decided by where it is, and every so often it decides to be something else.
 */
public class UnrememberedBlock extends Block {
    public static final MapCodec<UnrememberedBlock> CODEC = createCodec(UnrememberedBlock::new);
    public static final IntProperty IDENTITY = IntProperty.of("identity", 0, 7);

    public UnrememberedBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(IDENTITY, 0));
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(IDENTITY);
    }

    public static int identityAt(BlockPos pos, long salt) {
        return Hashing.bounded(Hashing.position(salt, pos.getX(), pos.getY(), pos.getZ()), 8);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(IDENTITY, identityAt(ctx.getBlockPos(), 0));
    }

    /** Identity 0 shines; the rest give off only a faint light. */
    public static int luminance(BlockState state) {
        return state.get(IDENTITY) == 0 ? 13 : 3;
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (random.nextInt(6) == 0) {
            int next = identityAt(pos, world.getTime() / 24000L);
            world.setBlockState(pos, state.with(IDENTITY, next));
        }
        if (state.get(IDENTITY) == 5) {
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 0.6f, 0.5f + random.nextFloat());
        }
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (world.isClient()) {
            super.onSteppedOn(world, pos, state, entity);
            return;
        }
        switch (state.get(IDENTITY)) {
            case 1 -> {
                // It remembers being a slime.
                Vec3d v = entity.getVelocity();
                if (v.y < 0.1) {
                    entity.setVelocity(v.x, 0.9, v.z);
                    entity.velocityDirty = true;
                }
            }
            case 2 -> {
                // It remembers being ice.
                Vec3d v = entity.getVelocity();
                entity.setVelocity(v.x * 1.15, v.y, v.z * 1.15);
                entity.velocityDirty = true;
            }
            case 3 -> {
                // It remembers being hungry.
                if (entity instanceof ItemEntity item && world.getTime() % 20 == 0) {
                    item.discard();
                }
            }
            case 4 -> {
                if (entity instanceof LivingEntity living) {
                    living.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 0));
                }
            }
            case 6 -> {
                if (entity instanceof LivingEntity living) {
                    living.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 30, 0));
                }
            }
            case 7 -> {
                if (entity instanceof LivingEntity living) {
                    living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 3));
                }
            }
            default -> {
            }
        }
        super.onSteppedOn(world, pos, state, entity);
    }
}
