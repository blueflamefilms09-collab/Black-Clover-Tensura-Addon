# Compatibility: Multiverse of Anime and other mods

What works with what, in the code and data of this repo. Nothing here has been run in a game; it was written by reading files
and checked by Python and plain-Java tests (see "Checks").

## Three things ship

| Thing | Where | What it is |
|---|---|---|
| Data pack | `tools/compat/multiverse_x_tensura_no_index/` (source), `docs/compat/multiverse_x_tensura_no_index.zip` (built) | Loot and tags that join this mod and Tensura No Index. |
| Built-in copy | `src/main/resources/resourcepacks/nusmp_tni_compat/`, `compat/CompatPacks.java` | The same pack inside the jar, offered (and switched on) only when `tensuranoindex` is loaded. |
| Compatibility Codex | `compat/CompatBook.java` (text), `CompatCodexItem.java` + `CompatItems.java` (the grimoire-style item `nusmp:compat_codex`), `client/compat/CompatCodexScreen.java` (its screen), `CompatBookItem.java` (the old written book, kept as the fallback), `CompatCommand.java` | A grimoire-style book in the creative tab, `/multiverse compat book` (gives it), `/multiverse compat book written` (the old vanilla written book) and `/multiverse compat status` (live list in chat). |

Rebuild the pack and its copy: `python tools/compat/build_compat_pack.py` (add `--check` to only validate). The build is deterministic.
`CompatPackTest` fails if the in-jar copy is out of date.

## Install the data pack

- With both mods and this jar: nothing to do. The jar adds the pack `mod/nusmp_tni_compat` itself.
- Standalone: put `multiverse_x_tensura_no_index.zip` (do not unzip it) into `<world>/datapacks/`, `/reload`, check `/datapack list`.
  Pick one of the two, not both (the modifiers would be listed twice).

## What the pack covers

- Our two real chest tables (`nusmp:chests/library_ruins`, `nusmp:chests/grimoire_tower`) get a 25% extra roll of
  `tensuranoindex:phase_dust` (1-2), `name_slip` or `canalis_relic`; the tower also 3% of `relic_philosophers_stone`.
- Our eight bosses (`nusmp:dante_zogratis`, `vanica_zogratis`, `zenon_zogratis`, `lucius_zogratis`, `devil_avatar`, `goliath`,
  `charybdis_spawn`, `zagred`) get 15% Phase Dust (1-4) or Canalis Relic and 4% Philosopher's Stone when they roll a loot table.
  They mostly drop through code, and the Sentient Dungeon clears boss drops inside the Dungeon, so expect this outside it.
  Whether NeoForge runs a loot modifier for a boss that has no loot table is unverified.
- `tensura:no_max_ep_plunder` (path `data/tensura/tags/entity_type/`) gets our eight bosses and the seven bosses of Tensura No Index
  (`coronzon`, `fuse_kazakiri`, `sasha_kruschev`, `auriel`, `nokth`, `warden_of_the_even`, `innocentius`). Every id is `required: false`.
- Loot modifiers are registered the way NeoForge needs: `data/neoforge/loot_modifiers/global_loot_modifiers.json` lists them.
- Every loot file carries a `neoforge:mod_loaded` condition for `tensuranoindex`; the standalone zip refers only to ids that were checked
  against the Tensura No Index 0.71.0 jar (`tools/compat/known_ids.json`).

## What it does not cover, and why

- Toaru skills in a skill codex: this repo has no data-driven skill codex (no reload listener, no `skills/codex` reader) and no Riven boss
  (only `RIVEN_REMAKE_BOSS.md`). The other mod's `tni_multiverse/skills/codex/toaru.json` therefore has nothing to attach to. No Java was invented for it.
- Index grimoires as loot: one item whose book number is a component; a plain loot entry would be blank.
- Loot of their bosses: they have no loot tables. Our relics in their tables: their only chest table (`tensuranoindex:chests/croce_di_pietro`) belongs to a
  weapon, and nothing lists it.
- Curios: their wearables (`harmonizer_prism`, `diffusion_field_dampener`) are already in `curios:charm` and `curios:curio` from their own tags, and the
  Philosopher's Stone is carried, not worn, so it does not belong in our `dungeon_relic` slot. Nothing was added.
- Mimic drops and the relic chest modifier (`nusmp:relic_chests`) are code; the pack leaves them alone.

## Overlap with the bridge pack inside Tensura No Index 0.71.0

That mod ships `resourcepacks/multiverse_bridge` (pack `mod/tensuranoindex_multiverse_bridge`, required, switched on when `nusmp` is loaded).
Read against this repo, it does not do what its wiki page says:

