# Water VFX remake: research and baseline (phase 1, nothing implemented)

Scope: the Water element of the "remake the 4 basic Elements VFX" request (Fire, Water, Wind, Earth; this file is Water only). Phase 1 = research + the current look + three art
directions (`water_proposal.md`). No gameplay or code was changed. Rules applied: CLAUDE.md 3 (Time Magic VFX untouched), 10 (English wiki first, Japanese fallback, sources named).

## 0. How the research was done and what failed (honest part)

* The sandbox has no way to open fandom pages, YouTube or image hosts. `WebSearch` returns a short TEXT summary per query (the summary names the pages it drew on). No image or video
  could be downloaded or seen. So every "how it looks" line below is a text description, NOT a frame study.
* Queries run (extended mode): EN wiki / spell pages for Sea Dragon's Roar, Cradle, Nest, Valkyrie Dress, Aqua Javelin; EN episode pages (Episode 46, 108); EN animation / Studio Pierrot
  animator interviews; JA queries for all spell names, for ep 108 and for the Vetto fight's 作画 (drawing) and effects.
* Source tags used per line: **[EN-wiki]** = a blackclover.fandom.com page that the search listed and summarised (Sea_Dragon's_Roar, Sea_Dragon's_Nest, Valkyrie_Dress, Episode_46, Noelle_Silva);
  **[EN-news]** = comicbook.com / screenrant / tvtropes / epicdope summaries; **[JA]** = Japanese blogs, MANTANWEB, anibu, ja.wikipedia as summarised by search; **[own art]** = what I saw with my own
  eyes in your uploaded packs; **[mod]** = the repo.
* NOT fetchable / not found: the fandom page text itself (only search summaries), `ja` fandom wiki (never surfaced), AniList / MyAnimeList / Crunchyroll episode summaries (never surfaced in
  results), any video, any animator interview about water effects (only an unrelated IMDb piece about a freelance animator "Beast" on episode 166), any frame-by-frame breakdown. The 2026-10 season
  note in one result was not checked. Colours and exact shapes below are therefore partly **my reading of your own art**, flagged as such.

## 1. Canon, per spell (what the anime shows, text only)

| Spell (EN / JA) | Anime appearance, as far as text sources go | Source |
|---|---|---|
| Sea Dragon's Roar / 海竜の咆哮 (Water Creation Magic) | Debut ep 46 "Awakening" (Underwater Temple arc, vs Vetto; Kahono's cheering). A giant water dragon: the wiki says the spell manifests the HEAD of a sea dragon with sharp teeth and fires it at the target; the caster can steer it mid-flight if it misses. It breaks Vetto's Beast Armor, tears off his arm, throws him out of the temple. Later uses cost less mana. | [EN-wiki] Sea_Dragon's_Roar, Episode_46; [EN-news] comicbook.com; [JA] raffanime ep 46 blog ("海竜の咆哮強すぎない"), JA summaries: 右腕を吹き飛ばした |
| Point-Blank Sea Dragon's Roar | A large dragon head with the user standing in its throat (close-range variant). | [EN-wiki] Point-Blank_Sea_Dragon's_Roar |
| Sea Dragon's Cradle / 海竜の揺り篭 | A large sphere of water that carries her and allies, protects from pressure and whirlpools; learned in the Vetto fight from the allies' cheers (JA); used to transport the Bulls into the temple (EN). | [EN-news] epicdope, tvtropes; [JA] ja.wikipedia (防御魔法, 仲間の応援), raffanime |
| Sea Dragon's Nest / 海竜の巣 | The user crouches, both hands near the ground, and a dome of several whirlpools forms round her; the swirls block many piercing hits from any direction without breaking. JA: a ball-shaped barrier of swirling water. First real use: protecting villagers (Heath's group). | [EN-wiki] Sea_Dragon's_Nest; [JA] ja.wikipedia (渦を巻く水で作られた球体状のバリア) |
| Valkyrie Dress / ヴァルキリードレス (Valkyrie Armor) | Ep 108 "戦場の舞姫" (JA) / ep 108 (EN). A suit of water condensed round her body with a shifting water lance; she controls the mana round the armour to glide / "swim" through the air; roar can be cast from it. JA viewers: "魔法少女のよう", armour reminded one blogger of Saint Seiya cloths. | [EN-wiki] Valkyrie_Dress, Noelle_Silva; [EN-news] comicbook.com (ep 108); [JA] mantan-web, anibu, hatenablog ep 108 |
| Saint Valkyrie Dress / 聖域・海神戦乙女の羽衣鎧 | Later upgrade with Undine: halo, lance, a full set of wings. | [EN-wiki] Saint_Valkyrie_Dress; [JA] syntelligence summary |
| Mermaid Form | Sleeker, faster Valkyrie Armor; armour turns into a fish-like tail (ep 165). | [EN-news] comicbook.com ep 165; [EN-wiki] Valkyrie_Dress |
| Valkyrie Armor: Dragon Form / Sea Dragon God Form | Leviathan-linked armour of the final arc (manga ch. 359 spoiler summary). Not looked at in the anime text. | [EN-news] animeexplained; [JA] syntelligence |
| Aqua Javelin | NOT found as a Noelle spell. NamuWiki lists it for a different user (a "Liar (copy)" entry). Treat the mod's Aqua Javelin as a mod original, keep the name. | [EN-news] NamuWiki via search |
| Sea Dragon's Waterball / Wrath / Tide Lance / Scale Mail / Torrent | Mod extensions, no canon looks (already noted in `docs/attributes/water_rebuild.md`). | [mod] |

