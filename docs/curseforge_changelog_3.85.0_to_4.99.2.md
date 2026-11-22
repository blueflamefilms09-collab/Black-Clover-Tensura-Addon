# Multiverse of Anime: everything new from 3.85.0 to 4.99.2

This is the full list of what changed from 3.85.0 up to 4.99.1. It is written for players and server owners.
Almost every change has a setting that turns it off (server or client config), so you can go back to the old behaviour. Old worlds keep working: items, skills and saved data from older versions are still read.
Some content is still being built and is not listed here yet.

---

## SHORT VERSION (paste this into the CurseForge changelog box if the long one is too big)

**3.85.0 to 4.99.2: the biggest stretch of the mod so far.**

**Berserk:** the Black Sword Greed (8 forms, one per world) and five more Weapons of Mortal Sin, each built around its own Ultimate skill.
**Dragon Balls and Saiyans:** Earth and Super Dragon Balls, Shenron, a wish system with overpowered wishes and voice wishes, a real Saiyan race with hair, tail, armour and 17 forms, the Super Saiyan God ritual, Ki techniques and the Giant Ki Avatar.
**Grimoires:** a thicker animated book, leaf power (covers now matter), canon cover meanings, a 30-question Tower Wizard, Rebirth and Second Book sigils, a grimoire slot in your own inventory, and a Spirit Dive that no longer needs a Tensura Lord level.
**Weapons:** 100+ hero and vassal weapon forms rebuilt as real 3D models made in Blender, real arrows and meteors, real katanas, shields with straps, weapons held in the hand without floating or clipping.
**Devils, Djinns, Beast Wills:** devil contracts with their own bodies, rituals, stacked pacts and 26 canon devil looks; new Djinn looks; Beast Wills that follow their source series.
**Powers:** Ultimate classes (Lord, King, God, Dragon), late-game damage scaling, a meditation screen to hold several powers (two at a time), and anime-style effects for hundreds of skills.
**New menus:** the Multiverse Hub (radial menu), a Weapon Tracker, a Wiki book with a table of contents, a creative tab for every anime verse, and 500+ redrawn skill icons.
**Fixes:** hero weapons no longer hit for thousands, transformations no longer bug on death, lag from the grimoire slot is gone, and many more.
**Removed (on request):** the Faith system, the Black Wings skill and the old Dragon Ball skills.

---

## FULL CHANGELOG

### Highlights by theme

- **Berserk of Gluttony:** the Black Sword Greed, Lord of Gluttonous Greed and five more Weapons of Mortal Sin.
- **Dragon Balls and Saiyans:** the whole Dragon Ball system, the Saiyan race, forms and techniques.
- **Grimoires and the Tower Wizard:** a rebuilt book, leaf power, canon covers, a 30-question wizard, Rebirth, a slot in your inventory.
- **Weapons:** Blender-made models for the Legendary, Seven Star and Vassal weapons, Berserk sin weapons, hero bows with real arrows, real meteors.
- **Devils, Djinns, Beast Wills, Spirits:** new bodies, new canon rules, new looks, a meditation screen, a Spirit Dive for everyone.
- **Skills and powers:** Ultimate classes, anime-style effects, redrawn icons, damage that grows with you.
- **Interface:** the Multiverse Hub, the Weapon page, the Weapon Tracker, the Wiki, a creative tab for every verse.
- **Balance and fixes:** damage scale-down, hero weapon damage, performance work.

### 4.99.x: Spirit Dive for every grimoire wielder
- Before, the Spirit Dive page only opened for a Lord of that spirit in Tensura's own spirit data, so most mages never saw it. Now any wielder of a Wind, Fire, Water or Earth grimoire can use it. Still needed: the spirit answers you, it is not bonded to another mage, the first dive costs a Spirit Charm, and the dive costs magicule while it lasts.
- Setting: server `spiritDive.requireLordLevel` (default off). Turn it on to get the old rule back. All dive looks and tiers (Yuno, Fuegoleon, Noelle) are unchanged.

### 4.96.0: A creative tab for every verse
- One creative tab per verse (28 more: Dragon Ball, Shield Hero, DanMachi, Seven Deadly Sins, Naruto, Bleach, One Piece and many more). Server owners can run `/multiverse verses` to see how full each tab is.
- All tabs now share one fill of the creative menu. The seven older tabs and all their items are still there.

