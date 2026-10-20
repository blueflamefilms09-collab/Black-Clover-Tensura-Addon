# The Rukh (magoi) belong to the Djinn and the dungeons (1.103.0)

## The owner's decision
"Magoi should be a Djinn user exclusive particle, besides maybe use them in dungeons", and "you can make custom particles instead of just using magoi". So:

* The Dark Rukh and Light Rukh (`nusmp:dark_rukh`, `nusmp:light_rukh`, `particle/rukh`) are now used by exactly two groups: the Djinn users (everything of `relics/djinn/**`
  and `client/relics/djinn/**`: equip, assimilation, acts, weapons, screen FX, GUI) and the dungeons (`labyrinth/**`, `client/labyrinth/**`, `dungeon/**`, `client/dungeon/**`:
  the Rukh flow, floors, System, safe rooms, rifts and pockets, ambience, bosses inside the dungeons). `relics/Fx.java` is the one shared helper (ring, burst, align, dust) the Djinn
  code calls; it keeps its Rukh, and every other caller was moved to `particle/fx/FxRelic` (same job, custom particles, picked by the hue of the colour).
* Everything else that used the Rukh since 1.36.0 (relic waves, worn relic FX, magics, songs, skills, corpses, grimoires, sword draws, hero weapons, the Spatial portal, the arena
  items) has its own custom particles: `docs/custom_particles.md`, `data/nusmp/particle_themes.json`.
* Nothing was deleted: the Rukh types, textures, emitters (`RukhFx`, `RukhClientFx`), the purge scope and its test all stay. The Rukh is also the fallback of the facade when
  `particles.custom` is off for a call site that used to spawn it.

## How it is kept
* `docs/rukh/exclusive_scope.txt` lists the owners (the single source of truth); `tools/particles/map.json` "rukh_keep" must list exactly the same paths (a test compares them).
* `RukhExclusiveScopeTest` scans every Java file outside the owners and the infrastructure (`particle/rukh`, `client/particle/rukh`, `particle/fx`, `NUSMP.java`) for `RukhFx`,
  `RukhClientFx` and `RukhParticles`. It fails on any use. The files other builders were editing when the codemod ran (the relic waves, `client/relics`, the sockets, the order, the
  pairs, the forge, the rift pockets: map.json "lanes") are tolerated until the marker file `docs/rukh/exclusive_enforced` exists.
* `RukhPurgeScopeTest` (the 1.36.0 purge: no vanilla particle in the scope of `docs/rukh/scope.txt`) is unchanged in what it enforces; its text now says that the Rukh scope shrank.

## Moving a call site: the codemod
`python3 -B tools/particles/codemod.py` rewrites `RukhFx.spray/burst/stream/gather/aura`, `RukhClientFx.*`, `RukhFx.Alignment`, `RukhParticles.light()/dark()/of()` and the
`relics.Fx.ring/burst/align/alignOf/dust` helpers outside the owners into the facade (`Fx`, `FxTone`, `FxRelic`) with the theme of the file's package; the Djinn and dungeon files are
left alone. Run it with `--include-relics --include-lanes` after the builders that work in `relics/wave`, `client/relics` and `labyrinth/mega/rift` have merged, then create
`docs/rukh/exclusive_enforced`. It is idempotent (`--check` says whether a run would change anything).
