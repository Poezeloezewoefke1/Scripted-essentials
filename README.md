# ScriptedEssentials

An all-in-one essentials suite for Paper servers. Every piece of behaviour is a **feature** that
can be switched on or off at runtime from a single in-game control panel — no restart, no config
editing, no reload.

**53 features**, one toggle each.

```
/se
```

---

## Requirements

| | |
| --- | --- |
| Server | Paper 1.21.x (or a Paper fork: Purpur, Pufferfish) |
| Java | 21 |
| Dependencies | none |

Paper specifically, not Spigot: the plugin uses Paper's command map access, Adventure text
components and the async chat event. There are no hard dependencies — PlaceholderAPI and a voice
chat plugin are recognised if present and ignored if not.

## Building

```sh
mvn package
```

The jar lands in `target/ScriptedEssentials-1.0.0.jar`. Drop it in `plugins/` and start the
server; `config.yml`, `messages.yml`, `features.yml` and a `data/` folder are created on first run.

> The build resolves `io.papermc.paper:paper-api` from `repo.papermc.io`, so that host has to be
> reachable from wherever you build.

## The control panel

`/se` opens the panel. Each icon is one feature and shows its current state, the commands it owns
and a short description; clicking flips it. Category tabs across the bottom filter the grid.

Everything the panel does is also available from chat and the console:

| Command | Effect |
| --- | --- |
| `/se` | Open the control panel |
| `/se list` | List every feature and its state |
| `/se toggle <feature>` | Flip one feature |
| `/se enable <feature>` / `/se disable <feature>` | Set one feature explicitly |
| `/se reload` | Re-read `config.yml`, `messages.yml` and every data file |
| `/se version` | Show the version |

Toggle state lives in `features.yml` and survives restarts. 9 features ship switched **off**
because they change how the server plays (keep inventory, server lock, orbital weapons and so on) —
turn them on deliberately.

---

## Features

### Player

| Feature | Commands | Permission | What it does |
| --- | --- | --- | --- |
| **Durability** | `/durability` | `scriptedessentials.durability` | Sets the held item's remaining durability to an exact percentage. |
| **Feed** | `/feed` | `scriptedessentials.feed` | Refills a player's hunger and saturation. |
| **Fly** | `/fly`, `/flyspeed` | `scriptedessentials.fly` | Toggles flight, with fall damage protection on landing and a speed control. |
| **Full Tools** | `/fulltools` | `scriptedessentials.fulltools` | Gives a fully enchanted, unbreakable netherite tool and armour set. |
| **Gamemodes** | `/gm`, `/gmc`, `/gms`, `/gma`, `/gmsp` | `scriptedessentials.gamemode` | Switch game mode, with a shorthand command for each mode. |
| **Heal** | `/heal` | `scriptedessentials.heal` | Restores health and hunger, puts out fire and clears potion effects. |
| **Hunger** | `/hunger` | `scriptedessentials.hunger` | Sets a player's hunger bar to an exact value between 0 and 20. |
| **Invisibility** | `/invis` | `scriptedessentials.invisibility` | Toggles an endless, particle-free invisibility effect. |
| **No Effects** | `/noeffects` | `scriptedessentials.noeffects` | Removes every active potion effect from a player. |
| **Pause Effects** | `/pauseeffects` | `scriptedessentials.pauseeffects` | Freezes your potion effect timers, then restores the time left when resumed. |
| **Potion Presets** | `/potion` | `scriptedessentials.potionpresets` | Applies a named set of potion effects configured in config.yml. |
| **Random Head** | `/head`, `/randomhead` | `scriptedessentials.randomhead` | Gives the head of a named player, or of a random player who is online. |
| **Repair** | `/repair`, `/repair all` | `scriptedessentials.repair` | Repairs the item in hand, or every damaged item you are carrying. |
| **Set Health** | `/sethealth` | `scriptedessentials.sethealth` | Sets a player's health to an exact number of half-hearts. |
| **Totem** | `/totem`, `/totem auto` | `scriptedessentials.totem` | Gives totems of undying, and can auto-swap one into your off hand. |

### Teleportation

| Feature | Commands | Permission | What it does |
| --- | --- | --- | --- |
| **Custom Spawn** | `/spawn`, `/setspawn` | `scriptedessentials.spawn` | A server spawn point, optionally used on join and on respawn. |
| **Offline Teleport** | `/offlinetp` | `scriptedessentials.offlinetp` | Teleports you to the last place an offline player logged out. |
| **Top Teleport** | `/top` | `scriptedessentials.top` | Teleports you to the highest block directly above you. |
| **Warp System** | `/warp`, `/warps`, `/setwarp`, `/delwarp` | `scriptedessentials.warps` | Named teleport destinations with a menu and per-warp permissions. |

### Inventory

