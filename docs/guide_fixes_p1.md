# Guide fixes, P1 #2, #3, #5

Source: the owner's master build guide (Part 0 §0.5 and Part 4 §5, audited on 3.83.8). Items #1 (cost through `EnergyHelper.isOutOfEnergy`)
and #4 (ultimate tier / mastery) are done by another change and are not in this one.

## #2 Rebuilt-skill damage through Tensura's skill source (REPLACED, old kept behind a setting)

- `skill/SkillDamage.hit(...)` takes the skill instance as a new last argument. With it, the hit's source is
  `((TensuraSkill) instance.getSkill()).createSource(instance, caster, key, 0)` (the same call `GrimoireBook.hurtAs` makes, and the
  one Tensura addons make from helper classes): `Kind.MAGIC` -> `TensuraDamageTypes.MAGIC_GENERIC`, `Kind.ATTACK` -> vanilla
  `DamageTypes.PLAYER_ATTACK` (still a physical player attack, so Saiyan, Fire Casting and Spirit Dark melee hooks see it as before).
  Tensura resistances, nullifications, skill attribution and kill credit now apply.
- The amount is unchanged: `DM_DAMAGE_MULT` x `BalanceLaw.damage` (EP and mastery scaling, PvP hit cap, mob multiplier).
- 37 call sites pass their instance: Adolla Burst, Arc's Ray, Argonaut, Hero's Argonaut, Book Maker (its `pin` helper now takes the
  instance), Devil's Footprints, Firebolt, Futsunomitama, Gate of Babylon, Hell Conqueror, Immortal Will, Ki, Nen and `NenHatsu`,
  Rea Laevateinn, The Visionary, Wynn Fimbulvetr.
- Nen's "blow" test (`NenSkill.onDamageEntity`), Hati's magic defence and Liebe's Anti-Magic ward (`GrimoireMagicSkill.onBeingDamaged`)
  also recognise the new magic source (`SkillDamage.isSkillMagic`), so a Nen bullet is still not a weapon blow and Hati / Liebe still
  stop skill magic.
- The attached mode is 0: `hit()` is not told the pressed mode (marked `shortcut:` in the code).

**Setting (SERVER config `NUConfig`, section `guide_fixes`):** `tensuraSkillDamageSource = true`.
`false` = the old vanilla `indirectMagic` / `playerAttack` sources everywhere (and the three checks above behave exactly as before).

**Left on the old vanilla source (no skill instance there):**
- `skill/DbMoves` (7 calls in `hit`, `rush`, `barrage`, `disc`, `psychic`): the Dragon Ball / Beast Will move table is static and is
  given only the player and a mastery number (`DbSkill`, `BeastSkill` via `BeastMoves`). Moving it needs the instance threaded through
  about twelve helpers.
- Not `SkillDamage` and out of this item: `GrimoireMagicSkill.strike` (the legacy `grimoire_*` skills, raw damage without the balance
  law), `TimePages` line 200, `GodTier` (`playerAttack` for the share kill), and ~160 other files that build vanilla sources.

**Owner decision:** `saiyan/SaiyanCombat.kindOf` and `manacore/ManaCoreRuntime` ("magic_resist") recognise only vanilla magic
types. Skill magic now reaches them as Tensura magic, the way grimoire spells always have (Saiyans read it as melee, magic_resist
does not cut it). Adding `TensuraDamageTypes.MAGIC_GENERIC` to those two checks would change grimoire hits too, so it is left to you.
Tensura's magic type may also be reduced by armour where vanilla indirect magic bypassed it.

## #3 Lang keys for the 43 grimoire_<magic> skills (NEW)

- `tools/lang/grimoire_skills.json` (86 keys) merged into `lang/en_us.json`: the name is `MagicType.displayName`, the description
  follows the other 62 grimoire skills ("Grimoire magic: X Magic. Hold your grimoire to cast. ...").
- The 43: steel, thread, transmutation, ash, cotton, recombination, painting, world_tree, dice, slash, compass, kotodama, game, air,
  hair, memory, mineral, modification, mucus, mud, nail, permeation, poison_plant, red_ochre, rock, sandstone, scale, shakudo, skin,
  smoke, snow, song, soul_corpse, soul, sound, spike, switching, tongue, tree, stone, vine, vortex, wing.
- Test `grimoire/GrimoireSkillLangTest`: every `MagicType` (plus `grimoire_magic`) has a name and description key, the fragment is merged.

## #5 SpellRuntime lifecycle (fix)

- `SpellRuntime.onServerStopping` (registered in `NUSMP` next to the tick listener): every pending temporary block is taken back
  (same rule as expiry: only if the spell's block is still there, through `DungeonGuard.internal`, each in `SafeCast`), then the
  BOLTS / ZONES / LATER / TEMP lists are cleared, so no player, level or instance outlives the server.
- A bolt whose owner is removed (death, logout), disconnected, or in another dimension fizzles without its end effect (`SpellRuntime.gone`).
- Zones and delayed tasks carry no owner; they are cleared on stop and still end on their own timers.
- Not done: persisting temp blocks (a crash skips the stop event and leaves them; marked `shortcut:`).

## Tests and self-tests

- Pure: `skill/GuideP1SourceScanTest` (source, switch, every instance call, SpellRuntime stop/clear/gone, registrations, smoke list),
  `grimoire/GrimoireSkillLangTest`.
- Self-tests `guide.p1.damage_source`, `guide.p1.grimoire_skills`, `guide.p1.spell_runtime` (registry and source reads with a stand-in
  player; nothing is damaged, placed or cleared). `guide.p1` is on the smoke list after `greed`; the smoke `--restart` runs the stop path.

## Cost (rule 11)

- Damage source: one `instanceof` and one config read per hit, the same one `DamageSource` allocation as before; no packets, no entities.
- Bolts: three field reads per bolt per tick. Server stop: one pass over the pending temp blocks (bounded by what spells placed), once.
- Lang: 86 strings. Nothing per frame on the client.
