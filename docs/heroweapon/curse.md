# The Curse Series of the hero weapons (1.62.0)

> **1.99.0, THE HUNGER BAN.** Gluttony no longer drains the food bar: its downside is now a bite of the wielder's magicules (`sins.gluttonyExhaust` 0.4 = 0.2% of the pool a second at normal strength, never under the arena floor of 10%). The key `gluttonyFoodFloor` is kept but unused; kills still feed. See `docs/labyrinth/hunger_ban.md`.


The owner's text: "*Curse Series: unlocked when the wielder suffers extreme emotional trauma or despair, tapping abilities tied to the seven deadly sins at a heavy
physical or mental cost.*" This document is the WHY of everything the curse system does, its numbers, its config, its commands, and an honest list of what has not
been run. The binding contract is `docs/heroweapon/design.md` section 7; where this file goes further, it says so.

New, replacement, kept: **everything here is NEW** (nothing like it existed). Nothing is replaced and nothing is removed. The only existing code it touches is
read-only (`BalanceLaw`, `VfxSpawn`, the party bridge, the Safe Zone's `RestState`, the quest door `NusmpEvents`).

## 1. Canon used (search summaries of the fandom wiki only; the pages could not be opened from the build machine)

| Fact | Source | How known |
|---|---|---|
| The Curse Series is unlocked after an extreme psychological stimulus, a defence mechanism of the weapon; seven series: wrath, greed, gluttony, pride, lust, envy, sloth. | https://shield-hero.fandom.com/wiki/Curse_Series | summary |
| Using it brings a physical and a psychological curse; the physical heals with time, the psychological is much harder; a penalty that scales with the curse level applies when the variant is removed or its skills are used. | https://shield-hero.fandom.com/wiki/Curse_Series , https://shield-hero.fandom.com/wiki/Curse_Series_(Web_Novel) | summary |
| Wrath (the Shield, Naofumi): self-burning curse, loss of all SP, blood drawn out (Blood Sacrifice), all stats but defence cut by 30% until cured, empathy suppressed; unlocked after a false accusation and a lost duel. The curse flames reduce healing. | https://shield-hero.fandom.com/wiki/Wrath_Series_(Web_Novel) , https://shield-hero.fandom.com/wiki/Curse_Series/Weapon_List | summary |
| Greed (the Sword, Ren): the user cannot hold anything of value, takes others' equipment. Gluttony: cannot gain experience, levels fall, kills for experience. | https://shield-hero.fandom.com/wiki/Greed_Series_(Web_Novel) , https://shield-hero.fandom.com/wiki/Gluttony_Series_(Web_Novel) | summary |
| Lust (the Spear, Motoyasu): Temptation, an area allurement; unlocked after his companions abandoned him. Lust and Envy together came from jealousy of Naofumi and Melty's bond with Filo; the Envy skill amplifies jealousy. | https://shield-hero.fandom.com/wiki/Lust_Series_(Web_Novel) , https://shield-hero.fandom.com/wiki/Envy_Series_(Web_Novel) | summary |
| Pride (the Bow, Itsuki): his arrows put the struck under his control; the cost is an obsession with his own justice, a passionless, suggestible self; unlocked after Malty's manipulation. | https://shield-hero.fandom.com/wiki/Pride_Series_(Web_Novel) | summary |
| Sloth (the Legendary Hunting Tool, the fifth hero, Kizuna): lethargy, loss of levels, a curse emanation that curses whoever stands too close; broken by rekindling the wielder's passion. | https://shield-hero.fandom.com/wiki/Legendary_Hunting_Tool | summary |
| Vassal weapons have cursed forms too (a cursed axe, claw, book, instrument, harpoon, and the Mirror of Wrath). | https://shield-hero.fandom.com/wiki/Vassal_Weapons , the weapon pages | summary |
| The Bless Series is only reached by those who overcome a terrible curse; its Shield of Compassion nullifies the curse of its weapon's series; Curse and Bless can be used together. | https://shield-hero.fandom.com/wiki/Bless_Series | summary |
| Envy is tied to no Holy Weapon, Sloth to none of the four (it belongs to the Hunting Tool): they are given to the Vassal Mirror and the Vassal Chain by design (design.md section 18). | design.md | design |

Everything below that is not in this table is **design** for Minecraft, not canon.

## 2. The loop

```
 terrible things happen          the weapon whispers            the wielder chooses              the curse form is worn
 (despair 0..1000)       ---->   (despair >= 600, an offer  --->  EMBRACE (sneak + key held 3 s, ---->  corruption rises (0..1000)
                                  of the sin, 90 s)               Weapon Book button, command)         the sin's downside runs
                                       |                          price: hearts of max health,                 |
                                       v ignored / declined       the first curse form is learned              v at 1000
                                  the offer lapses                                                         OVERRUN 10-20 s: the weapon takes the hands,
                                  (despair falls to 300,                                                   swings at the nearest living thing; then the curse
                                   the weapon waits 3 min)                                                 form ends, corruption 400, a minute of weakness
```

* **Nothing ever unlocks by itself.** The series list changes only by `CurseMachine.embrace` on an OPEN offer, or by an operator (`/multiverse hero curse unlock`). A unit test
  runs every cause, every tick and every refusal for 200 000 ticks and asserts the list is still empty.
* **The curse is worn only while the weapon is out.** A curse form that is the active form but whose weapon is stowed is dormant: corruption falls, no downside runs,
  no tint. Putting the weapon away is the wielder's answer to a rising corruption (the warning at 800 says so). During an Overrun the weapon cannot be put away
  (the core's loan asks `HeroCurse.locked`).
* **The forms system seals what is not embraced**: a curse form whose sin is not in `series` cannot be activated, cycled to, or learned by absorption (its own
  `FormTree.sealed`, reading `bond.extra("curse").series`). As a safety net the curse system also puts a sealed curse form back to the base form within a second.

## 3. State (`HeroBond.extra("curse")`, written by `CurseStore`; every key optional and clamped on read)

| Key | Type | Meaning |
|---|---|---|
| `despair` | int 0..1000 | the trauma meter |
| `corruption` | int 0..1000 | the curse meter; 1000 starts an Overrun |
| `series` | list of strings | the sin ids embraced, in order. **Read by the forms system** (agreed in design.md and the briefs) |
| `offered` | string | the sin of the offer that is open (empty for none). Read by the forms system for the Weapon Book |
| `offeredAt` | long | game time the offer opened |
| `nextOfferAt` | long | no offer opens before this game time |
| `overrunUntil` | long | game time the Overrun ends; 0 for none. `HeroCurse.locked` is `overrunUntil > now` |
| `weakUntil` | long | game time the weakness after an Overrun ends |
| `tax` | int | hearts of maximum health taken for good (the stages added up, before the floor) |
| `refunds` | int | how many times a Purification Charm has given hearts back |
| `lastGain` | long | game time of the last despair gain (the calm timer) |
| `lastRest` | long | the last Safe Zone rest already honoured |
| `causes` | compound | cause key to the game time it was last counted (the pauses); pruned when older than the longest pause |

The state belongs to the bond: it is kept through death (the bond is copied to the new body), wiped when the weapon is released or re-bound. An old save, a save with a
missing key or a hand-edited one loads as "nothing". The max-health tax is a TRANSIENT attribute modifier (`nusmp:hero_curse_tax`, re-applied from the saved `tax` on login,
respawn and every few seconds), so removing this mod or releasing the weapon can never leave a permanent modifier behind.

## 4. Despair: causes, numbers, pauses

Only a bonded player in survival or adventure gains despair. Each cause has an amount (config) and a pause (config): the same cause is not counted again until its pause is over.

| Cause (`Cause`) | When | Amount | Pause |
|---|---|---|---|
| `pet_death` | a creature owned by the wielder (a `TamableAnimal`, any `OwnableEntity`) dies within 32 blocks, same dimension | 150 | 100 ticks (5 s) |
| `avatar_death` | the wielder's loyal Avatar (`avatar.AvatarEntity`) dies within 32 blocks | 300 | 100 |
| `party_death` | a player who is a party mate (vanilla team, or the same FTB team through the dungeon party's `PartyResolver`) dies within 32 blocks | 250 | 100 |
| `npc_death` | a friendly arc character (`ArcNpcEntity` with a character) dies within 32 blocks (they only die by `/kill`, the void, or `arc.npcsMortal`) | 200 | 100 |
| `betrayal` | the wielder is hurt (0.5 health or more) by a party mate, by their own Avatar (loyal or turned), by a creature they own, or by a friendly arc character | 200 | 200 (10 s) |
| `near_death` | health under 15% of the maximum, polled once a second | 120 | 1200 (a minute) |
| `defeat` | the wielder dies while bonded | 200 | none |
| `boss_loss` | the killer is a boss (`BalanceLaw.isBoss`: the boss tag or 300+ maximum health), on top of `defeat` | 400 | none |
| `overrun_kill` | a wielder in an Overrun kills someone who mattered to ANOTHER bonded player within 32 blocks (that player's pet, Avatar, party mate, or an arc friend) | 300 | 100 |
| `other` | another system reports through `HeroCurse.addDespair(player, amount, why)`; an unknown `why` is this cause: the caller's amount (at most 500 per call), no pause | caller's | none |

A known `why` (`pet_death`, `betrayal` ...) uses that cause's pause and the caller's amount. Creative and spectator players are never touched by the automatic parts.

**Fading.** While the wielder is calm (not hurt in the last five seconds, no despair for 20 s, no offer open) despair falls by 2 a second.

**Sources of the despair events in this repo**: the party bridge is `labyrinth/mega/party/PartyResolver.keyOf` (FTB Teams through the soft reflection bridge `quests/QuestTeams`; a
personal or absent team is a party of one), the Avatar is `avatar/AvatarEntity` (`creatorUuid`, `isLoyal`; the brief's `entity/avatar` holds only the Devil Avatar boss), the arc friend is
`arc/ArcNpcEntity.character()`.

## 5. The offer and the Embrace

At despair >= 600 (config), with no offer open, the pause since the last one over, no Overrun, and a sin the weapon can offer, the weapon **whispers**: an action-bar line in the sin's
colour with one of the sin's four whispers, a chat line that states the PRICE and how to accept, the Elder Guardian's curse sound, a Curse Brand effect, the quest hook
`hero.curse_offer` (argument: the sin id), and the screen starts to glow (client). The offer stays open 90 s.

**Which sin.** `CurseCandidates.of(kind, forms)`: the weapon's canon sins first (`HeroKind.sins()`: Shield Wrath, Sword Greed then Gluttony, Spear Lust, Bow Pride), then any other sin the
weapon's forms file carries as a curse form (the Vassal Mirror's Envy, a Chain's Sloth, a justified extra series); a sin is a candidate ONLY when the forms hold a curse form of it (an
offer with nothing behind it would take hearts and give nothing). The first candidate not yet embraced is offered.

**Accept** (four doors, all end in `CurseSystem.embrace(ServerPlayer)`, which re-checks everything):
1. hold **Sneak** and the **Embrace key** (`key.nusmp.hero_embrace`, default **O**) for 3 seconds: a gauge under the crosshair fills; the client sends one `nusmp:hero_curse_act` packet;
2. the Weapon Book's Embrace button (the forms system calls `CurseSystem.embrace(ServerPlayer)` by reflection);
3. `/multiverse hero curse embrace`;
4. hold **Sneak** and the core's **third hero key** for 3 seconds (the design's own gesture), when the core's key bookkeeping is there: every ten ticks the server asks
   `HeroKeys.isHeld(player, KEY_3)` of a wielder with an open offer (found by name, so a tree without that class simply has no fourth door) and accepts after seven looks in a row (`HoldCount`, tested;
   the count starts when the offer is open, so a key already down has to stay down for three more seconds). The offer's chat line then says so. Its drawback is the reason the Embrace key exists:
   a press of key 3 also casts that key's technique on its first tick.

The Embrace key has its own gauge (the client knows its own key); the third key has none (the client has no view of the core's key state), only the chat line.

**Accepting** (permanent): the sin goes into `series`; the **tax** is taken once (4 hearts of maximum health for the first series, 2 for each further one; never leaving less than 4 hearts:
if the wielder's maximum is 14 health, a 4-heart tax takes 6 health, not 8; a wielder at 4 hearts or under loses nothing yet, and the rest is owed if their maximum ever grows);
the first curse form of that sin in the weapon's forms file (lowest tier, first of equals) is learned; despair falls to 150; the weapon waits 3 minutes before it offers again; a
title in the sin's colour ("The Curse of Wrath"), a sound and effects play; the quest hook `hero.curse` fires (argument: the sin id). Further forms of the series are learned the usual way (absorption), now unsealed.

**Decline or ignore**: `/multiverse hero curse decline`, or let it run out. The offer closes, despair falls to at most 300, and the weapon is silent for 3 minutes. Nothing is unlocked.

## 6. While a curse form is worn

"Worn" = the active form (`HeroForms.active(bond)`) has `curse: true` and the weapon is summoned. Detected once a second.

* **Corruption** rises by the form's `corruptionPerSecond` (never less than 2 a second, config `minPerSecond`, so a data mistake cannot make a free curse) and by **+6 per technique** (an ultimate +18),
  both multiplied by `scale`. A weapon's own `HeroCurse.addCorruption(player, n)` calls (the Shield's Blood Sacrifice 80, Megiddo 150, the Sword's Maw 100 ...) stack on top: they are
  the price of those specific techniques, the generic +6 is the price of every technique. When no curse form is worn, corruption **falls 2 a second**.
* **The warning**: when corruption first passes 800 the action bar says the weapon strains and a heartbeat sounds.
* **The downside of the sin** (section 7).
* **Overrun at 1000** (section 8).

How the curse learns of a technique: the dispatcher must call `HeroCurse.onTechnique(player, skill)` after a successful cast and `HeroCurse.refuses(player, skill)` before paying. These two calls are the
only change the curse needs in the core (exact lines in the report). Until they are wired the per-technique corruption, Greed's surcharge, Sloth's lock, Envy's lost technique and the optional
"no techniques in an Overrun" do nothing; everything else works.

## 7. The seven downsides (design; every one bounded and none lethal)

Numbers are the config defaults. `k` is the sin's intensity (`intensity<Sin>`, default 1, 0 switches the downside off, at most 3). The upside of each form (stats, techniques) is the weapon builder's own forms data;
the curse engine adds only the cost.

| Sin | What it does to the wielder | Numbers | Bound (why it cannot kill or lock) | The answer |
|---|---|---|---|---|
| **Wrath** (canon: self-burning curse, blood drawn, healing reduced) | **Burning blood**: each second health falls by max(0.5, 2% of max) x k, with a flame and a hiss every third second; **every heal is cut by 30%** x k | floor at 30% of max health and never under 1 heart (2 health points); a wielder under 1 heart is not drained at all | the floor; the heal cut only reduces | kill fast, stow the weapon to stop the burn, heal after |
| **Pride** (canon: obsession with one's own justice, suggestible, cannot take help) | **Refuses help**: any heal larger than 1 health point at once (a potion, an ally's technique) is cut by 60% x k; natural recovery (1 point at a time) is untouched; on the client the **sight narrows**: a heavier tint that reaches further in | heal cut 0.6, self-heal limit 1.0 | it only ever reduces a heal, never cancels natural recovery | avoid damage, do not rely on healers |
| **Lust** (canon: Temptation, allurement) | **Dragged to the nearest foe**: every half second, if the wielder stands on the ground and a hostile is 3 to 14 blocks away, they are nudged toward it (0.14 speed) | range 14, stops at 3, speed cap 0.45 | never pulls when the wielder sneaks, swims, climbs, flies, rides or glides; never over a drop of more than 2 blocks, into lava, fire, cactus, magma, berries, a campfire, powder snow or a wall | **sneak** resists it; kite from range |
| **Envy** (canon: Ressentiment, jealousy) | every 12 s it **takes one of the form's techniques for 6 s** (the dispatcher refuses it) and **eats one vanilla potion boon** | interval 12 s, lock 6 s | a technique returns after 6 s; only `minecraft:` beneficial effects are eaten (another mod's effect may be a skill's own state) | keep spare techniques, refresh boons |
| **Greed** (canon: cannot hold anything of value) | **Treasure weighs you down**: carrying ingots, gems and blocks of gold, diamond, emerald and netherite slows the wielder by up to 25% (score 128 for the full burden; an ingot or gem 1, a block 9, netherite block 27, nether star 8); **every technique costs 50% more** magicules after the cast | max slow 0.25, surcharge 0.5 | slow clamped to 60%; the surcharge is paid only if the wielder can | leave the treasure at home |
| **Sloth** (canon: lethargy, curse emanation) | **A slow body**: -20% movement speed, -15% attack speed; **a 1.5 s lock after each technique** | slow 0.20 / 0.15, lock 30 ticks | slow clamped to 60% | positional play; do not rely on dodging |
| **Gluttony** (canon: cannot gain experience, kills for experience) | **Devours magicules** (0.4 = 0.2% of the pool a second; since 1.99.0 never the food bar); vanilla experience orbs are not picked up (they stay on the ground); **kills feed**: 2 food points and 4% of max health back (through the healing law) | bite 0.4 (key `gluttonyExhaust`) | it never takes the pool under 10%, and never touches the food bar | keep killing, bring food |

Design notes. Greed was given "treasure weighs you down" instead of the first idea's "drops are pulled in": pulling loot toward the wielder is a convenience, not a cost. Envy's "copies" is the Mirror's
own techniques (the Mirror builder's forms); the engine adds the loss. Wrath's "stronger blows" is the form's own stats. The three that need the dispatcher (Envy's lost technique, Greed's surcharge, Sloth's
lock) are inert until the two calls of section 6 are wired; Envy still eats a boon and Greed and Sloth still slow the wielder without them.

## 8. The Overrun

At corruption 1000 the weapon takes over for **10 to 20 seconds** (a whole number of seconds rolled from the config bounds).

* `HeroCurse.locked(player)` is true: the loan refuses a dismiss, the forms system refuses a change of form, an Embrace and a charm are refused (a message says why). The wielder cannot sneak (the flag is cleared every half second).
* The weapon is summoned if it was stowed.
* **Every ten ticks the wielder swings at the nearest living thing within 4.5 blocks, allies included** (their pets, other players, mobs; not themselves, the mount they sit on, armour stands, creative or spectating players, or
  things that cannot be hurt). The blow is `BalanceLaw.damage(wielder, target, 4.0, 0.5)` through `target.hurt(playerAttack)` (the balance law's EP scaling and mob multiplier apply; a blow on another player is cut to 10% of
  their maximum health, so a friend who steps away survives: two swings a second take a fifth of their health a second). The wielder is NOT moved or turned by the weapon: it cannot walk them over a cliff.
* Techniques still work unless `blocksTechniques` is on (needs the dispatcher call).
* **A kill in an Overrun grieves others**: a wielder who kills someone close to another bonded player adds `overrun_kill` despair to that player.
* When the time is up: the Overrun is saved as over FIRST (so the forms system's `locked` question is false), the form is put back to the **base form** with the forms system's own forced change, `HeroForms.forceActive(player, "base")`
  (found by name when the real forms system is there; it skips the curse lock and the lock after a technique, re-applies the loadout, refreshes an open book and tells the weapon's logic). When it is missing or refuses, the curse sets the form on the bond
  directly (the plain change can refuse within two seconds of a technique and would print an error at that moment), asks for the loadout to be re-applied and gives the weapon's logic `onFormChanged` itself. **Corruption falls to 400**, and **Weakness II and Slowness I**
  last a minute (re-applied each second while the minute runs, so milk does not cure a curse). Quest hook `hero.overrun` fires when it starts (argument: the weapon's item id).
* A wielder who dies in an Overrun ends it at once (corruption 400, no weakness) and the weakness never carries over a death.
* A creative player is never held: an Overrun that began in survival is ended quietly.

## 9. Purification (the stand-in for the Bless Series; the Bless forms themselves stay out of scope)

1. **By itself**: corruption falls 2 a second while no curse form is worn.
2. **Resting** at a Safe Zone of the Mega-Dungeon: a finished rest (`labyrinth/mega/safe/RestEvents` writes `RestState.lastRest`; the curse reads that once a second, so no edit to the Safe Zone code was needed)
   clears **half** the corruption. A rest seen more than 5 seconds after it ended (a login) is remembered but not honoured, and a rest never reaches a wielder the weapon holds.
3. **The Purification Charm** (`nusmp:purification_charm`): right click. Clears **all** corruption and, **the first time** a wielder ever uses one while they owe tax, gives back **2 hearts** of it
   (config: `charmRefundHearts`, `charmRefundUses`). It does nothing for a player without a hero weapon, nothing while the weapon holds them, and is not used up in those cases or when there is nothing to clean.
   Recipe (shaped): `AGA / LTL / AGA` with A = amethyst shard, G = gold ingot, L = lapis lazuli, T = ghast tear (the tear is the nether trip that keeps it a late-early-game item). Icon: a white pearl and gold shield pendant
   with an aqua gem, drawn by `tools/heroweapon/gen_curse_icons.py`.
4. An operator: `/multiverse hero curse purify <player>` (a charm without the item) or `clear <player>` (a full wipe, including the series and the tax).

## 10. Config

`nusmp-heroweapon-curse-server.toml` (registered in `CurseSystem.init`; every key a plain `define`/`defineInRange`, read through `CurseConfig.get`, which falls back to the default while the spec is not loaded;
the defaults are the `DEFAULT_` constants of `CurseTuning`, one source of truth with the tests). Section.key = default (range).

| Key | Default | Range | Meaning |
|---|---|---|---|
| `curse.enabled` | true | | master switch: off = no despair, offers, corruption or Overrun; the modifiers are taken off |
| `despair.offerThreshold` | 600 | 50..1000 | despair at which the weapon whispers |
| `despair.offerSeconds` | 90 | 10..600 | how long an offer stays open |
| `despair.reofferSeconds` | 180 | 10..7200 | silence after a lapse, decline or Embrace |
| `despair.declineDespair` | 300 | 0..999 | despair is lowered to at most this when an offer lapses or is declined |
| `despair.embraceDespair` | 150 | 0..999 | ... when an offer is accepted |
| `despair.decayPerSecond` | 2 | 0..100 | despair that fades per second while calm |
| `despair.calmSeconds` | 20 | 0..600 | seconds without despair before it fades |
| `despair.petDeath` `avatarDeath` `partyDeath` `npcDeath` `betrayal` `nearDeath` `defeat` `bossLoss` `overrunKill` | 150 300 250 200 200 120 200 400 300 | 0..1000 | the amount of each cause |
| `despair.pauseAllyTicks` `pauseBetrayalTicks` `pauseNearDeathTicks` | 100 200 1200 | 0..72000 | pause between two counted reports of a cause |
| `despair.allyRadius` | 32.0 | 4..128 | blocks within which an ally's death is felt |
| `despair.nearDeathShare` | 0.15 | 0.02..0.5 | health share counted as near death |
| `corruption.perTechnique` | 6 | 0..500 | per technique with a curse form worn (needs the dispatcher call) |
| `corruption.ultimateFactor` | 3 | 1..20 | an ultimate adds this many times more |
| `corruption.minPerSecond` | 2 | 0..100 | a curse form never rises slower |
| `corruption.decayPerSecond` | 2 | 0..100 | fall per second while no curse form is worn |
| `corruption.scale` | 1.0 | 0..10 | multiplier of every gain; 0 = curse forms cost no corruption |
| `overrun.minSeconds` `maxSeconds` | 10 20 | 1..300 | length bounds |
| `overrun.endCorruption` | 400 | 0..999 | corruption left after an Overrun |
| `overrun.weakSeconds` | 60 | 0..3600 | Weakness II and Slowness I after it |
| `overrun.reach` | 4.5 | 1.5..8 | reach of the forced swings |
| `overrun.rawDamage` | 4.0 | 0..30 | raw damage per swing, before the balance law |
| `overrun.playerShare` | 0.10 | 0..1 | share of another player's maximum health one swing may take |
| `overrun.hitsPlayers` | true | | whether other players can be struck |
| `overrun.blocksTechniques` | false | | whether techniques are refused during an Overrun (needs the dispatcher call) |
| `tax.firstHearts` `nextHearts` `minHearts` | 4 2 4 | 0..9, 0..9, 1..10 | the tax stages and the floor of maximum health in hearts |
| `purify.charmRefundHearts` `charmRefundUses` | 2 1 | 0..9, 0..100 | what the charm gives back, and for how many uses |
| `purify.restClearShare` | 0.5 | 0..1 | share a finished rest clears |
| `sins.intensityWrath` `Pride` `Lust` `Envy` `Greed` `Sloth` `Gluttony` | 1.0 each | 0..3 | strength of each sin's downside |
| `sins.wrathDrainShare` `wrathDrainMin` `wrathFloorShare` `wrathHealCut` | 0.02 0.5 0.30 0.30 | | Wrath |
| `sins.prideHelpCut` `prideSelfHealMax` | 0.60 1.0 | | Pride |
| `sins.lustRange` `lustMinDistance` `lustPull` | 14 3 0.14 | | Lust |
| `sins.envyIntervalSeconds` `envyLockSeconds` | 12 6 | | Envy |
| `sins.greedBurdenFull` `greedMaxSlow` `greedSurcharge` | 128 0.25 0.5 | | Greed |
| `sins.gluttonyExhaust` `gluttonyFoodFloor` `gluttonyKillFood` `gluttonyKillHeal` `gluttonyBlocksXp` | 0.4 3 2 0.04 true | | Gluttony |
| `sins.slothSlow` `slothAttackSlow` `slothLockTicks` | 0.20 0.15 30 | | Sloth |

`nusmp-heroweapon-curse-client.toml` (each player): `curse.overlay`, `vignette`, `vignetteMax` (0.8, at most 0.92), `bar`, `barX` (6), `barBottom` (120: stacked just above the tallest the core's HUD panel gets, which sits at the bottom left: 92 pixels with five techniques listed, 103 with its repelled line too), `whispers`, `heartbeat`.

## 11. Commands (`/multiverse hero curse ...`, permission 2 unless said)

| Command | Does |
|---|---|
| `show [player]` | three lines: weapon, level, form; despair, corruption, offer, Overrun, weakness; series, tax, charm refunds. Level 0 for yourself, 2 for another player |
| `embrace`, `decline` | level 0, yourself: answer the open offer |
| `despair <player> <0..1000>`, `corruption <player> <0..1000>` | set the meter (1000 starts a real Overrun at once) |
| `unlock <player> <sin> [tax]` | embrace a series with no offer; the first curse form is learned; with `tax` the stage's hearts are taken, without it the series is a gift |
| `offer <player> <sin>` | the weapon whispers that sin at once |
| `clear <player>` | wipe the record (series, tax, despair, corruption, offer, Overrun, weakness); a worn curse form returns to base |
| `purify <player>` | a Purification Charm without the item |

All of them refuse a player with no hero weapon with that reason.

## 12. Network, client

* `nusmp:hero_curse_sync` (server to the wielder only; `CurseSyncPayload`): despair, corruption (varints 0..1000), the wire numbers of the offered sin and of the sin of the ACTIVE form when it is a curse form, the weapon out or not (0 none, enum position + 1; so `HeroCurse.active` and `activeSin` answer the same on both sides), the ticks the Overrun and
  the offer have left, the series bit mask. Numbers only, no text. Sent on login, respawn, dimension change, bind, release, every transition, every second while something counts down, otherwise only on change and every 10 s.
* `nusmp:hero_curse_act` (client to server; `CurseActPayload`): 0 embrace, 1 decline. The server re-validates everything.
* Both registered optional, version "1". Client state: `CurseView` (common package, no Minecraft types), so `HeroCurse.active/locked/corruption/despair` also answer for the local player of a client.
* `CurseClient`: the key, the GUI layer `nusmp:hero_curse_overlay` above everything, the heartbeat (from 600 corruption, 60 beats a minute rising to 150), the client config.
* `CurseOverlay` (plain `fill` calls, no textures): the tint at the edges in the sin's colour (up to 0.8 opacity; Pride's is heavier and reaches further in; an open offer glows faintly; an Overrun floods the screen). It is drawn as
  nested rectangular frames, a hundred or so thin ones in one batch (`GuiGraphics.drawManaged`), so it falls off smoothly; `VignetteMath.layout` keeps the frames from ever going past the middle of the screen (frames that did would leave a bare strip across it, a line),
  the bar at the left, stacked above the core's HUD panel, with a mark at 60% (where the heartbeat starts) and the despair line under it, the offer panel under the crosshair with the hold gauge and the seconds left, the whisper lines in the
  middle (fade in, hold, fade out, every 8 s). It does not draw over open screens or with the GUI hidden, and a failure switches it off with one log line.

## 13. For the other systems

* `HeroCurse.active/locked/corruption/despair` (contract), plus `hasSeries`, `activeSin`, `tax`, `onTechnique`, `refuses` (new), `addDespair`, `addCorruption` (contract).
* `CurseSystem.embrace(ServerPlayer)` / `decline(ServerPlayer)` (booleans), `CurseSystem.creativeItems()`.
* Quest hooks (`NusmpEvents.fire(player, key, arg)`): `hero.curse_offer` (the sin id), `hero.curse` (the sin id), `hero.overrun` (the weapon's item id). The design listed `hero.curse (weapon, sin)`; the quest door
  takes one argument, and the weapon is implied by the bond, so the argument is the sin.
* Calls the lead must add to the core's dispatcher (`HeroUse`): see the report.
* Weapon builders: a curse form needs `curse: true`, a `sin` and a `corruptionPerSecond` of at least 1 (the engine raises it to 2); the sin's downside is the engine's (the Spear builder's and the Sword builder's docs already
  say so); `HeroCurse.addCorruption` calls for specific costly techniques stack with the generic per-technique corruption.

## 14. Files

| File | Why it exists |
|---|---|
| `heroweapon/curse/Cause.java` | the closed list of causes of despair, with default amounts and pause groups |
| `CurseTuning.java` | every number the pure machine needs in one plain bag; the defaults are the single source of truth |
| `CurseState.java` | the whole curse of one wielder as plain data |
| `CurseMachine.java` | the pure state machine (despair, offer, Embrace, tax, corruption, Overrun, weakness, cleansing) |
| `SinMath.java`, `OverrunMath.java` | the bounded arithmetic of the seven downsides and of the Overrun |
| `HoldCount.java`, `KeyThree.java` | the count of looks for the third-key hold (pure, tested) and the by-name call to the core's `HeroKeys.isHeld` |
| `CurseCandidates.java` | which sin a weapon offers, and which form the Embrace teaches |
| `CurseWire.java`, `CurseView.java` | the wire encoding of sins and series, and the client's picture of the curse |
| `CurseText.java`, `CurseMessages.java` | every word of the curse (English table, whispers) and how it is sent |
| `CurseConfig.java` | the server config and the conversion to `CurseTuning` |
| `CurseStore.java` | the only reader and writer of `bond.extra("curse")` |
| `CurseService.java` | the orchestration: one place for what happens to a wielder, with the clock passed in |
| `CurseEffects.java` | the world effects: the tax modifier, the seven downsides, the dispatcher hooks |
| `CurseAllies.java` | who matters to whom (pets, Avatar, party, arc friends) |
| `CurseEvents.java` | the game-bus handlers and the bond listener |
| `CurseSync.java`, `CurseSyncPayload.java`, `CurseActPayload.java`, `CurseNet.java` | the two packets and their registration |
| `CurseAdmin.java`, `CurseCommands.java` | the operator's tools and the command tree |
| `CurseItems.java`, `PurificationCharmItem.java` | the one new item |
| `CurseSelfTests.java` | `hero.curse`, the in-game self-test |
| `HeroCurse.java`, `CurseSystem.java` | the door and the entry point (replace the stubs of the contract; every public signature kept) |
| `client/heroweapon/curse/CurseClient.java`, `CurseOverlay.java`, `CurseClientConfig.java`, `VignetteMath.java` | the client |
| `tools/heroweapon/gen_curse_icons.py`, `assets/.../textures/item/purification_charm.png`, `models/item/purification_charm.json`, `data/nusmp/recipe/purification_charm.json` | the charm's art and recipe |

## 15. Tests

JUnit (pure, run locally with the shim, mutation-checked once: removing the max-health floor from `appliedTaxHp` makes `theTaxNeverTakesMoreThanLeavesFourHearts` fail):
`CurseMachineTest` (despair pauses and clamp, fade, the offer, lapse, decline, nothing unlocks by itself, the tax stages and floor, the Embrace refusals, corruption, the Overrun's start, length and end, death, the charm, the rest, the wipe),
`SinMathTest` (no drain under the floor for any health/maximum/share, heals only reduced, the stomach floor, slows clamped, treasure weights, the pull geometry, Sloth's lock, Envy's pick), `CurseCandidatesTest`,
`CurseTextTest` (every sin has the same number of whispers, ASCII only, argument counts of every format string), `OverrunMathTest`, `CurseWireViewTest` (round trips and hostile packets), `HoldCountTest`, `VignetteMathTest`.
In game: `hero.curse` (`/multiverse mega selftest hero.curse`): see `CurseSelfTests` (a stand-in player bound to the Vassal Hammer; the real service with an explicit clock).

## 16. Numbers worth knowing

* A 20-health wielder: first series 20 -> 12 health, second -> 8, third and later stay at 8 (4 hearts).
* Time to an Overrun by the corruption rate alone: 3 a second 5.5 minutes, 6 a second 2.8 minutes, 10 a second 100 seconds; every technique brings it closer (6, or 18 for an ultimate), and the weapons' own costly techniques add their prices.
* From 400 after an Overrun to 0 by decay alone: 200 seconds. A rest at a Safe Zone halves what is left.
* Despair: four pet deaths (with 5 s between them), or a death to a boss (200 + 400), reach the threshold.

## 17. What is NOT verified (nothing here was run in the game; there is no compiler for the whole project)

* The code was type-checked against the project's API stubs (the six errors left in my files are the known stub artefacts: `new KeyMapping`, `new MobEffectInstance`, `new Clientbound*Packet` are "abstract" in the stubs;
  `client/song/SongClient.java` and `DjinnKeys.java` show the same `KeyMapping` error and compile in CI). A deliberately wrong call is caught by the same check (negative control). CI compiles the real thing.
* Not run: every handler, the whole `hero.curse` self-test, the real overlay drawing (`GuiGraphics`, the game's font), the key hold, the packets, the commands. What WAS looked at: the icon (x16) and a mock picture of the overlay
  (an AWT canvas that runs the real `VignetteMath` numbers and a transcript of the draw routines, with a stand-in font and a stand-in for the core's HUD panel); it found a bare strip across the middle of the screen when Pride's frames went past it,
  which `VignetteMath.layout` now prevents (tested), and it showed the steps of the tint, which the three-fold thinner bands remove.
* Assumptions about vanilla behaviour: that natural recovery heals 1 point per event (Pride's "own recovery" limit); that cancelling `PlayerXpEvent.PickupXp` simply leaves the orb; that `hurtMarked` with a changed
  `deltaMovement` pushes a player's client (the way knockback does); that `FakePlayer` has a connection that swallows packets (the self-test and `CurseMessages` avoid it anyway); that a transient attribute modifier on
  `MAX_HEALTH` is cleared on relog and re-applied by `onEnter`; that `GuiGraphics.drawManaged(Runnable)` (it exists in the stubs) batches the vignette's rectangles into one draw and keeps the drawing order.
* Not done by design: the Bless Series forms; hitting the wielder's own aim (the swings do not turn the wielder); a gauge for the third-key hold; per-weapon downsides beyond the seven sins' own.
* Dependent on the core: the two dispatcher calls (section 6); the loan refusing a dismiss while `locked` (already in its brief); the forms system's Embrace button (it already reflects `CurseSystem.embrace`);
  the third-key door (needs `HeroKeys.isHeld(ServerPlayer, HeroTrigger)`, found by name; its absence is silent); the forms system's `HeroForms.forceActive(ServerPlayer, String)` (found by name, with a fallback).
