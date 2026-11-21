# Canon leaves

One master setting, `canonLeaves` (SERVER config, section `black_clover`, default true). **false = exactly the 4.84.2 behaviour everywhere.**
Every other setting below only matters while `canonLeaves` is on, and each one has a value that switches only its own part off.

## Sources

The English Black Clover wiki was not reachable when this was written, and the Japanese wiki was not tried. Every canon fact below is from the lead's memory
of the series, not from a fetched page. Nothing could be fetched at all. Please check these before treating them as final:

- Three leaves = Faith, Hope, Love; the grimoire of almost everyone.
- The fourth leaf = Luck / Good Fortune; rare; a sign of being chosen (Yuno).
- The fifth leaf = the Devil's leaf; forbidden, devil-bound (Asta's anti-magic book).
- Suit grimoires (Spade, Heart, Diamond) have covers of their own kingdom, ranked by the same leaf count.
- `BLACK_MAGIC` and `GOD_TIER` are invented by this mod and say "Mod-only, not canon".

## What it does

| # | Part | New or replacement | Where |
|---|------|--------------------|-------|
| 1 | One meaning line for every cover (tooltip, Wizard result, `grimoire cover` command) | NEW | `grimoire/GrimoireMeaning`, `CanonTooltip`; enum lore untouched |
| 2 | Four-leaf and Five-Sided Diamond are luck covers: a lucky hit chance | NEW | `CanonLeaves.luckFactor`, hook in `GrimoireBook.pageDamage` |
| 3 | The plain altar / ceremony / Wizard roll no longer gives the five-leaf; it is devil-bound and carries a price | REPLACES the five-leaf weight (the old weight returns with `canonLeaves=false`) | `CanonLeaves.tierWeights`, `Ceremony.rollTier`, `NUGameRules.forbiddenStart` |
| 4 | Mana and level matter: overreach cost and mastery bonus | NEW | `grimoire/GrimoireMastery`, hooks in `GrimoireBook` |
| 5 | `/anime_multiverse grimoire cover <player>` | NEW (the old `setcover` already existed and is unchanged) | `GrimoireCommand` |

KEPT: the +5% page chance of the luck covers, all enum entries, the leaf power and leaf cost of 4.84.0, BLACK_MAGIC and GOD_TIER weights, the old 12-question sheet, `setcover`, `give`, `roll`.

### 1. Meaning text
`GrimoireMeaning.text(cover)` (pure, never empty). The tooltip uses `anime_multiverse.grimoire.meaning.<cover>` (lang fragment `tools/lang/canon_leaves.json`, merged into en_us), with the same English as fallback.
The Wizard result prints it as one extra line after "Your magic: ...". The Codex has no grimoire page that lists covers (the codex classes of the mod are the Tensura compat books), so no hook was added there.

### 2. Luck cover
`luckFactor(cover, canon, chance, mult, random)`: for FOUR_LEAF and FIVE_SIDED (`GrimoireCover.isRare`) a spell hit is multiplied by `luckMult` (default 1.5, clamped 1..10) with probability
`luckChance` (default 0.06, clamped 0..1), decided from the caster's own random, once per hit. Chance 0 never, chance 1 always. Not a per-tick cost, no packet.

