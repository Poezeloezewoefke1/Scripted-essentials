package org.bukkit;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface World {
    String getName();
    UUID getUID();
    Environment getEnvironment();
    Location getSpawnLocation();
    boolean setSpawnLocation(Location location);
    int getHighestBlockYAt(int x, int z);
    int getHighestBlockYAt(Location location);
    Block getBlockAt(Location location);
    Block getBlockAt(int x, int y, int z);
    Chunk getChunkAt(Location location);
    Chunk getChunkAt(int x, int z);
    Chunk[] getLoadedChunks();
    boolean isChunkLoaded(int x, int z);
    boolean loadChunk(int x, int z, boolean generate);
    boolean unloadChunk(int x, int z);
    List<Entity> getEntities();
    Collection<Entity> getNearbyEntities(Location location, double x, double y, double z);
    List<Player> getPlayers();
    <T extends Entity> T spawn(Location location, Class<T> clazz);
    Entity spawnEntity(Location location, EntityType type);
    Item dropItem(Location location, ItemStack item);
    Item dropItemNaturally(Location location, ItemStack item);
    void strikeLightning(Location location);
    void strikeLightningEffect(Location location);
    void createExplosion(Location location, float power, boolean setFire, boolean breakBlocks);
    void playSound(Location location, String sound, float volume, float pitch);
    void spawnParticle(Particle particle, Location location, int count);
    void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ);
    void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra);
    WorldBorder getWorldBorder();
    long getTime();
    void setTime(long time);
    boolean isThundering();
    void setStorm(boolean hasStorm);
    void setThundering(boolean thundering);

    enum Environment { NORMAL, NETHER, THE_END, CUSTOM }
}
