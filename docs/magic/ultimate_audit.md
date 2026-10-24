# Ultimate Magic: audit (new vs replacement) and the proposals for every other attribute

Builder `ultimate`, system 3 of the owner's master spec ("Ultimate Magic"). The framework and the eight flagship Ultimates are built; every other attribute is listed below with a proposed Ultimate, **unbuilt**, for the owner to approve.
How it works: `docs/magic/ultimate_magic.md`. Research sources: see the end of this file.

## 1. Audit: every bullet of the spec against what the mod already had (rule 5)

| Spec bullet | Status before | Result | Where |
|---|---|---|---|
| One Ultimate per attribute / skill tree | missing. Only single character pages named "Ultimate Magic" (Lightning Battle Fiend `thunder_fiend`, Apocalypra Astrauza, Excelitius Leonum): per character, not per attribute | NEW (8 flagship); the old pages are KEPT untouched | `ultimate/UltimateRegistry.java` |
| Long wind-up that locks movement, charging animation | missing (the grimoire's hold is 20 ticks; Leopold's charge and the Mana Zone have their own loops) | NEW: 100-400 tick wind-up, two transient attribute modifiers (movement, jump), the existing looping clip `cast_mana_zone` as the charge pose | `UltimateManager.java` |
| Needs max magicule, consumes 100 percent of the current | missing (pages cost a percent; the Mana Zone costs 5 percent) | NEW: start needs 99 percent of max, reserve of 90 percent held during the wind-up, ALL the current magicule taken at the release, then the mana law's exhaustion (a blackout that never kills by default) | `UltimateRules.java` |
| Cancel-safe (death, logout, dimension, too much damage) | partly: the Mana Zone ends on death / logout / dimension, but it has no wind-up to cancel | NEW, DECISION: nothing is spent before the release, so nothing is ever refunded (see 3); damage over 35 percent of max health, seal, sneak, low reserve, setting off, server stop also cancel | `UltimateManager.cancel` |
| Massive AoE damage, bounded targets, radius, damage clamps | missing (Pages15 kit: at most 10 targets, radius 8) | NEW: 3 pulses (60 / 25 / 15 percent), at most 24 foes each (72 hits per cast), radius 6-24, raw damage clamp 80 per pulse, then the grimoire's balance law and its PvP hit cap | `UltimateRules.pulseRaw`, `GrimoireBook.hurt` |
| Respects ally / protection rules and PvP settings | exists: `heroweapon.weapon.kit.Foes.isFoe` | REUSED: no allies, teams, pets, summons, creative, spectators, NPCs; players only when `canHarmPlayer` allows (server PvP) and the server setting `hitPlayers` is on | `UltimateManager.foes` |
| Heavy debuffs | missing as a rule | NEW: 3-4 potion effects per Ultimate (halved on bosses), fire / push / pull / lift | `UltimateDef` |
| Cinematic but cheap particle logic | exists: `VfxSpawn`, `PerfParticles`, the family shapes | REUSED (no new effect, no new client class, no packet): the circle follows the caster, the ground picture is re-sent every 40 ticks (6 times at most), 2 budgeted particle packets every 10 ticks; at most 16 effect packets per cast | `UltimateManager`, previews in `docs/previews/ultimate/` |
| Global per-server cap and a long per-player cooldown | missing | NEW: `maxSimultaneous` (default 2, 1-8); cooldown default 15 minutes (never under 5) | `UltimateConfig` |
| Data-driven registry (id, name, wind-up, radius, damage, debuffs, shapes, element, canon / original, lang) | missing | NEW | `UltimateDef`, `UltimateRegistry` |
| A grimoire page per Ultimate, appended LAST, late-game gate | exists as a recipe (Pages15, ManaZoneGate) | REUSED: the page is wrapped OUTSIDE the Pages15 hook so no older mode moves; the gate (full mastery bar, Senior 1st Class, 1,000,000 EP) is asked through `GrimoireBook.pageBlock` | `UltimatePages.java` |
| Time Magic: no Ultimate, no touching its VFX | rule | KEPT: no Time Ultimate, no Time shape (a test fails on a `TIME_` shape) | `UltimateRegistryTest` |

