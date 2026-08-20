package org.bukkit;

import org.bukkit.block.Block;
import org.bukkit.util.Vector;

public class Location implements Cloneable {
    public Location(World world, double x, double y, double z) { }
    public Location(World world, double x, double y, double z, float yaw, float pitch) { }
    public World getWorld() { throw new UnsupportedOperationException(); }
    public void setWorld(World world) { }
    public double getX() { throw new UnsupportedOperationException(); }
    public double getY() { throw new UnsupportedOperationException(); }
    public double getZ() { throw new UnsupportedOperationException(); }
    public int getBlockX() { throw new UnsupportedOperationException(); }
    public int getBlockY() { throw new UnsupportedOperationException(); }
    public int getBlockZ() { throw new UnsupportedOperationException(); }
    public void setX(double x) { }
    public void setY(double y) { }
    public void setZ(double z) { }
    public float getYaw() { throw new UnsupportedOperationException(); }
    public float getPitch() { throw new UnsupportedOperationException(); }
    public void setYaw(float yaw) { }
    public void setPitch(float pitch) { }
    public Block getBlock() { throw new UnsupportedOperationException(); }
    public Chunk getChunk() { throw new UnsupportedOperationException(); }
    public Location add(double x, double y, double z) { throw new UnsupportedOperationException(); }
    public Location add(Vector vector) { throw new UnsupportedOperationException(); }
    public Location subtract(double x, double y, double z) { throw new UnsupportedOperationException(); }
    public Location multiply(double factor) { throw new UnsupportedOperationException(); }
    public double distance(Location other) { throw new UnsupportedOperationException(); }
    public double distanceSquared(Location other) { throw new UnsupportedOperationException(); }
    public Vector toVector() { throw new UnsupportedOperationException(); }
    public Vector getDirection() { throw new UnsupportedOperationException(); }
    public Location setDirection(Vector vector) { throw new UnsupportedOperationException(); }
    @Override public Location clone() { throw new UnsupportedOperationException(); }
}