| Feature | Commands | Permission | What it does |
| --- | --- | --- | --- |
| **Custom Item Maker** | `/itemmaker` | `scriptedessentials.itemmaker` | Rename items, write lore, add enchantments and set flags from a menu. |
| **Ender Chest View** | `/echest` | `scriptedessentials.enderchest` | Opens your ender chest, or another player's. |
| **Inventory Rollback** | `/rollback` | `scriptedessentials.inventoryrollback` | Saves inventory snapshots on death and quit, restorable from a menu. |
| **Inventory See** | `/invsee` | `scriptedessentials.invsee` | Opens another player's inventory. Edits apply to them immediately. |
| **Keep Inventory** *(off by default)* | `/keepinv` | `scriptedessentials.keepinventory` | Players keep their items and experience when they die. |
| **Kit System** | `/kit`, `/kits`, `/createkit`, `/delkit` | `scriptedessentials.kits` | Saved loadouts with cooldowns, permissions and a kit menu. |

### Moderation

| Feature | Commands | Permission | What it does |
| --- | --- | --- | --- |
| **Command Blocker** | `/cmdblock` | `scriptedessentials.commandblocker` | Blocks listed commands, including their plugin-prefixed forms. |
| **Freeze** | `/freeze` | `scriptedessentials.freeze` | Locks a player in place, blocks their commands, and warns staff if they log out. |
| **Half Heart** *(off by default)* | `/halfheart` | `scriptedessentials.halfheart` | No single hit can kill: damage is capped at half a heart of health. |
| **Recording Mode** | `/recording` | `scriptedessentials.recordingmode` | Hides chat, blocks stray commands and can hide other players while filming. |
| **Server Lock** *(off by default)* | `/serverlock` | `scriptedessentials.serverlock` | Only staff may join, and the server list shows a locked message. |
| **Vanish** | `/vanish` | `scriptedessentials.vanish` | Hides you from the world and the tab list, with silent join and quit. |

### World

| Feature | Commands | Permission | What it does |
| --- | --- | --- | --- |
| **Auto Clear** *(off by default)* | `/autoclear` | `scriptedessentials.autoclear` | Sweeps dropped items off the ground on a timer, with a countdown warning. |
| **Chunk Tools** | `/chunkinfo`, `/chunkload`, `/chunkunload` | `scriptedessentials.chunktools` | Inspect the chunk you are in, and keep it loaded when nobody is nearby. |
| **Dimension Lock** | `/dimlock` | `scriptedessentials.dimensionlock` | Closes chosen worlds. Portals and teleports into them are refused. |
| **Fake World Border** *(off by default)* | `/fakeborder` | `scriptedessentials.fakeworldborder` | Pushes players back at a set distance from spawn, per world. |

### Chat

| Feature | Commands | Permission | What it does |
| --- | --- | --- | --- |
| **Chat & Sign Filters** | `/chatfilter` | `scriptedessentials.chatfilter` | Blocks or censors listed words in chat and on signs, and stops spam. |
| **Chat Mute** | `/chatmute`, `/mute`, `/unmute` | `scriptedessentials.chatmute` | Lock global chat, or mute one player for a set time. |
| **Clear Chat** | `/clearchat` | `scriptedessentials.clearchat` | Wipes the chat window for everyone without the bypass permission. |
| **Command Feedback** *(off by default)* | — | `scriptedessentials.commandfeedback` | Reports staff command use to other staff and to the console. |
| **Feedback Sounds** | — | `scriptedessentials.feedbacksounds` | Plays a short sound when a menu button or command succeeds. |
| **Join & Leave Messages** | — | `scriptedessentials.joinleavemessages` | Custom join, first-join and leave broadcasts. |
| **Private Messages** | `/msg`, `/r`, `/msgtoggle` | `scriptedessentials.privatemessages` | Direct messages with a reply command and an opt-out toggle. |
| **Team Chat** | `/tc` | `scriptedessentials.teamchat` | A private chat channel for each team, with a latching toggle. |
| **Voice Chat Mute** | `/vcmute`, `/vcunmute` | `scriptedessentials.voicechatmute` | Mutes a player in voice chat by revoking their speak permission. |

### Systems

| Feature | Commands | Permission | What it does |
| --- | --- | --- | --- |
| **Custom NPCs** | `/npc` | `scriptedessentials.npcs` | Clickable NPCs that run commands, send messages or open menus. |
| **Custom Permission System** *(off by default)* | `/seperm` | `scriptedessentials.permissions` | Groups with inheritance and per-player grants. Leave off if you run LuckPerms. |
| **Custom Team System** | `/team` | `scriptedessentials.teams` | Coloured teams with prefixes, name tag colours and friendly fire control. |
| **Custom Villager Maker** | `/villagermaker` | `scriptedessentials.villagermaker` | Places decorative villagers with a fixed profession and no trading. |
| **Nickname System** | `/nick`, `/nickother` | `scriptedessentials.nicknames` | Display names in chat and the tab list, with length and impersonation checks. |
| **On-Death Actions** | `/deathactions` | `scriptedessentials.deathactions` | Runs commands, effects and messages of your choosing when a player dies. |
| **Orbital Weapons** *(off by default)* | `/orbit` | `scriptedessentials.orbitalweapons` | Calls a telegraphed lightning strike on a target, with a countdown. |
| **Skin Library** | `/skin` | `scriptedessentials.skins` | Stores named skins and puts them on head items. Live player skins need packet access. |
| **Stasis Rod** *(off by default)* | `/stasis`, `/stasis release` | `scriptedessentials.stasis` | Freezes a thrown ender pearl in the air, to be recalled on command. |

