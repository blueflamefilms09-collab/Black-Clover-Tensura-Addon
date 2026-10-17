# Avatar system, part AV4: the rebuilt entity side (native subordinate, inventory, weapon AI, looting, skill loadout, evolution, party, delving)

The owner's brief for the rebuild ("My Avatar's Path to Greatness", the second brief) is the spec. This part keeps what already met it (the roll, the personalities, the chatter, the three-day outcome engine, the Yandere
jealousy and frenzy, `FtbTeamHelper`, the race data pack, the casting through a body double), replaces what the brief supersedes, and adds what was missing. Everything is server logic except the inventory screen and the arm poses.
**Nothing was run in the game.** What was checked: every pure class has JUnit tests that were run with the repo's JUnit shim (CI runs the real JUnit); every Minecraft-facing class was type-checked against the API stubs
(`tc_wt.sh` over the whole `avatar` package, and `tc_full.sh` over the whole tree with the tests); the only errors left are the known stub artifacts (`INBTSerializable.deserializeNBT` on two entity subclasses, and
`new MobEffectInstance(...)` reported as abstract in six places, two of them new). See "Unverified" before the first run.

Creation, the Soul Forge screen, the ritual, the payloads and the aptitude catalog belong to AVUI and were not touched.

## NEW, REPLACEMENT, KEPT
| Part | Status | Notes |
|---|---|---|
| Base class `TamableAnimal` (a native Tensura subordinate) | REPLACEMENT | `AvatarEntity` extended `PathfinderMob`. Same entity type id, same NBT keys, old saves load. Evidence below. |
| Synched data `CreatorStoredLuck`, `OutcomeTimer`, `HasUniqueOrUltimateSkill` (+ the stance mirror) | NEW | The saved fields stay the truth; the synched values are mirrors (float for the double, once a second for the timer). |
| Stance (Follow / Stay / Delve), `Stance`, `TensuraBond` | NEW | Stay IS the vanilla "ordered to sit" flag, so Tensura's Follow / Stay command and this mod's buttons are the same thing. |
| Inventory: `avatar/inventory/**` (container, 32 slots, menu, screen, orders, command, relic bridge) | NEW | Hands, 4 armour, 8 trinket slots (a view of the relic pack), 18 pockets. Opens by sneak + empty-hand click of the creator. |
| Dynamic weapon AI (`CombatPlanner`, `AvatarCombat`, melee / ranged / mage / shield goals) | NEW | The melee goal replaced the vanilla one in the goal list (kept as the betrayed Avatar's); `avatar.weaponAi` off restores the old behaviour. |
| Looting AI (`AvatarLootGoal`, `LootRules`, `LootClassifier`, `Graves`) | NEW | The Merchant's old "drift relics toward the creator" nudge in `AvatarWatcherGoal` is KEPT, but only runs while `avatar.loot` is off. |
| `VirtualSkillLoadout` (+ `LoadoutPlanner`, `SkillRoles`, `PageTier`) | NEW | `AvatarCastSkillGoal` presses it; with `avatar.loadout` off it runs the 1.51.0 code, kept as `pressClassic`. `SimulatedPlayerCaster` gained several books; its old methods still mean "the creator's book". |
| `AvatarEvolution` (+ `EvolutionRules`) | NEW | Race chain from the data pack, simulated festival, hook on Tensura's festival events, EP from kills. |
| Autonomous naming and the NPC party (`avatar/party/**`) | NEW | |
| Independent delving (`avatar/delve/**`) | NEW | |
| `TensuraNamingInterceptor` | NEW (facade) | The old `AvatarNamingHook` logic moved here; `AvatarNamingHook.register()` is kept and delegates. |
| `TensuraNamingEngine.apply` | REPLACEMENT (fix) | Native naming: the D&D surge is now a floor, the evolution never lowers EP, the bond is restored. The 4-argument form is kept. |
| `LuckMath.looksLikeLuckSkill` | REPLACEMENT (fix) | Tensura's probability lives in `tensura:tuner`; the Tensura source of Total Luck was always zero. |
| Goal start checks `% 4`, `% 20` | REPLACEMENT (fix) | Half of the Avatars never ran them (see "Audit", `TickSlot`). |
| Yandere frenzy price | REPLACEMENT (fix) | It burned CURRENT health; the brief and the config text say MAXIMUM health. The old burn stays as the fallback. |
| Vanilla particles in `AvatarSignatures` and `YandereFrenzyHandler` | REPLACEMENT | Now `VfxSpawn` shapes (the mod's rule since 1.36.0). |
| `PlayerAvatarRenderer` arm poses | NEW | Bow draw, crossbow charge and hold, shield block, held-item pose. The rest of the renderer is untouched. |
| Race JSON, chatter, outcome maths, `FtbTeamHelper`, `YandereJealousyTargetGoal`, `AvatarHuntCreatorGoal`, `BetrayalDimensionHandler` | KEPT | Re-checked against the brief (audit table). |

## Why `TamableAnimal` (the base-class decision, with evidence)
The brief says: "`AvatarEntity` MUST extend Tensura's native subordinate class (e.g. `TensuraCompanionEntity` or implement `ITensuraSubordinate`)". Neither name exists. What the Tensura Reincarnated 2.0.1.x API dump
(signatures only; there is no jar on disk) contains:
* `io.github.manasmods.tensura.entity.template.subclass.ISubordinate extends OwnableEntity`, with default methods `isTame`, `setTame`, `getOwnerUUID`, `setOwnerUUID`, `tame(Player)`, `isOrderedToSit`, `setOrderedToSit`, `getBehaviour`, `getOwnerCommand`, `cycleCommands`, `shouldTarget` ...
* `io.github.manasmods.tensura.mixin.MixinTamableAnimal implements ISubordinate` (with `getOwnerUUID(UUID)`, `isTame(boolean)`, `tame(Player, CallbackInfo)` callbacks). Tensura's own mixin therefore makes EVERY vanilla `TamableAnimal` a subordinate.
* `TensuraTamableEntity extends TamableAnimal implements NeutralMob` (Tensura's own monsters), `TensuraHumanoidEntity extends TensuraTamableEntity` (its native humanoid inventory and menu), `PlayerLikeEntity extends TensuraHumanoidEntity implements RangedAttackMob`.
* `SubordinateHelper` (`isOrderedToStay`, `setStay`, `setFollow`, `setWander`, `setProtect` ...), `IExistence` / `ExistenceStorage` (permanent owner, name, nameable flag, EP, harvest ticks), `TensuraEntityEvents` (`NAMING_EVENT`, `ENTER_HARVEST_FESTIVAL_EVENT`, `HARVEST_FESTIVAL_REWARD_EVENT`, `POST_TAME_EVENT`).
* No `TensuraCompanionEntity`, no `ITensuraSubordinate`, no `calculateCompanionEP` anywhere in the dump (searched for "companion": no hits).

Options weighed: (1) `TamableAnimal` (chosen): the Avatar is a subordinate exactly as Tensura's monsters are, by the same inheritance, with the smallest change (a `Mob` subclass either way, same goal system, same
renderer, same NBT). (2) `TensuraHumanoidEntity` / `PlayerLikeEntity`: native humanoid inventory and bow AI, but they run on SmartBrainLib brains, their menu is sized by Tensura's own packets, and their `mobInteract` and command
handling would fight this mod's own; none of it can be checked without the game, and the Avatar's goals, outcome engine and casting double would all have to be re-seated. Rejected. (3) Only implementing `ISubordinate`
on a `PathfinderMob`: its defaults assume a `TamableAnimal` underneath (the mixin supplies the storage); rejected.

What follows from it: `isTame()` is true exactly while the Avatar is loyal; `getOwnerUUID()` is the creator while loyal and null after a betrayal or an awakening (so the vanilla team and ally rules, Tensura's checks and the
death message all follow); the sit flag is Tensura's Stay (`TensuraBond.order` sets the flag and calls `SubordinateHelper.setStay/setFollow`, and `stance()` is derived from the flag so a Tensura Follow command cancels the
Avatar's Stay); Tensura's permanent-owner record in `ExistenceStorage` is kept in step once a second (`TensuraBond.sync`: creator while loyal, cleared by betrayal and awakening, restored by a naming). Feeding, breeding, love
mode and being a child are switched off (a command-spawned Avatar would otherwise be a baby one time in twenty). Tensura's other behaviours (Wander, Protect, Neutral, Passive, Aggressive) are its own flags; the Avatar does
not act on them.

## Classes and why they exist
### avatar (root)
| Class | Why |
|---|---|
| `Stance` | The creator's order in one small saved enum (Follow default, Stay, Delve). Pure. |
| `TensuraBond` | Keeps Tensura's owner record and Stay state in step with what the Avatar is. Every Tensura call is guarded; `avatar.tensuraBond` switches it off. |
| `TensuraBridge` (extended) | Owner read / write, ordered-to-stay, `isTensuraMob`, `nameable`, `addBaseEp`, `ep`, `maxMagicule`: one place for the Tensura storage calls. |
| `AvatarEntity` (rebuilt) | Tamable base, synched mirrors, stance, inventory, combat style, drops, shield wear, arrow supply, saved keys `Stance` and `Pockets`. |
| `AvatarAiConfig` (extended) | The new keys (below). |
### avatar/ai
| Class | Why |
|---|---|
| `WeaponKind`, `CombatStyle`, `CombatPlanner` | The decision table of the weapon AI: the style from the two hands, the distance band (Cowardly farther, Aggressive closer), the strafing hysteresis, the shield cycle (30 of 50 ticks), the bow draw and the shot interval, the spread from DEX, the arrow damage scale. Pure, tested. |
| `AvatarCombat` | Reads the hands into a style, applies the band and the stay leash, looses arrows and crossbow bolts. The one place the goals share. |
| `AvatarMeleeGoal` | The melee goal, only while the style is MELEE, never from behind a raised shield, never beyond a stay leash. |
| `AvatarRangedGoal` | Bow and crossbow: the vanilla goals need a `Monster`; this does the same job for the Avatar (approach to the band, strafe, back off, draw a full second, load and fire). |
| `AvatarMageGoal` | A grimoire, a charter grimoire or a staff in hand: keeps the band, faces the target, strikes back only when cornered. The cast goal does the casting. |
| `AvatarShieldGoal` | Raises a held shield against a near target; no movement flag. |
| `AvatarStrollGoal` | The vanilla stroll, but not when told to stay or on a trip. |
| `AvatarCastSkillGoal` (rewritten) | Presses the loadout; the 1.51.0 behaviour is `pressClassic`, used with `avatar.loadout = false`. |
| `LootRules`, `LootClassifier`, `AvatarLootGoal`, `Graves` | See "Looting". |
| `TickSlot`, `AvatarGates.slot` | A goal's start check runs on every other tick, with a parity that depends on the entity; `tickCount % 20 == 0` is true for half of the Avatars and never for the rest. `TickSlot.on` divides by two first. |
| `AvatarWatcherGoal` (extended) | Refreshes the combat style when a hand changes and every two seconds, enforces the limits of a dungeon trip, and no longer nudges relics toward the creator while looting is on. |
| `AvatarAi` (extended) | Installs the new goals and registers the new events and the inventory menu type. |
| `AvatarAiEvents` (extended) | Party members never pick or hurt what their Avatar may not. |
| `AvatarMind` (extended) | `lastEngagedAt`: a long silence means a new fight (a buff goes first). |
### avatar/inventory
| Class | Why |
|---|---|
| `InventoryLayout` | 32 indices (hands 0-1, armour 2-5, trinkets 6-13, pockets 14-31) and every pixel position; the save format and the screen cannot drift. Pure, tested (no overlap, everything fits). |
| `GearScore` | Is this armour better, is this weapon better (damage, enchantments, wear)? Pure. |
| `AvatarInventory` | The `Container` over the three stores: the entity's own six equipment slots (so combat, the renderer and the vanilla save see the same stacks), the trinket view, the pockets (saved under `Pockets`). |
| `AvatarTrinkets`, `AvatarRelicBridge` | The Curios-like slots are a VIEW of the relic pack that `MobRelics` keeps for a named mob, so relics in them really work, can be taken back with the old gesture, and are dropped by the old death drop. The bridge repeats `MobRelics`' two private persistent-data keys; `MobRelicsKeysTest` reads both sources and fails if they ever differ. |
| `AvatarGear` | Reads stacks (weapon kind, armour stats, focus), takes things in (better armour / weapon is worn, the old piece goes to a pocket), supplies arrows, hands valuables over, drops everything on death. |
| `AvatarInventoryMenu`, `AvatarInventoryMenus`, `AvatarOrders` | The menu (vanilla menu-button packet for the four buttons, three synced numbers), its type registration (self-registering, see "Integration"), the orders shared by the buttons and the command. |
| `AvatarInventoryEvents` | Sneak + empty hand opens the window, ahead of the relic system's take-back (which the trinket slots replace). |
| `AvatarOrdersCommand` | `/avatar follow|stay|delve|recall|share [name]`, `/avatar party`, `/avatar release [name]`: the creator's own command (no operator rights), for an Avatar that is not within reach. |
| `client/AvatarInventoryScreen`, `client/AvatarInventoryClient` | The window (anime panels when that theme is on, plain otherwise) and its registration, loaded only on a client. |
### avatar/cast
| Class | Why |
|---|---|
| `LoadoutPlanner` | Pure: the ordered loadout (ULTIMATE, UNIQUE, PERK, aptitude skills, aptitude pages, creator pages, Tensura magic) and which ready entry to press (heal below half health, a buff when the fight begins, else the best attack by rank, range, size of the foe and personality). |
| `SkillRoles` | Pure: attack / buff / heal / never-press by a skill's registry path, and the base cooldown by role. |
| `PageTier` | Pure: how many pages of an aptitude's book are open for an Avatar with this EP and INT. |
| `VirtualSkillLoadout` | Builds the loadout from the entity's facts, teaches an exact skill through ManasCore when first used, presses through `TensuraCaster` (press, hold, release), pays from the Avatar's pool, starts the cooldown. |
| `SimulatedPlayerCaster` (extended) | Several books: one per Magic Aptitude (the aptitude's `magic` -> `MagicType.lookup` -> `NUSkills.bookFor`), then the creator's. A page is still only cast when `AvatarSkillList` allows it. |
### avatar/evolution, party, delve, naming
| Class | Why |
|---|---|
| `EvolutionRules` | Pure: EP needed for a race, when the check is due, the chain of a race, EP per kill (capped at 2% of the Avatar's own at once, nothing for weak victims), the festival clock, the "never weaker" factor. |
| `AvatarEvolution` | The evolution, the Harvest Festival hooks, EP from kills. |
| `PartyRules`, `NameGen` | Pure: when an Avatar may name, which monster fits, what it costs, the party tax, a syllable name generator that always passes `NameRules`. |
| `AvatarPartyManager`, `PartyNaming`, `AutonomousNamingGoal` | The party record (SavedData `nusmp_avatar_party`), the act of naming, the goal that walks up and does it. |
| `DelveRules` | Pure: every limit of a trip, the depth, the walk through the floor's room graph. |
| `AvatarDelve`, `AvatarDelveGoal` | The trip: going in, the limits, coming back; the walking. |
| `TensuraNamingInterceptor` | The brief's interceptor of Tensura's `NAMING_EVENT`. |

## How each part works
### Weapon AI
The watcher reads both hands into a `CombatStyle` as soon as a hand changes (`setItemSlot` marks it stale) and every two seconds: a bow or crossbow with ammunition -> BOW / CROSSBOW; a focus (grimoire, charter grimoire, summons
grimoire, an item in the `nusmp:avatar_magic_focus` tag, or one whose id has the word staff / wand / scepter / tome / spellbook / codex) in either hand and something to cast -> MAGE; everything else -> MELEE. The goals only run in
their own style, so they can never fight over the legs. Ranged: the band is `avatar.rangedMinRange` .. `rangedMaxRange` (6 and 18), a crossbow a little closer, a mage in between, Cowardly +3/+2, Aggressive -3/-4; it backs off inside the near
edge, strafes inside the band, draws a bow for 20 ticks, loads a crossbow for its enchanted charge time, shoots through `ProjectileUtil.getMobArrow` with a spread from DEX and damage scaled by the Avatar's attack (1x to 4x).
Ammunition: free by default; `avatar.consumeAmmo` takes arrows from the pockets or the off hand. Shield: up for 30 of every 50 ticks while the target is within 5.5 blocks (staggered by entity id); the melee goal does not swing while it is
up; the shield wears like a player's. Stay: the Avatar fights within 8 blocks of the spot where it was told to stay, shoots and casts from there, and does not walk, follow or loot beyond 2.5 blocks.
### Inventory
Sneak + empty-hand click of the creator opens the window. Armour slots take only armour of their kind (`Equipable`), trinket slots only relics, hands and pockets anything (the off hand may carry the arrows). Relics: a relic that needs
attunement is attuned to the Avatar when it goes in (three at most, like anybody), the bond is cut when it comes out (also by shift-click), and the relic system's "this mob carries relics" marker is kept true while one is held. Death: every
stack the Avatar carries is dropped where it fell (`avatar.dropGearOnDeath`, default true) or handed to the creator when that is off (overflow at their feet, at the death spot if they are offline); the vanilla equipment drop and the relic
pack are emptied first so nothing drops twice. Auto-equip: better armour or a better weapon of the kind it holds is worn when picked up (the old piece goes to a pocket; no swap without room); `avatar.autoEquip`.
### Looting
Every `avatar.lootScanTicks` the goal looks at the items within `avatar.lootRadius` (a Merchant: RARE drops within `merchantLootRadius`), at most 40, and picks by `LootRules.priority`: an armour upgrade +2, a valuable +1.5, rarity RARE and up
+1 and more, a stack of valuables +1.5, a relic or magic stone +1, divided by 1 + 0.08 per block, times 2.5 for a Merchant's rare drop. Never what a player threw less than `lootPlayerGraceTicks` (60) ago, never what lies within six blocks of a
place where a player died in the last five minutes (`Graves`: a death drop has no thrower, so the 3-second rule alone would let an Avatar loot a dead player's pile), never an item with pickup delay, never into full pockets unless it is an upgrade or a rare find, and an item it
failed to take is left alone for ten seconds. A following Avatar stays within 20 blocks of its creator (a Merchant 40 for a rare drop); on a trip there is no leash. With the pockets nearly full and the creator within 8 blocks it hands the
valuables over (`avatar.lootShareWithCreator`), or on the Share button.
### Skill loadout
Entries, in this order: the Soul Forge **Ultimate** (`chosenUltimate`), the **Unique** (`chosenUnique`), the awakened **perk**'s move, the Tensura skills of each Magic Aptitude (`Aptitude.skills`), the attack pages of each aptitude's
grimoire (fresh book, pages opened by EP and INT, `PageTier`), the creator's own grimoire, and last one allowlisted Tensura magic (the 1.51.0 fallback; one time in five it comes first when only page entries are ready). A skill's
role comes from `SkillRoles` (screens, crafting, camera and teleport skills are never pressed). Pressing: the exact skill is learned through ManasCore if the Avatar does not know it, Tensura's pool is topped up, `TensuraCaster.cast` presses,
holds 8 ticks and releases (a skill that throws is dropped for good, the next entry is tried, up to three per press); the price is `avatar.loadoutSkillCostPercent` of the Avatar's pool (twice for an Ultimate), free in a Yandere
frenzy; the cooldown is the role's base (Ultimate attack 400 ticks, other attack 160, buff 1200, heal 600) scaled by INT / WIS / personality / frenzy like a page. Heal below half health, a buff when the fight begins, the big skills only
against a foe of 60+ max health or when the Avatar itself is below half. **Decision for the owner:** the skills a creator picks in the Soul Forge do NOT by themselves count as a secured Special Skill: `AvatarOutcomes.pollUniqueSkill` skips the
loadout's own skills, so only the Instant Skill Awakening, a naming, or a skill another system gave bypasses the three-day roll (otherwise the first fight would silently remove the risk).
### Evolution
Each `avatar.evolutionCheckSeconds` the Avatar compares its EP with `avatar.evolutionEpFactor` (0.6) of the next race's base EP (`evolves_to_race_id` in the Avatar race JSON). When it is due (and `avatar.evolutionCooldownSeconds` have
passed) it goes through a Harvest Festival of its own: it stands still and shining for `avatar.festivalSeconds`, hard to hurt, then changes race, look and attributes (`AvatarEvolution.evolve`, the same code naming uses; the EP never ends below what it
was). The festival is **simulated**: Tensura's evolution tree runs on players through ManasCore's race system and cannot be applied to a mob, and `ExistenceStorage.enterHarvestFestival` was not written for a mob namer. What is hooked is
Tensura's own festival: when `ENTER_HARVEST_FESTIVAL_EVENT` fires for an Avatar (for example because `applySubordinateHarvestFestival` reached it) the Avatar waits, and `HARVEST_FESTIVAL_REWARD_EVENT` makes it evolve one step. EP also grows by kills: 4% of a
victim's EP (`avatar.killEpShare`), nothing from victims under 10% of the Avatar's own EP, never more than 2% of its EP at once, not from players, Avatars, villagers, pets or party members.
### Party
A loyal, calm Avatar with room in the party (`avatar.partyMax`, 3), the cooldown over (`autoNamingCooldownSeconds`, 300), and at least 80% of its pool, scans every three seconds within `autoNamingRadius` (12) for a Tensura monster that is wild (no
owner, no name, nothing Tensura bound, `nameable`), not a boss, villager, player or Avatar, not fighting, with EP between 10% and 120% of the Avatar's. It walks up, pays 25% of its pool, names it (`NameGen`), gives it the naming EP surge, capped
at 90% of the Avatar's EP, writes Tensura's name and owner record (the Avatar is its master), marks it (`nusmp_party_of` in its persistent data, the truth) and records it. Each member costs the Avatar `avatar.partyTaxPercent` (5%) of its
maximum magicule for as long as it lives (one transient modifier, at most half in all). Once a second the party is looked after: dead or vanished members are dropped, a member that fell 28 blocks behind is brought back, it gets the Avatar's
target, and any target the Avatar may not attack (the creator, the creator's friends and pets, the party) is taken away; events cancel a member's hits and target choices against such targets at once. A betrayal or the Avatar's death releases the whole party (the
mark and Tensura's record are cleared, the monsters stay named). `/avatar party` lists, `/avatar release` frees.
### Delving
Stance Delve + `avatar.delve` + a healthy (70%), rested (`delveRestMinutes`, 3) loyal Avatar + a creator in the same dimension + a Mega-Dungeon with a finished sector on the configured road (`delveRoad`, holy) -> two seconds of a space-portal
effect, then a dimension transition (the same call the betrayal tracker uses) to the arrival cell of the floor the creator last reached on that road (their `nusmp_mega_landing_<road>` memory), never deeper than `avatar.delveMaxChain` (chain index 4, the
fifth sector or floor) and never waking the dungeon itself. The trip is recorded in the Avatar's persistent data (so the new instance the transition creates, or the Avatar after a restart, goes on with it). On the floor it walks the room graph
(`DelveRules.nextRoom`: the nearest room not yet seen), attacks monsters within its strength (an ordinary monster up to 3x its EP, a boss only when no stronger than it), and the looting goal takes the drops. The limits, checked once a second by the
watcher even in the middle of a fight: below `delveReturnHealth` (35%), pockets full, `delveMinutes` (10) up, something stronger near, the stance no longer Delve (`/avatar recall` or the Follow button) or the creator in the same
dimension, the creator offline, the switch or the dimension gone. It then returns beside its creator (or to the spot it left from when the creator is away), hands the valuables over and tells the creator what happened.
The Avatar does not walk through the Gateway or use stairs (the stairs move players only); it opens its own way. The Directional Lock is a rule for players and is not asked.
### Naming review
`TensuraNamingInterceptor` (Tensura's `NAMING_EVENT`): an invalid or duplicate name is refused before Tensura spends anything; the Avatar's EP from before Tensura's own gain is remembered; the same tick `TensuraNamingEngine.apply(..., false, epBefore)`
adds the Soul-Bond Lock (cancels the roll, cures a betrayal, restores Tensura's owner record), the evolution by JSON (never lowering EP), the D&D surge as a floor (the Avatar ends with the larger of Tensura's gain and the surge on its old EP, never both added), the creator's tax and sleep.
`INameEvolution` is deliberately not implemented (its defaults are unknown and would evolve the Avatar a second time). **Native EP sharing:** `calculateCompanionEP` does not exist; the Avatar's EP lives in Tensura's `ExistenceStorage`
(`EnergyHelper`), written from the D&D scores at creation and evolution, so Tensura reads it natively.

## Config (own spec `nusmp-avatar-ai-server.toml`, section `avatar`; already registered in `NUSMP.java`)
New keys of the rebuild (the older chatter / cast / Yandere keys are unchanged except `frenzyHealthBurnPerCast`, whose text now says what it does):
| Key | Default | Meaning |
|---|---|---|
| `dropGearOnDeath` | true | Drop everything on death; false = the gear goes to the creator. |
| `autoEquip` | true | Wear better armour / weapon when picked up. |
| `consumeAmmo` | false | Bow / crossbow use arrows from the pockets (false = free arrows). |
| `tensuraBond` | true | Keep Tensura's owner record in step with loyalty. |
| `weaponAi` | true | false = melee whatever it holds. |
| `rangedMinRange` / `rangedMaxRange` | 6 / 18 | The bow band (2-20 / 6-28). |
| `shieldAi` | true | Raise a held shield against near targets. |
| `loot` | true | Pick up items. |
| `lootRadius` / `lootScanTicks` | 8 / 20 | How far / how often it looks. |
| `merchantLootRadius` | 24 | A Merchant sees RARE drops this far. |
| `lootPlayerGraceTicks` | 60 | Items a player dropped are safe this long. |
| `lootShareWithCreator` | true | Hand valuables to the creator. |
| `loadout` | true | The Soul Forge skills, aptitude books and the perk are pressed (false = the 1.51.0 casting). |
| `loadoutSkillCostPercent` | 8.0 | Pool share a pressed Tensura skill costs (x2 for an Ultimate). |
| `evolution` | true | Race evolution by EP and by Tensura's festival. |
| `evolutionEpFactor` | 0.6 | Share of the next race's base EP needed. |
| `evolutionCheckSeconds` / `evolutionCooldownSeconds` | 10 / 300 | Check interval / rest after an evolution. |
| `festivalSeconds` | 20 | Length of the Avatar's own festival. |
| `killEpShare` | 0.04 | EP share gained per kill (0 = off). |
| `autoNaming` | true | Avatars name wild monsters. |
| `partyMax` | 3 | Members per Avatar. |
| `autoNamingCooldownSeconds` / `autoNamingMinMagicule` / `autoNamingCostShare` / `autoNamingRadius` | 300 / 0.8 / 0.25 / 12 | Cooldown, pool needed, pool paid, search radius. |
| `partyTaxPercent` | 5.0 | Max-magicule share per living member (at most half in all). |
| `delve` | true | Trips into the Mega-Dungeon. |
| `delveMinutes` / `delveRestMinutes` | 10 / 3 | Longest trip / rest between trips. |
| `delveReturnHealth` / `delveMinHealth` | 0.35 / 0.7 | Turn back below / set out only above. |
| `delveMaxChain` | 4 | Deepest chain index (0 = first). |
| `delveRoad` | "holy" | "holy" or "abyssal". |

## Commands
`/avatar follow|stay|delve|recall|share [name]`, `/avatar party`, `/avatar release [name]` (no operator rights; only the caller's loaded Avatars). The operator command `/nusmp avatar ...` is unchanged.
Item tag: `nusmp:avatar_magic_focus` (data pack: add staves, wands, tomes of other mods; empty by default).

## Audit of the brief (sections 2, 3, 4, 5, 6, 8, 9)
| Brief | State | Evidence / what was done |
|---|---|---|
| 2 Functional inventory (hands, armour, Curios slots) | DONE | `avatar/inventory/**`. "Curios slots" = a Curios-like container (Curios is not a dependency); it is a view of the relic pack so relics work. |
| 2 Dynamic weapon AI | DONE | Bow / crossbow -> ranged, melee weapons -> melee, grimoire / staff -> mage, shield, re-read when a hand changes, personality still shifts the band. |
| 2 Looting AI, Merchant paths to rare drops | DONE | Real pickup, upgrade auto-equip, grace period and graves, bounded scans. |
| 2 Virtual Skill Loadout | DONE | Ultimate, Unique, aptitudes (skills and books), perk, creator's book; with cooldowns and costs. A skill that assumes a player caster is dropped by `TensuraCaster` at run time (unverified which). |
| 3 Evolution | PARTIAL, honest | JSON race chain + simulated festival + hook on Tensura's festival events. Tensura's player evolution tree itself cannot be applied to a mob. |
| 3 Autonomous naming and NPC party | DONE | Subordinate behaviour of the members depends on Tensura's AI accepting an Avatar as master (unverified); the safety net does not (teleport, shared target, cancelled hits). |
| 3 Independent delving | DONE with a simplification | The Avatar opens its own way instead of walking through the Gateway / using stairs; one floor per trip. |
| 4 Synched data: `CreatorStoredLuck`, `PrimaryPersonality`, `SecondaryPersonality`, `MagicAptitudes`, `OutcomeTimer`, `AppearanceData`, `HasUniqueOrUltimateSkill`, `IsFrenzied` | FIXED | `CreatorStoredLuck`, `OutcomeTimer` and `HasUniqueOrUltimateSkill` were saved fields only; they are now mirrored as synched data (a float for the double, the timer once a second) and read from it on the client. The other five were synched already. |
| 4 Native naming override: D&D surge, JSON evolution, Soul-Bond Lock | FIXED | The native path had no surge and an evolution reset the EP below Tensura's gain; see "Naming review". |
| 4 Native EP sharing `calculateCompanionEP` | NOT POSSIBLE as written | The method does not exist. EP is stored in Tensura's own `ExistenceStorage`. |
| 5 Stat polling: vanilla LUCK, Tensura Probability, Fate Magic, Dice Magic | FIXED | The Tensura source was always zero: its probability manipulation is the Unique skill `tensura:tuner` ("Change your fate ... manipulate probability" in the 2.0.1.x skill list), which none of the three searched words matched; `LuckMath.looksLikeLuckSkill` now matches it. The fate and dice sources were fine. |
| 5 Luck > 10 -> 5d6 drop 2 | OK | `StatRoller.usesFiveDice(luck, luckBonusThreshold)`, config default 10. |
| 5 Instant Skill Awakening | AVUI | The roll belongs to `AvatarCreation.finalizeAvatar` (AVUI). The outcome engine already honours `HasUniqueOrUltimateSkill`. |
| 6 Fourteen personalities | OK | `AvatarPersonality` has exactly LOYAL .. STOIC in the brief's order (constants only appended). |
| 6 Chatter within 30 blocks, hybrid mixing | OK | `chatterRadius` default 30, `HybridMixer`. |
| 8 `FtbTeamHelper` soft dependency | OK | Goes through `QuestTeams` / `Squads` (reflection), answers false without FTB Teams. |
| 8 Jealousy: 5 blocks, body-blocks | OK | `yandereJealousRadius` default 5; intercept share 70%. |
| 8 Frenzy handler on `LivingDamageEvent.Post` | OK | `AvatarAiEvents.onDamagePost`. |
| 8 Frenzy casting: no mana, 90% CDR, drains Max Health | FIXED | No mana and 90% CDR were right; the price was CURRENT health. Now the maximum drops per cast (transient modifier, never below 1, returned when the frenzy ends). |
| 8 Vanilla particles | FIXED | Replaced by `VfxSpawn` shapes. |
| 9 72,000 ticks; unnamed and no Unique skill | OK | `outcomeTicks` 72000; `OutcomeMath.decide`. |
| 9 Betrayal: severs the Tensura bond, hunts across dimensions | FIXED (bond) | The bond is cleared in `AvatarOutcomes.apply` via `TensuraBond.sync`; the hunt goal and `DimensionTransition` existed. |
| 9 Soul Awakening: "custom Unique Skill via Tensura's API" | PARTIAL, by design | The skill is data (`AvatarUniqueSkill.Perk`: name, attribute, signature move); a skill cannot be added to Tensura's registry at run time. It is now pressed by the loadout. |
| 9 Ordinary: "remains a standard Tensura subordinate" | FIXED | True now by inheritance and by the bond. |
| (found) goal start checks on half of the Avatars | FIXED | `Retreat`, `EatDroppedFood`, `DefendVillagers` used `tickCount % 4 / % 20`. |

## Tests (`src/test/java/.../avatar/{ai,cast,delve,evolution,inventory,party}` and additions)
`CombatPlannerTest`, `LootRulesTest`, `TickSlotTest`, `GearScoreTest`, `InventoryLayoutTest`, `MobRelicsKeysTest`, `LoadoutTest`, `EvolutionRulesTest`, `PartyRulesTest`, `DelveRulesTest`, and additions to `LuckMathTest`,
`YandereTest`, `NamingTest`. The 13 classes of this part (124 tests) were run with the shim, all green (`AvatarCastTest` and `OutcomeMathTest`, unchanged, were green in an earlier run). Mutation check: `TickSlot.on` replaced by `tickCount % period == 0` (the bug it fixes): `TickSlotTest` fails; restored, green.

## Unverified (read these first when it is first run)
* Nothing ran. The base-class change is the biggest risk: Tensura's `MixinTamableAnimal` code was never executed against an Avatar. If Avatars misbehave on load, `AvatarEntity` is the one class to look at (it can be put back on `PathfinderMob` by
  reverting the parent and the five overrides under "the parts of Animal"; the rest keeps working).
* `SubordinateHelper.setStay/setFollow/isOrderedToStay`, `IExistence.get/setPermanentOwner` on a mob Tensura does not track: guarded by try / catch, behaviour unseen.
* Party members: Tensura's own AI for a monster whose master is an Avatar (not a player) is unknown; `TamableAnimal.getOwner()` is null for them (it looks players up), so their follow / defend relies on the manager's per-second pass.
* `ExistenceStorage.applySubordinateHarvestFestival` reaching an Avatar, and the event argument order of `ENTER_HARVEST_FESTIVAL_EVENT` (the two `Changeable<Integer>` are ignored).
* Tensura skills as Avatar skills: the existing `TensuraCaster` approach for mobs; a skill that needs a `Player` is dropped on its first failure. Aptitude books are fresh instances (no creator mastery), so a page's strength may differ from the creator's.
* Delving: that `Sector.arrival` is a standable cell on the floor the generator builds in the parallel rework, that navigation from room centre to room centre works through its doorways and closed doors, and that a loaded-by-ticket dungeon chunk
  keeps the Dungeon Master's spawns off (they key on players). The Avatar's chunk tickets are the vanilla portal ticket (300 ticks), renewed every four seconds by the watcher; the arrival chunk is loaded with `getChunk` from a goal tick, like `LabyrinthTravel.arrive` does.
* The screen: layout, the portrait scale (28), the bracket on the chosen stance, the tooltip order; the arm poses against the model; the six `VfxShape` choices (`WIND_RING`, `WIND_SLASH`, `FIRE_BURST`, `SPIRIT_AURA`, `SPACE_PORTAL`, `MAGIC_CIRCLE`) and their `power` meaning, chosen from the shape comments only.
* `ModLoadingContext.get().getActiveContainer().getEventBus()` during the mod constructor (how the menu type registers itself); the explicit line in the integration notes does the same thing.
* Sneak-click: this mod's listener runs at HIGH priority ahead of `MobRelics` (NORMAL); the relics' take-back for an Avatar now happens in the trinket slots.
* The shim's JUnit subset is smaller than the real one; CI runs the real JUnit.

## Integration notes (for the lead)
* `NUSMP.java`: nothing is required. `AvatarAiConfig` was already registered. The menu type registers itself from `AvatarAi.registerEvents`; an explicit, harmless second call is
  `com.newuniverse.nusmp.avatar.inventory.AvatarInventoryMenus.init(modEventBus);` after `AvatarSystem.init(...)`.
* Lang: merge `arc/frag/avai.lang.json` (five `nusmp.avatar.inv.*` keys) into `en_us.json`.
* Version, README / LIBRARIES jar name and CHANGELOG are the lead's.
* For the builders: `AvatarEntity.mobInteract` is public now (the animal parent requires it); the entity exposes `STORED_LUCK`, `OUTCOME_TIMER`, `SPECIAL`, `STANCE` accessors, `stance()`, `setStance`, `inventory()`, `combatStyle()`; the Avatar reads the Mega-Dungeon only through
  `MegaDungeon.level / in / isDungeonMonster`, `MegaData.get / chain / chainSector / sector`, `Sector.arrival / rooms / edges / state / chainIndex / firstFloor()` and `Room.center() / kind.isSafe()`; the creator's depth is the persistent int
  `nusmp_mega_landing_holy` / `_abyssal` (read as plain strings).

## Sources
Tensura Reincarnated 2.0.1.x public API signatures (the repo's committed API dump, used for every class named above) and the skill id list of the same version (`tensura:tuner` and its description); the Minecraft 1.21.1 and NeoForge 21.1 API stubs of
`tools/typecheck/stubgen.py` for every vanilla signature (`ProjectileUtil.getMobArrow`, `CrossbowItem.performShooting`, `ServerPlayer.openMenu(MenuProvider, Consumer)`, `InventoryScreen.renderEntityInInventoryFollowsMouse` ...). No web source was used.
