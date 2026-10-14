# The magic framework (1.27.0): adding a magic with zero edge-case fixes

Until 1.26.0 every new `MagicType` needed hand fixes in a few places (a restricted list, a palette case that threw while the book
registered, a colour lookup, a page that was never counted ...). The framework below gives **every** magic a complete profile with safe
defaults, audits the rest, and keeps one broken magic from taking the server tick or the client frame down.

Package `com.newuniverse.nusmp.core.magic.profile`. Everything here is additive: the enum constants, their order and their old fields are
unchanged; the old methods now answer through the profile.

## 1. The profile

`MagicProfile` is an interface; only `id()` is abstract. Every other method has a default that is non-null and never throws:

| Part | Method | Default |
|---|---|---|
| names | `displayName()`, `word()` | prettified id |
| soul / hit | `soulType()`, `hit()`, `secondaryHit()` | `EMPTY`, `PIERCE` (no rider), none |
| rules | `restricted()` | by NAME in `MagicKeys.RESTRICTED` |
| colour | `color()` | the colour of the magic's own book, else the grimoire palette (generated for unknown names) |
| particle / guard | `particle()`, `guardEffect()` | `GLOW`, Resistance |
| effects | `vfxStyle()`, `castShape()`, `fieldShape()`, `burstShape()` | the generic wiki-style layers (`WIKI_MAGIC_*`), which read their colour from the payload |
| armour | `auraStyle()` | the `Aura` constant of the same name, else none |
| icon | `iconPath()`, `icon()` | `textures/skill/grimoire/<name>.png` (fallback `merged.png`) |
| numbers | `safeCostPercent()`, `safeCostFloor()`, `safeCooldownTicks()` | 8 %, 150, 80 ticks |
| failure | `fallbackCast(player)` | a burst of the magic's colour at the aim point |

`MagicProfiles.of(MagicType)` / `MagicProfiles.of(String id)` returns, for ANY magic or any id string (null, blank, a stale id, a merged id):

1. an explicit profile registered for the id (`register`, or `override(type, o -> o.color(..).shapes(..))` which replaces only what is set);
2. else the profile derived from the enum constant (`EnumMagicProfile`);
3. else the final fallback (`FallbackMagicProfile`: GLOW, generic wiki effect, Resistance, a colour generated from the id).

It never returns null and never throws. `MagicType` itself now answers through it: `restricted()`, `particle()`, `guardEffect()`, and the new
`profile()`, `displayName()`, `word()`, `soulType()`, `hitEffect()`, `lookup(name)` (null for an unknown name; `byName` still turns it into Flame).

Where the effects of a magic come from (`MagicShapes`, pure): its own `<NAME>_FX1/2/3` shapes if they exist, else a hand-picked legacy set
(Flame, Water, Wind, Earth, Time ... see `MagicShapes`), else the wiki layers for a wiki attribute, else the generic ones.

## 2. How to add a magic (the whole list)

1. **Append** the constant to `MagicType` (never insert: item models and saves use the ordinal). `name`, `word`, soul, hit effect, particle, guard effect.
   If it must never be rolled for a soul, add its NAME to `MagicKeys.RESTRICTED` (not to a switch).
2. **Book**: register `book_<name>` in `NUSkills` (add it to `allBooks()` or, for a wiki attribute, to `WikiBooks.NEW_ATTRIBUTES`). Without one the grimoire falls
   back to the legacy skill with no pages: the audit says `NO_BOOK`.
