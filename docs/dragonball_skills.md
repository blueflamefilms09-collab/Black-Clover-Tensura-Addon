# Dragon Ball skills (3.56.0)

Fifteen characters, each a **Unique** skill that evolves into an **Ultimate** once mastered (the same rule as the other Unique -> Ultimate pairs: mastered Unique plus the server's minimum EP, checked every 5 seconds; the older evolution table is untouched). All of it is data in `skill/rules/DbRules.java`; what a mode does is one of 13 active and 17 passive primitives (`DbMoves`, `DbPassives`).

* **Obtain:** `/multiverse dragonball give <player> <id> [unique|ultimate]` (permission 2), `/multiverse dragonball list`, or `/tensura edit <player> ability grant nusmp:<id>_unique`.
* **Passives** sit under the skill's own ON/OFF toggle (the Unique form has only its first passive, at half strength). **Actives** are on the mode wheel; "Crouch + Press" modes need sneaking; the finisher answers only a mastered skill.
* **Scaling** is by the Super Hero (2022) standing of the form each skill is based on (`scale`): Gohan Beast 1.00 (the top), Goku 0.97, Vegeta 0.95, Piccolo (Orange) 0.92, Future Trunks 0.80, Android 18 0.78, Android 17 0.76, Trunks 0.72, Goten 0.70, Tien 0.62, Krillin 0.60, Roshi 0.58, Yamcha 0.45, Chiaotzu 0.42, Bulma 0.30. A Unique is half an Ultimate. Damage then goes through the Balance Law and the boss cap like every other skill.
* **Setting:** server `dragonBallEnabled` (default true); false = every mode refuses and the passives do nothing.

| Id | Skill | Unique | Scale | Passives | Actives |
|---|---|---|---|---|---|
| `db_goku` | Wukong, Lord of Limitless Combat | Saiyan Prodigy | 0.97 | Autonomous Ultra Instinct, God Bind | Energy Landmine, Evil Containment Wave, God Kamehameha (mastery) |
| `db_vegeta` | Vegeta, Lord of Pride | Saiyan Prince | 0.95 | Royal Authority, Ego's Fuel | Galick Gun, Big Bang Attack, Final Flash (mastery) |
| `db_gohan` | Gohan, Lord of the Beast | Half-Saiyan Scholar | 1.00 | Beast's Awakening, Latent Rage | Masenko, Super Kamehameha, Beast Special Beam Cannon (mastery) |
| `db_piccolo` | Piccolo, Lord of Namek | Namekian Tactician | 0.92 | Pride of Namek, Orange Awakening | Light Grenade, Hellzone Grenade, Special Beam Cannon (mastery) |
| `db_krillin` | Krillin, Lord of Technique | Earth's Strongest | 0.60 | Veteran's Instinct | Expanding Energy Blast, Kamehameha, Destructo Disc (mastery) |
| `db_android18` | Lazuli, Lord of Infinite Energy | Infinite Cyborg | 0.78 | Infinite Reactor | Energy Shield, High Speed Rush, Destructo-Disc (mastery) |
| `db_android17` | Lapis, Lord of Barriers | Park Ranger Defender | 0.76 | Perpetual Stamina | Energy Shield, Android Kick, Super Electric Strike (mastery) |
| `db_goten` | Goten, Lord of Inheritance | Hybrid Prodigy | 0.70 | Prodigy's Pace | Assault!, Full Power Energy Blast Volley, Super Kamehameha (mastery) |
| `db_trunks` | Trunks, Lord of the Future | Capsule Corp Heir | 0.72 | Tactical Retreat | High Speed Rush, Full Power Energy Wave, Finish Buster (mastery) |
| `db_tien` | Tien, Lord of the Crane | Tri-Clops Grandmaster | 0.62 | Third Eye Vision | Ki Blast Cannon, Dodon Ray, Neo Tri-Beam (mastery) |
| `db_yamcha` | Yamcha, Lord of the Wolf | Desert Wolf | 0.45 | Desert Bandit's Agility | Kamehameha, Wolf Fang Fist, Chou Soukidan (mastery) |
| `db_chiaotzu` | Chiaotzu, Lord of Telekinesis | Psychic Emperor | 0.42 | Psychic Float | Telekinesis, Psychic Rock Throw, Farewell, Mr. Tien (mastery) |
| `db_roshi` | Roshi, Lord of the Turtle School | Turtle Hermit | 0.58 | Hermit's Wisdom | Afterimage Strike, Evil Containment Wave, MAX Power Kamehameha (mastery) |
| `db_future_trunks` | Trunks, Lord of Hope | Timeline Savior | 0.80 | Chronology Denial | Burning Attack, Finish Buster, Shining Sword Attack (mastery) |
| `db_bulma` | Bulma, Lord of Invention | Genius Inventor | 0.30 | Genius Intellect | 3D Scan Mode, Capsule Toss, Dragon Radar Ping (mastery) |

## Cost (rule 11)
A press: one entity query, at most 24 creatures hit, at most 24 projectiles removed, one beam or ring of effect, no spawned entities, no terrain touched. Passives: once a second per owner whose skill is ON, at most one query of 24 creatures per passive. The damage listener returns at once unless a player with the form tag is attacker or victim; Latent Rage walks the level's players only when a player is hurt. Per-player state is in the player's own data and attribute modifiers, removed when the skill is toggled off, forgotten or the tag is cleared. No packets of its own.

## Honest notes
* **Not checked against a wiki.** The move names and effects come from the owner's list; the English and Japanese wiki sites could not be reached from the build session, so every move detail is from memory and unverified.
* Simplified: Energy Landmine is an instant blast around you; Hellzone Grenade / Wolf Fang Fist / Volley / Chou Soukidan are bounded multi-hit moves on one target; Psychic Rock Throw does not move terrain; Capsule Toss gives a random buff set, no vehicles; Genius Intellect shortens the owner's own cooldowns by 30% and gives nearby allies Haste; Infinite Reactor refills 6% of the maximum Magicule each second (and the skill's own costs are 0); Evil Containment Wave locks and silences, it does not seal into an item; Goku's Autonomous Ultra Instinct is a dodge chance.
* The header "Modes: N" of the owner's file does not always match the list under it; the skills have the listed modes.
* Not seen in a running game: balance, the icons in the skill list.
