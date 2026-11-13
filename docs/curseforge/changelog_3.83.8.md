## 3.83.8 - Saiyans, Dragon Ball skills, Magic Knight missions, devils of eight worlds, and all 21 hero weapons remade in Blender

This file covers everything since the last CurseForge upload (3.54.0): 3.55.0 to 3.83.8.

**New**
- **Saiyan race** (stages 1 to 3): Battle Power with six ranks, Ki as the Tensura magicule, Ki Blast and Rapid Barrage, Ki Flight (5 tiers), Saiyan Senses, Warrior's Reflex, Battle Adaptation, Zenkai and training meters. Son Goku ultimate skill with the Kamehameha and Rock, Scissors 'N' Paper. 16 transformations (Kaio-ken to Super Saiyan 4 Limit Breaker, God, Blue, Ultra Instinct). Techniques: Ki Barrier, Instant Transmission, Spirit Bomb, Meteor Combination, Dragon Fist, God Bind, Ki Deflection, Energy Transfer. New look: battle armour, flame aura, glowing eyes, fur and tail, an animated charge-up with ray burst, tremor and wind. Commands: `/saiyan`. Settings: `nusmp-saiyan-server.toml`.
- **Fifteen Dragon Ball characters as skills**: 30 skills (Unique -> Ultimate) from Goku to Chiaotzu. `/multiverse dragonball give|list`.
- **Adaptability**, a new toggled Unique skill: every kind of damage you take adds a stack that lowers that damage, and Severance hits can make you immune.
- **Magic Knight system**: Mission Board (`/multiverse mission`), conduct (black stars for killing villagers or squadmates), the Star Awards Festival every 28 days, the nine squad styles of the anime, and gold stars paid automatically for finished missions.
- **Devils of eight anime worlds** (`/devil worlds`): 26 devils from Black Clover, Chainsaw Man, Black Butler, Blue Exorcist, Tensura, Dororo, Berserk and Fairy Tail, each with its own ritual and price. The devil's attribute now acts in a fight from 20 % assimilation (ten new attributes, three new sacrifices). The devil shows itself at the end of a ritual.
- **18 new Beast Wills** (27 in all, from Fenrir and Jormungandr to Apophis), each a Unique -> Ultimate Tensura skill pair (54 skills). All 27 remade as real 3D cube bodies with idle animation, new arms and wings, and fierce heads with fangs and brow ridges. Solid 3D Beast Will Core.
- **Nen rework**: layered aura flames in your Water Divination colour, Ren burst, En dome, Gyo eyes, and six signature Hatsu.
- **Djinns**: each of the eight kinds of Djinn magic is drawn in its own element.
- **Hero weapons**:
  - Crystal of Release (craft it with a Gneiss Crystal) and `/multiverse hero abandon` let a player give up their weapon without an operator.
  - "Do you want to equip it?" with [Equip it] / [Not now].
  - Anime-strength damage and defence.
  - A Seven Star or Vassal Weapon, a Djinn or a Beast Will phase makes you deal 25 times the damage (`nusmp-power-server.toml`).
  - The four Legendary Weapons always stay on top (`legendaryMargin`).
  - From 3.55.0: x4 damage, half cooldowns and costs, and the Legendary Shield throws back 75 % of every blow.
- Black Clover bosses have 3 times the health and deal 3 times the damage (`bossRules.blackCloverStatMultiplier`, 1 = off).
- With a hero weapon in hand, right-click now eats and drinks from the off hand (`consumablesFirst`).

**Replaced**
- **All 21 hero weapons are now made in Blender**: the 4 Legendary (sword, shield, spear, bow with its three draw stages), the 7 Seven Star (hammer, claw, axe, whip, staff, gauntlet, projectile) and the 10 Vassal (katana, scythe, fan, harpoon, chain, carriage, book, mirror, instrument, sewing set). They are modelled, unwrapped and baked in Blender, with baked runes, inlays, engraving and glowing light. The four Legendary curse looks (Greed, Wrath, Lust, Pride) are Blender-made too.
- The blue spiral aura (used by about fifty abilities) is now a rising flame aura in the caster's colour.
- Beast Will looks, arms, wings and heads (see New); the Nen effects.

**Kept**
- The old hero-weapon models, complete. Turn on `classicHeroWeaponModels` in `nusmp-heroweapon-client.toml` (section `models`) to draw them again. The 16x16 inventory icons, ids, grips and engravings are unchanged.
- The old 27 Beast Wills (`beastwill.classicWills`, `beastwill.classicSpirits`, `flatSpirits`), the old devil, Saiyan and Nen basics, every other item and skill, and all saves.

**Fixed**
- A Tower Wizard and library books piling up until joining crashed (3.59.1).
- An Illusioner casting blindness with no target crashed the server (3.65.1).
- See-through rank cores, the Dragon Ball vs no-hunger rule, and several build fixes.

**Known issues**
- Seen in a real game (CI screenshots): all 21 hero weapons in first and third person. Not yet seen in a game: the four curse looks, the bow's draw stages, the glow in the dark, and a side view of the hold. Colours may differ slightly on real graphics cards.
- Some Saiyan, Dragon Ball and Beast Will balance is untested in long play.

**Update note:** remove any older `multiverse-of-anime-*.jar` from `mods` first. Install on both client and server.