### 4.95.0: Dragon Ball help for new players
- Holding a Dragon Ball now gives in-game hints and a `/dragonwords` helper so new players are not lost. Nothing is required; it only helps. Setting `dragonBalls.teachWords` (default on).

### 4.94.0: Weapons no longer block hold-and-use items, Real Doubles UI
- Fixed: the Hourglass of the Final Hour, Chronicle Sigil, Calling Gem and similar utility items were treated as weapons when a Holy Weapon was bound, which cancelled them and shocked you. A tag `hold_use_items` now lists everything that is never a weapon.
- New HUD stack for Real Doubles: one card per double (name, distance, time left, health) with keys to pick the one you aim at.

### 4.93.0: Dragon Balls get overpowered wishes and voice input
- New setting `dragonBalls.overpowered` (default on; off = the old wishes). Many more wish types, including any race registered in the game, ranks and more.
- The anime scatter: after the first granted wish the balls fly apart across the world and are locked for a while (setting `overpoweredLockMinutes`).
- Voice wishes: with the Simple Voice Chat plugin, the microphone of a player at a complete ball set is recognised offline (optional speech model, not bundled) and the words go through the same checks as typed chat.

### 4.92.0 and 4.90.1: Meditation list and weapon damage
- Fixed: the meditation screen listed only your active devil; it now lists every devil you hold.
- Fixed: a hero or vassal blow multiplied three stacked raises together. Each raise now keeps only a share (new setting `raiseShare`, default 0.02). A Legendary blow now ends near the weapon's intended strength.

### 4.89.x: Grimoire slot rework, the Dragon Radar, the Tower Wizard
- The grimoire slot is now a gold-framed slot in your own inventory screen (click to take, place or swap; shift-click moves the book). Setting `black_clover.grimoireSlotInventory` (false = the old window). Every click is checked on the server and rate limited.
- Lag fixed: the slot no longer compares and copies stacks every tick. Several slot bugs fixed (copies, empty stacks, pick-all pulling from the slot).
- The grimoire book is drawn solid, not see-through (4.89.2).
- The Dragon Radar costs more to craft: gold ingots, an ender eye, diamonds, a compass and a redstone block (4.89.3).
- No time limit on the Tower Wizard's questions: the sheet is no longer dropped after 5 minutes (setting `answerLimitMinutes`, 0 = none). An open sheet is forgotten when you log out (4.89.1).

### 4.88.x: Audit follow-up, hero weapons and ten more rebuilt
- 60 error-handling sites in 37 files now catch only what they should and log once, so real problems are no longer hidden.
- A Holy or Vassal Weapon used to reject (and shock you with) the top-grade relic weapons. New setting `heroweapon.genesisRelicsAllowed` (default on).
- Style wave 3: Legendary Spear and its Curse form, Portal Spear, Dragon Minute Hand, Siderite Katana, White Tiger Katana, War Carriage, Queen's Chariot, Yata Mirror and Phoenix S get a new look. Same ids, grip, engravings and Better Combat files. Client option `vassalForms.classicStyleWave3` brings back the old look.

### 4.87.0: Rebirth
- **Sigil of Rebirth:** erase your grimoire skills and your own grimoire items and go back to the Tower Wizard for a new magic type. Needs a second click to confirm.
- **Sigil of the Second Book:** keep your first grimoire and take a second one of another magic type.
- Own config file `nusmp-rebirth-server.toml` (cooldown, confirmation time, XP cost) and two crafting recipes.

### 4.86.0: Canon leaves, what each grimoire cover means
- Every cover has a short meaning line in its tooltip and in the wizard result.
- The four-leaf and the Five-Sided Diamond are luck covers: a spell hit sometimes lands lucky.
- By default the five-leaf is bound to the devil: it comes from despair or a devil contract, and costs something while wielded.
- Your level now matters: a cover far above your level costs more mana, and a veteran with a lower cover gets bonus spell damage.
- Master setting `canonLeaves`; off restores 4.85 exactly.

### 4.85.0: The long Tower Wizard questionnaire
- The wizard asks 30 questions in five parts from a pool of 99. Your answers now decide your magic, kingdom, blood, class and how many leaves your book has.
- Setting `wizard.longQuestionnaire` (default on; off = the old 12 questions).

