package dev.scripted.essentials.storage;

import dev.scripted.essentials.config.YamlDocument;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/** A YAML file on disk, created on demand and reloaded with the plugin. */
public final class DataFile {

    private final Path file;
    private final Logger logger;
    private YamlDocument document;

    public DataFile(Path file, Logger logger) {
        this.file = file;
        this.logger = logger;
        reload();
    }

    public YamlDocument get() {
        if (document == null) {
            reload();
        }
        return document;
    }

    public void reload() {
        try {
            this.document = YamlDocument.load(file);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not read " + file.getFileName()
                    + "; continuing with an empty document", e);
            this.document = YamlDocument.empty();
        }
    }

    public void save() {
        if (document == null) {
            return;
        }
        try {
            document.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not save " + file.getFileName(), e);
        }
    }

    public Path path() {
        return file;
    }
}
