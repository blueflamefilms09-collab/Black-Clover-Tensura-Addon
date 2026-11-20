# Berserk of Gluttony weapons: the rest of the Weapons of Mortal Sin, and the two-hand fix

Series: *Berserk of Gluttony* (Black Sword of Gluttony = the Mortal Sin "Greed"). Mod ids are in `com.newuniverse.nusmp.sinweapons` (new weapons) and `com.newuniverse.nusmp.greed` (the fix).
Not seen in a running game. The Java was not compiled here (no Gradle in the session); CI is the first compile.

## 1. Sources (rule 10)

| Used for | Source |
|---|---|
| The list of Weapons of Mortal Sin (one per sin: Pride, Greed, Wrath, Envy, Lust, Gluttony, Sloth and their named weapons), what Greed's forms are | English wiki, `berserkofgluttony.fandom.com` (Terminology page and the Black Sword of Greed / Greed pages) |
| The holy sword Seifort | English wiki, same site (the holy-sword text) |
| Fallback for names | Japanese wiki, `ja.wikipedia.org` (read through `index.php?action=raw`; `api.php` was rate-limited) |

Not fetched at all: the ja fandom wiki. What the wikis do NOT give: any game mechanic. The wiki names the weapons and says what they look like and who they belong to; the abilities below are THIS MOD's DESIGN, not canon. The sin of the ring weapon (the Chakrams) is not named by the sources I could read, so "Black Chakrams" is a placeholder name. Existing Greed bow and scythe technique names differ from the wiki in places; I did not change them (rule 2).

## 2. NEW (rule 5: nothing like these existed)

All five are real 3D models in hand (`weapon_meshes/<id>.txt`, skins + glow, old-style sprite as inventory icon), engraved (rule 6, `item/WeaponEngravings.java`), have a Better Combat file (rule 13), a class row in both `weapon_classes.json`, lang in `tools/lang/berserk_weapons.json`, and sit in the creative tab (weapons) and the "berserk" franchise tab. Obtain path: creative tab and an operator's `/give`, the same as the Black Sword Greed and the Valkyrie lance (no loot table, no recipe, no drop).

