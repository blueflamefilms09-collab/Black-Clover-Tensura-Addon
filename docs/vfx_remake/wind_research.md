# Wind VFX remake, phase 1: research and baseline

Scope: the four basic elements are being remade one by one (this file: WIND). Phase 1 = research + baseline only. **No gameplay or code changes.**
Rule 3 respected (Time Magic VFX untouched). Rule 10: English wiki first, Japanese as fallback, source named on every line.

## 0. How "internet research" was actually done (honest limits)

| Fact | Detail |
|---|---|
| Sandbox | no outbound internet: fandom pages, YouTube, AniList, MAL, Crunchyroll could NOT be opened or fetched |
| WebSearch | returns short text summaries of pages, never images or video. Every canon line below is a WebSearch summary of the page named in brackets |
| Owner's art | looked at with my own eyes: `Wind_Magic_Spirit_Dive...zip` (AMV mp4, `Spirit_of_Zephyr.webp` anime frame, `Spirit_Dive.webp`, spirit art), clip `13c7e9c6-*.mp4` (Yuno's spiral ribbons, other mod), clip `6243570a-*.mp4` (water valkyrie, not wind: only its wing/ring ideas noted) |
| Reference board | `docs/previews/vfx_remake/wind_reference_board.png` (crops, captions) |
| Baseline | `docs/previews/vfx_remake/wind_baseline.png` (real layer classes, repo preview tool) |

Source tags used below: **[EN-wiki]** blackclover.fandom.com page seen only as a WebSearch summary; **[EN-ep]** fandom episode page / ANN review via WebSearch; **[JA]** Japanese site via WebSearch (ciatr.jp, atwiki aniwotawiki, manga-tantei.com, baidu baike ja, anibu.jp, bclover.jp); **[owner]** the owner's own pack / clips; **[sakuga]** sakugabooru tag listing.

### Failed / not fetchable
- Every fandom page body (Tornado Fang, Spirit Storm, Slicing Wind Emperor, Crescent Kamaitachi, Wind Magic, Sylph): only 1 to 3 sentence summaries came back, **no colour or shape description of the anime effect** on any of them.
- Episode pages for Ep 5, 84, 154, 160, 161 (only the one-line summaries).
- ja fandom wiki (ブラッククローバー Wiki), AniList, MyAnimeList, Crunchyroll: not returned by search at all (only bclover.jp official story pages and fan sites).
- Studio Pierrot VFX breakdowns / animator interviews: **none exist in results**. Only sakugabooru tag pages came up (Kosuke Kato tagged `wind` + `smears` on post 65619; Vincent Chansard tagged `wind` + `smears` + `genga` on posts 147589 and 147594). Those are fan tags on clips, not interviews, and do not name Yuno's spells. Not used as facts, only as a pointer for the owner: look at those posts for how Pierrot draws wind (smears and Kanada-style effect shapes).
- Videos: could not be watched; the only motion references are the owner's two mp4 clips and the AMV.

## 1. What the owner's art shows (own eyes, high confidence)

