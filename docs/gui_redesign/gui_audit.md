# GUI audit (phase 1 of "The Great Spirit & Grimoire Fusion")

Read-only audit of the mod's GUI as of the current branch (jar 2.x; theme code since 1.27.0). Nothing in `src/` or `assets/` was changed.
BEFORE sheet: `docs/previews/gui_redesign/gui_before.png` (software mock-ups made by the repo's own harnesses `tools/ui_preview/preview_ui.py` and
`tools/system/preview_overlay.py` plus `tools/gui_redesign/gs_before.py`; vanilla bitmap font; **not the game**). Rebuild with
`python3 tools/gui_redesign/make_gui_redesign.py`.

## 0. The headline facts

1. **The mod already has a Great-Sage look.** `ui.theme = anime` (default since 1.27.0, `docs/gui_theme.md`) draws translucent blue-black glass panels, cyan neon
   edges, 6 px corner brackets, rotating rune circles (`MagicCircle`, textures `gui/theme/circle_*.png`), animated bars, a typed "Analysis complete. / Result: ..."
   voice (`SageBlock`, `UiLang`) and a turning-page animation (Codex, Four Kingdoms, Compat Codex). `ui.theme = classic` keeps the older flat/parchment drawing.
   So the redesign is a **third look layered on the anime one**, not a first redesign.
2. **There is no mana / magicule / aura gauge in the mod's HUD.** Tensura draws its own bars (its jar is not available in this session, so its layout and
   textures are not audited here). Grimoire "Mana" **is** the Tensura magicule pool (`ManaLaw` pays spells as a fraction of `EnergyHelper.getMaxMagicule`,
   page cards say "12% MP"); Anti-Magic and the slash/light/body/earth families pay **Tensura Aura** (`docs/antimagic_aura.md`). The only mod-side readouts of
   these pools are the Djinn bar (Magoi = `Magoi.fraction`, synced as a percent by `DjinnNet.Sync`), the Shishou panel (his own mana pool), the status screen's
   "Mana" block (stage, cost %, corruption 0..100) and the Anti-Magic "Aura %" line. **A ring gauge needs a new client feed** (see directions doc, decision 1).
3. **No clover shapes exist in the UI.** Skill icons are 32x32 PNG squares with a baked 4 px square frame (gold for grimoire/unique art, silver/red for others);
   Tensura draws them in its own windows via `Skill#getSkillIcon`. The only clover in the UI is the stand-in Four Kingdoms logo
   (`textures/gui/four_kingdoms_logo.png`, 256x256, `tools/gen_menu_textures.py`). Leaves exist as **data**: `GrimoireCover` has `tier` 3/4/5 (THREE_LEAF, FOUR_LEAF,
   FIVE_LEAF, plus BLACK_MAGIC and GOD_TIER at 5; `isForbidden()` = five and above except god tier) and the Tensura `Skill.SkillType` (EXTRA, UNIQUE, ULTIMATE, RESISTANCE, COMMON)
   is what the mod's skills declare (counts of files naming each type: EXTRA 8, UNIQUE 25, ULTIMATE 22, RESISTANCE 2, COMMON 1).
4. **Chat / log "alerts" have three separate voices** and the server sends most messages as plain chat/action-bar text (377 files call
   `sendSystemMessage` / `displayClientMessage`): the dungeon System toasts (`[SYSTEM]`, HUD), the page-unlock toast ("Notice: a new page has opened."),
   and the Great-Sage lines inside screens. There is no central notice API and nothing prefixes `[Notice... Great Sage has...]` today.
5. **The "Mana Zone gate menu" is not a screen.** The gate (`mana/zone/ManaZoneGate`, six requirements) is shown by `/multiverse mana zone` as a coloured chat
   checklist ("[x] / [ ] requirement: have (needs n)"); the Mana Zone itself is a grimoire page. Client side there is only the world rendering and the Grand-Zone tint
   (`client/manazone/ManaZoneClient`, a `RenderGuiLayerEvent.Pre` on `CAMERA_OVERLAYS`). A gate screen would be new work.

## 1. Screens (`extends Screen` / `AbstractContainerScreen`, 23 classes)

