# Meditation attunement: Beast Wills, Djinns, Devils and Spirits together

**Owner's ask (voice-typed, "James" = Djinns).** A meditation GUI to use several powers on top of each other (Djinns, Beast Wills, Devils, Spirits); these powers on par with King-class-or-above
Ultimate skills in damage; powers whose elements clash (a fire Beast Will and an ice Djinn) reject each other.

Code: `src/main/java/com/newuniverse/nusmp/attune/**` (common), `client/attune/**` (the screen). Config: `nusmp-attune-server.toml` (SERVER). Self-tests: `attune.rules`, `attune.player`
(prefix `attune` on the smoke list). Tests: `AttuneRulesTest`, `AttuneSourceScanTest`. Lang: `tools/lang/attune.json` (merged into `en_us.json`).

## How a player uses it

1. Type **`/meditate`** (no key: the owner has too many already, the spirits already went the command way). The server records where you sat and sends what you hold:
   every bound Beast Will, your Djinn (own or lent), your Devil contract, every Spirit that answers you.
2. The **Meditation** screen: the slots on top, your powers below. Click a power to put it in a slot (one power per system: a second Djinn/Beast Will/... replaces the first); click a slot to
   empty it. Colours: **red** = rejects a power in the slots, **gold** = resonates with one, white/grey = neutral. The footer shows the resonance bonus or the clash.
   While the screen is open you get a calm visual only (a slow breathing tint and two enchant motes every 8 ticks, client side).
3. **Attune** sends one packet. The server checks again from nothing: the setting is on, you opened a session (`/meditate`) at most 2 minutes ago and stood within 1.5 blocks of that spot,
   one ask per 10 ticks, you own each id, no more than the slots, one per system, the cooldown (30 s by default; resending the same set costs nothing), and the clashes.
   **A clash is refused**: a message naming the two powers, a backlash of at most 4 health that never takes you under 1 (never lethal) and 3 s of Slowness II. Nothing changes.
4. `/meditate status` lists your powers (system, elements, attuned, out, SUPPRESSED); `/meditate clear` forgets your attunement. Operators: `/meditate status <player>`, `/meditate clear <player>`.

**Suppression.** If two clashing powers are out at the same time anyway (an old save, a command, never meditated), the weaker one is suppressed: it loses its damage raise (and the 3.74.0 raise it had)
until the clash is gone. Strength: Djinn 3, legendary Beast Will 3, other Beast Will 2, Devil = its tier (1 low .. 4 supreme), Spirit Lord 3, other Spirits 2; on a tie the attuned one (earlier slot) wins.
A player with a clashing saved attunement is told at login. Suppression does not switch the power's own abilities off.

## Element table (`AttuneRules`)

Each power's elements are read from the words of its existing label: a Beast Will's `affinity`, a Djinn's `Affinity` (+ Dark for a dark Djinn), a Devil's `DevilAttribute`, a Spirit's grimoire magic.

| Label words | Element |
|---|---|
| fire, flame, heat, magma, explosion, scorch | FIRE |
| water, ocean, sea, flood, strait, tide, abyss | WATER |
| ice, frost, snow, cold, freez | ICE |
| wind, storm, gale, sky | WIND |
| earth, stone, rock, sand, labyrinth | EARTH |
| lightning, thunder (read before "light") | LIGHTNING |
| light, holy, judgement, radian | LIGHT |
| dark, shadow, chaos, dread, curse, anti_magic, abyss | DARK |
| life, plant, wood, heal | LIFE |
| death, decay, rot, venom, poison, soul, ghost, spectre, underworld | DEATH |
| space, spatial, gravity, time, void | SPACE |
| anything else (Aether, Illusion, Might, Fang, Strength, Sound, Word, Beast, Foresight, Ether, Causality) | neutral: fits all, resonates with none |

What the current content reads as: Beast Wills: Flame Serpent FIRE; Typhon FIRE+WIND; Thunder Fang Chimera LIGHTNING; Wind Skimmer WIND; Pegasus, Ammit, Sphinx LIGHT; Shadow Panther, Apophis, Nue DARK;
Kraken WATER+DARK; Hafgufa, Jormungandr, Scylla and Charybdis, Yamata no Orochi WATER; Lion, Crested Boar, Minotaur EARTH; Elderwood Guardian LIFE; Cerberus, Cu Sith, Fenrir, Hydra, Nidhoggr DEATH;
Dragon, Kitsune, Ridge Wolf neutral. Djinns: Heat FIRE, Lightning, Water, Wind, Life, Light as named, Strength and Sound neutral. Devils: Flame FIRE, Water, Earth, Light as named, Gravity/Spatial/Time
SPACE, Curse and Anti-Magic DARK, Ghost/Soul/Rot DEATH, the rest neutral. Spirits: Salamander FIRE, Sylph WIND, Undine WATER, Gnome EARTH, Light LIGHT, Darkness DARK, Space SPACE,
the Spirit Lord neutral (it holds five magics).

**Reject (opposites):** FIRE-WATER, FIRE-ICE, LIGHT-DARK, LIFE-DEATH, WIND-EARTH.
**Resonate (allies, and any element with itself):** FIRE-WIND, FIRE-LIGHTNING, FIRE-EARTH, WATER-ICE, ICE-WIND, WIND-LIGHTNING, EARTH-LIFE, LIGHT-LIFE, DARK-DEATH, DARK-SPACE.
Everything else is neutral. Two powers: any opposed pair of their elements rejects; else any allied pair resonates. The table is symmetric (tested). Resonance: +10 % per resonating pair of attuned
powers that are out (`resonancePerPair`), at most +30 %.

