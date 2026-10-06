# The Convergence (0.39): server owner's guide

The server starts as pure Tensura. A few players are **anomalies**: their magic comes from the Black Clover world. As the season goes on, the four kingdoms bleed into the world one by one.

Config: `serverconfig/nusmp-multiverse-server.toml`, section `[convergence]`. Turn it off (`enabled = false`) to get the old rules back (everyone eligible at the Acceptance Ceremony).

## Who becomes an anomaly
- **One roll per account, on the server, the first time they join.** Logging out and back in, or joining with an alt, cannot reroll it.
- **Default odds:** `anomalyChance = 5.0` percent.
- **Pity rule (on):** after `pityEvery = 20` Tensura-origin players in a row, the next player is an anomaly.
- **Kingdom:** each anomaly also rolls a kingdom: `weightClover 50`, `weightDiamond 20`, `weightHeart 20`, `weightSpade 10`. On average that is about one Spade anomaly per 200 players.
- **The kingdom picks the covers.** Clover gets the full ladder (3-, 4- and 5-leaf, Black Magic, God-Tier). Spade gets Spade, Double Spade, and the devil-inhabited Triple Spade. Diamond gets Diamond and Five-Sided. Heart gets Heart covers.
- **Existing grimoires are kept.** Players who already had a grimoire before 0.39 become anomalies of their cover's kingdom.
- **Tensura progression is untouched.** Everyone keeps their Tensura race, evolutions and skills. An anomaly is a Tensura character who also has a grimoire, and can use both systems.

## Awakening
1. After `detectMinutes = 10` minutes of play, the anomaly sees **[Unknown Magic Detected]**. Only they see it.
2. After `awakenMinutes = 30` minutes, they get the title **"You have been chosen by another world's magic."** and the world gives them their grimoire.
   - A Grimoire Tower or a Grimoire Altar can choose them sooner.
3. Everyone else only sees the anonymous line **⚠ The world has experienced an anomalous magical disturbance.**
4. Nobody else is ever chosen.

## Stages
| Stage | What happens |
|---|---|
| `SIGNS` | The starting stage. Players overhear rumours (a "Clover Kingdom", a book that flew by itself, a strange energy). Library ruins that match no history of the world. |
| `FIRST_GRIMOIRE` | Opens when the first anomaly is chosen. |
| `CLOVER` | "...You mean this isn't the Clover Kingdom?" Grimoire Towers start appearing in newly explored land. The Black Clover parts of the status panel and `/multiverse locate tower` open to everyone. |
| `DIAMOND` | A second rupture. Diamond researchers start studying magicules. |
| `HEART` | Forests change, mana thickens, spirits wake. |
| `SPADE` | Devils, then soldiers, then frost. **❄ THE SPADE KINGDOM HAS ARRIVED.** |

- **Advancing:** with `autoAdvance = true`, each stage opens after `stageDays = [3, 7, 7, 7]` real days in the one before (FIRST_GRIMOIRE, CLOVER, DIAMOND, HEART). An admin can jump at any time with `/multiverse convergence stage <stage>`.
- **Settlements, researchers, mana zones and Spade soldiers** are only flavour for now (rumours and stage announcements). They come in later versions, one stage at a time.

## Secret side quests (anomalies only)
- **Where they show up:**
  - A new quest is whispered in chat: **✦ Secret quest: …**
  - The open ones are listed in the Multiverse status panel, in the Tensura menu's custom area.
  - `/multiverse quests` shows the hints.
- **Rewards:** gold stars and grimoire mastery.
- **The quests:**
  - Unknown Magic Detected
  - Chosen by Another World
  - Words of Another World (first spell)
  - Ruins That Don't Belong
  - A Mage Must Practise (100 spells)
  - Survive the Other World (50 monsters with the grimoire out)
  - A Tower Without a Kingdom (CLOVER stage)
  - Proof of Strength (a boss)
  - What Is This Energy? (20,000 max magicules, DIAMOND stage)
  - The Forest Breathes (spirit bond, HEART stage)
  - Whispers from Below (30 night kills, SPADE stage)
  - One "homesick" quest per kingdom: Clover (3 gold stars), Diamond (25% mastery), Heart (20 spells cast in forests), Spade (25 night kills)
- **With FTB Quests installed:** the mod writes `config/ftbquests/quests/chapters/nusmp_convergence.snbt`, but only if that file is missing, so you can edit it.
  - Each quest completes through a hidden advancement (`nusmp:convergence/<id>`), so the book always matches the mod and nothing can be ticked off by hand.
  - The chapter stays invisible to anyone who never completes "Unknown Magic Detected".
  - Note that FTB Quests progress is shared within an FTB team.
- **Without FTB Quests:** the chat lines and the status panel are the whole system.

## The grimoire cannot be copied or plundered
- **A grimoire skill is only ever given by the world or an admin.** World means the awakening, the Ceremony or an altar. Admin means `/multiverse grimoire give|canon`, `/nusmp grimoire …` or binding a creative grimoire.
- **Tensura's skill plunder and skill learning are refused for grimoire skills.** That covers Predator, Gluttony, Usurper, Analyst-style copies, clones and bestowal.
- **Catch-all:** any grimoire skill that still turns up on a player who was never given one is taken back within 5 seconds.
- **Turn it off** with `grimoireGuard = false`.

## Commands (admin, permission level 2)
- `/multiverse convergence status` shows whether it is on, the stage, days in the stage, players rolled and anomalies.
- `/multiverse convergence stage <signs|first_grimoire|clover|diamond|heart|spade>` jumps to a stage, with its announcement.
- `/multiverse convergence origin <player> [tensura|clover|diamond|heart|spade]` shows or sets a player's origin.
- `/multiverse convergence reroll <player>` and `/multiverse convergence awaken <player>`.
- `/multiverse quests [player] [complete <quest> | reset]`.
- `/multiverse grimoire give <player>` still works for anyone. The admin's word is final.

## Known limits
- **JEI/EMI item lists and the creative tab still show Black Clover items.** Hide them with your pack's JEI or EMI config if you want total secrecy.
- **Chunks generated before the CLOVER stage keep their lack of towers.** Explore new land after the stage opens.
