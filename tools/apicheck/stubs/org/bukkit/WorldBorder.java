package org.bukkit;

public interface WorldBorder {
    Location getCenter();
    void setCenter(double x, double z);
    double getSize();
    void setSize(double newSize);
    void setSize(double newSize, long seconds);
    void setWarningDistance(int distance);
    void reset();
}
