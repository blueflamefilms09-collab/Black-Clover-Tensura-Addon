# Hero weapon hold fit (4.45.0)

The shield frame fix (`ShieldFrame`, 4.44.1) keeps a shield from floating off the hand or passing through the arm. The same standard now holds for every hero weapon
(Legendary, Seven Star, Vassal, every form). Everything below is numeric: no game client was run, so nothing was looked at on screen.

## What was wrong
- A hero form mesh is `<item>__<form>`; there is no `models/item/<form>.json`, so `WeaponGrip.displays()` found no `display` block and put the grip at the centre of the item cube
  (about 0.22 blocks off the hand in third person) and first-person-placed every form against no display. Fix: `GripTable.modelIds` also tries the item before `__`.
- Seven mirror forms were held by the glass: class `shield` made the finder ignore the mesh's own `grip` line. Fix: per-form fractions, `HOLD_FIXES` in `grip_table.py`, written to
  `weapon_grips.json` `holdFractions`.
- A part behind the grip (book, carriage, scythe, long bows) passed through the torso or the forearm. Fix: `HoldFit` (pure), applied once per loaded mesh; it moves the weapon
  out of the torso (at most 0.20) and slides it along its axis or forward out of the forearm (at most 0.08 + 0.06), never further than the hand can still reach (float <= 0.10).

## Audit
`python tools/weapon_grip/hold_audit.py` (`--old` = the 4.44.x placement, `--all` = every weapon, `--write` = `hold_golden.json`). Per mesh, per context (third/first person, right/left hand):
FLOAT (grip further than 0.10 from the hand), ARM (weapon past 0.34 inside the forearm box), BODY (through the torso), NONE (cube pivot), DIRECTIVE (grip vs the mesh's own `grip`
line), LOW / CAMERA (first person). `HoldAuditTest` repeats the third-person numbers in Java and compares them with `hold_golden.json`.
Old 4.44.x: 34 of 127 hero meshes flagged. Now: 0. Armour stand and item display use the same display block, so the same numbers apply; bow draw states (`_pull1..3`) are judged for FLOAT only.

## Switches (look only)
Client config `models.holdFit` (`nusmp-heroweapon-client.toml`) or `-Danime_multiverse.weaponHold=off`: restores the 4.44.x placement (no form display, no hold move, no per-form fractions).

## Worst-case cost
Per hero mesh load: at most 60,000 surface samples, about 20 passes over them, two cached shifts (right, left). No packets, no per-frame work, no server code. The samples are dropped after the shifts are made.

## Not covered
Non-hero weapons keep their placement (`hold_audit.py --all` lists them as a follow-up). `vassal_gauntlet` and `vassal_claw` use HandWrap's own frame. Shields stay on `ShieldFrame`.
