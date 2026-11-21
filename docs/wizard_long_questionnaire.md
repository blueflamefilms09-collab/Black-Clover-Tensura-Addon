# The long Tower Wizard questionnaire

Server setting `wizard.longQuestionnaire` (multiverse server config, default **true**). Off = the old 12-question sheet and the old leaf rule, exactly as before.

## What changed, in plain words

The Tower Wizard used to ask 12 questions drawn from a small pool, mixed in one shuffled list, and the leaf count (3, 4 or 5) was mostly luck: two resolve questions plus a 0..2 roll.
Now (setting on) the wizard asks **30 questions in five parts**, and the leaf count is decided by **six resolve questions** plus a tiny luck roll (0 or 1).

| Part | Decides | Old sheet | New sheet | Old pool | New pool (old kept + new added) |
|---|---|---|---|---|---|
| 1 Nature | the magic attribute | 6 | 12 | 20 | 52 (+32) |
| 2 Hearth | the kingdom | 2 | 4 | 4 | 10 (+6) |
| 3 Blood | the Tensura race's blood | 2 | 4 | 5 | 11 (+6) |
| 4 Origin | the social class | 1 | 4 | 2 | 12 (+10) |
| 5 Resolve | the leaves (3 / 4 / 5) | 1 | 6 | 2 | 14 (+12) |
| | | **12** | **30** | 33 | 99 (+66) |

Inside a part the questions are random and the answers of every question are still shown in a random order. The old questions were not touched; the new ones are appended (`wizard/LongQuestions.java`). Nothing in a question names a magic (a test checks it), and every one of the 16 flavors (Hot, Cold, Flow, Gust, Stone, Metal, Gem, Green, Bright, Shade, Soul, Space, Time, Body, Mind, Maker) is worth points in at least 6 of the new Nature questions, so every magic attribute stays reachable.

### New vs replacement (CLAUDE.md rule 5)
- NEW: 66 questions, the five-part ordering, the section ids on the sheet / packet, the result summary, the stored resolve score, `LeafRoll`, `SheetSections`, the setting, the self-test.
- REPLACED: nothing. The old pools, the old 12-question draw, the old leaf rule and the old tie rule stay and are used when the setting is off.

## The parts on screen
The sheet carries one section id per question (`"C"` in each question tag; optional, so an old sheet without it still opens). The screen shows "Part 2 of 5: Hearth (3/4)" as the header and the overall count ("7 / 30") at the right. An old sheet shows the old header. The screen never shows what an answer is worth (the points stay on the server).

## The leaf odds
Leaves = total of the six resolve answers + a luck roll of 0 or 1, then: 18 or more = 5 leaves, 15 or more = 4 leaves, else 3 (`wizard/LeafRoll.java`: `FOUR_FROM = 15`, `FIVE_FROM = 18`). Five leaves still need the forbidden-start rule (`allowFive`), else they become four.

Resolve answers are worth 3 (the best, often two or three roads tie: grit, humility, a friend, a promise), 2 or 1 (middling) and 0 or -1 (arrogant, lazy, boastful). The loud answer is not always the best: boasting is -1, thanking a rival is 3.

Measured by the seeded Monte Carlo test `LeafRollTest` (60,000 sheets each):

| Player | 3 leaves | 4 leaves | 5 leaves |
|---|---|---|---|
| Random answers | 78.3% | 18.8% | 2.9% |
| Always the best resolve answer | 0% | 61.5% | 38.5% |
| Old rule, for comparison (setting off) | mostly 3-4, luck-driven | | |

Honest note: the aim was 78 / 18 / 4 for random and "mostly four, sometimes five" for the best player. With six independent answers and a luck of only 0 or 1, a random player cannot reach five leaves much more often than 3% without the best player getting five most of the time, so 2.9% / 38.5% is the closest balance found (searched numerically; a higher random-five share pushes the best player past 55% five).

