# Dungeon boss rules (owner's rules)

Status: DRAFT, rules only. No boss code is built from this yet. Items marked OPEN need the owner's answer.

## The three core rules
1. **One custom Unique/Ultimate skill per boss.** Built by us (not a base Tensura skill); it may pull status effects and abilities from Tensura.
   - A real registered skill (own name, icon, tooltip). The tooltip does NOT have to disclose the borrowed Tensura effects.
   - Exactly one skill per boss, one-of-one (no two bosses share a skill).
   - Players can get it: it gives a **10% gain**; it can be taken or sealed by any of the existing means (OPEN: what the 10% applies to, and whether "1 of 1" means one copy per server or one skill per boss).
2. **One signature mechanic, separate from the skill.** An arena or phase rule the player must understand and counter (e.g. Madara's Six Paths). Telegraphed, with an intended answer.
3. **Strength starts slightly above base Tensura and keeps rising.** Scaled by dungeon rank, F-Rank up to SSS-Rank, from one scaling table that every boss reads (HP, damage, armor, speed per step). See "Rank ladder" below. Also scaled by nearby player count. Not scaled to player power.

## Rank ladder
Ranks: F, E, D, C, B, A, S, SS, SSS (9 ranks). Inside each rank a boss sits on a sub-step, from negative up to positive, and then ranks up:
`F-` -> `F` -> `F+` -> `E-` -> `E` -> `E+` -> ... -> `SS+` -> `SSS-` -> `SSS` -> `SSS+` (27 steps).
The scaling table has one row per step, so the strength curve is smooth and every boss names its step (e.g. "C-Rank, plus"). (OPEN: confirm this reading; the alternative is that "negative to positive" means weak to strong inside a rank with no named sub-steps.)

## Floors to ranks (rank up every 20 floors)
A floor is a place in the Mega-Dungeon, a rank is how strong a boss is. The dungeon ranks up every 20 floors, counted by distance from Floor 0 (the same distance `FloorMap.threat(floor)` uses, so the negative side mirrors the positive side):
| Floors (distance from Floor 0) | Rank |
|---|---|
| 1-20 | F |
| 21-40 | E |
| 41-60 | D |
| 61-80 | C |
| 81-100 | B |
| 101-120 | A |
| 121-140 | S |
| 141-160 | SS |
| 161-180 | SSS |
Inside a rank the sub-steps (minus, plain, plus) follow the floor: about the first 7 floors are minus, the middle 6-7 plain, the last 6-7 plus. Floors 181-200 stay at SSS+. While the dungeon is still shallower, A to SSS can live in rifts and secret floors.
A boss uses the step of the floor it stands on. A boss from a tier list keeps its own named rank and appears on floors of that rank.
DECIDED (update): the dungeon will grow in the future, to about floor 200 (confirmed by the owner: 200). With rank up every 20 floors, floors 1-180 cover F to SSS exactly as in the table above, and floors 181-200 stay at SSS+ (the final stretch). Until the dungeon is that deep, the floors that exist today only reach their current rank, and the higher-rank bosses (A, S, SS, SSS) can live in special rifts or secret floors in the meantime. A boss in a rift or secret floor uses the step of its own rank, not the floor number.

## Source material
Bosses from other series follow their source material (skills, look, behaviour).

## Same element, different feel
Every boss has a Feel Sheet (silhouette/model, colour + VFX palette, movement + rhythm, sound). Two bosses of the same element differ in at least three of the four. Two ice bosses are planned as the first test (see "Ice pair" below).

## Fight structure
- 2-3 phases at HP thresholds (about 66% / 33%); the mechanic changes or intensifies.
- Soft enrage timer.
- Intro and defeat sequence (title card, voice line, defeat scene).
- Fixed arena / boundary; leaving it resets or punishes.

## Fairness
- A visible telegraph before every big hit.
- At least one counter to the signature skill and one to the mechanic.
- Anti-cheese: resistance or immunity to pearl-skipping, pillar-stacking, suffocation, void-pushing.
- Full kit allowed for players, except a short banned list inside boss arenas (OPEN: exact list; proposal: teleport-out, instant-kill on the boss, time-stop on the boss).

## Multiplayer and server (CLAUDE.md rules 9 and 11)
- All state server-side, keyed by player UUID.
- HP/damage scale with players nearby; per-player damage tracking decides loot.
- Per-player loot and credit, no kill-stealing.
- Hard caps on minions, particles and projectiles per boss; effects sent to nearby players only; worst case stated in each boss doc.
- Solo/group: F-B solo-able, A group-recommended, S-SSS group-required.

## Rewards
- Guaranteed signature drop (unique item or relic) plus a rare engraved weapon (CLAUDE.md rule 6).
- First-kill reward per player; repeat-kill cooldown or daily limit.

## Presentation
- Named boss bar with phase markers; theme music per boss.
- 3D model, glow and effects at the Genesis standard (CLAUDE.md rule 7).
- Codex lore entry per boss.

## Production
- One data-driven boss template: skill, mechanic, phases, scaling rank, loot, arena, feel sheet.
- A spawn / kill / loot self-test per boss in the CI smoke test.

## Ice pair (example of the feel rule)
| | Boss A "Frozen Stillness" | Boss B "Blizzard Fury" |
|---|---|---|
| Fantasy | calm, precise sovereign of cold | wild storm-beast of cold |
| Look | tall, slender, clear crystal, pale blue/white, clean shards | bulky, jagged, frosted, deep teal/grey-white, snow and mist |
| Movement | slow, deliberate, teleports between ice mirrors | fast, charging, sliding |
| Mechanic | time-freeze zone: cooldowns freeze while you stand in it | whiteout: vision and range shrink, stay near heat points |
| Counter | break the mirrors, burst damage | reach heat points, avoid the blizzard |
(Placeholder design; final bosses and mechanics are the owner's call.)

## Open questions
- What exactly is the "10% gain", and what does "1 of 1" mean (one skill per boss, or one copy in the world)?
- Which two bosses are the ice pair, and in the same dungeon or different ones?
- How many bosses per rank (F, E, D, C, B, A, S, SS, SSS)?
- Rank ladder: is my reading right (see below)?
- How are the rifts / secret floors for A, S, SS, SSS entered?
- Exact banned-skill list inside boss arenas.
- Per-rank multipliers for the scaling table (proposal: F- at about 1.1x the base Tensura equivalent, rising about 1.1x per sub-step, so one full rank is about 1.33x).
