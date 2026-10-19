# Anti-Magic runs on Aura (1.94.0)

Owner request: "make anti-magic use aura for its abilities, not the custom anti-magic bar".

Before this change the Anti-Magic grimoire (`book/AntiMagicBook.java`) cost nothing to cast, and its one resource, Anti-Magic Power (AMP, 0 to 1000,
kept on the grimoire skill instance), was what the Lord's Black Form needed (100 AMP to start, 25 AMP drained every second). Now every Anti-Magic
ability is paid in Tensura Aura, exactly as the Slash, Light, Body and Earth families do it (`book/EnergyBridge.aura(player, frac, floor)`: a fraction
of the max Aura with a flat floor, magicules when the Aura is short, free in creative).

## Cost table

Prices live in `antimagic/AntiMagicCosts.java` (pure Java, unit tested); `antimagic/AntiMagicAura.java` pays them.

| Ability | Fraction of max Aura | Floor | Note |
| --- | --- | --- | --- |
| Black Slash (`demon_slayer_slash`) | 2% | 8 | cheap utility |
| Bull Thrust (`bull_thrust`) | 3% | 12 | cheap utility |
| Black Divider (`black_divider`) | 4% | 15 | |
| Infinite Slash (`infinite_slash`) | 5% | 20 | strong |
| Black Meteorite page (`black_meteorite`) | 6% | 25 | strong |
| Black Hurricane (`black_hurricane`) | 6% | 25 | strong |
| Black Asta (`black_form`, before the Lord awakens) | 12% | 60 | ultimate, timed form, three a day |
| Infinite Slash Equinox (`infinite_slash_equinox`) | 15% | 80 | ultimate |
| Black Form (the Lord's toggle): start | 3% | 12 | recast to end it, ending is free |
| Black Form: every second held | 1.5% | 3 | ends with a message when the Aura cannot pay, with the old brief Weakness |
| Demon-Slayer Nullification Field (held sword), every second | 0.3% | 1 | only a second in which a foe or a hostile spell is in its reach; goes quiet while the Aura cannot pay, then wakes again |
| Demon-Slayer Black Meteorite (sneak + right-click) | 80% | 100 | unchanged, it already paid Aura |

Compare: Forearm Blades 2% (then 1% a second), Ripper Dash 2.5%, Light Speed 3%, Earth family 5%. A failed payment says "Not enough aura to ..."
(for example "Not enough aura to hold Black Form."). A page whose spell then does nothing ("No room to strike") gives its price back.
The page entries of the book still carry a magicule price of 0, so nothing is paid twice.

## What happened to AMP

* The stored value, the `"AMP"` tag on the grimoire, `AntiMagic.amp / setAmp / addAmp / MAX_AMP`, the multiverse sync key `"AMP"` and the legacy
  Lord skill are all kept, so saves and other code that reads them keep working.
* AMP still gathers (magic that hits you, demon-sword hits, drained foes, five a second while the form rests). It is only an accumulated stat now:
  nothing spends it, nothing requires it. It was never a damage or nullification scaler (the sword's affinity reads the Lord and Black Form
  states, its severance scales with EP), and the Lord awakens by mastery or the admin command, not by an AMP threshold, so there was nothing to
  re-source beyond the Black Form gate, which is the Aura price above.
* No AMP bar was ever drawn by the client. The one place that showed AMP, the Multiverse status screen, now shows the Aura share
  (`Anti.Aura`, in percent); the action bar of Black Form shows "Black Form - Aura 43%". Tensura's own Aura bar is the resource.
* Migration: `AntiMagicBook.migrate` (run once per grimoire from the once-a-second upkeep, and when an old Lord skill is folded in) stamps the data
  version `AuraVer = 1` and keeps the Lord state, Black Form, and AMP (clamped to 0..1000). A Lord who was in Black Form keeps the form and from
  then on pays it in Aura. The maths is `AntiMagicCosts.migrate` (unit tested: idempotent, a legacy Lord skill counts as awakened, a form
  without the Lord is dropped, a newer version is never lowered).

## Where it changed

* `antimagic/AntiMagicCosts.java` (new), `antimagic/AntiMagicAura.java` (new), `book/AntiMagicSelfTests.java` (new), test `AntiMagicCostsTest`.
* `book/AntiMagicBook.java`: page casts wrapped in `paid(...)`, `blackForm`, `upkeepForm` (was inline in `tickBook`), `migrate`.
* `antimagic/AntiMagicLordSkill.java` (the legacy skill): the same Aura toggle and drain.
* `item/DemonSlayerSwordItem.java`: the field pays Aura per second, tooltip and class text.
* `antimagic/AntiMagic.java`: documentation, and stamping the version when an old Lord skill is folded in.
* `multiverse/MultiverseSync.java`, `client/multiverse/MultiverseStatusScreen.java`: Aura share instead of the AMP line.
* `NUSMP.java`: one line registering the self-test `antimagic.aura`.
* Lang: `tools/lang/antimagic_aura.json` (overrides of the book, page and legacy-skill texts that said "costs no mana" or "Anti-Magic Power").

Not touched on purpose: `Nullification`, `NihilityZone` (zones and helpers, no player resource; the sword's Black Meteorite already paid 80% Aura),
the weapons' right-click techniques (`MagicWeaponItem`, cooldown only, never AMP), `MagebaneShieldArts`, `Wave0Hooks` and `Bonds` (they only
read `isUser` / `lord` or draw the anti-magic slash, never AMP), and the Asta NPC line (`arc.dialogue.asta.17`, "charges your Anti-Magic Power",
which is still true of the stat and is checked by `BlackBullsDialogueTest`). Older docs (`mode_armor_spec.md`, `demon_slayer_genesis_spec.md`,
`weapons_spec.md`, the README line on the Spirit Lord) still describe the AMP pool; this page supersedes them.

## Self-test

`antimagic.aura` (registered with `SelfTests.register`, run by the CI server smoke test): with a stand-in player that has Tensura existence data,
a page lowers the Aura and leaves the AMP alone; a page that does nothing gives its price back; with no Aura and no magicules a page does not
run and fails with "Not enough aura to ..."; a second of Black Form is drained from the Aura; Black Form switches off at zero Aura and the Lord
state survives; stored AMP and the Lord state survive the migration.
