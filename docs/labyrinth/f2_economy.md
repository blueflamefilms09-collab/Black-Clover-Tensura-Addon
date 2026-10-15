# F2: monster economy, dungeon hunger and gloom (1.36.0)

Package `labyrinth/dnd/economy/`. Nothing here was run in game; the pure classes are unit tested, the event classes were type-checked.

## Why this exists
Two ideas from the owner's brief. The anime half (DanMachi, Delicious in Dungeon): the dungeon is an ecosystem and loot is physical, a
monster leaves a Magic Stone you can sell and a cut of meat you can cook, not a number on a bar. The D&D half: a dungeon is a resource clock,
torches and rations decide how deep you can go. The two meet in one loop: the meat is the ration, the stone is the money, the dark and the
hunger are the clock that makes you need both.

## Classes (the WHY of each)
* **EconomyMath** (pure). The drop table as numbers so it is tested without a server. It does not invent stone values: it calls the Living
  Dungeon's `StoneTable` (same grades, same ranges, Looting adds to the top of the range), so a stone from the Labyrinth equals one from the
  old caves and one place balances both. New here: a chance gate (a kill may leave nothing, which makes a stone an event) scaled by
  toughness (tier 1 x0.8 ... juggernaut x1.1), the rule that a death no player caused is only `StoneTable.ambient` (one in four, one stone,
  so a monster dropped into the void is not a farm), and the meat table: entity-id words choose the raw cut (slime -> jelly, armored/shelled ->
  armor flank, snake/spider/basilisk -> fillet, wolf/boar -> haunch, anything else plain), fleshless creatures (skeleton, blaze, golem, spirit)
  leave none, tier-3 or heavier leave two, Looting adds 5 points of chance per level and sometimes one more cut.
* **EconomyDrops**. The two global NeoForge events: `LivingDropsEvent` adds item entities, `LivingExperienceDropEvent` sets the orbs to 0 (config
  `noExperience`). Global events, not per-mob code, so a monster from any mod standing in a Spire side chamber pays the same. Scope: level is the
  Labyrinth, the mob is a `Mob` and an `Enemy`, and not a boss. Boss = `BudgetedBoss`, the `nusmp_peak_boss` tag the Peak Altar sets, the
  `nusmp:floor_warden` id, or the `c:bosses` entity tag (other mods' bosses follow it), so bosses keep their own loot (relics, Shadow Key).
  Toughness is read from the BASE max-health attribute, so F3's health buffer (a modifier) cannot inflate the payout. Drops are added to
  the vanilla list, never replace it. Items: Tensura `tensura:magic_stone`, `low/medium/high_quality_magic_crystal` (looked up by id at drop
  time; a missing crystal falls back to the stone, a missing stone to nothing, nothing throws) and `DungeonFood.rawFor(kind)` (the mod's own
  `raw_slime_jelly`, `raw_armor_flank`, `raw_basilisk_fillet`, `raw_direwolf_haunch`, `raw_monster_meat`). Raw meat stays toxic (magicule
  sickness): it must be cooked or purified (`purified_monster_meat`, the seared/roast meals), which is the survival loop. Looting comes from the
  killer's main hand, killer = the damage source's player, else the kill credit if it is a player. `StoneDrops` was left untouched (it is
  package-private); the same ids are resolved again here.
* **GloomClock** (pure). Two small state machines. Gloom: seconds in the dark climb by 1, fall by 2 in the light, cap at twice the allowed
  seconds, WARNING at 3/4 of the allowance, GLOOM at the full count; the 2x recovery means one step past a torch does not wipe the
  warning but a real rest does, and the cap bounds how long the way back is. Hunger: `extraExhaustion` is the player's own exhaustion spent
  since the last sample times (drain - 1), with the vanilla wrap at 4 handled; the first sample only records, and the amount we add is
  settled so it is never counted twice. Standing still costs nothing extra.
* **LightItems** (pure). Id test for carried lights (torch family, lanterns, `ki_resonance_lantern`, words like lamp/glow/torch) so a mod's new
  lantern counts without a list edit. The handler also accepts any `BlockItem` whose block emits light.
* **DungeonHunger**. The per-player server tick (`PlayerTickEvent.Post`, once a second). Clocks live in a UUID map (not saved: relog = clean
  slate), dropped on logout and when the player leaves the dimension or is creative/spectator. Dark = block light at the eyes <= `gloomLight`
  and no light in either hand. Gloom applies vanilla Darkness plus Weakness I, refreshed each second so they end 5 s after you reach light
  (no state to undo), with an actionbar line; the warning line shows once. Hunger multiplies exhaustion by `hungerDrain`; at 6 food or less
  Slowness I. Effects are created with hidden particles; no particle is emitted by this builder at all, so the Rukh API is not used.
* **EconomyRegistry**. One `register(IEventBus gameBus)` that adds the four listeners. No registry entries (stones are Tensura's, meat is
  `DungeonFood`'s), so no ordering constraint against `NUItems.ITEMS`.

## Config (existing `DndConfig`)
`drops`, `stoneChance`, `meatChance`, `noExperience`, `gloom`, `gloomLight`, `gloomSeconds`, `hungerDrain`. No new key was needed.

## Files
Java: `EconomyMath`, `GloomClock`, `LightItems`, `EconomyDrops`, `DungeonHunger`, `EconomyRegistry`. Tests: `EconomyMathTest`,
`GloomClockTest` (pure). Lang fragment `f2.lang.json` (three actionbar lines). No JSON, NBT or PNG files.
