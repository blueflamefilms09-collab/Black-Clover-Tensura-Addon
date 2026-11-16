# Shinra Kusakabe rework (Hell Walker, Devil's Footprints, Adolla Burst, Hell Conqueror)

Request: "Hell Walker, which is based off of Fire Force, make it into like the main character, like a skill."
Hell Walker (`hell_walker`) is now the Shinra Kusakabe skill. Server setting `shinraRework` (default true) restores the old behaviour.

## What Shinra does in Fire Force, and where each fact came from

Research status: **the wiki pages could not be opened in this session.** `fire-force.fandom.com`, `fireforce.fandom.com`, `ja.wikipedia.org` and `sportskeeda.com` all failed to resolve (outside the session's allowed network domains). Rule 10 (English wiki first, Japanese wiki as fallback) could not be followed. Only a web-search result list (search snippets of the English Fire Force Wiki and of articles) and general knowledge of the series were available.

| Fact | Source |
|---|---|
| Shinra is a Third and Fourth Generation pyrokinetic, called "Devil's Footprints"; he ignites his feet at will for speed and manoeuvring | English Fire Force Wiki, search snippet only (fire-force.fandom.com/wiki/Shinra_Kusakabe); not opened |
| He gained the Adolla Burst by creating a Link with his brother Shou; the Adolla Link shares memories and emotions and can sense the emotions of people without an Adolla Burst | English Fire Force Wiki and Heroes Wiki, search snippets only |
| Technique names on his page: Breath of Life, Corna, Hellfire, Hysterical Strength, Rapid, Rapid-Man Kick | English Fire Force Wiki navigation box, search snippet; **effects not read** |
| Ignition-and-kick fighting, rocket dashes, the devil's grin that comes with excitement or danger | general knowledge of the anime, **unverified** |

Not fetched at all: the Japanese wiki entries (炎炎ノ消防隊 / 日下部森羅 on ja.wikipedia and the ja fandom), the English page text, and the effects of Corna, Hellfire, Hysterical Strength, Rapid and Breath of Life. Those moves are NOT implemented; the Rapid-Man Kick is implemented only as a plausible fast kick under its listed name, and its exact anime effect is unverified.

## What it is now

`hell_walker`, shown as "Shinra Kusakabe" (Unique). One ON/OFF switch for the passives and a wheel of actives (the same pattern as `BeastSkill`).

| Part | What | Origin |
|---|---|---|
| Passive, always on | Your own flames cannot burn you (all fire damage is cancelled) | KEPT from Devil's Footprints |
| Wheel 1 | Ignition Dash (rocket along your sight line, no fall damage from it) | KEPT, folded from Devil's Footprints |
| Wheel 2 | Devil's Kick (burning kick, ignite and knockback) | KEPT, folded from Devil's Footprints |
| Wheel 3 | **Rapid-Man Kick**: a short ignition dash (7 / 8 / 10 blocks) that kicks and ignites every foe on its line, 9 / 13 / 18 damage, 200 / 500 magicules, cooldown 4 s (Ultimate 2.5 s) | NEW |
| Toggle (Hell Mode, 60 magicules/s) | The lower your health the harder you hit (x1 to x2.1 Unique, x2.6 mastered, x3.5 Ultimate), Ultimate heals from blows under half health | KEPT (old Hell Walker, unchanged formula) |
| Toggle, under 35 % health | **Devil's Grin**: Speed I | NEW |
| Toggle, under 25 % health, mastered or Ultimate | **Adolla Link**: Speed II, Strength I (Ultimate II), every move recharges 40 % faster | NEW |

Ultimate `adolla_burst` ("Adolla Burst"): now the Ultimate of Hell Walker (evolution Hell Walker -> Adolla Burst while the setting is on). Wheel: Light Speed (the dash, a blink up to 32 blocks), Devil's Kick (8 blocks), Adolla Burst (the 9-block blast), Rapid-Man Kick. The first three keep their old positions 0, 1, 2 (saved cooldowns stay right). It also has Hell Mode and the grin/link, and the old Hell Conqueror hellfire aura (foes within 4.5 blocks burn each second under half health) and berserk under a third of health are now part of its Adolla Link.

## NEW / REPLACED / KEPT

- NEW: Rapid-Man Kick, Devil's Grin, Adolla Link, `ShinraRules`, `ShinraMigration`, `ShinraSelfTests`, setting `shinraRework`, `tools/lang/shinra_rework.json`, `ShinraRulesTest`.
- REPLACED (owner asked for the rework): Hell Walker's name/description (now Shinra Kusakabe) and role (toggle only -> wheel + toggle); its evolution target (Hell Conqueror -> Adolla Burst, only with the setting on); Adolla Burst's base class (Devil's Footprints -> Hell Walker) and its description.
- KEPT: all four ids, icons and registry entries; Devil's Footprints' dash/kick code (now static helpers used by Hell Walker); the old Hell Walker damage curve; Adolla Burst's three old moves, numbers and positions; Hell Conqueror's class and behaviour; the `m.put(DEVILS_FOOTPRINTS, ADOLLA_BURST)` and `m.put(HELL_WALKER, HELL_CONQUEROR)` lines of the evolution table (the Hell Walker one is overridden by an appended line while the setting is on).

## Decisions

- **Devil's Footprints** is folded in. Not an inert stub: it stays registered and fully working (so nothing breaks if it is given by hand), and `ShinraMigration` (called from the 5-second evolution check) moves a holder to Hell Walker with the same mastery and tag, or just removes it if the player already has Hell Walker or Adolla Burst. `LegacyMigration` was not used (it only forgets inert `LegacySkill` stubs, it cannot grant a replacement).
- **Hell Conqueror** stays registered and keeps working for the players who have it (class KING, hellfire aura, berserk), but nothing evolves into it any more. It also inherits Hell Walker's wheel and fire immunity as an Ultimate-tier Shinra. New players reach Adolla Burst instead. It can be turned into a forwarding stub later if the owner wants it gone.
- Setting `shinraRework=false`: Hell Walker is only the toggle (no wheel, no fire immunity, no grin/link), no folding, Hell Walker evolves into Hell Conqueror, Adolla Burst has its old three-move wheel and no toggle.

## Server and cost

- All rules run on the server; client code is not touched. No per-player static state: cooldowns live in the skill instance, effects expire by themselves (3 s), so nothing needs clearing at logout, death or dimension change.
- Damage goes through `SkillDamage.hit` -> `BalanceLaw.damage` (EP scaling, PvP hit cap, class cap for Adolla Burst). The Hell Mode multiplier also applies to these hits, as it applied to every blow before.
- Worst case, per press: one entity query along a line (Rapid-Man Kick), at most `min(SkillTargets.MAX_FOES, 12)` hits, one velocity packet for the caster, one effect shape and one spray (the existing perf-budgeted `SkillFx` / `SkillAnimeFx2.devilKick`). Cooldown of at least 2.5 s, so at most 0.4 casts per second per player.
- Per second while Hell Mode is on: one pay, up to two potion effects (refreshed, no new entities), one small particle spray; Adolla Burst adds one radius-4.5 entity query and a bounded number of hits.
- Migration: one skill lookup per player every 5 seconds, nothing else.

## Not verified

Nothing was run or seen in a game (no Gradle here, CI is the compiler). Unverified: that ManasCore sends the skill key to `onPressed` while the same skill is toggleable (copied from the working `BeastSkill` pattern); the exact anime effect of the Rapid-Man Kick; wiki facts (see above). Lang: `tools/lang/shinra_rework.json` overrides the existing `hell_walker`, `hell_walker.description` and `adolla_burst.description` keys, which the merge script never replaces, so the coordinator must edit those three lines of `en_us.json` by hand. `SkillAssetsTest.theEvolutionTableIsUnchanged` was changed from 7 to 8 `m.put` lines.
