package dev.scripted.forgotten.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Gravity + Recursion: gravity applied to itself until it pulls on everything. Draws items and
 * creatures inward, and drags loose blocks one step closer at a time until they clump around it.
 * Uses scheduled ticks rather than a block entity so it costs nothing when nothing is near.
 */
public class SingularityBlock extends Block {
    public static final MapCodec<SingularityBlock> CODEC = createCodec(SingularityBlock::new);
    public static final double RANGE = 10.0;

    public SingularityBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        world.scheduleBlockTick(pos, this, 2);
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        Vec3d center = Vec3d.ofCenter(pos);
        for (Entity e : world.getOtherEntities(null, new Box(pos).expand(RANGE))) {
            if (e instanceof PlayerEntity p && (p.isCreative() || p.isSpectator())) {
                continue;
            }
            if (!(e instanceof ItemEntity) && !(e instanceof LivingEntity)) {
                continue;
            }
            Vec3d toward = center.subtract(e.getEntityPos());
            double dist = Math.max(0.5, toward.length());
            Vec3d pull = toward.normalize().multiply(0.12 / Math.max(1.0, dist / 3.0));
            e.addVelocity(pull.x, pull.y, pull.z);
            e.velocityDirty = true;
        }
        if (random.nextInt(4) == 0) {
            pullBlock(world, pos, random);
        }
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 8, 0.6, 0.6, 0.6, 0.05);
        world.scheduleBlockTick(pos, this, 2);
    }

    private void pullBlock(ServerWorld world, BlockPos center, Random random) {
        BlockPos from = center.add(random.nextInt(9) - 4, random.nextInt(9) - 4, random.nextInt(9) - 4);
        if (from.equals(center)) {
            return;
        }
        BlockState state = world.getBlockState(from);
        if (state.isAir() || state.hasBlockEntity() || state.getHardness(world, from) < 0 || state.getHardness(world, from) > 5
                || state.isOf(this) || !state.getFluidState().isEmpty()) {
            return;
        }
        int dx = Integer.signum(center.getX() - from.getX());
        int dy = Integer.signum(center.getY() - from.getY());
        int dz = Integer.signum(center.getZ() - from.getZ());
        BlockPos to = from.add(dx, dy, dz);
        if (to.equals(center) || !world.getBlockState(to).isAir()) {
            return;
        }
        world.setBlockState(to, state, Block.NOTIFY_ALL);
        world.removeBlock(from, false);
    }

}
