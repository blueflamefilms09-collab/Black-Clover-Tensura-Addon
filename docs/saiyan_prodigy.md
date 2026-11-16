# Saiyan Prodigy and the Saiyan-blooded evolution

What the owner asked for: the Saiyan race is an evolution of Human, gives some intrinsic skills and slight buffs; one intrinsic skill, the **Saiyan Prodigy**, holds every Saiyan ability as a mode so a Saiyan needs one slot and not one key per ability; the evolution is a real race evolution (called Saiyan-blooded); and the old Dragon Ball skills get a use.

Nothing here was run in a game. It compiles, the pure tests and the source-scan tests pass, and a server self-test (`prodigy.*`) is added for CI. See "Caveats".

## 1. The Saiyan Prodigy (one skill, one slot)

`anime_multiverse:saiyan_prodigy`, an **intrinsic** skill (icon `textures/skill/saiyan_prodigy.png`). Slot it, press its key to use the mode, the mode key changes the mode (locked modes are skipped). Sneak + press does the sneak action of a mode that has one. A hold mode (Kamehameha, Spirit Bomb, Ki Wave) is held and released like the old key.

Every mode sends the **same op the old key sent** through the same server entry (`SaiyanNet.onAction`), so the awakening, the Goku lock, Ki costs, cooldowns and the 4-tick rate limit are exactly as before. The table is one data list, `saiyan/SaiyanProdigyModes.BUNDLE` (pure, unit tested); the generic part is `skill/modes` (`ModeSpec`, `ModeBundle`, `MultiModeSkill`), reusable by any other multi-mode skill.

| # | id | press | sneak + press | needs |
|---|----|-------|---------------|-------|
| 0 | ki_blast | Ki Blast (op 1) | | awakened |
| 1 | ki_barrage | Ki Barrage (2) | | awakened |
| 2 | ki_flight | Ki Flight toggle (3) | Flight burst (4) | awakened |
| 3 | senses | Ki Senses (5) | Suppress ki (6) | Saiyan |
| 4 | ki_barrier | Ki Barrier (14) | | awakened |
| 5 | deflection | Ki Deflection (20) | | awakened |
| 6 | energy_transfer | Energy Transfer (21) | | awakened |
| 7 | ki_wave | hold: start (29), release (30) | | awakened |
| 8 | form | toggle the selected form (10) | next form (11) | awakened |
| 9 | art | perform the selected art | select the next art | anyone with the skill |
| 10 | dragon_fist | Dragon Fist (18) | | awakened |
| 11 | kamehameha | hold: start (8), release (9) | | Goku line |
| 12 | rsp | Rock-Scissors-Paper (7) | | Goku line |
| 13 | instant_transmission | Instant Transmission (13) | | Goku line |
| 14 | spirit_bomb | hold: start (15), release (16) | | Goku line |
| 15 | meteor_combination | Meteor Combination (17) | | Goku line |
| 16 | god_bind | God Bind (19) | | Goku line |
| 17 | style | switch the selected fighting style on / off | next style | anyone with the skill |

"awakened" = the Ki awakening is done (as before). "Goku line" = awakened and the Goku Ultimate (the old lock of those techniques). Mode numbers are saved by Tensura: **only append new modes at the end**.

Ops 1 to 35 are all reachable except the two named in `SaiyanProdigyModes.UNREACHED`: the previous form (the form list wraps, so "next" reaches every form) and the branch key (an owner decision of the Dragon Ball rework, off by default). The old keys keep working, so nothing is lost.

### Art mode (the Dragon Ball rework techniques and the old Dragon Ball skills)

The art list has two sources:

