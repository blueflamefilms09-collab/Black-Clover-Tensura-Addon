> **REMOVED in 1.93.0 (owner's request).** The merge generator (the "custom" magic a player received when they held the grimoires of two attributes that have no hybrid of their own) is gone:
> `core/magic/merge/**`, `client/merge/**`, `MergedMagicLayer`, the `/multiverse magic merge|list|info|remove|grant|revoke` commands, the sync and the combined armour and effects. Two attributes now
> simply stay two attributes, each with its own grimoire skill, pages, effects and armour; nothing combines them. For saves: the 64 skill ids `nusmp:merged_00..63` stay registered as inert
> `core/magic/profile/RetiredMagic.RetiredBook` placeholders and a player who still holds one has it forgotten at login; `SavedData nusmp_merged_magic` is simply never read again; the enum constants
> `Aura.MERGED` and `VfxShape.MERGED_*` stay so no ordinal moves; `MagicKeys`/`MagicAlignments` still tolerate a stale `merged:` id. The startup audit that used to ride on `MergeEvents` is now
> `core/magic/profile/MagicAuditEvents`. The text below is the historical design.

# The merge generator (1.27.0)

Takes two to four existing magics and makes a **new magic** of them: its own name, soul type, hit effects, spell list, effects,
temporary armour and 3D emblem, all derived from the parents. Enum constants can not be created at runtime, so a merged magic is **data**:
a definition saved in the world and synced to every client. Nothing of the parents is ever altered or removed.

Code: `core/magic/merge/**` (pure rules + the game glue), `vfx/client/layer/MergedMagicLayer.java`, `client/merge/**`.
It is built on the magic framework ([magic_framework.md](magic_framework.md)): a merged magic has a `MagicProfile` like any other.

## Commands (`/multiverse magic ...`)

| Command | Who | What |
|---|---|---|
| `merge <a> <b> [<c> [<d>]] [name]` | op | creates the merged magic of 2-4 magics. Words after `b` that name a magic are more parents, the first that does not starts the name; put a name in quotes if it is also a magic's name (`merge flame water "Soul"`). |
| `list` | all | every merged magic of the world |
| `info <merged>` | all | parents, soul, hit effects, palette, spells, whether you hold it |
| `remove <merged>` | op | deletes the definition (only on request; online holders lose the skill; the parents are untouched) |
| `grant <player> <merged>` / `revoke ...` | op | gives / takes the merged magic |
| `audit` | op | the magic framework's audit |

`<merged>` is the name, the id (`merged:flame+water`) or `#n` (the number `list` shows).
Example: `/multiverse magic merge flame water Steam Lord`.

## Rules

* Parents: 2 to 4, **distinct**, real `MagicType`s, **not restricted** (Anti-Magic, Kotodama, Curse, the Demon magics are refused), not another merged magic.
* **Deterministic and idempotent**: the parents are put in alphabetical order, so `flame water` and `water flame` are the same set: id
  `merged:flame+water`, name `Flame-Water Magic`, the same dominant parent (the first in that order: Flame), the same slot logic. Merging a set that exists changes
  nothing (a different name is ignored).
* A custom name is 3-32 letters, digits, spaces, `-` `'` `.`; it must be unique among merged magics and not the name of one of the game's magics.
* **At most 64 merged magics per world.** Each is cast by one of 64 pre-registered skills (`nusmp:merged_00` .. `merged_63`); a new one takes the lowest slot never used, and
  only when all 64 were used once is a freed slot reused (the skill remembers the id it was granted for, so a reused slot never gives an old holder the new magic).

## What a merged magic is

| Part | How it is derived |
|---|---|
| soul type | the dominant parent's |
| hit effect | the dominant parent's; the first different one of the other parents rides along on every hit (`MergeContext.onHit`) |
| particle | the parents' particles in turn |
| guard effect | the dominant parent's |
| palette | the parents' colours; weights 55/45, 40/33/27, 34/26/22/18; the blend is the weighted mean lifted 18 % in saturation (`ColorBlend`) |
| spells | `SpellUnion`: 4 pages per parent for two parents, 3 for three, 2 for four, spread evenly so the starter and the signature page are always in, interleaved by rank |
| cost, cooldown, mastery need | the largest of the parents' pages of the same rank x **1.15** (capped at 60 % of max magicule) |

Casting reuses the parents' own cast code: each merged page calls the parent's page with the **parent's own book** (so every spell works exactly as for its owner), inside a
`MergeContext`, inside `SafeCast`. A spell that fails plays the generic effect and puts the magic on a recovery cooldown instead of crashing the tick.
The merged book needs no grimoire of its own: any summoned grimoire of the caster opens its pages. Pages open with mastery like every grimoire's (`MergeAccess.unlockPages`).

**Who has it:** `grant <player> <merged>`, or automatically (once, never again after a `revoke`) to a player who holds the grimoire skill of **every** parent.

## What you see

* **Effects.** While a merged page casts, every effect it sends passes `VfxSpawn.send` -> `MergeContext.adapt`: the parent's effect is recoloured into the shared palette
  (its own colour pulled 35 % toward the blend), and if it is one of that parent's cast / field / burst effects, **every other parent's own effect of the same kind**
  is sent at the same place (drawn by that parent's own layer; a Time parent's effect is just sent, `TimeMagicLayer` is never touched) together with the composite
  layer `MergedMagicLayer`: one strand of motes per parent round a blended core (cast), one counter-turning rune ring per parent (field), a ring that changes colour
  from parent to parent plus sparks alternating the palette (burst). Sizes follow the weights. At most 10 companion effects per tick.
* **Temporary armour** (5 s after a cast): `Aura.MERGED` (`client/merge/MergedArmorPainter`). The two heaviest parents that have an armour painter draw it, one over the other
  (3 % larger so their skins never share a surface); every other parent is a tinted glow shell of the player's model; one more shell in the blended colour breathes.
* **3D model** (the floating emblem): `MergedModelPlan` (pure) lays out up to 9 pieces from the merged id: real geo models of parents that have them (Bronze crest and
  pauldron, Copper crown, Corundum crown and gem shard, Key mini key, Beast head, shrunk to trinkets), procedural crystals for every parent, and a fusion gem in the blended
  colour. `MergedMesh` bakes it **once per merged id**: the pieces are drawn into a recording vertex consumer, centred, normalised to one block and flipped into the armour
  layer's space; each frame only streams those float arrays into the buffer (no per-frame allocation in this code). A missing geo model becomes a crystal.

## Persistence and sync

`MergedMagicData` (SavedData `nusmp_merged_magic`) holds the `MergeStore` as lines (`id|slot|custom|name|PARENT,PARENT`, plus the used-slot mask); decoding never throws and
enforces the limits. After every change the server publishes a copy to `MergedRegistry` and sends `nusmp:merged_magic_sync` (`MergedSync`) to everyone; it is also sent on login. The
client publishes it to the same registry, so profiles, the skill's name and pages, the layers and the armour all read one source.

## Offline preview

`python tools/magic/merge_preview.py flame water [--name "Steam Lord"] [--png out.png]` prints the id, name, soul, hit effects, palette, weights, tints, composite layout and the emblem's
pieces for a merge, and a picture. It ports the pure code and reads the colours from the Java sources; `--selftest` checks the port against numbers the Java tests also assert.
`tools/magic/gen_merged_icon.py` draws `textures/skill/grimoire/merged.png` (the merged skills' icon and every magic's fallback icon).

## Tests

`MergeCoreTest` (naming, ids, determinism, idempotence, limits, spell union, colour blending, store codec, model plan, Java/Python parity), `MergeArgsTest`,
`MergedRegistryTest` (registry, the profile built from the real enum, the mesh maths), and the profile package's tests.

## Limits and known gaps

* A merged book counts as FLAME for the few places that read a book's fixed `magic` field (the grimoire UI icon, `GrimoirePages.magicOf`); its casting hooks use `magicNow()`.
* Spells whose code refers to their own book's subclass work (they get their parent's book); spells that send effects with `VfxSpawn` directly are recoloured too (the hook is in `VfxSpawn`),
  but only a parent's own cast / field / burst shapes get companions.
* A parent's armour painter is drawn with style 0; a parent whose painter keys on a style of its own shows its default look.
* Merged magics have no universal pages (Summon / Store / Draw Weapon, Mana Strike / Zone, the wizard's own spells, Devil Union): they stay on the parents' books.
