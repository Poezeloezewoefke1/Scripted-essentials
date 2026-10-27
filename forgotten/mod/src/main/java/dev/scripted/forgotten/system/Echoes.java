package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Reality;
import dev.scripted.forgotten.logic.chronicle.ChronicleEntry;
import dev.scripted.forgotten.logic.chronicle.ChronicleEvent;
import dev.scripted.forgotten.util.Scheduler;
import dev.scripted.forgotten.util.Texts;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * The Nether remembers pain. Die more than once in the same part of it and it grows a creature
 * out of the memory: a Wither Skeleton carrying a copy of what you were holding when you fell.
 */
public final class Echoes {
    private Echoes() {
    }

    public static void onPlayerDeath(ServerWorld world, ServerPlayerEntity player, DamageSource source) {
        if (world.getRegistryKey() != World.NETHER) {
            return;
        }
        BlockPos pos = player.getBlockPos();
        List<ChronicleEntry> deaths = Reality.memory(world.getServer()).chronicle
                .near(world.getRegistryKey().getValue().toString(), pos.getX(), pos.getY(), pos.getZ(), 48, ChronicleEvent.PLAYER_DIED);
        if (deaths.size() < 2) {
            return;
        }
        ItemStack weapon = player.getMainHandStack().copyWithCount(1);
        String name = player.getName().getString();
        Scheduler.after(600, () -> {
            WitherSkeletonEntity echo = EntityType.WITHER_SKELETON.create(world, SpawnReason.EVENT);
            if (echo == null) {
                return;
            }
            echo.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
            if (!weapon.isEmpty()) {
                echo.equipStack(EquipmentSlot.MAINHAND, weapon);
            }
            echo.setCustomName(Text.literal("Memory of " + name + "'s death").formatted(Formatting.DARK_RED));
            echo.setPersistent();
            world.spawnEntity(echo);
            ServerPlayerEntity back = world.getServer().getPlayerManager().getPlayer(player.getUuid());
            if (back != null && back.getServerWorld() == world) {
                Texts.tell(back, Texts.world("The Nether has not forgotten how you died here."));
            }
        });
    }
}