## Power level vs "King class"

**What "King class" is here.** The repo has no tier literally named "King class" (searched docs and code: `king class`, `king-class`, `KING`). The highest skill tier is the Tensura **Ultimate**
(`skill/rules/Tier.ULTIMATE`), and its King-titled Ultimates are the benchmark: the Dragon Sovereign Ultimate (`bw_dragon`, docs/beast_skills.md) hits **40 to 85 raw** with its actives and **150 raw**
with its finisher **Dragon King's Annihilation**; the Ultimate Magic pages hit **10 to 80 raw** (`UltimateRules`). Target: **60 to 150 raw** per hit before the Balance Law, i.e. an Ultimate active up to
the King finisher.

| System | Raw now (source) | Factor (setting, default) | Raw-equivalent with the factor |
|---|---|---|---|
| Beast Will | abilities 3 to 6 (`beast_will/*.json`, cap 8) | `beastWillFactor` 25 (same as the 3.74.0 raise, which already applies; not stacked) | 75 to 150 |
| Djinn | Extreme Magic about 30 (docs/djinn_roster_36.md) | `djinnFactor` 25 (same as the 3.74.0 raise; not stacked) | about 750 (already above) |
| Devil | no spell of its own: the attribute adds a bonus to the host's blows (`DevilCombat`) | `devilFactor` 12 on the host's blows while let in | 60 to 144 for a 5 to 12 blow |
| Spirit | combat spells 5 to 12 (`SpiritCatalog`) | `spiritFactor` 12 | 60 to 144 |

The factor applies while an **attuned** power is out (Beast Will form held, Djinn Lesser Summon and up, Devil at 20 % assimilation or more, Spirit beside you). Where it sits: the damage event at LOW
priority, the same place as the 3.74.0 power raise (`power/PowerEvents`), which is the one point every kind of damage of the four systems passes through; `balance/BalanceLaw` is NOT touched.
The two hooks never multiply: `AttuneService.factor` aims at a total (`max` of the factors of the powers out, the hero weapon factor, times resonance) and divides by what `PowerEvents` already put on
the blow (`PowerEvents.appliedFactor`, appended). Several powers out: the largest factor, never the product. Against a player the raised blow is held at the PvP hit cap (40 % of maximum health
by default), never lowered below the blow itself.

## Settings (`serverconfig/nusmp-attune-server.toml`, `[attune]`)

`enabled` (true; **false = the old behaviour exactly**: `/meditate` only says it is off, no rejection, no suppression, no factor), `suppressClashes` (true), `slots` (3, 1 to 4), `cooldownTicks` (600),
`beastWillFactor` (25), `djinnFactor` (25), `devilFactor` (12), `spiritFactor` (12), `resonancePerPair` (0.10, at most 0.30).

## Multiplayer and cost (rules 9, 11), worst case

* **State:** one attachment per player (`anime_multiverse:attunement`: at most 4 ids and a time, clamped on load, copied on death, not written for someone who never meditated). Sessions:
  a map keyed by player UUID, one entry while a screen is open, dropped on confirm, logout, death, dimension change and server stop. No static player.
* **Per tick:** nothing.
* **Per blow a player deals (setting on):** 3 attachment reads that never create one, one Djinn attachment check, one map lookup per registered spirit (9 today; the Spirit Lord at most one UUID
  lookup per level), lists of at most 4 entries. Returns at once for a non-player dealer.
* **Network:** `/meditate` sends one packet to that player only, at most 24 powers (about 3 KB worst case); a confirm is one packet of at most 4 ids (under 300 bytes). Both rate limited to one
  per player per 10 ticks (2 a second). Nothing is broadcast.
* **Client:** the screen draws one panel, at most 4 slots and 24 text rows per frame (texts and colours worked out on a click, not per frame); 2 particles every 8 ticks while it is open. No entities.

## NEW / REPLACED / KEPT

* **NEW:** package `attune` (`AttuneRules`, `AttuneConfig`, `AttuneStore`, `AttunePowers`, `AttuneService`, `AttuneNet`, `AttuneEvents`, `AttuneSelfTests`, `AttuneBoot`), `client/attune`
  (`AttuneClient`, `MeditationScreen`), the command `/meditate`, the config file, two payloads (`attune_open`, `attune_confirm`), self-tests `attune.rules` and `attune.player`, the two tests,
  18 lang keys, this doc.
* **REPLACED:** nothing (the request is new: no stacking or element-clash system existed; Beast Will's one-active-will rule, the Djinn bond and the Devil contract are untouched).
* **KEPT:** the four systems as they were (their own keys, pages, commands, numbers); the 3.74.0 power raise (`PowerEvents` only got three read-only methods appended); the Balance Law.
  Shared files only appended to: `NUSMP` (two lines), `GuiMoveRegistry` (one fixed entry), `smoke.yml` (`attune`), `en_us.json`, `PowerEvents`.

## Caveats (not seen in a game)

* Nothing was compiled by Gradle or run in Minecraft in this session; `AttuneRules` was compiled and checked with plain `javac`.
* Meditation slots do not grow with progression (no clean hook was picked); the setting decides.
* The Spirit Lord is neutral; reading its bond's element would make it clash too.
* Like the 3.74.0 raise, the factor raises every blow of the player while the power is out (a sword swing too), not only the power's own abilities.
* Suppression removes the damage raise only; the suppressed power's own abilities still work.
* A lent Djinn gets no 3.74.0 raise (as before) but does get `djinnFactor` when attuned.
* Element words come from labels, so a new will, djinn or devil gets its element with no new rule; a label with none of the words is neutral.
