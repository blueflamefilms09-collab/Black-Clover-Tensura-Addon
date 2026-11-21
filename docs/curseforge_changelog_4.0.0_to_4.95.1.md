# Multiverse of Anime: everything new since 3.105.0 (4.0.0 to 4.95.1)

This is the full list of what changed from the last 3.x release (3.105.0) up to 4.95.1. It is written for players and server owners.
Almost every change has a setting that turns it off (server or client config), so you can go back to the old behaviour. Old worlds keep working: items, skills and saved data from older versions are still registered.

---

## SHORT VERSION (paste this into the CurseForge changelog box if the long one is too big)

**4.0.0 to 4.95.1: the biggest stretch of the mod so far.**

**New bosses:** Goku (a boss that changes form like the anime, later "Goku Omega", the strongest boss), Sinbad, Kakashi Hatake (14 canon moves, ninja-scroll boss bar), a boss realm for Goku and Sinbad, new dungeon bosses (Floor Warden, Direwolf, Basilisk, Living Armor, Labyrinth Guardians, Decennials) and more than 60 Black Clover, Midnight Sun and devil characters on the detailed player model. All 54 bosses now pay a Signature item with its own icon and a timed boon.
**Dragon Balls:** Earth and Super Dragon Balls, Shenron and Super Shenron, a wish system, overpowered wishes, voice-chat wishes and in-game hints for new players.
**Grimoires:** a thicker animated book, leaf power (covers now matter), canon cover meanings, a 30-question Tower Wizard, Rebirth and Second Book sigils, and a grimoire slot in your own inventory with no more lag.
**Weapons:** 100+ hero and vassal weapon forms rebuilt as real 3D models in Blender, solid arrows and spear heads, real katanas, shields with straps, weapons held in the hand without floating or clipping, Berserk of Gluttony weapons with their own skills, and a Weapon page with mastery bars.
**Devils:** devil contracts with their own bodies, rituals, stacked pacts, shared devils, price by EP, trial devil, Double Magic and 26 canon devil looks.
**Saiyans:** a real Saiyan race, hair, tail and armour, 17 forms, the Saiyan Prodigy skill and Saiyan-blooded evolution.
**New menus:** the Multiverse Hub (radial menu), a Weapon Tracker, a Wiki book with a table of contents, and a long list of new Codex pages.
**Fixes:** hero weapons no longer hit for thousands, bosses fight the moment you are near, bosses no longer lose health to effects nobody cast, transformations no longer bug on death, and many more.
**Removed (on request):** the Faith system and the Black Wings skill.

---

## FULL CHANGELOG

### Highlights by theme

- **Bosses:** Goku and Goku Omega, Sinbad, Kakashi, the boss realm, dungeon bosses, 60+ new character looks, Signature drops, party scaling, reactive guards, bosses that fight at once.
- **Dragon Balls:** the whole system from 4.20.0, with overpowered wishes, voice wishes and new-player hints in 4.93.0 to 4.95.0.
- **Grimoires and the Tower Wizard:** a rebuilt book, leaf power, canon covers, a 30-question wizard, Rebirth.
- **Weapons:** Blender-made models for the Legendary, Seven Star and Vassal weapons, Berserk sin weapons, hero bows with real arrows, real meteors.
- **Devils, Djinns, Beast Wills:** new bodies, new canon rules, new looks.
- **Interface:** the Multiverse Hub, the Weapon page, the Weapon Tracker, the Wiki.
- **Balance and fixes:** damage scale-down, hero weapon damage, boss rules, performance work.

---

### 4.95.1: Boss damage diagnostics
- Added a read-only log option for finding out why a boss loses health. It changes nothing in the fight.

### 4.95.0: Dragon Ball help for new players
- Holding a Dragon Ball now gives in-game hints and a `/dragonwords` helper so new players are not lost. Nothing is required; it only helps. Setting `dragonBalls.teachWords` (default on).

### 4.94.0: Worthwhile boss drops, weapons no longer block hold-and-use items, Real Doubles UI
- All 54 bosses (and a fallback table) now pay a **Signature item** named after the boss, with its own icon, a rarity by tier and a right-click boon of 4 to 15 minutes. Every fighter also rolls personal extras. Some bosses pay a unique item.
- Fixed: the Hourglass of the Final Hour, Chronicle Sigil, Calling Gem and similar utility items were treated as weapons when a Holy Weapon was bound, which cancelled them and shocked you. A tag `hold_use_items` now lists everything that is never a weapon.
- New HUD stack for Real Doubles: one card per double (name, distance, time left, health) with keys to pick the one you aim at.

### 4.93.0: Dragon Balls get overpowered wishes and voice input
- New setting `dragonBalls.overpowered` (default on; off = the old wishes). Many more wish types, including any race registered in the game, ranks and more.
- The anime scatter: after the first granted wish the balls fly apart across the world and are locked for a while (setting `overpoweredLockMinutes`).
- Voice wishes: with the Simple Voice Chat plugin, the microphone of a player at a complete ball set is recognised offline (optional speech model, not bundled) and the words go through the same checks as typed chat.