| Screen | File (`client/...` unless noted) | What it draws | Textures | Themed? |
|---|---|---|---|---|
| Codex (the book; tabs You, Status, Weapons, Guide ...) | `ui/CodexScreen`, `CodexPages`, `theme/CodexSkin` | two-page spread, tabs on the edge, page turn (`FLIP_TICKS`), figure + floor circle | none (fills + circle textures) | anime + classic |
| Four Kingdoms (the **grimoire menu**) | `multiverse/FourKingdomsScreen`, `FourKingdomsSlot` | header with logo, grimoire summary, open book with unlocked pages (name, cost, cooldown, text), 450 ms page flip with `BOOK_PAGE_TURN`; slot hooked into Tensura's `AbilityCategoriesScreen` by a mixin | `gui/four_kingdoms_logo.png`, `gui/grimoire_page.png` | anime + classic |
| Multiverse Status | `multiverse/MultiverseStatusScreen`, `MultiverseStatusClient` | 3-column analysis panels (rank, stars, squad, race, grimoire, mana, spirit, anti-magic, ceremony), UiBar under Grimoire / Spirit / Mana(corruption), typed Sage readout; compact panel injected in Tensura's menu | none | anime + classic |
| Grimoire Slot | `grimoire/GrimoireSlotScreen`, `GrimoireSlotClient` | one slot above the inventory, gold circle | none | anime + classic |
| Compat Codex | `compat/CompatCodexScreen` | Codex-style book of compat notes | none | anime + classic |
| Engravement Codex | `engraving/EngravingCodexScreen` | dark two-page book of every engraving | none | own palette |
| Tower Wizard questionnaire | `wizard/QuestionnaireScreen` | question, answers, progress bar, circle | none | anime + classic |
| Avatar builder / Soul Forge / action / inventory | `avatar/AvatarBuilderScreen`, `SoulForgeScreen`, `AvatarActionScreen`, `avatar/inventory/client/AvatarInventoryScreen` | creation pages, round category buttons, tooltips with magicule tax | none | glass look (own `UiTheme` use) |
| Black Market | `skilltree/shop/ShopScreen`, `ShopPainter`, `ShopLayout` | plum glass, witch portrait, rows, scroll | portrait art | own |
| Hub Quest / Hub Shop / Dungeon Map | `elite/hub/QuestScreen`, `ShopScreen`, `DungeonMapScreen` | vanilla-style lists, room rectangles | none | plain |
| Relic Forge | `relics/forge/ForgeScreen` | forge view, buttons | none | anime-ish |
| Requip Armoury | `requip/RequipArmouryScreen` | 54-cell grid, 3 tabs | none | anime + classic |
| Weapon Book (hero weapons) | `heroweapon/forms/WeaponBookScreen` | forms, absorb list | none | own |
| Shishou Stomach | `shishou/ShishouStomachScreen` | chest menu with star-field panel, level/mana gauges in title row | `AkashicTheme` fills | own |
| System Stats / System Edit | `mega/system/SystemStatsScreen`, `SystemEditScreen` | stat allocation, window placement | none | System style (`OverlayStyle`) |
| GUI Size / GUI Move | `ui/GuiSizeScreen`, `ui/move/GuiMoveScreen` | sliders, drag handles, dashed outlines | none | plain |
| Ability windows of **Tensura** (Skills, categories, sub-mode) | drawn by Tensura; `ui/theme/TensuraScreenTheme` overlays circle + brackets + Sage readout | overlay only, **off by default** (`ui.tensuraOverlay`) | circle textures | optional |
| Buttons | `ui/theme/ThemedButton` | vanilla `Button` drawn as a glass chip with hover glow | none | anime + classic |

Common machinery: `ui/GuiScale` + `ui/scale/*` (per-screen and per-HUD size, "fit to window"), `ui/move/*` (GUI Move: every HUD element can be dragged), registry ids in
`GuiScaleRegistry` / `GuiMoveRegistry` (HUD: `system_hud`, `hero_hud`, `curse_bar`, `djinn_panel`, `boss_bar`, `quest_tracker`, `relic_keys`, `song_meter`,
`scale_readout`, `floor_card`, `page_toast`, `shishou_hud`; panels: `four_kingdoms`, `codex`, ...).

## 2. HUD layers (`RegisterGuiLayersEvent.registerAboveAll`, 12) and hooks

