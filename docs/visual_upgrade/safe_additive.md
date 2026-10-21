# Safe additive: the permanent fix of the white squares (1.110.0)

Owner report: wired white squares around spell magic circles, and now a hard white rectangle behind the hand / weapon of the upgraded relics and Genesis weapons (halo cards,
flares, rings, light shafts; Elsdocia shows a giant white translucent column with a grid, a white diamond at the hand and a plume of white square puffs).

## What was found

1. The 1.102.0 / 1.105.0 fix (`AdditiveTypes`, `NURenderTypes.additiveAlpha`, `vfx.safeBlend`) only covered the aura painters. The new renderers (`VuRenderer`, `WeaponUpgrade`,
   `GodMeleeBRender`, `WornLibDraw`, the Djinn and Tiny Djinn draws, `ArmorGlow`, ...) called `RenderType.eyes(tex)` and `RenderType.entityTranslucentEmissive(tex)` directly, so they never went through the audit.
2. `RenderType.eyes` blends `ONE, ONE`: the vertex and texture alpha do not weigh the colour, and the shader only drops texels under alpha 0.1. A card painted "white everywhere, alpha shapes it"
   is a full white box wherever its alpha is 0.1 or more. The shared sheet `textures/entity/weapon_vu/halo.png` (all 16 halo cards and flares of every Genesis weapon) was exactly that:
   * white RGB under alpha 0 (136k texels), the classifier did not see it as square-prone because the soft texels were not "fully transparent";
   * cell 15 ("solid") was a flat white square, alpha 212..255 over the whole 128 x 128 cell;
   * the borders of most cells were not transparent (shaft alpha 253 at its top edge, bolt 255, feather 244, streak 42, flame 46, sparkle 40, glow 8), so even a soft card ended in a hard edge.
