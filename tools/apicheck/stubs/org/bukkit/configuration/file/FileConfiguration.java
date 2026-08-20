package org.bukkit.configuration.file;

import org.bukkit.configuration.MemoryConfiguration;

import java.io.File;
import java.io.IOException;

public abstract class FileConfiguration extends MemoryConfiguration {
    public void save(File file) throws IOException { }
    public void save(String file) throws IOException { }
    public void load(File file) throws IOException { }
    public String saveToString() { throw new UnsupportedOperationException(); }
}
