# Devil contracts, phase 2: rules of the pact

Phase 1 (docs/devil_double_magic.md) gave every devil one holder in the world (One of One), Double Magic and the creeping corruption. Phase 2 is the owner's contract mechanics spec turned into server rules: a ritual that can fail, an ego (EP) requirement, a trial fight, three kinds of contract, stacking, shared devils, sever by killing and a sabotage debuff. Everything is decided on the server, per player UUID, at an event (a ritual ends, a contract is signed, a player dies or logs out, a blow lands between two holders). There is no tick work, no scan, no packet of its own.

Every rule has its own switch that gives the game as it was before. The master switch `devilPhase2.enabled = false` restores phase 1 exactly.

## 1. The rules in plain words (for players)

| # | Rule | What happens | Switch (server config `nusmp-devil-phase2-server.toml`) |
|---|------|--------------|------|
| 1 | The devil may not come | When the ritual ends the devil answers with a chance by rank: Low 90 %, Mid 75 %, High 55 %, Supreme 35 %. Carrying something of that devil (the devil core of Liebe, Lucifero, Beelzebub, Astaroth or Megicula, or the Behelit egg for the God Hand; the item tags `devil_relics/<devil>` list them and a pack can add more) adds 35 points. Never under 5 %. A failed call is spent: the ritual's own cost is not given back. Creative players are always answered. | `summon.enabled` |
| 2 | The devil may turn on the circle | Some devils (data) strike everyone in the circle except the caller when they appear: 40 % of their health (never the last hearts), Weakness and Darkness. At most 8 players. Lucifero 50 %, God Hand 25 %, the Noir and Rouge daemons 30 %, Beelzebub 15 %, Astaroth, the Curse Devil 10 %, and so on. | `summon.betrayal` |
| 3 | Ego (EP) | The contract asks your Tensura existence points (maximum EP). Supreme about 1 billion, each rank down a tenth: High 100 million, Mid 10 million, Low 1 million. Checked on the server when you call, when the devil answers and when you sign. Creative players pass. | `ep.enabled`, `ep.scale` |
| 4 | The trial | Devils that fight (Liebe, Lucifero, Beelzebub, Astaroth, Megicula, Lucifugus, the Curse Devil, a kin of Lucifer, the Noir and Rouge daemons) step out of the circle and fight you before they agree. Only you can hurt it and it only fights you. Bring it to half health and it is impressed (even or submission). Beat it and you may dominate it. It leaves after 150 seconds, or if you die, leave or log out. | `trial.enabled`, `trial.seconds`, `trial.max`, `trial.impressShare`, `trial.healthScale` |
| 5 | Three kinds of contract | Submission: a quarter of the devil's power, safer (the corruption of its price grows half as fast). Even, 50-50: half. Dominance: all of it, but only after you beat the devil in the trial and your EP is 3 times the requirement. A devil that does not fight signs submission or even, never dominance. | `kinds.enabled`, `kinds.dominanceEpFactor` |
| 6 | Read before you sign | The offer is a panel: how many contracts the devil has already, your share after joining, your EP against what it asks, the kinds you may take (clickable) and why the others are shut, and the risks. `/devil terms` explains all of it. | `kinds.confirm` |
| 7 | Stacking | A lesser devil (under Supreme) stacks freely, up to 8 at once. A Supreme devil asks 5 times its requirement for the second one, rising toward 10 times for further ones, and two Supreme is the most. Only one devil is active at a time: `/devil pact use <devil>` swaps (once a minute, not while the devil is let in). The others are held, their price paid, their share counted. | `stack.enabled`, `stack.weakMax`, `stack.supremeMax`, `stack.strongFirstFactor`, `stack.strongMaxFactor` |
| 8 | One of One, and shared devils | Supreme devils keep one holder in the world. Every lesser devil is shared by up to 4 contracts and its power splits evenly: two holders get half each, four get a quarter. A player whose devil gains a holder is told. | `shared.enabled`, `shared.maxContracts` |
| 9 | Sever by killing | A holder who kills another holder of the same shared devil severs the victim's contract with it for good: the victim can never sign with that devil again (an operator can lift it). The killer's share grows. | `sever.enabled`, game rule `devilSeverOnKill` (default true) |
| 10 | Sabotage | The killer who severs a contract is cursed for 3 minutes: their devil's power is cut to half and they carry Weakness. The same cost falls on a holder who hits fellow holders of one shared devil 5 times inside 30 seconds. | `sabotage.enabled`, `sabotage.seconds`, `sabotage.power`, `sabotage.curseMark`, `sabotage.friendlyFireHits`, `sabotage.friendlyFireWindowSeconds` |
| 11 | The four grimoire pact devils | Ignivar, Verbael, Gravemaw and Kairovore stay open to everybody (the owner's decision). | `shared.pactDevilsOneOfOne = true` gives each one holder (enforced when the pact is bound) |

### What the explanation in the signing panel says (lang keys `anime_multiverse.devil.phase2.*`)

The panel and `/devil terms` are translatable lines with their English as the fallback. They are generated from the code by `tools/lang/gen_devil_phase2.py` (68 lines, merged append-only into `en_us.json`, kept in `tools/lang/devil_phase2.json`), so the lang file and the code cannot drift apart; `Phase2SourceScanTest` fails if a line is missing.

## 2. The EP table (data: `assets/anime_multiverse/devil_phase2.json`)

| Rank (grade) | Single requirement | Summon chance | With a relic | Devils of that rank (roster) |
|---|---|---|---|---|
| Supreme (God) | 1 000 000 000 | 35 % | 70 % | Lucifero, Beelzebub, Astaroth, Sebastian, the Noir and Rouge daemons, the God Hand |
| High (Legend) | 100 000 000 | 55 % | 90 % | Liebe, Megicula, Lucifugus, the Curse Devil, the Future Devil, a kin of Lucifer, five daemons (Blanc, Jaune, Violet, Vert, Bleu), the Hall demon, an Etherious |
| Mid (Unique) | 10 000 000 | 75 % | 100 % | the Fox Devil, the Ghost Devil, kins of Iblis, Egyn and Amaimon |
| Low (Rare) | 1 000 000 | 90 % | 100 % | a kin of Astaroth |

A devil can carry its own number (`devils.<id>.ep`); none does by default. The server multiplies the whole table by `ep.scale` (0.5 = half).

Derived requirements with the defaults:

| Contract | Needs |
|---|---|
| First Supreme devil, even or submission | 1 000 000 000 |
| Dominance of a Supreme devil | 3 000 000 000 (3 times) |
| Second Supreme devil | 5 000 000 000 (5 times; 10 times with `strongFirstFactor = 10`) |
| Third and later (only if `supremeMax` is raised) | 7 500 000 000, then 10 000 000 000 (the factor rises in two equal steps from 5 to 10 and stays) |
| A lesser devil | its rank's requirement; dominance 3 times |

The pure formulas are in `Phase2Rules` and are unit tested (`Phase2RulesTest`: chance, EP, stacking, share, caps, sever); the table file is tested against the roster (`Phase2TableTest`).

## 3. Contract kinds, shares and what they do to power

| Kind | Share of the devil | Needs | Corruption of the price |
|---|---|---|---|
| Submission | 25 % | the requirement; for a fighting devil, it must be impressed | grows at half speed |
| Even | 50 % | the requirement; for a fighting devil, it must be impressed | normal |
| Dominance | 100 % | 3 times the requirement, a won trial, a devil that fights | normal |

Your power is: kind share, divided by the number of holders of that devil (1 for a Supreme devil), times the sabotage factor (1, or 0.5 while cursed). It scales the devil's stat bonuses (strength, speed), the attribute's combat effect and the damage of the Devil Magic arts. A contract older than phase 2 is adopted as dominance at login, so nobody loses power by the update.

Example: a shared devil with 4 contracts, one of them dominance: that player has 25 %; if a fellow holder is severed, the other three have 33 %.

## 4. How the owner's wording was read (decisions to confirm)

* "5 times to 10 times the single requirement": the second Supreme devil asks 5 times; the factor rises in equal steps to 10 times for any further one. With the default cap of two Supreme the rise to 10 is only reached if the owner raises the cap. `strongFirstFactor` can be set to 10 for a flat tenfold second devil.
* "The 10-star menu is the intrinsic skill list": no new menu was built; the existing intrinsic skill Devil Bond stays the list.
* "Tinsel": the existing tag `#anime_multiverse:devil_contract_blood` is kept.
* "Sabotage debuff on the killer": the killer of a fellow holder both severs the contract (rule 9) and is cursed (rule 10). Repeated friendly fire between holders (without a kill) costs the same curse after 5 blows in 30 seconds. A kill by a player who does not share a devil with the victim is neither. The debuff does not stack: a new one only extends the end time.
* "Betrayal strike": it is not lethal (it leaves at least two health) so a ritual can not be used to kill a bystander in one blow.
* "Impressed": the devil's health at or under `trial.impressShare` (half). Dominance needs the kill.
* "Three players within the same Minecraft day": the owner's message ended there, so no rule acts on it (section 7).

## 5. Commands, game rule

* `/devil sign [submit|even|dominate]`: with the panel on and no word, shows the panel; with a word, signs that kind if the server allows it. `/devil refuse`, `/devil terms` (the explanation).
* `/devil pact list` (your contracts, kinds, shares, bans, curse), `/devil pact use <devil>` (swap the active devil), `/devil pact unban <player> <devil>` (operator).
* `/devil clear <player>` also ends every contract of the ledger (bans stay).
* Game rule `devilSeverOnKill` (default true, MISC category): switch the kill rule off in a world without a restart. Both it and `sever.enabled` must be on.

## 6. Settings

All gameplay settings are SERVER settings (`serverconfig/nusmp-devil-phase2-server.toml`), each clamped again when read. Defaults:

| Key | Default | Meaning |
|---|---|---|
| `devilPhase2.enabled` | true | master switch: false = phase 1 as it was |
| `summon.enabled` / `summon.betrayal` | true / true | chance of the answer / the strike on the circle |
| `ep.enabled` / `ep.scale` | true / 1.0 | the ego rule / multiplier on the EP table |
| `trial.enabled`, `seconds`, `max`, `impressShare`, `healthScale` | true, 150, 4, 0.5, 1.0 | the trial fight, its length, the most at once on the server, the impress share, the health multiplier |
| `kinds.enabled`, `dominanceEpFactor`, `confirm` | true, 3.0, true | three kinds, the dominance EP factor, the panel |
| `stack.enabled`, `weakMax`, `supremeMax`, `strongFirstFactor`, `strongMaxFactor` | true, 8, 2, 5.0, 10.0 | stacking and its bounds |
| `shared.enabled`, `maxContracts`, `pactDevilsOneOfOne` | true, 4, false | sharing, the cap, the four pact devils |
| `sever.enabled` | true | the kill rule |
| `sabotage.enabled`, `seconds`, `power`, `curseMark`, `friendlyFireHits`, `friendlyFireWindowSeconds` | true, 180, 0.5, true, 5, 30 | the debuff and the friendly-fire count (0 hits = friendly fire is not punished) |
| `sabotage.sameDayTripleHook` | false | the reserved hook (section 7) |

Hard bounds that no config can pass: 16 contracts per devil, 16 devils per player, 8 Supreme, 64 bans per player, 4096 players in the ledger, 16 trials on the server.

## 7. The unfinished third sabotage rule (reserved hook, default off)

The owner's last section began "if three players make a contract within the same Minecraft day ..." and was never finished. Nothing in the mod acts on it. With `sabotage.sameDayTripleHook = true` every new contract posts the event `devil.phase2.DevilPactJoinedEvent` once, on the server, carrying the player, the devil, how many contracts it now has and the Minecraft day (game time / 24000). A later rule, a datapack mod or another addon can count three in one day without scanning anything. It is not cancellable and costs one event post per signature (nothing when off).

## 8. Where the state lives

* `DevilLedger` (world saved data `nusmp_devil_phase2`, by player UUID): each player's contracts (devil, kind, day signed), ban list and the end of their debuff, plus a per-devil list of holders kept in step, so the count a screen shows is a lookup, never a scan. Bounded when written and again when read.
* The active contract stays in the existing attachment `DevilContract` (so Phase 1 code is unchanged); the held-but-not-active devils are in the ledger only.
* One-of-One rows (`DevilHolders`) are kept for Supreme devils only (and for the pact devils when that setting is on); a player with two Supreme devils has two rows (new `claimKeep`; death, absence and renewal now apply to all of a player's rows).
* Live, never saved: the trial's entity id, its result, the friendly-fire count. Cleared on logout, death and a change of dimension (the bans, the debuff and the ledger stay: they are the world's memory).

## 9. Worst-case cost (rule 11)

| Where | Worst case |
|---|---|
| Entities | one trial devil per player, at most `trial.max` (default 4, hard 16) on the server, each at most 150 s and removed when its caller dies, leaves, goes far (48 blocks) or runs out of time. Never saved, never a mob-cap entity. No other entity is spawned. |
| Packets | no packet of its own. Chat lines to the one player concerned (a panel is at most 14 lines; a betrayal at most 8 victims, one VFX packet each through the existing effect budget). The trial devil is a normal tracked mob (position updates to players within 8 chunks, at most 4 of them) and a vanilla boss bar to its caller (sent only when the health changes). The contract sync after a change is the existing one: a join or a sever refreshes the other holders of that devil, at most 4 by default (hard 16), 1 to 2 packets each, once per event. |
| Server work per tick | none. No tick event of its own. The existing once-a-second contract step reads the share: one ledger lookup (at most 16 pacts) and a few multiplications per player with a contract. |
| Work per event | gate: one ledger lookup, one EP read, at most 41 stacks checked against a tag (once per ritual end); a death between holders: a compare of at most 16 by 16 ids, and for each severed devil a refresh of its holders (default at most 8 devils x 4 holders = 32 refreshes, hard 16 x 16 = 256, once per kill); a blow between two players: two row lookups, allocation-free when they share no devil; a trial check once a second per trial entity. |
| Memory | the ledger: at most 4096 rows of 16 contracts and 64 bans; written to disk only when something changed. |
| Client draw cost | one vanilla humanoid per trial (at most 4 exist). Nothing else is drawn. |

`PerfProbe` sections are not added: there is no new per-frame or per-tick path to time.

## 10. NEW, REPLACED, KEPT

NEW: the summon chance and the relic tags (`tags/item/devil_relics/<devil>.json`); the betrayal; the EP rule and table; the trial entity, its control and its renderer; the contract kinds; the signing panel and `/devil terms`; stacking and `/devil pact use|list`; shared lesser devils; sever by killing and the game rule `devilSeverOnKill`; the sabotage debuff and friendly-fire count; the ban list and `/devil pact unban`; the pact-devil setting; the reserved same-day event; the server config `nusmp-devil-phase2-server.toml`; the lang lines; the self-tests `devil.phase2.*`; the tests `Phase2RulesTest`, `Phase2TableTest`, `Phase2SourceScanTest`.

REPLACED: nothing was removed. One behaviour changed under a switch: with phase 2 on, a lesser (non-Supreme) devil is no longer One of One (it is shared up to 4). `shared.enabled = false` or `enabled = false` restores phase 1 for it. The single `[Sign]` button of the offer is replaced by the panel while `kinds.confirm` is on (`/devil sign` itself is kept).

KEPT: Double Magic, the corruption curve, the duel, the lapse rules, the old [Sign]/[Refuse] flow (when the panel is off), the contract attachment and its save format, the three old grimoire prices, every Time Magic VFX (untouched), all existing lang keys.

Shared files that were touched, each by a few appended lines: `DevilRituals`, `DevilContracts`, `DevilOwnership`, `DevilHolders`, `DevilCommands`, `DevilBoot`, `DevilCombat`, `DevilContract`, `DevilEntities`, `DevilNpcs` (one method made public), `DevilClient`, `DevilArts`, `ForbiddenMagic`, `NUGameRules`.

## 11. Not verified, and what is a placeholder

* Nothing here was seen in a running game. The pure classes (rules, table) were compiled and their 17 unit tests, the 8 source-scan tests of this package and the older devil, double-magic, config-name, server-safety and perf scans were run (all green); the code that touches Minecraft was written against the repo's APIs but could not be compiled offline (CI is the compiler). The self-tests `devil.phase2.rules|ep|summon|stack|share|sever|trial|save` run on the server smoke test.
* The trial devil's look is a vanilla zombie-skinned humanoid with the glowing flag and the devil's name over a boss bar. There is no Blender or Blockbench model for it: CLAUDE.md rule 14 is not met for it. The figure of the ritual (the real devil model) is unchanged.
* The panel is a chat panel with clickable words (no new packet, no client screen). A real client screen would need a packet pair and was not built.
* Held-but-not-active devils are ledger entries: their price is paid and their share counts, but only the active devil gives powers. `/devil pact use` swaps.
* The pact-devil one-of-one is enforced when a pact is bound, not afterwards (there is no unbind of a grimoire pact to hook).
* The EP of a body is `EnergyHelper.getMaxEP`; if Tensura is absent it reads as 0, so with the rule on nobody can sign (switch `ep.enabled` off in such a pack).
* A player killed by a fellow holder keeps the sacrifices of the severed devil (they are only removed by an operator, as for every dissolved contract).

## 12. Files

Code: `devil/phase2/` (`Phase2`, `Phase2Rules`, `Phase2Table`, `Phase2Config`, `Phase2Text`, `DevilLedger`, `DevilTrialEntity`, `Trials`, `DevilPactJoinedEvent`, `Phase2SelfTests`), `client/devil/DevilTrialRenderer`. Data: `assets/anime_multiverse/devil_phase2.json`, `data/anime_multiverse/tags/item/devil_relics/*.json`. Lang: `tools/lang/gen_devil_phase2.py`, `tools/lang/devil_phase2.json`. Tests: `Phase2RulesTest`, `Phase2TableTest`, `Phase2SourceScanTest`. New smoke prefix: `devil.phase2`.
