# Hero weapons: acquisition, the Calling Gem and the Vassal Seals (1.62.0)

**Honest first line: acquisition is my default.** The owner did not say how a player comes to hold a hero weapon, so this is a design inside a small box: a
survival route that feels like the series (a hero is summoned and the weapon CHOOSES them), cheap to build, hard to break, with admin routes for testing.
The owner may well want drops, an altar or a quest reward instead; section 11 lists what would change. Nothing here was run in the game (see section 10).

## 1. What it is for, and the canon it follows

In the series the Legendary (Holy) Weapons are spirits that pick their own heroes: the locals cannot summon just anyone, they perform a ritual (with fragments of
holy relics, at set times) that brings a person from another world, and the weapon accepts or does not. A hero keeps the weapon until death; then it disappears and
waits for the next one. The Vassal Weapons (the Seven Star Weapons and the others) can come without a summoned candidate: natives can wield them if they prove
themselves worthy, and the weapons roam the world or wait in a place for such a person. This system turns that into play:

* the **Holy Calling Gem** is the summoning ritual in one item: held, it makes ONE of the four Legendary Weapons answer (random among the free ones);
* a **Vassal Seal** is a weapon that comes to someone worthy: held, it makes THAT Seven Star or Vassal Weapon answer;
* the weapon chooses; the player never picks. (A player who wants a specific Holy Weapon asks an operator: `/multiverse hero bind`.)

Sources (fandom wiki search summaries only; the pages themselves could not be opened from the build machine, so every canon claim here is "from a summary"):
https://shield-hero.fandom.com/wiki/Legendary_Weapons , https://shield-hero.fandom.com/wiki/Vassal_Weapons , https://shield-hero.fandom.com/wiki/Heroes ,
and for the jewel colours (Shield green, Sword blue, Spear red, Bow yellow; web novel pages)
https://shield-hero.fandom.com/wiki/Legendary_Shield_(Web_Novel) , `/Legendary_Sword_(Web_Novel)` , `/Legendary_Spear_(Web_Novel)` , `/Legendary_Bow_(Web_Novel)`.
Design, not canon: that the choice is a held ritual, the recipes, the amber and aqua tints of the Seals.

**Brief versus design.md.** design.md section 10 says a Seal is "crafted per weapon from a Calling Gem plus that weapon's signature material". My brief says a Holy
Gem is NOT required (the series: vassal weapons choose natives) and a Seal needs a Gem Fragment instead. design.md says the details are the gem builder's, so the
brief's version is what is built; one line to change if the owner prefers the other (add the Holy Calling Gem to `GemRecipes`'s Seal rows and regenerate).

## 2. The items (nineteen, one registry of their own)

| item id | what | stack | rarity |
|---|---|---|---|
| `nusmp:calling_gem` | the Holy Calling Gem (a cut jewel in the four Holy colours) | 1 | epic |
| `nusmp:gem_fragment` | a shard of a calling gem; every Seal is crafted from one | 64 | uncommon |
| `nusmp:vassal_seal_<weapon>` (17) | `hammer claw axe whip staff gauntlet projectile` (Seven Star, amber rim), `carriage fan scythe katana book mirror instrument harpoon sewing_set chain` (Vassal, aqua rim) | 1 | rare |

All are listed by `GemSystem.creativeItems()`, which `HeroWeaponSystem.creativeItems()` already adds to the creative tab (no change to `NUCreativeTab` is needed).
The three weapons with a canon holder name them in the Seal's tooltip (Carriage: Fitoaria, Fan: Glass, Scythe: L'Arc Berg).