### 3. Five-leaf is devil-bound
- The ceremony tier weights: with canon on the five-leaf weight (`weightRare`) moves to the four-leaf weight (`weightUncommon`); the sum stays the same; black and god weights are kept.
- `NUGameRules.forbiddenStart` is false with canon on. That is what the legacy soul roll (`GrimoireAcceptance.roll`) and the Tower Wizard questionnaire (`Questionnaire.result(..., allowFive, ...)`) already read, so both cap a five at the four-leaf. This also covers the long questionnaire of 4.85.0 (`LeafRoll`): its five is capped by the same `allowFive` line, nothing in `LeafRoll` or `Questionnaire` changed. The Wizard result says that the fifth leaf is earned through despair or a devil (only shown with canon on and a four-leaf cover).
- Paths that still give the five-leaf (checked): **despair** (`GrimoireMagicSkill.onDeath`, `GrimoireCover.despair()`: four-leaf -> five-leaf, double spade -> triple spade, chance `despairChance`), **the devil contract** (`ForbiddenMagic.bind`, which already darkens the cover to FIVE_LEAF / TRIPLE_SPADE), the admin commands `grimoire give <player> 5` and `grimoire setcover`, and the creative tab. No extra route was added: the contract path is already the obvious hook.
- The price (covers with `isForbidden()`: five-leaf, triple spade, black magic; not the god-tier):
  - (a) the devil holds `devilMaxManaShare` (default 0.10) of the wielder's max mana. The mod has no simple max-mana hook (the pool belongs to Tensura and a reduction would be a permanent attribute change), so it is expressed as extra cost per spell, `1 / (1 - share)` (x1.11 at 10%), in `GrimoireBook.getMagiculeCost`.
  - (b) after a successful cast that leaves the wielder under 15% of max magicule, the devil takes 2% of max health. At most once per second per player (20 ticks), never below one heart (2 hp), so never lethal, and never for creative, spectator, invulnerable or dead players. It is a direct health change, not a damage event, so no pvp rule or damage hook is involved. The health is taken after the cast; it does not refund mana.

### 4. Mana and level
The wielder's level is the mod's existing level notion, the EP (`TensuraBridge.maxEp`, the value `BalanceLaw.epScale` reads). A band is one power of ten above 1,000 EP (1k = 0, 10k = 1, 100k = 2, 1M = 3).
A cover asks for a band: tier 3 = 0, four-leaf = 1, five-leaf = 2, black = 2.5, god = 3.
- Cost: `1 + (overreachMaxCost - 1) * (1 - exp(-over / 2))`, `over` = bands the cover is above the wielder. Default max x1.5, in `1..3`, never below x1; 1 = off.
- Damage: `1 + masteryMaxBonus * (1 - exp(-surplus / 2))`, `surplus` = bands the wielder is above the cover. Default max +12%, in `0..1`; 0 = off.
Both are finite for any EP (NaN, negative, infinity read as band 0).

### 5. Command
`/anime_multiverse grimoire cover <player>` (op level 2, read only): cover, meaning, leaf power, leaf cost, the level+devil cost factor, luck, devil share.
`setcover` already existed with the same shape (`GrimoireAcceptance.grantExact`), so it was left as it is.

## Settings (SERVER, `black_clover`)
| Key | Default | Range |
|-----|---------|-------|
| `canonLeaves` | true | master |
| `luckChance` | 0.06 | 0..1 |
| `luckMult` | 1.5 | 1..10 |
| `devilMaxManaShare` | 0.10 | 0..0.5 |
| `overreachMaxCost` | 1.5 | 1..3 |
| `masteryMaxBonus` | 0.12 | 0..1 |

## Worst-case cost (rule 11)
- Packets: none new.
- Per spell hit: a few multiplications, one `nextDouble`, one EP read (the same read `BalanceLaw.damage` already does).
- Per mana cost check: one EP read and two multiplications.
- Per cast of a five-leaf or above: one magicule read, one comparison; at most one health write per player per second.
- State: one `long` per player who ever paid the devil, in a server-thread map keyed by UUID, removed on logout. Worst case = the players online.
- Client: the tooltip adds up to three text lines on a grimoire item; nothing per frame.
- No entity, no scan, no allocation per tick.

## Tests
- Pure: `GrimoireMasteryTest`, `CanonLeavesTest` (meaning text and the two mod-only flags, luck with an injected random, clamps, ceremony weights on/off with equal sums, the devil price never lethal and rate limited, mastery monotone, finite, >= 1).
- Self-test prefix `canon.`: `canon.meaning`, `canon.ceremony`, `canon.luck`, `canon.price`, `canon.mastery` (registered in `NUSMP` next to `ScalingSelfTests`, class `CanonLeavesSelfTests`).

## Not verified
Nothing was played in a game. The numbers (6%, x1.5, 10%, 2%, x1.5, +12%) are a first proposal. The 15% low-mana test reads Tensura's magicule right after the cast; it was assumed that `EnergyHelper.isOutOfEnergy` has already taken the cost by then.
