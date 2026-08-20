package org.bukkit.util;

import org.bukkit.Location;
import org.bukkit.World;

public class Vector implements Cloneable {
    public Vector() { }
    public Vector(double x, double y, double z) { }
    public double getX() { throw new UnsupportedOperationException(); }
    public double getY() { throw new UnsupportedOperationException(); }
    public double getZ() { throw new UnsupportedOperationException(); }
    public Vector setX(double x) { throw new UnsupportedOperationException(); }
    public Vector setY(double y) { throw new UnsupportedOperationException(); }
    public Vector setZ(double z) { throw new UnsupportedOperationException(); }
    public Vector add(Vector other) { throw new UnsupportedOperationException(); }
    public Vector subtract(Vector other) { throw new UnsupportedOperationException(); }
    public Vector multiply(double factor) { throw new UnsupportedOperationException(); }
    public Vector normalize() { throw new UnsupportedOperationException(); }
    public double length() { throw new UnsupportedOperationException(); }
    public Location toLocation(World world) { throw new UnsupportedOperationException(); }
    @Override public Vector clone() { throw new UnsupportedOperationException(); }
}