### 4.92.0: Goku Omega, the strongest boss
- New switch `goku_boss.omega.enabled` (default on; off = the old Goku exactly).
- A last form, **Mastered Ultra Instinct**, silver hair and aura with a halo ring and a near-perfect dodge and counter.
- Goku keeps a saved level for the whole world that grows when he wins, trains while nobody fights him, and unlocks more abilities with the level. He can take energy and seal skills from players, and he is built to stay stronger than any other boss.
- Hits on a full-health player can be larger in this mode (settable, hard cap).
- Fixed: the meditation screen listed only your active devil; it now lists every devil you hold.

### 4.91.0: Bosses fight back the minute a player is near
- Fixed the cause of idle bosses: they only got a target from vanilla's "nearest player" goal, which needs line of sight, shrinks for sneaking players and sometimes rolls to do nothing.
- Every framework boss (and Zagred, the Floor Warden and Charybdis) now takes a player as target as soon as one is in range (default 32 blocks). A boss with no move for 4 seconds walks up and melees.
- New settings `bossRules.aggroOnSight` and `aggroRange`.

### 4.90.1: Hero and vassal weapons no longer hit for thousands
- Fixed: a hero or vassal blow multiplied three stacked raises together. Each raise now keeps only a share (new setting `raiseShare`, default 0.02). A Legendary blow now ends near the weapon's intended strength.

### 4.90.0: Goku bows out instead of dying; no random damage on him
- When beaten, Goku is not killed: he is knocked up, cannot be hurt for a moment, says goodbye, flashes away and drops his loot. The fight counts as won. Setting `goku_boss.farewell`.
- New setting `bossRules.anonymousDamageBosses` (default Goku): a listed boss takes no damage when nothing attacked it.

### 4.89.3: The Dragon Radar costs more to craft
- New recipe: gold ingots, an ender eye, diamonds, a compass and a redstone block (same result).

### 4.89.2: The grimoire book is solid, not see-through
- Fixed the book model so its big faces can no longer vanish or show the world through them.

### 4.89.1: No time limit on the Tower Wizard's questions
- The wizard used to drop your sheet after 5 minutes. There is now no limit (setting `answerLimitMinutes`, 0 = none).
- An open sheet is forgotten when you log out.

### 4.89.0: Grimoire slot rework, and no more lag
- The grimoire slot is now a gold-framed slot in your own inventory screen (click to take, place or swap; shift-click moves the book). Setting `black_clover.grimoireSlotInventory` (false = the old window).
- Every click is checked on the server and rate limited.
- Lag fixed: the slot no longer compares and copies stacks every tick.
- Several slot bugs fixed (copies, empty stacks, pick-all pulling from the slot).

### 4.88.2: Audit follow-up
- 60 error-handling sites in 37 files now catch only what they should and log once, so real problems are no longer hidden.

### 4.88.1: A hero weapon no longer locks out the primordial relics
- A Holy or Vassal Weapon used to reject (and shock you with) the top-grade relic weapons. New setting `heroweapon.genesisRelicsAllowed` (default on).

### 4.88.0: Style wave 3, ten more weapons rebuilt
- Legendary Spear and its Curse form, Portal Spear, Dragon Minute Hand, Siderite Katana, White Tiger Katana, War Carriage, Queen's Chariot, Yata Mirror and Phoenix S get a new look. Same ids, grip, engravings and Better Combat files. Client option `vassalForms.classicStyleWave3` brings back the old look.

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

### 4.80.1 and 4.80.0
- A clickable **table of contents** for the Wiki and every long Codex page. Client option `visual.codexContents`.
- Cleanup fixes: no leftover position history for players who left, test stand-ins forgotten at stop, narrower error handling.

### 4.79.0 and 4.78.0: The Wiki
- New players get the Multiverse Wiki book once (setting `starter.starterWiki`).
- Eleven new Wiki pages (power scale, rarity, hero weapons, hero bows and meteors, Saiyans, devils, Djinns and Beast Wills and more) and short up-to-date paragraphs on fourteen older pages.

### 4.77.0: `/multiverse sin purge`
- Removes a Berserk weapon skill and deletes every stack of that weapon from the player.

### 4.76.0: The 26 canon devils polished
- New ears, horns, wings and eyes for the 26 canon devil models. Client option `bossLooks.devilCanonClassic` keeps the old ones.

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

### 4.56.0 to 4.55.0: Weapon page and voices
- A **Weapon page** in the Codex: the weapon you hold, its upgrade options and mastery bars.
- Goku, Sinbad and Kakashi speak: 218 voice clips in synthetic voices, all with subtitles.
- Later (4.66.0) Goku and Sinbad voices were polished.

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

