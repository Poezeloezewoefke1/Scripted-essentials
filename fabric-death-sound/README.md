# Scripted Death Sound

A Fabric **client** mod for Minecraft **1.21.11** that replaces the player death
sound with a custom one — and touches nothing else.

Built jar: [`dist/ScriptedDeathSound-1.0.0.jar`](dist/ScriptedDeathSound-1.0.0.jar)

## What it changes

Exactly one sound event: `minecraft:entity.player.death`. It plays whenever any
player dies — you or anyone else you can hear — in singleplayer and on any
server, including vanilla ones.

## Why it is done this way

Vanilla ships **no dedicated death sound**. `entity.player.death` reuses the same
three samples as `entity.player.hurt`:

```jsonc
// vanilla assets/minecraft/sounds.json
"entity.player.death": { "sounds": ["damage/hit1", "damage/hit2", "damage/hit3"], ... },
"entity.player.hurt":  { "sounds": ["damage/hit1", "damage/hit2", "damage/hit3"], ... }
```

So the obvious approach — dropping a replacement `assets/minecraft/sounds/damage/hit1.ogg`
into the pack — would also change the sound you hear every time you take damage,
and it would be inherited by anything else that references those files. That is
the "overwriting other sounds" trap.

This mod instead re-points the **event** at a file in its own namespace:

```jsonc
// assets/minecraft/sounds.json  (this mod)
{
  "entity.player.death": {
    "replace": true,
    "subtitle": "subtitles.entity.player.death",
    "sounds": [{ "name": "scripteddeathsound:player_death", "stream": false }]
  }
}
```

Two details make this safe:

- Minecraft **merges** `sounds.json` across every loaded pack, entry by entry.
  Only the `entity.player.death` key is overridden; the other 1,990 vanilla sound
  events keep coming from the vanilla pack untouched.
- `"replace": true` swaps the vanilla sample list out instead of appending to it,
  so the death sound is always the custom one rather than a 1-in-4 chance.
- `damage/hit1..3` are left in place, so `entity.player.hurt` is unaffected.
- The vanilla subtitle key is reused, so subtitles keep working and no language
  file is overridden either.

The mod contains **no code and no mixins** — nothing to crash, nothing tied to a
specific Minecraft version's internals.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/installer) 0.16.0+ for Minecraft 1.21.11.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.11 in your `mods` folder
   (its `fabric-resource-loader-v0` module is what exposes a mod's assets to the game).
3. Drop `ScriptedDeathSound-1.0.0.jar` in your `mods` folder.
4. Launch. No resource pack needs to be enabled — mod resources load automatically.

## Using your own sound

Replace `src/assets/scripteddeathsound/sounds/player_death.ogg` and rebuild:

```bash
./build.sh
```

The file must be **Ogg Vorbis** (not Opus, not MP3) and should be **mono** —
Minecraft only applies 3D positional audio and distance attenuation to mono
sounds; a stereo file plays flat at full volume regardless of where the player
died. To convert:

```bash
ffmpeg -i your-sound.wav -c:a libvorbis -ac 1 -ar 44100 player_death.ogg
```

Keep it a few seconds at most. `"stream": false` loads it fully into memory,
which is what you want for a short effect; set it to `true` only for anything
music-length.

## Building

```bash
./build.sh
```

The mod is resources only, so the build is a deterministic `zip` — no Gradle, no
Fabric Loom, no network access, and byte-for-byte reproducible. The script also
checks that every sound `sounds.json` declares actually exists in the jar and is
a real Ogg file, which is the one mistake that silently produces no sound at all.

## Scope and limits

- **Client-side.** Each player who should hear the new sound needs the mod. A
  server cannot push it to vanilla clients — it would have to send a server
  resource pack containing the same two files.
- A resource pack the player has enabled sits **above** mod resources, so a pack
  that also defines `entity.player.death` wins. That is normal pack precedence.
- Mob death sounds (`entity.zombie.death`, etc.) are separate events and are
  deliberately left alone.
- `>=1.21` is declared as the supported range and `pack_format` 75 (1.21.11) is
  set with a wide `supported_formats` window, so the jar keeps working across
  neighbouring 1.21.x releases instead of hard-failing on the next patch.
