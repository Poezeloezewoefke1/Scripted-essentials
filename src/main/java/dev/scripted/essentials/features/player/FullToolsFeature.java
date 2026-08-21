package dev.scripted.essentials.features.player;

import dev.scripted.essentials.ScriptedEssentials;
import dev.scripted.essentials.command.SECommand;
import dev.scripted.essentials.core.Feature;
import dev.scripted.essentials.core.FeatureCategory;
import dev.scripted.essentials.core.FeatureDefinition;
import dev.scripted.essentials.util.Enchants;
import dev.scripted.essentials.util.Sounds;
import dev.scripted.essentials.util.Text;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@code /fulltools} — a fully enchanted set in the material tier you ask for.
 *
 * <p>Enchantments are chosen for player-versus-player combat:
 *
 * <ul>
 *   <li><b>No Thorns.</b> It costs extra armour durability on every hit you take and returns
 *       trivial damage, so it is a straight loss in a fight that lasts more than a few seconds.
 *   <li><b>No Knockback on the sword.</b> Pushing an opponent out of reach breaks your own combo;
 *       melee players deliberately leave it off.
 *   <li><b>Protection, not the situational protections.</b> Blast, Fire and Projectile Protection
 *       each beat Protection against one damage type only, and all four are mutually exclusive.
 *   <li><b>Fortune, not Silk Touch</b>, on the digging tools — also mutually exclusive.
 * </ul>
 *
 * <p>Every enchantment is applied at whatever this server says its maximum level is, rather than
 * a number hardcoded here, so a datapack that raises a cap is respected automatically.
 */
public final class FullToolsFeature extends Feature {

    private static final List<String> ARMOUR =
            List.of(Enchants.PROTECTION, Enchants.UNBREAKING, Enchants.MENDING);
    private static final List<String> HELMET_EXTRAS =
            List.of(Enchants.RESPIRATION, Enchants.AQUA_AFFINITY);
    private static final List<String> LEGGINGS_EXTRAS =
            List.of(Enchants.SWIFT_SNEAK);
    private static final List<String> BOOTS_EXTRAS =
            List.of(Enchants.FEATHER_FALLING, Enchants.DEPTH_STRIDER);

    private static final List<String> SWORD = List.of(Enchants.SHARPNESS, Enchants.FIRE_ASPECT,
            Enchants.LOOTING, Enchants.SWEEPING_EDGE, Enchants.UNBREAKING, Enchants.MENDING);
    // An axe disables shields, which makes Sharpness on one a real combat choice, not just a tool.
    private static final List<String> AXE = List.of(Enchants.SHARPNESS, Enchants.EFFICIENCY,
            Enchants.UNBREAKING, Enchants.MENDING);
    private static final List<String> DIGGING = List.of(Enchants.EFFICIENCY, Enchants.FORTUNE,
            Enchants.UNBREAKING, Enchants.MENDING);

    /**
     * The material tiers a set can be made from.
     *
     * <p>Not every tier covers both halves: wood and stone have no armour, and leather and
     * chainmail have no tools. Those tiers hand out only the half that exists.
     */
    private enum GearTier {
        LEATHER("LEATHER", null),
        CHAINMAIL("CHAINMAIL", null),
        WOODEN(null, "WOODEN"),
        STONE(null, "STONE"),
        GOLDEN("GOLDEN", "GOLDEN"),
        IRON("IRON", "IRON"),
        DIAMOND("DIAMOND", "DIAMOND"),
        NETHERITE("NETHERITE", "NETHERITE");

        private final String armourPrefix;
        private final String toolPrefix;

        GearTier(String armourPrefix, String toolPrefix) {
            this.armourPrefix = armourPrefix;
            this.toolPrefix = toolPrefix;
        }

        String lowerName() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** Accepts the enum name and the spellings players actually type. */
        static GearTier match(String input) {
            return switch (input.toLowerCase(Locale.ROOT)) {
                case "leather" -> LEATHER;
                case "chain", "chainmail" -> CHAINMAIL;
                case "wood", "wooden" -> WOODEN;
                case "stone" -> STONE;
                case "gold", "golden" -> GOLDEN;
                case "iron" -> IRON;
                case "diamond", "dia" -> DIAMOND;
                case "netherite", "neth" -> NETHERITE;
                default -> null;
            };
        }
    }