1. No `data/neoforge/loot_modifiers/global_loot_modifiers.json` in it: its two modifiers (`tni_multiverse:nusmp_chests`, `nusmp_story_bosses`) are never run.
   (The mod's own list is also empty, which switches off its Croce di Pietro modifier.)
2. `nusmp:entities/riven_remake` and `nusmp:entities/marquis_remake` are not loot tables of this mod.
3. `tni_multiverse:chests/index_shelf` names `tensuranoindex:church_manuscript`, removed from that mod in 0.51; a table with an unknown item is rejected.
4. Its tag is in `data/tensura/tags/entity_types/`; 1.21 reads `entity_type`. Ignored.
5. Its 15 codex skills have no reader here.

Ours leaves that pack alone, uses other ids (`nusmp_tni_compat:*`), and writes the tag where Minecraft reads it. If that mod fixes its pack later,
chests get two independent low-chance rolls; delete `library_chests.json` from ours then. Tag overlap is harmless.

## In-jar registration (for the lead)

Mod bus: `modEventBus.addListener(com.newuniverse.nusmp.compat.CompatPacks::onPacks);` The pack is offered as `required` because that is
what switches a built-in data pack on by itself (the same form `MultiverseBridge` of the other mod uses); it is only offered when the other mod is loaded.

## The Codex

`/multiverse compat book` (everyone) gives a vanilla written book "Compatibility Codex" by "Multiverse of Anime": required mods (mirrors
`neoforge.mods.toml`, tested), mods needed through Tensura (Architectury, GeckoLib), optional integrations the code really uses (Curios, Better Combat,
FTB Teams, TR Nightmare, Ice and Fire), the Tensura No Index bridge and its limits, and a plain "never run in a game" page. Each mod reads Loaded or
Not installed from the mod list when the copy is made; `/multiverse compat status` prints the same list live. Optional integrations were found by grepping
`ModList`, reflection by class name, and `data/` for foreign ids. Lootr has no code of ours and is not listed.

### 1.34.0: the Codex in the grimoire design system

The Codex is now an item, `nusmp:compat_codex`, built from the grimoires' own parts instead of a vanilla written book:

- **Model.** The grimoire geometry (`grimoire/GrimoireBookPlan`, baked by `client/grimoire/GrimoireBakedModel`, loader `nusmp:grimoire`) with a fixed look
  `compat/CompatCodexLook` (pure): violet leather `0x35295E`, gold frame metal, a gold emblem of two interlocking rings, a cyan glow while held. It is a new
  cover entry `COMPAT` (motif `BookMotif.COMPAT`, appended): textures `emblem_compat`, `cover_base_compat`, `cover_art_compat` in
  `textures/item/grimoire_book`, the 16x16 icon `textures/item/compat_codex.png` (model `compat_codex_icon`). All generated by
  `python tools/compat/gen_compat_codex_assets.py`; look at it with `python tools/compat/preview_compat_codex.py` (renders the real plan classes headless).
- **Screen.** Right-click opens `CompatCodexScreen` (client only, no packet; the item calls `CompatCodexClient.open()` behind `level.isClientSide()`).
  It is the Codex's open book: classic = leather cover and parchment, anime = translucent glass pages, turning magic circle, Great-Sage status line
  (config `ui.theme`). Page turn, arrow buttons, `<` `>` corners, mouse wheel, Left/Right/A/D/Up/Down/Home/End, ESC closes. Each mod is a row with a live chip
  (Loaded green, Not installed grey) read from the mod list once when the screen opens; the footer says plainly that nothing has been run in a game.
- **One source of text.** `CompatCodexModel` (pure) turns `CompatBook.blocks(...)` and `CompatCatalog` into items and pages (`paginate` takes the heights the
  screen measured); nothing is written twice. `CompatCodexModelTest` checks the pages, the paging rules and the look.
- **Tooltip.** Same conventions as the grimoire's: aqua kind line, gold line, purple italic lore, grey details (keys `nusmp.compat.codex.tip.*`).
- **Fallback.** `CompatBookItem.stack()` still builds the written book; `/multiverse compat book written` gives it.

Not run in a game: the model loader path for a second item, the screen layout at other GUI scales and the item colour handler are unverified.

## Checks done

`python tools/compat/build_compat_pack.py --check`; `CompatBookTest`, `CompatCatalogTest`, `CompatPackTest` run with a JUnit shim (one mutation of each
of two rules was caught). The glue classes were type-checked against API stubs; the pack classes, `WrittenBookContent` and `AddPackFindersEvent.addRepositorySource`
are missing from the stubs, so those lines were written from the form Tensura No Index's compiled bridge uses and from the 1.21.1 record. CI is the first real compile.
