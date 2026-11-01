## 3.48.0 - Devil Contract system

**New**
- Devil tiers (Low / Mid / High / Supreme) from the price of the contract (`/devilpact accept tax|seal|maxhp|eye_left|eye_right|half`).
- Assimilation key `N` (rebindable): lets the devil in or pushes it back, 0 to 100 %. The magicule drain grows with the level, damage / speed bonuses follow a smooth curve, wings give flight from 50 %.
- Devil Union at 100 %: exactly five minutes, then the level drops to 0 and the union is locked for 24 in-game hours. It excludes the old Anti-Magic union (one devil at a time).
- Weg markings by tier (wide and slim skins), horns from 20 %, wings from 50 %, a glow in the union, a movable HUD panel.
- Prices that last through death: half your maximum health, or a lost eye (darker screen edge, narrower view).
- `/devil status|grant|level|sacrifice|cooldown|clear` for admins; `nusmp-devil-server.toml` and `nusmp-devil-client.toml`.

**Kept**
- The three old prices and their effects, the original devils and pages, the Anti-Magic Black Form, everything else.

**Known issues**
- Wing and horn shapes and the balance of the numbers have not been seen in a running game yet.
- The devil attribute has no combat effect yet, the first-person arm is not marked, a dimension change does not end the union.
- Key `N` shares its default with the Hero weapon cycle key; rebind one of them.

**Update note:** remove any older `multiverse-of-anime-*.jar` from `mods` first. Install on both client and server.

*Earlier in the 3.4x series:* Beast Will (3.45), Nen and a ten-mode All Fiction (3.46), Gate of Babylon and Ki (3.47), Shishou rework and the Reset Scroll guard (3.42-3.43).
