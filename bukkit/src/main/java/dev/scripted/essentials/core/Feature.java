package dev.scripted.essentials.core;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.storage.DataFile;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

/**
 * A single toggleable unit of behaviour.
 *
 * <p>Commands and listeners are registered exactly once, when the plugin starts, and stay
 * registered for the lifetime of the server. Turning a feature off flips {@link #isEnabled()};
 * commands then refuse to run and listeners return early. Doing it this way means a toggle can be
 * flipped any number of times without leaking handlers or leaving stale command entries behind in
 * the server's command map.
 *
 * <p>{@link #onEnable()} and {@link #onDisable()} are for state that genuinely must start and stop:
 * repeating tasks, bossbars, applied potion effects.
 */
public abstract class Feature {

    protected final ScriptedEssentials plugin;
    private final FeatureDefinition definition;
    private final List<DataFile> dataFiles = new ArrayList<>();
    private boolean enabled;

    protected Feature(ScriptedEssentials plugin, FeatureDefinition definition) {
        this.plugin = plugin;
        this.definition = definition;
    }

    public final FeatureDefinition definition() {
        return definition;
    }

    public final String id() {
        return definition.id();
    }

    public final boolean isEnabled() {
        return enabled;
    }

    /**
     * Called once at startup, whatever the stored toggle state. Register commands, listeners and
     * data files here.
     */
    protected void onRegister() {
    }

    /** Called when the feature is switched on, and at startup if it is stored as on. */
    protected void onEnable() {
    }

    /** Called when the feature is switched off, and at shutdown if it was on. */
    protected void onDisable() {
    }

    /** Called when {@code /se reload} runs, whatever the toggle state. */
    protected void onReload() {
    }

    final void register() {
        onRegister();
    }

    final void setEnabled(boolean value) {
        if (this.enabled == value) {
            return;
        }
        this.enabled = value;
        if (value) {
            onEnable();
        } else {
            onDisable();
        }
    }

    final void reload() {
        dataFiles.forEach(DataFile::reload);
        onReload();
    }

    final void save() {
        dataFiles.forEach(DataFile::save);
    }

    /** Registers a listener for this feature. The listener must guard on {@link #isEnabled()}. */
    protected final void listener(Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    /** Registers a command owned by this feature. Disabled features reject their own commands. */
    protected final void command(SECommand command) {
        plugin.commands().register(command.owner(this));
    }

    /**
     * Registers a command that stays usable while the feature is switched off.
     *
     * <p>Only for the commands that toggle their own feature: binding those to the feature would
     * make it impossible to switch back on from chat.
     */
    protected final void commandUnbound(SECommand command) {
        plugin.commands().register(command);
    }

    /** Switches this feature on or off and persists the new state. */
    protected final void setFeatureEnabled(boolean enabled) {
        plugin.features().setEnabled(this, enabled);
    }

    /** Creates a data file under {@code plugins/ScriptedEssentials/}, reloaded with the plugin. */
    protected final DataFile data(String path) {
        DataFile file = plugin.dataFile(path);
        dataFiles.add(file);
        return file;
    }
}
