package dev.scripted.forgotten.dimension;

import dev.scripted.forgotten.Forgotten;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

/**
 * The dimensions are not places, they are ideas: each is a different way reality can exist.
 * The JSON for each lives in data/forgotten/dimension; these are the keys code uses.
 */
public final class Dimensions {
    public static final RegistryKey<World> BACKSIDE = key("backside");
    public static final RegistryKey<World> UNWRITTEN = key("unwritten");
    public static final RegistryKey<World> STACK = key("stack");
    public static final RegistryKey<World> ARCHIVE = key("archive");
    public static final RegistryKey<World> UNDERDREAM = key("underdream");
    public static final RegistryKey<World> FIRST_WORLD = key("first_world");
    public static final int DEPTHS = 5;

    private Dimensions() {
    }

    static RegistryKey<World> key(String name) {
        return RegistryKey.of(RegistryKeys.WORLD, Forgotten.id(name));
    }

    public static RegistryKey<World> depth(int n) {
        return key("depth_" + n);
    }

    /** How deep in the Matryoshka nesting a world is: 0 for anything that is not a depth. */
    public static int depthOf(World world) {
        var id = world.getRegistryKey().getValue();
        if (id.getNamespace().equals(Forgotten.MOD_ID) && id.getPath().startsWith("depth_")) {
            try {
                return Integer.parseInt(id.getPath().substring("depth_".length()));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * The integrity a dimension's chunks start at. The Overworld (and Nether and End) start
     * fully remembered and depend on distance from spawn; the new dimensions are, by nature,
     * already somewhere along the scale. Returns -1 for "use the distance-based baseline".
     */
    public static int baselineIntegrity(World world) {
        RegistryKey<World> key = world.getRegistryKey();
        if (key == UNWRITTEN) {
            return 0;
        }
        if (key == STACK) {
            return 18;
        }
        if (key == BACKSIDE) {
            return 30;
        }
        if (key == UNDERDREAM) {
            return 40;
        }
        if (key == ARCHIVE) {
            return 70;
        }
        if (key == FIRST_WORLD) {
            return 100;
        }
        int depth = depthOf(world);
        return depth > 0 ? 100 - 12 * depth : -1;
    }

    public static boolean isDepth(ServerWorld world) {
        return depthOf(world) > 0;
    }
}
