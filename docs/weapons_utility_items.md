# Weapons no longer fight the hold-and-right-click items (4.89.4)

Owner report: the weapons of the mod (Vassal, Holy, Legendary, relic weapons) "counteract" items that are held and right-clicked, such as the Hourglass of the Final Hour
that carries you to the Lucius dimension.

Nobody could run Minecraft in the session. Everything below was found by reading the code and is covered by unit tests and a server self-test; none of it was seen in a game.

## The causes found

1. **The hourglass was read as a bow (the main cause).** `arena/ZogratisHourglassItem.java:89` answers `UseAnim.BOW` (the draw animation while the three-second channel
   runs). `heroweapon/rules/HeroRules.classOf` turned any `UseAnim.BOW` (also `BLOCK` and `SPEAR`) into a weapon class (`HeroRules.java`, the three `use ==` lines). A player bound to a
   Holy Weapon (Sword, Shield, Spear) is shocked by every weapon of another class, and a bow is a "fired" class, so:
   * `HeroRuleEvents.onUseStart` (`LivingEntityUseItemEvent.Start`) cancelled the start of the hourglass channel and shocked the player (damage, slowness, "rejects the weapon");
   * `onRightClick` (`RightClickItem`) cancelled the click the same way;
   * `onAttack` and `onPhysicalDamage` shocked a blow struck with it.

   The same animation is on `arc/arena/ChronicleSigilItem.java:127` (Chronicle Sigil) and `heroweapon/gem/CallingGemItem.java:65` (Calling Gem), so they failed the same way.
   Vassal and Seven Star wielders are not rejected by the rule (`HeroClass.rejectsOthers` is false for them); for them the cause is number 2.
2. **Hand order.** Vanilla tries the main hand and stops at the first hand that answers. A weapon of the mod in the main hand answers every use key (a hero technique, a guard, a draw:
   `HeroWeaponItem.use`, `RelicWeaponItem`, `MagicWeaponItem`, ...), so a hold item in the off hand never got its turn. Only food and drink had an exemption (`ConsumablePass`, 3.63.0).
3. **Better Combat (not changed, see "Not fixed").** `relics/djinn/BcHands.java` documents that Better Combat answers an EMPTY off hand behind a two-handed weapon (123 of the
   `weapon_attributes` files are `two_handed`). An hourglass in the off hand behind such a weapon cannot be used by anything in this mod.

Checked and found clean: the hourglass's own code (`ZogratisHourglassItem`, `HourglassRules`, `ArenaTravel`) has no condition on a weapon, on the inventory or on a hero bond (pinned by a test);
the summoned-weapon loan (`HeroLoan`) only occupies its own hotbar slot and never touches the hand, the cooldowns or another stack; the other `RightClickItem` / `Start` handlers of the mod
(`RelicEngine`, `SocketEngine`, `TierRitual`, `DarkDjinn`, `Wave0Hooks`, `Wave6`, `Wave7`, `Wave28`, `Wave29`, `TimeStop`, `JudgmentEvents`) act only on a sneak ritual with
a named relic, on a named state (slime form, beheaded, asleep, time-stopped) or only on food and drink; none reacts to "a weapon is carried". There is no client input handler that
cancels the use key.

## What changed

* `heroweapon/rules/UtilityItems.java` (new, pure): the rules. Name words, the tag exemption, "ignore the use animation of a utility", "the main hand yields".
* Tag `data/anime_multiverse/tags/item/hold_use_items.json` (new, data driven, a datapack may add to it).
* `HeroRules.classOf`: a tagged item is never a weapon; a utility (tag, or a name of this mod made of a utility word) that declares no weapon class, is not a relic and does not hit
  hard has its use animation ignored (a real weapon that only has a word like "seal" in its name keeps its class). `HeroRules.rejectedClass` checks the exemption first, so every
  cancel and shock in `HeroRuleEvents` (they all go through it) steps aside for a held utility. Also new: `utility`, `utilityTagged`, `weaponLike`.
* `heroweapon/rules/UtilityYieldEvents.java` (new): a `RightClickItem` listener at the highest priority. A weapon in the main hand, a ready (not on cooldown) utility in the off hand, not
  sneaking: the main-hand click is cancelled with PASS, so the game goes on to the off hand and the utility runs. The decision reads only things the client has too, so both sides agree.
* Setting `heroweapon.utilityItemsAlwaysWork` (server, `nusmp-heroweapon-server.toml`, default true). **Off restores the old behaviour exactly** (the tag and the name words are ignored, the
  animation counts as before, the weapon takes every click).
* Self-test `utilkeep.rules` (registered from `HeroCore`); unit tests `UtilityItemsTest`, `UtilityScanTest`.

## NEW / REPLACED / KEPT

* NEW: everything above. REPLACED: nothing is removed; the behaviour of `classOf` / `rejectedClass` is changed only behind the setting.
* KEPT: the Holy rejection (a Sword hero is still shocked by a bow, a shield, an axe), `ConsumablePass`, the Genesis exemption, the casting-relic exemption, every weapon attribute file.

## Items now protected

In the tag (explicit): Hourglass of the Final Hour, Chronicle Sigil, Calling Gem, Dragon Radar, the fourteen Dragon Balls, Dungeon Map, Grimoire, Grimoire Summons, Multiverse Wiki,
Compat Codex, Engraving Codex, Devil Contract, Release Crystal, Purification Charm, Shishou, Beast Will Core, Genesis Core, Mana Core, Shadow Core, Shadow Key, Magic Key, Sigil of
Rebirth, Sigil of the Second Book, Paint Brush, Paint Palette, Spirit Lord Skill, Magic Stone. By name (anime_multiverse items whose path has one of the words hourglass, sigil, radar,
book, grimoire, codex, wiki, map, seal, gem, crystal, charm, key, ball, contract, compass, scroll, ticket, summons, core, shard), for the dynamic ones: vassal seals, Djinn seals, rank cores,
charter grimoires, future items. The name words only switch off the use-animation reading; they never hide a sword, axe, mace or hero weapon.

## Worst-case cost

* Server: no new tick work, no entity, no packet, no allocation. Two tag reads and one registry lookup per right-click or use-start, a registry lookup only when the item has a
  bow/block/spear animation.
* Client: the yield listener runs on a right-click only (two tag reads, one cooldown read). No rendering.

## Not fixed / could not be seen

* Nothing was run in a game (no Minecraft here). The classification and the decisions are tested as pure code; `utilkeep.rules` runs in the CI server smoke test on stand-in stacks.
  The real hand order (the PASS making the client try the off hand) follows vanilla's `Minecraft.startUseItem` and NeoForge's `CommonHooks.onItemRightClick` as read, but was not observed.
* Better Combat: behind a two-handed weapon the off hand is empty to the game, so an hourglass in the off hand still cannot be used there; put it in the main hand (select its hotbar
  slot). Fixing that needs a Better Combat change or a mixin into it, not a data change, and was not done.
* A hero weapon that is summoned stands in the selected hotbar slot (the item that stood there is displaced until it is put away): to hold the hourglass, select another slot.
* While a weapon is in the main hand and a utility in the off hand, the weapon's own use key (guard, draw, technique) is given up to the utility; sneak + use still reaches the weapon.
  Turn the setting off to restore the old order.
* The hourglass itself still refuses in the Labyrinth and dungeons, mid-fight entry and within five seconds of a blow, by its own rules (unchanged).
