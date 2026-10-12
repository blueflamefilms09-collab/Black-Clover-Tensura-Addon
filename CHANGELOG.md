# Changelog

## [1.19.0] — The Living Dungeon, polish: real Tensura mobs, Ice and Fire compatibility, unbreakable blocks, the Golden City, its own sky and sounds, meals that lend skills, a Maw reward

**State:** written without a compiler; CI is the first compile. Nothing run in game. Every part below is **new** except where it says **replacement**.

### Replacements (owner request: "not custom ones")
<!--WILD-->

### Unbreakable (owner request: "unbreakable by any means")
<!--GUARD-->

### The Golden City
- **A real place** (`dungeon/city/*`): when a player in the Dungeon comes within 56 blocks of a city cell (`DungeonTier.isCityCell`, an addition), the city builds itself over a few seconds of ticks, resumable after a restart (`CitySave`): a sealed vault with a carved cavern, a broken outer wall with two golden gates, four avenues, a plaza with a glowing moat and a dais, 8 to 14 buildings (towers with stairs, halls, shrines, obelisks, arches, some ruined), lamp posts and chandeliers. The layout is deterministic per cell (`CityPlan`, pure and tested).
- **The Aurelian Altar** (new block): each player may claim one **Aurelian Core** from each city; using the Core in the air spends it and grants a 20 minute **Golden Ward** (no magicule radiation). Gilded Sentinels (real mid-tier mobs) stand on the plaza. Nothing in the city is a chest.
- Admin: `/multiverse dungeon city where|build|reset`.

### Magic Sense, meals that lend skills, the Maw reward
- **Magic Sense blunts the city's radiation** (it builds only every other second, with a one-time hint); a Mana Zone or a Golden Ward shuts it out completely and lets it drain (`dungeon/RadiationShield.java`).
- **Cooked meals lend a Tensura skill for a few minutes** (`MealSkills`): Candied Slime Jelly = Magic Sense, Grilled Armor Flank = Physical Attack Resistance, Roast Basilisk Fillet = Poison Resistance, Roast Direwolf Haunch = Danger Sense. Taken back when the time is up; relogging, dying or restarting cannot make it permanent; a skill you already own is never touched. Every skill id is checked against the registry at run time.
- **Meals attune you to a second magic** (`MealAttunement`): Gel, Iron, Poison or Blood. While attuned your spell hits also carry that magic's rider, at most once a second per target. Tooltips and the action bar say what and for how long.
- **Maw reward** (`MawReward`): everyone who chopped a Maw's roots and stands within 32 blocks when it closes gets experience and 1 to 3 **Maw Fruit** (the faster the cut, the more). Maw Fruit feeds, gives a short Regeneration and +15% spell power for 3 minutes.

### The Dungeon's own sky, fog and sounds
- **Sky and fog** (`client/dungeon/DungeonSky`, effects id `nusmp:living_dungeon` in `dimension_type/living_dungeon.json`: **replaces** the Nether look it used): each tier has its own mood, blended smoothly: cold grey-blue dust in the Upper Catacombs, deep teal in the Deep Waterways, red-black smoke and embers in the Volcanic Vents, warm gold haze and gold motes in the Golden City. Lava, water, powder snow, Blindness and Darkness fog are left to the game.
- **Ambient sounds** (`dungeon/DungeonAmbience`): tier-specific vanilla sounds near each player, and a far growl when an awake apex predator is within 40 blocks.
- A custom biome `nusmp:dungeon_depths` (new chunks only; existing chunks keep their stored biomes) joins the two vanilla cave biomes.

### Not done
- Predator dens as structures and the pack AI for the real mobs; the custom Living Armor model was dropped on request (the real mobs are used instead).
- Everything above is unverified in game. Honest list: the biome file, the fog hook, the Level mixin, the Tensura and Ice and Fire mob driving, and the city's look.

## [1.18.0] — The Living Dungeon, stages 2 and 3: the food web, carcasses, the Butcher Cleaver and cooking

**State:** written without a compiler; CI is the first compile. Nothing run in game. The creatures' AI, the births and the harvesting have never been seen working; the Direwolf and Basilisk models were checked with the repo's headless preview only. All **new** (nothing replaced). The weapon is the Butcher Cleaver (3D model, engravings, Curios belt slot).

