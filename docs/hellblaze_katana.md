# Hellblaze, The Demon Sin as a katana

Owner request: "rework the sword called Hellblaze, The Demon Sin into a katana with a sheathe on the other hand, black and dark red. also have it get a purplish tone every hit."

Item: `hellblaze_the_demon_sin` (`relics/wave/Wave71.java`). Only the LOOK changes: the damage row (God longsword), the abilities, the engravings and the item id are untouched.

## What you get

| Part | What it is | Where |
|---|---|---|
| Katana | Curved single-edge blade, dark-red hamon and edge glow, black wrap with dark-red thread, eight-thorn tsuba, "Sin's eye" menuki, glowing kashira gem, sageo. Black steel, dark-red glow (`A8142A`). Same hand spot and the same 1.72 block length in hand as the old broadsword. | `weapon_meshes/up/hellblaze_the_demon_sin__katana.txt` (+ `_lod`), skins `textures/entity/weapon/up/..__katana*.png` |
| Sheath (saya) | Black lacquer, follows the curve, glowing red lines, koiguchi, kojiri, kurikata, sageo. Drawn in the OFF-HAND only when that hand is empty, in first person and third person. Purely visual: no item, no slot, no packet. | `..__saya.txt`, `client/HellblazeSayaLayer.java` |
| Purple on every hit | The glow, the veins and the halo shift from dark red to purple (150, 60, 200) when the wielder lands a hit and fall back to red over 10 ticks (about 0.5 s). | `client/upgrade/HellblazeHits.java`, `HellblazeTone.java`, `WeaponRenderer`, `WeaponUpgrade` |
| Katana swing | Better Combat `katana` preset, one-handed, the longsword's reach (+0.25). | `data/anime_multiverse/weapon_attributes/hellblaze_the_demon_sin.json` |
| Setting | `visual.hellblazeKatana` in `config/nusmp-visual-upgrade-god-client.toml` (CLIENT config, this machine only), default `true`. `false` = the old black jagged broadsword with its orange fire, exactly as before. | `UpgradeConfig` |

## NEW / REPLACED / KEPT (rule 5)

- NEW: the katana meshes, skins and LOD; the sheath mesh, skins and layer; the hit-tone watcher; the `hellblazeKatana` switch; the `katana` block in the spec; the `hellblaze_katana` Better Combat profile; the two pure/asset tests; `tools/blender/hellblaze_katana.py`, `tools/hellblaze/make_spec.py`.
- REPLACED (by default, switchable back): the in-hand look of Hellblaze (broadsword -> katana); its Better Combat swing (longsword -> katana); the description text (`tools/lang/hellblaze_katana.json`, replaces only `item.anime_multiverse.hellblaze_the_demon_sin.desc`).
- KEPT: the classic meshes, skins, LOD, glow, veins, the inventory icon and sprite, the classic fields of the spec, the classic recipes and act ids (the katana block keeps the same act ids, so the server's signatures are unchanged), the class `longsword` (damage row, size band), the engravings (`elemental_boost@3`, `severance`, `ruination`, `energy_steal`; severance is a katana's cut, so none were added).

## How it is read

- Spec: `visual_upgrade/hellblaze_the_demon_sin.json` has the classic fields as before plus one block `"katana"`. `UpgradeSpec.merge` lays it over the classic spec (objects key by key, arrays and numbers replaced). `UpgradeSpecs.get` returns the merged spec while the switch is on. Flipping the switch in the game drops the cached meshes, so it applies without a restart.
- The glow and veins skins are grey masks; the colour is the `glow` line of the mesh and the spec, so the purple flash needs no second texture.
- Hit detection is client-side only: for a wielder that swung in the last 12 ticks, look for a victim in a box (reach 5) whose `hurtTime` just started and whose last damage source is that wielder. The tone is keyed by player UUID (8 slots) and read by the renderer through `HolderTracker`.

## Worst-case cost (rule 11)

- Packets: 0 new. All server rules are unchanged (the game state is decided as before; the client only draws).
- Katana draw: at most 1190 quad lines (body + glow), the low-detail mesh 223 (picked at 12 blocks, glow off at 24). The upgrade passes are the existing ones (skin, glow, veins, halo), run through the existing perf budgets.
- Sheath: 496 quad lines per wielder, only within 32 blocks, only if the off hand is empty and the main hand holds Hellblaze, hard cap `HellblazeSayaLayer.QUADS = 600`; far players are skipped, invisible and spectating players too.
- Hit watcher: per tick, only for players that swung within 12 ticks and are within 48 blocks: one box query (reach 5) per such player; at most 8 tone slots (one per player UUID); a slot is freed when its player stops holding Hellblaze or leaves the range, and everything is cleared when the world is left. The box query allocates one small list, only on ticks where that player is inside the 12-tick swing window.
- Draw path: precomputed quaternions for the sheath; the tone read is an array lookup with no allocation.

## Made in Blender

Made in Blender through `tools/blender/bl_lib.py`: the katana (9 parts), the low-detail katana, the sheath (5 parts). Previews (Blender renders): `docs/hellblaze/previews/katana_blender.png`, `saya_blender.png`. Re-bake: `python tools/blender/hellblaze_katana.py` (`--only katana,lod,saya`, `--fast`, `--trial DIR`).

## Tests

`HellblazeToneTest` (the tone curve and the blend), `HellblazeKatanaAssetsTest` (files exist and fit their sizes, classic files kept, spec overlay, switch default, the sheath and the watcher are client-only with no packet, Better Combat parent, engravings). Refreshed: `tools/weapon_grip/grip_golden.json`, `tools/bettercombat/manifest.json`, the weapon attribute file.

## Not checked (honest)

- Nothing here has been seen in a running game. Only the Blender renders were looked at.
- The sheath poses (position and angle in the arm model space for third person and in the camera space for first person) are computed, not tuned by eye; they may need a nudge.
- The hit watcher relies on `hurtTime` / `lastDamageSource` timing and on the swing flag; a hit on a target with hurt immunity (no `hurtTime` reset) may not flash.
- The description text says "katana" even with the switch off.
- The in-hand preview tools (`tools/item_preview`) were not run in this session; the grip is the old Hellblaze's (`grip 0.150`), so the hand should sit as before.