### 4.84.0 and 4.83.0: Leaf power with a price
- A cover now multiplies every spell: a four-leaf is far stronger than a three-leaf, and the five-leaf, Black Magic and God-Tier covers climb much higher. Higher covers also cost more mana per spell, but power per mana still rises with the cover.
- Settings `leafPower` and the per-cover numbers; the item tooltip shows the new totals. Off = the old covers.

### 4.82.0: Infinity Stones, Phase B
- The Infinity Gauntlet and a new Nano Gauntlet hold up to six socketed stones with a synergy system.
- A reworked Snap with a long cooldown and a toll, and a new staff that holds the Mind Stone.
- New Vormir Altar block (off by default as a requirement).
- Config `nusmp-stones-server.toml`, section `infinityStonesB`.

### 4.81.0: Grimoire revamp
- A thicker 3D book made in Blender: layered cover, metal corners, rounded spine, ribbon, page block and a raised emblem plate per magic. Asta's tattered five-leaf has its own worn look.
- While a grimoire floats open: a glow halo, two counter-turning rune rings in the magic's colour, page motes and a flare on every cast.
- Client option `visual.classicGrimoires` restores the old book.

### 4.80.x: Wiki table of contents and cleanup
- A clickable **table of contents** for the Wiki and every long Codex page. Client option `visual.codexContents`.
- Cleanup fixes: no leftover position history for players who left, test stand-ins forgotten at stop, narrower error handling.

### 4.79.0 and 4.78.0: The Wiki
- New players get the Multiverse Wiki book once (setting `starter.starterWiki`).
- Eleven new Wiki pages (power scale, rarity, hero weapons, hero bows and meteors, Saiyans, devils, Djinns and Beast Wills and more) and short up-to-date paragraphs on fourteen older pages.

### 4.77.0: `/multiverse sin purge`
- Removes a Berserk weapon skill and deletes every stack of that weapon from the player.

### 4.76.0: The 26 canon devils polished
- New ears, horns, wings and eyes for the 26 canon devil models. A client option keeps the old ones.