| Layer id | File | What it draws | Reads |
|---|---|---|---|
| `nusmp:system_overlay` | `mega/system/SystemClient`, `SystemOverlay`, `OverlayLayout` (pure), `OverlayStyle` | holographic dungeon window (floor, threat, quest bar, five stats), toast column (`[SYSTEM] ...`, kinds info/success/failure/reward/shadow), typed title; only in the dungeon dimensions; fills + one gradient kind + default font, no textures | `ClientSystem` (server pushes) |
| `nusmp:system_floor_card` | `mega/system/FloorCardClient`, `FloorCardLayout` | floor report card on arrival | `FloorCard` packet |
| `nusmp:page_unlock` | `ui/theme/PageUnlockOverlay`, `PageUnlockTracker`, `ToastTimeline` | "Notice: a new page has opened." panel with a building rune circle, typed description, "Result: cost ..., cooldown ...", mastery bar; queued | synced Multiverse status (`MultiverseStatusClient.status()`), diff of the grimoire `PageList` |
| `nusmp:hero_hud` | `heroweapon/HeroHud` | hero weapon gem, level bar, form, technique cooldowns (bottom left) | hero sync |
| `nusmp:hero_curse_overlay` | `heroweapon/curse/CurseOverlay` | edge vignette, corruption bar, curse offer panel | curse sync |
| `nusmp:djinn_hud` | `relics/djinn/DjinnScreenFx`, `djinn/gui/DjinnGui` | vignette + fringe + FOV squeeze, the bar above the hotbar: 4 phase pips, **Magoi (magicule percent)**, assimilation, trust, timers. Style `NEW` or `CLASSIC` (`DjinnGuiConfig.style()`, a precedent for a style setting) | `DjinnNet.Sync` (magoi percent) |
| `nusmp:relic_hud` | `relics/RelicClient`, `RelicActKeys` | key-hint chip for worn relics | local |
| `nusmp:shishou_hud` | `shishou/ShishouHud` | sword panel: level, **mana gauge** (`AkashicTheme.gauge`), selected skill, appraisal readout | Shishou sync (own pool) |
| `nusmp:song_meter` | `song/SongClient` | voice level meter near the hotbar | local mic level |
| `nusmp:scale_weights` | `scalecast/ScaleHud` | plate over the hotbar (scale casting weights) | prop word |
| `nusmp:hub_quest_tracker` | `elite/hub/HubClient`, `QuestOverlay` | quest progress, top right | hub sync |
| (hook) boss bars | `gui/bossbar/ModBossBarOverlay`, `CustomBarOverlay`, `BossBarPainter`, textures `gui/bossbar/*.png` (bg, edge, fill, flame, frame, glow, pips, shine) | replaces vanilla boss layer on `RenderGuiLayerEvent.Pre`, vanilla layout, mod bosses get styled bars | boss events |
| (hook) Grand Magic Zone tint | `manazone/ManaZoneClient.onGuiLayer` | one translucent fill on `CAMERA_OVERLAYS` | region sync |
| (hook) Tensura menus | `FourKingdomsSlot`, `MultiverseStatusClient.onScreenRender`, `TensuraScreenTheme` | slot, compact panel, optional overlay on `ScreenEvent.Render.Post` | - |

## 3. How gauges read mana / magicules / aura today

* **Server truth.** Magicule and Aura are Tensura's (`EnergyHelper.getMagicule/getMaxMagicule`, aura through `EnergyBridge.aura`); spells pay a fraction (`GrimoireBook` cost x `ManaLaw.costMult`, "12% MP" on cards, `costPct` in `SpellModel`).
* **Client readouts that exist.** Djinn: `Magoi.fraction(p)` x 100 in `DjinnNet.Sync` to nearby players (bar `0xFF1A1A28` track, `0x3A7BFF` mixed with the Djinn colour). Status screen: `Mana.Corruption` (0..100), `Anti.Aura` (percent, `AntiMagicAura.share`), `Mana.Cost/Power` percents, all inside `MultiverseSync` NBT. Shishou: `s.mana()/s.maxMana()`.
* **Not available client-side from the mod today:** a live magicule fraction and aura fraction for the local player outside the Djinn case. Tensura syncs its own capability to its own HUD; reading it from the mod needs a check against the real jar (not available here) or a small self-only packet.
* **Pure helpers that already exist** and can be reused: `UiBar` (eased value, shimmer), `UiMath` (easing, pulse, angle, mix), `ClientPoints`, `GuiScale.hud(id)`, `GuiMove.begin/end`.

## 4. Textures used by the GUI

