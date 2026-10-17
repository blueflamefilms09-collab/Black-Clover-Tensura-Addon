# Multiplayer parties and FTB Teams (1.58.0, builder PARTY)

The owner's request: "make the new dungeon multiplayer friendly (should also work with FTB team)". This file is the account of the party layer of the second edition of the
Mega-Dungeon (`docs/labyrinth/mega_v2.md` section 2): what was built, why each part is the way it is, the lane arithmetic, the FTB Teams bridge and what could not be checked
without FTB Teams installed, the config, the commands, what was measured, and, honestly, what has NOT been seen running. Read it with `mega_v2_gen2.md` (the floor model it
builds on) and `mega_v2_sys2.md` (the System seam it fills).

Nothing in it ran in a game. Everything that can be pure is tested; everything that needs the game is type-checked against API stubs, covered by in-game self-tests
(`party.*`) and by a phase of the smoke script for CI to run.

## 1. New, replacement, kept (rule 5)

| Part | Status |
|---|---|
| Party lanes: `MegaGrid.laneZ(polarity, party)`, `PARTY_STEP`, `MAX_PARTIES`; `PartyLanes` (pure lane questions) | NEW (the one-argument `laneZ` is KEPT and means party 0) |
| `Sector.party` (saved only when not 0) and the party-aware lookups of `MegaData` (`chain`, `chainSector`, `chainTail`, `sectorCount`, `parties`) | NEW (every old signature is KEPT and means party 0, so no other package changed to compile) |
| `PartyKey`, `PartyIndex` (pure), `MegaParties` (saved book `nusmp_mega_parties.dat`), `PartyResolver`, `PartyService`, `PartyGuard`, `PartyConfig`, `PartyCommand`, `PartyLines`, `PartySelfTests`, `PartyRegistry` | NEW (package `labyrinth.mega.party`) |
| The expansion queue grows the chain of the party of the sector it is given; `tryFirst(server, road, party)`; `mega.maxSectors` counted per party; the "Labyrinth shifts" line only to the party that waits | CHANGED (party 0 behaves exactly as before) |
| Seeds of a party: the party is mixed into the dungeon seed (`SectorDraft.partySeed`) | NEW (party 0 uses the dungeon seed as it is: every plan of a world without parties is unchanged, pinned by `PartyPlanTest`) |
| `MegaTravel`: the party is decided at the gate, the Landing offered and the first floor woken are those of the party's chain; a line about the party after the arrival; quest hook `mega.party` | CHANGED |
| `StairTravel`, `NextTheme`, `FloorAssigner`, `AvatarDelve`: they follow the party of the sector (or the Avatar's creator) | CHANGED (one line each) |
| `SystemParty.setProvider(PartyService::questMembers)`: the System's kill credit goes to the party (the team) | NEW provider; REPLACES the default "players in the same sector" |
| `/multiverse mega status [party n]`, `expand [holy\|abyssal] [party n]`, `party list\|of\|tp\|set` | NEW arguments and node (party 0 output unchanged) |
| `PartyMath.Member` carries the party; the Dungeon Master never makes one table of two parties | CHANGED (the 4-argument member is KEPT = party 0) |
| Self-tests `party.lanes`, `party.book`, `party.resolve`, `party.guard`, `party.chains`; `core.travel` and `core.chain_travel` pin their stand-in to party 0; `stairs.*` walk every party | NEW / CHANGED |
| `tools/ci/server_smoke.py` phase `phase_party` (+ `party-*` checks, the party-1 restart check) and `tools/ci/mock_server.py` speaking the party commands | NEW |
| `QuestTeams.teamOfId`, `ownerOf` (optional reflection methods) | NEW (the existing bridge is REUSED, not copied) |
| The Hub: ONE shared Sanctuary, its gates, the Directional Lock, stats, gear, rests | KEPT |
| The floor model, the stairs, the themes, the encounter tables, the Crucible and the Guardian logic | KEPT (they are already per sector, and a sector now belongs to a party) |

## 2. The model in one page

* **A party** is the FTB Team of a player (the team's id is the key), or the player alone (the player's id is the key; a player whose only FTB team is the personal team FTB gives
  everybody is a party of one). The Hub is one shared town; only the dungeon is instanced.
* **A party index** (0 to `party.maxParties - 1`) is handed out the first time a party goes in, saved for ever in the book `data/nusmp_mega_parties.dat`, and says which LANE the
  party's floors stand on. Each party has a Holy chain and an Abyssal chain on its own lane (section 3). Party 0 is the shared / legacy lane: it is where every world saved before
  parties has its floors, and where everybody walks when `party.mode = "shared"`.
* **The party of a player is decided when they ENTER from the Hub** (the polarity gate, the Gateway with `hub.enabled = false`, the enter command), after the Directional Lock has
  agreed. The index is written into the player's data (`nusmp_mega_party`) and kept while they are in the dungeon, whatever happens to their team meanwhile; the next entry
  decides afresh. Teammates who go in together share floors; a teammate who comes later joins the chain that exists, at the deepest Landing they reached, never deeper than the
  party's newest finished floor (the existing `LandingRule`, applied to the party's chain).
