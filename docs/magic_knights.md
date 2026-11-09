# The Magic Knight system (3.67.0 and 3.68.0)

## What was already there (KEPT)
Ranks Junior / Intermediate / Senior (classes 5th to 1st), Grand Magic Knight and the Wizard King; gold and black stars with automatic promotion (`starsPerClass`); FTB Teams squads (Captain, Vice-Captain, member, standings, rank limits to found and join); the Wizard King crown (the most net stars wins it); the grimoire ceremony; the robe sets of the squads; the questline chapter "Squads and Kingdoms".

## What 3.67.0 added
Gold stars from missions (questline advancements, 1 / 2 / 5, once per player). See `docs/mission_stars.md`.

## What 3.68.0 adds (NEW)
| Part | How it works | Settings (`nusmp-multiverse-server.toml`) |
|---|---|---|
| **Mission Board** | `/multiverse mission` lists three offers a day (a day is 20 real minutes) out of four kinds (hostile creatures, the undead, spiders and other arthropods, raiders), scaled to your rank (15 percent more kills per rank step). `take <1-3>`, `status`, `abandon`. Your own kills count by themselves (not a player, not a tamed pet). Finishing pays gold stars (one per few kills) through the normal star path. One mission at a time, a time limit, a daily cap, no penalty for abandoning. | `[board]` `enabled`, `perDay` (5), `minutes` (30), `rewardPercent` (100) |
| **Conduct** | Killing a villager or a wandering trader gives 1 black star, killing a member of your own squad gives 2; at most one such penalty per cool-down (60 s), so a rampage is not a pile-up. | `[conduct]` `enabled`, `villagerBlackStars`, `squadmateBlackStars`, `cooldownSeconds` |
| **Star Awards Festival** | Every N in-game days (28) the standings are announced to everyone and each online member of the top three squads gets gold stars (5 / 3 / 1). Offline members get nothing. `/multiverse squad awards` (admin) holds one at once. The first run only starts the clock. | `[awards]` `enabled`, `days`, `first`, `second`, `third` |
| **Squad styles** | The captain sets `/multiverse squad style <style>`; the squad takes the name, colour, captain and a line of one of the nine squads. Cosmetic: no number changes. Shown in `/multiverse squad info`. | none |

### The nine squads (facts from web search snippets, rule 10)
| Style | Captain | Line |
|---|---|---|
| Golden Dawn | William Vangeance | The elite squad; named the squad with the most stars at the Star Awards. |
| Silver Eagle | Nozel Silva | Led by Nozel Silva of the royal Silva family. |
| Crimson Lion Kings | Fuegoleon Vermillion | The fire mages of the Vermillion line. |
| Blue Rose Maidens | Charlotte Roselei | Charlotte Roselei's squad. |
| Black Bull | Yami Sukehiro | Known for its negative star count. |
| Aqua Deer | Rill Boismortier | Painting Magic; once the Grey Deer under Julius Novachrono. |
| Green Praying Mantis | Jack the Ripper | Slash Magic; recruits commoners. |
| Purple Orca | Kaiser Granvorka | Took the captaincy over from Gueldre Poizot. |
| Coral Peacock | Dorothy Unsworth | Dream Magic. |

What the anime says that the system follows: stars come by how hard the task was; a year-end Star Awards Festival names the squad with the most stars (the Golden Dawn at the top, the Black Bulls with negative stars); promotion comes with stars; becoming a Magic Knight means being accepted by a captain after the entrance exam.

## Sources and what could not be fetched (rule 10)
The English wiki (blackclover.fandom.com), en.wikipedia.org, ja.wikipedia.org, a Wikipedia mirror and sportskeeda could not be opened from the build session (the hosts do not resolve; page fetching is blocked). The facts above come from web search result summaries that quote the Black Clover Wiki, MyAnimeList, Sportskeeda, Ranker, Epicstream and fan sites. Where sources disagreed (Charlotte Roselei's squad, the Aqua Deer being "Azure Deer" on one site, Jack the Ripper's fate) the better supported reading is used or the detail is left out. Captains change over the story; these are the captains named in the sources. Unconfirmed: the colours of Blue Rose, Aqua Deer, Green Praying Mantis and Purple Orca (taken from the squad name).

## Not in this release
The Entrance Exam and captain selection, squad hideouts, captain-only powers, a graphical mission board, an offline payout for the festival, and a numeric perk for a style.

## Cost (rule 11)
No tick work. One event per creature a player kills (a tag test and a profile read), a few integer operations per offer, the festival check once every 10 seconds, the festival itself one pass over the squads once per period. No entity, block or chunk work, no packet of its own. State: one small record per player in the profile (the board, the conduct clock) and one integer in the world.

## Code
`SquadStyle`, `MissionBoardRules`, `StarAwardRules` (pure), `MissionBoard`, `KnightConduct`, `StarAwards`, `Squads` (style), `MultiverseCommands`, `MultiverseConfig`; tests `KnightSystemRulesTest`, `KnightSystemSourceScanTest`; smoke keys `knights.rules`, `knights.player`.