3. The vertex alpha of the halo cards (the group's pulse and fade) was ignored by `eyes`, so the cards never faded, they just stayed a full white shape.

## What was fixed

| Part | Change |
|---|---|
| One central helper | `client/SafeAdditive` (`additive`, `emissive`, `lightning`). Every additive / emissive pass of the mod now asks it (58 call sites in 28 client classes, listed below). `additive` = `AdditiveTypes.of`: the alpha-weighted type (`SRC_ALPHA, ONE`, eyes shader, nearest sampling, no depth write, colour-only write mask) for any texture that is not premultiplied; `RenderType.eyes` only for a texture proven premultiplied (same look as before). `emissive` keeps the translucent emissive type (alpha blended, cannot box a transparent texel). |
| Classifier | `TextureAlpha.classify`: new rule `straightAlpha`: a texture whose semi-transparent texels are mostly brighter than their own alpha (a white card with a soft alpha) is not premultiplied, so it takes the alpha-weighted blend even when it has no fully transparent texel with colour. Premultiplied glows (rgb <= alpha) keep `eyes`. |
| Config fallback | `vfx.safeBlend` (`config/nusmp-vfx-client.toml`, CLIENT) now also forces `SafeAdditive.emissive` to the alpha-weighted additive type, besides every `additive` pass and the VFX buffer swap that already existed. |
| Textures | `tools/safe_additive/scan_textures.py` (scan, `--fix`): 326 PNG files lose the colour hidden under alpha 0 (visible result unchanged: alpha-0 texels add nothing in any blend); `halo.png` also gets a 6 px smooth alpha window on every card border and a soft radial falloff on the solid cell. The card interior is untouched. `vu_halo.build` applies the same cleaning, so regenerating the sheet gives the same file. PNGs re-packed with `tools/optimize_pngs.py` (pixel-exact check). |
| Card caps | `visual/genesis/VuCardLimits` + `vfx/SafeAdditiveMath`: after a mesh is parsed a halo card longer than 0.9 x the weapon's body height is scaled about its centre, and a flare is capped at 0.2 x (renderer). Every shipped mesh is inside the caps (largest card ratio 0.87, Elsdocia 84 of 125.6 = 0.67), so nothing changes today; it guards future meshes. |
| Blend maths | `vfx/SafeAdditiveMath` (pure): alpha-weighted add, plain add, the eyes discard cut, the caps. |

### Every additive / emissive pass, and what it uses now

* Genesis weapons, relics (`client/relics/item/vu/VuRenderer`): glow pass, vein pass, halo cards, flares -> `SafeAdditive.additive` (was `eyes`, direct).
* `client/WeaponUpgrade` (void/glow/vein/halo of the god-tier upgrades), `client/GodMeleeBRender` (glow, veins, wave, outline), `client/WeaponRenderer` (core glow, socket glow), `client/DemonSlayerRenderer` (bloom, cracks), `client/heroweapon/carriage/CarriageRenderer` -> `additive` / `emissive`.
* Worn relics: `client/relics/worn/lib/WornLibDraw` (atlas glow, shimmer, glow map, lit pass, star pass, halo pass), `ArmorGlow`, `DanteGravityCrownRenderer`, `ZenonSpatialRingRenderer`.
* Djinn: `client/relics/djinn/DjinnDraw`, `DjinnArmorLayer`, `tiny/TinyDjinnPortrait`.
* Auras and layers: `client/aura/AuraRender.emissive`, `client/mode/ModeArmorLayer`, `client/corpse/CorpseDecayLayer`, `client/thread/RougeRenderer`, `ZagredRenderer`, `remodel/KitZagredRenderer`, `GrimoireDaemonRenderer`, `MirrorDoubleRenderer`, `PaintedConstructRenderer`, `SpiritOrbRenderer`.
* Rifts and props: `client/labyrinth/rift/{DimensionalRiftRenderer, MegaRiftRenderer, RealityShards, RiftParallax}`, `client/labyrinth/vault/RelicPedestalRenderer`.
* Left as they were (already alpha-weighted, they call `blendFunc(SRC_ALPHA, ONE)` themselves): the VFX buffer (`VfxBlend`), `FxRenderer`, `PortalClient`, `FxRenderTypes`, GUI painters (boss bars, `MagicCircle`, `ForgeUi`, `ManaZoneClient`).
* Allow-listed to build render types themselves: `SafeAdditive`, `AdditiveTypes`, `NURenderTypes` (the Zagred fresnel aura keeps its own additive state with its own shader). The Time Magic layer (`TimeMagicLayer`, `time_*` textures, `TimeBook`) was not touched.

## Tests

* `vfx/SafeAdditiveMathTest` (pure blend maths, classifier rule), `vfx/SafeAdditiveSourceScanTest` (no client class outside the allow-list builds an additive render type or names `ADDITIVE_TRANSPARENCY` / eyes shader),
  `vfx/SafeAdditiveTextureScanTest` (no colour under alpha 0 in the additive scope, halo sheet borders and solid card, Elsdocia textures), `visual/genesis/VuCardLimitsTest` (every Genesis mesh, Elsdocia explicitly, the clamp).
* CI self-test: `visual.safe_additive` (`vfx/SafeAdditiveSelfTests`, registered with one line in `NUSMP`).
* Lang fragment: `tools/lang/white_square_fix.json` (key prefix `nusmp.white_square_fix`).

## NEW / REPLACED / KEPT

| | |
|---|---|
| NEW | `SafeAdditive`, `SafeAdditiveMath`, `VuCardLimits`, `SafeAdditiveSelfTests`, `tools/safe_additive/scan_textures.py`, the four test classes, this page, the lang fragment. |
| REPLACED | The direct `RenderType.eyes / entityTranslucentEmissive / lightning` calls of 28 client classes (now `SafeAdditive`); `halo.png` card borders and the solid card cell (alpha only, interior kept); the colour hidden under alpha 0 of 326 additive textures. |
| KEPT | `AdditiveTypes`, `AdditivePolicy`, `NURenderTypes.additiveAlpha`, `vfx.safeBlend`, every texture, mesh, cell index, flare and card position, the premultiplied glows' `eyes` look, the Time Magic VFX, the GUI blend code. |

## Not verified (nothing can be seen in a game)

* No frame was rendered. That the squares are gone is derived from the blend maths, the texture pixels and the source scan, not from a screenshot.
* Glow maps that the classifier now reads as straight alpha (white-ish with soft alpha) draw through `SRC_ALPHA, ONE`: their soft edges are dimmer than before (that is the point), but how much on each weapon was not looked at. `vfx.safeBlend` forces the same for all, `false` keeps `eyes` only for proven premultiplied ones.
* Dynamic (runtime-painted) glow textures cannot be audited from the resource manager; they take the alpha-weighted type (a log line the first time).
* The giant column in the Elsdocia screenshot: its shaft is 0.67 of the blade's height in the mesh, so the size was not the cause there; the cause found is the box blend plus the solid cell and hard card edges. If the in-hand size still looks too big, that is the weapon mesh scale (another change), not a card.
* Re-packing the PNGs with `optimize_pngs.py` was checked pixel-exact by the tool, not by eye.
