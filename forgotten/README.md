# The World That Forgot Itself

A Fabric mod for **Minecraft 1.21.11** that combines seven design ideas into one game: a world
that is slowly forgetting what it is supposed to be, and the player's choice about what it
becomes. (The request described ten ideas, but the message was cut off after the seventh. Only
those seven are implemented. See [Missing ideas](#missing-ideas).)

| Idea | What it became here |
|---|---|
| 1. The World That Forgot Itself | Memory Integrity per chunk, its five stages and the endings |
| 2. The Paradox Engine | Contradiction crafting, Causal Debt, Architect ranks, Reality Storms |
| 3. The Memory of the World | The Chronicle, memory bleeding, the Archive and the Curator |
| 4. Silicon & Spores | The Compiler Chest, Bio-Nodes, the Stack, reality debugging |
| 5. The Unraveling | The Mender and the Unmaker, glitched mobs, the Crash and the Compiler |
| 6. The Waking World | World Lucidity, the Observer, the Underdream, Merge |
| 7. The Matryoshka | Frame Anchors, nested depths, time dilation, Mass Debt, the Unwrapper |

## How the ideas connect

Where two ideas described the same thing, they were merged into one system instead of being
built twice:

- **Integrity is Stability.** Idea 1's Memory Integrity and idea 5's chunk Stability (0-100) are
  one number per chunk. Its stages are idea 1's (Stable, Déjà Vu, Contradiction, Fragmentation,
  Amnesia, Unwritten), and a chunk at 0 is idea 5's Unmade zone.
- **Lucidity is Historical Pressure.** Idea 6's world-wide Lucidity rises with idea 3's
  Chronicle, the record of everything that happened. Significant events (deaths, demolitions,
  bosses) push it harder.
- **The Observer is the Witness.** Idea 1's Observer and idea 6's Witness are the same figure.
  Befriending it opens the Underdream.
- **Concepts come from memories.** The Memory Siphon draws Memories out of things, and the
  Paradox Engine distils them into the twelve Concepts (Gravity, Memory, Identity and so on).
  Concepts are the currency of idea 2's contradiction crafting, the answers to the Author's
  trial, and the vocabulary of the laws.
- **One set of endings.** Every idea ended with a choice, and they reduce to five: Preserve
  (also Repair, Reboot and Refuse), Restore the First World (also Fork), Become the Author (also
  Become the Engine), Merge, and Run Again (also Terminate and Delete). Every run is written to
  a palimpsest file outside the save, and the next world on that install grows echoes of it.
- **Debugging tools are Mender and Unmaker tools.** The Breakpoint Wand and Git Reset (idea 4)
  sit beside the Stitcher and Patch Notes (Mender) and the Destabilizer, Glitch Grenade, Editor
  and Crash (Unmaker) from idea 5.
- **Each dimension is a way reality can exist**, reached by doing something the world does not
  expect:

| Dimension | How you get there |
|---|---|
| The Backside (the Overworld from behind) | Fall through ground at Amnesia or worse, or wade through Null there |
| The Unwritten (white nothing) | Fall out of the Backside |
| The Stack | Throw an Infinite Loop cartridge onto obsidian ringed by bookshelves |
| The Underdream | Sleep through a thunderstorm, or at all once the Observer trusts you |
| The Archive | Hold a Lens to a written book on a lectern |
| The First World (the same seed, untouched) | Crouch-use the Memory of the First World |
| Depths 1-5 | Use a Frame Anchor; crouch-use one in a depth to climb back up |

## The arc of a game

1. **Something is wrong.** Far from spawn, integrity drifts down: odd drops, animals that
   stare, furnaces that forget they are furnaces. A cube assembles itself in the sky and the
   first Memory of the First World falls from it.
2. **Learn the rules so you can break them.** The Lens shows integrity. The Memory Siphon
   collects memories. The Paradox Engine contradicts reality and charges Causal Debt; when the
   debt is high, the Second You comes for your things. Insight raises your Architect rank.
3. **Choose a side, or both.** As a Mender, you anchor chunks and stitch glitched creatures back
   together. As an Unmaker, you destabilize chunks, throw Glitch Grenades and finally perform
   the Crash. Programs compiled in the Compiler Chest turn spliced animals into Bio-Nodes.
4. **The world wakes.** Lucidity rises. Forests remember who cut them, and creepers stop to
   think. Reality Storms rewrite the rules for a while. The Observer appears near the places
   you return to. Once the world is Awake, it grows Anachronisms (water that flows upward, fish
   made of light), and the Inheritors return. They are an older people who trade, accept gifts,
   remember harm, and go to war if harmed enough.
5. **Build a universe of your own.** Reality Architecture: a Core of Identity powers nearby
   chambers. Gravity points the way you choose, time runs faster, two doors join across any
   distance, a Memory Vault rebuilds what was lost, and Possibility Seeds grow into whatever
   they might have been.
6. **Politics** (multiplayer). Architect Guilds claim chunks and set rules there (floating base,
   permanent night, sealed ground). Rule Anchors enforce local laws until a Reality Storm
   suspends them. Elections choose a World Admin for a day. Killing an Architect leaves a
   Codex Fragment that lends their rank.
7. **Go where it keeps itself.** The Backside, the Stack, the Underdream (Marrowglass, Quiet,
   Tomorrow), the Archive, the First World, and nested copies of the world where time runs
   faster. Below the deepest copy is the Workshop. On its one workbench sits the real grass
   block, and anyone who reaches it can rewrite the world's laws, at a cost to everyone.
8. **Bosses:** the Recursion Queen, the King of Yesterday, the Gardener, the Cartographer, the
   Compiler (in the Null Layer left by the Crash), the Curator (in the Archive) and the
   Unwrapper (when the nesting grows unstable). Each one can only be hurt in its own way.
9. **The Author.** The Compiler drops Source Code. Feed it to the Genesis Interface and the
   Author comes to argue that every change you made was a deviation. Answer each of its steps
   with the right Concept, and when its conviction runs out it asks its question. The Genesis
   Interface then writes seven world rules, the ending, and (if you become the Author) 36 laws.

## Layout

```
forgotten/
  logic/   pure Java, no Minecraft: integrity, lucidity, chronicle, paradox recipes, the
           Bio-Node compiler and VM, concepts, the world's language, nesting, boss rules,
           laws, endings, palimpsest. 52 JUnit tests.
  mod/     the Fabric mod. Built against Yarn 1.21.11+build.6, Fabric Loader 0.20.1,
           Fabric API 0.141.6+1.21.11 and Loom 1.19.
           src/main/java/.../gametest: 48 gametests run on a real dedicated server.
  tools/   generate_resources.py (every texture, model, recipe, loot table, advancement,
           language entry and dimension file is generated from tables in this script),
           validate.py, syntax_check.sh, and templates/ (vanilla 1.21.11 JSON the
           dimensions derive from).
```

Most creatures and bosses are vanilla mobs driven by the mod: a scaled spider, a wither skeleton
wearing a crown, a giant creaking. The menus are server-side chest screens. So a vanilla
client draws almost everything correctly, and the client entrypoint only registers one
translucent block and one thrown-item renderer.

## Building

```
cd forgotten
./gradlew -PskipMinecraft=true :logic:test   # logic only; needs Maven Central
./gradlew :mod:build :mod:runGametest        # needs maven.fabricmc.net and Mojang's servers
python3 tools/generate_resources.py          # after changing any item/block/recipe table
```

Loom needs JDK 25 to run, and the mod is compiled for Java 21. The `.github/workflows/forgotten.yml`
workflow runs all of the above on every push.

## Verification status

This section says what has actually been checked and what has not.

**Where the build ran.** The development container could not reach `maven.fabricmc.net`,
`meta.fabricmc.net`, `libraries.minecraft.net` or Mojang's `piston-*` hosts (the proxy answers
403). That rules out Fabric Loom, Fabric Loader, Fabric API, Yarn and the Minecraft 1.21.11
jars locally, so the mod could not be compiled in the container. Every Minecraft-side check
below ran in GitHub Actions, on this branch, for the commit named. Older Actions runs in this
repository belong to the Paper plugin, a different project, and are not evidence for this mod.

