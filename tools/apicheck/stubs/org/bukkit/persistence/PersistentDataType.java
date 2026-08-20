package org.bukkit.persistence;

public interface PersistentDataType<P, C> {
    PersistentDataType<String, String> STRING = null;
    PersistentDataType<Integer, Integer> INTEGER = null;
    PersistentDataType<Double, Double> DOUBLE = null;
    PersistentDataType<Byte, Byte> BYTE = null;
    PersistentDataType<Long, Long> LONG = null;
}
