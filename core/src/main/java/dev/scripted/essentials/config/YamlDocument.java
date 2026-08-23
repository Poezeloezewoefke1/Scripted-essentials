package dev.scripted.essentials.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A YAML document addressed by dotted paths.
 *
 * <p>This exists because Bukkit's {@code YamlConfiguration} is Bukkit-only, and config, messages
 * and stored data all have to be read identically on every platform. The API is deliberately
 * close to Bukkit's so the behaviour server owners already expect — missing key falls back to a
 * default, {@code set(path, null)} removes — carries over unchanged.
 *
 * <p>Values are plain YAML types only. Anything richer, an item stack in particular, is encoded
 * to a string by the platform before it gets here, which is also what lets a kit saved on Paper
 * be read on Fabric.
 */
public final class YamlDocument {

    private final Yaml yaml = createYaml();
    private Map<String, Object> root = new LinkedHashMap<>();
    private YamlDocument defaults;

    public static YamlDocument empty() {
        return new YamlDocument();
    }

    public static YamlDocument load(Reader reader) {
        YamlDocument document = new YamlDocument();
        document.read(reader);
        return document;
    }

    public static YamlDocument load(Path file) throws IOException {
        YamlDocument document = new YamlDocument();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                document.read(reader);
            }
        }
        return document;
    }

    /** Values to fall back on when a path is absent, mirroring Bukkit's defaults mechanism. */
    public void setDefaults(YamlDocument defaults) {
        this.defaults = defaults;
    }

    public void save(Path file) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            yaml.dump(root, writer);
        }
    }

    public String saveToString() {
        return yaml.dump(root);
    }

    // ---- reading ------------------------------------------------------------------------

    public boolean contains(String path) {
        return resolve(path) != null || (defaults != null && defaults.contains(path));
    }

    public Object get(String path) {
        Object value = resolve(path);
        if (value == null && defaults != null) {
            return defaults.get(path);
        }
        return value;
    }

    public String getString(String path) {
        return getString(path, null);
    }

    public String getString(String path, String fallback) {
        Object value = get(path);
        return value == null ? fallback : String.valueOf(value);
    }

    public int getInt(String path) {
        return getInt(path, 0);
    }

    public int getInt(String path, int fallback) {
        Object value = get(path);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    public long getLong(String path) {
        return getLong(path, 0L);
    }

    public long getLong(String path, long fallback) {
        Object value = get(path);
        return value instanceof Number number ? number.longValue() : fallback;
    }

    public double getDouble(String path) {
        return getDouble(path, 0.0);
    }

    public double getDouble(String path, double fallback) {
        Object value = get(path);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    public boolean getBoolean(String path, boolean fallback) {
        Object value = get(path);
        return value instanceof Boolean bool ? bool : fallback;
    }

    public List<String> getStringList(String path) {
        Object value = get(path);
        List<String> strings = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object element : list) {
                if (element != null) {
                    strings.add(String.valueOf(element));
                }
            }
        }
        return strings;
    }

    public List<Object> getList(String path) {
        Object value = get(path);
        return value instanceof List<?> list ? new ArrayList<>(list) : new ArrayList<>();
    }

    /** The immediate child keys of a section, or an empty set when the path is not a section. */
    public Set<String> getKeys(String path) {
        Object value = path == null || path.isEmpty() ? root : get(path);
        if (!(value instanceof Map<?, ?> map)) {
            return Set.of();
        }
        Set<String> keys = new LinkedHashSet<>();
        for (Object key : map.keySet()) {
            keys.add(String.valueOf(key));
        }
        return keys;
    }

    /** The top-level keys of the document. */
    public Set<String> getKeys() {
        return getKeys("");
    }

    // ---- writing ------------------------------------------------------------------------

    /** Sets a value, creating intermediate sections. A null value removes the entry. */
    @SuppressWarnings("unchecked")
    public void set(String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> node = root;

        for (int index = 0; index < parts.length - 1; index++) {
            Object child = node.get(parts[index]);
            if (!(child instanceof Map)) {
                if (value == null) {
                    return; // Nothing to remove along a branch that does not exist.
                }
                child = new LinkedHashMap<String, Object>();
                node.put(parts[index], child);
            }
            node = (Map<String, Object>) child;
        }

        String leaf = parts[parts.length - 1];
        if (value == null) {
            node.remove(leaf);
        } else {
            node.put(leaf, value);
        }
    }

    // ---- internals ----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void read(Reader reader) {
        Object loaded = yaml.load(reader);
        root = loaded instanceof Map ? (Map<String, Object>) loaded : new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private Object resolve(String path) {
        if (path == null || path.isEmpty()) {
            return root;
        }
        Object node = root;
        for (String part : path.split("\\.")) {
            if (!(node instanceof Map)) {
                return null;
            }
            node = ((Map<String, Object>) node).get(part);
            if (node == null) {
                return null;
            }
        }
        return node;
    }

    private static Yaml createYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        return new Yaml(options);
    }
}