**Checked in CI** (workflow `forgotten.yml`):
- Logic unit tests: 52 tests pass.
- Full mod compile against Minecraft 1.21.11 + Fabric API 0.141.6: passes.
- Gametests on a dedicated 1.21.11 server, run through the game's own code paths. The commits
  that went green, and the count of required gametests passing in each:
  - c6545d4 and 95f602e: 2, then 14
  - ad35a0c (Silicon & Spores and the Unmaker's tools): 25
  - f74fbcd (dimensions): 29
  - 691f4b1 (creatures and bosses): 36
  - ef36384 (the Author, Genesis Interface, rules, laws, endings): 41
  - e724da9 (Reality Architecture, politics, Inheritors, Workshop): **all 48 pass**
  - Each run also uploads the built mod jar as the `forgotten-mod` artifact.
- `tools/validate.py`: every generated resource refers to registered items, blocks and vanilla
  ids; every Minecraft import exists in Yarn 1.21.11; every Fabric import exists in Fabric
  API; every `Class.member` reference to the mod's own classes exists. Mutation-tested.

**Not verified:**
- **Nobody has played it.** No human has run the client. Gametests exercise mechanics, not
  feel, balance or how things look. The textures are procedurally generated 16×16 placeholders.
- **The custom dimensions have never been entered.** The gametest server only creates the
  vanilla worlds. The dimension files are checked by parsing them with the game's own
  `DimensionOptions` codec against the live registries, as a real server does on load. But no
  test has generated terrain in them or teleported a player there. The travel code that sends
  you to each dimension is compiled and partly unit-tested, but not run end to end.
- **Multiplayer**, performance on a large world, and compatibility with other mods.
- **Client-side rendering** of the thrown Glitch Grenade, the translucent Marrowglass and the
  Possibility sprout.
- **The guild, election and World Admin commands** (`/forgotten guild …`, `election`, `vote`,
  `worldadmin`). They compile, and the territory rules they set are covered by gametests, but
  the commands themselves have not been run.

**Approximations.** In these places the design asked for something a server cannot do, and the
mod does something close:
- **Inverted controls.** The server cannot read keys. It pushes the player against their own
  movement instead.
- **Looking at a Null Crawler shows wireframe geometry.** That needs a shader. The mod uses
  Darkness and Nausea instead.
- **The Author unmakes your enchantments and inventory.** Enchantments are not stripped
  (that would risk deleting real items): the player gets Weakness instead. The inventory step
  swaps the held slot.
- **The Crash's Null Layer.** It is the crashed region itself, not a separate dimension.
- **The Editor.** Its edits are held in memory for one minute. If the server stops during that
  minute, the edit becomes permanent.
- **Guild "permanent night".** Time of day is shared by the whole world. In a guild's
  territory, the sun stops burning the undead and outsiders get Darkness, but the sky does not
  change.
- **Anachronisms and Inheritor cities** are small, generated near players at runtime, not
  worldgen structures.
- **The World/Causality law** (undoing explosions) and **World/Finality** (nothing breaks)
  snapshot the blocks around a creeper or TNT one tick before it explodes. Other explosions
  (beds, crystals, withers) are not caught.

## Missing ideas

The request promised ten ideas, but the message ended mid-sentence after the seventh ("Be
honest 1.21.11 MINECRAFT: …"). Ideas 8 to 10 were never received, so they are not in the mod.
