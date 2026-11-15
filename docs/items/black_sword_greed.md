# Black Sword Greed (Berserk of Gluttony) and Lord of Gluttonous Greed

Built from the owner's blueprint `berserk_of_gluttony_x_tensura_addon_implementation_blueprint.md`. One sword per world, eight forms (eight items, one active at a time), eight tiers that open the forms, and the Ultimate skill **Lord of Gluttonous Greed** that its wielder holds while the sword is bound.

**Nothing here has been seen in a running game.** The pure rules, the assets and the source rules are unit-tested; the Minecraft glue was written against the real API but was only compiled by CI; the server self-tests `greed.rules`, `greed.world`, `greed.forms` cover the world lock, the tiers and the forms.

## NEW / REPLACED / KEPT (rule 5)

| | what |
|---|---|
| NEW | everything about the sword: package `greed/` (rules, saved data `nusmp_greed`, attachment `greed`, components `greed_owner`, `greed_token`, 8 items `black_sword_greed`, `_bow`, `_scythe`, `_shield`, `_staff`, `_gauntlets`, `_spear`, `_dual_blades`, the skill `anime_multiverse:lord_of_gluttonous_greed` and its icon, the key `R` (form switch), the HUD layer `greed_hud`, the command `/multiverse greed ...`, the server config `nusmp-greed-server.toml`, 8 Better Combat / engraving rows, the Blender models, the lang keys |
| REPLACED | nothing |
| KEPT | everything else. Shared files only got appended lines: `NUSMP` (boot + client init), `MultiverseCommands`, `NUSkills`, `WeaponRenderer`, `NUCreativeTab`, `WeaponEngravings`, `en_us.json`, weapon-class tables, the grip golden, `smoke.yml` |

## The rules in plain words

- **One in the world.** `GreedData` stores spawned / token / owner / last-active. A bare stack from the creative tab adopts only if no sword exists; every other copy dissolves. The wielder is the owner; a thief's stack is torn away (it goes back to the wielder or, with no wielder, is released).
- **Bound to a soul.** Dying (not by a player) returns the sword at respawn. Killed by a player: the sword is **released** (the devoured stats are lost, a new token is minted, a fresh unowned stack drops where the wielder died). 14 days offline (config) also releases it.
- **Devouring.** A kill absorbs 15 % of the victim's EP (30 % in Half-Starvation) as max health / attack / armour (square-root growth, hard caps 200 / 60 / 30), restores satiation and stores EP. Stored EP opens tiers at 0, 2.5k, 10k, 35k, 100k, 300k, 750k, 2M; tier N opens form N.
- **Hunger.** Satiation (0..100) falls with the burden; below 20 the wielder starves (Night Vision, Speed II, slows weaker mobs; at 0 a fatal burden makes the sword abandon its wielder). Tier 7 (Level Zero) is never hungry. Half-Starvation (skill mode 2) doubles absorption and hunger.
- **Forms.** R cycles the open forms (cooldown 2 s, server-checked). Sword, Bow (lock-on hitscan), Scythe, Shield (absorbs into a pool, reflects), Staff (Black Flames cone), Gauntlets (threads / yank), Spear (thrust breaks shields), Dual Blades (blink strike). Every form has one **Secret Technique** (skill mode 1): it costs a share of the devoured stats (10 to 40 %).
- **Vault.** A kill steals a skill (always from a player, 35 % from a mob) into a vault of up to 200 entries; entries can be equipped, favourited or sacrificed for EP (`/multiverse greed vault`).
- **Soul Sever.** The sword's hit marks the target so it cannot heal (persistent NBT flag).

## Worst-case cost (rule 11)

- Server per tick: the per-player scan runs once per second (inventory only, no world scan); hunger and grant sync every 5th tick; the inactivity check every 6000 ticks (one saved-data read). Form actions are cooldown-gated (switch 40 ticks, technique 60 ticks) and hit at most 16 targets from one box query. No chunk loads, no unbounded spawns, no block edits.
- Per player: one attachment (a few numbers and a vault map of at most 200 ids); maps keyed by UUID are dropped on logout and server stop.
- Network: C2S `SwitchForm` (one int, validated 0..7 or next/prev, sender must hold the sword); S2C `Hud` (five small ints) only when the packed value changes; particles go through `PerfParticles` to nearby players only.
- Client: one HUD layer, a vignette drawn only while starving, no per-frame allocation; the models are static meshes through the existing `WeaponRenderer` budget.

## Not verified / honest caveats

- Not seen in a game: the HUD, the key, every form's use, the shield pool, the vault chat, the models in hand (preview renders were checked).
- The skill grant is synced by `GreedGrantService`; Tensura's skill screen was not opened.
- The Tensura engravings of each form are in `WeaponEngravings`; their effect with the real enchantments was not tried.
- Canon names (Bloody Ptarmigan Cross, Piercing Meteor, Soul Reap, Reflection Fortress, Twilight Healing, Dimension Destruction, Revolt Brionac, Infinity Divide) come from the owner's blueprint; no wiki page was fetched for them.
