# Faith fix: worshippers, offerings, chosen gifts

Owner report: the Faith skill ("face skill"; ids `faith` and `goddess_of_faith`) "does not fully work". A reference skill built the right way is Mitama Faith
("Masiba") in Tensura More Skills. Nothing could be run in a game here and there is no compiler in the session (CI is the only one), so everything below is
from reading the code and from `javap` output of the reference jars. **Read** = taken from javap/strings. **Inferred** = my reading of what the code must do.

## 1. What was wrong with our Faith (read in our source)

The code compiled and its arithmetic was tested, but as a skill it was half a system:

1. **Faith had one source, and a poor one.** Only Pray gave Faith: 1000 magicules and a 10 s wait for +1 (Goddess +3). A bestowal costs 10. A new player usually cannot afford
   even one prayer, so the skill looks dead. Nobody else could ever feed it.
2. **Bestow was a dice roll, not a choice.** One random skill from a two-skill config list (`magic_sense`, `sage`). The owner could never pick the gift.
3. **Bestow needed a second player.** Alone on a test world "Look at a player to bestow" is the only thing the skill ever says.
4. **No worshippers at all.** The skill is named Faith, but nobody has any relation to the holder. No recruit, no offering, no follower list, nothing saved per player.
5. **Pray paid with mode 0** (`pay(instance, player, 0, ...)`) although Pray is mode 1. The price still worked (the cost hook ignores the mode), but Tensura was told the
   wrong mode.
6. **Admin `faith add`** could wrap negative with a huge amount (int sum). Clamped later, but the wrap happened first.
7. Lang, icon, evolution table, Faith tag key and the three registry ids were all fine and are untouched.

## 2. What the reference does (javap, `MitamaFaithSkill*`, `MitamaOfferingActionPayload`, `MitamaOfferingMenu`, `MitamaFaithHandler`, `MitamaFaithConfig`)

- **Read:** five modes (Recruit Worshipper, Offering / Bestowal, Homing Divine Beams, Faith Skill Creation, Heaven's Edict). The god keeps a saved list of followers
  (tag `MitamaFollowers`, each with a worship value, `MitamaWorship` total). `addFollower`/`removeFollower`/`isFollower`/`cleanDeadFollowers`.
- **Read:** invitations: `PENDING_INVITES` map (god UUID -> invited UUID), `sendInvite`, and commands `accept`, `reject`, `status`, `offering` registered in the handler.
- **Read:** an offering menu with kinds `DONATE` (follower gives to the god), `GIFT` (god gives to a follower), `CREATE` (god makes a skill). The stats offered are
  `MAGICULE`, `AURA`, `EP`, `SOULS`, each with a worship multiplier; skills are worth worship by type. The client sends one validated payload
  (`MitamaOfferingActionPayload`: kind, target UUID, stat, amount, skill id) and the server runs `performMenuAction`. Ranges (`recruitRange`, `transferRange`), cooldowns
  (`recruitSuccessCooldownTicks`, `playerInviteCooldownTicks`) and caps are server config values.
- **Read:** worship feeds the god's own power (health, damage, magicules, aura per worship; `getDamageMultiplier`), and Beams / Edict spend it.
- **Inferred:** the save is the skill instance's tag (static helpers read `ManasSkillInstance`), the menu contents are built on the server and sent when it opens.

## 3. The other reference: Konosuba Faith (`tensura-isekai-evolved-addon`, `FaithService`, `FaithRegenerationMath`, `FaithSubordinateCensus`)

- **Read:** a Faith *meter* per player (`TrevolvedFaith`, max ~100, HUD "Faith %s/%s"), `consume`, `tick`, synced to the client. It **regenerates from the number of
  subordinates** (a census saved world-wide, an "effective count" that is weighted and capped by config).
- **Inferred:** it is a regenerating mana-like pool spent by skills, not a currency one player gives to another.

**Which is closer to ours?** Ours is a Faith *currency* that is spent on a bestowal, kept on the skill, carried across evolution. That is Mitama's shape (followers,
offerings, bestowal), so Mitama is the model. From Konosuba I took one idea only, regeneration from followers, but made lazy (see 4.5) so nothing ticks.

## 4. What changed

Everything new sits behind the SERVER setting `faith.worshipEnabled` (default true). Turn it off and Faith is exactly the old skill: only Pray and the random Bestow
work, no prayer bonus, no trickle, no discount; the two new modes answer "Worship is switched off on this server."

