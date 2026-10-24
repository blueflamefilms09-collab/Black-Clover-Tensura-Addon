# Companion system: audit of what the mod already has (before building)

Request: build OUR OWN companion system (owner chose "build our own version": no dependency on `tensuranoindex`), modelled on the other mod's
`COMPANION_API.md`: talk screen with four tabs, gifts with favourites and hates, high five, stances, a pouch, following through dimension changes, speech
bubbles, voice lines, and the rule that nobody attacks a companion. Everything lives under `com.newuniverse.nusmp.companion` (client half under
`client/companion`).

Rule 5 verdict first: **every feature of the request is NEW** (nothing in the mod does the same job), but **seven existing pieces are reused** and one
existing character entity becomes the sample companion. Nothing is replaced, nothing is removed (rule 2).

Legend: EXISTS = same feature already here, PARTLY = a neighbour of it exists, MISSING = nothing like it.

## 1. Systems the request touches

| Area | What the mod has | Verdict | Decision |
|---|---|---|---|
| Friendly characters of the arcs (`arc/ArcNpcEntity`, `ArcRoster`, `BlackBullsNpcs`, `*Cast`, `DanMachiNpcs`) | ONE entity type `nusmp:arc_npc`, identity = synced character id (about 30 characters: Yami, Asta, Noelle, Charmy, Mimosa ...). Draws through `client/arc/ArcNpcRenderer` (art-kit model by id, talk clip), stays at a post, never despawns, invulnerable unless `arc.npcsMortal`, name tag in the character colour. No owner, no follow, no stance, no gifts, no screen. | PARTLY (the people exist, the relationship does not) | **Reuse as the sample companion**: a small subclass `ArcCompanionEntity extends ArcNpcEntity` registered additively as `nusmp:arc_companion`, character **Charmy Pappitson** (Cotton and Food Mage, an existing roster NPC: the canon glutton, so favourites are natural). Reuses her model, renderer class, name and the `beginTalk` clip. The 30 shared NPCs are not touched. |
| Arc dialogue (`arc/dialogue/ArcDialogue`, `DialogueBook`, `TalkPlan`, `DialogueSender`) | Story-aware lines (first meeting, quest stage, farewell) sent as COLOURED CHAT MESSAGES, started by a right click on `arc_npc`. Cooldown `arc.talkCooldownTicks`. Shops hook in with `ShopHook.tryOpen` (sneak + right click on the Old Witch). | EXISTS for chat talk, MISSING for a screen | Kept as is. The companion's right click opens the new talk screen instead; the old `arc_npc` keeps its chat talk. |
| Talk / dialogue SCREEN with options | None for NPCs. Screens that exist: `AvatarActionScreen` (a few buttons, opened by `S2C_OpenActionMenuPayload`), `client/skilltree/shop/ShopScreen` (Black Market), `client/elite/hub/QuestScreen`, `ShopScreen`. Theme kit `client/ui/theme` (`UiPanel`, `UiDraw`, `UiBar`, `ThemedButton`) and GUI Move. | MISSING | New `TalkScreen` (4 tabs). Reuses `UiPanel` / `UiTheme` colours, and the "pure layout + painter over a tiny Canvas, previewed headless" pattern of `client/relics/djinn/gui` + `tools/gui/DjinnGuiPreview.java`. |
| Speech bubbles over a head | None. Other speech: Shishou -> action bar (`ShishouSpeech`), Avatar barks -> chat (`AvatarChatterEngine`, `C2S_BarkRequestPayload`), arc bosses -> chat, `Voice` -> sound only. `book/BubbleArts` is Bubble Magic (a spell), unrelated. | MISSING | New `Bubble.say` (S2C packet to trackers, throttled) + client-only renderer. |
| Voice / sound pipeline | `audio/voice`: `Voice.say / sayNear(id)` over a fixed `VoiceCatalog` (120 lines + Gnome, Sylph, Undine, Shishou lists), `VoiceLimiter` (per player UUID cooldown + per-second cap), `VoiceNet.Say`, client `VoiceClient`, config `nusmp-voice-server.toml`. Per-creature sets `MobVoice`, `SpiritLightVoice`, `ShishouVoiceLines` (catalogue only, NO audio recorded yet). No companion has audio. | EXISTS (generic pipeline), MISSING (a hook from a chat line key to a sound) | `VoiceLines.register(langKey, soundEvent)` + `play(...)` is a REAL small hook (not only a stub): a line key maps to a SoundEvent supplier, or to an existing `VoiceCatalog` id (then it goes through `Voice.sayNear`, reusing its limiter and range). No audio is generated: no companion line has a recording, so `play` answers false until one is registered (the sample registers none and says so). |
| Followers / minions | `avatar/AvatarEntity` (TamableAnimal, owner = creator, Stance FOLLOW/STAY/DELVE, party manager `AvatarPartyManager`, `AvatarFollowCreatorGoal`, betrayal), `shishou/ShishouEntity` (floating sword, owner UUID, recall), `entity/SpiritLordEntity` (+Wind/Elemental, TamableAnimal with `FollowOwnerGoal`), `corpse/*` Soul Corpse summons (`NamedCorpseEntity`, ledger, pacts, caps), `book/GrimoireSummon`, `GrimoireDaemonEntity`. | PARTLY (each has an owner and follows, each in its own way) | Companions are a separate, simpler relationship (an NPC that WALKS WITH you and can be asked to wait). They do not touch these. Their follow goal and caps are modelled on the Avatar's (owner UUID, teleport when too far). |
| Stances | `avatar/Stance` FOLLOW/STAY/DELVE = a movement order. No fighting stance anywhere. | PARTLY (different meaning) | New `companion.bond.Stance` GUARD / AGGRESSIVE / PASSIVE (a different enum in another package, the Avatar's is untouched). |
| Friend / foe rule | `avatar/goal/AvatarTargeting.canHarm` (Avatar only: creator, creator's pets and Avatars, squadmates, creative players), `avatar/FtbTeamHelper.sameTeam` (FTB team or `Squads` party, soft dependency by reflection), `LivingEntity.isAlliedTo` used in many spells. No general "this is a companion, nobody hurts it". | PARTLY | New `Bond.foe(companion, owner, candidate)`: hostile things, and whoever the player struck or was struck by; never a player, another companion (`Companion` / `Talkable`), a tamed pet of the owner, a squadmate (**reuses `FtbTeamHelper.sameTeam`**). A damage guard (`LivingIncomingDamageEvent`) stops mobs targeting and players hitting companions of the same owner. |
| FTB squads | `multiverse/Squads` (Magic Knight squads on FTB Teams, reflective), `quests/QuestTeams`. | EXISTS | Reused only for "is a squadmate" in `Bond.foe`. |
| Following through dimensions | `avatar/betrayal/BetrayalDimensionHandler.teleportNear` (finds a standable spot 2-4 blocks from the target, `Entity.changeDimension(new DimensionTransition(...))`, re-creates the entity from NBT, uses a 300-tick portal chunk ticket for unloaded ones), `AvatarDelve` (the same call). Avatar only. | PARTLY (the technique exists, not general) | New `CompanionTravel` for any `PortalFollower`: **same technique** (`changeDimension` + `DimensionTransition`, same spot search idea, no permanent chunk ticket), driven by `EntityTravelToDimensionEvent` (collect) + `PlayerChangedDimensionEvent` (place). Bounded (see the costs section of the doc). |
| Gifts, favourites, rapport | `book/SpiritBond`: spirit trust 0-100 from EVENTS with per-trait likes/dislikes in the player's persistent data. `avatar/AvatarPersonality`: traits. No item gifts, no tiers. | PARTLY (the likes / dislikes idea) | New `Favourites` registry (loves / accepts / hates by item), rapport tiers, gift rules. Rapport is kept in the player's persistent data (survives the companion dying), as the API doc does. |
| High five / social pose | None. | MISSING | New (a synced "social kind" + start time on the entity; the sample poses by reading it, see doc). |
| Pouch (9 slots, uses a healing item on you) | `shishou/ShishouStorage` (his stomach), `avatar/inventory` (Avatar inventory screen). Different jobs. | PARTLY | New `PersonState.pouch` (9 slots, saved as NBT) and `Bond.tick`. |
| Server config / self-tests / perf | `ArcConfig` pattern (ModConfigSpec, `get` with default before load), `labyrinth/mega/SelfTests` registry (CI smoke test, `FakePlayer` stand-ins), `perf/PerfBudget`, `BoundedCache`, `PerfProbe`; tests `ServerSafetyTest`, `ConfigFileNamesTest`, `PerfSourceScanTest`, `LangKeys` style tests. | EXISTS | All reused. New self-tests use the prefix `magic.companions`. |
| Packet validation helpers | `avatar/net/NetText.clean` (strip control chars, cut), `avatar/net/RollLimiter` (per UUID gap), `ShishouNet` (client hooks set by client class, no client class named in common code). | EXISTS | **Reused**: `NetText` and `RollLimiter`; the `volatile Consumer` hook pattern so common code never names a client class. |

## 2. What is reused, in one list

1. `ArcNpcEntity` + `ArcNpcRenderer` + the roster record of Charmy Pappitson (the sample companion's body, model, name, talk clip).
2. `UiPanel` / `UiTheme` palette (talk screen look) and the Canvas-preview pattern (`tools/gui`).
3. `FtbTeamHelper.sameTeam` (squadmates are never foes).
4. `BetrayalDimensionHandler`'s technique for `changeDimension` + spot search (copied idea, because that class is Avatar-typed and private).
5. `Voice` / `VoiceCatalog` / `VoiceLimiter` as one backend of `VoiceLines`.
6. `NetText`, `RollLimiter` for packet validation.
7. `ArcConfig` / `SelfTests` / `PerfBudget` / `BoundedCache` patterns.

## 3. What is missing and gets built (all NEW)

`Talkable`, `Companion`, `PersonState`, `Stance`, `Favourites`, `Bond` (options / choose / tick / foe), `Talk` (+ packets open / choose / close with
validation and per-player rate limit), `TalkView`, `Bubble` (+ client renderer), `PortalFollower` + `CompanionTravel`, `Panelable` (optional), `VoiceLines`,
`CompanionConfig` (server `companions.enabled`, caps) and a client config for the bubble look, the `TalkScreen` (client), the sample companion
(`ArcCompanionEntity`, Charmy), lang keys (`tools/lang/companions.json`), tests, a self-test `magic.companions`, a layout preview.

## 4. Things deliberately not done

* No keybinds (the screen opens by right click; the old `arc_npc` keeps its chat talk).
* No dependency on `tensuranoindex`; no copy of its code or assets (only the shape of its public contract, which the owner gave as a document).
* The 30 shared arc NPCs do NOT become companions in this change (only the one sample, behind the setting). Making others companions later is one
  line each (a favourites registration + an egg).
* No audio is recorded or generated.
* Panelable is an interface and the `g:panel` hook only; no growth panel screen is built.
