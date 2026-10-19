# Ash Magic rebuild (1.93.0)

Pipeline of `docs/rebuild/magic_rebuild_brief.md`. Key `ash`, `MagicType.ASH`, colour `B0AAA2`, hit effect wither, guard effect resistance. Ash is not one of the generic wiki books: its book is `book/AshBook` (0.34), rebuilt in place.

## NEW / REPLACED / KEPT

| Part | Status | What |
|---|---|---|
| Armour `Aura.ASH`, `client/aura/AshAura` | NEW (Ash had no armour at all; the shared `WikiAura` was never started for it) | ten geo pieces, shown after Ash Cloud and Revelation (5 s) and Ash Shroud (9 s) |
| Summons `PropKind.ASH_1`, `AshProps`, `AshPropPainter` | NEW | ash soldier, ash trap (armed and sprung) |
| VFX `ASH_FX1..3`, `AshLayer`, `textures/particle/ash_*` | NEW (the legacy `ASH_FORMATION` shape and its layer in `ArcaneSpellLayer2` stay registered and are no longer sent by the Ash pages) | |
| Pages `ash_bullets`, `ash_cloud`, `ash_absorbing_formation`, `revelation_of_the_cowardly` | KEPT ids, order, tiers, the 900 tick cooldown and the old numbers (3.5 x 6, 3 percent mana drain, 8 damage x 3 traps); REPLACED implementation (`book/AshSpells`; the old methods in `WikiSpells` stay, unused) with the new effects, armour and trap props | |
| Pages `ash_shroud`, `ash_soldiers`, `cursed_ash` | NEW, appended | |
| `AshBook.onTakenDamage` | KEPT behaviour (the ash traps on attackers), now through `AshSpells.trap` which also raises the trap picture | |

## Canon
The wiki is unreachable from the build environment. What the repo has: `AshBook` (0.34, written from the wiki then) names Zora Ideale as the user and the spells Ash Bullets, Ash Cloud, Ash Absorbing Formation and Revelation of the Cowardly; `.github/wiki/spell_ash_absorbing_formation.txt` ("Ash Curse Magic: Ash Absorbing Formation ... streams of cursed ash from their hands. These streams drain the mana of those who touch them, and the user absorbs this mana") and `spell_magic_warding_ash.txt` ("a giant ash tree ... branches ... wrap around and block spells"; a World Tree spell, which is why the armour's back piece is an ash tree). The soldiers, the shroud and Cursed Ash are the nearest honest additions in that language, not named anime spells.

## Armour (`tools/gen_ash_rebuild.py`, delays in ticks)
chest (body, 0, front): cracked charcoal plate over a ribcage of bare branches, ember core, ash collar (glow); helm (head, 2, above): hood of ash, ember slits, antlers of dead branches (glow); pauld_r / _l (arm, 3 / 4): charred plate, heap of ash, splintered spikes; vamb_r / _l (arm, 5 / 6): cracked vambraces, ash cuffs, ember knuckles; belt (body, 6, below): charred lumps and rags of ash; back (body, 8, behind): the ash tree, five branches a side with twigs and embers (glow, clip idle sway); greave_r / _l (leg, 9 / 10): charred boots, ash drifts. Render types: pieces CUTOUT assembled / TRANSLUCENT while forging or shattering, glow maps ADDITIVE on chest, helm and back; falling ash TRANSLUCENT; ground ring TRANSLUCENT; ember sparks ADDITIVE. Slim arms narrowed to 88 percent. About 8900 vertices assembled.

## Summon models
`ash_soldier` (41 cubes; idle, strike, crumble), `ash_trap` (51 cubes; idle: flattened branches, spring: they rise). `ASH_1`: variant 0 soldier (follows the owner, strikes the nearest hostile within 12: 6 damage x EP power, wither 2 s; hittable, 30 health), 1 armed trap (30 s; springs on any foe within 2 blocks: 8 damage x EP power, wither II 4 s, blind 3 s, root 1.5 s via `controlTicks`, glow), 2 sprung trap (picture only, 30 ticks, spawned under an attacker by the Revelation).

## Spells
| # | id | Name | Tier | Effect |
|---|---|---|---|---|
| 0 | `ash_bullets` | Ash Bullets | starter | six bullets, 3.5 damage each, blind 1.5 s |
| 1 | `ash_cloud` | Ash Cloud | mid | invisible 4 s, blind within 5 blocks for 4 s, mobs lose the caster, cloud 3 s, armed trap under it 30 s, 5 s armour |
| 2 | `ash_absorbing_formation` | Ash Absorbing Formation | zone | r 4 x size, 8 s: enemy shots removed, 3 percent of foe mana per second into you, wither, 1.5 damage |
| 3 | `revelation_of_the_cowardly` | Revelation of the Cowardly | signature, 900 | 30 s: three ash traps on whoever strikes you; hidden things glow; 5 s armour |
| 4 | `ash_shroud` | Ash Shroud | mid | 9 s: guard effect, empower +4, thorns, melee attackers withered, armour set |
| 5 | `ash_soldiers` | Ash Soldiers | zone | two soldiers, 15 s (up to +50 percent) |
| 6 | `cursed_ash` | Cursed Ash | mid | 6 damage, wither II 5 s, drains 6 percent (2 percent on a boss) of the target's mana |

## VFX
FX1: ember gather, dark ash head, eleven cloud puffs, burnt flakes, two splintered tendrils, embers, bloom. FX2: the ash-tree sigil on the ground with an ember sigil counter-turning, a rim ring, swaying tendrils, clouds rolling in, flakes spiralling in, embers. FX3: ember flash, ring, ash puffs, tendrils springing up, flakes, sparks. Textures: ash_cloud, ash_flake, ash_ember, ash_sigil, ash_tendril, ash_ring. Previews: `vfx_preview ash_cast ash_zone ash_burst`; `render_preview ash_soldier_view ash_aura_armour ash_prop_soldier ash_prop_trap ash_prop_sprung`.

## Not verified
Nothing was run in a game; previews are software re-implementations over stubs. The wiki could not be read. Whether the ember tips of the back branches read too bright in the real renderer is unseen. Numbers are untested in play.
