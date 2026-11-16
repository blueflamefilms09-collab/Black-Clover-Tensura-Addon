# Devil bodies, group 2 (Chainsaw Man, Black Butler, Dororo, Hall devils)

Made in Blender with `tools/blender/devil_group2.py` (bmesh lofts, sweeps, subdivision, decimate, smart UV, Cycles bake) through `bl_lib.py` / `bl_entity.py`. Checker: `python tools/blender/devil_check.py`.

| Devil | Source of the look | Parts | Quads (body + glow, LOD) | Height | Confidence |
|---|---|---|---|---|---|
| fox_devil | Chainsaw Man, from memory (fandom blocked) | 8 | 1427 + 78, LOD 586 | 2.35 | low-medium |
| curse_devil | Chainsaw Man, from memory | 8 | 1308 + 60, LOD 534 | 1.93 | low |
| future_devil | Chainsaw Man, from memory | 8 | 1167 + 39, LOD 498 | 1.88 | low |
| ghost_devil | Chainsaw Man, from memory | 11 (wings, tail) | 2062 + 222, LOD 880 | 2.44 | low |
| sebastian | Black Butler, from memory | 10 (tail coat) | 2325 + 132, LOD 990 | 1.95 | medium |
| hall_demon | Dororo (the 48 demons), from memory | 8 | 1931 + 90, LOD 771 | 2.15 | low |

Sources: the English and Japanese fandom wikis answered 403 (blocked) and were not worked around; Wikipedia character lists for Chainsaw Man were read in the session (`canon_sources.md`), but they hold no visual detail. No image was seen. Looks are therefore designed from memory of the series; each is a player-scale body with the series' recognisable features, not a copy.

NEW: six bodies. REPLACED: nothing. KEPT: the old generic horns and wings stay as the fallback and with the client setting `devil.customForms` off.
Cost: one body mesh of at most 2457 quads close up, a far copy of at most 990 quads, glow only within 48 blocks; no per-frame allocation.
