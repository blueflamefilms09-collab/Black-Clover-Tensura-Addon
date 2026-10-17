# Revchi Salik, the Chain Thief (`revchi_salik`)

Boss of the Magic Knights Entrance Arc, Saga 1 (saga `ELF_REINCARNATION`), tier 1, the first fight of the story. Classes: `arc/early/RevchiSalikBoss`, table `ChainTable`,
state `BindLedger`, record `EarlyRoster.REVCHI_SALIK`. Model: `tools/arc/char_revchi_salik.py`. Overview of the five early bosses: `docs/arc/early.md`.

## Canon (what the sources say, and how sure it is)

Research was done through a web search tool whose page fetcher could not reach any wiki (DNS refused), so everything below is from search digests of the
pages listed, not from reading the pages. Where the digests disagree the doc says so.

* Former 1st class junior Magic Knight of the Purple Orca squad, discharged after a mission that left a burn scar on the left side of his face; became a thief
  nicknamed "Revchi of Chain Magic". In the first chapter he ambushes Yuno outside the ceremony, takes his new grimoire and plans to sell it. Asta arrives, is
  knocked down by Dance of the Pitless Viper, then the tattered five-leaf grimoire gives him the anti-magic sword, which cuts the chains and ends the fight
  with one wide slash. He comes back as a minor villain in the Elf Reincarnation events (freed, joins Gueldre Poizot over their grudge against Asta).
* Magic: Chain Magic (Dance of the Pitless Viper, Scorpio Chain, Heavy Chain as offence; Magic Binding and Iron Chain Formation as support) and Creation
  Magic that shapes his chains into objects. "He uses the chains to bind opponents and seal their magic power, even if their power is far greater than his own."
  The last name is a pun on the Japanese word for chain (kusari).
* Look (one profile, in German): slim, middle-aged (32), 176 cm, long droopy hair, a large scar across the left side of the face, a greedy and calculating
  expression. Another review calls him "droopy-haired, manic".
* Sources: https://blackclover.fandom.com/wiki/Revchi_Salik , https://blackclover.fandom.com/wiki/Asta_vs._Revchi_Salik ,
  https://blackclover.fandom.com/wiki/Chain_Magic , https://villains.fandom.com/wiki/Revchi_Salik , https://blackclover.fandom.com/wiki/Arc_1 ,
  https://blackclover.fandom.com/wiki/Chapter_1 , https://anibase.net/de/character/4WKad/Revchi-Salik , https://myanimelist.net/character/146419/Revchi_Salik ,
  https://en.wikipedia.org/wiki/Black_Clover_season_1 .

## The fight: three mechanics, each with a counter

The lesson of the first fight of the story is the one the whole of Black Clover's villainy rests on: a binding spell is only as strong as your answer to it.

| Mechanic | What it does | Counters the party can learn |
| --- | --- | --- |
| **Magic Binding** (`BindLedger`) | A chain, drawn on the ground in the wind-up and locked at 45% of it (the line turns red), flies up to 22 blocks (stopped by walls) and holds the first foe in a 2-block-wide corridor: 6 damage, then a bind of 70 ticks (3.5 s, shortened by the balance law), slowness III, and a seal on the grimoire (`nusmp_sealed_until`, players only). One bind at a time per foe; the balance law's shared control lock keeps binds 12 s apart. | **Leave the line** after it turns red (or put a pillar or wall in it). **Tear it**: every point of damage the bound foe lands on him counts; each 2% of his health (at least 10 points) takes one of the two links, so a fighter is free after about 4% of his health (30 points at 750), a caster whose grimoire is sealed needs a weapon or an ally. **Anti-magic**: a holder of anti-magic is never bound, and an anti-magic blow frees at once (the way Asta's sword did). |
| **Prey** | A bound foe takes 30% more from every blow of his. | Do not stay bound: the bind is the setup, the damage is the cost of ignoring it. |
| **Total Lockdown** (ultimate, from 60%) (`ChannelGuard`) | At the start of a 56-tick wind-up he picks up to 5 foes (8 at the Gates) that he can see, nearest first, and draws a line to each; the lines follow them until the last 9 ticks, then lock. Chains fly along the locked lines: 12 damage and a bind each. | **Break line of sight** (the chain stops at walls). **Step off the line** in the 9 locked ticks (0.45 s). **Break the spell**: 12% of his maximum health inside the wind-up and the chains fall slack; he is staggered for 60 ticks and takes 25% more. If the spell fires, the framework's ultimate recovery (60 ticks) leaves him exposed anyway. |