### The food web (stage 2)
- **Four creatures** (`dungeon/DungeonCreature.java` and subclasses, spawn rules in `DungeonEcology.population`): **Dungeon Slime** (tier 1, three sizes by growth: grazes the chunk's magicules, the mycelium and carcasses), **Living Armor** (tier 2, a mollusk colony wearing found armour: hunts slimes), **Direwolf** and **Basilisk** (tier 3, apex: the Basilisk's bite poisons). Each has a **hunger** (0-100) that falls with time: hungry predators hunt (`HuntForSustenance`: the nearest prey within 16 blocks, kill = hunger +50 and the chunk's magicules +20), a **full predator sleeps** and ignores everyone (`Sleep`), one that starves takes damage, and players are prey only to a hungry creature or to one defending its den (within 6 blocks of where it lives). Slimes flee when hurt.
- **Closed-loop breeding** (every five seconds, per simulated chunk): slimes are born where magicules are dense (each birth costs 40) up to what the air can feed; Living Armors appear where there are at least 4 slimes and the air is fit to breathe; predators come where there are at least 2 Living Armors. A chunk that held six or more and is now empty has **collapsed**: a server-wide broadcast. Biomass is recounted from the living each pass.
- **Nothing drops loot in the Dungeon** (config `noLoot`, default true; players excepted): what dies leaves a **Carcass** block (`CarcassBlock`, no block entity: kind, meat portions and decay stage are block states). Scavengers eat a portion at a time; left alone a carcass rots through three stages (about 3 minutes) and returns to the soil: its nutrition goes back to the chunk's magicules and a Dungeon Mycelium veil grows there, which slimes graze. Creatures from other mods that die there leave a generic carcass too.
- Config `life` turns breeding off. Commands (admin): `/multiverse dungeon spawn slime|armor|direwolf|basilisk`, `census`.
- **Models:** the slime and the Living Armor use the game's own slime and humanoid models with textures of their own (`tools/gen_dungeon_assets.py`); the Direwolf and the Basilisk are GeoLite models with walk, idle, attack and sleep clips (`tools/gen_dungeon_mobs.py`, `client/dungeon/*Renderer`).

### The table (stage 3, "Delicious in Dungeon")
- **Butcher Cleaver** (`item/ButcherArts.java`, Kind appended at the end): a cook's cleaver in a real 3D model; right-click a living thing in front of you for the **Anatomical Cut** (armour does not stop it, it bleeds, and a Dungeon creature cut this way leaves an extra portion); right-click a **carcass** to cut the meat out: the Cleaver finds the prime cuts (sometimes two); any other blade (sword, axe: the `nusmp:butchering_blades` tag) takes one cut; anything blunt, a pickaxe or a bare hand, **ruins the meat**. Tensura engravings: severance, sturdy, enervation.
- **Raw monster meat** (slime jelly, armor flank, basilisk fillet, direwolf haunch, monster meat) is **toxic** (Magicule Sickness) unless the eater has Predator or Gluttony; holy water or honey purifies it (shapeless recipe, `#nusmp:purifiers`) into plain safe food. **Cooked** in a furnace, smoker or campfire it becomes a meal that feeds well and **bends the wizard's magic for a few minutes** (`dungeon/MealBuffs.java`, read by `ManaLaw` beside the Signature): Candied Slime Jelly (spells cost 15% less, 5 min), Grilled Armor Flank (+5% power, 4 min), Roast Basilisk Fillet (+20% power, 3 min), Roast Direwolf Haunch (+10% power, speed, 3 min), Seared Monster Meat (plain). Temporary skills and secondary attributes from meals are not done.

## [1.17.0] — The Living Dungeon, stage 1: the dimension, its ecosystem, its hazards and the Dungeon Maw

**State:** written without a compiler; CI is the first compile. Nothing run in game. The dimension reuses vanilla's `minecraft:caves` noise settings (a stone cave world with a bedrock roof and floor and water at sea level) and was never loaded: if the game refuses the dimension file the Maw will say "the Dungeon does not answer". The design is `docs/living_dungeon.md` (the original Forge 1.20.1 design translated to NeoForge 1.21.1: data attachments instead of capabilities, a dimension instead of a biome modifier). All of this is **new**; nothing replaces anything. The creatures, carcasses and cooking are stages 2 and 3 (1.18.0, 1.19.0).

### New
- **The dimension `nusmp:living_dungeon`** (`data/nusmp/dimension/living_dungeon.json`, `dimension_type/living_dungeon.json`): an enclosed cave world, y -64 to 128, no sky. Nothing spawns in it by itself: natural, chunk-generation, structure, patrol and reinforcement spawns are cancelled (`DungeonEcology.onFinalizeSpawn`).
- **The ecosystem of every chunk** (`dungeon/EcosystemData.java`, a NeoForge **data attachment** on the `LevelChunk`, `LivingDungeon.ECOSYSTEM`): magicule (0-1000), biomass (0-400), oxygen (0-100). Chunks within `simRadius` (3) of a player tick every five seconds: the air comes back toward its ceiling (60 in the Volcanic Vents), the roots feed magicule and life eats it. Spells cast in the Dungeon spend the chunk's magicules and **flame magic burns its oxygen**.
- **The four tiers** (`DungeonTier`): Upper Catacombs (y >= 24), Deep Waterways (the flooded floors), Volcanic Vents (patches of 112 blocks: burn damage, thin air) and the Golden City (below y -40, in rare 128-block cells). Hazards (config `hazards`): **heat** burns anyone not fire-resistant or wet in the Vents; **thin air** (oxygen at 35 or less) gives *Suffocation*; **magicule radiation** in the Golden City builds *Magicule Poisoning* (maximum health -10% a level, five levels) unless you keep a **Mana Zone** up; **water conducts**: an electric hit on someone in water jumps at half strength to every swimmer within 6 blocks and slows them; fire and explosion damage burn the chunk's oxygen. The first player into the Golden City breaks its seal for the whole server (a broadcast, once).
- **Three mob effects** (`DungeonEffects`, with icons): Suffocation, Magicule Poisoning, Magicule Sickness (raw monster meat, stage 3).
- **Mined terrain grows back** (`DungeonSave`, saved with the world): stone, deepslate, ores and dripstone broken in the Dungeon return after `regrowDays` (default 2 in-game days), never while a player is within 6 blocks; a **Dungeon Mycelium** veil grows in the hole meanwhile.
- **The Dungeon Maw** (`dungeon/DungeonMaw.java`): every grimoire spell adds `cost%^2/100` to the ambient magicule of its region (4x4 chunks; it decays 30 a minute); when a region crosses `mawThreshold` (default 600) the ground near the caster tears open into a **Dungeon Maw** wrapped in **Maw Roots** (cannot be broken itself; cut the roots with an axe to reach it, or to close it). Step in and you land on a safe floor of the Dungeon at the same x and z, beside a **Root Gate** that takes you back where you stood. A Maw closes when its roots are all gone or after `mawMinutes` (20). Server-wide broadcasts for a Maw opening (rounded to 100 blocks) and closing, and for the Golden City's seal (config `broadcast`). A region that opened a Maw falls to a quarter of the threshold; `mawCooldownMinutes` (30) keeps two apart.
- **Blocks:** Dungeon Maw, Root Gate, Maw Root, Dungeon Mycelium (models, textures from `tools/gen_dungeon_assets.py`, lang, axe tag; in the creative tab).
- **Commands:** `/multiverse dungeon status | exit` (everyone), `enter | maw open | maw close | ecosystem magicule|biomass|oxygen <v>` (admins).
- **Config:** `nusmp-dungeon-server.toml` (`enabled`, `hazards`, `broadcast`, `mawThreshold`, `mawMinutes`, `mawCooldownMinutes`, `regrowDays`, `simRadius`).
- Tests: `DungeonTierTest`.

## [1.16.0] — Every magic at the tower, fused grimoires, magic that is born, and a signature for every wizard

**State:** written without a compiler; CI is the first compile. Nothing run in game. The effects (`BornMagicLayer`) and the fused cover geometry have never been looked at on a screen; the blend table and the trait numbers are first guesses.

### Replaced
- **The tower no longer gives only Fire, Water, Wind and Earth.** The Tower Wizard's nature questions (six of them, drawn from a pool of twenty) now add points to sixteen *flavors* (Hot, Cold, Flow, Gust, Stone, Metal, Gem, Green, Bright, Shade, Soul, Space, Time, Body, Mind, Maker), never to a magic by name, and every attribute carries one or two flavors (`wizard/MagicFlavors.java`), so **every attribute that can be rolled** (all but Anti-Magic, Kotodama, Curse and the devil-only Demon magics) can be the answer; a two-flavor attribute (Glass: Gem and Hot) needs both. A mild tilt makes the rarest attributes rarer. Switch: `wizard.allMagics` (default true; false = the ceremony's `starterMagics` as before). The questionnaire is now 12 questions (6 nature, 2 hearth, 2 blood, 1 resolve, 1 class).
- **Two attributes no longer mean two grimoires.** When two attributes from different flavors are nearly tied (chance `wizard.dualAttributeChance`), they **fuse into one grimoire**. If the pair has a well-known blend the grimoire is that attribute's book (Fire + Water = **Glass**, Fire + Earth = Magma, Fire + Wind = Explosion, Water + Earth = Mud, Wind + Earth = Sand, Plant + Maker = **Cotton** as for Charmy, Fire + Maker = Food, ... 45 blends, `MagicFlavors.blendOf`), with the pair written on it. If the pair has no blend in the game, **a new magic is born** of them, named from the two ("Pyro" + "tide" = Pyrotide Magic). (`GrimoireAcceptance.grantExtra`, the old second grimoire, stays in the code but the tower no longer calls it.)

