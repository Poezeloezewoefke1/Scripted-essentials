package dev.scripted.essentials;

import dev.scripted.essentials.command.CommandRegistry;
import dev.scripted.essentials.command.RootCommand;
import dev.scripted.essentials.config.Messages;
import dev.scripted.essentials.core.FeatureCatalog;
import dev.scripted.essentials.core.FeatureManager;
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

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.messages = new Messages(this);
        this.commands = new CommandRegistry(this);
        this.features = new FeatureManager(this);

        this.prompts = new ChatPrompt(this);

        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(prompts, this);

        FeatureCatalog.populate(this, features);
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
    }

    /** Re-reads config.yml, messages.yml and every feature's own data files. */
    public void reloadEverything() {
        reloadConfig();
        messages.reload();
        features.reloadAll();
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