| Item id | Look | Melee (with the player's 1) | Right click (design) | Engravings | Better Combat |
|---|---|---|---|---|---|
| `black_axe_of_sloth` | violet-veined black battleaxe | 10, -3.2 speed | Dead Weight: foes within 6 blocks (max 8) get Slowness III and Mining Fatigue I for 4 s, cooldown 8 s. A plain hit adds Slowness I 2 s. | crushing 2, enervation 1, sturdy 2 | `heavy_axe`, two-handed |
| `black_gunblade_of_envy` | green-glow longsword with barrel | 8, -2.4 | Green-Eyed Shot: a hitscan line 28 blocks, ONE target, 14 damage, cooldown 3 s. Hit: Covet takes one beneficial effect from the victim and lends it to you up to 10 s (once per 2 s). | barrier_piercing 2, magic_interference 1, energy_steal 1 | `sword` |
| `black_spear_of_vanity` | magenta mirror-disc spear | 8, -2.6 | Vain Thrust: a line 8 blocks, max 4 foes, 12 damage, cooldown 4 s | barrier_piercing 3, severance 1, magic_weapon 1 | `spear` |
| `black_chakrams` | one spiked red ring, drawn once PER HAND | 6, -1.6 | Spinning Ring: foes within 3.5 blocks (max 6), 9 damage, cooldown 3.5 s. Twin: with an empty off hand each blow is followed by the 60% off-hand blow of the Greed twin forms. | severance 2, swift 2, sturdy 1 | `twin_blade`, two-handed |
| `seifort_holy_sword` | rose-gold longsword with a cross inlay | 9, -2.4 | Grand Cross: a stem 12 blocks long plus a crossbar 10 blocks wide at 8 blocks, max 8 foes, 16 damage (x1.5 against the undead), cooldown 5 s | holy_weapon 3, severance 2, barrier_piercing 1 | `sword` |

Techniques only ever hit hostile creatures and PvP-legal players (`relics.Fx.foesAround`, which also excludes allies, pets, summons and creative players); all damage goes through the Balance Law (`relics.Fx.damage`, player cap included). Server settings (`nusmp-sinweapons-server.toml`): `techniques` (false = plain melee weapons), `covet`, `damagePercent`.

## 3. FIXED: the Black Sword Greed gauntlets and dual blades (replacement of the look; the old one is kept)

The owner's report: the gauntlets were not gauntlets, and the two-handed swords did not become two swords.

| Problem | Fix |
|---|---|
| Gauntlet form was one mesh with a pair of gloves floating in front of the hand, one item in one hand | One glove per hand. `tools/greed_hands/split_meshes.py` cuts the old mesh by connected parts into a right glove (`black_sword_greed_gauntlets.txt`) and a mirrored left glove (`..._gauntlets_left.txt`). Worn on the hand like the Vassal Gauntlet (`HandWrap.glove`, first and third person). The off hand draws the left glove (`client/greed/TwinHandsLayer` third person, `GreedHandsClient.onRenderHand` first person) |
| Dual-blade form was one hilt with two blades | One sword mesh (`black_sword_greed_dual_blades.txt`), drawn in EACH hand the same way |
| Only one blow landed | The main hand strikes as before; the empty off hand adds a second blow (`GreedForms.offBlow`): 60% of the attack value (`PairRules.offHandDamage`, cap 40), one per 12 ticks, one target, one swing packet |
| Better Combat: the gauntlets were excluded as a "hand item"; the dual blades were a plain sword | Dual blades: `bettercombat:twin_blade`, two-handed (two swords in one weapon, the two slashes alternate hands). Gauntlets: `bettercombat:fist`, two-handed, a three-hit combo of Better Combat's own `one_handed_punch`, `dual_handed_slash_cross`, `dual_handed_stab` |
| Spear, scythe, staff, shield, bow, sword | Unchanged: each has its own file; the test checks every melee form has one |

Settings: server `dualStance` (false = the old single blow) in `nusmp-greed-server.toml`; client `classicHands` in `nusmp-greed-client.toml` (true = the old one-mesh looks). Both defaults are the new behaviour.

## 4. KEPT (rule 2)

Old meshes `weapon_meshes/classic/black_sword_greed_{gauntlets,dual_blades}.txt` and their skins `textures/entity/weapon/classic/` (what `classicHands` draws), the item ids, the `Form` enum, the model jsons and the old sprites as inventory icons, every other Greed form, all old data components. The wiki-name differences of old technique names were left as they are.

## 5. Worst-case cost (rule 11)

* Server, per right click of a new weapon: one box query (<= 24 entities, then cut to the kind's cap 1..8), <= 8 hits, <= 20 particle packets through the existing `Fx.spray` (nearby players only); one cooldown per player (the game's item cooldown, nothing stored), min 60 ticks apart. A plain hit: one effect (Sloth), one timestamp in the wielder's persistent data (Envy, one theft per 40 ticks). No per-tick work, no state kept at logout.
* Server, twin forms: at most one extra single-target hit and one swing packet per wielder per 12 ticks; one long in persistent data.
* Network: no new packet type.
* Client: one extra item draw per player holding a twin weapon (or the chakrams) with an empty off hand; the glove / sword meshes are 785 / 1057 quads (the old mesh was 1255); chakram 1124, gunblade 886, spear 600, holy sword 1162, axe 376; all through the existing `WeaponRenderer` mesh caps and cached in the same way. No per-frame allocation added except the vanilla-style `Axis` quaternions the vanilla hand layer also creates.

## 6. Tests and self-tests

* Pure / file tests: `greed/GreedHandsTest` (twin flags, mesh choice, off blow bounds and cooldown, BC files, `twin_blade` / `fist` flags, mirrored gloves, single sword smaller than the old two-blade mesh, old meshes kept), `client/relics/item/HandWrapGreedTest` (rotations are proper, finger / back / thumb directions, glove fits arm), `sinweapons/SinRulesTest` (caps, line and cross geometry, damage and Covet bounds, every weapon has its data files, engravings, lang, class row, twin BC file).
* Existing generators re-run: `tools/weapon_grip/grip_table.py --write` (golden), `tools/bettercombat/inventory.py` + `gen_attributes.py` (308 melee weapons covered, `--check` clean).
* CI self-tests (dedicated server smoke test): `greed.hands` and `berserk.weapons` (both new; add to the smoke list).

## 7. Honest caveats

* Nothing here was seen in a running game, and nothing was compiled. First-person and third-person placement of the gloves and of the second hand uses constants I derived by maths and unit-tested for direction only (`HandWrap.GLOVE_SCALE`, `FP_DROP`, `FP_BACK`, `FP_INWARD`, `FP_PITCH_DEG`); expect to tune them by eye.
* Blender was NOT available (rule 14). The new meshes were built with the Blender-less kit (`tools/relics/new197_*`, `tools/berserk_weapons/build.py`), and the glove / single-sword meshes are derived by script from the existing Blender-made meshes. None of the new models was modelled and baked in Blender. Previews: `docs/berserk_weapons_previews/` (rendered by `tools/item_preview`).
* The abilities, numbers and engravings of the five new weapons are design choices, not canon; the Chakrams' sin and name are placeholders.
* `display_presets.py --apply` was run to fit first-person scale of the new long weapons; its other proposed changes (and its manifest rewrite) were reverted so unrelated items were not touched. The new ids are therefore not in `display_presets_manifest.json`.
* The five items are not in `tools/relics/franchise_overrides.json` (the Black Sword Greed is not either); they are in the "berserk" creative tab through `FranchiseTabs`.
* Better Combat: only its own presets and animations are used, no custom animations.

## 8. The weapons are built with a skill (the Greed set-up for the other five)

Owner request: "all the Berserk weapons should have the same type of set up as the first one, meaning being built with a skill". The first one is the Black Sword Greed and its Ultimate skill Lord of Gluttonous Greed. The other five now have the same kind of skill. Sources (rule 10): none needed, the skill names are this mod's design (the wiki lists the weapons and sins, not skills); the chakram sin is unnamed in canon, so the "Twin Rings" name is a placeholder.

### NEW / REPLACED / KEPT (rule 5)

* NEW: five Ultimate skills of one class `skill/SinWeaponSkill` (Lord class, like Greed's), registered at the end of `skill/NUSkills`: `lord_of_slothful_weight` (Black Axe of Sloth), `lord_of_green_eyed_envy` (Black Gunblade of Envy), `lord_of_empty_vanity` (Black Spear of Vanity), `lord_of_the_twin_rings` (Black Chakrams, placeholder name), `seifort_holy_judgement` (Seifort Holy Sword). New files: `sinweapons/SinSkillRules` (pure rules), `sinweapons/SinSkillService` (server work), `sinweapons/SinSkillSelfTests` (self-test `berserk.skills`), five 32 x 32 icons `textures/skill/<id>.png` (made by `tools/gen_sin_skill_icons.py`), lang `tools/lang/berserk_skills.json` (merged into `en_us.json`), server setting `soulBound`, tests `SinSkillRulesTest` and `SinSkillSourceScanTest`.
* REPLACED: nothing. No old skill, item, model or behaviour was removed (rule 2).
* KEPT: the five items, their ids, models, engravings, Better Combat files, every constant of `SinRules`, the right click and its cooldown, the plain hits (Sloth's slow, Envy's Covet, the Chakrams' twin blow) and `berserk.weapons`. No new data component: the owner is a tag in the stack's custom data.

### The rules in plain words

* A weapon binds to the first player who carries it. Once a second per player the server looks at the inventory (at most 41 slots, staggered by entity id): an unbound stack of one of the five weapons gets that player's UUID written in its custom data (`sin_owner`) and the player is given that weapon's skill. The skill is permanent, so the weapon can be forged again later. A player who already carries a bound stack of the same weapon does not bind a second one (one bound weapon per player per kind). Creative tab and `/give` still produce an unbound stack; the first carrier binds it.
* A stack bound to another player keeps working as a plain melee weapon (hits, Better Combat swings, Sloth's slow, Covet); only its technique needs the owner. Server setting `soulBound` (in `nusmp-sinweapons-server.toml`, default true): false restores the old behaviour, anyone can use the technique. Another player's stack is never touched, taken or rebound.
* The right click calls one shared check (`SinSkillService.mayUse`); a refused click shows one message and costs a one-second item cooldown so a held button does not repeat it. Everything else of the right click is unchanged.
* The skill has two modes, cycled with the skill mode key like Greed's. **Technique**: the weapon of that kind in the main hand fires its technique exactly as the right click does (same gates, same item cooldown, same caps); if it is not in the main hand the player is told to hold it. **Manifest / Store**: if the player holds their own bound weapon in the main hand it is stored back into the skill (the stack is removed); else if they carry it anywhere in the inventory nothing happens; else a fresh bound stack is forged into the empty main hand, or into the inventory, with at least 100 ticks between forges (a store also starts that clock, so storing and forging again at once does not reset the weapon for free). With a full inventory and a busy hand nothing is forged and no cooldown is paid.
* The new skills are Ultimates of the Lord class (`docs/ultimate_classes.md`): classified, so they take the Lord mastery and tier. They cost no magicule (like Greed's Secret Technique they do not pay through `NUSkill.pay`); the technique keeps its own cooldown.

### Worst-case cost (rule 11)

* Server, once a second per online player: two passes over at most 41 inventory slots (an item-class test each, an NBT read only for the five weapons), no world scan, no entity query, no chunk touched. One int compare per player per tick, the rest of the tick costs nothing. Inside a `PerfProbe` section (`s.sinskill.scan`) and `SafeCast.tick`.
* Server, per use of a mode: Technique is exactly one right click (one box query of at most 24 entities, at most 8 hits, at most 30 particle packets through `Fx.spray`, nearby players only, cooldown 60 to 160 ticks). Manifest / Store: one inventory pass of at most 41 slots plus one stack created or removed, at most once per 100 ticks per weapon for a forge.
* State: one long per player and weapon in the player's persistent data (the forge clock); nothing static, nothing to clean up at logout, death or dimension change.
* Network: no packet of its own. The skill system's own sync of a newly learned skill, and one action-bar message to the one player.
* Client: nothing new except five 32 x 32 icons.

### Tests and registries that enumerate skills

* `sinweapons/SinSkillRulesTest` (pure): owner, bind, technique gate, manifest decision, cooldown (at least 100 ticks, clock set back), staggered bounded look, every skill has its lang text and its own 32 x 32 icon. `sinweapons/SinSkillSourceScanTest`: no client class, no static player or map, bounded and staggered look, the right click goes through the shared check and the old technique lines are kept, config is a server setting, ids registered as the rules name them, the modes do what the right click does.
* CI self-test `berserk.skills` (server only, stand-in players, no client class): five skills registered as Lord Ultimates with two modes, an unbound stack binds once and gives its skill, a second stack stays unbound, another player's stack is untouched and does not give the skill, the technique gate follows `soulBound`, Technique slows a zombie ahead and pays the item cooldown, Manifest stores, waits out its cooldown, forges one bound weapon and never a second while one is carried. **Add `berserk.skills` to `SMOKE_EXPECT` in `.github/workflows/smoke.yml`.**
* Appended to existing registries: `skill/UltimateClassSelfTests` (the five skills are expected to be Lord class) and `skill/UltimateClassSourceScanTest` (`SinWeaponSkill` is LORD and is named in `docs/ultimate_classes.md`, which now lists it); `skill/icons/SkillIconsV3Test` (the five icon ids join the list of icons outside the repaint manifest, like `saiyan_prodigy`). `ResetGuard` protects every skill of `NUSkills.SKILLS` by itself.

### Honest caveats

* Nothing here was compiled with the game's classes or seen in a running game; the pure and source-scan tests were run with plain JUnit, the self-test and the skill classes only by CI. The skill key, the mode key and the messages are untested by a person.
* The five skill names are the design's, not canon. The icons are simple original pictures drawn by script, not the animated icons of the older Ultimates (they are not in `skill_icons.json` and have no 8-frame strip, so they are static).
* One bound weapon per player per kind is enforced for what the player carries (the 41 inventory slots). A bound stack kept in a chest or an ender chest is not counted, so a player can forge a second one while the first is stored away; and a bound weapon that drops on death can be picked up and used by anyone as a plain melee weapon (its technique needs the owner). The owner can always forge a new one.
* A weapon stored by Manifest / Store loses its wear and any enchantments added by hand (it is forged fresh; the engravings are applied again by the existing once-a-second engraving pass). Nothing is stored in the skill.
* `tools/skills/lang_fragment.json` was not regenerated (it was already stale for other skills and regenerating it would change unrelated entries); the new skill texts are in `tools/lang/berserk_skills.json` and `en_us.json`.
