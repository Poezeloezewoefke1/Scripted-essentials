package dev.scripted.forgotten.util;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Builds display entities. They are vanilla entities, so every client can draw them, and they
 * cannot be targeted or hit: the right material for things that are seen but are not there.
 */
public final class Displays {
    /** Tag on every display the mod spawns, so strays can be cleaned up after a restart. */
    public static final String TAG = "forgotten_display";
    /** Displays spawned this session. A tagged display not in here was loaded from an old save. */
    public static final java.util.Set<java.util.UUID> LIVE = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private Displays() {
    }

    public static DisplayEntity.BlockDisplayEntity block(ServerWorld world, Vec3d pos, BlockState state,
                                                          float sx, float sy, float sz, boolean dark) {
        DisplayEntity.BlockDisplayEntity d = EntityType.BLOCK_DISPLAY.create(world, SpawnReason.EVENT);
        if (d == null) {
            return null;
        }
        d.setBlockState(state);
        d.setTransformation(new AffineTransformation(new Vector3f(-sx / 2, 0, -sz / 2), new Quaternionf(),
                new Vector3f(sx, sy, sz), new Quaternionf()));
        if (dark) {
            d.setBrightness(new Brightness(0, 0));
        }
        d.setViewRange(4.0f);
        d.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0, 0);
        d.addCommandTag(TAG);
        LIVE.add(d.getUuid());
        world.spawnEntity(d);
        return d;
    }

    /** A block-sized ghost at a block position: the remembered shape of something demolished. */
    public static DisplayEntity.BlockDisplayEntity ghost(ServerWorld world, net.minecraft.util.math.BlockPos pos, BlockState state) {
        DisplayEntity.BlockDisplayEntity d = EntityType.BLOCK_DISPLAY.create(world, SpawnReason.EVENT);
        if (d == null) {
            return null;
        }
        d.setBlockState(state);
        d.setTransformation(new AffineTransformation(new Vector3f(0.02f, 0.02f, 0.02f), new Quaternionf(),
                new Vector3f(0.96f, 0.96f, 0.96f), new Quaternionf()));
        d.setGlowing(true);
        d.setGlowColorOverride(0xB48CFF);
        d.refreshPositionAndAngles(pos.getX(), pos.getY(), pos.getZ(), 0, 0);
        d.addCommandTag(TAG);
        LIVE.add(d.getUuid());
        world.spawnEntity(d);
        return d;
    }

    public static DisplayEntity.TextDisplayEntity text(ServerWorld world, Vec3d pos, Text text) {
        DisplayEntity.TextDisplayEntity d = EntityType.TEXT_DISPLAY.create(world, SpawnReason.EVENT);
        if (d == null) {
            return null;
        }
        d.setText(text);
        d.setBackground(0x00000000);
        d.setBillboardMode(DisplayEntity.BillboardMode.CENTER);
        d.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0, 0);
        d.addCommandTag(TAG);
        LIVE.add(d.getUuid());
        world.spawnEntity(d);
        return d;
    }

    public static DisplayEntity.ItemDisplayEntity item(ServerWorld world, Vec3d pos, ItemStack stack, float scale) {
        DisplayEntity.ItemDisplayEntity d = EntityType.ITEM_DISPLAY.create(world, SpawnReason.EVENT);
        if (d == null) {
            return null;
        }
        d.setItemStack(stack);
        d.setTransformation(new AffineTransformation(new Vector3f(), new Quaternionf(), new Vector3f(scale, scale, scale), new Quaternionf()));
        d.setBillboardMode(DisplayEntity.BillboardMode.CENTER);
        d.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0, 0);
        d.addCommandTag(TAG);
        LIVE.add(d.getUuid());
        world.spawnEntity(d);
        return d;
    }
}
