package dev.scripted.essentials.core;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The static description of a feature: what it is called, what it looks like in the toggle menu,
 * and which commands it contributes. Built once per feature in its constructor.
 */
public final class FeatureDefinition {

    private final String id;
    private final String displayName;
    private final String description;
    private final Material icon;
    private final FeatureCategory category;
    private final List<String> controls;
    private final boolean enabledByDefault;

    private FeatureDefinition(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.icon = builder.icon;
        this.category = builder.category;
        this.controls = List.copyOf(builder.controls);
        this.enabledByDefault = builder.enabledByDefault;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public Material icon() {
        return icon;
    }

    public FeatureCategory category() {
        return category;
    }

    /** Human readable list of the commands this feature owns, shown in the menu. */
    public List<String> controls() {
        return controls;
    }

    public boolean enabledByDefault() {
        return enabledByDefault;
    }

    /** The permission that gates every command of this feature, e.g. {@code scriptedessentials.heal}. */
    public String permission() {
        return "scriptedessentials." + id;
    }

    public static final class Builder {

        private final String id;
        private String displayName;
        private String description = "";
        private Material icon = Material.PAPER;
        private FeatureCategory category = FeatureCategory.SYSTEMS;
        private final List<String> controls = new ArrayList<>();
        private boolean enabledByDefault = true;

        private Builder(String id) {
            this.id = id.toLowerCase(Locale.ROOT);
            this.displayName = id;
        }

        public Builder name(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder icon(Material icon) {
            this.icon = icon;
            return this;
        }

        public Builder category(FeatureCategory category) {
            this.category = category;
            return this;
        }

        public Builder controls(String... controls) {
            this.controls.addAll(List.of(controls));
            return this;
        }

        public Builder disabledByDefault() {
            this.enabledByDefault = false;
            return this;
        }

        public FeatureDefinition build() {
            return new FeatureDefinition(this);
        }
    }
}
