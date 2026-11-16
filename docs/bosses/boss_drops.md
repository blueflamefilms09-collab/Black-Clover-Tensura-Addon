# Boss drops (the drop framework, the trophies, the Trophy Plate, Lucifero's armour)

Owner request: every boss of the mod should drop something besides its grimoire, and Lucifero's armour should drop. This is the framework that does it. Nothing here was played in a game;
what is proved is proved by unit tests, source scans and the CI server self-tests (`bossdrops.*`).

## 1. NEW / REPLACED / KEPT

| | What |
| --- | --- |
| NEW | The data table `data/anime_multiverse/boss_drops/<boss_id>.json` (23 files) and the one hook that pays it (`bossdrops/BossDropEvents`). |
| NEW | 23 trophy items `trophy_<boss>`: one relic shape modelled in Blender (`tools/blender/boss_trophy.py`), recoloured per boss, a small solid object in hand. |
| NEW | The Trophy Plate armour (4 pieces, shapeless recipes made of any boss trophies, item tag `anime_multiverse:boss_trophies`). |
| NEW | Lucifero's armour: `lucifero_helmet`, `lucifero_chestplate`, `lucifero_leggings`, `lucifero_boots` (drop only). |
| NEW | Server config `nusmp-bossdrops-server.toml` (`[bossdrops]`: `enabled`, `chanceScale`, `maxStacksPerKill`), a per-player first-kill ledger (`nusmp_bossdrops` saved data), self-tests `bossdrops.items`, `bossdrops.tables`, `bossdrops.pay`. |
| REPLACED | Nothing. No boss, drop, item, texture or model was removed or changed. |
| KEPT | The Charter Grimoire drop (`CharterEvents`, its world lock, its chance), the dungeon loot modifier and every boss's own loot table. The new rows are paid on top, by a separate hook. `bossdrops.enabled = false` gives exactly the old drops. |

## 2. How a kill is paid

1. A boss of this mod dies (`LivingDeathEvent`, lowest priority, so a death another mod cancelled is not a kill). The boss id is the Charter id when the entity has one (`CharterTable.byEntity`, so `vanica_zogratis` is `vanica`), else the registry path.
2. The table of that id is read from `BossDropData` (loaded on every data reload, so `/reload` and a data pack work; a bad file is logged and skipped).
3. The killer is the player of the last blow (`LootVictims.killer`: the attacker, else the player the game credits). A creative or spectator killer gets nothing. Nobody else is paid.
4. `BossDropTable.roll` rolls the rows in file order and stops at the table's `max_stacks` (and at the config's `maxStacksPerKill`).
5. The items go into the killer's inventory, the rest drops at their feet (like the Charter book).

### The file

```json
{ "boss": "meliodas", "max_stacks": 6,
  "drops": [
    { "item": "anime_multiverse:trophy_meliodas", "chance": 1.0, "min": 1, "max": 2, "first_kill_only": false, "killer_only": true },
    { "item": "tensura:silver_coin", "chance": 1.0, "min": 10, "max": 24 },
    { "item": "tensura:gold_coin", "chance": 1.0, "min": 2, "max": 2, "first_kill_only": true } ] }
```

Rules checked on load (and by `BossDropTableTest`): a boss id of `[a-z0-9_]`, 1 to 12 rows, a lower-case item id, `0 < chance <= 1`, `1 <= min <= max <= 64`, `max_stacks` 1 to 16 (default 6),
`killer_only` may be written but never as false. `first_kill_only` means that player's first kill of that boss (kept per player UUID in saved data, never "the current player").

## 3. What each boss pays

Every one of the 23 grimoire bosses pays: its trophy (always, 1 to 2), Tensura silver coins (always) and a Tensura gold coin on each player's first kill of it. The coin amounts follow three tiers:

| Tier | Bosses | Silver coins | First-kill gold | Extra gold chance |
| --- | --- | --- | --- | --- |
| lesser | minotaur, black_goliath, amphisbaena, udaeus, vetto, mars, revchi_salik, lotus_whomalt, heath_grice, morris_libardirt | 3 to 8 | 1 | none |
| mid | ban, diane, king, gowther, rades_spirito, patolli, fana, rhya | 6 to 14 | 1 | 10% |
| high | meliodas, escanor, merlin, vanica | 10 to 24 | 2 | 25% |
| Lucifero | lucifero | 20 to 40 | 2 to 3 | none (his armour is the extra) |

The tiers are a judgement call by the builder (no boss list of grades was available for all 23), so they are one table in `tools/gen_boss_drop_assets.py` and the owner can move a boss to another tier by editing its JSON.

### Lucifero's armour

Each of his four pieces is its own row with a 25% chance (about one piece a kill, never a guaranteed set), plus a second row of 4% for a second copy. His table has 11 rows and `max_stacks` 10.
The set is armour 4 / 8 / 6 / 3 (21 points), toughness 3.5, knockback resistance 0.05, enchantability 30, durability factor 40: above netherite (20 points, toughness 3) and under the Boss Core Aegis (24, toughness 4).
It is an ordinary armour item with a vanilla layer texture (violet-black plate with gold ripples), the same way the Boss Core armour is made; there is no custom worn 3D model (see 6).

### The trophies and the Trophy Plate

