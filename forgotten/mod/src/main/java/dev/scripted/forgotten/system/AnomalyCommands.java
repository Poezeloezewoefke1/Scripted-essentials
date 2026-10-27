package dev.scripted.forgotten.system;

import dev.scripted.forgotten.Reality;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/** Forcing anomalies by stage, for operators and for tests. */
public final class AnomalyCommands {
    private AnomalyCommands() {
    }

    public static boolean force(ServerPlayerEntity player, int stage) {
        ServerWorld world = Reality.world(player);
        var random = world.getRandom();
        for (int attempt = 0; attempt < 8; attempt++) {
            boolean ok = switch (stage) {
                case 1 -> Anomalies.dejaVu(world, player, random);
                case 2 -> Anomalies.contradiction(world, player, random);
                case 3 -> Anomalies.fragmentation(world, player, random);
                case 4 -> Anomalies.amnesia(world, player, random);
                default -> Anomalies.unwritten(world, player, random);
            };
            if (ok) {
                return true;
            }
        }
        return false;
    }
}
