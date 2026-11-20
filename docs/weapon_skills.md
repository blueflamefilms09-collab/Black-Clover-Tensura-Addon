# Weapon skills: the Vassal weapons, the Djinn and the Beast Wills on the skill wheel

Too many keybinds. The Vassal weapons (Z/X/C/B/N), the Djinn (J/K/H) and the Beast Wills ([ ] ; \ =) are now ALSO normal skills: they appear in the player's
skill list and on the skill wheel and are cast with the one skill key / the wheel, like any other skill. **The old keys and packets stay and work exactly as
before** (nothing was changed or removed); the new skills call the very same server entry points the packets call.

Setting: SERVER config `weapon_skills.weaponSkills` (default `true`). `false` = no new skill is granted and the skills already held refuse every mode with a
message; the old keys work either way.

## What was built (12 skills)

| Skill id | Name | Speaks for |
|---|---|---|
| `ws_vassal_carriage`, `ws_vassal_fan`, `ws_vassal_scythe`, `ws_vassal_katana`, `ws_vassal_book`, `ws_vassal_mirror`, `ws_vassal_instrument`, `ws_vassal_harpoon`, `ws_vassal_sewing_set`, `ws_vassal_chain` | `<weapon> Arts` (e.g. Vassal Fan Arts) | the ten `HeroClass.VASSAL` weapons, one skill each, generated from `HeroKind` |
| `ws_djinn` | Djinn Equip | the Djinn the player holds (bound or lent) |
| `ws_beast_will` | Beast Will | the Beast Will the player carries, ANY of the wills (not only the 27 that have a `BeastSkill` pair) |

Type: Extra. No magicule cost of its own, no cooldown of its own, no mastery: the cost, the cooldown and every refusal are the old system's.

### Wheel modes, and the one server action of each