### 4.51.0 to 4.43.0: Kakashi, Sinbad and Goku
- **Kakashi Hatake boss:** 28,000 health, four phases, 15 telegraphed moves (Kamui, Raikiri and more), later 14 more canon moves, four looks (covered eye, Sharingan, Mangekyo, Susanoo) and a ninja-scroll boss bar.
- **Sinbad boss (Magi saga):** 17 moves, one Djinn at a time, a seven-step finale, a Weapon Tracker screen, and engravings that are locked to their weapon.
- **Goku and Sinbad redesigned** with 16 new player-model looks, their own boss bars and 117 voice lines.
- **A boss realm** for Goku and Sinbad: two arenas in the arena dimension (4.40.0).

### 4.41.0 to 4.20.0: Looks, races and systems
- 43 new Djinn busts, rank display now uses the relic grades.
- **Saiyan Prodigy:** one skill with 18 modes that holds every Saiyan ability, plus a Saiyan-blooded evolution for humans.
- **Damage scale-down:** nothing from the mod hits harder than 10,000 per hit except one apex weapon. Rankings for everything (Djinns, weapons).
- **Unique Guardian:** a destroyed one-of-one item goes back into the loot pool.
- **Weapon Tracker, Vassal weapon enchanting** and per-weapon wheel skills.
- **Belial reworked** as a white spirit-cutting scythe Djinn with his own 3D scythe and bone-dragon armour; the Fenrir Will reworked as an SS-class Hel-Hound.
- The Legendary sword, bow, spear and shield forms (about 44 models) are real Blender 3D models.
- **Dragon Balls, Super Dragon Balls, Shenron and the wish system** (4.20.0).
- Contract devils and Black Clover characters on the detailed player model.
- Boss reference study: reactive guard, attack timelines, party scaling, data-pack boss numbers and Black Clover x3 counts.
- Hero and vassal weapons no longer start at about 10,000 damage (4.37.1).

### 4.19.0 to 4.12.0: More boss work
- 38 Black Clover and Midnight Sun characters, then 21 more, then 14 more on the detailed player model with anime looks.
- **Dungeon bosses:** Floor Warden, Direwolf, Basilisk and Living Armor with new moves; the nine Labyrinth Guardians and the 200 Decennials get new moves, 41 new sounds and particle themes.
- A monkey tail and 3D Saiyan hair for each of the 16 forms; Hydra heads that fan out.
- Transformations no longer bug on death (4.14.2).
- Roblox-style effects redrawn for ten effects.
- Beast Wills follow their source series (tail sweep, thunder chain, devour) with progression milestones.
- Infinity Stone slot (Phase A): one stone, two new abilities, passives.

### 4.8.0 to 4.0.1: Skills and the first 4.x wave
- 121 skill icons redrawn in the Tensura style, 56 Ultimates animated (later 146 and 76 more).
- Wiki-accurate reworks of Argonaut, Shinra, All Fiction and the Summoner; Firebolt joins Argonaut; Hell Walker becomes Shinra.
- Saiyan is a real Tensura race with 3D hair, tail and craftable armour; Lucifero is a much stronger boss; boss drops for 23 bosses; the EP part of an addon skill's damage grows in with mastery.
- Devils get their own 26 bodies, a figure of the devil rises out of the ritual circle and speaks.
- Fixed: Beast Will forms had no arms or legs on the player's body (4.0.1).

### 4.0.0: The big update
- The Ultimate classes Lord, King, God and Dragon scale your existing Ultimate skills.
- Four Magic Knight guide pages in the Wiki.
- Anime-style skill effects for Tensura, DanMachi and Dragon Ball skills, all 27 Beast Wills, all hero weapon kinds, devils, the Black Sword Greed and grimoire attributes.
- Dragon Ball techniques, the Super Saiyan God ritual, Saiyan stages and the Giant Ki Avatar.
- Lucifero and the God of Order become moving Blender models.
- Boss-core armour, the meditation screen, the two-power limit and late-game damage scaling.

---

## Removed on request
- **The Faith system** (the Faith skill, Goddess of Faith and the Faith gods), removed in 4.18.0. Old saves load without it.
- **The Black Wings skill** (4.3.0). Its id stays registered as an inert stub so old worlds load.
- **The old flat weapon skins** for 22 bows and spears (4.62.1), which makes the jar about 6 MB smaller.

## Good to know
- Everything new has a setting that restores the old behaviour. Server settings change gameplay; client settings only change how things look.
- Existing config files keep their old values; delete a line from a config file to get the new default.
- New features were written to stay light on public servers: bounded work per tick, bounded packets, and effects sent to nearby players only.
- Not everything has been seen in a game by the author. Please report anything odd with your `latest.log`.

*Small build-fix and test-fix releases in between are not listed separately.*
