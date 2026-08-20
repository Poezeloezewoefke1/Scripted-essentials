package dev.scripted.essentials.features.world;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Text;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/** Inspecting and force-loading the chunk you are standing in. */
public final class ChunkToolsFeature extends Feature {

    public ChunkToolsFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("chunktools")
                .name("Chunk Tools")
                .description("Inspect the chunk you are in, and keep it loaded when nobody is nearby.")
                .icon(Material.MAP)
                .category(FeatureCategory.WORLD)
                .controls("/chunkinfo", "/chunkload", "/chunkunload")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "chunkinfo") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player player = asPlayer(sender);
                Chunk chunk = player.getLocation().getChunk();

                plugin.messages().send(sender, "chunk-info",
                        Text.placeholder("world", chunk.getWorld().getName()),
                        Text.placeholder("x", String.valueOf(chunk.getX())),
                        Text.placeholder("z", String.valueOf(chunk.getZ())),
                        Text.placeholder("entities", String.valueOf(countEntities(chunk))),
                        Text.placeholder("forced", chunk.isForceLoaded() ? "yes" : "no"));
            }
        }.playerOnly().describe("Show details about your current chunk.", "/chunkinfo"));

        command(new SECommand(plugin, "chunkload", "forceload") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player player = asPlayer(sender);
                Chunk chunk = player.getLocation().getChunk();
                chunk.setForceLoaded(true);
                plugin.messages().send(sender, "chunk-forced",
                        Text.placeholder("x", String.valueOf(chunk.getX())),
                        Text.placeholder("z", String.valueOf(chunk.getZ())));
            }
        }.playerOnly().describe("Keep the current chunk loaded.", "/chunkload"));

        command(new SECommand(plugin, "chunkunload", "unforceload") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                Player player = asPlayer(sender);
                Chunk chunk = player.getLocation().getChunk();
                chunk.setForceLoaded(false);
                plugin.messages().send(sender, "chunk-released",
                        Text.placeholder("x", String.valueOf(chunk.getX())),
                        Text.placeholder("z", String.valueOf(chunk.getZ())));
            }
        }.playerOnly().describe("Stop keeping the current chunk loaded.", "/chunkunload"));
    }

    private int countEntities(Chunk chunk) {
        Entity[] entities = chunk.getEntities();
        return entities == null ? 0 : entities.length;
    }
}
