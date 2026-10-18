# Hero weapons, the core (builder hcore)

Written for 1.62.0. **Nothing in this document was run in the game.** The build machine has no compiler for the whole project, so "it works" below always means "it type-checks against the API stubs and its pure part is unit-tested"; section 17 lists what that leaves open. "The design" means `docs/heroweapon/design.md`, the binding contract of the job.

## 1. What the core is, and what it is not

Why: the design fixed the shape of the system (21 weapons, one bond per player, forms, the Curse Series, the Calling Gem) and left the machinery that turns a bond into a real weapon in a real hand and keeps the owner's three restrictions true. The core is that machinery. It knows nothing about any single weapon (what a Hammer does is the Hammer builder's), nothing about what a form is (the forms builder), nothing about the curse (the curse builder) and nothing about how a player first gets a weapon (the gem builder). Its one rule: everything that matters is decided on the server. The client sends key presses and draws what the server last told it.

| | what |
|---|---|
| NEW | config `nusmp-heroweapon-server.toml` and `nusmp-heroweapon-client.toml`; the world register `nusmp_hero_registry.dat`; the loan glue (summon, dismiss, restore, shutdown, adoption, the container and cursor checks); the four payloads and the client copy of the bond; the dispatcher's cooldown book, payment receipt and key handling; the three restrictions and the experience rule; the HUD; the five keys; the tooltip; the base-stats and colour tables; the item tag `#nusmp:hero_allowed`; three self-tests (`hero.bond`, `hero.use`, `hero.rules`); 10 unit-test classes (56 tests) |
| REPLACEMENT | the contract's stubs, with every public signature kept: `HeroWeaponItem`, `HeroItems` (registers the real item class), `HeroSummon` (now the door to the real loan, plus `stack(player)`), `HeroUse`, `HeroInterference`, `HeroXp`, `HeroCommand` (core nodes added, the forms, curse and gem nodes kept), `HeroCore`, `HeroClient` (the forms and curse calls kept) |
| KEPT | everything else. The Djinn weapons' `WeaponLoan` is reused as it is; `MagicWeaponItem`, the Magebane Shield, `HeroKind`, `HeroBond(s)`, `HeroCtx`, `HeroForm`, `HeroSkill`, `HeroLogic(s)`, `HeroLevels`, `HeroComponents` are not touched. Three shared files got one small edit each (the renderer hook, the class-lookup suffix in Java and in its Python mirror; section 9) |

## 2. The bond and the register

Why: "one holder per Holy Weapon" has to hold across logouts (the holder may be offline), and the system has to be switchable, so both are gates on `HeroBonds.bind`.

