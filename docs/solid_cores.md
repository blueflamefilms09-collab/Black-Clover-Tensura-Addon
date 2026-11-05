# Solid cores: the rank Mana Cores and the Beast Will Core are no longer see-through

Owner report: "the item should not be transparent" (answered: both the Beast Will Core and the rank Mana Cores, and the fix covers every will's core, old and new).

## Cause (found by reading the mesh files and `client/WeaponRenderer`, not seen in a game)

1. **Every sphere quad of the 24 rank cores was wound clockwise seen from outside** (`tools/gen_mana_core_models.py`: corners `(i,j) (i+1,j) (i+1,j+1) (i,j+1)` with `i` = latitude going down, `j` = longitude going round). A known-good
   weapon mesh (anheteps_sunscourge) has 1870 of 1870 quads counter-clockwise against their normal; the 24 cores had 0 of 288. The body is drawn with `RenderType.entityCutout` and the glow with
   `NURenderTypes.additiveAlpha`; both keep back-face culling ON. So the near half of every orb was culled and the inside of the far half showed through: a hollow, glassy orb in hand, on the ground and in item frames.
2. **The glow pass sat exactly on the body** (same vertices, depth write off), so it flickered against the body and, with the front culled, lit the far inside wall.
3. **The 16x16 inventory icons had a soft rim** (96 partly transparent pixels each, from a LANCZOS downscale of an alpha mask), and the old Beast Will sprite had a translucent halo (21 partly transparent pixels).
4. Not a cause: the skins are opaque RGB (checked), the body is not drawn translucent, the void layer is not used by the cores.

## Fix

* **Winding**: `tools/gen_mana_core_models.py` now writes `(i,j) (i,j+1) (i+1,j+1) (i+1,j)` (outward). `tools/solid_cores.py` repairs the 24 shipped meshes without the GLBs (reverses every inward quad, idempotent; `--check` reports).
* **Glow = a thin outer shell**: layer 2 is scaled to 1.015 of the body (marker line `# solid-cores` in each mesh). The far side of the shell is hidden by the body's depth; the near side adds light only. It cannot show the inside.
* **Icons**: the icon is shaded at 64 px and box-averaged to 16 px; each pixel is then fully opaque or fully clear (transparent only outside the circle).
* **Beast Will Core = a solid 3D orb** (`tools/beastwill/gen_beast_core_model.py`, reusing the sphere / skin / mesh writer of the rank cores): rough dark stone (radius jittered up to 7 percent, facets of different grey), cut by a network of
  white-hot fissures that are widest round the equator (the glowing heart); opaque skin + glow skin; mesh `weapon_meshes/beast_will_core.txt`, `weapon_classes.json` "handitem", `models/item/beast_will_core.json` = `builtin/entity` with
  the display transforms of `core_e_beast.json`. Drawn through `BeastWillItems.renderables()` (a one-line list like `manacore/CoreItems.renderables()`) added to `WeaponRenderer.WEAPONS`.
* **Per-will colour (chosen: a tint, not 9 skins)**: the skin is neutral, and `beastwill/BeastCoreLook` turns the will id on the stack into a colour (the `tint` of each `data/nusmp/beast_will/<id>.json`, equal-checked by a test;
  an unknown data-pack will gets a stable colour from its id's hash, a bare core the old violet). `WeaponRenderer` multiplies it into the body vertex colour (60 percent toward the colour) and uses it as the glow colour; an item
  colour handler tints the flat icon the same way. One skin, one glow skin, no extra textures, works for all 9 shipped wills (the original four included) and any future one. The client has no will catalog, so the id on the
  stack (already synced) is the only input.

## NEW / REPLACED / KEPT

* NEW: `beast_will_core` 3D mesh, skin, glow skin, `BeastCoreLook`, `BeastWillItems.renderables()`, `tools/beastwill/gen_beast_core_model.py`, `tools/solid_cores.py`, `SolidCoresTest`, `BeastCoreLookTest`, `models/item/beast_will_core_icon.json`.
* REPLACED (the owner asked for it): the winding and glow placement of the 24 `core_*` meshes, their 16x16 icons (solid), `models/item/beast_will_core.json` (flat generated -> 3D `builtin/entity`), the old Beast Will sprite (same
  design, now solid and neutral so the tint can colour it; halo removed).
* KEPT: all 24 core ids, skins, glow skins, display transforms, item ids and data components (old worlds load), the rank-core `_icon` models, `gen_core_icon.py` (now writes `beast_will_core_icon.json`), every other weapon (the
  renderer change is two guarded lines that only act on a Beast Will Core).

## Cost and safety (rules 9 and 11)

Client only, no packet, no server work. Per core in hand: 288 body quads + 288 glow quads = 576 quads (2304 vertices), the same as a rank core; no allocation per frame (a string-keyed map lookup and integer maths). The new
common class `BeastCoreLook` names no client class (`BeastWillSourceScanTest` scans it); `WeaponRenderer` is already client-only. No change to `docs/server_safe_allow.txt`.

## Tests

`SolidCoresTest` (new): all 25 meshes exist, every body and glow quad is counter-clockwise against its normal, the glow is a shell outside the body, skins opaque, glow skins present, icons have no semi-transparent pixel and no hole inside
the circle, the Beast Will Core model / icon / class entries exist and `WeaponRenderer` lists both core families. `BeastCoreLookTest` (new): the tint table equals every will file, no two wills share a colour, unknown wills are stable and
distinct. Run in this session only through a hand-made harness (no Gradle offline); the full sweep belongs to the release.

## Caveats

Not seen in a game (no Minecraft jar here): the fix is from the geometry and the render-type flags; the preview image is a Pillow shading of the real skins, and its "before" column is a simulation of the culled-front look. If an orb
still looks hollow in game, check first that the resource reload re-read the meshes (`WeaponRenderer` clears its cache on `BakingCompleted`). The icon tint needs the item colour handler registered in `WeaponRenderer.init`; without it the
icon is grey, not wrong.