---

## Permissions

Each feature gates its commands on `scriptedessentials.<feature-id>`, listed in the tables above.
Beyond those:

| Node | Grants |
| --- | --- |
| `scriptedessentials.admin` | `/se` and the control panel |
| `scriptedessentials.bypass` | Ignore server lock, dimension lock, command blocker, chat filter and the soft border |
| `scriptedessentials.vanish.see` | See vanished players |
| `scriptedessentials.halfheart.exempt` | Take normal damage while half-heart protection is on |
| `scriptedessentials.chatmute.bypass` | Talk while chat is locked |
| `scriptedessentials.clearchat.bypass` | Keep your chat history through `/clearchat` |
| `scriptedessentials.enderchest.others` | Open other players' ender chests |
| `scriptedessentials.nicknames.others` | Use `/nickother` |
| `scriptedessentials.kits.manage` | Create and delete kits |
| `scriptedessentials.kits.nocooldown` | Claim kits with no cooldown |
| `scriptedessentials.setwarp` | Create and delete warps |
| `scriptedessentials.setspawn` | Move the server spawn |
| `scriptedessentials.commandfeedback.see` | Receive the staff command log |
| `scriptedessentials.privatemessages.override` | Message players who have DMs switched off |
| `scriptedessentials.orbitalweapons.nocooldown` | Call strikes with no cooldown |

The **Custom Permission System** feature can manage all of this in-game (`/seperm`) with groups and
inheritance. It is off by default — if you already run LuckPerms, leave it off and let LuckPerms
own permissions.

## Files

| File | Holds |
| --- | --- |
| `config.yml` | Settings for each feature |
| `messages.yml` | Every player-facing string |
| `features.yml` | Which features are on. Managed by `/se`; editable by hand |
| `data/*.yml` | Warps, kits, teams, NPCs, nicknames, mutes, inventory snapshots, permissions |

### Message formatting

`messages.yml` and the message-shaped settings in `config.yml` accept
[MiniMessage](https://docs.advntr.dev/minimessage/format.html):

```yaml
heal-self: '<green>You have been fully healed.'
prefix: '<dark_gray>[<gradient:#4facfe:#00f2fe>SE</gradient><dark_gray>] <gray>'
```

Legacy `&a` colour codes also work, so you do not have to convert anything you already have. A
string is read as legacy only when it contains no `<` tag, so the two styles never fight.

---

## Scope notes

Two things are worth stating plainly rather than discovering later.

**Skins.** The Skin Library stores named skins and stamps them onto head items. It cannot change
the skin worn by a player who is already logged in: the client only learns a player's texture from
the login profile, so swapping it mid-session requires packet-level access (ProtocolLib or an NMS
bridge). That dependency was deliberately not taken on.

**NPCs.** NPCs are real entities with their AI, gravity and damage switched off, tagged in their
persistent data container so they are recognised after a restart. Clicking one runs a configured
action list. For the same reason as above, they cannot wear a player's skin — pick an entity type
that already looks the part.

## Development

The plugin is a plain Maven project; `src/main/java` is all of it.

Adding a feature is one class and one line:

```java
public final class ExampleFeature extends Feature {
    public ExampleFeature(ScriptedEssentials plugin) {
        super(plugin, FeatureDefinition.builder("example")
                .name("Example")
                .description("What it does, shown in the control panel.")
                .icon(Material.PAPER)
                .category(FeatureCategory.PLAYER)
                .controls("/example")
                .build());
    }

    @Override
    protected void onRegister() {
        command(new SECommand(plugin, "example") {
            @Override
            protected void run(CommandSender sender, String[] args) {
                plugin.messages().send(sender, "example-message");
            }
        }.describe("An example.", "/example"));
    }
}
```

Then add `manager.register(new ExampleFeature(plugin));` to `FeatureCatalog`. The command is
registered at runtime, gated on the feature's permission, refused while the feature is off, and
the icon appears in `/se` automatically.

Commands and listeners are registered once at startup and stay registered; toggling a feature
flips `isEnabled()`, which commands check for you and listeners should check on entry. Use
`onEnable()` and `onDisable()` only for state that genuinely has to start and stop, such as
repeating tasks.

### `tools/apicheck`

`tools/apicheck/check.sh` type-checks `src/main/java` with `javac` against hand-written stubs of
the Bukkit/Paper API, for working in an environment that cannot reach `repo.papermc.io`. It is a
development aid, not the build — `mvn package` compiles against the real `paper-api`, and where a
stub and the real API disagree, the real API is right.
