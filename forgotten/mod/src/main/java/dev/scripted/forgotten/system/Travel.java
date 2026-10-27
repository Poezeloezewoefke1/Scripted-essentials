package dev.scripted.forgotten.system;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.TeleportTarget;

/** Moving things between places and between worlds. */
public final class Travel {
    private Travel() {
    }

    public static Entity teleport(Entity entity, ServerWorld world, Vec3d pos, float yaw, float pitch) {
        return entity.teleportTo(new TeleportTarget(world, pos, Vec3d.ZERO, yaw, pitch, TeleportTarget.NO_OP));
    }

    /** A safe standing position at a column: the top solid block, loading the chunk if needed. */
    public static Vec3d surface(ServerWorld world, int x, int z) {
        world.getChunk(x >> 4, z >> 4);
        BlockPos top = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        if (top.getY() <= world.getBottomY()) {
            top = new BlockPos(x, world.getBottomY() + 64, z);
        }
        return Vec3d.ofBottomCenter(top);
    }
}
