# Infinity Stones, Phase A: the Infinity Stone slot (rework of the Wave 30 stones)

Version: not bumped (the lead releases). Status: written without a Java compiler or a game; the pure rules were compiled and run with plain `javac` (`StoneRules`, `InfinityStone`), everything that touches Minecraft is unbuilt and unseen.

Owner request: "After 1 billion EP you can equip 1 infinity stone in an equipment slot like a relic slot and get the ability of said stone, 2 abilities plus passive bonuses, based on the actual Infinity Stones." Design: `docs/infinity_stones_design_owner.md`. Audit: `docs/infinity_stones_proposal.md` (not edited; read at the end of the work and applied where cheap and canon-correct, see section 7).

## 1. What was built (plain words)

A new Curios slot `infinity_stone` (1 place) on every player. Any of the six existing Wave 30 stones can be worn in it (the Space Stone or Tesseract, the Mind Stone, the Power Stone, the Time Stone or Eye of Agamotto, the Reality Stone, the Soul Stone). A stone worn there works only while the wearer's BASE maximum EP is at the gate (1,000,000,000 by default). While it works it gives:

- two new abilities (the relic key ring shows them after the stone's old abilities; SHIFT+key cycles as for every relic),
- a few passives (attribute passives once a second, plus event rules),
- every relic act of the wearer gets the Power Stone's 15% lower cost and 20% shorter cooldown, or the Time Stone's 25% shorter cooldown,
- on equip: a short surge (ring, burst, 3% health that can never kill), and now and then a whisper in the action bar.

Under the gate the stone sleeps: no slot abilities, no slot passives, nothing is destroyed. A stone put into the slot by someone under the gate is moved back into the pack at once (once the player has been in the world for five seconds, so a login is never an ejection). The old Wave 30 behaviour (carried, no gate, the old acts, the Gauntlet, the Snap, the Greater Wish, the Soul stone's once-a-day resurrection) is untouched and still works for a stone that is only carried or worn in the charm slot.

## 2. The abilities as built (numbers are DESIGN values, unseen in play)

`stoneScale = 1 + log10(baseEP / gate)`, at least 1, at most 2 (so 10x the gate is the cap). Damage is through `BalanceLaw.damage(..., UltimateClass.GOD)` (total x36), players never lose more than 30% of their health from one hit, never a creative or spectator player.

| Stone | Ability 1 | Ability 2 | Passives |
|---|---|---|---|
| Space (blue) | **Portal Step**: blink along the look, at most 32 blocks (the `Fx` blink: loaded chunks, safe landing). 3% mana, 8 s. | **Spatial Fold**: foes within 16 blocks pulled to a point 3 blocks ahead, or pushed away when sneaking. At most 12 moved, never players, never bosses. 6% mana, 25 s. | +30% movement speed, +50% knockback resistance (both x scale) |
| Mind (yellow) | **Psionic Lance** (replaces the removed Dominion): a bolt of thought at the foe in sight, GOD-class damage 14 x scale, a short nausea (through `BalanceLaw.controlTicks`/`beginControl`) and Glowing. No control. 4% mana, 15 s. | **Psionic Reach**: telepathy. The ten nearest minds within 48 blocks glow for 10 s, and the six nearest are listed with health, EP and distance. 2% mana, 30 s. | Mental shield: confusion, fear, nausea (and the Tensura insanity/mind-control effect ids) do not apply; +25% magicule regeneration (x scale) |
| Reality (red) | **Rewrite**: the attacks of a mob in sight are rewritten to nothing for 8 s (Weakness 10; a player 3 s via the balance law). Never a boss. 7% mana, 45 s. | **False World**: invisible for 20 s, and mobs within 24 blocks that hunted you lose you. 6% mana, 60 s. | One in five harmful effects does not happen; +2 luck (x scale) |
| Power (purple) | **Power Surge**: foes within 8 blocks take 18 x scale GOD-class damage and are thrown back (never a player or boss thrown). Recoil 5% of max health, never lethal. 4% mana, 20 s. | **Infinite Might**: Strength III and Resistance II for 30 s. 10% mana, 120 s. | +40% max health, +6 armour (x scale); relic acts cost 15% less and recover 20% sooner |
| Time (green) | **Rewind**: back to where you stood about 5 s ago, with that health (healing through the balance law). Same dimension, loaded chunk only. 8% mana, 60 s. | **Time Lock**: foes within 12 blocks held almost still (Slowness 7, Weakness 3): boss 1 s, player 3 s, mob 5 s, with the shared 12 s control cooldown and the repeat decay. 12% mana, 120 s. | Immune to slowing; Foresight: a single hit of 20% of max health or more is turned aside, once per 60 s; +15% attack speed (x scale); relic acts recover 25% sooner |
| Soul (orange) | **Soul Grasp**: the soul of the foe in sight is held for 3 s (balance-law cap): slowed to a stop and weakened; you read its health and EP. No instant kill, on anyone. 9% mana, 90 s. | **Soulworld**: for 15 s you and allied players within 8 blocks of this place heal 2% to 3% of max health a second (45% at most in all, under the 50% law). A zone effect, not a dimension. 6% mana, 120 s. | Magic damage x0.6; Soul anchor: a blow that would kill leaves 20% health, once per game day (the old once-a-day resurrection of the stone stays as well); +25% max aura (x scale) |

Mind control: the owner decided there is NO mind control anywhere in the Infinity Stones feature. Dominion ("a target becomes a temporary ally") and any touch or heart control are removed from the design and from the code, and there is no setting or lang line for them. It was replaced by the **Psionic Lance** (a bounded damage and disorient burst). Telepathy and sense (Psionic Reach), the mental shield (passive), and the stone's sentience (whispers) keep the Mind Stone canon-flavoured. Left for Phase B on the Mind Stone: telekinesis (lift and throw, respecting forced-movement rules), astral scouting, Mind Link (share vision and buffs with allies), the mastery bonus.

## 3. Settings (SERVER, `nusmp-stones-server.toml`, section `infinityStones`)

| Setting | Default | Meaning |
|---|---|---|
| `enabled` | true | false = the Wave 30 stones exactly as before (no slot acts, passives, gate, container rule, away rule) |
| `epGate` | 1.0E9 | base maximum EP needed to wake a stone in the slot |
| `passives` | true | the slot passives |
| `scaleWithEp` | true | the log10 scale; false = always x1 |
| `awayDays` | 7 | real days a holder may be offline before the stone is declared lost to the Order's ledger; 0 = never |
| `onDeath` | keep | "keep" returns the worn stone at the respawn; "drop" lets it fall with the loot |
| `noContainers` | true | a stone left in a chest, barrel, ender chest or shulker box returns to the player's pack when the container closes |
| `whispers` | true | rare whispers |

## 4. How it is wired (shared files only gained hooks)

- `RelicCatalog.init`: `raw.replaceAll(StoneKit::extend)` adds the two slot acts to the six stone defs; ids, old acts and passives untouched.
- `RelicEngine.activate`: a slot act is refused before anything is paid when the stone is not in the slot, the gate is not met or the setting is off; the Power/Time stones' cost and cooldown factors multiply the existing tier factors.
- `RelicCurioEquip.tryEquip`: a right-clicked stone goes to `infinity_stone` when the gate is met and the slot is free, otherwise to the charm slot as before.
- `UniqueLedger.loseAway`: one new public method (the existing 14-day rule only looks at online holders). One copy per server was already enforced by the Order's ledger; no second registry.
- `NUSMP`: one line (`StoneBoot.init`). `MultiverseCommands`: one line. `smoke.yml`: the prefix `stones` directly before `,scaling,boss.rework1`.
- Data: `curios/slots/infinity_stone.json` (size 1, order 57), `curios/entities/infinity_stone.json`, `data/curios/tags/item/infinity_stone.json` (the six stones; they stay in the charm tag too). The slot is not added to `RelicWear.OTHER_SLOTS`/`ENTITY_FILES` (existing tests pin those).
- The slot always exists (Curios slots are fixed data and the mod does not compile against Curios), so the gate is checked on the server at equip and once a second.
- Worn look: the stones' existing worn meshes and glow (palette colour) draw from any Curios slot; nothing client-side changed.

## 5. Commands

- `/multiverse stones give <player> <stone>` (operators): through the Order's ledger; a stone that already lives cannot be given twice.
- `/multiverse stones status` (anyone, for themselves): gate, base EP, scale, worn stone.
- The Soul Stone's Vormir trial (server-validated): `/multiverse stones offer skill` previews the highest-mastery Tensura skill, `offer skill confirm` gives it up; `offer pet <name>` / `offer confirm_pet <name>` gives up a named pet of yours within 16 blocks. Needs the EP gate and a Soul Stone with no keeper. The skill or pet is gone for good; nothing is consumed before the stone is certain to be issued. A player is never an offering; the owner's "50% of max EP" offering is not offered because it would drop the player under the gate (audit correction). A physical altar is Phase B.
- The owner's `/anime_multiverse stones give` is `/multiverse stones give` (the real command root is `multiverse`).

## 6. Worst-case cost per ability (rule 11)

| Part | Server work | Packets | Client |
|---|---|---|---|
| Slot pass | once a second per player (staggered by id): one cached Curios read shared with the relic scan; the slot map is built only for a wearer; attribute modifiers set only when the amount changes | none | none |
| Ambient burst | one `Fx.burst` per wearer per 3 s, through `PerfBudgets.effectPacket` | one particle group per 3 s per wearer | the existing particle budget |
| Portal Step | one `Fx.teleportLook` (at most 64 candidate spots) | 2 ring+burst groups | bounded by `Fx.ring` (at most 64 dust) |
| Spatial Fold | one `foesAround` (at most 24 entities), at most 12 moved | 1 group | as above |
| Psionic Lance | one `foesAround`, one hit | 1 group | as above |
| Psionic Reach | one entity query in a 97-block cube once per 30 s per wearer; at most 10 effects, 6 chat lines | 1 group + 6 lines | as above |
| Rewrite / False World | one `foesAround` / one `Mob` query (24-block radius, at most 24 retargets) | 1 group | as above |
| Power Surge / Time Lock | one `foesAround` (at most 24), at most 24 hits/effects | 1 group | as above |
| Rewind | a 6-sample ring per Time wearer (about 200 bytes); one teleport inside the loaded chunks | 2 groups | as above |
| Soul Grasp | one `foesAround`, one target | 1 group | as above |
| Soulworld | one zone per wearer, 64 on the server at most; one player query (8-block radius) per second for 15 s; a ring per second through the perf budget | 1 ring a second per open zone | as above |
| Events | damage and effect handlers return at once for a player without a working stone (one map read); containers: one slot sweep per chest close | none | none |
| Away rule | one ledger pass of six ids a minute | none | none |

Nothing is spawned by any slot ability (no entity, no block change, no chunk load). Per-player tables (state, rewind ring, zone, cooldowns, a stone waiting for its respawn) are keyed by UUID and emptied on logout, death and dimension change (`StoneEvents`, `StoneState`).

## 7. NEW / REPLACED / KEPT

NEW: the `infinity_stone` Curios slot and its data; the EP gate; twelve slot abilities; the slot passives; the cost/cooldown lend; the equip surge and whispers; `/multiverse stones ...`; the Vormir trial command; the container rule; the away rule (`UniqueLedger.loseAway`); the server config; self-tests `stones.rules`, `stones.defs`, `stones.state`, `stones.ledger`; tests `StoneRulesTest`, `StoneSourceScanTest`; `tools/lang/infinity_stones.json`.

REPLACED: nothing of the Wave 30 content. Within this feature the design's Dominion was replaced by the Psionic Lance (owner decision: no mind control), the design's Soul Grasp instant kill by a 3 s soul hold, the design's "pocket dimension" by a zone effect, the design's 128-block Portal Step by the 32-block `Fx` blink, the design's Vormir "50% of EP" offering by skill or pet only.

KEPT: the six stones (ids, items, models, icons, palettes, worn looks), all their old acts and passives, the Gauntlet, the Snap, the Greater Wish, the Wave 26 Mind Stone helm, the Order's ledger, the carried-effective behaviour (setting `enabled = false` restores the old behaviour exactly).

Coordinator corrections applied: (1) rework, not six new stones; (2) the Order's ledger reused, no new SavedData; (3) the gate reads base max EP (`TensuraBridge.baseMaxEp`), never the grade-boosted value; (4) slot always exists, gate checked at equip and on the once-a-second pass; (5) BalanceLaw caps (control 1/3/5 s through `controlTicks`, healing under 50% a cast, GOD-class damage, no unbounded effect); (6) the Soul offering never lowers EP and Soul Grasp is a trap, never a kill; (7) canon limits added where cheap: Rewrite never on bosses and temporary, the recoil never kills, Soulworld is a zone, Time Lock excludes allies (allies are never foes), no player is thrown, sacrifices are gone for good, the Soul anchor is once a day, a sleeping stone is not destroyed (the audit's "banish the unworthy" was not needed). Owner decision applied: no mind control (section 2).

## 8. Caveats and what was not verified

- Nothing was compiled or run (no Gradle in the session); CI is the first compiler. Pure code was compiled and checked with `javac`.
- The Blender rule (14): no model was made or changed. The stones' existing models, icons and worn looks were reused; there is no new visual asset. No Blender session was available.
- Better Combat and engravings do not apply (the stones are not weapons).
- Portal Step does not fire a teleport event, so protection mods that listen to teleports will not see it (like every other relic blink using `Fx.teleportLook`). Forced-movement flags (`FORCE_MOVEMENT_EVENT`, `NO_FORCED_MOVE`) were not found in the repo; Spatial Fold simply never moves a player or a boss.
- The keep-on-death rule takes the stone from the drop list at lowest priority; whether Curios has already put the worn item into the list at that point was not seen in a game. If Curios drops it another way, the stone falls as before (`onDeath = drop` behaviour). With keepInventory nothing is dropped.
- A stone inside a shulker box inside a chest is not caught by the container rule; hoppers and droppers are not covered.
- Wards: Portal Step lands at the nearest safe spot like other relic blinks; it does not check protection regions.
- The Foresight dodge of the Time Stone overlaps the old Foresight ritual's first-blow guard (the audit says pick one); both are kept, the new one has a 60 s cooldown.
- The attribute passives for Mind and Soul use Tensura attribute ids (`tensura:magicule_regeneration_multiplier`, `tensura:max_aura`) that are skipped if the registry does not know them.
- Soul Grasp "see its skills" is only health and EP. The audit's cap of the scale at 3 instead of 2 was not taken (the owner wrote 2).
- Wiki research (rule 10): no wiki was fetched in this work; the canon notes come from the owner's design and from the audit file (`docs/infinity_stones_proposal.md`), which lists its own sources and what it could not fetch.

## 9. Phase B (not built)

The Infinity Gauntlet / Nano Gauntlet carrier work (a gauntlet that holds several stones), Iron Man Mark LXXXV, Ultron armour, Loki's Scepter (without mind control: the owner's decision covers it), the Snap rework and its options, the stone synergy circuit (the comics feedback loop), other stone sources (Vormir altar block, dungeon and boss drops, the End raid, the shrine; only the admin command and the Soul trial command exist now), a stone returning to its shrine, Portal Step's two-way portal pair, block rewind of the Time Stone (a journal of explosion and mob damage, containers excluded), the Reality clone, Mind Link and telekinesis, Death and Ego stones, the per-stone ability tooltips and card text in the relic book, new stone VFX layers (the Time VFX must not be touched, rule 3), Blender models for a slot-worn variant.
