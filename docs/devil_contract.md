# Devil Contract system (3.48.0)

What was asked: the owner's file `claude_prompt_devil_system.md` (tiers, an assimilation key with a scaling magicule drain, stat and flight bonuses, a strict five minute Devil Union with a 24 in-game hour cooldown, the Weg markings with horns and wings, and sacrifices that stay through death). The plan and the new-versus-existing comparison are in `docs/devil_contract_guide.md`; the owner answered "go" and this is what was built. Wiki lookups were not possible from the build session (rule 10: the English and Japanese wiki hosts are not reachable), so no canon text is claimed; the names Low / Mid / High / Supreme, Weg and Devil Union are used as the owner's file uses them.

## How it is used

1. **Get a contract.** Nothing about the old way changes: the Devil Contract item and `/devilpact accept <price>` bind one of the four original devils (see `book/ForbiddenMagic`). The price now also picks the tier: `tax` Low, `seal` Mid, `maxhp` (two hearts) Mid, `eye_left` / `eye_right` High, `half` Supreme. The devil's magic picks the attribute (Gravity, Spatial, Curse, Anti-Magic, Flame, Word, Time). The old prices act exactly as before; the new ones are `half` (half the maximum health for good) and `eye_left` / `eye_right` (a lost eye, for good).
2. **The key.** `N` by default (Controls, category "Multiverse", `Devil: let in / push back`; it shares its default with the Hero weapon cycle key, rebind either). One press lets the devil in (assimilation climbs), the next pushes it back (it falls). It only asks: the server decides.
3. **The level** climbs 1 / 1.5 / 2 / 2.5 percent a second for Low / Mid / High / Supreme (a Low devil needs 100 s, a Supreme one 40 s) and falls 4 percent a second when switched off. Every second it costs magicules: the base of the tier (10 / 20 / 35 / 60) times `1 + 9 x^2` with x the level, so the cost at 100 % is ten times the cost at 0 %. When the magicules run out the devil withdraws.
4. **What the body shows:** from 5 % the Weg markings of the tier (translucent, fainter at low levels, under any armour), from 20 % horns, from 50 % wings and creative-style flight (the wings are spread and beating in the air, folded on the ground), at 100 % the Devil Union.
5. **The Devil Union** begins by itself at 100 %: exactly five minutes (an absolute game time stored with the player, so logging out does not stretch it), stats times 1.5, the drain a quarter of the cost at 100 %. It ends at the five minutes (or when the magicules run out, or on death), the level falls to 0, and the union is locked for 24 in-game hours (24 000 ticks, counted in game time: sleeping does not shorten it). While the lock runs the level stops at 99 %. Only one devil at a time: while the old Anti-Magic Black Form union is on, this key says so, and while this union is on the Black Form does not answer.
6. **Stats** (added to the player, hard caps in the rules): attack damage 2 / 4 / 7 / 12, movement speed +10 / 20 / 30 / 45 percent, each growing along a smooth curve from 0 at 0 % to the full bonus at 100 %, 1.5 times that in the union, never past 20 damage or +75 % speed whatever the settings.
7. **Sacrifices** are stored in the contract, copied to the new body on death and applied again on login and respawn. Half health is a permanent -50 % maximum health modifier. A lost eye darkens that side of the screen and narrows the field of view (client setting `eyeOverlayStrength`, 0 turns both off) and, when the server setting is on, shortens the reach by up to 0.75 blocks. Nothing removes them except `/devil sacrifice <player> remove <name>`.
8. **Commands.** `/devil status` for anybody; for operators (permission 2): `status <player>`, `grant <player> <tier> [attribute]`, `level <player> <0-100>`, `sacrifice <player> add|remove <half_health|lost_eye_left|lost_eye_right>`, `cooldown <player> reset`, `clear <player>`.

## Settings

Server (`serverconfig/nusmp-devil-server.toml`, section `devil`; `enabled = false` restores the game as it was: the key does nothing, no stat is added, a level in progress ends, the new sacrifices have no effect but stay on record): `union.unionSeconds` (300, 20 to 3600), `union.cooldownHours` (24, 0 to 240), `union.deathStartsCooldown` (true), `balance.drainScale` (1.0), `balance.statScale` (1.0), `balance.allowFlight` (true), `sacrifices.halfHealth` (true), `sacrifices.eyeReachPenalty` (true). Client (`config/nusmp-devil-client.toml`): `wegLayer`, `hud`, `eyeOverlayStrength`. The HUD panel is movable with GUI Move (id `devil_hud`).

