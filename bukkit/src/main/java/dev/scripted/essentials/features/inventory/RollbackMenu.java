package dev.scripted.essentials.features.inventory;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.gui.ItemBuilder;
import dev.scripted.essentials.gui.Menu;
import dev.scripted.essentials.util.Sounds;
import dev.scripted.essentials.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/** Lists a player's inventory snapshots, newest first, and restores the one that is clicked. */
public final class RollbackMenu extends Menu {

    private static final DateTimeFormatter STAMP_FORMAT =
            DateTimeFormatter.ofPattern("d MMM HH:mm:ss").withZone(ZoneId.systemDefault());

    private final InventoryRollbackFeature rollback;
    private final UUID subject;
    private final String subjectName;

    public RollbackMenu(ScriptedEssentials plugin, Player viewer, InventoryRollbackFeature rollback,
                        UUID subject, String subjectName) {
        super(plugin, viewer);
        this.rollback = rollback;
        this.subject = subject;
        this.subjectName = subjectName;
    }

    @Override
    protected Component title() {
        return Text.parse("<dark_gray>» <gray>Snapshots: <white>" + subjectName);
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void render() {
        List<Long> stamps = rollback.snapshots(subject);

        for (int slot = 0; slot < Math.min(45, stamps.size()); slot++) {
            long stamp = stamps.get(slot);
            String reason = rollback.reasonOf(subject, stamp);

            button(slot, ItemBuilder.of(iconFor(reason))
                    .name("<aqua>" + STAMP_FORMAT.format(Instant.ofEpochMilli(stamp)))
                    .lore("<gray>Reason: <white>" + reason)
                    .lore("<gray>Items: <white>" + rollback.itemCountOf(subject, stamp))
                    .blank()
                    .lore("<green>Click to restore")
                    .lore("<dark_gray>Their current inventory is saved first.")
                    .build(), event -> {
                Player target = Bukkit.getPlayer(subject);
                if (target == null) {
                    plugin.messages().send(viewer, "rollback-target-offline",
                            Text.placeholder("player", subjectName));
                    return;
                }
                if (rollback.restore(subject, stamp, target)) {
                    Sounds.success(plugin, viewer);
                    plugin.messages().send(viewer, "rollback-restored",
                            Text.placeholder("player", target.getName()));
                    plugin.messages().send(target, "rollback-restored-target");
                    viewer.closeInventory();
                }
            });
        }

        button(49, ItemBuilder.of(Material.BARRIER).name("<red>Close").build(),
                event -> viewer.closeInventory());
        fill(ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());
    }

    private Material iconFor(String reason) {
        return switch (reason) {
            case "death" -> Material.SKELETON_SKULL;
            case "quit" -> Material.OAK_DOOR;
            case "pre-restore" -> Material.CLOCK;
            default -> Material.PAPER;
        };
    }
}
