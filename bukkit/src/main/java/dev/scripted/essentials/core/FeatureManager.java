package dev.scripted.essentials.core;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.storage.DataFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Owns every {@link Feature}: registration, persisted toggle state, and lifecycle. */
public final class FeatureManager {

    private final ScriptedEssentials plugin;
    private final Map<String, Feature> features = new LinkedHashMap<>();
    private final DataFile stateFile;

    public FeatureManager(ScriptedEssentials plugin) {
        this.plugin = plugin;
        this.stateFile = plugin.dataFile("features.yml");
    }

    /** Adds a feature and lets it claim its commands, listeners and data files. */
    public void register(Feature feature) {
        if (features.putIfAbsent(feature.id(), feature) != null) {
            plugin.getLogger().warning("Duplicate feature id '" + feature.id() + "' was ignored.");
            return;
        }
        feature.register();
    }

    /** Reads features.yml, filling in defaults for features that have never been seen before. */
    public void loadStates() {
        boolean dirty = false;
        for (Feature feature : features.values()) {
            String key = "features." + feature.id();
            if (!stateFile.get().contains(key)) {
                stateFile.get().set(key, feature.definition().enabledByDefault());
                dirty = true;
            }
        }
        if (dirty) {
            stateFile.save();
        }
    }

    /** Switches on every feature stored as enabled. Called once, after registration. */
    public void enableStored() {
        for (Feature feature : features.values()) {
            boolean shouldEnable = stateFile.get()
                    .getBoolean("features." + feature.id(), feature.definition().enabledByDefault());
            if (shouldEnable) {
                try {
                    feature.setEnabled(true);
                } catch (RuntimeException e) {
                    plugin.getLogger().warning("Feature '" + feature.id() + "' failed to start: "
                            + e.getMessage());
                }
            }
        }
    }

    /** Flips a feature and persists the new state. Returns the state it ended up in. */
    public boolean toggle(String id) {
        Feature feature = features.get(id.toLowerCase(Locale.ROOT));
        if (feature == null) {
            return false;
        }
        setEnabled(feature, !feature.isEnabled());
        return feature.isEnabled();
    }

    public void setEnabled(Feature feature, boolean enabled) {
        feature.setEnabled(enabled);
        stateFile.get().set("features." + feature.id(), enabled);
        stateFile.save();
    }

    public Optional<Feature> get(String id) {
        return Optional.ofNullable(features.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<Feature> all() {
        return features.values();
    }

    public List<Feature> byCategory(FeatureCategory category) {
        List<Feature> matches = new ArrayList<>();
        for (Feature feature : features.values()) {
            if (feature.definition().category() == category) {
                matches.add(feature);
            }
        }
        return matches;
    }

    public int enabledCount() {
        return (int) features.values().stream().filter(Feature::isEnabled).count();
    }

    public void reloadAll() {
        features.values().forEach(Feature::reload);
    }

    /** Stops every running feature and flushes their data files. */
    public void shutdown() {
        for (Feature feature : features.values()) {
            try {
                feature.setEnabled(false);
                feature.save();
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Feature '" + feature.id() + "' failed to stop cleanly: "
                        + e.getMessage());
            }
        }
        stateFile.save();
    }
}
