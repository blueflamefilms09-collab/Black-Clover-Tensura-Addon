"""Writes docs/custom_particles.md from the catalog, the theme table, the codemod map and git (the files the codemod edited).

    python3 -B tools/particles/gen_docs.py [--base 51fdb09]

Run it after tools/gen_custom_particles.py, tools/particles/gen_themes.py and tools/particles/codemod.py.
"""
import argparse
import collections
import json
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
RES = os.path.join(ROOT, "src", "main", "resources")
NUSMP = "src/main/java/com/newuniverse/nusmp/"


def load(path):
    with open(path, encoding="utf-8") as fh:
        return json.load(fh)


def git(*args):
    return subprocess.run(["git"] + list(args), cwd=ROOT, capture_output=True, text=True).stdout


def codemod_report():
    tmp = os.path.join(ROOT, "build_codemod_report.tmp.json")
    subprocess.run([sys.executable, "-B", os.path.join(HERE, "codemod.py"), "--check", "--no-gen", "--report", tmp], cwd=ROOT, capture_output=True, text=True)
    try:
        return load(tmp)
    finally:
        if os.path.exists(tmp):
            os.remove(tmp)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default="51fdb09")
    a = ap.parse_args()
    cat = load(os.path.join(RES, "data", "nusmp", "particle_catalog.json"))["particles"]
    themes_doc = load(os.path.join(RES, "data", "nusmp", "particle_themes.json"))
    themes = themes_doc["themes"]
    cmap = load(os.path.join(HERE, "map.json"))
    uses = collections.defaultdict(list)
    for tid, t in themes.items():
        for p in t["particles"] + t.get("dark", {}).get("particles", []):
            if tid not in uses[p]:
                uses[p].append(tid)
    groups = collections.OrderedDict()
    for p in cat:
        groups.setdefault(p["group"], []).append(p)

    L = []
    w = L.append
    w("# Custom particles (1.103.0)")
    w("")
    w("The owner's request: make the mod's own particles instead of reusing the vanilla ones, and let the Rukh (the magoi) belong to the Djinn and the dungeons only (`docs/rukh/exclusive.md`).")
    w("Everything below was built without a running game: nothing here has been looked at in game (see the last section).")
    w("")
    w("## What is in it")
    w("")
    w("| Part | Where |")
    w("|---|---|")
    w("| %d particle types in %d groups, 4 to 8 frames each, 16x16 and 32x32 pixel art, additive glows | `particle/fx` (`FxTypes`), textures `assets/nusmp/textures/particle/<id>_<n>.png`, sprite lists `assets/nusmp/particles/<id>.json` |" % (len(cat), len(groups)))
    w("| One particle class with a physics profile per type (gravity, drag, spin, swirl, sway, jitter, size curve, fade, light, blend, lifetime) | `client/particle/fx/FxParticle`, profiles in `data/nusmp/particle_catalog.json` (written by the generator) |")
    w("| One provider for all types, one atlas lookup list per type, nothing allocated per particle but the particle | `client/particle/fx/FxClient` |")
    w("| Tint payload (colour + size) with a codec and a stream codec | `TintedParticleOptions`, `TintedParticleType`, pure data in `FxTint` |")
    w("| Theme table: %d themes (all 105 magic types, 7 relic grades, 21 relic elements, 100 relic waves, skills, bosses, dungeon areas), 1 to 4 particles + a colour + a dark flavour each | `data/nusmp/particle_themes.json` (written by `tools/particles/gen_themes.py`), `ParticleThemes`, `ThemeKey` |" % len(themes))
    w("| The facade `Fx` (spray / add / burst / stream / gather / aura / send / ring) through the perf budgets, the quality level and the distance | `particle/fx/Fx`, `FxRelic` (relics by colour hue) |")
    w("| Switches `particles.custom` (client file `nusmp-particles-client.toml`, server file `nusmp-particles-server.toml`, default true); off = the old vanilla particle of the mapping (Rukh for the calls that used the Rukh) | `FxConfig` |")
    w("| `/multiverse particles list [group]`, `spawn <id> [count] [color]`, `theme <key>`, `status` (operators) | `FxCommands` |")
    w("| The re-runnable codemod and its map | `tools/particles/codemod.py`, `tools/particles/map.json` |")
    w("| Generators | `tools/gen_custom_particles.py` (textures, jsons, catalog, sheet), `tools/particles/gen_themes.py`, `tools/particles/gen_docs.py` (this file) |")
    w("| Preview sheet (every frame, dark and light background) | `docs/previews/custom_particles/sheet.png` |")
    w("| Self-test `particles.custom` (CI server smoke), lang fragment `tools/lang/custom_particles.json` | `FxSelfTests` |")
    w("")
    w("## Using it from code")
    w("")
    w("```java")
    w("Fx.send(level, FxThemes.BOSS_ZENON, x, y, z, 24, 0.6);                      // a puff of the theme's particles, shared over its 2-4 particles")
    w("Fx.ring(level, ThemeKey.of(\"magic.FLAME\"), cx, cy, cz, 3.0, 24);             // a ring of its signature particle")
    w("Fx.spray(level, FxThemes.SKILL, FxTone.DARK, x, y, z, 6, 0.2, 0.2, 0.2, 0.02); // the dark flavour of the theme")
    w("Fx.options(ThemeKey.of(\"relic.grade.GOD\"), FxTone.LIGHT);                    // the ParticleOptions, for code that builds its own packets")
    w("```")
    w("")
    w("Every call goes through `PerfBudgets` (server: `maxParticlesPerTick`; client: `clientParticlesPerTick`), thins the count by `vfxQuality` (low 0.5, medium 0.75) and, on the client, by the distance")
    w("from the camera (the quality's near/far bands, nothing past the cull distance), and caps the count per call by quality (48 / 140 / 400). The Rukh-style emitters keep the caps the Rukh had (12 per burst, 6 per stream tick, 12 per")
    w("gather, 2 per aura pulse) so swapping a Rukh call for a themed call does not change how many particles an ability shows.")
    w("")
    w("## The particles")
    w("")
    w("Status: every particle in this table is **NEW** (nothing like it existed). Frames and size are the files on disk; `tint` means the texture is neutral light so the theme colour dyes it.")
    w("")
    for g, items in groups.items():
        w("### %s (%d)" % (g, len(items)))
        w("")
        w("| id | status | frames | px | tint | behaviour | used by (themes) |")
        w("|---|---|---|---|---|---|---|")
        for p in items:
            u = uses.get(p["id"], [])
            shown = ", ".join("`%s`" % x for x in u[:4]) + (" +%d more" % (len(u) - 4) if len(u) > 4 else "")
            if not u:
                shown = "the codemod's fixed replacements" if any(v.get("fixed") == p["id"] for v in cmap["particles"].values() if isinstance(v, dict)) else "(new code: `/multiverse particles spawn %s`)" % p["id"]
            fixed = [k for k, v in cmap["particles"].items() if isinstance(v, dict) and v.get("fixed") == p["id"]]
            if fixed:
                shown += (" ; replaces vanilla " + ", ".join(fixed))
            w("| `%s` | NEW | %d | %d | %s | %s | %s |" % (p["id"], p["frames"], p["px"], "yes" if p["tint"] else "no", p["behaviour"].replace("|", "/"), shown))
        w("")
    w("## Replaced, kept, new")
    w("")
    w("| What | Status |")
    w("|---|---|")
    w("| The %d custom particle types and their textures, jsons, catalog | NEW |" % len(cat))
    w("| Vanilla `ParticleTypes` calls (`sendParticles`, `addParticle`, helper arguments, switch results) in 76 files outside the Djinn, relic and rift lanes | REPLACED by `Fx` calls (the old particle stays as the `particles.custom=false` fallback in the generated `FxOld`) |")
    w("| Light/Dark Rukh calls (`RukhFx`, `RukhClientFx`, `RukhParticles`, `RukhFx.Alignment`) outside the Djinn and the dungeons | REPLACED by `Fx` / `FxTone` / `FxRelic` |")
    w("| The Rukh types, textures, emitters, purge scope and tests | KEPT (the Djinn and the dungeons use them; `particles.custom=false` falls back to them) |")
    w("| Rukh in `relics/djinn/**`, `client/relics/djinn/**`, `relics/Fx.java` (the Djinn's shared helper), `labyrinth/**`, `client/labyrinth/**`, `dungeon/**`, `client/dungeon/**` | KEPT |")
    w("| Vanilla particles that carry data: block and falling-dust options (`BlockParticleOption`), item particles, dust options held in a variable or built by a helper | KEPT (reasons below) |")
    w("| Time Magic: `book/TimeBook.java`, `vfx/client/layer/TimeMagicLayer.java`, `textures/particle/time_*.png`, `tools/gen_time_vfx_textures.py` | KEPT, untouched (the codemod's skip list; a test pins it) |")
    w("| The textured-quad VFX layers (`vfx/**`) | KEPT (not particle types) |")
    w("| `MagicType.particle()` / `MagicProfile.particle()` | CHANGED type: `SimpleParticleType` became `ParticleOptions`; each of the 105 magics now answers with its own theme's particle |")
    w("")
    w("## The themes")
    w("")
    w("A theme is `[mote, accent, spark, ambient]` (`FxSlot`) plus a colour and a dark flavour. The codemod maps `END_ROD`, `GLOW`... to the MOTE slot, `ENCHANT`, `PORTAL`... to ACCENT, `CRIT`, `ELECTRIC_SPARK`... to SPARK,")
    w("`SMOKE`, `CLOUD`, `POOF`... to AMBIENT; elemental ones keep their element whatever the theme (a `FLAME` is a `flame_tongue`, `SNOWFLAKE` a `snowflake`) and the one-shot shapes become `impact_burst`, `circle_flash`, `slash_streak`,")
    w("`shock_ring` in the theme colour (`tools/particles/map.json`, section `particles`). Counts: %s." % ", ".join("%s %d" % (k, v) for k, v in sorted(collections.Counter(t.split(".")[0] + ("." + t.split(".")[1] if t.startswith(("relic.", "boss.", "dungeon.", "skill.")) else "") for t in themes).items())))
    w("")
    w("### The 105 magic types")
    w("")
    w("| magic | particles (mote, accent, spark, ambient) | colour |")
    w("|---|---|---|")
    for tid, t in themes.items():
        if tid.startswith("magic."):
            w("| %s | %s | `%s` |" % (tid[6:], ", ".join(t["particles"]), t["color"]))
    w("")
    w("### Relic grades and elements")
    w("")
    w("| theme | particles | colour |")
    w("|---|---|---|")
    for tid, t in themes.items():
        if tid.startswith(("relic.grade.", "relic.element.")):
            w("| `%s` | %s | `%s` |" % (tid, ", ".join(t["particles"]), t["color"]))
    w("")
    w("A relic helper that only knows a colour (`relics.Fx.ring/burst/dust`) is moved to `FxRelic`, which picks `relic.element.<x>` from the hue of the colour (`FxHue`: red fire, dark red blood, gold light, green nature, cyan ice, blue water, violet arcane, pink dream, dark, grey metal, white holy) and dyes the particles with it.")
    w("")
    w("## The codemod")
    w("")
    w("```")
    w("python3 -B tools/particles/codemod.py                  # rewrite in place, print the report")
    w("python3 -B tools/particles/codemod.py --check          # change nothing, exit 1 if a run would change something")
    w("python3 -B tools/particles/codemod.py --diff --only 'item/**'")
    w("python3 -B tools/particles/codemod.py --include-relics --include-lanes   # AFTER the builders of relics/wave, client/relics, relics/djinn and the rift pockets merged")
    w("```")
    w("")
    w("It skips: Time Magic, the particle libraries, `NUSMP.java`, the lanes (`relics/wave`, `client/relics` outside the Djinn, `relics/socket`, `relics/order`, `relics/pair`, `relics/forge` unless `--include-relics`; `labyrinth/mega/rift` unless")
    w("`--include-lanes`), Rukh calls in the Djinn and dungeon paths, anything that carries data it cannot map. It is idempotent. It also writes the two generated files `FxOld.java` and `FxThemes.java`.")
    w("After the flagged run: create `docs/rukh/exclusive_enforced` (turns the tolerance of `RukhExclusiveScopeTest` into a failure).")
    w("")
    w("### Measured run (on the tree before the codemod, default flags)")
    w("")
    w("81 existing files edited (76 by the first run, 5 more when the relic helpers were added), 426 call sites replaced plus 66 type and comparison tokens (`Alignment` to `FxTone`):")
    w("vanilla `sendParticles` 150, vanilla `addParticle` 14, vanilla particles as values 81, dust options 11, `RukhFx`/`RukhClientFx` calls 56, `RukhParticles` values 105 and `sendParticles(RukhParticles...)` 3, relic helpers 6.")
    w("With `--include-relics --include-lanes` on top: 126 more files (relic waves 113, client relics 4, sockets 2, order 2, pair 1, forge 1, rift pockets 3), 1567 more edits; the result type-checks against the API stubs and a second run changes nothing.")
    w("")
    rep = codemod_report()
    w("### Call sites left, with the reason (now)")
    w("")
    reasons = collections.Counter(r for v in rep["left"].values() for _, r, _ in v)
    w("| reason | count |")
    w("|---|---|")
    for r, c in reasons.most_common():
        w("| %s | %d |" % (r, c))
    w("")
    w("Not in a lane, left on purpose:")
    w("")
    w("| file | line | reason |")
    w("|---|---|---|")
    for f, v in sorted(rep["left"].items()):
        for ln, r, txt in v:
            if r.startswith("lane:"):
                continue
            w("| `%s` | %d | %s |" % (f, ln, r))
    w("")
    w("### Existing files edited, by package (git, against `%s`)" % a.base)
    w("")
    names = git("diff", "--name-status", "--diff-filter=M", a.base, "HEAD", "--", "src/main/java").splitlines()
    by = collections.defaultdict(list)
    for line in names:
        st, path = line.split("\t", 1)
        rel = path[len(NUSMP):]
        if rel.startswith(("particle/fx/", "client/particle/fx/")):
            continue
        parts = rel.split("/")
        key = "/".join(parts[:2]) if len(parts) > 2 else (parts[0] if len(parts) > 1 else "(root)")
        by[key].append(rel)
    w("| package | files | |")
    w("|---|---|---|")
    for k in sorted(by):
        w("| `%s` | %d | %s |" % (k, len(by[k]), ", ".join("`%s`" % os.path.basename(x) for x in sorted(by[k]))))
    w("")
    w("Shared files touched with append-only lines: `NUSMP.java` (two lines: `FxBoot.init`, `FxClient.init`). Hand edits after the codemod: `core/magic/profile/*` (`particle()` returns `ParticleOptions`, `themeKey()`), `blackclover/MagicType`, `skill/GrimoireMagicSkill`, `labyrinth/mega/guard/GuardianTraits` (ring helper takes any options), comments in `skill/SkillFx`, `skilltree/line/LineFx`, `vfx/client/portal/PortalClient`.")
    w("")
    w("## Tests")
    w("")
    w("* `FxLibraryAssetsTest`: every catalog entry has a sprite json listing exactly its frames, every frame is a non-empty PNG of the catalog size, the families the owner asked for exist, the 8 rune glyphs differ, additive particles are full-bright, the type loop and the provider loop are in place.")
    w("* `ParticleThemesTest`: themes name only registered particles, cover all 105 magic types (read from `MagicType.java`), the 7 grades, the relic elements and every theme the map names; MOTE/AMBIENT slots are small; resolution rules.")
    w("* `FxMathTest`: size/alpha curves, frame selection, thinning by quality and distance, the share of a burst, the Rukh caps, the profile parser, the tint payload round trip (bytes), colour parsing.")
    w("* `FxHueTest`: the colour wheel to relic element.")
    w("* `FxCodemodMapTest`: `FxOld` and `FxThemes` match `map.json`; the skip list keeps Time Magic out.")
    w("* `FxSourceScanTest`: no mapped vanilla particle (`END_ROD` above all) outside the allow-list, the facade keeps its budgets and quality scaling, the provider allocates only the particle, the command's lang keys exist.")
    w("* `RukhExclusiveScopeTest`: the Rukh are for the Djinn and the dungeons only (lanes tolerated until `docs/rukh/exclusive_enforced`); `RukhPurgeScopeTest` and `RukhBudgetTest` still pass.")
    w("* Self-test `particles.custom` (in the real game, CI server smoke): every type registered and bound, the real stream codec round trip, the theme table valid, the facade with the switch on and off, the budget respected.")
    w("")
    w("## What could not be verified")
    w("")
    w("* Nothing was seen in game. The textures were reviewed on the generated sheet only; how each particle moves and reads at its in-game size (0.15 to 1 block) is reasoned, not observed.")
    w("* The additive render type (`FxRenderTypes.ADDITIVE`: source-alpha plus one, depth write off) follows the vanilla translucent sheet with another blend function; if a custom render type misbehaves in the engine of this version, set `blend` to `translucent` in the generator's profiles.")
    w("* `RenderSystem.setShader`, `TextureAtlas.LOCATION_PARTICLES` and `RegistryFriendlyByteBuf` are reported by the API stub type-check as unknown or abstract (the stubs are incomplete; the same lines exist in the existing renderers); the whole tree type-checks with the same error set as before this change plus those three stub artefacts.")
    w("* Gradle was not available: the JUnit tests ran in the offline shim (pure and file-scanning classes only); tests that need Minecraft classes (`MagicType` users) were not run.")
    w("* The particle atlas now holds 422 more small sprites; nothing was measured.")
    w("")
    out = os.path.join(ROOT, "docs", "custom_particles.md")
    with open(out, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(L) + "\n")
    print("wrote", out, len(L), "lines")


if __name__ == "__main__":
    main()