3. **Icon**: `textures/skill/grimoire/<name>.png`. Missing: audit `NO_ICON` (the profile's `icon()` is still defined).
4. **Lang**: `nusmp.skill.book_<name>`, `.description`, and `nusmp.page.book_<name>.<pageId>` per page. Missing: `NO_LANG_NAME`, `NO_LANG_DESC`, `NO_LANG_PAGES`.
5. **Effects**: nothing is required. To give it its own look add `<NAME>_FX1..3` to `VfxShape` (append) and a layer; the profile finds them by name.
   A colour for a wiki attribute needs no `WikiBooks.color` case any more (a generated palette is used instead of throwing).
6. **Armour layer**: add an `Aura` constant of the same name (append) and a painter; without it a merge overlays a tinted shell for this parent.
7. Special cases: `MagicProfiles.override(MagicType.X, o -> ...)` at startup. Never an `if (magic == X)` in shared code.
8. Run the audit (below). A new magic should come back with no ERROR and, ideally, no WARN.

## 3. The audit

`MagicAudit` (pure, `core/magic/profile/MagicAudit.java`) takes the profiles and a `Probe` (resources, lang keys, books, page ids) and reports, magic by magic:

| Code | Severity | Meaning |
|---|---|---|
| `NULL_PROFILE`, `NO_ID`, `DUPLICATE_ID`, `NO_NAME`, `NO_WORD`, `NO_SOUL`, `NO_HIT`, `NO_VFX_SHAPE`, `NO_VFX_STYLE`, `NO_COLOR`, `NO_ICON_PATH`, `BAD_SAFE_NUMBERS` | ERROR | the profile itself is incomplete |
| `RESTRICTED_UNKNOWN` | ERROR | a name in `MagicKeys.RESTRICTED` is not a magic (a typo) |
| `NO_FALLBACK_ICON`, `NO_LEGACY_SKILL`, `BOOK_THROWS`, `PAGES_THROW` | ERROR | something that would throw at runtime |
| `NO_BOOK`, `NO_PAGES`, `NO_ICON`, `NO_LANG_NAME`, `NO_LANG_DESC`, `NO_LANG_PAGES` | WARN | works through a fallback, should be filled in |
| `CARD_SEED_LIMIT` | WARN | `SpellArchetype.packSeed` keeps the magic ordinal in two digits; 105 magics exist, ordinals 100+ decode as another magic (the spell-card seed is not used in production today) |
| `GENERIC_VFX` | INFO | the magic draws the generic effect |

* **Test time**: `MagicEnumAuditTest` runs it over the real enum and this checkout's resources; ERRORs fail the build, lang gaps are printed (never fail it).
  It also pins `restricted()` to the eight values the old switch returned.
* **Start time**: `MergeEvents.onServerStarted` calls `MagicAuditRuntime.runAtStartup()`: errors one by one, warnings as a summary by code.
* **In game**: `/multiverse magic audit` (op) lists the findings.

## 4. The runtime guard

`SafeCast` (`SafeRunner` + `FaultLedger`, pure core):

* `SafeCast.run(magicId, gameTime, player, cast)` runs one magic's cast. If it throws, the exception is **logged once per magic id**, the magic goes on a
  recovery cooldown (100 ticks, doubling per further fault up to 1200), `profile.fallbackCast(player)` plays the generic effect, and the cast counts as spent
  (the player is not stuck). `SafeCast.blocked(id, now)` is checked first, before any mana is spent. A `StackOverflowError` or a missing class (`LinkageError`)
  is caught too; real VM errors (out of memory) are not.
* `SafeCast.tick(key, runnable)` guards the callbacks of the spell runtime (bolt hits, zone steps, delayed tasks); `SafeCast.clientFault` is used by the VFX manager.

## 5. The central call sites that changed (everything else is untouched)

| File | Change |
|---|---|
| `blackclover/MagicType.java` | `restricted()` by name; `particle()`/`guardEffect()` through the profile, null-safe; `rawParticle()`, `rawGuardEffect()`, `profile()`, `lookup()`, accessors |
| `book/GrimoireBook.java` | `castPage`: recovery-cooldown check before mana, the cast inside `SafeCast.run`; `magicNow()/magicId()/magicLabel()/colorNow()`, `pagesVersion()`, `universalPages()`, `givesGrimoireItem()`, `summonMagic()` hooks (defaults = old behaviour); `holdingBook` protected; `hurtAs` calls `MergeContext.onHit` |
| `book/SpellRuntime.java` | bolts / zones / delayed tasks wrapped by `MergeContext.wrap` and run inside `SafeCast.tick` |
| `vfx/VfxSpawn.java` | `send(level, payload)` passes the payload through `MergeContext.adapt` (a null check outside a merged cast) |
| `vfx/client/VfxManager.java`, `VfxInstance.java` | spawn / tick / render guarded per effect; `MergedMagicLayer` registered; `expire()` |
| `book/WikiBooks.java` | `color()` default: a generated palette instead of `IllegalArgumentException` |
| `skill/NUSkills.java` | `MERGED_BOOKS` (the 64 merged-magic skills; not in `BOOKS`) |
| `vfx/VfxShape.java`, `aura/Aura.java` | `MERGED_CAST/FIELD/BURST/AURA`, `MERGED` appended |
| `client/aura/PlayerAuraClient.java`, `AuraRegistry.java` | `painter(Aura)` public; the merged painter registered |

## 6. What the audit found in 1.26.0 (and what was done)

* `MagicType.restricted()` was a switch over constants (compiled to ordinals): fixed, by name, pinned by a test.
* `WikiBooks.color()` threw while a wiki book registered if the new attribute had no `case`: fixed (generated palette).
* A null / throwing particle or guard-effect supplier was passed straight to the game: now GLOW / Resistance.
* `MagicType.byName` turns any unknown string (a typo, a merged id) into Flame silently: `lookup` added; use it where Flame is wrong. Not changed at the old call sites.
* `GrimoireAcceptance.starterPool` compares `soulPool.size()` with `MagicType.values().length - 2` ("all but Anti-Magic and Kotodama"), stale since eight magics are
  restricted: harmless today (the result is the same), not changed.
* `SpellArchetype.packSeed`: two-digit ordinal, 105 magics (see `CARD_SEED_LIMIT`). Not changed (unused in production).
* 30 wiki books had no skill name and no description in `en_us.json`, and no page descriptions (the skill showed its raw key): the lang fragment of this change adds them.
* `WikiMagicLayer` finds the magic of an effect by matching its colour against the 30 wiki palettes (`WikiBooks.magicForColor` / the style table of the wiki rework): a 31st wiki attribute, or two close palettes, draw the first style. Not changed here (the wiki layer is its own change); the profile's `color()` / shapes are the way to give a new magic its look without this table.
* Any exception inside a spell's cast, a bolt callback, a zone step or an effect layer used to reach the server tick or the render loop. Now contained.