A trophy is a crafting material (stack 16, rare). Four Trophy Plate pieces are shapeless recipes of 5 / 8 / 7 / 4 trophies of any bosses (the tag `boss_trophies`): armour 3 / 6 / 5 / 2 (16 points,
toughness 1), between iron and diamond, repaired with a trophy. One shape for every boss keeps the recipe family small; the set is the same whichever bosses you farmed.

The 23 trophies (names are invented from each boss's canon power, colours from each boss's own sheet; `tools/boss_drops/bosses.json`): Meliodas Hellblaze Ember, Escanor Sunstone of Pride, Ban Fox's Hunt Fang,
Diane Giant's Heart Stone, King Spirit Spear Seed, Gowther Goat's Dream Charm, Merlin Fragment of Infinity, Fana Third Eye Ember, Rhya Rooted Dirge Seed, Rades Soul Thrall Gem, Patolli Loaded Die Shard,
Heath Grice Glacier Bloom Crystal, Lotus Whomalt Poison Lotus Pearl, Mars Crystal Weapon Core, Revchi Salik Storm Anvil Link, Vetto Bound Hail Crystal, Minotaur Labyrinth Bull Horn Crest,
Black Goliath Dungeon Wrath Core, Amphisbaena Twin-Head Water Scale, Udaeus White Palace Bone Seal, Morris Libardirt Scholar's Modified Specimen, Lucifero Heart of Gravity, Vanica Curse-Warding Blood Pearl.
Preview of the icons and armour: `docs/bosses/previews/boss_drops_icons.png`; the neutral relic as Blender drew it: `tools/boss_drops/trophy_relic_preview.png`.

## 4. Cost, worst case (rule 11)

| What | Worst case |
| --- | --- |
| Server work per kill | One table lookup, one roll of at most 12 rows, at most `max_stacks` (16) stacks and `MAX_PIECES` (32) item pieces; at most one saved-data lookup (first-kill row). |
| Server work per tick | None. There is no tick or timer; the framework only reacts to a boss death. |
| Network | 0 packets of its own (items go through the vanilla inventory sync). |
| Memory | 23 small tables, replaced as one immutable map on reload; the ledger holds at most players x 23 short strings. |
| Client | 23 small meshes (267 quads each, 202 body + 65 glow) drawn only while held or dropped, through the existing weapon renderer's budgets. |
| Multi-player | State is per player UUID; the killer alone is paid; creative and spectator killers are never paid. |

## 5. Tests

* `BossDropTableTest` (pure): good file, 15 bad files refused, chance / count / first-kill / cap, counts inside their range over 5000 rolls, the piece budget.
* `BossDropAssetsTest` (reads the repo): the catalog equals `tools/boss_drops/bosses.json`; every table valid, named after its boss, pays its trophy, names only models that exist (or the two Tensura coins); Lucifero's four pieces are rare (about one a kill); trophy mesh winding, skins, icons, models, hand class in both `weapon_classes.json`; tag and recipes; armour files; the lang file; no client class and no tick event in the package; defaults of the config; the self-tests are in the smoke list.
* CI self-tests `bossdrops.items`, `bossdrops.tables`, `bossdrops.pay` (a stand-in player is paid with a rigged dice; the first-kill row pays once; the caps hold).

## 6. What was not done, and what is uncertain

* **Per-boss themed equipment is not built.** Each boss pays a trophy and coins; the one equipment family made from trophies is the generic Trophy Plate, and Lucifero is the only boss with armour of his own. A set per boss (23 x 4 pieces with their own textures and models) was too much for one change and would need the owner's design call.
* **Only the last-blow killer is paid, not the top damager.** The Charter drop works the same way. A shared-damage ledger would be per-hit state on every boss; it was left out on purpose (rule 11).
* **Not routed through the dungeon loot modifier (`anime_loot`).** The rows are paid straight to the killer like the Charter book (a boss that clears its loot list does not lose them); the owner's "read by one small loot hook that works with the existing path" is met by the one `LivingDeathEvent` hook, not by a loot-table modifier.
* **The armour does not render a custom 3D model when worn.** It uses vanilla layer textures like the Boss Core armour (rule 14: not made in Blender, not possible without a new armour renderer). The item icons are 2D sprites, kept as the inventory icon (rule 7).
* The trophy relic is ONE neutral Blender model; per-boss shapes are not modelled. The per-boss look is the tint (crystal colour, accent-tinted metal, glow colour).
* The Tensura coin ids (`tensura:silver_coin`, `tensura:gold_coin`) are the ones the mod's own `CoinKind` uses; the CI self-test `bossdrops.tables` resolves them in the real registry.
* The lang keys are in `tools/lang/boss_lucifero_drops.json` and still have to be merged into `en_us.json` by the release step (until then an item shows its key as its name); the item tooltips carry an English fallback in code.
* Wiki research: no wiki was fetched for this change; the trophy names are invented.
* Nothing was seen in a game; the unit tests and Gradle were not run in this session (CI is the compiler).
* Decision for the owner: the 23 grimoire bosses are the ones listed above. The Zogratis siblings with their own Charter ids (Dante, Zenon, Lucius, the Devil Avatar, Goliath, the Charybdis Spawn) are not in the catalog; adding one is a line in `BossDropCatalog`, a row in `tools/boss_drops/bosses.json` and a rerun of `tools/gen_boss_drop_assets.py`.
