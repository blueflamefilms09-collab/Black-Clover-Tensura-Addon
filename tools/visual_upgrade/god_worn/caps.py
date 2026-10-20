"""The per-grade cap table of the worn relic models (append-only; documented in docs/visual_upgrade/god_worn.md).

Until 1.102.0 every worn model had one cap: MODEL_CAP = ENSEMBLE_CAP = 2600 vertices (tools/relics/gen_worn_all.py), the limit the owner's performance pass was measured against.
Those two numbers are NOT changed: every other relic keeps them. The visual upgrade raises the allowance for its own items only, by grade, in this table; a later slice of
another grade appends its own row (never edits an existing one). The same numbers are read by GodWornUpgradeTest (Java) so the data and the test cannot drift.

  model_vertices     one model file (the shared WornMeshTest allows 5000)
  ensemble_vertices  everything one relic wears at full detail (sum over its pieces, a mirrored pair counts twice)
  held_vertices      the held / ground / item frame form of one relic (the God hand cap of the brief)
  lod_fraction       the LOD form may have at most this share of the full ensemble's vertices (drawn beyond 12 blocks and at the low quality)
  lod_distance       blocks beyond which the LOD form is used
  additive_cutoff    blocks beyond which the additive passes (glow pass, glow map, shell) are skipped for an upgraded relic
"""

GRADE_CAPS = {
    "LEGACY": {"model_vertices": 2600, "ensemble_vertices": 2600},                       # every relic that is not upgraded (gen_worn_all.py MODEL_CAP / ENSEMBLE_CAP)
    "GOD": {"model_vertices": 4800, "ensemble_vertices": 16000, "held_vertices": 2500, "lod_fraction": 0.15, "lod_distance": 12.0, "additive_cutoff": 24.0},
}
# the God ensemble cap is high on purpose: the 5x quad target of the brief is measured against the old counts (up to 648 quads for a pair of greaves), so a pair of arms or legs
# needs 3000+ quads; the cost is bounded by the LOD (15% beyond 12 blocks and at the low quality) and the per-grade counts above, not by the old cap.


def check_item(rid, slot, stats, grade="GOD"):
    c = GRADE_CAPS[grade]
    out = []
    full, held, lod = stats["full"], stats["held"], stats["lod"]
    if full["vertices"] > c["ensemble_vertices"]:
        out.append("full ensemble %d vertices > %d" % (full["vertices"], c["ensemble_vertices"]))
    if held["vertices"] > c["held_vertices"]:
        out.append("held form %d vertices > %d" % (held["vertices"], c["held_vertices"]))
    if lod["vertices"] > c["lod_fraction"] * full["vertices"] + 1e-6:
        out.append("lod %d vertices > %.0f%% of %d" % (lod["vertices"], 100 * c["lod_fraction"], full["vertices"]))
    return out
