# Avatar personalities, chatter, Yandere and the caster (AV2, release 1.51.0)

Everything here is NEW (nothing like it existed). The only existing thing touched is the contract enum `ChatEvent`, which got one value appended
(`JEALOUS_WARN`). Datapack namespace note: the mod id is `nusmp` (the owner's brief said `multiverse_mod`), so data is `data/nusmp/avatar_chatter/*.json`.

## What it does
* **Personality behaviour** (`AvatarPersonalityTraits`, pure table): follow distance, aggro range, retreat HP, attack speed, cooldown multiplier,
  Tsundere protection threshold (creator below 50% HP gives +25% damage, +15% speed), flags (Sadistic weak-point avoidance, Cowardly boss fear,
  Merchant loot pull, Gluttonous eating, Lazy idling, Stoic one word, Chunibyo renaming, Heroic villager defence, Scholar relic comments).
  Two personalities combine: follow distance, attack speed, retreat HP and protection threshold are averaged; aggro range takes the larger,
  cooldown multiplier the lower; every yes/no flag is OR-ed; style tags are united.
* **Chatter** (`AvatarChatterEngine`, `chatter/*`): 14 personalities x 7 events (COMBAT_START, CREATOR_LOW_HP, SKILL_CAST, NAMING, BETRAYAL, FRENZY, IDLE)
  x 14 hand-written lines (1386 lines with the Yandere jealousy warnings), loaded by a reload listener from JSON into an immutable snapshot.
  A hybrid Avatar draws 50% primary pool, 30% secondary, 20% a glued line (`HybridMixer`: first sentence of the "leading" personality + last sentence of
  the other, e.g. tsundere + heroic). Per Avatar per event cooldown and a global per-tick budget (`ChatterLimiter`). Chunibyo renames spells for the
  SKILL_CAST shout (`ChunibyoNames`). Output: `[Name]: line` to players within `chatterRadius`.
* **Casting** (`SimulatedPlayerCaster`, `AvatarCastSkillGoal`, `AvatarSkillList`, `AvatarCastPlanner`, `AvatarManaPool`, `AvatarCastMath`).
* **Yandere** (`YandereJealousyTargetGoal`, `YandereInterceptGoal`, `YandereFrenzyHandler`, `YandereFrenzyState`, `YandereMath`).
* `FtbTeamHelper`: soft team check through the existing `QuestTeams` and `Squads` reflection; false without FTB Teams.

## The caster, exactly (how the cost and the player-only code are handled)
Grimoire pages are `BookPage.Cast` lambdas taking a `ServerPlayer`. An Avatar is a mob, so each Avatar gets ONE lazily created body double,
`SimulatedPlayerCaster extends FakePlayer`, with the AVATAR'S OWN UUID and a name taken from the Avatar. It is never added to the level (no entity list,
no player list, no tab list, no advancements, nothing saved). Before every cast `sync()` copies position, aim at the target (`lookAt` the target's body),
both hands, health fraction, max health, tick count, the Avatar's Tensura max magicule and max EP. After the cast `syncBack()` copies what the spell did
to its caster (motion, a short dash, potion effects) back to the Avatar.
* We do NOT call `GrimoireBook.castPage` (it needs a real key press, charge and the player's mana). We run the page's own `Cast` inside `SafeCast`
  (a faulty spell puts that book on its recovery cooldown like for a player) and pay ourselves.
* **Cost**: computed with the book's real formula, `book.getMagiculeCost(double, instanceCopy, mode)`, which reads the double's max magicule (mirrored from
  the Avatar). It is paid from `AvatarManaPool`: max = the larger of Tensura's magicule maximum for the Avatar and a floor of 600 + 40 per INT above 10;
  the current value lives in the Avatar's persistent data (`nusmp_av_mana`) and regenerates lazily (full in 120 s). Nothing is taken from the creator.
  A frenzied Yandere pays nothing and burns its own health instead (`frenzyHealthBurnPerCast`, floor 1 HP).
* **Cooldowns**: per page id in the Avatar's persistent data (`nusmp_av_cd`), page cooldown x INT/WIS multiplier (each point of combined D&D modifier
  shaves 3%, x0.5 to x1.25) x personality multiplier (Lazy 0.7) x `cooldownMultiplier()` (frenzy 0.1, never applied twice); plus a global gap between casts.
* **Book**: the Avatar casts from a private COPY of the creator's grimoire instance (or `HasGrimoire.avatarGrimoire()` if the entity provides one), refreshed
  every 30 s; only pages the creator has unlocked are castable.
* **Damage**: spells hurt with the double as the attacker. `AvatarAiEvents.onIncoming` (HIGH priority) cancels such a hit and re-deals it with the AVATAR
  as attacker (the double carries the Avatar's UUID, so even a projectile that loses its cached owner resolves to the Avatar). Area pages hit "everyone
  but the caster", so the same handler drops hits on the Avatar itself, its creator, the creator's FTB team, the creator's other Avatars and pets, and villagers
  (unless it is the Avatar's chosen target).
* **Whitelist** (`AvatarSkillList`): ATTACK pages are chosen by id tokens (strike, shot, bolt, lance, ball, blast, slash, shard, volley, barrage ...)
  minus an explicit deny list and deny words (summon, corpse, domain, armour, heal, seal, gate, door, key, time, swap, dice, wall, cage, trap, ...), cost capped at
  20% of max magicule (signature pages are never cast), plus a six-id self-buff list. Tensura magic goes through the mod's existing `TensuraCaster`
  (the way bosses and summons cast it) with ten attack-magic id keywords (fire ball, fire lance, stone shot, mud spears, icicle lance / spear, thunder lance /
  orb, water cutter, wind cutter), only if the Avatar has no grimoire page ready (or 1 time in 5).

## Yandere
* **Jealousy** (`yandere` personality as primary or secondary, loyal Avatar, `yandereJealousy`): anyone (player, NPC, villager, mob) within `yandereJealousRadius` (5)
  of the creator is WARNED first ("Step away from {creator}." etc., 20 ticks, no contact), then body-blocked (the Avatar stands between them and the creator),
  and attacked only if they act hostile toward the creator (target them or hit them) or stay 3 seconds. Never the creator, their FTB team / party, other Avatars of
  the same creator, the creator's pets, creative / spectator players, bosses. A Yandere's target change to the creator or team is cancelled.
* **Non-lethal** (`avatar.yandereLethal` = false, default): a Yandere's raw damage to a player is capped so the player stays at 1 HP or more (absorption counts).
* **Intercept**: arrows and charging attackers heading for the creator make the Avatar step into their line (short teleport, 30 tick cooldown, only into free
  space); the damage split gives the Avatar `yandereInterceptShare` (70%) of each hit on the creator, never more than leaves it at 1 HP.
* **Frenzy**: when the creator takes damage and a Yandere Avatar of theirs is within 48 blocks: `IS_FRENZIED` on (the renderer's red eyes), +30% speed,
  cooldown x0.1, costs waived, health burn per cast, the attacker becomes its target, FRENZY barks, red spore and smoke particles. It lasts 15 s and refreshes while the
  creator keeps being hurt; it always ends in a Slowness II exhaustion (6 s). State is two ints in the Avatar's saved data, so a reload cannot leave the flag stuck.
  Not for an Avatar that is no longer loyal (betrayed or independent): `AvatarGates.loyal` asks `isLoyal()` reflectively.

## Personality goals
`PersonalityGoals`: Retreat (Cowardly boss fear within 24 blocks, or any Avatar under its retreat HP), LazyIdle (creator still for 5 s), EatDroppedFood (Gluttonous),
DefendVillagers (Heroic: a hostile going for a villager within 20 blocks becomes the target). `AvatarWatcherGoal` (no flags): CREATOR_LOW_HP edge-triggered below 30%
(re-armed at 45%), idle remarks, Cowardly boss scare (+ short Resistance for the creator), Merchant nudging dropped relics and magic stones toward the creator (never taken),
Tsundere protective bonus, the personality attack-speed modifier. All goals read the personality LIVE (registerGoals runs before the synced fields are set, and AV1 calls
`install` several times).

## Config (SERVER, `nusmp-avatar-ai-server.toml`, class `AvatarAiConfig`, plain define / defineInRange)
`avatar.chatterEnabled`, `chatterRadius` (30), `chatterCooldownCombatStart` (20 s), `chatterCooldownCreatorLowHp` (15), `chatterCooldownSkillCast` (4), `chatterCooldownIdle` (90),
`chatterCooldownFrenzy` (6), `chatterBudgetPerTick` (3), `castEnabled`, `castGapTicks` (60), `yandereJealousy`, `yandereLethal` (false), `yandereJealousRadius` (5),
`yandereInterceptShare` (0.7), `yandereFrenzySeconds` (15), `yandereExhaustionSeconds` (6), `frenzyHealthBurnPerCast` (2.0), `frenzySpeedBonus` (0.3).

## Data
`data/nusmp/avatar_chatter/<personality>.json`: `{"personality":"TSUNDERE","events":{"COMBAT_START":["...", ...], ...}}`. A pack may add a file for the same personality;
its lines are ADDED to the shipped ones. Placeholders: {name} {creator} {target} {skill} {grimoire} {dungeon} {relic} {magicule}. The source of the shipped files is
`tools/avatar/chatter_data/*.py`; `python tools/avatar/gen_chatter.py` validates (>= 14 distinct lines per event, < 160 characters, known placeholders) and rewrites the JSON.

## Tests (pure, JUnit)
`AvatarPersonalityTraitsTest`, `HybridMixerTest` (determinism, the 50/30/20 split, combine rules, template filling), `ChatterDictionaryTest` (schema of every shipped file, merge,
bad files, limiter cooldown and budget), `YandereTest` (warn first, never lethal, burn floors at 1 HP, frenzy state machine), `AvatarCastTest` (whitelist, pool, cooldown math, planner).

## Unverified (nothing could be run in game)
* The `FakePlayer` constructor signature and that the double survives a page's player-only code (Curios, quest hooks, `ManaLaw`) without throwing; `SafeCast` is the net.
* Whether the registry ids of the Tensura attack magic really contain the ten keywords (if not, the Avatar simply casts no Tensura magic).
* That every page matching an ATTACK id is really safe: the page code is not audited one by one, the filter is by shape.
* `FtbTeamHelper` against a real FTB Teams (reuses the reflection that `QuestTeams` and `Squads` already do).
* The visual of the frenzy beyond the particles (AV3 renders the red eyes from `IS_FRENZIED`).
