# Rank audit: every rank, tier or rarity the player reads, and the relic grade it now shows

Owner request: the rankings must use the grades the relics already use (stars, grade name, place), not a custom text. This file lists every place of the mod that showed its own rank, tier or
rarity (found by reading every `enum` with a rank-like name and every display site of it), what it shows now, and what was left alone and why.

## The rule

A rank or rarity the player reads is the **relic card line**: the grade's stars (`Grade.stars()`, seven stars, filled up to the tier), two spaces, the grade name (`Grade.label()`, "God Grade"),
and the place if there is one (" #4"), all in the grade's colour. It is one helper: `relics/Rankings.Rank.card()`, drawn through `tooltip/RankCards` (the table is `rankings.json`, key `"legacy"`).
The relic card itself (`UnifiedTooltipRules.banner`, `RelicTooltip`) builds the same string, so a Beast core, a Devil panel and a relic read alike.

* Items that have a rank in `rankings.json` (the Djinn, the Genesis weapons, rules by prefix and suffix) show the card in the one rank line under the name (`tooltip/RankTooltip.java:55`).
* The mod's own older ranks keep their enums (saves and packets still use the old names; owner rule 2) and are mapped to a grade in `rankings.json` `"legacy"`; the display asks `RankCards`.
* Setting: `rankDisplay.rankDisplayClassic` (client file `nusmp-rank-display-client.toml`, default false). On: every site below shows its old text again.

## Converted (what the player reads changed)

| rank | where it is defined | where it was shown (and now shows the card) | old text | grade table |
|---|---|---|---|---|
| every item of the mod with a rank (Djinn weapons and seals, Genesis weapons, rules) | `relics/Rankings.java`, `rankings.json` | `tooltip/RankTooltip.java:55` (one line under the name) | "Rank: God Grade #4" (no stars) | unchanged: item, Djinn order, rule, weapon table |
| Beast rank C B A S SS | `beastwill/BeastRank.java:7` | Beast Will Core tooltip `beastwill/BeastWillCoreItem.java:50`; Beast Will panel `client/beastwill/BeastWillClient.java:178` (own line, grade colour) | "From a rank S beast: Lion", "Rank S" | beast table below |
| Devil tier LOW MID HIGH SUPREME | `devil/DevilTier.java:7` | Devil panel `client/devil/DevilClient.java:245` and `:270` (own line, grade colour) | "Low Devil" only (kept as the title; the grade is the new line) | devil table |
| Saiyan rank six steps (`SaiyanRules.Rank`) | `saiyan/SaiyanRules.java:22` | Saiyan panel `client/saiyan/SaiyanClient.java:307` and `:345` (own line, grade colour) | "Low-Class" ... (kept as the title) | saiyan table |
| Magic Knight rank | `multiverse/KnightRank.java:4` | status screen `client/multiverse/MultiverseStatusScreen.java:119` | "3rd Class Senior Magic Knight" (kept, the card is added under it) | knight table |
| Loot rarity common to legendary | `labyrinth/loot/LootRarity.java:12` | loot gear tooltip `labyrinth/loot/GearTooltip.java:30` (under the header) | "Epic (depth 4)" in the rarity colour | loot table |
| Aptitude rarity (`Aptitude.Rarity`) | `avatar/aptitude/Aptitude.java:29` | Soul Forge aptitude slot tooltip `client/avatar/AptitudesPage.java:265` | name in a rarity colour only | aptitude table |
| Mana Core rank E D C B A AA S SS | `manacore/ManaCoreRules.java:19` | Mana Core tooltip `manacore/RankCoreItem.java:43` | "SS-Class Beast" | mana table |

### The mapping (old rank to relic grade; place where one grade holds two ranks)