What no text source could give me: colour values, how many frames the dragon is on screen, whether the anime draws it as flat cel water or as layered effects animation. The only visual proof I
can offer is the owner's own art (section 3 and `water_reference_board.png`).

## 2. What your own art says about the Black Clover water look (seen with my own eyes)

| Image (pack) | What it shows | Use |
|---|---|---|
| Valkyrie Armor fan art, OneHoox (`Black clover modes/image_837729.jpg`) | Armour and wings of translucent azure crystal; hard white specular streaks; ripple-pattern texture inside the plates; a thin pale halo; small blue droplets flying off; feather wings with blade tips. Dominant colours roughly #3FA9E8 (body), #8FD8FF (light), #FFFFFF (specular), #1E5FA8 (shade). | Armour/wing shapes, droplet scale |
| Bubble Refresher (`VFX Effects art`, anime still) | Water as foam: rounded white lobes, pale-blue shading underneath, big bubbles with bright rim highlights and a small glint. | Foam / bubble / splash crown shapes |
| Conrad steals Noelle's magic (anime still) | Noelle's magic drawn as a flat, bright cyan (#2FD8FF-ish) outline glow with soft bloom, no inner gradient. | Outline colour of a cel look |
| Ice Wedge Magic, Heavenly Ice Fang (anime stills) | The show's transparent-hard-material look: cyan body, white facet edges, dark-teal outline; speed spikes with a white core and a blue-grey fall-off; greyed impact frame behind. | Cel facets, impact frames |
| Hydra of Darkneros (manga panel) | Water serpents as black ink with white spray and speed lines (dark water, but the manga's way to draw a water monster). | Ink / impact-frame look |
| Blue grimoire cover (Black Clover Art pack) | Curling wave-tentacle scrolls round a cyan clover on dark teal. | Sigil / wave scroll |
| `docs/previews/noelle_forms` (mod render) | The mod's own Sea Dragon God form: blue scale texture, cyan glints, white speed streaks. | Stay consistent with the forms |

## 3. Every water effect the mod has today

Quad cost = vertices / 4; every effect is capped at 400 vertices (= 100 quads) per activation by the layer budget. Max numbers are from the repo preview (`python tools/vfx_preview/preview.py <scene>`,
`water_baseline.png`; scenes: `tools/vfx_preview/scenes.json`, `scenes.d/water_fx.json`, and `scenes.d/water_baseline.json` which I added for the two shapes that had no scene).

| VfxShape | Layer | Fired by (live?) | Look today | Max verts (quads) |
|---|---|---|---|---|
| WATERFX_CAST | WaterFxLayer | LIVE: `WaterArts` roar, javelin, wrath, tide lance, waterball | sea sigil + ripple charge, a bright spray head running to the target trailing a thin straight stream with three winding strands, drops shed along it, crest + ripple at landing | 204 (51) |
| WATERFX_ZONE | WaterFxLayer | LIVE: cradle, nest | sigil turning on the ground, three ripples running out, spouts curling at the rim, falling drops | 156 (39) |
| WATERFX_BURST | WaterFxLayer | LIVE: impacts, scale mail, torrent, Mermaid / God / Black Dragon forms | three ripples, eight wave crests thrown out, a spray cloud, drops on arcs | 120 (30) |
| WATER_SPLASH | WaterMagicLayer | LIVE: Mist Step, blood pages, paint pages, Tensura shots | a flat splash plane + glow, up to 20 glow-dot droplets on arcs | 92 (23) |
| WATER_RING | WaterMagicLayer | LIVE: Mist Veil / Prison, basic arts, paint pages | three ground rings (water-blend ring + additive rim) | 400 (100, at the cap) |
| WATER_DRAGON | WaterSpellLayer | OLD, no page sends it | beam-chain body with a sine wave, foam collar ring, one flat head plane, 16 spray billboards | 220 (55) |
| WATER_CRADLE | WaterSpellLayer | OLD | whirling sphere with two currents and orbiting globs | 308 (77) |
| WATER_BURST | WaterSpellLayer | OLD | splash crown, droplets, ripples | 340 (85) |
| WATER_JAVELIN | WaterSpellLayer | OLD | dense core, two twisting currents, spray cone, pressure rings | 348 (87) |
| WATER_NEST | WaterSpellLayer | OLD | dome of counter-turning bands, spouts, ripples | 328 (82) |
| WATER_DRESS | WaterSpellLayer | OLD (the live Valkyrie armour is the 3D `WaterAura` model set, `docs/attributes/water_rebuild.md`) | skirt of currents, breastplate band, crown ring, orbiting drops | 296 (74) |
| DEMON_WATER_FX1-3, BOSS_TIDE_*, MIST_MAGIC | DemonWaterLayer, TideBossLayer, others | LIVE for Demon Water (dark water) and the Tide boss | a different, darker look; out of this remake unless you say so | n/a |

Textures: `textures/particle/water_band|flow|sphere|drop|dragon|splash(_0-5).png` (old), `waterfx_stream|ripple|drop|sigil|wave|spray|head.png` (generated by `tools/gen_water_fx_textures.py`).
Blends: `VfxBlend.WATER` (tinted translucent, draws after ALPHA), ADD for glints.

## 4. Gap analysis (current look vs. canon + your art)

Judged from `water_baseline.png` (my eyes) against sections 1 and 2:

1. **Roar is a thin spark, not a dragon head.** The live WATERFX_CAST is a bright dot dragging a hair-thin stream (visible in the contact sheet: a white-blue star on a line). Canon is a big
   toothed dragon head (EN wiki). The old WATER_DRAGON had a head plane and a body, but no jaw/teeth silhouette and is not sent by any page. Biggest gap.
2. **No cel / outline language.** The anime shows water as flat shapes with hard white highlights and a bright cyan outline (Conrad/Ice Wedge stills, fan art). The mod's water is soft additive
   glow plus noisy strands: it reads as "magic sparkle", not as water.
3. **No foam.** Water in the show is drawn as rounded white foam lobes and rimmed bubbles (Bubble Refresher). Splash and burst use spray dots and thin ripple lines.
4. **Zones (cradle / nest) are a ground sigil with spouts.** Canon: a closed sphere / dome of whirlpools with visible bands. The old WATER_CRADLE / WATER_NEST were closer but are unused.
5. **Ripples are line art.** WATER_RING is 100 quads (at the cap) and still looks like thin scratches in the preview.
6. **Impacts lack an impact frame.** The show punctuates hits with a white-out / ink flash (Heavenly Ice Fang, Hydra panel); none of the water effects has one.
7. **Valkyrie.** The 3D armour set covers the body; its VFX layer (halo, wing droplets, tail trail) is only sprites. Canon adds a lance, halo, wings; the 3D set already has them, so remake VFX should
   only add water behaviour round it (drips, wing spray, Mermaid trail).
8. **Unused code.** Six old shapes (WATER_DRAGON .. WATER_DRESS) are dead weight but must stay (rule 2).

## 5. Budget facts the remake must respect

* Layer cap 400 vertices per activation; today's live shapes use 92 to 204 (WATER_RING hits 400). The proposal budgets stay at or below the current live cost per effect.
* No shaders: everything is quads with generated PNGs and the existing blends (ALPHA, WATER, ADD). Distance culling / `ctx.seg(n, min)` LOD already exist in the layers.
* Client option goes in a CLIENT config (looks only), e.g. `vfx.element.water = V2 | V1`, default decided by the owner; all gameplay and packets unchanged (rule 9, 11).