1. The **eleven Dragon Ball rework techniques** (Meteor Strike, Vanishing Step, Aerial Rush, Warrior's Counter, Ground Breaker, Combat Instinct, Charged Ki Sphere, Ki Reinforcement, Giant Ki Avatar, Afterimage Technique, Gravity Training), for an awakened Saiyan. They run through the old op, so their costs and cooldowns are unchanged.
2. The **active moves of the 15 Dragon Ball characters** (the modes of the old `DbSkills`, 30 ids that were orphaned by the phase 1 removal), through the old `DbMoves.cast` (so every old damage cap, the Balance Law and the boss cap apply):
   * A character opens at Battle Power `1000 * 10^(3.3 * (scale - 0.30) / 0.70) * dbArtsBpScale` (Bulma 1000, Krillin about 26 thousand, Vegeta about 1.2 million, Goku about 1.44 million, Gohan about 2 million) **or** if the player owns that character's old skill (Unique or Ultimate; kept, never taken).
   * Strength: Ultimate strength from 4 times the unlock, or when the Prodigy is mastered, or when the old Ultimate is owned; otherwise the Unique's half (as the old skill).
   * The mastery finisher of a character opens from 16 times the unlock, or mastered, or an owned Ultimate.
   * Cost: the old formula (`DbRules.cost`, `DMUtil.cost`, then `price` / `charge`, Tensura's magicule pay). Cooldown: the old per-move cooldown, kept per art in the skill tag (only arts still recharging are stored). The old "crouch and press" gate of some moves is dropped here (sneak selects the next art).

A non-Saiyan who owns an old Dragon Ball skill still gets the Prodigy at login (migration) and can use that character's arts and style; Saiyan modes stay closed for them.

### Fighting style (the old passives)

The mode "style" switches one character's **passive stance** (the old Dragon Ball passives: speed, regeneration, aura, reactor, ...) on and off through the old `DbPassives.second` once a second, so the passives are not orphaned either. One style at a time. Switching the Prodigy off in Tensura's skill screen ends it. The stance clears on forget, on toggle off, and when the hub or `dragonBallEnabled` is switched off.

## 2. Saiyan-blooded: the Human evolution

* `SaiyanTensuraRace` (`anime_multiverse:saiyan`, already a real Tensura race) now gives the Prodigy as its **intrinsic skill** (Tensura learns it when the race is set) and asks **three deeds** from a Human (`SaiyanBlood`, numbers in `SaiyanBloodRules`):

  | deed | default need | weight |
  |------|--------------|--------|
  | existence points (Tensura's own EP requirement) | 20000 (Enlightened Human asks 100000) | 40 |
  | worthy hostile creatures beaten (hostile, 10+ health, not a player or pet) | 25 | 40 |
  | survived at or below 20% health (counted once per 10 minutes) | 3 | 20 |

  Weights are Tensura's convention (add up to 100, the evolution opens at 100). A deed with a need of 0 is off and gives its weight to the others. The two counters live in the **Human race instance tag** (saved and synced with the race, so the evolution screen shows them) and are capped at 1000.
* `HumanRaceEvolutionMixin` (optional, `require = 0`, pseudo target) adds the Saiyan to `HumanRace.getNextEvolutions`, so Tensura's evolution screen offers it and Tensura's own server-validated evolution packet accepts it.
* Becoming a Saiyan by the evolution fires ManasCore's `SET_RACE`, which the existing `SaiyanRaceLink` turns into the Saiyan flag (Ki, Battle Power, Zenkai and everything else as before), grants the Prodigy once, and (setting `evolutionAwakens`) **awakens the Ki at once** (the sit-still awakening is skipped).
* `/saiyan blood` shows the progress. `/saiyan prodigy` shows the status, `/saiyan prodigy grant <player>` (permission 2) grants it.

### Buffs (the "slight buffs", no hunger anywhere)

For every Saiyan (while the hub and `perks.enabled` are on), re-applied once a second as transient modifiers, removed on logout, death cleanup and when the race is lost:

* +4 maximum health (two hearts), flat
* +3% attack damage
* +0.5% Ki regeneration per second (of the maximum), awakened Saiyans only

These come on top of the existing race numbers (`SaiyanRaceRules.BONUS`, unchanged) and the stage buffs.

## 3. Settings (all SERVER, `config/nusmp-saiyan-prodigy-server.toml`)

| setting | default | what it does |
|---------|---------|--------------|
| `prodigy.prodigyHub` | true | the master switch. **false restores the old behaviour exactly**: nothing is granted or migrated, the race gives no skill, the old Dragon Ball skills act as before, `DbSkills.onLogin` forgets them again |
| `prodigy.grantToSaiyans` | true | grant the Prodigy to every Saiyan once |
| `prodigy.prodigyMigrate` | true | migration at login (see 4) |
| `prodigy.dbArts` | true | the Dragon Ball characters' moves and styles in the Art and Style modes |
| `prodigy.dbArtsBpScale` | 1.0 | multiplier on the Battle Power the characters open at |
| `prodigy.artCooldownSeconds` | 2 | floor between two arts |
| `prodigy.evolutionAwakens` | true | the evolution awakens the Ki at once |
| `blood.bloodEvolution` | true | the Human can evolve into the Saiyan (false = only command and race menu, as before) |
| `blood.bloodEp` / `bloodKills` / `bloodNearDeath` | 20000 / 25 / 3 | the three deeds (0 = off) |
| `perks.enabled` | true | the small gifts |
| `perks.health` / `attack` / `kiRegenPercentPerSecond` | 4 / 0.03 / 0.5 | their numbers |

Existing switches still apply on top: `dragonBallEnabled` (master switch of the Dragon Ball moves), `phase.enabled` (rework techniques), `enabled` (the Saiyan system).

## 4. Migration

At login (`SaiyanProdigyHub.onLogin`), once per player, server-side, keyed by the player's UUID (the stamp `nusmp_prodigy_given` is in the player's own saved data):

* a Saiyan without the Prodigy gets it (`grantToSaiyans`);
* a player who owns any of the 30 old Dragon Ball ids or the Ki skill gets it (`prodigyMigrate`);
* the old skills **stay owned** (nothing is deleted). While the hub is on, `DbSkills.onLogin` no longer forgets them (see decision 1). The old Dragon Ball skill, if pressed, tells the player to use the Prodigy's Art mode; its passives are driven by the Prodigy's Style mode.
* Idempotent: a second login, a second grant or a player who forgot the skill is not given it again by the migration.

## 5. NEW / REPLACED / KEPT

**NEW**: the Saiyan Prodigy skill and its icon, the generic `skill/modes` helper (`ModeSpec`, `ModeBundle`, `MultiModeSkill`), `SaiyanProdigyModes`, `SaiyanProdigyConfig` and its file, `SaiyanProdigyHub`, the Human to Saiyan evolution (`SaiyanBlood`, `SaiyanBloodRules`, `HumanRaceEvolutionMixin`), the Saiyan-blooded buffs, `/saiyan prodigy`, `/saiyan blood`, the Art and Style modes, the lang fragment `tools/lang/prodigy.json`, the self-test prefix `prodigy`.

**REPLACED** (all behind `prodigy.prodigyHub`, off restores the old way): the Saiyan having no intrinsic skill and no evolution (now Prodigy and the Human evolution); the separate-key way of using Saiyan abilities as the intended one (the keys still work); the removal at login of the old Dragon Ball skills (kept while the hub is on).

**KEPT**: all 30 Dragon Ball skill ids and the Ki skill (still registered, still owned; the Dragon Ball ones are inert and forward to the Prodigy while `dragonBallLegacySkills` is false); every old Saiyan key, packet, op and rule; `BONUS[SAIYAN]` and the race numbers; the Dragon Balls / Shenron wish system and items, `TransformGuard` and the Saiyan tail model (not touched); the Time Magic VFX (not touched).

## 6. Worst-case cost (rule 11)

* **No new packet.** A press goes through the existing skill key and the existing `saiyan_action` entry (validated server-side, rate limited to one ask per 4 ticks per player; a release is never dropped). The skill's own selection modes have a 4-tick gap.
* **A press:** one old op (unchanged cost); an Art or Style press builds the art list once: at most 128 entries and 30 skill lookups, no world access. At most one `DbMoves.cast` (its old bounds: 24 targets, 24 projectiles).
* **Per second per owner of the Prodigy:** three flag compares, and, only with a style on, one old `DbPassives.second`. The skill instance syncs only when a mirrored flag changes, when an art is selected or when a Dragon Ball art starts its cooldown (the `ArtReady` tag holds only arts still recharging, at most 128 small numbers).
* **Per second per Saiyan:** two attribute reads (written only when the value differs) and one Ki regen call.
* **Events:** a death or damage event does two cheap checks before it looks at the race; a deed is one counter write, capped at 1000, a survival at most once per 10 minutes per player.
* **Mixin:** one small list copy each time Tensura builds the Human's evolution list (the evolution screen).
* **Client draw:** nothing new is drawn. No static player state; per-player state is the skill tag, the race tag and the existing per-UUID run map, all cleaned on logout and death (the perks too).

## 7. Tests

* Pure: `ModeBundleTest`, `SaiyanProdigyModesTest` (unique ids, order, every op reachable, the Goku lock, the art list, the unlock formula, the cap), `SaiyanBloodRulesTest`, `SaiyanProdigySourceScanTest` (server config, no client class or hunger or static player or world writes, mixin listed and optional, every key in the fragment, the old pins kept).
* Server self-test, prefix **`prodigy`** (`SaiyanProdigySelfTests`, CI): `prodigy.rules`, `prodigy.skill`, `prodigy.race`, `prodigy.player`. The mixin check in `prodigy.race` reports, never fails.
* Existing tests adjusted by appending only: `SaiyanSourceScanTest` also reads `tools/lang/prodigy.json`; `SkillIconsV3Test` lists `saiyan_prodigy` as a root icon outside the repaint manifest.

## 8. Decisions for the owner

1. **The old Dragon Ball ids are no longer taken at login while the hub is on.** Phase 1 removed them from play and forgot them at login; rule 2 says keep what exists, and the Prodigy needs them to open a character by ownership. They stay owned but inert. Set `prodigyHub = false` to get the phase 1 removal back.
2. **Perks and deed numbers** (4 health, 3% attack, 0.5% Ki, 20000 EP, 25 kills, 3 near-death survivals) are my guesses at "slight"; all are settings.
3. **The race-menu route** still makes a Saiyan from nothing (existing behaviour). Say if the menu should stop offering it now that the evolution exists.
4. **The `Ki` Ultimate skill** (Solar Flare, Kaioken, Ultra Instinct ...) was not folded in: it is a separate, player-owned Ultimate with modes the Saiyan system does not have. Say if it should become Prodigy modes too.

## 9. Caveats (not seen in a game)

* Nothing was run in Minecraft. Compiles; pure and source-scan tests pass; the CI self-test is new and unproven.
* The evolution mixin is optional: if Tensura's `HumanRace.getNextEvolutions` differs at runtime, the Saiyan is not offered in the evolution screen (the self-test says so) and is still reachable by command and menu.
* A client without the server's setting shows the evolution even if the server has `blood.bloodEvolution = false`; the server still refuses it.
* Whether the deed counters on the Human race tag show in Tensura's evolution screen as text (the requirement component) was not seen.
* Whether `learnIntrinsicSkills` for the intrinsic Prodigy toggles it as expected, and whether a tag-synced mode lock is current on the client at once (it updates once a second), were not seen.
* The previous-evolution link (Saiyan back to Human in Tensura's evolution tree display) is not set.
* The Art and Style modes show their choice in the mode name; the HUD of the mode wheel itself is Tensura's.