1. **Anime frame, Spirit of Zephyr** [owner, `Spirit_of_Zephyr.webp`]: wind is drawn as **flat, hard-edged shapes**: a huge pale yellow-white crescent wing (about #F4FBD8 core), and **white S-curve ribbon blades** (#F8FBF0) with pointed tails crossing in an X; the sword and body are a flat pale green (#CCEEBE to #9CE59A) with a soft outer glow. No noise, no volumetric smoke. Four-point sparkles (white stars) near the blade.
2. **Spirit Dive art** [owner, `Spirit_Dive.webp`]: pale-green body glow, spiky crown of connected crosses, a big leaf-shaped hard-edged wing.
3. **Manga colour AMV** [owner, `Download.mp4`, 65 s]: saturated lime to emerald (#89F842, #54D234, #46A92C, dark #37641E) with white-yellow highlights (#EEFAA1, #F6FAE6), black angular shards and speed lines, and a white-hot burst with thin radiating rays and a green ring. Hard edges everywhere.
4. **Other-mod clip, Yuno's spiral ribbons** [owner, `13c7e9c6-*.mp4`, 22 s, 30 fps]: at about 3 to 9 s the Spirit Dive start is a **cage of many thin (about 1 to 3 px) white-to-teal ribbons on tilted elliptical loops** that swirl around the body, crossing as a lattice, then a white flash with a few long rays, then a teal rim on the body, crown and wind sword. Thin lines read well on video; core wash is bright white-teal.
5. **Water valkyrie clip** [owner, `6243570a-*.mp4`] shows the same author's style: translucent wing feathers as many parallel thin blades, a halo ring, swirling thin ribbons at the feet. Not wind; style cue only.

## 2. Per effect: canon, anime look, what the mod does now, gap

Quad cost = vertices / 4; per activation the hard cap is `VfxVertexBuffer.MAX_VERTICES_PER_ACTIVATION = 400` vertices = 100 quads. Peak figures are from the preview tool (`python tools/vfx_preview/preview.py <scene>`), at full detail.

### 2.1 WIND_TORNADO (`WindSpellLayer.tornado`), peak 384 v = 96 quads
Used by: Spirit Storm (`WindBook.storm`, follows the caster, r = 5), Towering Tornado (`WindBook.toweringTornado`, aimed 22 blocks), character spells.
- **Canon, Towering Tornado** (風魔法「暴嵐の塔」 / 暴風の塔): "the user generates a tornado near the target; its destructive power can send the target flying" [EN-wiki Towering_Tornado]. Anime: Ep 5, Yuno vs Salim, Yuno's wind "takes over and swallows up the lightning" [EN-ep Episode_5; CBR article]. JA name 暴嵐の塔 [JA ciatr.jp, manga-tantei.com].
- **Canon, Tornado Fang** (風魔法「暴嵐の牙」): "a large **horizontal** tornado which **drills through** opponents and their spells; shatters rock" [EN-wiki Tornado_Fang]; JA: 暴嵐の塔の水平バージョン [JA manga-tantei.com]; first appears manga ch. 75, three-hit version 暴嵐の牙・三撃 ch. 138 [JA baidu baike ja]. **The mod has no horizontal drill version.**
- **Canon, Spirit Storm** (風精霊魔法): per [EN-wiki Spirit_Storm] the user absorbs the spirit, **gathers a large amount of mana in one palm and releases a dense BEAM of swirling wind** (not a standing tornado). Anime: Ep 84 vs Rill [EN-ep via YouTube title], Ep 154 (blocked by Ratri's defence spell) and Ep 161 (Gaderois blocks and destroys it) [EN-ep]. **Gap: the mod draws Spirit Storm as a standing tornado; canon is a palm beam.** (Tornado stays valid for Towering Tornado.)
- **Anime look:** not in any summary. Closest evidence is the owner's frames: flat white / pale-green shapes, white S ribbons.
- **Mod now:** 4 stacked hoops of the soft `wind_band` texture (funnel, WIND blend with 0.75 alpha softening), 6 spiral streaks x 5 segments, 6 leaves, 1 dust hoop. Reads as a soft grey-green cone, **lattice of bright streaks**, little white; no ribbons; leaves are ok. Camera in baseline: funnel reads, but looks like a cone of netting.
- **Gap:** too grey / soft vs flat cel white; no hard silhouette; no drilling direction; no thin spiral ribbons (owner clip).

### 2.2 WIND_GALE (`WindSpellLayer.gale`) and FX_WIND_GUST, peak 264 v = 66 quads
Used by: Gust Lane (`WindBook.lane`, 10 blocks, via Sylph origin), `BasicArts`, `CharacterArts`, `pages15/Batch1` (FX_WIND_GUST cone and dash pages).
- **Canon:** "Swallow's Gale" and "Gust Lane": **no source found** in 2 searches (only a Golden Dawn knight David Swallow) [EN search]. Treat as mod-invented names. Nearest canon: Sylph's Breath (condensed mana disperses into a vertical tornado that encases the opponent) [EN-wiki Sylph's_Breath, via search]; Slicing Wind Emperor: Gale (斬風皇・疾風), "plunges at the opponent with great force while surrounded by wind" [EN-wiki]; Whirlwind White Hawk, a white falcon made of wind (Ep 37 per a fan wiki) and wind blade shower [EN search, fan wiki]; Gale White Bow [EN-wiki title only].
- **Mod now:** up to 6 swallow sprites (`wind_swallow`, looks like small aeroplanes in the baseline), 14 speed streaks, 3 rolling rings, 6 leaves. **Weak spot: the swallows read as aircraft, not wind.**
- **FX_WIND_GUST:** declared in `VfxShape` and sent by 4 call sites, but **no layer in `vfx/client/layer` lists it in `shapes()`** (grep). Needs a check in game: probably draws nothing now. The remake should claim it (Gust Lane alias).
- **Gap:** swallows to be replaced by ribbons / a white hawk silhouette; a lane of thin streaks; FX_WIND_GUST unhandled.

### 2.3 WIND_EMPEROR (`WindSpellLayer.emperor`), peak 320 v = 80 quads
Used by: Slicing Wind Emperor page (28 blocks).
- **Canon:** 風創成魔法『斬風皇(カゼキリノスメラギ)』 is a **wind Creation Magic family: a wind sword** (Gale = plunging strike; Wintry Wind, Mountain Retreat; Whirlwind = swing binds limbs in whirlwinds) [EN-wiki + JA reading in the same summary]. A review calls out "Yuno's glowing wind crescents" as distinctive [EN search, theanimeworld review snippet]. **The mod draws a giant multi-blade crescent, which is the Kamaitachi family look, not the sword.**
- **Anime look:** the owner's Zephyr frame: the wind sword is flat green-white; wind arcs are white S-ribbons.
- **Mod now:** 6 layered `wind_band` arcs, 1 `wind_slash` ADD arc, 8 streaks, 1 glow. In the baseline camera the effect is **tiny and far**, hard to read; the arcs are soft and dim.
- **Gap:** crescent should be one big hard-edged white blade with a cel smear, plus 3 trailing S-ribbons; clearer silhouette at range; an optional sword-swing variant (Whirlwind).

### 2.4 WIND_ZEPHYR (`WindSpellLayer.zephyr`), peak 368 v = 92 quads
Used by: Spirit of Zephyr (follows the caster 600 ticks), `ModeArmor.Mode.WIND_SPIRIT_DIVE`.
- **Canon:** wind **Creation** spell: a powerful **wind sword with wings and a halo**, introduced manga ch. 208, anime Ep 161 vs Zenon [EN-wiki Spirit_of_Zephyr / Saint_Spirit_of_Zephyr, EN-ep Episode_161, ComicBook.com]. Spirit Dive look: left arm turns green, ring of connected crosses on the wrist, one green wing (later two), half then full crown of connected crosses [EN-wiki Spirit_Dive]. Debut Ep 84 [EN-ep].
- **Mod now:** 2 counter-rotating mantle hoops, 1 ground ring, 4 `wind_band` wing arcs, 4 spiral streaks, 8 mana motes. Reads as a swirl of net around the body; the four arc wings are faint.
- **Gap:** no sword, no halo / crown of crosses (the armour mesh may carry it: see `ModeArmorWindV2*`, `SpiritDiveWind3*`, previews `docs/previews/spirit_dive_wind*`, **not touched here**), wings should be 2 big hard crescent feathers like the Spirit Dive art, plus a thin-ribbon cage at the cast moment (owner clip).

### 2.5 WIND_SLASH (`WindMagicLayer.slash`), peak 288 v = 72 quads
Used by: Crescent Kamaitachi page (`WindBook.crescent`), `BasicArts`, `WikiSpells`, `TimeBook` (not touched), `KeySummons`, `ElementBooks` melee pages (Iron Fist, Palm Strike, Beast Charge, Feral Claws, Kakara dash).
- **Canon:** Crescent Kamaitachi: "with an open grimoire the user extends the arms outward and forms a **thin crescent-shaped blade of wind** sent at the target" [EN-wiki Crescent_Kamaitachi]; English dub / VIZ name "Crescent Moon Sickle" [EN-wiki]; JA カマイタチの三日月 [JA ciatr.jp, aniwotawiki]. Black variant (Anti Magic, Demon-Dweller Sword) exists [EN-wiki].
- **Mod now:** one `wind_slash` arc (WIND blend) + 3 ghost afterimages + thin ADD edge arc + glow + 4 to 10 random streaks (non-deterministic per frame via `inst.random()`).
- **Gap:** in the baseline the crescent is a dim green dot with a haze, it does not read as a hard crescent. It is also reused for 6 melee pages that are not Wind (Iron Fist etc.), so the V2 slash must still look neutral enough for them (tint by `inst.color`).

### 2.6 WIND_RING (`WindMagicLayer.ring`), peak 400 v = 100 quads (at the cap)
Used by: Tempest Ring, Eye of the Storm, Sandstorm, Lion's Roar, `BasicArts` (4 sites), tornado fallbacks (`ElementBook` maps it to BONE/BLOOD FX2 for other magics).
- **Canon:** none; generic expanding gust. Closest canon: the shock ring of a wind burst [no source needed].
- **Mod now:** 3 staggered ripple rings (2 quads rings each: WIND + ADD edge, 24 segments) + 6 rising spiral beams + glow. **Uses the whole 100-quad cap**, so any remake must shed cost, not add.
- **Gap:** flat rings read as ripples; wants a ground dust ring + a few thin spiral ribbons; budget reduction.

### 2.7 Other wind-adjacent code (read, not in the remake scope unless asked)
- `AuraLayer` (wind ring at the feet, WIND_SLASH texture) and `WindSpellLayer` share `ElementFx` (hoop, streak, hash, side): a V2 should append helpers there, not change existing ones.
- `AirLayer` (AIR_FX1..3, 1.93 Air Magic rebuild): additive thin ripples, speed lines, swirls, cut crescents, **60 / 80 / 92 vertices** (15 / 20 / 23 quads), very cheap. The doc `docs/attributes/air_rebuild.md` says canon was not reachable and no user is asserted. Air is a separate attribute from Wind; **KEPT as is** (shown in the baseline for reference of a cheap additive style). Can be offered as the V2 "cheap" tier.
- Spirit Dive wind armour / spirits (`SpiritDiveWind3*`, `ModeArmorWindV2*`, previews `spirit_dive_wind*`): meshes, not VFX layers; out of scope here, but V2 colours should match them.
- Texture generator: `tools/gen_element_vfx_textures.py` (wind_streak 256x32, wind_band 256x64, wind_swallow 128, wind_leaf 32; `wind_slash` is older; `wind_swirl_0..7` are particle sprites).
- No wind scene file existed for WIND_SLASH / WIND_RING: added `tools/vfx_preview/scenes.d/wind_baseline.json` (preview tooling only, no game code).

## 3. Summary table

| Shape | Canon (best match) | Current quads (peak) | Biggest gap |
|---|---|---|---|
| WIND_TORNADO | Towering Tornado (vertical), Tornado Fang (horizontal drill), Spirit Storm is a palm BEAM | 96 | soft grey net look; no horizontal / beam versions |
| WIND_GALE | none ("Swallow's Gale" not found); nearest Sylph's Breath, Whirlwind White Hawk | 66 | swallows look like aeroplanes |
| FX_WIND_GUST | same as Gale | 0 (no layer) | not drawn at all, check |
| WIND_EMPEROR | wind sword family (Gale / Whirlwind) | 80 | tiny soft arcs, not the sword |
| WIND_ZEPHYR | wind sword with wings, halo, crown of crosses | 92 | no sword or halo; faint wings |
| WIND_SLASH | thin crescent blade | 72 | dim, not a hard crescent; shared with non-wind melee pages |
| WIND_RING | generic | 100 | at the cap, flat ripples |