| kind | old rank -> grade | why |
|---|---|---|
| beast | C -> Special, B -> Rare, A -> Unique, S -> Legend, SS -> God | a beast core only drops from rank A (Unique) up; an SS beast (a dragon) is God Grade, the same step as the SS Mana Core |
| mana | E Normal, D Special #2, C Special #1, B Rare, A Unique, AA Legend #2, S Legend #1, SS God | eight letters on the same scale as the beast ranks: the same letter is the same grade in both systems; two letters share a grade where the grades run out, the better letter has place 1 |
| devil | Low -> Rare, Mid -> Unique, High -> Legend, Supreme -> God | a devil contract is a pact with a being of a grade; the supreme devils are the God step |
| saiyan | Low-Class Normal, Elite Rare, Super Elite Unique, Legendary Legend, Divine God, Transcendent Genesis | six ranks on the grades by name; Transcendent is the top and the only Genesis |
| knight | Junior Rare, Intermediate Unique, Senior Legend, Grand God, Wizard King Genesis | the Wizard King is one person at a time, the single top |
| loot | Common Normal, Uncommon Special, Rare Rare, Epic Unique, Legendary Legend | the first five grades (the higher ones are the relics' and are never dropped; see `GearGrade`) |
| aptitude | same as loot | same five steps |
| rift (not shown) | Common Special, Rare Unique, Mega-Rare God | kept ready for a later display |
| ceremony (not shown) | Common Normal, Uncommon Special, Rare Rare, Black Legend, God God | kept ready for a later display |

## Not converted (and why)

| enum | where | why it stays |
|---|---|---|
| `RiftTier` COMMON RARE MEGA_RARE | `labyrinth/mega/rift/pocket/RiftTier.java:9` | never printed as a word to the player (pity and chance logic); mapped in the table so a later display is one call |
| `Ceremony.Tier` | `multiverse/Ceremony.java:123` | the outcome of a roll, not shown as a rank; mapped in the table |
| `GearGrade` NORMAL SPECIAL RARE | `labyrinth/mega/gear/GearGrade.java:14` | already the relic grade (same three names, same colour and label) |
| `SpiritDiveTier.Grade` | `book/SpiritDiveTier.java:24` | which look of a spirit dive (Standard, Yuno, Fuegoleon, Noelle): a model choice, not a rarity |
| skill `Tier` | `skill/rules/Tier.java:10` | mastered or not: a state of one skill |
| `UltimateClass` | `skill/rules/UltimateClass.java:8` | the kind of an Ultimate skill, not a strength order |
| `HeroClass`, `WeaponClass` | `heroweapon/HeroClass.java:9`, `heroweapon/rules/WeaponClass.java:10` | the family of a weapon (Vassal, Hero, ...) |
| `TierLetter` F to SSS+++ | `arc/tier/TierLetter.java:11` | the owner's tier list for balance and floor bands; printed only in balance reports and validation messages, never to a player |
| `StoneTable.Grade` | `dungeon/sentient/StoneTable.java:11` | which stone table a monster rolls from (internal) |
| `TrueDamage.Tier` | `boss/god/TrueDamage.java:16` | the weight of a boss move (percent of health), a damage size |
| `Pages15Rules.Tier` | `book/pages15/Pages15Rules.java:42` | the unlock step of a book page |
| `SocialClass` | `multiverse/SocialClass.java:4` | a character's birth class in the Black Clover story (flavour) |
| `BeastWillRules.Tier` CALM to OVERLOAD, `SaiyanSenseRules.Tier`, `SaiyanFlightRules.Tier` | `beastwill/BeastWillRules.java:20`, `saiyan/SaiyanSenseRules.java:13`, `saiyan/SaiyanFlightRules.java:14` | a state (strain) or a training step, not a rarity |
| chat lines of commands and system messages: "Promoted: ..." | `multiverse/MultiverseProfile.java:57`, `saiyan/SaiyanCommands.java:156` | one-line chat text without a colour card; the status screen and panels carry the card. Left, listed so the owner can decide |
| lang flavour: "Needs Senior rank", "X-Rank" item names | `lang/en_us.json` | page gates and names from the series, not the mod's rank |
| the Djinn's own place (1 to 36) | `rankings.json djinnOrder/djinnTiers` | already grades with places (Genesis 1-3, God 4-12, Legend 13-24, Unique 25-36); the tooltip only gained the stars |

## Open decisions for the owner

1. Mana Core letters share grades (D and C Special, AA and S Legend). If the owner wants the eight letters on eight separate grades, SS would be EX-Singularity, so the table keeps SS at God.
2. The two chat lines above still show text; they can go through `RankCards` next.
3. `SpiritDiveTier.Grade` is a look and left alone.