| Skill | Mode (id) | Server action (same entry point as the key's packet) | Replaces key |
|---|---|---|---|
| Vassal weapon | Technique 1 (`hero_tech_1`) | `HeroKeys.onKey(p, 0, PRESS)` then `RELEASE` (a tap) | Z |
| | Technique 2 (`hero_tech_2`) | `HeroKeys.onKey(p, 1, PRESS/RELEASE)` | X |
| | Technique 3 (`hero_tech_3`) | `HeroKeys.onKey(p, 2, PRESS/RELEASE)` | C |
| | Summon / Put Away (`hero_toggle`) | `HeroKeys.onAction(p, ACT_TOGGLE)` | B |
| | Change Form (`hero_cycle`) | `HeroKeys.onAction(p, ACT_CYCLE)` | N |
| Djinn Equip | Lesser Summon / Half Equip / Full Equip / Extreme Magic (`djinn_lesser`, `djinn_half`, `djinn_full`, `djinn_extreme`) | NEW `DjinnEngine.onSkillTarget(p, 1..4)`: the key's guards (alive, severance, key spacing), then the same `go()` the keys end in | J (one step up) |
| | Step Down (`djinn_down`) | `DjinnEngine.onKey(p, ACT_DOWN)` | K |
| | Dismiss (`djinn_off`) | `DjinnEngine.onKey(p, ACT_OFF)` | Shift + J |
| | Draw Rukh (`djinn_rukh`) | `DjinnEngine.onKey(p, ACT_RUKH)` | H |
| Beast Will | Acquire Form (`beast_acquire`) | `BeastWillManager.onAction(p, OP_FORM, ACQUIRE)` | [ |
| | Integrate Form (`beast_integrate`) | `BeastWillManager.onAction(p, OP_FORM, INTEGRATE)` | ] |
| | Ability 1 / Ability 2 (`beast_ability_1`, `beast_ability_2`) | `BeastWillManager.onAction(p, OP_ABILITY, 1 / 2)` | ; and \ |
| | Swap Will / Swap Will Back (`beast_swap`, `beast_swap_back`) | `BeastWillManager.onAction(p, OP_SWAP, 1 / -1)` | = and Shift + = |

The Beast Will calls include the `AttuneGate.beast` equip limit (it lives inside `onAction`), and the Djinn calls include `AttuneGate.djinn` (it lives inside `go()`).

### Decisions

* **One Djinn skill, not 36.** One player holds one Djinn at a time and every Djinn has the same five steps; the Djinn's own moves are the Half Equip special and the Extreme Magic, which the server already
  picks from the bound Djinn. 36 registered skills (and 36 icons) would add nothing to the wheel and would need a grant swap whenever a Djinn changes hands. The Extreme Magic mode is "the held Djinn's move".
* **Ten weapon skills, from the table.** `WeaponSkillRules.heroKinds()` is the `HeroClass.VASSAL` filter of `HeroKind`, and the skills are registered from it in a loop (no hand-written class per weapon). Extending to the Seven Star or
  Holy weapons is the one predicate `coversHero` (plus an icon and lang run). They are NOT in this release (the Holy ones hold keys for the Curse Series).
* **One generic Beast Will skill** because the 27 `BeastSkill` pairs are a different thing (a mastered-will reward with their own passives); this one is the key set for every will, bound or not by mastery.
* **A technique is a tap.** The skill sends the press and at once the release, so a technique cast through the wheel never counts as a held key (the Curse Series' "hold sneak and key three" still needs the real key).
* Mode names are generic (a technique's own name depends on the form the weapon is in; the client does not know it).

## Ownership and granting

The skill works only while the player owns what it speaks for; otherwise it says so on the action bar (red) and does nothing: the weapon is not bonded (`HeroBonds.kindOf(p) == kind`), no Djinn
(`DjinnState.active() != null`: bound OR lent), no will (`BeastWillStore` active will). The server setting is checked first.

| Event | Where it is caught | Skill |
|---|---|---|
| a Vassal weapon bonds | `HeroBonds` listener `onBound` | granted |
| the weapon is released | `HeroBonds` listener `onReleased` | taken back |
| a Djinn is bound / lent | `DjinnState.bind` / `lend` (append: `weaponSkills()`) | granted |
| the bond ends (release, loss, a lent Djinn is taken back) | `DjinnState.clearBond` | taken back |
| a Beast Will is absorbed | `BeastWillAcquire.grant` (append) | granted |
| every will is cleared | `/beastwill clear` (append) | taken back |
| login, respawn | one pass `WeaponSkills.reconcile` (12 storage reads) | every skill set to what the player owns (old saves get theirs) |

Logout keeps the skill. Admin: `/multiverse weaponskill give|take <player> <id>`, `sync <player>` (set to what they own), `list` (permission 2; give works without owning, the skill still refuses to act).

## Cost (rule 11), worst case

* Server tick: **nothing** (no tick listener, no `canTick`, no scan). The skill work happens only on a press.
* A press: at most **one action per player per tick** (`WeaponSkills.turn`, a UUID map with one `long` per online player, dropped at logout and at server stop); then the old system's own spacing applies
  (hero key spacing, Djinn 4 ticks, Beast Will 4 ticks). One press = at most 2 calls into `HeroKeys` (press and release) or 1 call elsewhere.
* Grant/revoke: one skill-storage read per bond change and 12 per login or respawn. No chunk, no entity spawn, no world scan.
* Network: **no packet of its own.** A press travels as ManasCore's own skill-key packet (one per press); what the old systems send (HUD sync) is unchanged. A grant is ManasCore's normal skill sync, once, to that player.
* Client draw: none of its own (12 icons of 32 x 32 on the wheel and the list).
* Memory: 12 skill objects, one `Map<UUID, Long>` of at most the online players.

## Files

NEW: `skill/rules/WeaponSkillRules.java` (the pure table), `skill/WeaponSkill.java`, `skill/WeaponSkills.java` (registration, grant, command), `skill/WeaponSkillSelfTests.java`, `WeaponSkillRulesTest`,
`tools/gen_weapon_skill_icons.py` + 12 icons `textures/skill/ws_*.png`, `tools/gen_weapon_skills_lang.py` + `tools/lang/weapon_skills.json` (48 keys, merged into `en_us.json` by that script: JSON load, update, dump),
the setting `weaponSkills`, the command `/multiverse weaponskill`.
APPENDED to old files (no old behaviour changed): `DjinnEngine.onSkillTarget`, `DjinnState` (3 calls + 1 private method), `BeastWillAcquire.grant` (1 line), `BeastWillCommands` clear (1 line),
`NUSkills` (last field), `NUConfig`, `MultiverseCommands`, `NUSMP` (`WeaponSkills.init()`).
REPLACED: nothing. KEPT: every key, packet, handler and skill.

## Tests

* `WeaponSkillRulesTest` (pure): every mode is exactly one action, no mode id or action twice, every action belongs to one mode, the 12 ids, the verdict and one-per-tick rules, `WeaponSkill.dispatch` has a case for every action and no `default`,
  the old packets still end in the entry points the skill uses, the old keys are still sent, the new files name no client class and are not on `docs/server_safe_allow.txt` (so `ServerSafetyTest` needs no change), no tick listener, wiring, icons, lang.
* Self-test `weapon.skills`: 12 skills registered as `WeaponSkill` of type Extra, wheels match the table, mode ids unique; on a stand-in player a Djinn bind / let go and a Beast Will bind / clear grant and take the skill back, and one press of each
  runs without an exception. (The Vassal weapon grant is not bonded in the self-test, it needs the hero system's gates; it uses the same `WeaponSkills.set`.)
