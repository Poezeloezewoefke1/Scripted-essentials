package dev.scripted.essentials;

import dev.scripted.essentials.command.CommandRegistry;
import dev.scripted.essentials.command.RootCommand;
import dev.scripted.essentials.config.Messages;
import dev.scripted.essentials.core.FeatureCatalog;
import dev.scripted.essentials.core.FeatureManager;
import dev.scripted.essentials.core.PermissionRegistrar;
import dev.scripted.essentials.gui.MenuListener;
import dev.scripted.essentials.util.ChatPrompt;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point for ScriptedEssentials.
 *
 * <p>Every piece of behaviour in this plugin lives in a {@link dev.scripted.essentials.core.Feature},
 * which can be switched on and off at runtime from {@code /se} without a restart. This class only
 * wires the managers together and hands control to the {@link FeatureManager}.
 */
public final class ScriptedEssentials extends JavaPlugin {

    private Messages messages;
    private CommandRegistry commands;
    private FeatureManager features;
    private ChatPrompt prompts;
    private dev.scripted.essentials.config.YamlDocument configDocument;
    private PermissionRegistrar permissions;

    @Override
    public void onEnable() {
        reloadConfigDocument();

        this.messages = new Messages();
        reloadMessages();
        this.commands = new CommandRegistry(this);
        this.features = new FeatureManager(this);

        this.prompts = new ChatPrompt(this);

        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(prompts, this);

        FeatureCatalog.populate(this, features);

        this.permissions = new PermissionRegistrar(this);
        permissions.registerAll();

        features.loadStates();
        features.enableStored();

        commands.register(new RootCommand(this));

        getLogger().info("Enabled with " + features.enabledCount() + "/" + features.all().size()
                + " features active.");
    }

    @Override
    public void onDisable() {
        if (features != null) {
            features.shutdown();
        }
        if (permissions != null) {
            permissions.unregisterAll();
        }
    }

    /** Re-reads config.yml, messages.yml and every feature's own data files. */
    public void reloadEverything() {
        reloadConfigDocument();
        reloadMessages();
        features.reloadAll();
    }

    /** Reads messages.yml, falling back to the copy inside the jar for anything missing. */
    private void reloadMessages() {
        saveResource("messages.yml", false);
        try {
            messages.load(getDataFolder().toPath().resolve("messages.yml"),
                    getResource("messages.yml"));
        } catch (java.io.IOException e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Could not read messages.yml", e);
        }
    }

    /**
     * config.yml, read through the shared YAML layer rather than Bukkit's own.
     *
     * <p>Using the platform-neutral reader here means the Fabric mod reads exactly the same file
     * with exactly the same semantics, instead of two config dialects drifting apart.
     */
    public dev.scripted.essentials.config.YamlDocument config() {
        return configDocument;
    }

    public void saveConfigDocument() {
        try {
            configDocument.save(getDataFolder().toPath().resolve("config.yml"));
        } catch (java.io.IOException e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Could not save config.yml", e);
        }
    }

    private void reloadConfigDocument() {
        saveResource("config.yml", false);
        try {
            this.configDocument = dev.scripted.essentials.config.YamlDocument.load(
                    getDataFolder().toPath().resolve("config.yml"));
            java.io.InputStream bundled = getResource("config.yml");
            if (bundled != null) {
                try (java.io.Reader reader = new java.io.InputStreamReader(
                        bundled, java.nio.charset.StandardCharsets.UTF_8)) {
                    configDocument.setDefaults(
                            dev.scripted.essentials.config.YamlDocument.load(reader));
                }
            }
        } catch (java.io.IOException e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Could not read config.yml", e);
            this.configDocument = dev.scripted.essentials.config.YamlDocument.empty();
        }
    }

    /** Opens a YAML file inside the plugin's data folder. */
    public dev.scripted.essentials.storage.DataFile dataFile(String relativePath) {
        return new dev.scripted.essentials.storage.DataFile(
                getDataFolder().toPath().resolve(relativePath), getLogger());
    }

    public Messages messages() {
        return messages;
    }

    public CommandRegistry commands() {
        return commands;
    }

    public FeatureManager features() {
        return features;
    }

    /** Shared "type your answer in chat" helper, used by the editor menus. */
    public ChatPrompt prompts() {
        return prompts;
    }
}