1. **Worshippers** (`FaithWorship`, list saved on the god's skill tag `Worshippers`, by player UUID, carried over when Faith evolves because the evolution merges the whole tag).
   New mode **Recruit Worshipper**: invites the player you look at (`recruitRange` 12). They accept with a click on `/faith accept <god>` (or decline with `/faith reject`).
   Limits: `maxWorshippers` 16, one invitation per god, `inviteSeconds` 60, `recruitCooldown` 100 ticks. A player belongs to one god (joining a new one leaves the
   other online lists). `/faith leave` and `/faith status`.
2. **Offerings** (reference DONATE). `/faith offering` lists `[Offer 1/5/10 Faith]`; `/faith offer <n>` costs `n x offeringMagiculeCost` (1000) magicules from the worshipper
   and gives the god `n` Faith. The god must be online, in the same dimension and within `offeringRange` (16); one offering per `offeringCooldown` (100 ticks). Only 1, 5 and
   10 are accepted by the server. This is the missing source of Faith other than the god's own Pray. Magicules only (see 7).
3. **Chosen gift** (reference GIFT). New mode **Choose a Gift**: look at a player, get a clickable list of the configured skills that player lacks (at most 12), click one to
   run `/faith gift <player> <skill>`. The server re-checks everything at the click: the sender holds Faith, the target is a live player in the same level within range
   (+4 blocks of slack), the wait, the price and that the skill is on the offered list. A worshipper pays `worshipperGiftDiscount` (25 %) less. The old random Bestow is kept.
4. **Prayer** gains +1 Faith per 4 worshippers (at most +3).
5. **Slow regeneration** (Konosuba idea): `worshipTrickleTicks` (6000 = 5 min) gives 1 Faith per period, +1 for every 4 worshippers after the first, none without
   worshippers. It is settled lazily when the god presses the skill, a command or `/faith status` (a stamp on the tag), paid for at most 20 periods at once, so there is no tick work.
6. Pray now pays with its own mode number. `faith add` sums in `long`.
7. Mode numbers: Faith `0 Bestow, 1 Pray, 2 Check` (kept), `3 Recruit, 4 Gift` (appended). Goddess `0..4` kept (`3 Blessing, 4 Miracle`), `5 Recruit, 6 Gift` appended.

### NEW / REPLACED / KEPT
- **NEW:** `FaithWorship`, `FaithWorshipRules`, `FaithSelfTests`, the `/faith ...` commands, modes Recruit and Choose a Gift, 9 server settings, `tools/lang/faith_fix.json` (35 keys), tests.
- **REPLACED:** nothing removed. Two small corrections in place: Pray's mode argument, the `faith add` arithmetic.
- **KEPT:** ids `faith`, `goddess_of_faith`, the `Faith` tag key, the evolution, Blessing, Miracle, the old random Bestow and every old config key.

## 5. Settings (SERVER config, section `faith`)
`worshipEnabled` true, `maxWorshippers` 16, `inviteSeconds` 60, `recruitCooldown` 100, `recruitRange` 12, `offeringRange` 16, `offeringMagiculeCost` 1000,
`offeringCooldown` 100, `worshipTrickleTicks` 6000 (0 = off), `worshipperGiftDiscount` 25. The five old keys are unchanged.

## 6. Worst-case cost (rule 11)
- **Tick work: none.** No tick handler, no scan per tick.
- **State:** two maps keyed by player UUID, `INVITES` (<= 256, one per god, expiring) and `OFFER_LAST` (<= 512, a rate limit that is simply emptied if it fills); both are
  removed on logout and cleared on server stop. The worshipper list is at most 64 entries (config clamp), 16 by default.
- **Per command:** `findGod` walks the online players once (N players x <= 64 UUID compares), only when a worshipper uses `/faith offering|offer|leave|status`; each is rate
  limited (offering) or free of side effects. Accepting walks the online players once. A gift menu is <= 12 chat lines.
- **Network:** no new packet. A command is one small client-to-server chat packet; replies are 1 to 14 chat lines to the one player (plus one line to the god). The existing
  Pray / Bestow effects are unchanged; the new effects are one sigil and one spray, sent through the existing `SkillAnimeFx` / `SkillFx` paths (nearby players only).
- **Client draw:** none added.

## 7. Not included, and what could not be verified
- **No GUI screen.** The reference opens a container menu with a screen and a payload. Ours is a clickable chat menu over server commands, which gives the same server
  authority with no client class and nothing to desync. A real screen can be added on top later (it would only call the same commands).
- **Not ported:** aura / EP / soul offerings (only magicules, to avoid guessing Tensura's aura/EP write API blind), Homing Beams, Heaven's Edict, Faith Skill Creation, the
  worshipper stat bonuses and skill donation from follower to god. Say so if any of them is wanted.
- **Not compiled and not seen in a game.** The session has no Gradle. Likely friction points for the first CI run: the `ClickEvent` constructor form (same as `GreedCommand`), `CompoundTag.hasUUID/putUUID`,
  and `ResourceLocationArgument`.
- Offline gods: a worshipper's name stays on an offline god's list; `/faith offering` needs the god online. Joining a new god cleans online lists only.
- The 1000-magicule Pray price was left as is; whether it feels fair on a real server is for the owner to judge (the offering price is its own setting).

## 8. Tests
- `FaithWorshipRulesTest` (pure JUnit): recruit checks, cap clamp, offerings, rate clock, prayer bonus, gift price, trickle bound, invitation lapse, and a wiring scan
  (self-test registered, smoke list, command, mode numbers, rules free of Minecraft classes, state bounded).
- Self-tests `faith.rules` and `faith.worship` (prefix `faith`, added to `SMOKE_EXPECT` between `attune` and `scaling`): live config inside clamps, both skills' mode counts and ids
  (old numbers kept), the `/faith` and `/anime_multiverse faith` command nodes, worshipper tag round trip, the Faith key untouched and the evolution merge keeping both.
