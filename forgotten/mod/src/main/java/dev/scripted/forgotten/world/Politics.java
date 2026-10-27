package dev.scripted.forgotten.world;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Multiplayer politics: Architect Guilds and the territory they claim, the rules they set in it,
 * and the elected World Admin. One per server, stored with the Overworld.
 */
public final class Politics extends PersistentState {
    public static final Codec<Politics> CODEC = NbtCompound.CODEC.xmap(Politics::fromNbt, Politics::toNbt);
    public static final PersistentStateType<Politics> TYPE = new PersistentStateType<>("forgotten_politics", Politics::new, CODEC, null);

    /** What a guild can make true inside its territory. */
    public enum GuildRule {
        /** Their base floats: buildings in claimed chunks are lifted into the sky once. */
        FLOAT,
        /** Permanent night: the sun does not burn the undead there, and outsiders cannot see well. */
        NIGHT,
        /** No one outside the guild may break blocks there. */
        SEALED
    }

    public static final class Guild {
        public final String name;
        public UUID leader;
        public final Set<UUID> members = new LinkedHashSet<>();
        /** Claimed chunks, as "dimension|chunkKey". */
        public final Set<String> claims = new LinkedHashSet<>();
        public final Set<GuildRule> rules = new LinkedHashSet<>();

        Guild(String name, UUID leader) {
            this.name = name;
            this.leader = leader;
        }
    }

    public final Map<String, Guild> guilds = new LinkedHashMap<>();
    public UUID admin;
    public long adminUntil;
    public long electionEnds;
    public final Map<UUID, UUID> votes = new HashMap<>();

    public static Politics get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE);
    }

    public Guild guildOf(UUID player) {
        for (Guild g : guilds.values()) {
            if (g.members.contains(player)) {
                return g;
            }
        }
        return null;
    }

    public Guild owner(String dimension, long chunkKey) {
        String key = dimension + "|" + chunkKey;
        for (Guild g : guilds.values()) {
            if (g.claims.contains(key)) {
                return g;
            }
        }
        return null;
    }

    public Guild create(String name, UUID leader) {
        if (guilds.containsKey(name) || guildOf(leader) != null) {
            return null;
        }
        Guild g = new Guild(name, leader);
        g.members.add(leader);
        guilds.put(name, g);
        markDirty();
        return g;
    }

    public void leave(UUID player) {
        Guild g = guildOf(player);
        if (g == null) {
            return;
        }
        g.members.remove(player);
        if (g.members.isEmpty()) {
            guilds.remove(g.name);
        } else if (player.equals(g.leader)) {
            g.leader = g.members.iterator().next();
        }
        markDirty();
    }

    private NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        NbtList list = new NbtList();
        for (Guild g : guilds.values()) {
            NbtCompound c = new NbtCompound();
            c.putString("name", g.name);
            c.putString("leader", g.leader.toString());
            NbtList members = new NbtList();
            g.members.forEach(m -> members.add(NbtString.of(m.toString())));
            c.put("members", members);
            NbtList claims = new NbtList();
            g.claims.forEach(k -> claims.add(NbtString.of(k)));
            c.put("claims", claims);
            NbtList rules = new NbtList();
            g.rules.forEach(r -> rules.add(NbtString.of(r.name())));
            c.put("rules", rules);
            list.add(c);
        }
        nbt.put("guilds", list);
        if (admin != null) {
            nbt.putString("admin", admin.toString());
        }
        nbt.putLong("adminUntil", adminUntil);
        return nbt;
    }

    private static Politics fromNbt(NbtCompound nbt) {
        Politics p = new Politics();
        for (NbtElement el : nbt.getListOrEmpty("guilds")) {
            if (!(el instanceof NbtCompound c)) {
                continue;
            }
            try {
                Guild g = new Guild(c.getString("name", "?"), UUID.fromString(c.getString("leader", "")));
                for (NbtElement m : c.getListOrEmpty("members")) {
                    m.asString().ifPresent(s -> g.members.add(UUID.fromString(s)));
                }
                for (NbtElement k : c.getListOrEmpty("claims")) {
                    k.asString().ifPresent(g.claims::add);
                }
                for (NbtElement r : c.getListOrEmpty("rules")) {
                    r.asString().ifPresent(s -> g.rules.add(GuildRule.valueOf(s)));
                }
                p.guilds.put(g.name, g);
            } catch (IllegalArgumentException ignored) {
                // a guild that no longer makes sense is forgotten
            }
        }
        String admin = nbt.getString("admin", "");
        if (!admin.isEmpty()) {
            try {
                p.admin = UUID.fromString(admin);
            } catch (IllegalArgumentException ignored) {
                p.admin = null;
            }
        }
        p.adminUntil = nbt.getLong("adminUntil", 0);
        return p;
    }
}