* **Inside the dungeon the truth is the floor under the player's feet**: the party of a sector is saved in the sector itself, so a login in the middle of a floor, a restart or a
  lost record can never put anybody in the wrong party (`PartyService.partyOf`).
* **Shared by the party:** the floors reached (the chain), the sectors, the Crucible's relic (one pick per decade: the Crucible is per sector and a sector is the party's), the
  Guardian kill and the Mana Core, the System's "defeat N monsters" credit (section 8). **Personal:** the Directional Lock, stats, gear, the System's other quests, rests (a rest
  by one player rests the others only when they share the safe zone, as before; two parties never share one).
* **A sector holds players of one party only.** The doors send everybody to their own floors; the guard (section 6) is the backstop for what the doors cannot see.

## 3. The lane arithmetic

A floor of the floor model stands at `x = chainIndex * 198` on the lane of its road and party (`FloorPlan.entry(road, chainIndex, party)`) and is at most 171 blocks wide in z
(plus 8 for a door cap and 12 for the gap: `PartyLanes.REACH` = 106 from the lane). Lanes are `MegaGrid.PARTY_STEP` = 4096 blocks apart.

| | Holy road | Abyssal road |
|---|---|---|
| party 0 (shared / legacy) | z = 0 | z = 300 000 |
| party k, k >= 1 | z = -k * 4096 | z = 300 000 + k * 4096 |

Why the Holy lanes go to the NEGATIVE side: party 0's two lanes must stay exactly where 1.54.0 to 1.57.0 put them (an existing world), and the Abyssal group starts at 300 000, so the
Holy group cannot grow towards it without a rule; the negative side is empty. The two groups can never meet: the Holy lanes fill `[-(P-1) * 4096, 0]`, the Abyssal ones `[300 000, 300 000 + (P-1) * 4096]`.

| `party.maxParties` | nearest lane z | farthest lane z | farthest block from the origin along z |
|---|---|---|---|
| 1 | 0 | 300 000 | 300 106 |
| 64 (default) | -258 048 | 558 048 | 558 154 |
| 171 | -696 320 | 996 320 | 996 426 (the most that fits a world border of 1 000 000 blocks) |
| 256 (the limit of the maths) | -1 044 480 | 1 344 480 | 1 344 586 |

Along x a chain runs to `(maxSectors - 1) * 198` = 197 802 blocks at the default `mega.maxSectors` of 1000 (per party now). The vanilla world border is 29 999 984: everything fits.
`PartyLanesTest` pins every number of this table, that no two of the 512 lanes are the same, that neighbours are exactly one step apart, that the inverse (`partyAt`, `roadAt`)
agrees, and that a floor's reach is far less than half a step. A step of 4096 is eight view distances of 32 chunks, so nothing of one party's lane is ever loaded, lit or ticked
because of a player in another (no cross-lane loading or wandering: the wandering bosses are bound to their sector, the Dungeon Master rolls per table, the sweeps look at
distances).

**The world border.** In vanilla every dimension follows the Overworld's border. A server that keeps a small border (`/worldborder set 10000`) has always cut off the Abyssal road at
z = 300 000 (an issue of 1.55.0, not new); with parties the lanes reach further. `PartyRegistry` logs the lane range at every start and WARNS when the border of
`nusmp:mega_dungeon` does not contain it. Lower `party.maxParties` or widen the border.

**The old ten-floor sectors (`floors.perSector` above 1) are not instanced.** Their chains wind through the dimension by the planner's choice and can wander thousands of blocks, so
nothing would keep one party's chain out of the next lane. `PartyPolicy.instanced` is false there, every answer is party 0, and the dungeon is shared exactly as in 1.57.0.

