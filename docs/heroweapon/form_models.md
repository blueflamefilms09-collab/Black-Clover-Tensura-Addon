# Hero and Vassal weapon forms: one model per form (4.23.0)

Owner request: a form change of a Legendary, Seven Star or Vassal weapon must be a real change of the weapon's shape, matching the form's name, not a rename of the same
model. Every form that has a name of its own gets its own 3D model, made in Blender (rule 14), and the game draws it while that form is active.

## Convention (every art builder follows it exactly; the engine builder implements the reading side)

* **Mesh id** = `<weapon item id>__<form id>` (two underscores), e.g. `legendary_sword__wind_king_katana`, `vassal_scythe__demon_dragon_scythe`.
  Files: `assets/anime_multiverse/weapon_meshes/<meshid>.txt`, `textures/entity/weapon/<meshid>.png` and `<meshid>_glow.png` (the same format as the existing base meshes; `tools/blender/bl_lib.py` `finish()` writes them).
* **Bows** also write the three draw stages `<meshid>_pull1`, `_pull2`, `_pull3` (as `legendary_bow_pull1..3` do; they share the skin).
* **Which forms**: every form in `data/anime_multiverse/hero/forms/<weapon>.json` except `base`, and except the Legendary curse forms (`curse: true` on a Legendary weapon keeps the existing `_curse` look). The
  curse forms of the Seven Star / Vassal weapons (they had no curse look) DO get their own model (a monstrous version of the weapon, named after the form).
* **Same hand, same size class, same grip**: a form mesh uses the base weapon's `place` line orientation and the base's size class (`weapon_classes.json`, `tools/relics/weapon_classes.json`): its apparent
  length stays inside the class range (see `python3 tools/relics/audit_weapon_scale.py` for the table; never ship a form that audits `small`), the grip stays where the base's grip is (the hand holds the
  same place), `orient manual` only where the base has it. The silhouette however must change: the form's name says what it is (a katana is a katana, a hundred-blade sword has a hundred blades,
  a carapace sword is armoured with a tortoise shell). A recolour of the base is NOT acceptable.
* **Glow**: each form has its own glow skin (`glow` line) in the colour of its theme; no hunger or gameplay effect is involved (looks only).
* **Blender scripts**: one file per family, `tools/blender/forms_<family>.py`, built with `tools/blender/bl_lib.py` (read it and `legendary.py` / `vassal_a.py` first; do not edit `bl_lib.py` or the existing
  scripts: put helpers in your own file). Run it as `python3 -B tools/blender/forms_<family>.py [--only id,id] [--trial DIR]` (bpy 5.3 is installed; no window needed). Keep the bake light (the container has 4 CPUs
  shared with other builders: use the lowest sample count that gives a clean skin, the same one the existing scripts use).
* **Manifest**: each builder writes `tools/blender/forms_manifest_<family>.json`: `{"<meshid>": {"weapon": "<id>", "form": "<form id>", "class": "<size class of the base>"}}` for every mesh it made (bow stages
  are implied). The coordinator registers the classes and refreshes the grip table from the manifests.
* **Previews**: each builder also writes `docs/heroweapon/form_previews/<family>.png` (a contact sheet, base weapon first and then each form, drawn by the repo's software preview of the mesh, as the
  existing weapons' previews are) so the owner can see what was made.
* **Do not touch**: shared Java files, `smoke.yml`, `CHANGELOG.md`, `gradle.properties`, `en_us.json`, the existing base and curse meshes, `weapon_classes.json`, `grip_golden.json`. Leave the work uncommitted in your worktree.

## Reading side (engine builder)

* The stack carries the active form id (`anime_multiverse:hero_form`, a new data component, set where the forms system sets `hero_look`). `WeaponRenderer.meshId` draws `<id>__<form>` when that mesh exists, else
  the curse look (curse forms), else the base look. Client setting `heroweapon.formModels` (default on) off = the old behaviour (base / curse look only).
* Self-test prefix `heroforms`.

## Done (engine side, 4.23.0)

Everything below is NEW (nothing replaced); the old base / curse looks are the fallback and stay.

* **Component** `anime_multiverse:hero_form` (string, persistent, network-synchronised, registered next to `hero_look` in `heroweapon/HeroComponents.java`; `HeroComponents.form(stack)` reads it, `base` when absent).
  It is written at the same two places as `hero_look`: `FormLoadout.shape` (every refresh, only when the value differs; the base form writes `base`) and `HeroLoan.make` (a fresh projection carries the active form).
* **Renderer** `client/WeaponRenderer.meshId`: for a `HeroWeaponItem` whose form is not `base`, with the setting on and the classic models off, `<id>__<form>` is used when that mesh exists; else the curse look; else the base look.
  The joined id is cached per (weapon, form) in a small bounded map (`formMeshId`), so a frame allocates nothing. Bows: the draw stages come from `RangedStage.chain(meshId, stage)` inside `ItemStateVariants.resolve`,
  which already works from the id it is given, so `<meshid>_pull1..3` resolve exactly as `legendary_bow_pull1..3` do (pinned by `FormMeshFilesTest`); no change was needed there. Grip / size class of a form mesh come from its own
  `weapon_classes.json` entry (`WeaponClasses.baseId` strips `_pull1..3`, so bow stages need no entry).
* **Setting** (CLIENT config, looks only, `nusmp-heroweapon-client.toml`, section `models`): `formModels`, default `true` (the key `heroweapon.formModels`). `false` = never a form mesh. `classicHeroWeaponModels = true` also means no form meshes
  (the classic models keep the old look).
* **Registering meshes** after the Blender builders ran: `python3 -B tools/heroweapon/register_form_meshes.py` reads every `tools/blender/forms_manifest_*.json`, adds `<meshid>: <class>` to `tools/relics/weapon_classes.json`
  (existing entries are never overwritten; a different class is reported) and runs the three syncs (`audit_weapon_scale.py --write`, `grip_table.py --write`, `scan_textures.py --fix`). `--no-sync` only edits the class table; `--selftest` proves it on a fake manifest in a temp copy.
* **Tests**: `FormMeshFilesTest` (every `weapon_meshes/*__*.txt` names a hero weapon and a non-base, non-Legendary-curse form of its forms file; skin and glow skin exist for every non-pull mesh; bow stages resolve),
  `FormModelsSourceScanTest` (the renderer hook: component read, setting, classic off, cached join, curse fallback after it; the component is registered, synced and written with the look; the setting is a client config default on).
  Self-test `heroforms.component` (prefix `heroforms`): a stand-in player's stack reads `base`, then the learned form, then `base` again; server-only code.
* **Cost** (rule 11): at most one string compare (`"base".equals`) plus two map reads per hero weapon per frame; no new packet (the component rides the normal item sync, a few bytes, only when the form changes);
  server work is one string compare per weapon stack inside the existing form refresh. Worst case memory: 256 weapon ids x 64 form ids of cached strings, cleared when exceeded.
* **Not done here**: the meshes themselves (art builders), the smoke list (`heroforms` must be added to `SMOKE_EXPECT` in smoke.yml by the coordinator), the changelog and the version.