### 4.75.0 to 4.71.0: Shields, bows, katanas, arrows and meteors
- **Shields:** every shield is drawn with leather straps on the arm; the hero Shield blocks every frontal blow and turns arrows back at the shooter; shields sit a little off the arm and no longer stand out past it.
- **Katanas:** the six hero katanas (White Tiger, Soul Eater, Siderite, Sakura Stone, Wind King, Hellblaze) are rebuilt as true single-edged katanas.
- **Bows:** every bow is drawn larger in hand; hero bows fire a real arrow entity with a physical and a magic hit and a per-form effect.
- **Meteors:** one shared real falling meteor (Meteor Sword, Meteor Swarm, Black Comet, Merlin's Starfall and more) replaces the instant sky-streaks.
- **Techniques last longer:** setting `heroweapon.durationScale` (default 2.0).
- Hero and vassal techniques now hit any living target (animals, villagers, neutral mobs) except allies, your own pets and other people's pets.
- 48 wing models that swept forward are mirrored to the back.
- **Devil mastery:** a Devil Awakening skill and mastery per devil, and canon looks for the 26 devils.
- The Infernal Contract is now a plain craftable item held by its rod.

### 4.70.0 to 4.57.0: New weapon looks
- **Style wave 2:** fourteen vassal weapons lose the gold-and-navy look (hammer, axe, staff, claw, gauntlet, whip and more). Client option `vassalForms.classicStyleWave2`.
- **Hellblaze, the Demon Sin** is now a katana with its own sheath.
- **Curse bow** gets its own horned stave; Sagittarius is a slim S-curve bow; six bows no longer look alike.
- 14 katanas rebuilt thin; 44 spear, harpoon and bow meshes rebuilt solid; Belial's second pair of arms is drawn.
- Reset scrolls now take your skills like normal; only soul-bound skills stay and fall silent (`guard.resetSoulOnly`).
- The weapon page shows more masteries; the old Status screen gets a Weapon tab.

### 4.69.0 to 4.59.0: The Multiverse Hub
- **The Multiverse Hub:** a radial menu of 8 nodes (Status, Weapons, Skills, Quests, Attunement, Multiverse, Codex, Settings) with a skippable intro, hover and click effects. It replaces the Codex book as the entry point; setting `ui.hub = false` gives exactly the old behaviour.
- Hub v2: the Codex lives inside the hub, 30 custom icons, a smaller framed window (`ui.hubSize`) and a posing player model on the Status page.
- A Weapons tab with weapon chips, upgrade rows and mastery bars.
- The hub emblem sits in Tensura's former "Coming Soon" slot.

### 4.56.0 to 4.55.0: Weapon page
- A **Weapon page** in the Codex: the weapon you hold, its upgrade options and mastery bars.

### 4.54.0 to 4.42.0: Vassal weapons made in Blender
- More than 100 forms across vassal axes, books, carriages, chains, whips, flails, scythes, staffs, mirrors, katanas, instruments and projectile weapons, each with its own skin and glow. Whips, flails and morning stars now bend and swing.
- Berserk sin weapons rebuilt: 8 new forms, one-of-one rules, a named drawback per Lord weapon, effects and mastery bars (4.53.0).
- Weapons are now held in the hand: hero forms find their hold with no floating and no clipping through arm or body (4.50.0).
- Older versions can restore the classic look with `classic` settings per wave.

### 4.53.0 to 4.38.0: Berserk of Gluttony weapons
- The Black Sword Greed gets real gauntlets (one glove per hand) and dual blades (4.21.0).
- Five more Weapons of Mortal Sin, each built around its own Ultimate skill (Lord class), with extra techniques, passives and themed effects.
- `/multiverse sin remove` takes a weapon skill away.

### 4.52.0 to 4.48.0, 4.64.0 to 4.64.1: Devil contracts
- **Phase 2:** stack several devil pacts, share lesser devils between holders, price by Existence Points, betrayal rules, and a trial devil with its own model.
- **Devil Double Magic:** devil arts that mix with your grimoire, one-of-one devils, ranks and a craftable contract.
- The Trial Devil gets a real model; devils face you and stand on the ground.
- Devil magic no longer drains food (4.50.1).

### 4.51.0 to 4.43.0: Weapon Tracker and looks
- A **Weapon Tracker** screen, and engravings that are locked to their weapon.

### 4.41.0 to 4.20.0: Looks, races and systems
- 43 new Djinn busts, rank display now uses the relic grades.
- **Saiyan Prodigy:** one skill with 18 modes that holds every Saiyan ability, plus a Saiyan-blooded evolution for humans.
- **Damage scale-down:** nothing from the mod hits harder than 10,000 per hit except one apex weapon. Rankings for everything (Djinns, weapons).
- **Unique Guardian:** a destroyed one-of-one item goes back into the loot pool.
- **Vassal weapon enchanting** and per-weapon wheel skills.
- **Belial reworked** as a white spirit-cutting scythe Djinn with his own 3D scythe and bone-dragon armour; the Fenrir Will reworked as an SS-class Hel-Hound.
- The Legendary sword, bow, spear and shield forms (about 44 models) are real Blender 3D models.
- **Dragon Balls, Super Dragon Balls, Shenron and the wish system** (4.20.0).
- Contract devils on the detailed player model.
- Hero and vassal weapons no longer start at about 10,000 damage (4.37.1).

### 4.19.0 to 4.12.0: Looks and Saiyan details
- A monkey tail and 3D Saiyan hair for each of the 16 forms.
- Transformations no longer bug on death (4.14.2).
- Roblox-style effects redrawn for ten effects.
- Beast Wills follow their source series (tail sweep, thunder chain, devour) with progression milestones.
- Infinity Stone slot (Phase A): one stone, two new abilities, passives.

### 4.8.0 to 4.0.1: Skills and the first 4.x wave
- 121 skill icons redrawn in the Tensura style, 56 Ultimates animated (later 146 and 76 more).
- Wiki-accurate reworks of Argonaut, Shinra, All Fiction and the Summoner; Firebolt joins Argonaut; Hell Walker becomes Shinra.
- Saiyan is a real Tensura race with 3D hair, tail and craftable armour; the EP part of an addon skill's damage grows in with mastery.
- Devils get their own 26 bodies, a figure of the devil rises out of the ritual circle and speaks.
- Fixed: Beast Will forms had no arms or legs on the player's body (4.0.1).

### 4.0.0: The big update
- The Ultimate classes Lord, King, God and Dragon scale your existing Ultimate skills.
- Four Magic Knight guide pages in the Wiki.
- Anime-style skill effects for Tensura, DanMachi and Dragon Ball skills, all 27 Beast Wills, all hero weapon kinds, devils, the Black Sword Greed and grimoire attributes.
- Dragon Ball techniques, the Super Saiyan God ritual, Saiyan stages and the Giant Ki Avatar.
- The meditation screen, the two-power limit and late-game damage scaling.

### 3.105.0: Dragon Ball and Saiyan techniques
- New physical techniques: Meteor Strike, Vanishing Step, Aerial Rush, Warrior's Counter, Ground Breaker, Combat Instinct.
- New Ki techniques: Charged Ki Sphere, Ki Wave (hold, release, channel), Ki Reinforcement, Afterimage, Gravity Training and the Giant Ki Avatar (a Ki giant drawn as an effect, never an entity).
- The Super Saiyan God ritual, Saiyan stages 1 to 4 (Saiyan, Primal, Divine, Autonomous Martial God) with small buffs, and `/saiyan stage`.
- Kaio-ken x10 and x20 and the mastered-form drain rule, behind switches (config `nusmp-saiyan-phase2-server.toml`).
- Every older technique, key, form and number is unchanged.

### 3.104.0 and 3.101.0: Anime-style skill effects
- Real anime shapes instead of plain particles for: Gate of Babylon (golden portals, falling weapons), Firebolt, Adolla Burst and the Devil's Footprints dash, Hero's Argonaut, Wynn Fimbulvetr, Hestia, Rea Laevateinn, Freya, All Fiction, Hephaestus, Liaris Freese, Hati, Hawk Eye, Spirit Card, Summoner and Lord of Summons, Book Maker, Shishou, the Gate of Babylon armour, Vimana, King and Elixir modes, the Almighty and The Visionary.
- All 27 Beast Wills draw their move family (line, rush, burst, volley, seal, aura), hero weapons draw their motif, and grimoire attributes get their own look.
- Client settings `skills.classicVfx` and `skills.classicVfx2` give the old effects back.

### 3.103.0, 3.87.0 and 3.86.0: Skill icons
- 3.86.0: 365 skill icons get a finishing pass (stronger colour, light from the top-left, soft vignette, dark rim).
- 3.87.0: 195 grimoire and Beast Will icons are redrawn as bevelled, lit reliefs on richer backgrounds in ornate gem frames, with a 128x128 detailed copy of each.
- 3.103.0: 146 icons that shared a placeholder (29 magics of the 0.53 expansion and 30 wiki magics) are redrawn with their own themed glyph.
- Every original icon is kept; ids and file names are unchanged.

### 3.102.0, 3.99.0 and 3.97.0: Ultimate classes and the master guide
- **Ultimate classes (3.99.0):** every Ultimate skill has a class. GOD: Almighty, The Visionary, All Fiction. KING: Gate of Babylon, Hell Conqueror, Lord of Summons. LORD: Adolla Burst, Hero's Argonaut, Immortal Will, Ki, Nen, Shishou, Lord of Gluttonous Greed and the Dragon Ball and Beast Will Ultimates. A DRAGON class exists for later.
- Classified Ultimates take more mastery to master, read a "mastered" tier, and have milestones at 25, 50, 75 and 100 percent (longer reach, a second effect, a cheaper and faster cast, the mastered form). Each can be switched off in `[ultimateClasses]`.
- Addon skills now pay through Tensura: the price grows with your maximum magicule (a few percent late game, never below the old flat price). Setting `flatCosts` brings the old flat cost back (3.99.0, 3.102.0).
- Four Magic Knight Wiki pages: ranks and stars, missions and conduct, squads and the Star Awards, the ceremony and the Wizard King.
- Mod skills now hit with Tensura's own skill damage source, so Tensura's resistances, nullifications and kill credit apply to them. The damage amount is unchanged (3.97.0).
- The 43 `grimoire_<magic>` skills that had no name or description now have both; temporary spell blocks are removed when the server stops (3.97.0).
- Grimoire casts send a cancellable pre-cast and post-cast event (3.102.0).

### 3.98.0 and 3.94.0: Anime-style effects for Greed, devils and Saiyans
- Every form of the Black Sword Greed draws real shapes: ink-black crescent slashes with a crimson edge, a black-red bow bolt, the scythe's Soul Reap arc, the shield's black-scale dome, Black Flames, Twilight Healing and more.
- A devil's combat hit now shows its world: Black Clover sigil crack, Chainsaw Man saw rip, Black Butler pentagram, Blue Exorcist blue flames, Berserk Brand and eclipse ring, Fairy Tail curse runes.
- The Saiyan techniques draw real Dragon Ball shapes: Ki Blast, Rapid Barrage, Kamehameha (charge sphere, then a thick beam with a white-hot core) and more (3.94.0).
- Client settings `greed.classicVfx`, `devil.classicVfx` and `saiyan.classicVfx` give the old look back.

### 3.95.0 and 3.93.0: Meditation, and at most two powers
- `/meditate` opens the **Meditation screen**: your bound Beast Wills, your Djinn, your Devil contract and every Spirit that answers you, with slots to hold them together.
- Clashing elements reject each other (fire and water, fire and ice, light and dark, life and death, wind and earth) with a short backlash; matching elements resonate for a bonus.
- A player can have at most two of Beast Wills, Djinns, Devils and Spirits working at once, and never a Devil together with a Spirit (3.95.0). Setting `equipLimit`; `enabled=false` restores the old behaviour.
- Attuned Devils and Spirits hit much harder, level with the Ultimate tier.

### 3.91.0: Damage grows with the player; grimoires hit harder
- Late-game scaling: every addon hit a player deals grows with their maximum magicule (x1 up to 10,000, then more per tenfold, at most x6). Settings `lateGameScaling`, `lateGameScalingBaseline`, `lateGameScalingCap`.
- Grimoire spells hit x1.75 harder (`grimoireDamageMultiplier`). Off or 1.0 gives the old damage.

### 3.90.0: Fenrir reworked to the Norse myth
- The Fenrir Beast Will skills are now the wolf of the myth: Fenrir, Devourer of the Gods (Ultimate), Wolf of Limitless Growth (Unique), and passives and actives named after the myth. Ids, numbers and looks are unchanged, so old saves load as before.

### 3.89.0: The old Dragon Ball skills are removed from play
- The 30 old Dragon Ball skills (15 characters) are no longer usable, because the new Dragon Ball system replaces them. A player who still has one forgets it at the next login. Server setting `dragonBallLegacySkills` (true restores the old skills).
- The registrations, ids and icons stay, so old worlds load unchanged.

### 3.88.0: Creative tabs by franchise
- Seven new creative tabs: Black Clover, Tensura, Berserk, Fairy Tail, Magi, Hero Weapons and Other Anime Worlds. The seven older tabs are unchanged. Client setting `visual.franchiseTabs`.

### 3.85.0: The Black Sword Greed
- A one-per-world, soul-bound sword with eight forms (Sword, Bow, Scythe, Shield, Staff, Gauntlets, Spear, Dual Blades, Level Zero), eight tiers opened by devouring, hunger and Half-Starvation, a skill vault, Soul Sever, one Secret Technique per form, and the Ultimate skill **Lord of Gluttonous Greed** (3 modes). Key `R` switches the open forms. `/multiverse greed ...` for operators and the wielder.
- 3D models made in Blender, Tensura engravings per form, Better Combat files for the melee forms.
- 3.85.1 and 3.85.2 fixed the build checks of 3.85.0 (effects use the mod's own particles, the starvation hit is magic damage, the shield blocks through the normal item use).

---

## Removed on request
- **The Faith system** (the Faith skill, Goddess of Faith and the Faith gods), removed in 4.18.0. Old saves load without it.
- **The Black Wings skill** (4.3.0). Its id stays registered as an inert stub so old worlds load.
- **The old Dragon Ball skills** (3.89.0), replaced by the new Dragon Ball and Saiyan system.
- **The old flat weapon skins** for 22 bows and spears (4.62.1), which makes the jar about 6 MB smaller.

## Good to know
- Everything new has a setting that restores the old behaviour. Server settings change gameplay; client settings only change how things look.
- Existing config files keep their old values; delete a line from a config file to get the new default.
- New features were written to stay light on public servers: bounded work per tick, bounded packets, and effects sent to nearby players only.
- Not everything has been seen in a game by the author. Please report anything odd with your `latest.log`.

*Small build-fix and test-fix releases in between are not listed separately.*