*A world that has both.* A world that was built with the old ten-floor sectors and is then switched to the floor model (`floors.perSector` 1) keeps its old sectors, all of them party 0 (a sector
without a saved party reads as 0), and new floors are built on the lanes of the parties. The old sectors are not moved, so the one thing to check is that none of them reaches into the band of
the next party. `PartyLanes.insideBand(minZ, maxZ, party)` answers that for one region (both ends in the band of the party, within half a step = 2048 blocks of its lane on its road). Two places ask it:
`PartyRegistry` logs a WARN at every start (while parties are on) for each old sector that is not inside the band of party 0, and the self-test `party.lanes` FAILS (also only while parties are on) when one is not, so a CI
world or a test world shows it at once. Nothing is moved or deleted because of the answer. `PartyLanesTest` pins it.

## 4. Party identity and the FTB Teams bridge

`PartyKey.of(teamId, playerId)`: the team's id, or the player's own id when there is no team or the "team" is the player's personal one (FTB's personal team has the player's id).
The key is the id alone: a name, a colour or the member list change while the party stays the same party.

The bridge is the one the mod already has, `quests/QuestTeams` (reflection on FTB Teams' public API, soft dependency, every failure is "no team"). Two optional methods were added to it
(`teamOfId`: the team of a player given only their id, so an OFFLINE player can be placed; `ownerOf`), resolved apart from the rest so an FTB Teams without them still works.
`PartyResolver` turns a team into a key, caches the answer per server tick, and has a `Source` seam (the self-tests plug in there; so could another team layer).
With `party.useFtbTeams = false`, or without FTB Teams, every player is a party of one.

What was read (the source of FTB Teams, branch `1.21.1/main`, mod version 2101.1.11, fetched into a scratch folder and read, never run; also `main`, which is Minecraft 26.1):

* `FTBTeamsAPI.api()` returns the instance (it may be `null` before FTB's init), `API.isManagerLoaded()`, `API.getManager()`.
* `TeamManager.getTeamForPlayer(ServerPlayer)` delegates to `getTeamForPlayerID(UUID)`, which answers `PlayerTeam.getEffectiveTeam()`: the party team when the player is in one, else
  the player's own `PlayerTeam` (`TeamManagerImpl.java`, `PlayerTeam.java`). `Team.getId()`: the player's UUID for a player team, a random UUID for a party or server team.
  `Team.getOwner()`: the owner's UUID for a party, the nil UUID otherwise. `Team.getMembers()`: the member UUIDs. `Team.getShortName()`: display name, `#`, eight hex digits.
* URLs: `https://raw.githubusercontent.com/FTBTeam/FTB-Teams/1.21.1/main/common/src/main/java/dev/ftb/mods/ftbteams/{api/FTBTeamsAPI,api/Team,api/TeamManager,data/TeamManagerImpl,data/PlayerTeam,data/PartyTeam}.java`.

NOT verified: that the reflection really runs against the FTB Teams jar a server installs (no FTB Teams was installed anywhere here; the method names and signatures match the 1.21.1 source,
and the older bridges that use the same names have shipped), that an older FTB Teams (the 1.20 line) has `getTeamForPlayerID` (it is optional here), and what a SERVER team
(admin-made) does with `getMembers()` in practice: it is treated as a team like a party team. FTB's team events (`PlayerJoinedPartyTeamEvent` ...) are deliberately NOT used: they
need compile-time classes, and nothing needs them (the party is decided at entry).

## 5. The book of lanes (`PartyIndex`, `MegaParties`)

Pure rules, tested in `PartyIndexTest` (15 tests):

* The first party gets index 0 (so on an upgraded world the FIRST party to go in adopts the legacy chain), the next the lowest free index, never twice; a known party keeps its index whatever happens.
* An index is never handed out when floors already use it although no party holds it (`reserved`: a lost book can never give somebody's floors to a stranger). Index 0 is never reserved.
* The book is full at `party.maxParties`: the player is told so and does NOT go in (the only other place is among strangers). Parties that already hold a larger index (the config was lowered) keep it.
* A NEW team may take over the lane of one of its members who explored alone before, the owner first (`party.teamInheritsLane`, default on), and only when that lane is the member's alone;
  the member's own entry is removed, so a player who later leaves the team starts a fresh lane instead of walking back into the team's floors.
* An admin can put a party on any lane (`party set`), even one another party holds: that is how two groups are merged. Nothing else ever undoes it.
* The file stores the key (`t:<uuid>` team / `p:<uuid>` player), the index and a label; a damaged entry is dropped, the rest kept.

**Upgrading a world.** The default `party.mode = "party"` means that after the update the first party to enter adopts the old chain (party 0) and everybody else starts fresh lanes. A
server of friends who played in one shared dungeon has three choices: `party.mode = "shared"` (nothing changes), form an FTB team (a new team takes over the lane of the member who played alone, the owner first: `party.teamInheritsLane`), or put the others on the old chain with `/multiverse mega party set <player> 0` (a merge).

**Switching later.** Setting `party.mode = "shared"` (or `floors.perSector` above 1) on a server that already has parties deletes nothing: the floors of every party and the book stay in the save, the gates lead
everybody to party 0's chain (`PartyService.enter` answers 0 and records 0), the guard is off, and a player who happens to stand on another party's floor can walk on, because the stairs and the next floor
follow the party saved in the floor, not the mode. Switching back to `party` finds the book and the lanes as they were. (Read from the code; not run.)

## 6. Entering, leaving, dying, logging out, and the guard

* **Entering:** `MegaTravel.enterNow` asks the Directional Lock, then `PartyService.enter` (lane, record), then the party's chain: the Landing to offer, or the party's first floor of the road is
  queued on its lane and the player waits (the existing wait, now for the party's chain). A line follows the arrival: "Your party X has floors of its own. n of you are in the dungeon now."
  (or "a party of one"), and the quest hook `mega.party` fires when two or more of the party are inside.
* **Stairs:** the floor a stair leads to is the next floor of the SAME party's chain (`StairTravel` reads the party of the floor the player stands on). A party member who follows uses the stairs themselves.
* **The next floor** is queued when a real player enters a floor (`FloorGrowth`), in the chain of that floor's party; a boss floor's next floor waits for the Crucible, as before.
* **Dying:** unchanged (the origin and the pad are forgotten, the respawn is the ordinary one: bed or world spawn); the record `nusmp_mega_party` stays and is replaced at the next entry.
* **Logging out inside a floor:** the player is saved where they stand and logs back in there (nothing in the mod moves a player at login). Their party is that floor's party; nobody is sent out.
  Offline players hold no chunks: the dungeon uses short tickets only, so an empty lane unloads like any other chunk.
* **The guard** (`PartyGuard`, every second, `party.guard`, default on): a player whose record is not the party of the floor under their feet is told so and sent to the Hub with `LabyrinthTravel.leaveAny`.
  A player with NO record adopts the floor they stand in (an upgraded world, a teleport by command, a login after an update): nobody is thrown out of a floor they were already in. Operators and spectators
  are never moved (`party.opsBypass`). It runs only when parties are instanced. The verdict is the pure `PartyPolicy.verdict`.
* **Changing teams while inside:** nothing happens until the player leaves; the party is decided at the next entry.

## 7. The generator

`ExpansionQueue.tryFirst(server, road, party)` creates the first floor on the party's lane (a road another party opened is no reason to refuse); every later floor continues the chain of the
sector it follows. `SectorPlanner.first/floor(..., party)` and `FloorPlan.entry(road, k, party)` put the floor on the lane; `SectorDraft.partySeed(dungeonSeed, party)` (party 0: the dungeon seed
itself; party k: `Rng.mix(seed, 0x504152540000 + k)`) is folded in before the theme, the relic count and the sector seed are drawn, so two parties never walk the same floors (`PartyPlanTest`: 3 072
different seeds for 256 parties x 2 roads x 6 floors). `WorldObstacles` and `SectorGenerator.obstacles` take a party, so a banded plan never depends on how far another party got.
`mega.maxSectors` is counted per party (both roads together), so a busy server does not stop all parties at once; ids stay global. There is still ONE queue and one job at a time: parties that grow at the same moment wait for each other, in the order they asked.

## 8. The System and the other systems

* `SystemParty.setProvider(PartyService::questMembers)`: a kill credits the killer and every member of their GROUP who is in the same level, alive and within `system.shareRadius`. Inside the dungeon in
  party mode the group is the party of the floors (two parties merged by an admin share credit; strangers cannot be on one floor); otherwise it is the FTB team, so a shared dungeon credits teammates,
  not whoever stands nearby (the SYS2 default, "players in the same sector", is replaced). "No damage", "speed" and "rooms" quests stay personal (SYS2's reasoning).
* The Crucible, the Guardian, the vault and the safe zones are per sector and need no change; `NextTheme` (the previous floor of the chain) and `FloorAssigner` (the length of the road) read the sector's party.
* The Dungeon Master: `PartyMath.Member` carries the party number of the floor a player stands on and `group` never joins two parties, whatever the coordinates say; the budget, noise and light work per table as before.
* The Avatar: `AvatarDelve.target` delves in the chain of its creator's party and never wakes one; a creator whose party has no floors yet gets no trip.

## 9. Configuration (`nusmp-labyrinth-mega-party-server.toml`, section `[party]`)

| key | default | meaning |
|---|---|---|
| `mode` | `"party"` | `"party"`: instanced chains per party. `"shared"`: everybody is party 0 (the 1.57.0 behaviour). A typo reads as `party`. Parties are only separated in the floor model (`floors.perSector = 1`). |
| `maxParties` | 64 (1..256) | the most parties given lanes; the farthest lane z is `300000 + 4096 * (n - 1)`. |
| `useFtbTeams` | true | the FTB Team of a player is their party; false = every player is a party of one. |
| `guard` | true | a player on another party's floor is sent to the Hub within a second. |
| `opsBypass` | true | operators (permission 2) and spectators are never moved. |
| `teamInheritsLane` | true | a new team takes over the lane of a member who explored alone (owner first). |

Plain `define` / `defineInRange` only, read through `get(...)` (the default while the spec is not loaded). Also changed in meaning: `mega.maxSectors` is per party (identical with one party).

## 10. Commands (`/multiverse mega ...`, permission 2)

* `status [party <n>]`: the sectors of one party's chains. Without an argument the console shows party 0 (so every older reader reads what it always read) and a player shows the party of the floor
  they stand on or of their record. When more than party 0 exists a second line says `party: showing party <n> (parties with floors: 0,1,4; status party <n> shows another)`.
* `expand [holy|abyssal] [party <n>]`: grows that party's road. Without an argument the console grows party 0; a player grows the party of the floors they stand on, else their own (handed out now).
* A sector of a party other than 0 ends its line with `party=<n>`; party 0 lines are unchanged. Commands that name a sector by id (`sector`, `regen`, `audit`, `tp`, `unseal`, `plan`, `rooms`, `verify`) need no party.
* `party list`: `party: mode=party instanced=true maxParties=64 known=2 sectors=18`, then `party <index> <team|solo|unowned> key=<uuid|none> name=<name> holy=<n> abyssal=<n> online=<n>` per party
  (`unowned` = a lane with floors that no party in the book holds).
* `party of <player>`: `party of Dev: index=3 kind=solo key=<uuid> name=Dev where=party 3 floor +4` (an offline player works: FTB Teams is asked by id).
* `party tp <index> [holy|abyssal]`: to the first finished floor of a party (an admin looking at it; operators are never sent out).
* `party set <player> <index>`: puts the player's party (their team, if they have one) on a lane; every online member gets the record, and one who stands on another party's floors is sent out.
* Quest hook: `mega.party`, one argument, the number of the party inside the dungeon after an entry (2 or more). Fired once per entry through a gate.

## 11. Files and why each exists

New, `labyrinth/mega/party`: `PartyKey` (the stable id), `PartyIndex` (the book's rules, pure), `PartyLanes` (lane questions, pure), `PartyPolicy` (mode, when parties are instanced, the guard's verdict, the team name; pure), `PartyLines`
(the words the smoke test reads; pure), `PartyConfig`, `MegaParties` (the saved book), `PartyResolver` (the FTB bridge, the per-tick cache, the seam), `PartyService` (the facade), `PartyGuard`, `PartyCommand`,
`PartyText` (message keys), `PartySelfTests`, `PartyRegistry` (the entry point). Tests: `PartyKeyTest`, `PartyIndexTest`, `PartyLanesTest`, `PartyPolicyTest`, `PartyLinesTest`, `gen/PartyPlanTest`, `enc/PartyMathPartyTest`,
`mega/MegaDataPartyTest` (needs Minecraft classes: CI only).

## 12. Proof, and what it does not prove

Run here: `PartyKeyTest` 5, `PartyIndexTest` 15, `PartyLanesTest` 10, `PartyPolicyTest` 8, `PartyLinesTest` 6, `PartyPlanTest` 7, `PartyMathPartyTest` 3, with the local JUnit shim (54 tests); and the old tests of the pure classes
the change reaches, all green with the same shim: `PartyMathTest` 9, `CommandLinesTest` 13, `SectorDraftTest` 7, `SectorDraftFloorTest` 7, `FloorMapTest` 9, `FloorModelTest` 7, `MegaContractTest` 12,
`RoadsTest` 7, `FloorChainTest` 6, `SectorPlannerTest` 15, `WorldGrowthTest` 4, `RegenReplayTest` 3, `AttachTest` 5, `FloorAuditTest` 13, `SectorAuditTest` 13, `FloorAssemblerTest` 9 (139 tests). A mutation check of the pure classes (18 deliberate breakages, each put in, run against the tests and taken out again) was caught 18 times out of 18. The smoke script was run against the stand-in server: the good run passes every check, and four deliberate
faults (a floor of party 1 on party 0's lane, party 1's floors leaking into the plain `status`, a stair that leads into another party's chain, no `party list`) fail exactly the checks they should.
The type check covers every file added or changed (the only errors are the known stub artefacts: `new BlockPos` and packet constructors reported as abstract).

In CI (written, not run): the self-tests `party.*`, `core.*` and `stairs.*` against real floors of two parties, the phase `phase_party`, and `MegaDataPartyTest`.

## 13. Scale (an estimate, not a measurement)

The sectors of every party are in ONE saved file (`nusmp_mega.dat`), as before. A floor's NBT (about 30 rooms, their markers and edges) is several kilobytes; 64 parties with 2 roads of 30 floors each is
about 4 000 floors, tens of megabytes of NBT (a few megabytes on disk, compressed), rewritten as a whole at every autosave. `MegaData.sectorAt` (called by most systems for every player every tick) tries the
last four answers first, so the linear scan over thousands of sectors is the exception, not the rule. A per-party file would be the next step if a server ever really has that many parties.

## 14. Unverified (honest list)

1. **Nothing ran in the game.** All game-side code is type-checked against API stubs and nothing else.
2. **The FTB Teams reflection against a real FTB Teams jar** (section 4).
3. **The world border of the Mega-Dungeon in a real server** (whether a dimension follows the Overworld's border in 1.21.1 is read from memory of `MinecraftServer.createLevels`, not verified); the startup warning depends on `WorldBorder.isWithinBounds(BlockPos)` (used elsewhere in the mod).
4. **A FakePlayer's `teleportTo` inside the same level** (the self-tests read `LabyrinthTravel.arrivalPad`, as `core.travel` does, instead of the position).
5. **Chunk loading far from the origin** (z = -258 048 and 558 048 with 64 parties): the first floor of a second party is the first thing to watch; float precision of the rendering at that distance (0.06 blocks) was reasoned, not seen.
6. **The size and the autosave time of `nusmp_mega.dat` with many parties** (section 13).
7. **`/multiverse mega party set` with a player's profile that is not in the profile cache**: `GameProfileArgument` refuses unknown names, as for `/op`.
8. **The legacy banded model** is only ever shared; a banded chain was never run with parties. The mixed case (old ten-floor sectors as party 0 beside floor-model lanes, section 3) is guarded by a
   startup WARN and a self-test, not by any move of the old sectors; whether a real old world has a banded chain that wandered past 2048 blocks of its lane is not known (the startup log tells).
9. **FTB Teams' server teams** count as parties like party teams; nobody tried it.
10. **The shared lane 0 adoption** ("the first party to enter an upgraded world takes the old chain") is a design call; the alternative is a default of `shared` for worlds that already have floors.

## 15. For the lead

See the report (the registration lines, the config registration, the command attachment, the lang fragment `arc/frag/party.lang.json`, the smoke script's `SMOKE_EXPECT` prefix `party`).

## 16. Sources

* The owner's request and `docs/labyrinth/mega_v2.md` (the contract), `mega_core.md`, `mega_floors.md`, `mega_hub.md`, `mega_v2_gen2.md`, `mega_v2_sys2.md`, `mega_v2_prune.md`.
* FTB Teams source, branch `1.21.1/main` (mod version 2101.1.11) and `main`, URLs in section 4, read as data into `scratchpad/ref/party/`, nothing executed.
* The existing bridges in this repository: `multiverse/Squads.java`, `quests/QuestTeams.java`, `avatar/FtbTeamHelper.java`.