* `HeroBondEvents.register` adds one gate and one listener to `HeroBonds`.
  * The gate refuses when `enabled` is off, when `maxBonds` is 0, and when a unique weapon already has another holder (`holyUnique` for Shield, Sword, Spear and Bow; `vassalUnique` for the other seventeen). The refusal is a short English sentence returned to the caller (the commands and the gem's ritual show it).
  * After a bind the listener claims the weapon in the register, fires the quest hook `hero.bound`, asks the forms system to `refresh`, and sends the client its copy. After a release it ends the loan (the weapon vanishes for good), clears the cooldowns, frees the register entry, tells the client it has no weapon, and, one tick later (the listeners run before the bond is cleared), asks the forms system to `refresh` so it can take its modifiers off.
* The register (`HeroRegistry`, the saved-data file `nusmp_hero_registry.dat` in the Overworld's data storage) maps a weapon id to the uuids that hold it. Its rules (claim, release, "release everything except", sweep, "who else holds it") are the pure `HeroRegistryData`. It heals itself: at login, respawn and dimension change every bonded player re-claims their own weapon and drops any other claim (so a world made before the register existed fills as its players return); a claim held by an online player whose bond no longer says so is swept the moment it blocks someone. The way out when a holder's data is gone for good is `/multiverse hero registry clear <weapon>`.
* The bond holds exactly one weapon, so `maxBonds` can only be 0 (close the door) or 1. The range is 0..1; a larger value would be a lie.

## 3. The loan: the weapon is a projection of the bond

Why: the owner wants the weapon "permanently bound to the user's body", but a Minecraft item can be dropped, put in a chest, handed over, burnt and lost. The answer is the Djinn weapons' answer (already built and tested with random events): the bond is the truth, the item in the hand is a projection of it.

* **State.** The pure `WeaponLoan<ItemStack>` machine of the Djinn weapons does the slot work (which hotbar slot is home, which item it displaced, the stash of items that found no room). The core's record of it is `bond.extra("loan")`: `home` (int), `moved` (int) and `movedItem` (item NBT), `stash` (list of item NBT). The bond's own `summoned` flag says whether the weapon is out. The loan reads 42 places: the 36 main slots, the off hand, the four armour slots and the cursor.
* **The projection** is a `HeroWeaponItem` stack with a non-empty `nusmp:hero_owner` (the owner's uuid) and `nusmp:hero_look` (`curse` when the active form says so), engraved once through `WeaponEngravings.engrave` if the lead's table has lines for it. A stack with no owner (creative tab, `/give`) is "bare".
* **Doors.** `HeroSummon.summon/dismiss/isSummoned/stack` for everyone else; `HeroLoan.summon/dismiss(player, explain)` return an `Outcome` (`OK, NO_BOND, ALREADY, NOT_OUT, EXEMPT, LOCKED, DISABLED, DEAD`) so a key can say why. Summon puts the weapon in the selected hotbar slot and the displaced item goes to the first free slot (or the stash), never deleted; dismiss gives it back. A dismiss is refused while `HeroCurse.locked(player)`; a curse system that throws does not trap the wielder.
* **What is checked, and when.**

| moment | what the core does |
|---|---|
| every 4 ticks, and at once when marked dirty | `reconcile`: weapon out means exactly one current projection in the home slot and none elsewhere (a stray one is wiped, a missing one is rebuilt); weapon away means none at all. About 42 slot reads, no NBT read, in the common case. Then `scanMenu` |
| an open menu | every hero weapon in a slot of the open menu that is not the player's own inventory (chest, crafting grid, anvil, ender chest, a modded backpack screen) is emptied; the cursor is one of the loan's own slots |
| `inventoryTick` of the item | `HeroLoan.vet`: a projection of someone else, a stale one (bond gone, kind changed) or any stack when the system is off dissolves; a bare stack is queued for adoption |
| adoption | the pure `HeroLoanPolicy` decides (keep, dissolve, adopt). A bare stack is adopted by a survival player: bound to it if free (the gates apply, so a taken Holy Weapon refuses and the stack dissolves with a line), then summoned. Config `adoptStacks` off makes every bare stack dissolve |
| toss (Q, creative drop) | `ItemTossEvent` cancelled, the loan marked dirty, so the weapon is back within a tick |
| item entity | no hero weapon ever joins the world as an `ItemEntity` (`EntityJoinLevelEvent` cancelled), and `LivingDropsEvent` strips them |
| item frame, armour stand, allay | the interaction is cancelled for non-creative players |
| bundle | `canFitInsideContainerItems()` is false; `onDroppedByPlayer` is false |
| death | `LivingDeathEvent` at HIGHEST priority: the loan ends before the drops, so the displaced item is among them and the weapon is not. The bond and its level stay. `HeroForms.refresh` follows |
| logout, server stopping | `shutdown`: the projection is taken out of the inventory that is about to be saved, the displaced item put back, and the bond keeps `summoned = true` so the next login restores it |
| login, respawn, dimension change | register agrees with the bond, `reconcile`, `HeroForms.refresh`, full sync to the client |
| creative, spectator | outside the whole machine: the stack is a plain item, nothing is rebuilt or dissolved, a running loan ends when the mode changes |

`HeroForms.refresh` is called after every summon, dismiss, login, respawn, dimension change, bind and death, one tick after a release, and when `reconcile` ends a running loan because the system was switched off or the player went creative.

## 4. The item (`HeroWeaponItem`, one class for all 21)

Why: what a weapon DOES is in its logic and forms; what the item must do is common.

* **Properties.** One in a stack, Epic, fire-proof, unbreakable (`Unbreakable(false)`: no durability, no tooltip line). Melee numbers come from the pure `HeroBaseStats` table, turned into the item's attribute modifiers with the vanilla ids `base_attack_damage` and `base_attack_speed` (so the tooltip reads like any weapon) and, when the row has reach, an `ENTITY_INTERACTION_RANGE` modifier `nusmp:hero_weapon_reach`. The forms system adds its own modifiers on top.

| weapon | damage | attacks/s | reach | | weapon | damage | attacks/s | reach |
|---|---|---|---|---|---|---|---|---|
| Shield | 2 | 1.6 | +0 | | Carriage | 2 | 1.2 | +0 |
| Sword | 7 | 1.8 | +0 | | Fan | 5 | 1.9 | +0 |
| Spear | 7 | 1.5 | +1.5 | | Scythe | 8 | 1.2 | +1 |
| Bow | 2 | 1.2 | +0 | | Katana | 7 | 2.0 | +0 |
| Hammer | 9 | 0.9 | +0 | | Book | 2 | 1.4 | +0 |
| Claw | 5 | 2.4 | +0 | | Mirror | 2 | 1.4 | +0 |
| Axe | 9 | 1.0 | +0 | | Instrument | 2 | 1.4 | +0 |
| Whip | 4 | 1.7 | +2 | | Harpoon | 6 | 1.3 | +1 |
| Staff | 3 | 1.4 | +0 | | Sewing Set | 2 | 1.6 | +0 |
| Gauntlet | 6 | 2.2 | +0 | | Chain | 6 | 1.5 | +2 |
| Projectile | 3 | 1.6 | +0 | | | | | |

  The damage column is the owner's list. The speeds are mine, chosen so that no weapon out-damages a netherite sword (8 x 1.6 = 12.8 per second at full charge) by more than a fifth (the Katana's 14.0 is the highest); a hero weapon's real power is its techniques, which go through the balance law. The test pins this.
* **Use animation by the weapon's style (`HeroStyle`).** BLOCK (the Shield): `UseAnim.BLOCK`, use duration 72000, and `canPerformAction(SHIELD_BLOCK)` is true, so raising, the arm pose and vanilla's 5-tick raise delay are vanilla's. DRAW (the Bow): `UseAnim.BOW`, 72000, and the release is handed to the weapon's logic as `onRelease(ctx, chargeTicks)`; it costs the dispatcher nothing and starts no cooldown (the shot is the weapon's own, its techniques are the dispatcher's). Every other style: `UseAnim.NONE`, and the use key is the first technique.
* **`use`.** Main hand only. Sneaking with something in the off hand first offers it to the gem (`HeroForms.absorbOffhand`); if that took it, stop. Then the trigger: sneak means `SNEAK_USE`; a raised style (BLOCK, DRAW) has no plain-use technique, so the use key raises the guard or draws the string; every other style fires `USE`. A fired technique returns success; a raised style that fired nothing starts using the item.
* **Blows.** `onLeftClickEntity` remembers how charged the swing was (the strength is reset before the hit is reported); `hurtEnemy` reports the landed blow to `HeroCombat`: the logic's `onHit(ctx, target, dealt, fullSwing)`, one mastery point (`FormMastery.hit`) and weapon experience 1 (3 on a full swing, strength 0.9 or more).
* **Blocks.** `LivingShieldBlockEvent` for a raised Shield: the logic's `onBlocked` says how much gets through, the event blocks the rest, the guard takes no wear, and a block that stopped something is one mastery point (`FormMastery.block`).
* **Tooltip** (`HeroTooltip`): class label, whose it is, level (progress to the next), active form and the techniques on their inputs (the last three from the client's copy of the bond, only on a stack of the weapon the local player is bound to), the keys, the Holy restrictions in one line, "bound to its wielder's body".

## 5. The dispatcher (`HeroUse.fire(player, trigger)`) and the cooldown book

Why: one entry point turns an input into a cast, so the owner's balance rules are true for every weapon and no skill can forget its own cooldown or cast for free.

1. The checks run in a fixed order (`DispatchRules.check`, pure and tested): weapon not summoned (creative counts as summoned), weapon not in the main hand, no skill on this input in the active form, a wind-up already running (busy, silent), the skill's cooldown, the global lock. Each refusal that matters shows one action-bar line.
2. **Pay first.** `HeroCtx.pay(skill.magoiShare())` spends magicules and then Aura. `HeroPay` reads the pool before and after into a `Receipt` so that a technique that then refuses, fails, throws or is cancelled during its wind-up gives exactly that back (never above the pool's maximum, never taking away what regenerated meanwhile). A player who cannot pay never starts a wind-up. Creative players and players with no Tensura record pay nothing.
3. **Wind-up.** A skill with `windUpTicks > 0` is scheduled with `SpellRuntime.later`, with a ticket so a cancelled timer does nothing when it fires. It is cancelled (cost refunded) when the wielder dies, puts the weapon away, releases the bond, changes dimension or logs out; it also checks at the end that the weapon is still out and in the hand.
4. **Cast** inside a try/catch (`RuntimeException | LinkageError`): a technique that throws is logged once, refunded, and counts as a refusal.
5. **After a good cast:** cooldown = the skill's base ticks x `HeroLevels.cooldownScale(level)` (1.0 at level 0 down to 0.75 at level 100), never below `minCooldownTicks` (20); the global lock `skillGlobalLockTicks` (40) starts; creative players get a quarter of the cooldown (floor 10 ticks) and a lock of at most 10; `FormMastery.skill`; `HeroXp.gain(2)`; the quest hook `hero.skill`; the client is told the cooldown for its bars. A key press also swings the arm.
6. Cooldowns live in the pure `CooldownBook` (per player, per skill id, in absolute game time, clamped to 20 minutes, safe against a world clock set back). They are not saved; a player's book is cleared on logout, release and server stop.

**Keys on the server (`HeroKeys`).** A press is answered at most every `keySpacingTicks` per key (a held key or a macro cannot flood it); the summon and form keys at most every 10 ticks. While a technique key stays down the client sends a heartbeat every 10 ticks; `KeyHold` (pure) turns presses, beats and releases into "how long has it been held", and a key with no beat for 25 ticks counts as released. `HeroKeys.heldTicks(player, trigger)` and `isHeld` are the public reads (the curse builder uses them for sneak + key three held for three seconds).

## 6. Keys (client)

| key | id | default | what |
|---|---|---|---|
| technique 1 | `key.nusmp.hero_skill_1` | Z | press: fire `KEY_1` of the active form; held: heartbeat |
| technique 2 | `key.nusmp.hero_skill_2` | X | `KEY_2` |
| technique 3 | `key.nusmp.hero_skill_3` | C | `KEY_3` |
| summon / put away | `key.nusmp.hero_summon` | B | toggles the weapon (`HeroLoan.summon/dismiss`) |
| change form | `key.nusmp.hero_cycle` | N | `HeroForms.cycle`, shows "Form: X" |

Category `key.categories.nusmp`. The keys the mod already uses are V, R, G, J, K, H, Y, U, the arrow keys, Home and End (checked by grep: no use of Z, X, C, B, N or M anywhere else); M is the Weapon Book (forms builder). The keys do nothing and send nothing without a bond, with a screen open, or in spectator mode.

## 7. The three restrictions

### 7.1 Rejection (Holy Weapons only; config `holyRejectsOthers`)

Why: canon says a hero is shocked when they try to fight with a weapon of another class; the owner's text says the Holy weapons "reject non-class weapons".

* **The class of an item** is decided by the pure `WeaponClassifier` from facts that `HeroRules.classOf` reads off the stack: the allow tag first, then a class the item declares, then how it is used (raised: SHIELD; drawn: BOW; thrown: SPEAR), then its Java class and tags (sword, axe, mace), then "a tool is not a weapon", and last "adds 4 or more attack damage: OTHER_WEAPON". Classes: SWORD, AXE, SPEAR, BOW, SHIELD, BLUNT, OTHER_WEAPON, NONE.
  * Declared classes: the 21 hero weapons (Shield, Sword, Spear, Bow have their own class; the other seventeen are NONE, so always allowed); this mod's Magebane Shield (SHIELD), Bolt Thrower and Crossbow of Swift Death (BOW), Club of Rot and Doom Hammer (BLUNT), Butcher Cleaver (AXE), Hand of Providence and Quickling's Ivy (OTHER_WEAPON); the Otherworld Trident (SPEAR); the Djinn weapons by their table row (lance, trident, glaive: SPEAR; greatsword: SWORD; axe: AXE; bow: BOW; staff: BLUNT; the rest OTHER_WEAPON); ranged relics (BOW); and two kinds of Tensura weapon whose Java class would say "sword": its spears (SPEAR) and scythes (OTHER_WEAPON).
  * Tags read: `#minecraft:swords`, `#minecraft:axes`, `#c:tools/spear`, `#c:tools/bow`, `#c:tools/crossbow`, `#c:tools/shield`, `#c:tools/mace`, and this mod's `#nusmp:hero_allowed` (ships empty; a datapack adds to it, and anything in it is never rejected).
* **Who is shocked:** a bonded Holy wielder (not creative, not spectator), for an item whose class is not NONE and not their own. A Seven Star or Vassal wielder is never shocked. Holding something is harmless; using it to fight is not.
* **Where it bites** (all server side, all cancelled before the damage): `AttackEntityEvent` (a blow), `LivingIncomingDamageEvent` for a physical hit that reached the damage step by another route (a mod item's right-click technique), `LivingEntityUseItemEvent.Start` and `ArrowLooseEvent` (drawing or loosing a bow, crossbow or trident of another class), `PlayerInteractEvent.RightClickItem` (a loaded crossbow fires at once). Tools used as tools are never affected: breaking blocks raises none of these events.
* **The shock** (`HeroRules.shock`): magic damage `shockDamage` (4 = two hearts), never lethal (`ShockRules.damage` leaves at least half a heart; a last-word damage listener caps it again after every other mod's multiplier), at most once a second; Slowness II for `shockSlowTicks` (20); a barrier ring and a shield-block sound in the weapon's colour; and the line "The Legendary X rejects the weapon" at most once every three seconds.

### 7.2 Bound to the body

Section 3. There is no switch for it: it is what makes the bond a bond. `adoptStacks` only decides what happens to a bare stack.

### 7.3 Interference (Holy Weapons only; config `repelXp`, `repelRadius`)

Why: canon says heroes cannot gain experience from defeated monsters while another hero is close; the weapons interfere. The owner wrote "too close to one another"; the design reads that as a radius.

* `HeroInterference.blocked(player)` is one hash lookup. The set behind it is rebuilt once a second (server tick % 20 == 5) from the Holy wielders online (alive, survival or adventure) by the pure `InterferenceGeometry`: a wielder is blocked when another wielder is in the same dimension within `repelRadius` blocks, 3D, inclusive. Both are blocked. Seven Star and Vassal wielders neither block nor are blocked. The set is stale by up to a second.
* **What is refused for a blocked player:** vanilla orbs (`PlayerXpEvent.PickupXp` cancelled; the orb stays where it is), the experience a kill would drop (`LivingExperienceDropEvent` set to 0 when the killer is blocked), and the weapon's own experience (`HeroXp.gain` returns 0).
* **What is told:** the line "The Legendary Weapons repel each other" at most once per `repelNoticeSeconds` (60), the quest hook `hero.repelled`, and a red line on the HUD while the state lasts.
* **What is NOT blocked, on purpose.** The Labyrinth System (`labyrinth/mega/system`) pays quests with stat points and Shadow Cores; it grants no experience and no levels (its only experience listener cancels the orbs of shadow minions), so there is nothing to wire. If the owner reads "stat points" as "levels", one line at the top of `SystemRewards.grant(ServerPlayer p, SystemState s, QuestProgress q)` does it: `if (com.newuniverse.nusmp.heroweapon.rules.HeroInterference.blocked(p)) return RewardRoller.Reward.none();` (a quest finished while repelled then pays nothing). It is not applied: it edits a System file and changes a prize, which is the owner's call. Tensura's evolution points, `/xp`, advancement experience rewards (`giveExperiencePoints` called directly) are not blocked: canon says "from defeated monsters", and orbs and kills are what that means.

## 8. The weapon's experience (`HeroXp`)

Why: the weapon's level (0 to 100, the contract's `HeroLevels`) must be granted through ONE method so the interference rule can refuse it.

| source | points |
|---|---|
| a melee blow | 1; 3 on a full swing |
| a technique that was cast | 2 |
| a kill by the wielder (weapon out, or creative) | victim's max health x `killExpShare` (0.25), x `bossKillExpFactor` (5) for a boss (`BalanceLaw.isBoss`), rounded, at most `maxKillExp` (500); nothing for the wielder's own allies and pets |
| mastery, absorption, anything else | the forms and gem builders call `HeroXp.gain` themselves |

All of it is multiplied by `weaponExpMultiplier` and stops at `maxLevel`. The curve is the contract's: 40 x L^1.7 in total (level 10 needs about 2 000, level 100 about 100 000). A level gained shows "The X grows stronger: level N", plays the level-up sound and fires `hero.level` once per level crossed. These rates are a first guess and will want tuning once someone plays it.

## 9. Client: HUD, tooltip, model predicates, the curse look

* **HUD** (`HeroHud`, layer `nusmp:hero_hud`, above everything): bottom left, in the weapon's colour and the mod's panel theme: gem diamond, name, level, an experience bar; while the weapon is out also the active form and one row per technique (input, name, and a bar with the seconds left while it recharges); while away "Put away: press B"; a red line while repelled. Moved and scaled by `nusmp-heroweapon-client.toml`. It reads `ClientHeroBond` only. Its drawing and the key tick run inside `SafeCast.client`, so a fault is logged once and never reaches the frame. The curse system draws its own bar below it.
* **`ClientHeroBond`** is the client's copy of the last `Sync` plus the cooldown bars. It imports nothing of the game's client, so the common tooltip may read it and a dedicated server never loads anything it cannot. The cooldown clock stops while a single-player game is paused. It is forgotten at logout.
* **Model predicates.** `blocking` on the Legendary Shield (1 while the guard is up) and `pull` / `pulling` on the Legendary Bow, with vanilla's names and formulas, so an item model JSON may switch display transforms with an `overrides` entry. Bow `_pull` meshes are not wired (the mod's state-variant logic ignores bows that are not relics).
* **The curse look.** The one edit in `client/WeaponRenderer.meshId`: a hero weapon whose stack says `look = curse` is drawn from the mesh `<id>_curse` (and its skins) when that mesh exists; otherwise it keeps the base look. The second edit, in `client/relics/item/WeaponClasses`, adds `curse` to `STATE_SUFFIX` so `<id>_curse` takes its base weapon's class and model info (the same rule as `_pull1`, `_charged`). The Python mirror `tools/relics/weapon_classes.py` has its own `STATE_SUFFIX`; it got the same token, so `test_weapon_scale.py` (35 tests, run: OK) and the shipped-mesh check treat a `<id>_curse` mesh as its base weapon. Both are edits outside the core's own files, made because the curse look cannot work without them; if the lead prefers, revert them and add the four `<id>_curse` entries to `weapon_classes.json` instead.

## 10. Config

`nusmp-heroweapon-server.toml`, section `[heroweapon]`, a SERVER config (one per world, in the world's `serverconfig` folder). Registered inside `HeroCore.init`, so the shared start-up needs no line for it. Every value is a plain `define` / `defineInRange` (never `defineInList`) read through `HeroConfig.get`, which answers the default while the spec is not loaded (the way `DndConfig` does).

| key | default | range | what |
|---|---|---|---|
| `enabled` | true | | master switch: nobody can be bound, summoned weapons are put away, no technique, restriction or experience rule runs |
| `maxBonds` | 1 | 0..1 | 0 closes the doors (no new bond; existing bonds stay) |
| `holyUnique` | true | | one holder per Holy Weapon |
| `vassalUnique` | false | | the same for the other seventeen |
| `holyRejectsOthers` | true | | restriction one |
| `repelXp` | true | | restriction three |
| `repelRadius` | 48 | 1..512 | blocks |
| `repelNoticeSeconds` | 60 | 1..3600 | between two "repel each other" lines |
| `adoptStacks` | true | | bare stacks are adopted by their first survival holder |
| `weaponExpMultiplier` | 1.0 | 0..100 | on every point of weapon experience |
| `maxLevel` | 100 | 1..100 | the highest level experience can reach (`level set` can still go to 100) |
| `killExpShare` | 0.25 | 0..10 | of the victim's max health |
| `bossKillExpFactor` | 5.0 | 1..100 | |
| `maxKillExp` | 500 | 1..1000000 | cap per kill |
| `shockDamage` | 4.0 | 0..20 | two hearts, never lethal |
| `shockSlowTicks` | 20 | 0..600 | |
| `skillGlobalLockTicks` | 40 | 0..600 | between two techniques of one wielder |
| `minCooldownTicks` | 20 | 1..600 | the balance floor |
| `keySpacingTicks` | 4 | 0..40 | the server answers a press of the same key at most this often |
| `showMessages` | true | | action-bar lines for the weapon |

`nusmp-heroweapon-client.toml`, section `[hud]`: `show` true; `x` 6 (0..4000, pixels from the left); `y` 6 (0..4000, from the bottom); `scale` 1.0 (0.5..3.0).

## 11. Commands (`/multiverse hero ...`)

| command | who | what |
|---|---|---|
| `status` | anyone | your weapon: class, holder, level and experience to the next, form, learned forms, absorbed kinds, out or away, repelled, despair and corruption, cooldowns running |
| `status <player>` | permission 2 | the same for another player |
| `list` | anyone | the 21 weapons and who holds each |
| `summon`, `dismiss` | anyone (yourself) | the same doors as the key, with the refusal reason |
| `bind <player> <weapon>` | 2 | through `HeroBonds.bind`, so the gates apply |
| `release <player>` | 2 | level, forms and mastery are gone |
| `level <player> set <n>` | 2 | 0..100; sets the experience to the level's threshold |
| `registry list` | 2 | same as `list` |
| `registry clear <weapon>` | 2 | frees every register claim of one weapon (my addition; the way out of a stuck Holy Weapon) |

The weapon argument suggests the 21 item ids and also accepts a short name (`HeroKind.parse`). The forms, curse and gem nodes hang under the same `hero` node (`FormsCommands`, `CurseCommands`, `GemCommands`) and are not the core's.

## 12. Payloads (mod channel version "1", all `optional()`, so a client without the mod can still join)

| id | direction | fields | server checks |
|---|---|---|---|
| `nusmp:hero_key` | to server | slot 0..2, phase 0 press / 1 hold / 2 release | slot range, alive, enabled, spacing |
| `nusmp:hero_act` | to server | action 0 toggle / 1 cycle / 2 summon / 3 dismiss | bond, spacing |
| `nusmp:hero_sync` | to client | weapon id (empty = none), exp, form id and name, summoned, flags (1 repelled, 2 cursed, 4 locked), learned forms, mastery map, absorbed-kinds count, absorbed total, the largest 24 absorbed items, the five technique ids of the active form (use, sneak, key 1, 2, 3) | lists capped at 512 on read |
| `nusmp:hero_cooldown` | to client | skill id, ticks | |

The sync is built from the bond as a record. It is sent at once on login, respawn, dimension change, bind and release, and otherwise polled once a second per bonded player and sent only if it differs from the last one sent (so a quiet second costs one comparison and no packet, and "every change, at most once a second" holds by construction). Stand-in players (self-tests) have no connection and are skipped.

## 13. Quest hooks (`NusmpEvents.fire`)

| key | argument | fired from |
|---|---|---|
| `hero.bound` | weapon item id | the core's bind listener (the gem builder must NOT fire it again) |
| `hero.summoned` | weapon item id | after every summon |
| `hero.level` | weapon item id | once per level crossed, so "reach level N" is a count (note: counters never decrease, so a released and re-bound weapon counts its levels again) |
| `hero.skill` | skill id | after every cast technique (my addition) |
| `hero.repelled` | none | when a refused gain is told to the player |

## 14. Events registered

Game bus (`NeoForge.EVENT_BUS`), all ignoring the client's copy of a player; every handler that runs game logic is inside `SafeCast.tick` (the few that only cancel or filter are plain):

| class | events (priority) |
|---|---|
| `HeroBondEvents` | `PlayerLoggedInEvent`, `PlayerRespawnEvent`, `PlayerChangedDimensionEvent`, `PlayerLoggedOutEvent`, `PlayerTickEvent.Post`, `ServerTickEvent.Post`, `ServerStoppingEvent`, `ServerStoppedEvent` |
| `HeroLoanEvents` | `ItemTossEvent` (HIGH), `EntityJoinLevelEvent`, `LivingDropsEvent` (HIGHEST), `LivingDeathEvent` (HIGHEST), `PlayerInteractEvent.EntityInteract` (HIGH), `PlayerInteractEvent.EntityInteractSpecific` (HIGH) |
| `HeroCombat` | `LivingDamageEvent.Post` (LOWEST), `LivingIncomingDamageEvent`, `LivingShieldBlockEvent`, `LivingDeathEvent` (LOW) |
| `HeroRuleEvents` | `AttackEntityEvent`, `LivingIncomingDamageEvent`, `LivingEntityUseItemEvent.Start`, `ArrowLooseEvent`, `PlayerInteractEvent.RightClickItem`, `PlayerXpEvent.PickupXp`, `LivingExperienceDropEvent` (all HIGH), `LivingDamageEvent.Pre` (LOWEST, the shock's cap) |

Mod bus: `RegisterPayloadHandlersEvent` (`HeroNet.register`). Client: `RegisterKeyMappingsEvent`, `RegisterGuiLayersEvent`, `FMLClientSetupEvent` (model predicates) on the mod bus; `ClientTickEvent.Post` and `ClientPlayerNetworkEvent.LoggingOut` on the game bus.

The player tick does, per bonded or stray-carrying player: every 4th tick (or when dirty) `reconcile` and `scanMenu`; adoption; every 5th tick the weapon's `onTick` while it is out; once a second the sync poll. The server tick rebuilds the interference set once a second.

All static state (cooldown book, pending wind-ups, key state, sync memory, swing and damage records, shock times, interference set, notice times, dirty and adoption queues) is cleared per player at logout and entirely on `ServerStoppedEvent`.

## 15. Tests

* **Self-tests** (run by the CI server smoke test, which expects the prefix `hero`; they skip with a note when the system is off in the config). `hero.bond`: bind refusals, one weapon per player, the register, the bond's NBT round trip (full, empty, unknown weapon, weapon only), the copy for the client and its codec, summon on a real inventory (projection in the selected slot, owner, displaced item), a second summon refused, a stray second projection wiped, dismiss and death give the displaced item back and leave no hero weapon, adoption of a bare stack, dissolving of another weapon's stack and of someone else's projection, a hero weapon in a crafting grid vanishing, release, and a unique Holy Weapon refused to a second player and free again after release. `hero.use`: a private test skill through the real dispatcher: cost, cooldown from the level and the floor, lock, repeat refused, a refusing technique costs nothing and starts nothing, refund of a paid refusal, wind-up (waits, busy, cancelled cleanly), a put-away weapon casts nothing. `hero.rules`: the class table on real items (vanilla swords, axes, bow, crossbow, trident, shield, mace, tools, food, the 21 hero weapons, three of this mod's), the rejection decision for stand-in wielders of a Holy and a Seven Star Weapon, the shock (stand-in at 1.5 health survives, is slowed, a second shock within the second changes nothing), and the interference of two stand-ins (near: both blocked and weapon experience refused; far: free and granted; a Seven Star neighbour: free).
* **Unit tests** (JUnit 5, pure classes, 56 tests): `HeroBaseStatsTest` 5, `HeroCurseLookTest` 2, `WeaponClassifierTest` 6, `InterferenceGeometryTest` 6, `KillXpShockTest` 6, `CooldownBookTest` 8, `DispatchRulesTest` 4, `KeyHoldTest` 7, `HeroRegistryDataTest` 8, `HeroLoanPolicyTest` 4. Run locally with a small JUnit shim (`jt.sh`), not the real JUnit. Mutation check: in `WeaponClassifier.rejects` the comparison `held != ownClass(wielder)` was flipped to `==`; `WeaponClassifierTest.aHolyWielderIsShockedByAnyOtherWeaponClassAndOnlyThat` failed ("SHIELD holding SWORD: expected true but was false"); restored, all pass.

## 16. Why each class exists

Pure (no `net.minecraft` import, unit-tested):

* `HeroBaseStats`: the melee numbers of the 21 weapons as a table (section 4). `HeroColors`: one colour per weapon for the HUD gem, wind-up charge-up and shock ring.
* `rules/WeaponClass`, `WeaponClassifier`: which class an item is and who is shocked by it (7.1). `InterferenceGeometry`: who is "too close" (7.3). `KillXp`: kill experience and its cap. `ShockRules`: the shock's damage can never kill. `Spacing`: "is it due again", with the two traps of a time stamp (never happened, clock set back), for key spacing, shock damage, the rejection line and the repel notice.
* `skill/CooldownBook`, `DispatchRules`, `KeyHold`: section 5.
* `bond/HeroRegistryData`: the register's rules (section 2). `HeroLoanPolicy`: keep, dissolve or adopt a stack found in an inventory (section 3).

Game glue:

* `HeroConfig`, `HeroCore`: the config and the one entry point called from `HeroWeaponSystem.init` (config, payloads, listeners, self-tests; safe to call twice).
* `HeroWeaponItem`, `HeroItems`, `HeroTooltip`: section 4.
* `bond/HeroBondEvents`: gates, listener, the moments of a player's life and the beats. `HeroRegistry`: the saved-data wrapper. `HeroLoan`, `HeroSummon`, `HeroLoanEvents`: section 3. `HeroNet`, `HeroSync`: section 12.
* `skill/HeroUse`, `HeroPay`, `HeroKeys`, `HeroCombat`: sections 4 and 5 (`HeroCombat` translates the game's fight events into the logic's `onHit`, `onDamaged`, `onBlocked`, `onKill`, and remembers a swing's charge and the damage just dealt).
* `rules/HeroRules`, `HeroRuleEvents`, `HeroInterference`, `HeroXp`: section 7 and 8.
* `HeroCommand`: section 11. `HeroBondSelfTests`, `HeroUseSelfTests`, `HeroRulesSelfTests`: section 15.
* Client: `HeroClient` (entry point, predicates), `HeroKeysClient`, `HeroHud`, `HeroClientConfig`, `ClientHeroBond`: section 6, 9.

## 17. What is unverified (honest list)

Nothing ran in the game. The whole project cannot be compiled here; my files were type-checked with a stub-based `javac` (only the known abstract-`new` artifacts remain: `KeyMapping` x5, `FriendlyByteBuf`, `ItemEntity`, `MobEffectInstance`; a deliberate wrong call proved the checker catches real mistakes), the pure parts were unit-tested under a JUnit shim. In order of how likely each is to need a fix after the first run:

1. **The shield block.** `LivingShieldBlockEvent`: I read from the API dump that `setBlockedDamage` is how much the shield absorbs and `setShieldDamage` the wear, and I set `blocked = original - through`, wear 0. That the event fires for an item that only answers `canPerformAction(SHIELD_BLOCK)`, that `isBlocking()` follows the item's use duration minus the 5-tick delay, and the third-person guard pose are assumed from vanilla's code, not seen.
2. **The loan on a live inventory.** The slot swap and its sync to the client; that cancelling a toss really leaves the slot empty for the beat; that `onDroppedByPlayer` is called on the Drop key path (the toss event is the guard either way); that `canFitInsideContainerItems` is read for bundles and shulker boxes. Items inserted by another mod's backpack or curio slot while its screen is closed are out of reach; with the screen open they are in `containerMenu.slots` and are emptied within four ticks.
3. **Logout and stop ordering.** I believe `PlayerLoggedOutEvent` fires before the player is saved and `ServerStoppingEvent` before `saveAll`. If either is wrong the saved inventory still contains the projection, and the next login's `reconcile` keeps it if it is in the home slot and rebuilds it if not (no duplication either way, by the loan machine).
4. **Stand-ins in the self-tests.** NeoForge's `FakePlayer` answers `position()` and `blockPosition()` with the origin and (I believe) reports itself invulnerable, so (a) effects are sent around the origin (harmless), (b) the shock's damage cannot be measured on a stand-in: `hero.rules` proves the slowness, that nothing throws and that a repeat within the second changes nothing, while the number itself is pinned by the pure `ShockRules` test and the last-word cap (`LivingDamageEvent.Pre`) is verified by reading the code only; (c) `addEffect` is assumed to work on a stand-in. If a self-test fails on its first CI run, suspect the stand-in before the rule.
5. **Raised styles.** For a Shield or Bow the client always calls `startUsingItem` on use, while the server may fire a sneak technique instead; the client then shows a raised guard that the server does not have (the same desync the Magebane Shield accepts).
6. **Client drawing.** The HUD, the key list, the tooltip and the model predicates were never rendered or opened. The panel helper `UiPanel.panel`'s two alpha arguments are taken from their names.
7. **Interactions cancelled** for item frames, armour stands and allays: `EntityInteract` and `EntityInteractSpecific` are assumed to cover every route by which a held item is hung or handed over.
8. **Melee detection.** A technique's damage uses `HeroCtx.hit` (damage type `playerAttack`), indistinguishable from a melee blow in the damage events; blows are therefore detected through the item (`onLeftClickEntity` + `hurtEnemy`), which reports only the primary target of a sweep.
9. **Classification coverage.** Other mods' weapons are classified by Java class, vanilla and common tags and the attack-damage catch-all. A weapon from a mod that is none of these and adds less than 4 damage is "not a weapon" and never rejected; a sword-class weapon of a third mod with an unusual Java class may be OTHER_WEAPON and so rejected by every Holy hero, including the Sword hero. The fix is the tag `#nusmp:hero_allowed` or a line in `HeroRules.declared`. The Tensura spear and scythe mapping was written from the class names in the API dump and not run against the real items (their registry ids are not in my dumps), so `hero.rules` does not cover it.
10. **Economy.** The experience rates, the speeds of the table and the 48-block radius are first guesses.
11. Canon: see section 18. The "bound to the body" restriction is the owner's text; my searches found no canon statement of it. The wiki summaries I used were search summaries, not the pages.

## 18. Sources

* Search summaries of the series' wiki, not opened (the pages cannot be fetched from the build machine): https://shield-hero.fandom.com/wiki/Legendary_Weapons (heroes cannot gain experience from defeated monsters while another hero is near, their weapons interfere; a hero can handle another weapon but is zapped when trying to fight with it), https://shield-hero.fandom.com/wiki/Heroes (the same drawback), https://shield-hero.fandom.com/wiki/Vassal_Weapons (vassal weapons trade potential power for fewer restrictions). The other pages in the design's canon section (Power-up Methods, Curse Series) were used only through the design.
* The facts about the Minecraft, NeoForge and Tensura APIs come from the local API dumps of the build machine (member lists and signatures), not from the web; behaviour that is not in a signature is from my memory of the 1.21.1 code and is listed in section 17.
* In this repository: `docs/djinn_weapons.md` and `relics/djinn/weapon/WeaponLoan` (the loan and its tests), `item/MagicWeaponItem` and `MagebaneShieldArts` (the shield), `labyrinth/dnd/DndConfig` (the config pattern), `item/WeaponEngravings` (engraving on first holding), `labyrinth/mega/SelfTests` (the stand-in pattern).

## 19. For the other builders and the lead

* **Forms:** `HeroSummon.stack(player)` is the live projection. The core calls `HeroForms.refresh` at the moments in section 3 and builds a new projection with `look = curse` when the active form's look says so; `FormMastery.hit/skill/block` are called by the core; the bow's `onRelease` is the weapon's own to reward (`HeroXp.gain`, `FormMastery.hit`) if wanted. **Release ordering:** `HeroBonds.release` calls its listeners BEFORE it clears the bond, so a `refresh` called from a release listener would still see a bound weapon. The core therefore calls `HeroForms.refresh(player)` one tick after a release (through `SpellRuntime.later`, skipped if the player has left), when the player has no weapon, so `refresh` can take every modifier off; a release listener of the forms system itself is welcome but not needed. `refresh` must be idempotent (the contract says it is).
* **Curse:** `HeroKeys.heldTicks(player, HeroTrigger.KEY_3)` and `isHeld` give "held for N ticks"; the dismiss refusal and the sync flags read `HeroCurse.locked/active`.
* **Gem:** `hero.bound` is already fired; `HeroRegistry.holderOf/holdersOf/nameOf/clear(server, ...)` serve a `holders` command; bind with `HeroBonds.bind`, then `HeroSummon.summon`.
* **Weapons:** the dispatcher already does the payment, cooldown, lock, mastery, experience and quest hook; a skill returns true or false and nothing else. `onHit` already gets the experience and mastery of the blow.
* **Art:** the `blocking`, `pull` and `pulling` predicates exist; `<id>_curse` meshes are used when present; `HeroColors` is the one place to align the HUD gem colours with the models' gems.
* **Lead:** the lang fragment is `$S/hero/frag/hcore.lang.json`; engravings lines for the table take effect on the next projection and by the one-a-second engraving tick; no `NUSMP` or `MultiverseCommands` line is needed.