## Network (rule 9, rule 11)

Three small optional packets. `Toggle` (client to server, empty; the server checks the sender is a real player with a contract, that the system is on, and that the last ask was at least 4 ticks ago). `Sync` (server to the owner only: tier, attribute, level, flags, sacrifice mask, seconds left of the union and of the cooldown, which the client counts down itself), sent only when something other than those counters changed: at most once a second while the level moves. `Visual` (server to the players who can see the devil: entity, tier, level in steps of five, union flag), sent only when the tier, the step or the union changes and once to a player who starts seeing the devil. All numbers read from the network are clamped. A client without the mod simply never gets them.

## Worst-case cost (rule 11)

- Server: for a player with no contract every handler is one `hasData` lookup. For a contracted player the once-a-second step is a few additions, at most one magicule payment and at most three attribute writes (each only when its value changed). No entity is spawned, no world is scanned, no chunk is loaded, nothing is kept in a static map.
- Packets: at most one `Sync` a second to the owner; `Visual` at most 20 times in a climb from 0 to 100 (a Low devil: 20 packets in 100 s) plus one per union change, to nearby players only.
- Client draw: one body copy for the markings (about 6 boxes), horns 6 boxes from 20 %, wings about 24 boxes (two bones, three fingers and three membrane panels per side) from 50 %, and the glow pass on the same parts in the union: under 100 boxes, about 600 quads, per devil. Players further than 32 blocks are skipped and at most 16 other players are drawn a frame; the owner is always drawn. No allocation per frame; section `c.devil.layer` of the performance probe times the layer. The eye shade is at most 32 fills a frame. The HUD is one panel of about 30 fills, with its text rebuilt once a second.
- Memory: the client keeps one small object per devil in view (at most 256), emptied on logout and swept every five seconds.

## NEW / REPLACED / KEPT

NEW: the whole `devil` package, the key, the HUD panel, the union and its cooldown, the stat and flight bonuses, the markings / horns / wings, the three new prices and the two sacrifices, `/devil`, both settings files. REPLACED: nothing. KEPT: every old price and page of the Forbidden magic, the Anti-Magic Black Form with its own 5 minute / 30 minute rule (the two unions exclude each other), the Beast Will assimilation (a different system), everything else.

## Not included, and where it differs from the guide

- **The attribute has no combat effect yet.** It is stored, shown and synced, but the devils' own attacks were not part of this release.
- **A dimension change does not end the union.** The guide said it would; since the end is an absolute game time nothing can refresh it, and ending it on a portal would punish ordinary travel. Logout and death still end it (the cooldown starts).
- **The first-person arm** is not marked (only the third-person body, your own in the third-person camera and everybody else's).
- **The textures and the horn / wing models are generated placeholders** (`tools/gen_devil_textures.py`, preview sheets in `docs/previews/devil/`). The textures are plain PNGs: a hand-painted or Blockbench replacement only has to keep the file names and sizes (`weg_<tier>[_slim][_glow].png` 64x64 with the marks on the base layer, `horns.png` 32x32, `wings.png` and `wings_glow.png` 64x64 with the bone material on the left half and the membrane on the right half). The horn and wing shapes are in `client/devil/DevilModels.java`.
- **Not seen in a running game by its author:** the wing and horn shapes and how the markings sit on a real skin were only checked against the box geometry and the flat texture sheets; the balance of the numbers is a proposal.
- The old "two hearts" price was not checked across death in a game.

## Tests

`DevilRulesTest` (every number and cap), `DevilStoreTagTest` (the tag format, clamping on read, enums by name), `DevilSourceScanTest` (no client class in the common package, no static player or collection, no world scan, packets checked and rate limited, server versus client config, the layer bounded, the textures exist), self-tests `devil.contract.registry`, `devil.rules.bounds`, `devil.store.roundtrip` (prefix `devil` in the CI smoke list).
