# Changelog

## v1.0.0

First release. An all-in-one essentials suite for Paper 1.21.x with **53 features**, each of
which can be switched on or off at runtime from `/se` without a restart.

### Features

**Player (15)** — Heal, Feed, Hunger, Set Health, Fly (with fall-damage grace on landing and a
speed control), Gamemodes (`/gm` plus four shorthands), Repair, Durability, Full Tools,
Invisibility, No Effects, Pause Effects, Potion Presets, Totem (with auto-swap), Random Head

**Teleportation (4)** — Warp System (per-warp permissions, browsable menu), Custom Spawn,
Offline Teleport, Top Teleport

**Inventory (6)** — Kit System (cooldowns, permissions, menu), Custom Item Maker, Inventory
Rollback, Inventory See, Ender Chest View, Keep Inventory

**Moderation (6)** — Vanish, Freeze, Server Lock, Command Blocker, Half Heart, Recording Mode

**World (4)** — Dimension Lock, Fake World Border, Chunk Tools, Auto Clear

**Chat (9)** — Chat & Sign Filters, Chat Mute, Clear Chat, Private Messages, Team Chat,
Join & Leave Messages, Voice Chat Mute, Command Feedback, Feedback Sounds

**Systems (9)** — Custom NPCs, Custom Villager Maker, Custom Team System, Nickname System,
Skin Library, On-Death Actions, Orbital Weapons, Stasis Rod, Custom Permission System

Nine features ship switched **off** because they change how the server plays: Keep Inventory,
Half Heart, Server Lock, Auto Clear, Fake World Border, Command Feedback, Orbital Weapons,
Stasis Rod and the Custom Permission System.

### Requirements

- Paper 1.21.x, or a Paper fork such as Purpur or Pufferfish
- Java 21
- No dependencies. PlaceholderAPI and voice chat plugins are used if present and ignored if not.

### Installing

Drop the jar in `plugins/`, start the server, then run `/se`. Config files are created on
first run.

### Notable behaviour

- **Vanish persists.** Log out vanished and you return vanished.
- **Half heart protection does not fight the void, `/kill`, or a plugin-forced kill.**
- **The chat filter blocks rather than censors when it cannot censor.** Detection folds
  leetspeak; censoring can only strike out literal spellings, so when the two disagree
  (`m0ney` is caught, but there is no `money` to star out) the message is blocked.
- **`scriptedessentials.*` works.** Every feature node is registered as a child of it at
  startup, which is also how LuckPerms discovers the node list.

### Known limits

- **Skins.** The Skin Library stores named skins and applies them to head items. It cannot
  change the skin of a player who is already logged in: the client only learns a texture from
  the login profile, so that needs packet-level access (ProtocolLib or an NMS bridge), which
  this plugin deliberately does not take on.
- **NPCs.** For the same reason, NPCs cannot wear a player's skin. Pick an entity type that
  already looks the part.
- **Custom Permission System.** Off by default and intended for servers with no permissions
  plugin. If you run LuckPerms, leave it off.

### Verification

Compiled against the real Paper 1.21.4 API; 36 unit tests pass; no deprecation or removal
warnings originate in this project's sources. The plugin has not yet been run on a live
server — please report anything that misbehaves on boot.