`gui/theme/circle_{outer,star,poly,inner,arcs}{,_128,_64}.png`, `circle_glow.png` (white on transparent, `blur` + `clamp` mcmeta; made by `tools/gen_ui_theme_textures.py`), `gui/four_kingdoms_logo.png`, `gui/grimoire_page.png` (128x160), `gui/bossbar/*` (8 files), `gui/black_market`, `gui/shishou`, `gui/soulforge`, `gui/boss_bar`. Skill art: `textures/skill/*.png` (49), `skill/grimoire/*.png` (107, one per magic), `skill/icons/*` (235: `<magic>_buff`, `_ultimate`, `bg_*`, `mod_*` badges, all 32x32; made by `tools/gen_skill_icons.py`). Everything else (panels, bars, brackets, the System window) is **fills and gradients** (`GuiGraphics.fill/fillGradient`), no texture. Shaders: none in any GUI path (`NUShaders` is for world VFX).

## 5. Sounds on clicks today

Vanilla events only: `UI_BUTTON_CLICK` (Hub screens, Weapon Book), `BOOK_PAGE_TURN` (Four Kingdoms page flip), `VILLAGER_NO` (Black Market refusal), `SimpleSoundInstance.forUI` in Soul Forge. The sword-draw sequence (`anim/SwordDraw`) uses `BOOK_PAGE_TURN`, `BOOK_PUT`, `ANVIL_PLACE`, `AMETHYST_BLOCK_CHIME`, `PLAYER_ATTACK_SWEEP`, `WITHER_SHOOT`. **No new audio asset is needed** for "heavy book snap" (`BOOK_PUT`, low pitch) and "sword unsheathed" (`PLAYER_ATTACK_SWEEP` high pitch + `AMETHYST_BLOCK_CHIME`); a custom sound would be new work.

## 6. Mapping of the prompt's four features to this mod (new vs replacement, rule 5)

| Prompt feature | What the mod has | Verdict | Where it lands |
|---|---|---|---|
| 1a Neon borders + ornate filigree corners | cyan edge + 6 px plain brackets (`UiPanel.panel`, `OverlayStyle`) | **REPLACEMENT of the corner part** (brackets stay in anime/classic); neon line = KEPT | new corner texture at the 4 corners of `UiPanel`, behind the theme switch |
| 1b Concentric gauge (outer Mana particles, inner Magicule slime) | nothing in the mod HUD (Tensura draws its own; Djinn has a flat magoi bar) | **NEW** | new HUD layer `nusmp:great_spirit_gauge`; Djinn/Shishou bars stay |
| 2a Floating holographic Grimoire menu | Four Kingdoms open-book menu + Codex (2D, anime glass, runes) | **REPLACEMENT (new skin)** of the Four Kingdoms / Codex drawing; behaviour, hit boxes, page flip logic kept | third theme value, same screens |
| 2b Ink script to matrix compile | typed `SageBlock` text (typewriter), no ink font | **NEW** (extends `SageBlock`/`SageLayout`) | new pure `CompileTimeline` + a custom font provider for the ink |
| 3 Clover skill icons | square 32 px art, tier only in text/frame colour | **NEW** (frames are added around the old art; old PNGs stay registered) | clover frame textures + drawing in mod screens; optional pre-composited copies for Tensura's windows |
| 4a Alert widget with crest and `[Notice... Great Sage has...]` | `[SYSTEM]` toasts (dungeon only), page-unlock toast, chat lines | **NEW widget; the 3 toasts become feeders, chat stays** (server messages are not rewritten) | new layer fed by a small client `GreatSageNotice` queue |
| 4b Slime ripple on hover, click audio | `ThemedButton` hover glow, vanilla click sounds | **REPLACEMENT of the hover drawing** in the new theme; sounds = existing events | `ThemedButton` branch |

## 7. Not audited / could not be seen

* Tensura's own HUD bars and ability windows (jar not in the session; only the mod's overlay hooks were read).
* Nothing was run in a game; all pictures are software mock-ups. The harness text uses the vanilla `ascii.png` atlas (fetched into `build/`, not committed); the 2x ink font is a stand-in (FreeSerif Italic).
* English/Japanese wiki lookups (rule 10): none were needed, no canon detail was looked up. "Magic Knight squad insignia" and the Black Clover / Tensura names come from the owner's prompt; the crests drawn are original shapes (a winged shield with a clover, a gilded seal, a slime face), not copies of any squad emblem.