The rest of his table is there to make the three above readable: lines and circles that are drawn first and then locked (Scorpio Chain, Heavy Chain, Thief's
Rush, Chain Pillars, Iron Chain Formation, Chain Whirl). He changes no block, spawns nothing, heals nothing; the framework's Gates give him the single refill.

## Move table (base damage; the framework scales it with power, victim EP and the balance law; DPS budget 6% per second)

| id | name | from | wind-up (Gates) | cooldown | range | damage | tell and counter |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `viper_dance` | Dance of the Pitless Viper | 20% | 18 (14) | 80 | 0-9 | 4.5 x 3 (x 4 at Gates) | three lines, one straight and one 55 degrees to each side, 7.5 blocks; step out of the cone |
| `scorpio_chain` | Scorpio Chain | 20% | 20 (15) | 150 | 5-18 | 8 + slow, pulls the foe 2.4 blocks from him | locked line, 18 blocks; leave it |
| `heavy_chain` | Heavy Chain | 20% | 24 (18) | 130 | 3-20 | 11 in a 3.2 circle, slowness III, driven into the ground | ring on the ground, follows the target for 55% of the wind-up, then locked |
| `magic_binding` | Magic Binding | 20% | 22 (17) | 270 | 4-20 | 6 + bind | a centre line and two side lines, locked at 45% of the wind-up (they turn red) |
| `chain_whirl` | Chain Whirl (crowd) | 20% | 20 (15) | 170 | 0-6 | 6.5 x 2 in 5.6 blocks, knock-back | ring that grows to the radius; get out |
| `thief_rush` | Thief's Rush | 20% | 20 (15) | 150 | 6-22 | 10, 16 blocks, 2.8 wide | locked lane |
| `creation_chains` | Creation Magic: Chain Pillars (crowd) | 40% | 32 (24) | 260 | 0-24 | 7 per pillar, up to 6 foes marked | a ring and a column on every marked spot; spots lock at half the wind-up |
| `iron_chain_formation` | Iron Chain Formation (crowd) | 40% | 44 (33) | 360 | 5-24 | 9 + bind in a ring of 6.2 (7.75 at Gates) | the ring is drawn and shrinks; leave it before it shuts |
| `total_lockdown` | Total Lockdown (ultimate) | 60% | 56 (42) | 1500 | 0-24 | 12 + bind | see above |

Plain blows come from the framework's melee at 70% of his attack. He wants to stay 4 to 9 blocks away (a skirmisher: chains reach where a blade does not).
At the Gates (second bar): four lashes, a cage 25% wider, eight lockdown chains, 10% larger.

## Numbers (record)

Health 750, attack 10, speed 0.30, armor 4 (tier 1 band 700-1100). Floors 10 and 20 on either road. Themes: shadow, spatial, gravity, infernal_foundry,
dwarven_forgehold. Story: first of the five (no prerequisite). Charter Grimoire (40% chance): sticky_steel_thread, shadow_bind, curse_bind, earth_lock,
magic_jamming, steel_strength, strengthen_body, pain_resistance (all in the Tensura 2.0.1.3 id reference, which `EarlyRosterTest` checks; none is a Unique or Ultimate skill by name,
though the mod's own rarity table is not machine-checked here; the in-game self test `arc.charter` checks every id against the skill registry; `tensura:usurper` never).

## Model

`tools/arc/char_revchi_salik.py`: slim, 1.95 blocks, shaggy dark hair, scar across the left cheek and eye, stubble, a knee-length dark coat with steel trim and
a tattered purple cape (what is left of the Purple Orca robe), a chain bandolier, steel chains coiled round both forearms and a swaying chain with a kunai blade
at his belt (the mod's own ChainLayer ends its chains in kunai blades), a dagger in the right hand. Glow: only the steel. Previews: see the report.

## Weapon

None. Revchi is not a member of the Seven Deadly Sins, so he gives no sacred treasure. If a weapon were ever made for the chains, fitting Tensura engravings
would be `severance` and `barrier_piercing`.

## Unverified

* Every colour of the clothes, the cape, the kunai blades, the hair colour (the sources give a hairstyle only).
* The exact numbers of his spells in the series (the fight is a design, not a copy).
* Nothing runs in a game here: the chain visual (`SEAL_CHAINS` following a foe), the pull of Scorpio Chain, the seal flag being honoured by the grimoire code
  are written against the APIs and type-checked, not seen.