    public FullToolsFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("fulltools")
                .name("Full Tools")
                .description("Gives a fully enchanted tool and armour set in any material tier, "
                        + "with enchantments picked for PvP.")
                .icon(Material.NETHERITE_PICKAXE)
                .category(FeatureCategory.PLAYER)
                .controls("/fulltools [tier] [player]")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "fulltools", "godset") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                // The first argument is a tier if it names one, otherwise it is the target
                // player, so both /fulltools iron and /fulltools Notch read the way they look.
                GearTier tier = GearTier.NETHERITE;
                int targetIndex = 0;

                if (args.length > 0) {
                    GearTier named = GearTier.match(args[0]);
                    if (named != null) {
                        tier = named;
                        targetIndex = 1;
                    } else if (plugin.getServer().getPlayerExact(args[0]) == null) {
                        // Neither a tier nor anyone online: almost certainly a mistyped tier.
                        throw fail("fulltools-unknown-tier",
                                Text.placeholder("input", args[0]),
                                Text.placeholder("tiers", tierNames()));
                    }
                }
                Player target = resolveTarget(sender, args, targetIndex);

                int given = grant(target, tier);
                Sounds.play(plugin, target, Sounds.EQUIP, 0.8f, 1.0f);

                plugin.messages().send(sender, "fulltools-given",
                        Text.placeholder("player", target.getName()),
                        Text.placeholder("tier", tier.lowerName()),
                        Text.placeholder("count", String.valueOf(given)));
                if (sender != target) {
                    plugin.messages().send(target, "fulltools-received",
                            Text.placeholder("tier", tier.lowerName()));
                }
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                if (args.length == 1) {
                    List<String> options = new ArrayList<>();
                    for (GearTier value : GearTier.values()) {
                        options.add(value.lowerName());
                    }
                    options.addAll(onlineNames(sender, args[0]));
                    return filter(args[0], options);
                }
                return args.length == 2 ? onlineNames(sender, args[1]) : List.of();
            }
        }.describe("Give a fully enchanted set.", "/fulltools [tier] [player]"));
    }

    /** Hands out every piece the tier has. Returns how many items were given. */
    private int grant(Player player, GearTier tier) {
        int given = 0;
        if (tier.armourPrefix != null) {
            given += give(player, tier.armourPrefix + "_HELMET", ARMOUR, HELMET_EXTRAS);
            given += give(player, tier.armourPrefix + "_CHESTPLATE", ARMOUR, List.of());
            given += give(player, tier.armourPrefix + "_LEGGINGS", ARMOUR, LEGGINGS_EXTRAS);
            given += give(player, tier.armourPrefix + "_BOOTS", ARMOUR, BOOTS_EXTRAS);
        }
        if (tier.toolPrefix != null) {
            given += give(player, tier.toolPrefix + "_SWORD", SWORD, List.of());
            given += give(player, tier.toolPrefix + "_AXE", AXE, List.of());
            given += give(player, tier.toolPrefix + "_PICKAXE", DIGGING, List.of());
            given += give(player, tier.toolPrefix + "_SHOVEL", DIGGING, List.of());
            given += give(player, tier.toolPrefix + "_HOE", DIGGING, List.of());
        }
        player.updateInventory();
        return given;
    }

    /** Builds one enchanted piece and puts it in the player's hands. Returns 1 if it was made. */
    private int give(Player player, String materialName, List<String> base, List<String> extras) {
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            // A tier that does not have this piece, e.g. there is no chainmail sword.
            return 0;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }
        List<String> keys = new ArrayList<>(base);
        keys.addAll(extras);
        for (String key : keys) {
            Enchantment enchantment = Enchants.byKey(key);
            if (enchantment == null) {
                plugin.getLogger().warning("This server has no enchantment called '" + key + "'.");
                continue;
            }
            // Nothing is hidden and nothing is made unbreakable: the gear should read as normal
            // gear, wear down, and be repairable by the Mending on it.
            meta.addEnchant(enchantment, enchantment.getMaxLevel(), true);
        }
        item.setItemMeta(meta);

        player.getInventory().addItem(item).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        return 1;
    }

    private static String tierNames() {
        List<String> names = new ArrayList<>();
        for (GearTier tier : GearTier.values()) {
            names.add(tier.lowerName());
        }
        return String.join(", ", names);
    }
}