## 2. The eight flagship Ultimates (built)

| Book | Page id | Name | Origin | Wind-up | Radius | Raw (pulse 1 at the centre) |
|---|---|---|---|---|---|---|
| `book_fire` | `ultimate_fire` | Ultimate Flame Magic: Crimson Sun Descent | original | 160 ticks (8.0 s) | 16.0 blocks | 40 x 0.60 = 24 |
| `book_water` | `ultimate_water` | Ultimate Water Magic: Sea God's Deluge | original | 180 ticks (9.0 s) | 18.0 blocks | 36 x 0.60 = 22 |
| `book_wind` | `ultimate_wind` | Ultimate Wind Magic: Heavenfall Tempest | original | 140 ticks (7.0 s) | 20.0 blocks | 32 x 0.60 = 19 |
| `book_earth` | `ultimate_earth` | Ultimate Earth Magic: World-Splitting Titan Quake | original | 200 ticks (10.0 s) | 14.0 blocks | 44 x 0.60 = 26 |
| `book_lightning` | `ultimate_lightning` | Ultimate Lightning Magic: Thunder Sovereign's Verdict | original | 120 ticks (6.0 s) | 18.0 blocks | 38 x 0.60 = 23 |
| `book_light` | `ultimate_light` | Ultimate Light Magic: Dawn of Judgment | original | 180 ticks (9.0 s) | 20.0 blocks | 34 x 0.60 = 20 |
| `book_dark` | `ultimate_dark` | Ultimate Dark Magic: Black Moon Eclipse | original | 180 ticks (9.0 s) | 18.0 blocks | 36 x 0.60 = 22 |
| `book_spatial` | `ultimate_spatial` | Ultimate Spatial Magic: World's Edge Collapse | original | 170 ticks (8.5 s) | 18.0 blocks | 34 x 0.60 = 20 |