### New
- **A custom-looking grimoire for a fused mage.** `BookLook` takes the pair: the leather is the two parents' colours blended, the metal is the second parent's, the glow both; the geometry grows a generated filigree (`grimoire/FusionDesign.java`, built into the plan): a ring of gems round the medallion alternating the two metals, corner brackets, gems down the spine, different for every pair, on the closed book and the open V. The item's tooltip says "Fused: Fire + Water" or "Pyrotide Magic, born of Fire and Water".
- **No two wizards' magic is the same** (`mana/Signature.java`): every grimoire grant also gives a *signature*: a name unique on the server ("Ashen Flame", "Sunless Gale of Blackmere", a registry saved with the world keeps them apart), two *traits* (small trades of spell power, mana cost and recharge: Overcharged, Quickened, Thrifty, Heavy, Focused, Wild, Steady, Radiant) that apply to every spell of the grimoire, and a *shape*. It shows in the Multiverse status panel and on the Signature page.
- **Custom spells** (`book/SignatureSpell.java`), appended after the Mana Zone so no mode number moves: the **Signature Spell** (every grimoire; named after the wizard's signature; its shape, a lance, nova, wave, orb, rain or spiral, is the wizard's own), and, only on a fused grimoire, **two more** (Fusion Spell, Born Spell) in the next shapes, heavier, built from both parents: both parents' on-hit effects land and both palettes are drawn.
- **A procedural effect generator for them** (`vfx/client/layer/BornMagicLayer.java`; shapes `BORN_BEAM`, `BORN_NOVA`, `BORN_WAVE`, `BORN_RAIN`, appended): the server sends only the first parent's colour, the second's (the seed's high half) and a style; the client builds the whole effect (two strands of motes winding round a core, a circle and a counter-turning ring, rings racing out, a falling bolt), so a magic born of any pair has effects in its own two colours without any asset being made for it.
- Tests: `FusionDesignTest`.

## [1.15.0] — The Magebane Shield is a real shield, the weapons fit Curios slots, and the questionnaire is no longer blurred

**State:** written without a compiler; CI is the first compile. Nothing run in game. The Curios integration reads Curios by reflection and has never met the real mod.

### Fixed
- **The Tower Wizard's questionnaire was blurred** (sheet, title and question): the screen drew its parchment and text and then called `Screen.render`, whose background pass blurs everything already on screen, so only the buttons stayed sharp. The sheet is now part of the background layer and the text is drawn after it.

### Replaced (the shield's use)
- **Magebane Shield: hold right-click to block, like a vanilla shield.** It reports `SHIELD_BLOCK`, `UseAnim.BLOCK` and a 72000-tick use, so blocking, the lowered arm pose, durability loss on blocked hits and an axe knocking it down are the game's own. The old one-press, 6 s-cooldown Spellbreak Parry no longer locks the shield: raising the shield opens the Parry whenever it has recharged (its own timer, 18 s by default), lasting 2 s (3 s with the Magebane Longsword in a hand) only while the shield stays up; otherwise the shield is simply a shield. Sneak + raise fires Absorb Surge when it is ready and a caster is near, else you just block. A client item model override (`blocking`) raises the shield in front of the first-person view (`models/item/magebane_shield_blocking.json`; the transform is a guess, not seen in game). Nothing removed: the passive magic cut, Aegis Slam, the bash and the paired Dispel Surge parry are unchanged (the latter no longer needs the shield up).

### New
- **Curios support.** Every weapon is listed in `data/curios/tags/item/` so Curios can socket it: blades on the **belt**, greatswords, hammers, crossbows, the trident, tools and the Magebane Shield on the **back**, the Hand of Providence in the **hands** slot, the Brooch Pin as a **charm**, and all of them in the generic **curio** slot. `neoforge.mods.toml` lists Curios as an optional dependency. With Curios installed, a weapon worn in a slot (and not also in a hand) grants a small sheathed bonus shown in its tooltip (e.g. katanas +4% speed, Elsdocia +4 health, the Magebane Shield keeps its magic cut and +1 armour); `item/CuriosLink.java`. The numbers are first guesses to tune. Curios draws nothing on the body for them (no worn renderers yet).

## [1.14.0] — The Convergence system is removed (on the owner's request)

**State:** written without a compiler; CI is the first compile. Nothing run in game.

### Removed
- **The Convergence** (`multiverse/Convergence.java`): the 5% anomaly roll, the origin and stages (SIGNS to SPADE), the rumours, the anonymous announcements, and the rule that only anomalies could be chosen by a grimoire. **Secret Quests** (`SecretQuests.java`) and the **FTB Quests "Convergence" chapter** (`FtbQuestsChapter.java`) that only served it, the `/multiverse convergence ...` and `/multiverse quests ...` commands, the status panel's Origin and Secret Quests blocks, `docs/convergence.md`, and every `convergence.*` config entry (the grimoire guard moved to its own `[guard]` section: `guard.grimoireGuard`, still on by default).
- Because of this the Grimoire Towers generate from the start (they used to wait for the Clover stage), `/multiverse locate tower` works for everyone, and any player without a grimoire is eligible for the old ceremony as well as the Tower Wizard. A grimoire granted by the old ceremony is announced like any other (3-leaf quietly, 4-leaf and up to the server).
- Old worlds keep a harmless saved-data file for the removed system; nothing reads it.

## [1.13.0] — The Tower Wizard calls every mage, whatever the Convergence says

### Fixed
- **Almost nobody could get a summons.** The Convergence (on by default) made only its 5% "anomaly" players eligible for a grimoire, so with the Tower Wizard on (`wizard.enabled`) the summons, the library and the questionnaire were closed to everyone else. In wizard mode eligibility is now just "no grimoire yet" plus `everyoneEligible` (or an admin's eligible flag); the Convergence's anomaly rule applies only to the older ceremony (`wizard.enabled=false`) and to the Convergence's own staged grant for anomalies. A player who receives a grimoire from the wizard counts as revealed to the Black Clover side (the Convergence already treats every grimoire holder so). Not run in game.

## [1.12.0] — The Tower Wizard has his own model

**State:** the model was checked with the repo's headless preview only (geo check OK, 0 errors); the Java renderer is new and uncompiled until CI; nothing run in game.

### Replaced
- **The Tower Wizard is no longer the vanilla witch.** From the owner's design (`docs/attributes/art_reference/tower_wizard_design.png`): a 60-cube model about 1.9 blocks tall with the hat (`tools/gen_tower_wizard.py` -> `geo/entity/tower_wizard.geo.json`, `animations/entity/tower_wizard.animation.json`, 256x128 `tower_wizard.png` and `_glow.png`): a wide-brimmed, tipped-back brown hat with a floppy cone and a band, a larger head with heavy lids, bushy brows, a nose and moustache, a long grey beard of stacked blocks, a flared layered robe with a belt and mantle, wide sleeves, and a glowing green orb at chest height with orbiting motes and a halo. Idle (breathing, beard and hat sway, orb bob) and talk (a nod and a hand gesture, played while a player stands within four blocks). `client/wizard/TowerWizardRenderer.java` draws it through the GeoLite pipeline with the glow pass. The vanilla-witch renderer is no longer registered for him (the entity class still extends the witch for its behaviour).
- Preview scenes: `python3 -B tools/render_preview/preview.py tower_wizard_idle` (front, three-quarter, full body, side).

## [1.11.0] — The wizard's library dimension, next-step hints, one menu, social class buffs and debuffs

**State:** written without a compiler; CI is the first compile. Nothing run in game. The Tower Wizard's custom model comes in a later version.

### New
- **The Tower Wizard's library** (`wizard/WizardLibrary.java`, dimension `nusmp:grimoire_library`): right-click the Grimoire Summons and it carries you to a round reading room in the void (shelves, lanterns, a rug, a lectern, the wizard at the centre) with 26 books flying around whoever stands in it (they exist only while someone is inside). Nothing there can be broken. Sneak + right-click the wizard, or finish the questionnaire, and you are sent back to where you stood.
- **Next-step hints:** when you reach the age / level the summons arrives with a chat message and a title ("You are summoned: right-click the Grimoire Summons"); a reminder every two minutes until you go; "click an answer" and "sneak to leave" in the library; after the grimoire arrives, what to do next. The summons item is used up.
- **Social class has buffs and debuffs** (it was flavour only): Royalty (spells 5% cheaper, +1 luck / -2 max health, 10% fewer gold stars), Noble (spells hit 4% harder and cost 3% less, +5% skill mastery gain / -1 armor), Commoner (+5% skill learning), Peasant (+4 max health, +3% speed, 10% more gold stars / spells cost 5% more and hit 4% weaker). The questionnaire has a childhood question that sets it; `/multiverse profile class` still works. Shown in the status panel.

### Replaced
- **One menu:** the Four Kingdoms slot in Tensura's magic screen (and its label) now opens the Codex, which holds every page (overview, status, grimoire, weapons, relics, bestiary, spirit lords), and the small "Multiverse" button in the corner of Tensura's screens is gone. The older Four Kingdoms screen is kept in the code but no longer opened from the menu.

## [1.10.0] — Costs through Tensura's consume helpers; resistances as flat modifiers

**State:** written without a compiler, from the owner's reading of the Tensura 2.0.1.3 bytecode; CI is the first compile. Nothing run in game.

### Replaced
- **Mana Zone and Negative Mana now pay through Tensura.** `EnergyHelper.isOutOfMagiculeConsuming(entity, cost, min)` deducts the cost itself and returns the unpaid shortfall (0 = paid): the Mana Zone's per-second upkeep uses it with a 5% floor and the zone collapses (and the mage faints) on a shortfall; opening a Mana Zone costs 5% the same way. Attuning to a Magic Stone now costs 20% of the magicule through `EnergyHelper.isOutOfEnergy` (true = cannot afford, nothing deducted; the stone is then not used up). No more direct writes of the magicule value for these.
- **Kingdom and blood resistances are flat modifiers.** Tensura's resistances have a base of 0, where a percentage does nothing: they are now flat additions (+/-0.05 to 0.1). Attributes with a base of 1 or more (element power, regeneration, gain, dodge strength, learning and mastery gain) stay percentages of the base.

## [1.9.0] — Kingdom and blood buffs and debuffs use Tensura's own attributes

**State:** written without a compiler, against the Tensura 2.0.1.3 signatures the owner supplied; CI is the first compile. Nothing run in game.

### New
- **Each kingdom and each mixed-in blood now also has a buff and a debuff on a Tensura attribute** (`TensuraAttributes`): magicule / aura regeneration and gain, skill learning and mastery gain, dodge strength, magic and elemental resistance, elemental power (e.g. Spade +10% darkness power, -10% magic resistance; Elf +10% wind power, -10% earth resistance; Undead +10% darkness resistance, -10% light resistance). They are percentages of the attribute's base (so they scale with Tensura whatever its units), added next to the earlier health / attack / speed modifiers, and shown in the status panel and the result of the questionnaire. **Unverified:** the real base values of these attributes in Tensura; a percentage of a base of zero does nothing.
- The Wizard King's mana regeneration now goes through Tensura's `EnergyHelper.gainMagicule` instead of writing the magicule value, so it respects Tensura's own gain rules.

### Not changed
- Mana Zone and the Negative-Mana cost still write the magicule value directly: the signature of Tensura's consume helper (`isOutOfMagiculeConsuming`) does not say what its double means, and `drainEnergy` is for taking energy from another entity, not for paying a cost.

## [1.8.0] — The Wizard King's awakening runs through Tensura's own numbers

**State:** written without a compiler in the session, against the Tensura 2.0.1.3 API signatures the owner supplied; CI is the first compile. Nothing run in game.

### Replaced
- **The crown's awakening now uses Tensura's awakening mechanics** (`multiverse/CrownAwakening.java`) instead of only this mod's attribute buffs: it multiplies the base of Max Aura and Max Magicule (`TensuraAttributes`) by the bigger of the hero and demon-lord EP multipliers from Tensura's race config, refills both, and evolves the Tensura race to its awakening evolution (once), exactly as `RaceHelper.awakening` does for a hero or demon lord, **without** setting the true-hero or true-demon-lord flag. Tensura's `AWAKENING_EVENT` is refused for the Wizard King, so the crown holder cannot also awaken as a hero or a demon lord, and a mage who already is one cannot take the crown. Losing the crown divides the multiplier back out (the evolved race stays). Since Tensura's EP follows those attributes, every EP-scaled number of this mod (spell damage, balance law) rises with the crown. The earlier attribute buffs, Saint Stage casting and the Wizard King skill stay.

## [1.7.0] — The Wizard King: one holder, an awakening of its own, its own skill

**State:** written without a compiler in the session; CI is the first compile. Nothing run in game.

### New
- **The Wizard King is a title only one player on the server can hold** (`multiverse/WizardKing.java`, saved with the world). Take the crown with `/multiverse wizardking claim` (a Grand Magic Knight with at least `wizard.wizardKingMinStars` net stars, default 100, and more net stars than the holder), or an admin `appoint` / `dethrone`s. Setting the rank to Wizard King any other way goes through the crown, so there can never be two. Losing it (also while offline) takes everything back.
- **It counts as an awakening, a third path beside a true hero and a demon lord (you cannot choose those: the crown is its own).** The holder gets the same *kind* of power an awakening gives: +20 max health, +4 armor, +2 toughness, +15% attack, +10% speed, +0.3 knockback resistance, +2 luck; Saint Stage casting (spells 20% cheaper and 25% stronger on top); mana that refills itself (1.5% a second) and life that mends; and no black-out from spent mana. Tensura's own EP and its hero / demon lord awakening are not touched (no safe API for it): the crown's power is this mod's own, on top of the Tensura stats.
- **The Wizard King's skill** (Unique; only the holder has it; mode key cycles): *Sovereign Decree* (16 blocks: foes slowed, weakened and lit up, allies resistant and regenerating), *Crown of Mana* (a free Mana Zone for 60 s), *Sovereign Strike* (a 40-block piercing bolt), *Royal Guard* (8 s of immunity to spells, plus absorption), *Return to the Capital* (home at once, 5 minutes). Passive: all damage -10%.
- `/multiverse wizardking info|claim|appoint|dethrone`.

## [1.6.0] — The Tower Wizard: a summons, a questionnaire, a grimoire that floats to you; kingdom and blood buffs and debuffs

**State:** written without a compiler in the session; CI is the first compile. Nothing run in game. Details: `docs/mana_system.md`.

### New
- **Grimoire Summons** (item) given when you are old and grown enough (play time and/or experience levels, configurable), **Tower Wizard** (a witch-model NPC at the tower), a randomly drawn **questionnaire** (a screen) that decides your magic attribute(s) (a second grimoire for a dual mage), your grimoire type, the **kingdom** you hail from (buff and debuff) and the **blood of a Tensura race** mixed into yours (buff and debuff). The grimoire then floats from the wizard to you.
- **Heritage** (`mana/Heritage.java`): the kingdom and blood modifiers and Mage Stage buffs, as transient attribute modifiers on top of the player's Tensura stats. Shown in the status panel.
- `/multiverse wizard summon|spawn`.

### Replaced
- At a Grimoire Tower or Altar an eligible mage is now sent to the Tower Wizard instead of being chosen at once (`wizard.enabled`, default on). The 1.5.0 choose-your-magic list and the old roll remain for `wizard.enabled=false` and for admin grants.

### Not done
- The wizard has no custom model or skin (he is the vanilla witch); the questionnaire screen is plain parchment with buttons; towers that were never visited are not found by the summons.

## [1.5.0] — The mana system of the video, with no bad draws

**State:** written without a compiler in the session; CI is the first compile. Nothing run in game. Full table (video vs. mod): `docs/mana_system.md`.

### New
- **Mana as life force** (shock at 5%, black-out at 0.5%; lethal only with `mana.lethalExhaustion`), **Mage Stages 9-0 / Arcane / Saint** from rank and class (power from training, not from the draw), **Affinity** (loved / ordinary / hated: a balanced trade you can change any time), **Mana Zone** (page in every grimoire; thick mana in the deep sea, the Nether, mountains, the deep dark and the End makes it stronger: Grand Magic Zones), **Magic Stone** and negative mana with corruption, `/multiverse mana ...`, a Mana block in the status panel.
- **Mana Strike**: a universal combat page in every grimoire, so a healing, sealing or trap magic is never a dud.

### Replaced
- **The ceremony no longer rolls your magic: you choose it** (`mana.chooseMagic`, default on; chat buttons or `/multiverse choose <magic>`). The cover (rarity), Black Magic, God-Tier and Convergence paths are unchanged. Set `chooseMagic=false` for the old roll.
- The old fixed exhaustion at 5% mana is now `ManaLaw.exhaust` (same shock, plus the black-out).

### Not done
- Dungeons made of mana; mana inherited from parents; Mana Method runes do not scale with stage yet.

## [1.4.0] — Brooch Pin and Hand of Providence get their own abilities

**State:** not yet compiled when written; CI is the first compile. Nothing run in game.

### Replaced
- The stopgap `PinProvidenceArts` (fold / unfold, Mage Hand) is no longer wired (the file is kept). **Brooch Pin**: Piercing Pin, a lunge of up to 7 blocks that runs through every foe on the line (armour does not stop it) and slows them, striking the first from behind; Ruby Ward absorbs damage (`BroochPinArts`). **The Hand of Providence**: Laying On of Hands, a big mend, Regeneration and every curse and poison cleansed on the ally you look at within 6 blocks or on yourself (`HandOfProvidenceArts`). Engravings updated.

## [1.3.0] — Riven Remake removed, eight reference weapons, high-resolution weapon skins, real-life sizes

**State:** every commit of this line compiled in CI up to the Riven removal and the size/skin pass; the Brooch Pin / Hand of Providence wiring is new in this release. Nothing here was run in game.

### Removed (on the owner's request)
- **Riven Remake / Marquis Remake** completely: the boss and its story constructs, renderer, boss bar, config (`nusmp-riven.toml`), `/nusmp riven` commands, combat code, models, textures, animations, loot tables, the Black Bull Bard Relic, Story Page, Crest Cape token, Cracked Crown and spawn egg, the Riven canon grimoire, its boss VFX shape (`RIVEN_VOID_GATE`) and all lang entries. The anime-skill codex (`skill/codex`, `data/nusmp/skills/codex`) existed only for Riven's research tools and went with him. Sylph's Gale Feather stays.

### Fixed
- **Codex weapons page ran over the frame:** a normal (not turning) page was drawn without a clip, so long weapon lists spilled over the book edges and the page title. Pages are now clipped to the paper; the per-weapon line cap is gone; indented tooltip lines stay indented and wrap under themselves.

### New (from the reference sheets)
- **Magebane Shield, Weapon of Resurgence, Bolt Thrower, Crossbow of Swift Death, Club of Rot, Doom Hammer** (3D models, engravings, techniques, full-swing strikes, VFX, icons), plus **Brooch Pin** (folds into a brooch / unfolds into a dagger; Hidden in Plain Sight, Pinprick Lunge) and **The Hand of Providence** (Mage Hand lay on hands / holy strike, Lay on Hands on yourself). The Brooch Pin and Hand meshes are first versions.

### Replaced
- **Weapon skins raised to 512x512, Elsdocia to 1024x1024** (`tools/hires_weapon_textures.py`, called by `gen_weapon_models.py`; the 128 originals are kept in `tools/weapon_tex_128/`). Elsdocia's blade is repainted as faceted blue crystal with white-hot cores and bright fractures, the bronze section keeps its clover cut-out. The other skins are smoothed and sharpened with surface grain, not repainted: they are higher resolution, not new artwork.
- **Real-life sizes:** katanas about 1 m, longsword 1.25 m, greatsword 1.8 m, trident 1.9 m, spear 2.1 m, Elsdocia 2.3 m (`REAL` table in `gen_weapon_models.py`; 1 block = 1 m).

### Not done
- The older weapon meshes are still 40-170 flat quads; they are not sculpted high-poly models like the tensuranoindex jar's. The Brooch Pin does not animate its fold.

## [1.2.0] — Compiled: the 1.1.0 line builds, Elsdocia grand, boss spawn grace, tooltip wrapping

**State:** the merge of the three lines (the owner's 1.0.0 Marquis / Riven line, the 0.99 Elsdocia + Key restoration line, and the 0.58 line with every new grimoire, VFX, sounds and summons) compiles in CI and passes its tests. Nothing here was run in game.

### Fixed
- **Build:** the Codex UI imported `net.minecraft.client.renderer.Lighting`; in 1.21.1 it is `com.mojang.blaze3d.platform.Lighting` (this broke the 1.1.0 build). The Riven loot test now expects the 5-pool table the Story Page drop made.
- **Boss dies on spawn (Marquis / Riven Remake):** on top of the 0.100 damage guard, for the first 100 ticks `setHealth(<= 0)`, `kill()` and every damage source that is not a creative player, `/kill` or the void are ignored (and logged with the real source: look for `Marquis/Riven: ignored` in `latest.log`). Unverified in game.
- **Elsdocia description ran off the screen:** every weapon tooltip is now wrapped at word breaks (44 characters, continuation lines indented); Elsdocia gained its wiki lore line.
- **Release notes:** the CI release body is now the newest section of this file.

### Replaced
- **Elsdocia remade again (owner reference image + the Black Clover Movie scale):** the blade top is worked bronze-gold with the clover cut out of it, the rest is shattered-glass crystal in deep azure with bright cracks and white-hot cells down to a long pointed tip; a thin, wide cross guard with swept tips; a slender dark grip; gold pommel. **Twice the size in hand** (a greatsword). New 16x16 inventory sprite (`tools/gen_elsdocia_icon.py`); the earlier sprites stay as `elsdocia_v099.png`.

### Version gap check (1.0.0 -> 1.1.0 -> 1.2.0): see `docs/version_gap_1.2.md`

## [1.1.0] — Resumed from v1.0.0 (compiled in 1.2.0)

The tree is v1.0.0 (the Marquis / Riven line) with the 0.100-0.102 work ported onto it; the older line's own Riven implementation (`RivenPassives`, `riven_codex/`, story weapons) is superseded by v1.0.0's and is not restored. Details are in README.md under "1.1.0". In short: seven reference-sheet weapons, Elsdocia remade, the usable Bard Relic, Story Page and Gale Feather, the Codex UI, and the four Black Clover Spirit Lords with true bodies (Tensura spirits stay orbs).

## [0.102.0] — The Black Clover Spirit Lords get their bodies (NOT yet compiled)

**New (from "Spirit Lord model Ideas"):** `ElementalSpiritLordEntity` with four hand-built models: **Salamander** (winged fire dragon), **Undine** (woman of water with foam hair, wave dress, fin ears and orbiting drops), **Sylph** (green fairy with dragonfly wings and bubbles) and **Gnome** (hunched stone giant). Each has idle / move / cast / hit animations, a glow layer, a technique and an aura for its master (see `docs/spirit_lords.md`). Exclusive as in Black Clover: one mage per element (`SpiritSlots`), only that mage can call it, one body per element. Models by `tools/gen_spirit_lords.py` (+ `tools/spirit_geo.py`), previews in `tools/render_preview/scenes.d/spirit_lords.json`.
**Tensura-style spirits stay small floating orbs** that follow the player (`SpiritLordEntity`, unchanged); without a Black Clover bond the Spirit Charm now calls one (use: pick the element, sneak-use: call). Admin: `spirit_body <player> <kind>` calls a body.
**Also:** a Spirit Lords page in the Codex.
**Replacement:** the Spirit Charm no longer turns a bonded mage's lord into an orb; it calls the real body (the orb entity is untouched).
**Build note:** still not compiled (maven.neoforged.net blocked in the session); build with `gradlew.bat build` and send me any errors.

## [0.101.0] — Reference weapons, boss drops, the Codex (NOT yet compiled)

**New:**
- 7 weapons from the "Custom weapon i want added" sheets, each with a 3D in-hand model, glow layer, inventory icon, Tensura engravings, a right-click technique, a sneak-click second technique and a full-swing strike (logic in `item/ReferenceWeapons.java`): **Quickling's Ivy** (rapier: rooting poison ivy), **Magebane Longsword** (strips buffs, jams casters), **Prism Blade** (blinding flash, seven-colour beam), **Thundercleave** (thunder arc, thunderstorm, hits arc to a second foe), **Bloodbound Katana** (Blood Charges, Bloodflare), **Permafrost Sabre** and **Cyclone Rapier** (the Boreal Blades; hold both for Boreal Fusion).
- Boss drops: **Story Page** (Riven Remake; Rewrite: cleanse + heal 40%), **Gale Feather** (Sylph; Gale Leap), loot tables `entities/riven_remake.json` and `entities/wind_spirit_lord.json`.
- **The Codex** (`client/ui`): an open book with page-turn animation, mouse-wheel scrolling, tabs, and on the first spread your own figure in a random pose (standing, sitting, bowing, casting, floating, meditating, victory, salute, on guard) next to the magic this mod has given you. Pages are registered in `CodexPages`, so the book grows without touching the screen.

**Replacement:** the Multiverse Status screen is replaced by the Codex everywhere it opened (hotkey, Tensura-menu button, panel click). Its status blocks are the Codex's Status page; the old class stays only for the compact Tensura-menu panel. The **Black Bull Bard Relic** (an inert trophy) is now usable (sneak-use: Bardic Inspiration); same item id.

**Build note:** this session could not reach maven.neoforged.net, so none of 0.100/0.101 was compiled. Build with `gradlew.bat build` and send me any errors.

## [0.100.0] — Elsdocia remake + boss spawn guard (NOT yet compiled)

**Replacement:** Elsdocia's 3D model skin, glow layer and inventory sprite (the wiki/reference look: long straight crystal blade, deep azure with a cyan core and light motes, gold clover emblem above a gold guard, dark grip). The old sprite is kept as `textures/item/elsdocia_v099.png`.
**Fix (unverified in-game):** Riven Remake spawn guard. Damage and deaths in his first 60 ticks are blocked and logged (`[nusmp] Riven: ignored ...` in latest.log, which names the real killer), Tensura passive grants are wrapped in try/catch, and he is given a magicule pool like Zagred so Tensura skills cannot starve and hurt him.
**Build note:** this session could not reach maven.neoforged.net, so 0.100.0 was not compiled. Build it locally with `gradlew.bat build`.

## [0.99.0] — Elsdocia + Key Magic Restoration

**Release Date:** 2026-10-10

### Added

#### Elsdocia: the first Wizard King's legacy sword
- New God-tier weapon: **Elsdocia** (`nusmp:elsdocia`) — broad crystal-like blade with a clover emblem near the golden hilt (per the Black Clover wiki)
- Custom engraving: **Legacy of the Wizard Kings** (Tensura enchantment, level 1) — additive damage in main hand
- Standard Tensura engravings: Magic Interference 2, Magicule Absorption 2, Barrier Piercing 1
- 3D in-hand mesh (broad crystal blade, clover-crest plate on both faces, ornate gold hilt), 16×16 sprite stays the inventory icon
- Mechanics:
  - **Absorbs/stores/releases magic:** hitting a foe drains 2% of their magicule pool into the sword's charge (`ElsdociaCharge`, 0-1000)
  - **Legacy Release** (right-click): releases stored charge as a broad spatial rift; heals the wielder proportional to spent charge; boosted by Key Magic grimoire
  - **Legacy Gate** (sneak+right-click): requires a floating Key Magic grimoire + ≥100 stored charge; opens a 32-block spatial rift that drains 4% of each hit foe's magicules
  - **Key Magic synergy:** with a Key Magic grimoire floating, Legacy Release reaches 28 blocks (vs 22), hits for ×1.35 damage, and Legacy Gate becomes usable
  - Country-destroying lore scale is lore-only (tooltip); no terrain destruction
- Lore tooltips (per wiki): broad crystal-like blade / clover emblem; crafted by Lemiel Silvamillion Clover; used by Asta to defeat Conrad; contains a piece of the past Wizard Kings' souls
- EPIC rarity, Netherite tier, unbreakable legacy blade

#### Key Magic restoration
- Restored the physical **Magic Key** item (`nusmp:magic_key`) — uses the mini-key model's 64x64 texture as its item icon, dropped by Janus Baptism, consumed by Janus Abigail for extra gates
- Restored `KeyArts.janusBaptism` to drop a Magic Key on cast (was a tag-charge-only system)
- Restored `KeyArts.janusAbigail` to consume held Magic Keys (keys fill the door slots after the legacy charges, with four extra doors total) to extend to 10 doors
- Improved `KeyAura` to use the textured `great_key` model at 0.08 scale (was `mini_key` at 0.3) — the later approved improvement, not an exact recreation of `v0.82.0-b126`

### Fixed
- `WeaponEngravings` now parses fully-qualified enchantment ids (e.g. `nusmp:legacy_of_the_wizard_kings`); short ids still resolve to the `tensura` namespace
- Build fix: `BronzeAura.piece` used nonexistent `PartPose.x()/y()/z()` accessors — now uses the public `x/y/z` fields
- Build fix: `BronzeProps` used nonexistent `MagicPropEntity.hasLineOfSight` — now uses a block raycast between the prop and its target (same behavior)
- These Bronze build errors were pre-existing on the default branch (`760f10a`) and prevented any build before 0.99.0

## [0.98.0] — Marquis Remake Raid Boss

**Release Date:** 2026-10-09

### Added

#### Marquis Remake Boss (0.1.0-ALPHA)
- New raid boss: **Marquis Remake** — Minecraft-skin-body Black Bulls bard with god-summon mechanics and New Order reality rewrites
- Player-scale humanoid model (1.8 blocks, Steve hitbox) with Minecraft skin + geo coat layers (4 phases)
- Four-phase fight progression:
  - Phase 1 (Bard): Fa Jin charge + New Order + random grimoire
  - Phase 2 (Hack): 75% HP, coat fade-in, Gearshift + Compress + Evil Eye
  - Phase 3 (Rift): 60s timer, dimension pull to `nusmp:story_rift`, arena-scale mechanics
  - Phase 4 (Final Form): 25% HP, page-wings + cracked crown, The Creator god summon
- **300,000,000 EP** via Tensura API (non-plunderable, shown on appraisal)
- **New Order**: Reality-rewrite gaze mechanic (6-block range, 1.0s tell). Legal orders: +50% cast warmup, weapon speed halved, ground becomes solid, speed boost. Barks the rule. Anti-magic fizzles.
- **Fa Jin**: Melee/dash kinetic charge (white rings on crest). 5 stacks → 4-block cone punch with knockback. 10s timeout. Cannot hold through stagger.
- **Gearshift**: Speed rank (40% Low / 200% Top) on touch. Red-to-blue motion trail. Minimum Low. 4s duration. Ranks shown on boss bar.
- **Compress**: Marble-compresses blocks, constructs, or failed-tell players. Purple sphere in offhand for 3s. Player: removed from fight 3s, no damage, then released. Block: arena-only, restores at phase end.
- **Random Grimoire**: Draws one registered `nusmp.skill.grimoire_*` book each combat roll, cast via SpellRuntime. Named on boss bar. Anti-magic burns page.
- **Evil Eye**: God-scale, arena-scale. Both eyes, violet, 0.8s tell. Marks target with Existence Reduction: regen (ultra/totem/absorption/natural/mod/avatar) does nothing for 6s. Current HP does not auto-drop. Hits taken while marked cannot heal after mark ends. Works on players, high-EP Tensura bodies, and his own avatars. Phase 4 mark lasts 8s. Boss bar shows eye + subtitle "This page ends."
- **God Avatars** (10 total, one at a time, 12s life):
  - Zeus: One punch + Gearshift Top. 4-tick afterimage tell + 4-block hit. Survives one Island Fall.
  - Beerus: Hakai sphere (3-block radius, arena blocks + constructs only, blocks restore). Players take signature damage only.
  - Truth: Equivalent Exchange. Avatar damage mirrored to last player who hit Marquis (capped at phase signature).
  - Ultimate Madoka: One arrow. Removes one negative on him or one positive on marked player.
  - Arceus: Judgment. Next grimoire draw forced to element target is weakest to (via ThreatScan).
  - Grand Zeno: Erase. Deletes one construct or marble-compresses Evil Eye marked player (releases at anchor with mark intact).
  - Anti-Spiral: Three galaxy-shuriken projectiles (wide, slow tell, dimension-slash shader).
  - Lord of Nightmares: Sea of Chaos pool under gold ring (4s, pauses regen even without Evil Eye).
  - Kami Tenchi: Passive. Arena gravity stabilizes, no elytra, his i-frame window blocked. Dim gold outline, no attack.
  - The Creator: Phase 4 only, once. Law: "healing does not exist here" for 8s, whole rift. Stacks with Evil Eye. Then avatar closes.
- **Combat Type Rolls** (every 12s phase 2, every 8s phase 3): Caster, Hexblade, Legion, Rift, Song, Hack. Named on boss bar.
- **Damage Phases** (signature damage, not multiplier):
  - Phase 1: 22 base
  - Phase 2: 42 base
  - Phase 3: 62 base
  - Phase 4: 85 base
- **Null Types** (1.5s i-frame, phase change only):
  - Phase 2: Physical null 4s, then magic null 4s (alternating). Other damage type still hurts. Repeating nulled type = 0 damage.
  - Phase 3: Spatial null 4s on rift entry.
  - Phase 4: Rotating null 4s.
  - Anti-magic cancels i-frame window if not started.
  - The Creator adds 1.5s i-frame on entrance.
- **Health Scaling**: Base 1200 HP, +400 per extra player (cap 4).
- **Model & Animations**:
  - Phase 1: Minecraft skin only, grimoire closed on hip, eyes dim
  - Phase 2: Black coat + bull emblem fade on over sweatshirt, eyes go violet, grimoire opens and orbits
  - Phase 3: Lightning on coat, page shards, gold rift ring
  - Phase 4: Page-wings and cracked bull crown (same rig, no entity swap)
  - Animations: idle, walk, cast, gearshift dash, compress, evil-eye, summon, hit, phase change, death (0.4–1.4s, interrupt into hit)
- **VFX**:
  - New Order: white text rune at target's feet
  - Fa Jin: white rings on crest, cone flash on release
  - Gearshift: red-to-blue motion trail
  - Compress: purple marble with player's face shrunk on it
  - Evil Eye: full violet eyes, cone (anti_magic_slash tinted violet)
  - Avatars: gold page-body, feature as weapon (not second player model)
  - Fallback to arc_portal if shader fails
- **Loot Table** (`data/nusmp/loot_table/entities/marquis_remake.json`):
  - Guaranteed: Black Bull bard relic, story page trophy, skin-cape cosmetic (sweatshirt crest)
  - One of: Page Edge engraving, Bull Brand engraving, single-use rift key
  - 8% drop: Final Form crown (vanity)
  - No Tensura unique, no New Order item, no Evil Eye item
- **Boss Bar**:
  - Name: Marquis Remake
  - Subtitle swaps by phase
  - Combat type roll displays
  - Gearshift rank display
  - Eye indicator on Evil Eye mark
  - Hides outside arena leash

#### Arena & Visuals
- Story rift dimension (`nusmp:story_rift`, phase 3 mechanic)
- Character sheet visual lock (black high-collar coat, silver filigree, Black Bull skull, violet-blue eyes, floating grimoire)
- Minecraft skin body (brown hair, blue sweatshirt with white crest, gray pants, white shoes, player proportions)
- Coat and grimoire as geo layers that grow in, not replacement entity
- Phase progression shows visual escalation

#### Compatibility
- Tensura race/barrier/EP detection via ManasCore
- Black Clover grimoire book registry (`nusmp.skill.grimoire_*`) integration
- Anti-magic and NihilityZone: stagger Marquis, burn drawn page, force redraw
- ThreatScan integration for New Order order selection and Arceus judgment
- No Predator copy, no Usurper take-over, no Tensura unique skills

#### Configuration
- `config/nusmp-marquis.toml`
- Base health: 1200
- Per-player health: 400
- Avatar duration: 12s
- Evil Eye mark duration: 6s (phase 4: 8s)
- New Order duration: 8s
- Gearshift duration: 4s
- Compress marble duration: 3s

### Technical

- Package root: `com.newuniverse.nusmp`
- Entity: `MarquisRemakeBossEntity`
- Brain: `MarquisBrain` (threat-scan + god summon picker)
- Attacks: `MarquisAttacks` (New Order, Fa Jin, Gearshift, Compress, grimoire cast, Evil Eye, summon)
- Payload: `MarquisStatePayload` (phase, HP, cast ID, combat type, gearshift rank, evil eye mark)
- Renderer: `MarquisRenderer` (geo layers, coat fade, phase-specific textures)
- Boss bar: `MarquisBossBar` (animated overlay, phase subtitle, combat roll, eye indicator)
- Avatars: `GodsAvatarEntity` (10 variants, AI-driven, 12s life, breaks into pages)
- Asset structure:
  - `assets/nusmp/geo/entity/marquis_remake.geo.json` (4 phase variants on one rig)
  - `assets/nusmp/animations/entity/marquis_remake.animation.json` (idle, walk, cast, gearshift, compress, evil-eye, summon, hit, phase, death)
  - `assets/nusmp/textures/entity/marquis_remake_skin.png` (player skin)
  - `assets/nusmp/textures/entity/marquis_remake_coat.png` (coat layer, phases 2–4)
  - `assets/nusmp/textures/entity/marquis_remake_wings.png` (page-wings, phase 4)
  - `assets/nusmp/textures/entity/marquis_remake_crown.png` (cracked crown, phase 4)
  - `assets/nusmp/models/entity/gods/` (10 avatar models, gold page-body variants)
  - `data/nusmp/boss/marquis_remake.json` (phases, HP, drops, rift anchor)
  - `data/nusmp/loot_table/entities/marquis_remake.json` (drops)
  - `data/nusmp/dimension/story_rift_marquis.json` (arena dimension, phase 3)
- Lang keys in `assets/nusmp/lang/en_us.json`:
  - `entity.nusmp.marquis_remake` = Marquis Remake
  - `entity.nusmp.marquis_remake_spawn_egg` = Marquis Remake Spawn Egg
  - `boss.nusmp.marquis_order.*` = Order barks
  - `avatar.nusmp.gods.*` = Avatar names

### Dependencies

- NeoForge: [21.1,)
- Minecraft: 1.21.1
- Hard:
  - tensura [2.0.1.0,)
  - manascore_skill [4.0.0.2,)
  - manascore_storage [4.0.0.2,)
- Optional: bettercombat

### Testing & Quality Assurance

- New Order only uses legal orders (no delete/erase world/inventory commands)
- Fa Jin charge clears on stagger, cannot exceed 5 stacks
- Gearshift minimum is 40% speed (never 0), no negative speed
- Compress marble is visible, has 3s timer, no permanent locks
- Evil Eye stops only regen types (not direct damage), works on avatars
- Avatar summons one at a time, 12s life hard limit, breaks into pages
- The Creator fires once per phase 4, cannot be resummoned
- 300,000,000 EP shows correctly on appraisal, is not plunderable
- Dimension pull has tell + anchor release
- Anti-magic stagger correctly triggers page burn and redraw
- Zagred and dream dimension unchanged
- Dedicated server + client sync via `MarquisStatePayload`
- Boss bar UUID stable across all phases

### Known Issues & Limitations

- v1: Story rift dimension exists only during phase 3; players outside leash cannot enter
- god avatars do not load chunks outside rift (containment by design)
- The Creator law "healing does not exist" is rift-wide, not global (by design)
- Marquis + Zagred can share arena but do not have coupled AI (v2 feature)

### Future Enhancements

- Coupled Marquis + Zagred dual-boss mode
- Extended god summon pool (more anime franchises)
- Boss dialogue system (phase barks, order taunts, avatar summon callouts)
- Configurable arena themes for story rift (forest / castle / void)
- Kill replay system for New Order / Compress analysis

---

## [0.61.0] — Previous Release (Riven Remake)

See [RIVEN_REMAKE_RELEASE_NOTES.md](RIVEN_REMAKE_RELEASE_NOTES.md) for full details on Riven Remake (0.1.0-ALPHA).

---

## Versioning

This mod uses **semantic versioning**: `MAJOR.MINOR.PATCH`

- `0.98.0`: Marquis Remake raid boss added to nusmp 0.97.0 baseline
- Target `1.0.0` when both Riven and Marquis are production-ready

## Contributors

- **MarcusBlu**: Design, spec, and implementation
- **blueflamefilms09-collab**: Repository maintainer

---

**Last Updated:** 2026-10-09