## The answers matter (long sheet only)
- **Less luck.** The luck roll is 0 or 1 (`rng.nextInt(2)`), not 0..2.
- **Ties follow the later answers.** When two natures, kingdoms, bloods or classes tie on points, the one the later answers lean to wins; random only decides if even that is a tie. So a consistent player gets what they steered towards. Tests: a hand-built all-hot path always gets a fire-family magic (40 of 40); on real sheets, taking the hot answer whenever it is offered gives a fire-family magic in 268 of 300 sheets against 43 of 300 for random answers.
- **A summary after the last answer.** The wizard says one line per part, winner and how clear it was in words (clear / leaning / narrow), never the points of options not chosen:
  `Nature: Wind (clear), with a pull towards Fire` / `Hearth: Spade (leaning)` / `Blood: Elf (clear)` / `Origin: Noble (narrow)` / `Resolve: 14 of 18, a four-leaf grimoire`. Two fused natures read `Nature: Fire and Water, fused into Glass (finely balanced)`. Built by the pure `Questionnaire.summary(Result)`.

## What each part does in the game
- **Nature**: the grimoire's magic attribute (fusions as before).
- **Hearth**: the kingdom: grimoire suit and a buff / debuff (`mana/Heritage`, existing).
- **Blood**: the race's buff / debuff (`Heritage`, existing).
- **Origin** (social class): already real: attribute modifiers, spell cost and power multipliers and the star-gain multiplier (`Heritage.socialMods / socialCost / socialPower / starMult`, `MultiverseProfile.socialClass`). Nothing new needed.
- **Resolve**: sets the leaf count of the cover, and the total is now kept on the player: persistent data `nusmp_resolve` (the score) and `nusmp_resolve_max` (the best that sheet allowed), per player, never cleared, for the later rebirth feature to read (`wizard/ResolveScore`). Nothing else reads it yet.

## Cost (CLAUDE.md rules 9 and 11)
- Server: one sheet draw per click on the wizard (shuffles of pools of at most 52 entries), one `result()` per answered sheet, one pending sheet per player (the existing map, cleared on answer or timeout). No per-tick work, no scans, no spawns.
- Network: one packet to the one player who clicked, about 6 KB measured (30 questions), and one answer packet of 30 ints back. The server checks the answer count and every index before reading anything (a wrong-sized or out-of-range answer is refused). Far under the packet limits.
- Client: one question screen at a time, a few strings per frame; no per-frame allocation added beyond the header string.
- Chat: five short lines to the one player after the last answer.

## Tests and self-test
- `LeafRollTest`: the Monte Carlo bands above, the old rule exactly, luck 0 or 1.
- `QuestionnaireLongTest`: counts, every new question parses (4 options, valid tokens, right token kind per pool, fits the button), unique texts across all pools, old questions still present, all 16 flavors reachable, no option names a magic, resolve value mix, section order and sizes, answers still shuffled, old sheet unchanged and without sections, packet size, progress line, summary lines, steering, tie-breaking by later answers, bad answers refused, five leaves still need `allowFive`.
- Self-test `wizard.longq.sheet` (smoke prefix `wizard.longq`, listed in `SMOKE_EXPECT` just before `scaling`): seeded sheet size and sections, old sheet kept, best and random path give 3..5 leaves, the summary has five lines, a wrong-sized answer list is refused.

## Not done / not checked
- Not seen in a game: the screen header layout (long option text and the new header were not looked at on a real client at all GUI scales).
- No second kind of questionnaire for non-grimoire paths; the resolve score has no reader yet (the rebirth feature).
- The five-leaf share for random answers is 2.9%, not 4% (see the note above).
- No preview image (no new visual).

## No answer time limit (4.89.1)

Owner report: the 30 questions took too long, because the wizard threw the sheet away after a fixed 5 minutes ("You took too long"). Now there is no time limit: server setting `answerLimitMinutes` (multiverse config, default 0 = none). A value above 0 restores a limit in minutes (5 = the old one). The wizard still wants you within 20 blocks of him. An open sheet is dropped when the player logs out (one small entry per player, no per-tick work, no packets). The number of questions is unchanged (30); a smaller sheet is a separate decision.