Canon check (rule 10, English wiki first, Japanese as the fallback): Black Clover names an "Ultimate Magic" for only a few mages (Mereoleona "Ultimate Flame Magic: Excelicitus Leonum", Luck Voltia "Lightning Battle Fiend" and "Black Lightning Battle Fiend", Gadjah "Apocalypra Astrauza", Licht's Demon-Dweller Sword). The mod already has the Fire and Lightning ones as character pages (`excelitius_leonum`, `thunder_fiend`, `apocalypra_astrauza`), so those names are NOT reused (they would be a replacement of working pages; the owner may fold them in later). Every flagship Ultimate is therefore **original**, named in the show's style ("Ultimate <Element> Magic: ..."). The Fandom wiki could not be fetched (the host is not reachable from the build machine); the facts above come from WebSearch summaries only, and no Japanese page listing "Ultimate Magic" by attribute was found.

## 3. Decisions made (the spec said "decide and document")

* **Spend:** nothing at the start. All current magicule is taken at the release. A cancelled cast therefore needs no refund (and survives a crash). While winding up, the caster must keep 90 percent of the max: casting other spells during the wind-up breaks it.
* **Cancel price:** a 10 second lockout (never the long cooldown). The long cooldown starts only at the release.
* **Exhaustion:** after the release the mana law's exhaustion applies (it is the existing "mana is life force" rule; the Wizard King never collapses; by default it never kills). Switch off with `exhaustAfter = false`.
* **Dead caster after the release:** the remaining pulses are dropped (the magicule is already spent).
* **Character grimoires:** a grimoire bound to a character whose set does not list the page hides it (the existing set rule); the plain and basic grimoires show it. Listing it in a character's set is a separate decision.
* **Wind-up animation:** the existing looping clip `cast_mana_zone` and the release clip `cast_signature`; a dedicated Blockbench charge clip is not made (nothing here can open Blockbench).

## 4. Unbuilt proposals: one Ultimate for every other attribute (names are ORIGINAL proposals unless noted; the owner approves, then each is one more row in `UltimateRegistry`)

Time Magic is deliberately absent (owner rule 3). The framework needs no code change for a new row except the book's grimoire path and (for a few entries) a decision noted in the idea column.

| MagicType | Attribute | Proposed Ultimate | Idea |
|---|---|---|---|
| `EXPLOSION` | Explosion Magic | Ultimate Explosion Magic: Dawn Detonation | a chain of ever-larger blasts that ends in one crater-sized detonation; heavy knockback, deafness (nausea) debuff |
| `MAGMA` | Magma Magic | Ultimate Magma Magic: Planet's Core Eruption | the ground melts into a lava field that erupts in three waves; burn + slowness |
| `ICE` | Ice Magic | Ultimate Ice Magic: Absolute Zero Coffin | the whole area flash-freezes; freezing ticks, heavy slowness, shattering pulses |
| `MERCURY` | Mercury Magic | Ultimate Mercury Magic: Silver Tide Requiem | a silver flood of liquid metal that clings and drags; slowness, weakness |
| `MIST` | Mist Magic | Ultimate Mist Magic: Veil of the Thousand Fogs | a blinding fog bank that saps and confuses; blindness, nausea, damage over three pulses |
| `STAR` | Star Magic | Ultimate Star Magic: Stellar Rain of Ruin | a meteor shower of star shards over the radius; pierce damage, glowing marks |
| `STORM` | Storm Magic | Ultimate Storm Magic: Heaven's Wrath Typhoon | a storm front with lightning and gale; shock + levitation (Storm is staff-only by config) |
| `PLANT` | Plant Magic | Ultimate Plant Magic: Forest of the World | a forest bursts out of the ground and binds; poison, heavy slowness |
| `SAND` | Sand Magic | Ultimate Sand Magic: Desert Sovereign's Grave | a sandstorm that buries and blinds; blindness, slowness, fatigue |
| `SWORD` | Sword Magic | Ultimate Sword Magic: Thousand Blades Heaven | a rain of spectral swords; many small pierce hits, bleeding-style wither |
| `SHADOW` | Shadow Magic | Ultimate Shadow Magic: Endless Night Banquet | shadows rise and devour; darkness, slowness, wither |
| `POISON` | Poison Magic | Ultimate Poison Magic: Ten Thousand Venoms | a toxic cloud with every venom; poison, hunger, weakness |
| `MIRROR` | Mirror Magic | Ultimate Mirror Magic: Infinite Reflection Hall | a hall of mirrors that reflects the pulses back several times; blindness, nausea |
| `GRAVITY` | Gravity Magic | Ultimate Gravity Magic: Collapse of the Star | a gravity well pulls and crushes; heavy slowness, pull-in, damage |
| `SEALING` | Sealing Magic | Ultimate Sealing Magic: Grand Seal of Ages | chains and seals everything in range; heavy slowness, weakness, no damage bias (control) |
| `REINFORCEMENT` | Reinforcement Magic | Ultimate Reinforcement Magic: Titan's Final Fist | one colossal punch shockwave; knockback, weakness |
| `BEAST` | Beast Magic | Ultimate Beast Magic: King of the Hunt | a spectral beast pack sweeps the field; bleed-style wither, slowness |
| `BONE` | Bone Magic | Ultimate Bone Magic: Ossuary Cathedral | bone spires erupt in rings; pierce, slowness |
| `BLOOD` | Blood Magic | Ultimate Blood Magic: Crimson Sea Reaping | a sea of blood that drains; wither, hunger, small heal for the caster is NOT included (no lifesteal in v1) |
| `CREATION` | Creation Magic | Ultimate Creation Magic: Genesis Armoury | a rain of created weapons; pierce damage, pretty and cheap |
| `COPY` | Copy Magic | Ultimate Copy Magic: Mirror of Every Art | replays the last three spells the caster used as AoE pulses; needs the copy memory |
| `ILLUSION` | Illusion Magic | Ultimate Illusion Magic: World of False Dreams | a dome of illusion; blindness, nausea, confusion, low damage |
| `DREAM` | Dream Magic | Ultimate Dream Magic: Eternal Nightmare | a nightmare dome; darkness, weakness, slowness |
| `ANTI_MAGIC` | Anti-Magic | Ultimate Anti-Magic: Null Zone | strips buffs and silences magic in the radius (no damage); needs a design decision (Anti-Magic is not rollable) |
| `STEEL` | Steel Magic | Ultimate Steel Magic: Iron Fortress Fall | falling steel blades and a crushing wall; pierce, slowness |
| `THREAD` | Thread Magic | Ultimate Thread Magic: Crimson Loom of Fate | a web of threads over the area; slowness, weakness, damage |
| `TRANSMUTATION` | Transmutation Magic | Ultimate Transmutation Magic: Alchemical Reversal | everything in range is transmuted: debuffs swapped for heavy weakness; no block changes |
| `ASH` | Ash Magic | Ultimate Ash Magic: Cinder Apocalypse | a falling ash storm; blindness, wither, burn |
| `COTTON` | Cotton Magic | Ultimate Cotton Magic: Cloud Sheep Dreamland | a giant soft cloud that smothers; slowness, weakness, low damage |
| `RECOMBINATION` | Recombination Magic | Ultimate Recombination Magic: Molecular Rewrite | reconstructs targets' bodies into a weak state; weakness, fatigue |
| `PAINTING` | Painting Magic | Ultimate Painting Magic: The Masterpiece Canvas | a huge painted scene comes alive and strikes; mixed debuffs |
| `WORLD_TREE` | World Tree Magic | Ultimate World Tree Magic: Yggdrasil Awakening | the world tree grows and lashes; slowness, poison |
| `DICE` | Dice Magic | Ultimate Dice Magic: All-or-Nothing Jackpot | a fate roll chooses one of six effects for the whole area; the only Ultimate with variance |
| `SLASH` | Slash Magic | Ultimate Slash Magic: Dimension-Cleaving Heaven | countless slashes; pierce, slowness |
| `COMPASS` | Compass Magic | Ultimate Compass Magic: Lodestar Rift | all foes are pulled toward the centre by the needle; pull-in, slowness |
| `KOTODAMA` | Kotodama Magic | Ultimate Kotodama: Word That Ends the World | a spoken command that stuns the area; heavy slowness; very late-game only |
| `GAME` | Game Magic | Ultimate Game Magic: Final Boss Raid | an arena rule-set with timed pulses; debuffs |
| `DEMON_BEAST` | Demon Beast Magic | Ultimate Demon Beast Magic: Hellhound Stampede | a stampede of devil beasts; wither, slowness |
| `BODY` | Body Magic | Ultimate Body Magic: Overdrive Apex | a self-centred shockwave; knockback, weakness |
| `EYE` | Eye Magic | Ultimate Eye Magic: Gaze of the Thousand Eyes | an all-seeing gaze; glowing marks, blindness, weakness |
| `EYEBALL` | Eyeball Magic | Ultimate Eyeball Magic: Argus Vigil | eyeballs spawn visuals that mark and pierce; glowing, damage |
| `CURSE` | Curse Magic | Ultimate Curse Magic: Doom of the Seven Generations | a heavy curse on every foe; wither, weakness, unluck |
| `CURSE_WARDING` | Curse-Warding Magic | Ultimate Curse Warding: Great Purification | removes harm from allies in range and wards; defensive variant |
| `DEMON_FIRE` | Demon Fire Magic | Ultimate Demon Fire: Hellfire Throne | devil flame; burn, wither |
| `DEMON_ICE` | Demon Ice Magic | Ultimate Demon Ice: Cocytus Descent | devil ice; freeze, wither |
| `DEMON_LIGHT` | Demon Light Magic | Ultimate Demon Light: Fallen Dawn | corrupted light; blindness, wither |
| `DEMON_WATER` | Demon Water Magic | Ultimate Demon Water: Styx Flood | devil tide; nausea, wither, slowness |
| `BARRIER` | Barrier Magic | Ultimate Barrier Magic: Absolute Cage | a huge cage that pushes in; slowness, weakness |
| `KEY` | Key Magic | Ultimate Key Magic: Gate of the Last Lock | locks every foe (slowness IV) for a short time |
| `CHAIN` | Chain Magic | Ultimate Chain Magic: Prison of a Thousand Links | chains from the ground; slowness, weakness |
| `BUTOH` | Butoh Magic | Ultimate Butoh Magic: Dance of the Final Curtain | a dancing shockwave; knockback, nausea |
| `BRIAR` | Briar Magic | Ultimate Briar Magic: Thorn Empress Garden | a briar field; poison, slowness |
| `CHERRY_BLOSSOM` | Cherry Blossom Magic | Ultimate Cherry Blossom Magic: Ten Thousand Petals | a petal storm; blindness, damage |
| `FUNGUS` | Fungus Magic | Ultimate Fungus Magic: Spore Apocalypse | spores everywhere; poison, nausea, weakness |
| `FOOD` | Food Magic | Ultimate Food Magic: Grand Banquet of Ruin | a giant feast that sates allies and sickens foes; hunger, nausea |
| `CRYSTAL` | Crystal Magic | Ultimate Crystal Magic: Prism Cathedral | a crystal burst; pierce, glowing |
| `CORUNDUM` | Corundum Magic | Ultimate Corundum Magic: Ruby Sun Cutter | a hard-gem shatter; pierce |
| `BRONZE` | Bronze Magic | Ultimate Bronze Magic: Colossus of the Old Age | a bronze giant stamps; knockback, slowness |
| `COPPER` | Copper Magic | Ultimate Copper Magic: Conduit Overload | a conducting field; shock-style slowness and weakness |
| `IRON` | Iron Magic | Ultimate Iron Magic: Meteor Anvil | falling iron; knockback, slowness |
| `BLACK_OIL` | Black Oil Magic | Ultimate Black Oil Magic: Tar Ocean Ignition | a tar flood that ignites; burn, slowness |
| `GEL` | Gel Magic | Ultimate Gel Magic: Great Slime Engulf | a huge gel that traps; slowness IV |
| `GLASS` | Glass Magic | Ultimate Glass Magic: Shattered Sky | glass shards rain; pierce, blindness |
| `BUBBLE` | Bubble Magic | Ultimate Bubble Magic: Million Bubble Prison | bubbles lift and trap; levitation, slowness |
| `ICE_WEDGE` | Ice Wedge Magic | Ultimate Ice Wedge Magic: Glacier Spear Barrage | wedge-shaped ice spears; pierce, freeze |
| `LEGION` | Legion Magic | Ultimate Legion Magic: Army of the Chessboard | the whole chessboard marches; pulses with the board's pieces |
| `AIR` | Air Magic | Ultimate Air Magic: Pressure Collapse | a vacuum and pressure crush; knockback, weakness |
| `HAIR` | Hair Magic | Ultimate Hair Magic: Thousand Strand Binding | hair strands bind and cut; slowness |
| `MEMORY` | Memory Magic | Ultimate Memory Magic: Total Recall Erasure | scrambles targets' memory (mining fatigue, weakness); no damage bias |
| `MINERAL` | Mineral Magic | Ultimate Mineral Magic: Vein Eruption | mineral spikes; pierce |
| `MODIFICATION` | Modification Magic | Ultimate Modification Magic: Absolute Adjustment | adjusts the area: heavy slowness and weakness |
| `MUCUS` | Mucus Magic | Ultimate Mucus Magic: Slime Tide | a sticky tide; slowness IV |
| `MUD` | Mud Magic | Ultimate Mud Magic: Swamp of Despair | a swamp; slowness, fatigue |
| `NAIL` | Nail Magic | Ultimate Nail Magic: Iron Maiden Rain | nails rain; pierce |
| `PERMEATION` | Permeation Magic | Ultimate Permeation Magic: Phase Through All | phase strikes through armour; damage with no knockback |
| `POISON_PLANT` | Poison Plant Magic | Ultimate Poison Plant Magic: Venom Bloom | giant blooms; poison, wither |
| `RED_OCHRE` | Red Ochre Magic | Ultimate Red Ochre Magic: Crimson Earth Rite | red earth erupts; weakness, slowness |
| `ROCK` | Rock Magic | Ultimate Rock Magic: Mountain Fall | a mountain-sized fall; knockback |
| `SANDSTONE` | Sandstone Magic | Ultimate Sandstone Magic: Pharaoh's Tomb Collapse | a collapsing tomb; slowness, blindness |
| `SCALE` | Scale Magic | Ultimate Scale Magic: Judgement Scale | weighs every foe; damage by their weight, weakness |
| `SHAKUDO` | Shakudo Magic | Ultimate Shakudo Magic: Black Gold Flame | black-gold blades; pierce, burn |
| `SKIN` | Skin Magic | Ultimate Skin Magic: Second Skin Rend | a rending pulse; weakness, wither |
| `SMOKE` | Smoke Magic | Ultimate Smoke Magic: Great Burning Veil | a smoke wall; blindness, slowness |
| `SNOW` | Snow Magic | Ultimate Snow Magic: Whiteout Blizzard | a blizzard; freeze, slowness, blindness |
| `SONG` | Song Magic | Ultimate Song Magic: Requiem of the World | a song that wounds foes and stills them; slowness, weakness |
| `SOUL_CORPSE` | Soul Corpse Magic | Ultimate Soul Corpse Magic: Legion of the Dead | undead hands rise; wither, slowness (summons only as pictures) |
| `SOUL` | Soul Magic | Ultimate Soul Magic: Soul Harvest | a soul-drain pulse; wither, weakness |
| `SOUND` | Sound Magic | Ultimate Sound Magic: Resonance Collapse | a sonic boom; knockback, nausea |
| `SPIKE` | Spike Magic | Ultimate Spike Magic: Forest of Spears | spikes everywhere; pierce |
| `SWITCHING` | Switching Magic | Ultimate Switching Magic: Total Swap | scrambles positions of foes inside the area (positions of foes only); nausea |
| `TONGUE` | Tongue Magic | Ultimate Tongue Magic: Great Toad Lash | a gigantic tongue lash; pull-in, damage |
| `TREE` | Tree Magic | Ultimate Tree Magic: Ancient Grove Wrath | ancient trees strike; slowness, damage |
| `STONE` | Stone Magic | Ultimate Stone Magic: Monolith Rain | monoliths fall; knockback, slowness |
| `VINE` | Vine Magic | Ultimate Vine Magic: Jungle Strangle | vines bind; slowness IV, poison |
| `VORTEX` | Vortex Magic | Ultimate Vortex Magic: Maelstrom Abyss | a vortex pulls in; pull-in, nausea |
| `WING` | Wing Magic | Ultimate Wing Magic: Skyfall Feathers | a feather storm; pierce, levitation |

Beyond the Black Clover attributes the spec says "Tensura skill tree": proposals for the Tensura side (unbuilt, original): Predator "Ultimate Predation: Beelzebuth's Maw" (a devouring pull and drain), Great Sage "Ultimate Analysis: Raphael's Verdict" (marks every foe, big single-target-weighted pulses), Sovereign flame "Hell Flare Annihilation", Gluttony "Grand Feast", Thought Acceleration / Time-based skills are excluded like Time Magic. They need the skill-instance path (not a grimoire page) and are a separate decision.

## 5. Sources and what could not be fetched

* English wiki (Black Clover Fandom: "Ultimate Magic", "Mereoleona Vermillion", "Light Magic", "United Forces"): **not fetchable** (host not reachable); only WebSearch summaries were used (Mereoleona: Excelicitus Leonum; Luck: Lightning Battle Fiend; Gadjah: lightning Ultimate; Licht).
* Japanese: WebSearch for 究極魔法 found the game "Quartet Knights" (each character has main / sub / extra / ultimate / link magic) and ja.wikipedia listing 黒雷究極魔法「雷霆刹那黒戦鬼」; no per-attribute list. Not fetchable as pages.
* Repo copies: `docs/magic/character_spells_bulls.md`, `character_spells_rest.md` (the existing Lightning Ultimate pages).