**Why seventeen Seal items and not one item with a data component** (the arc spawn egg's design, which the brief named first, with "choose the simpler and robust
one and explain"): a component-carrying stack is ONE item to a recipe viewer, so seventeen Seals would be seventeen look-alike recipes under one name; a quest
reward or a loot table could not name one as a plain item id; the creative tab takes items, not stacks, from the hero system, so the seventeen could not be listed
without a new line in a lead-owned class; and the look would need either `custom_model_data` kept in step with the component by hand or a client-side item property.
Separate items need none of that: a loop over the `HeroKind` enum registers them (a weapon appended later gets its Seal item at once), the model of each is a plain
`item/generated`, and a recipe's result is just an item id. The price is eighteen more registry entries. If the lead prefers the component, `CallingGemItem.kind()`
is the only place that would read it.

## 3. The Choosing (the ritual)

Hold right click with a gem for `ritualSeconds` (default 5), standing still, on firm ground, in the open (at least `openAbove` blocks of clear space over your head:
a cavern is fine, a sky is not required). The vanilla item-use hooks drive it, exactly as the Hourglass of the Final Hour and the Chronicle Sigil do.

**To begin** (the first condition that fails is told, in this order; `GemRules.decide`): the survival route is open for you (`recipes`, or you are an operator or in
creative) / you carry no hero weapon yet / a gate does not refuse the weapon / for the Holy gem, at least one Legendary Weapon is free / you are not riding / your head
is not under water / you stand on the ground / your horizontal speed is at most `speedTolerance` blocks per tick / the space above is clear. "Free" means the core's
gates (`HeroBonds.refusal`, which carry its limit and its one-holder rule) allow it AND, with `holyGemFreeOnly`, no online player holds it.

**While held** (`GemRitual`, one tick at a time on the server): the ritual breaks, in this order of priority, if something hurt you (any damage that got through), if
you stand further than `moveTolerance` blocks from where the circle was drawn (also: another dimension), if the space above is covered (looked at five times a second),
or if you let go (`released`). A broken ritual keeps the gem and says why; a completed one never changes its mind.

**At the end** (`GemChoosing.finish`): the route, the carried weapon and the gates are checked once more (an operator may have bound you meanwhile). A Seal tries its
weapon; the Holy gem tries the free Legendary Weapons in a random order, so two players finishing in the same tick cannot take the same one. The bind is
`HeroBonds.bind`, which applies the core's rules and refuses with a reason. **Only when a bind succeeds is the gem spent** (`ItemStack.consume`: a creative player keeps
it), and then the title ("The Legendary Shield has chosen you.", the weapon's own colour; the subtitle names the canon holder when there is one), a chat line, the pillar
of light and the sounds. A refusal keeps the gem and tells the player ("The legendary weapons have already chosen their heroes." when none is free). The weapon is put
in the hand one tick later with `HeroSummon.summon` (the gem occupied that very slot and the game is still finishing the item use), not for creative or spectator players
(the core refuses them). The base form is learned by the forms system's own bind listener; this class learns nothing.

**What the player sees** (`GemFx`; `effects` switches the circle, motes and sounds off, the words stay): a holy sigil turning under the feet, drawn with the Lucius
boss effects' `DIVINE_SIGIL` for exactly the ritual's length: its closing ring is the time left, light rays stand up in the last third; the countdown in the action bar;
motes gathering each half second in the weapon's colour, and for the Holy gem walking through green, blue, red and yellow one colour a second (four weapons listening);
at the end `DIVINE_PILLAR`, `LIGHT_FLARE` (and four flares in the four jewel colours for the Holy gem) and the title. The screen is never shaken.

## 4. Recipes (19 files in `data/nusmp/recipe/`, written by `tools/heroweapon/gen_gem_recipes.py` from the table in `GemRecipes.java`)

Every ingredient is a vanilla item a survival player can always get (no Tensura item, nothing an operator must give). The Holy gem is the rarest thing in the route
because it chooses among the four Legendary Weapons; a Seal is cheaper (a Gem Fragment plus one signature material), because the Vassal Weapons come to natives.

**Holy Calling Gem** (shaped; `ENL / NSN / RNG`): emerald block (green, the Shield), lapis block (blue, the Sword), redstone block (red, the Spear), gold block (yellow,
the Bow) at the corners, four netherite ingots on the edges, a nether star in the middle. The star is the late-game gate (the Wither), the netherite the Nether's top
metal. Hard, not absurd: a beacon's worth of star plus four ingots and four blocks.

**Gem Fragment** (shapeless, one per craft): 4 amethyst shards + a gold ingot + an ender pearl (the crystal, the setting, the call that crosses worlds).

**Vassal Seals** (shapeless: one Gem Fragment plus):

| Seal | Class | plus | Why |
|---|---|---|---|
| Vassal Seal: Hammer | Seven Star | 2 x iron_block, stick | A hammer is a block of iron on a stick; two blocks make a head worth the name. |
| Vassal Seal: Claw | Seven Star | 2 x phantom_membrane, 2 x bone | Claws of bone with the hide of a night hunter drawn over them; phantoms only come to those who skip sleep. |
| Vassal Seal: Axe | Seven Star | diamond_axe, iron_block | The axe's own namesake given up whole, a diamond axe, with an iron block for the weight behind the blade. |
| Vassal Seal: Whip | Seven Star | 2 x lead, 2 x leather | A lash is braided hide and cord; a lead (four strings and a slime ball) is the nearest thing to a whip the world makes. |
| Vassal Seal: Staff | Seven Star | 2 x blaze_rod, amethyst_block | A rod of the Nether's fire with a crystal set at its head. |
| Vassal Seal: Gauntlet | Seven Star | 4 x iron_ingot, 2 x leather | An armoured glove: plates of iron riveted onto leather. |
| Vassal Seal: Projectile | Seven Star | 6 x iron_nugget, 2 x flint | Throwing stars are hammered from iron nuggets and edged with flint. |
| Vassal Seal: Carriage | Vassal | minecart, saddle, 4 x feather | A Filolial queen's coach: a cart on wheels, a saddle for the bird that draws it, and its feathers. |
| Vassal Seal: Fan | Vassal | 3 x feather, 3 x paper, 2 x bamboo | Ribs of bamboo, paper folded between them, and feathers for the wind. |
| Vassal Seal: Scythe | Vassal | diamond_hoe, 2 x soul_sand | The hoe is the farmer's scythe; souls drawn from the Nether's sand make it a reaper's. |
| Vassal Seal: Katana | Vassal | 3 x iron_ingot, 2 x charcoal, flint | Tatara steel is iron folded in charcoal fire, and flint is the edge it is ground against. |
| Vassal Seal: Book | Vassal | written_book, 2 x ink_sac, bookshelf | A written book for the words, ink for the next page, and a whole bookshelf for the library the book remembers. |
| Vassal Seal: Mirror | Vassal | 2 x tinted_glass, 2 x gold_ingot, ender_pearl | Glass darkened with amethyst, a gilded frame, and a pearl of the End so that it shows what is not here. |
| Vassal Seal: Musical Instrument | Vassal | goat_horn, 2 x note_block | The goat horn is the only instrument the wild gives up, and note blocks tune it. |
| Vassal Seal: Harpoon | Vassal | fishing_rod, 3 x prismarine_shard, 2 x iron_ingot | A barbed iron head on a line, with shards from the ocean monument's keepers. |
| Vassal Seal: Sewing Set | Vassal | shears, 3 x string, 2 x white_wool | Shears cut the cloth, string is the thread, wool is the cloth. |
| Vassal Seal: Chain | Vassal | 4 x chain, iron_block | Four links of the heaviest chain the world makes, and a block of iron to drag. |

The table is `GemRecipes.java`; edit it and run `python3 -B tools/heroweapon/gen_gem_recipes.py` (`--check` verifies, `--doc` reprints the table above).
`GemRecipesTest` checks: all seventeen weapons covered, no two Seals with the same ingredients, at most nine slots, every vanilla id in the game's own item list
(`src/test/resources/heroweapon/vanilla_item_ids.txt`, generated from the game's `Items` class), and the shipped JSON equal to the table.

**The switch `recipes`.** A recipe is loaded before the server config is, so no recipe condition can read a server config reliably; the robust form is a runtime rule.
`recipes = true` (default): anyone can use a gem. `recipes = false`: the gems still exist and still craft, but only operators (permission 2) and creative players can use
them, so a crafted gem does nothing for a survival player; a player who crafts one is told so at once (`GemEvents.onCrafted`). To remove the recipes themselves, override
`data/nusmp/recipe/calling_gem.json`, `gem_fragment.json` and `vassal_seal_*.json` in a data pack.

## 5. Config (`nusmp-heroweapon-gem-server.toml`, section `[gem]`; plain `define` / `defineInRange`, read through `GemConfig.get`)

| key | default | range | meaning |
|---|---|---|---|
| `ritualSeconds` | 5 | 1..60 | how long the use key is held |
| `recipes` | true | | the survival route (section 4) |
| `moveTolerance` | 1.5 | 0..16 | blocks the player may drift from the circle's centre |
| `speedTolerance` | 0.1 | 0..1 | fastest horizontal speed (blocks per tick) at which the ritual may begin (walking is about 0.22, sneaking 0.065) |
| `openAbove` | 4 | 0..16 | blocks of clear space above the head; 0 switches the rule off |
| `holyGemFreeOnly` | true | | the Holy gem only chooses Legendary Weapons nobody online holds (the gates always apply) |
| `effects` | true | | the circle, motes, pillar and sounds |

## 6. Commands (`/multiverse hero gem ...`, permission 2; `GemCommands`, attached by the core's `HeroCommand`)

* `give <player> holy`: a Holy Calling Gem.
* `give <player> seal <weapon>`: the Seal of one of the seventeen weapons (`hammer`, `scythe`, `sewing_set` ...; Holy weapons are refused with a hint).
* `give <player> fragment [count]`: Gem Fragments (to test the recipes).
* `holders`: every online player with a bond (weapon, level, form) and the weapons the core's `nusmp_hero_registry.dat` gives to players who are offline. The
  file's layout belongs to the core: I read its work-in-progress class `HeroRegistry` (saved as `{holders: {<weapon id>: [<uuid text>, ...]}}`) and the reader accepts
  that and three other shapes, tolerantly (`RegistryScan`). Once the core is merged, `GemHolders.offline` can call `HeroRegistry.holdersOf` and `HeroRegistry.nameOf`
  instead (the core's own docs name the gem's `holders` command as a user of them); the file reader is only there so this branch builds without the core.

`bind`, `release`, `status` and the rest are the core's.

## 7. Quest hooks

None new. `hero.bound` (weapon) is fired by the core inside its bind listener for every route; **this ritual does not fire it**, so one bind is never counted twice. If
the core's report says it does not fire it there, the single line to add in `GemChoosing.finish` after `stack.consume(1, p)` is
`com.newuniverse.nusmp.quests.NusmpEvents.fire(p, "hero.bound", bound.itemId());`.

## 8. Self-tests and unit tests

In the game (`/multiverse mega selftest`, the CI smoke test expects the prefix `hero`): `hero.gem` (the nineteen items registered with the right weapon each, seventeen
distinct Seals, the nineteen recipes loaded by the real recipe manager with the table's ingredients and the right result, the registry reader on the core's layout and the other shapes it accepts, and on noise)
and `hero.gem.ritual` (a stand-in player: the ritual breaks by a step away, a blow, a covered sky and letting go; a closed route starts nothing; a Seal completes, binds
its weapon and is spent; a gem that must refuse is kept; the Holy gem binds a Legendary Weapon or, when none is free, keeps the gem; everything is released again).
JUnit (`WT=<worktree> KEY=hgem bash $S/mega/jt.sh <class>`): `GemRitualTest`, `GemRulesTest`, `GemCatalogTest`, `GemMessagesTest`, `GemRecipesTest`, `GemAssetsTest`, `RegistryScanTest`
(54 tests, run locally with the shim of `jt.sh`; twenty-five deliberate mutations of the pure classes and the shipped data were each caught by at least one of them: a
tick off by one in the ritual, the order of the checks swapped, an inclusive limit made exclusive, a typo in an ingredient id, two Seals with one ingredient set, a recipe
file that drifts from the table, a Holy Weapon given a Seal, a changed canon colour, a reworded owner line, a lost refusal message, two identical icons, and
seven ways of making the registry reader less strict).

## 9. Files and why

| file | why |
|---|---|
| `gem/GemSystem` | the one entry point: config, item register, listeners, self-tests; `creativeItems()` |
| `gem/GemConfig` | the numbers a server owner may change |
| `gem/GemCatalog` (pure) | the nineteen ids, names, colours, derived from the `HeroKind` enum |
| `gem/GemRecipes` (pure) | the ingredient table and the reasons |
| `gem/GemRules` (pure) | which condition refuses a ritual first, the Holy shuffle, the common-refusal test |
| `gem/GemRitual` (pure) | the ritual's state machine |
| `gem/GemMessages` (pure) | every English sentence, the fallback and the source of the lang fragment |
| `gem/GemText` | builds a translatable line with that fallback |
| `gem/GemItems`, `CallingGemItem`, `GemFragmentItem` | the items and the held use |
| `gem/GemChoosing` | gathers the facts, steps the ritual, binds and spends |
| `gem/GemFx` | the circle, the countdown, the title, the sounds |
| `gem/GemEvents` | hurt, tick, logout, respawn, crafted, server stopped |
| `gem/GemCommands`, `GemHolders` | the commands; `GemHolders` finds and decodes the core's registry file |
| `gem/RegistryScan` (pure) | the tolerant reader of that file's layouts (the core's own and three more accepted, noise refused) |
| `gem/GemSelfTests` | `hero.gem`, `hero.gem.ritual` |
| `tools/heroweapon/gen_gem_icons.py` | the nineteen 16x16 icons and flat models (a rune disc per Seal: amber rim for Seven Star, aqua for Vassal, a pictogram of the weapon inside) |
| `tools/heroweapon/gen_gem_recipes.py` | reads `GemRecipes.java`, writes the nineteen recipes |

## 10. What is unverified (honest list)

* Nothing was compiled as a whole or run in the game. The files were type-checked against the API stubs (three known stub artifacts only: the abstract
  `Clientbound*Packet` constructors); pure logic and the data files are covered by JUnit run locally; the two self-tests have never run.
* The held-use flow copies the pattern of the Hourglass and the Chronicle Sigil; spending the gem in `finishUsingItem` and returning the same (spent) stack follows how
  vanilla food does it. If the game ever clears the hand differently, the visible symptom is a gem left in the hand after a bind (the bind itself is already done).
* The ritual circle uses `DIVINE_SIGIL`, `DIVINE_PILLAR` and `LIGHT_FLARE` as their layers' code describes them (read, never seen); the seed is made even on purpose so the
  full form is drawn. Sounds are vanilla `SoundEvents`.
* The start speed is `x - xo` read in the network phase; I believe the server sets `xo` at the start of the player's tick, so that is the last tick's movement.
* The registry reader was written against the core's work-in-progress `HeroRegistry` (uncommitted in its worktree when read), so the layout may still change; the reader
  accepts four shapes and the in-game test proves it reads each of them, not that it reads the core's final file.
* `HeroSummon.summon` one tick after the bind relies on the core's loan treating the just-emptied slot as free; creative players get no summon (the core refuses it).
* On a dedicated server whose `ritualSeconds` differs from the default, the client may show the drawn-bow pose for the default length; the server's timer decides what happens.
* The icons were looked at as enlarged pictures and in an inventory-slot mock-up, never in the game.
* The balance of the recipes is my judgement of cost, not a measurement.

## 11. What the owner may want instead (not built)

* **Drops**: a Gem Fragment or a Seal as a rare drop of the Mega-Dungeon bosses or arc bosses (the recipes would stay as the second route); one line in the drop code of each.
* **An altar**: a block that runs the same ritual for whoever stands on it (the ritual is already independent of the item: `GemChoosing.begin/step/finish` take a player).
* **A chosen weapon**: let an item in the off hand bias the Holy gem (an emerald for the Shield, lapis for the Sword ...); `GemChoosing.finish` is the place.
* **The Boat** of Glass' world and **the Bless Series** are not built (design.md section 18).

## 12. How to try it (for whoever runs the game first)

1. `/multiverse hero gem give @s holy`, stand on open ground, hold right click for five seconds: a circle of light closes under your feet, the action bar counts 5 to 1,
   then the title "The Legendary <weapon> has chosen you." and the weapon in your hand (use `/multiverse hero release @s` to give it back and try again).
2. `/multiverse hero gem give @s seal scythe`: the same with the Vassal Scythe; `give @s fragment 8` and a bench to test a Seal recipe (the table in section 4).
3. Break it on purpose: step three blocks away, let go early, stand under a roof, take a hit: each ends the ritual with its own line and keeps the gem.
4. Two players, one Holy gem each, the same moment: they get different Legendary Weapons, and the third gem is told "The legendary weapons have already chosen their heroes."
   once all four are held. `/multiverse hero gem holders` lists who holds what.
5. Set `recipes = false` in `nusmp-heroweapon-gem-server.toml` and use a crafted gem as a non-operator in survival: "The gem is silent. This server has closed the Choosing".
6. `/multiverse mega selftest` must show `hero.gem` and `hero.gem.ritual` as OK.
