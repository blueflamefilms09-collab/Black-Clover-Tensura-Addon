# Companions: the talk screen, the bond, followers, bubbles

Our own companion system (owner chose "build our own version"; no dependency on `tensuranoindex`, none of its code or assets). It follows the
shape of the contract in the owner's `COMPANION_API.md`, under `com.newuniverse.nusmp.companion` (client half `client/companion`). What the mod
already had, and what is reused, is in `companion_audit.md`.

Preview: `python3 -B tools/gui/preview_companion_gui.py` writes `docs/previews/companions/` (talk_screen_sheet.png = the four tabs; talk_bond_stranger.png =
Interact tab with a closed choice; talk_small.png = a 320 x 180 window; bubble_mock.png = a DRAWN MOCK-UP of the bubble, not a capture). The pictures are
made by the same `TalkPainter` / `TalkLayout` the game uses; only the drawing surface and the font metrics are stand-ins.

## NEW / REPLACED / KEPT (rule 5)

* NEW (everything): `Talkable`, `Companion`, `PersonState`, `Stance`, `Favourites`, `Bond`, `TalkView`, `Talk`, `Bubble`, `PortalFollower`, `CompanionTravel`,
  `Panelable`, `VoiceLines`, the five packets, `CompanionConfig`, the talk screen, the bubble renderer, the sample companion, the lang fragment, tests, self-tests.
* REPLACED: nothing. KEPT: every existing NPC, the arcs' chat dialogue, the Avatar's `Stance`, the Avatar / Shishou / Spirit Lord / Soul Corpse followers,
  `BetrayalDimensionHandler` (its technique is copied, the class is untouched), the `Voice` pipeline (used as one backend).
* Touched shared files (appended only): `NUSMP.java` (two lines: `CompanionBoot.init`, `CompanionClient.init`), `NUCreativeTab.java` (one line: the egg).

## How a companion takes part

An entity implements the interfaces it needs; each is independent.

| Interface | What it gives |
|---|---|
| `talk.Talkable` | `talkOpen(player)`, `talkChoose(player, id)` (returns the next `TalkView`, or `TalkView.closed()`), `talkClosed(player)`. Open from `mobInteract` with `Talk.open(serverPlayer, this)`. |
| `bond.Companion` | `bondKey`, `langPrefix`, `bond()` (a `PersonState`: stance, nine-slot pouch, use-it switch), `bondRapport` / `bondAddRapport`, `bondOwner`, `fighter`, `bondSpeak`, `bondFeel`, `socialKind` / `socialAge` / `social`, `canCarry`, `customGifts`. |
| `follow.PortalFollower` | `followedPlayer()`: she is carried through dimension changes. |
| `growth.Panelable` | optional: a button `g:panel`; the server calls `panelOpen` (this mod ships no panel screen). |
| `talk.Bubble.say(entity, component)` | a speech bubble over any entity, for the players tracking it. |
| `bond.Favourites.register(key, loves, accepts, hates)` | her tastes (once, at common setup). `accepts` = things that are not food but she is glad of (counts as a plain gift). |
| `voice.VoiceLines.register(langKey, soundSupplier)` / `registerVoice(langKey, catalogueId)` / `play(...)` | a recorded voice for a lang key. |

`Bond.options(builder, companion, player)` adds the shared choices to a view; `Bond.choose(companion, player, id)` answers every `k:` id (returns the line, or null
for an id that is the companion's own); `Bond.tick(companion)` is the pouch looking after the player; `Bond.foe(self, owner, candidate)` and
`Bond.wants(companion, self, owner, candidate)` decide who she may attack.

### Numbers (`CompanionRules`, pure, tested)

Rapport 0..99; tiers: stranger under 3, acquaintance under 10, friend under 25, close from 25. High five +1 the first time each in-game day (game clock / 24000,
so sleeping or `/time set` cannot reset it); loved gift +5 (the first two a day per companion, then +1); plain food or an accepted item +2; hated -1; an item that is
not food and on no list is refused and not taken. The pouch (9 slots) may be used by her on the player when their health is below 2/5, a healing potion first, else
the most nourishing food, at most every 30 seconds; the player can switch it off. Stances (GUARD default, AGGRESSIVE, PASSIVE) are offered to the player she walks
with (to anyone while she is nobody's); the pouch needs her to walk with you, or you to be a friend (tier 2) while she is nobody's.

### The talk screen limits (`TalkRules`)

Option id at most 40 plain characters (no space, control character or colour code); at most 40 options; ids beginning `k:` (bond), `g:` (growth panel), `x:`
(the screen itself) are reserved: `TalkView.Builder.add` throws for a companion's own id with such a prefix. Tabs: Talk, Interact, Bond, Pouch. A choice that cannot be
taken is built with `addBlocked(id, label, tab, reasonComponent)` (shown dimmed, the reason in a tooltip). Every text is a `Component` (lang key + arguments).

### Foe rule (`Bond.foe`)

A foe is something hostile (`Enemy`, or a mob targeting the owner) or whoever the owner struck / was struck by in the last 10 seconds. Never: a player, a villager or
a character of the arcs (`ArcNpcEntity`), another `Companion` / `Talkable`, a pet of the same owner, an ally (`isAlliedTo`, or a pet of an FTB squadmate through
`FtbTeamHelper`). A damage guard (`LivingIncomingDamageEvent`) stops a player's blow and another companion's blow landing on a companion (creative players, `/kill`
and the void still can). `StanceRules` says what a stance does with a foe: PASSIVE never; GUARD those tied to the owner, and hostiles within 8 blocks of the owner;
AGGRESSIVE any hostile within 16 blocks of her.

## The sample companion

**Charmy Pappitson** (Cotton and Food Mage, an existing roster NPC) as `nusmp:arc_companion`, a subclass of `ArcNpcEntity` (her model, name, colours and talk clip;
the 30 shared NPCs are untouched). Creative spawn egg `nusmp:companion_charmy_spawn_egg`, nothing places her in the world. Right click opens the talk screen. Talk tab:
who she is, what she likes to eat, her magic. Interact tab: walk with me (needs acquaintance; limited by `companions.maxFollowersPerPlayer`), wait here, high five. Bond tab:
gifts, what she likes, stance. Pouch tab. She follows (walks to you beyond 6 blocks, is put beside you beyond 24), comes through dimension changes, fights by her stance
using `Bond.wants`, says her answers in bubbles. With `companions.enabled` off a right click does the old arcs' chat talk.

Her tastes (`CompanionProfiles`) were CHOSEN FOR PLAY, not taken from a wiki page: loves cake, pumpkin pie, cookie, bread, cooked beef; glad of sugar and wheat; hates rotten flesh,
spider eye, poisonous potato, pufferfish. **Source: none** (the English and Japanese wikis were not consulted for this build; she is the canon glutton, which is all the choice relies on).

Adding another companion is one `Profile` record, one egg, and the lang lines under its prefix (the lang test lists exactly which).

## Settings

Server `nusmp-companions-server.toml` (`[companions]`): `enabled` (true), `maxFollowersPerPlayer` (2, 1..6), `travel` (true), `pouchUse` (true), `bubbles` (true).
Client `nusmp-companions-client.toml`: `bubbles` (true). Lang fragment: `tools/lang/companions.json` (to be merged into `en_us.json`).

## Voices

The mod has a voice pipeline (`audio/voice`: `Voice.say / sayNear` over a fixed catalogue with per-player limits). `VoiceLines` is a REAL hook, not a stub: a lang
key can map to a `SoundEvent` (played to nearby players) or to a catalogue id (through `Voice.sayNear`, so its limiter applies). **No recording exists for any
companion line**, so the mod registers none and `VoiceLines.play` answers false for every companion line today; the text and the bubble are the whole line.

## Worst-case cost (rule 11)

| What | Bound |
|---|---|
| Server tick | Nothing runs per tick except an `ARRIVED.isEmpty()` check (and only while a crossing is waiting). Sweeps once a second over at most one record per player. `Bond.tick`: once a second per companion, only when she has an owner. The sample's target scan: one box query (16 x 6 x 16) every 10 ticks while she walks with someone, every 40 when not, none when PASSIVE. |
| Packets in | Two kinds (pick, close). A pick is dropped unless: that player has a conversation open with that entity, within 8 blocks, alive, not faster than every 4 ticks, and the id was on the last screen. Ids read with a 48 character bound. At most 5 picks per second per player act. |
| Packets out | A screen is one packet to ONE player per accepted pick (at most 5 a second each). A bubble: one small packet to the players tracking the speaker; one per speaker per 2 s, at most 8 per tick on the whole server (PerfBudget), a 512-record cap. Voice: at most 4 lines per tick. |
| Dimension change | One box query (32 blocks) around the travelling player, at most `maxFollowersPerPlayer` (never over 6) followers carried, nearest first; their chunk gets the vanilla 300-tick portal ticket; placed only when the chunk the player stands in is already loaded (never forces one), at most 140 block checks per follower, record dropped after 100 ticks. |
| Memory | One small record per player with a screen open; one entry per player with a crossing in progress; one per speaker for the bubble gap. All dropped on logout, death, dimension change, entity removal and server stop. |
| Client | The screen is drawn from a layout computed per frame (a few ints) and pre-resolved strings; only the visible rows (about 6) are drawn; no per-frame allocation except a handful of small objects. Bubbles: at most 24 kept, each wrapped once to at most 4 lines, drawn only within 24 blocks: worst case 96 short text draws a frame, usually a handful. |

## Tests

`CompanionRulesTest`, `TalkRulesTest`, `StanceAndFoeRulesTest`, `TravelAndBubbleRulesTest`, `CompanionLimiterTest` (pure rules and limits); `TalkLayoutTest`, `TalkPainterTest` (the
screen's layout, hit tests and drawing through a counting canvas); `CompanionLangTest` (every key the code names exists; every companion record has the contract's lines with
placeholders); `CompanionSourceScanTest` (no client class in common code, no static player, no keybind, bounded reads, every check in `Talk.onPick` in order, throttles and budgets
present, gated per-tick work, settings in the right config types, self-test prefix). Self-tests (`SelfTests`, run by the CI smoke on a dedicated server with stand-in players):
`magic.companions.wiring`, `.talk`, `.bond`, `.follow` (includes a REAL `changeDimension` of followers to the Nether), `.bubble`. The smoke list needs the prefix `magic.companions`.

## Not verified in a game

Nothing here has been run in Minecraft: the build environment could not run Gradle (no network for the NeoForge toolchain), so the code was type-checked against API stubs
and the pure tests were run; the self-tests, the screen, the bubble renderer, the follow goal, the high five (no dedicated pose for the arc model: it shows particles and the talk
clip; `socialKind` / `socialAge` are synced and ready for a pose clip) and the Nether crossing have not been seen working. The stub check cannot tell `RegistryFriendlyByteBuf` and
`MobEffectInstance` are concrete (it reports four false errors on them). The potion's bottle is not given back when she uses a potion.

## Great Spirit screens (client only)

The Talk, Interact, Bond and Pouch screens and the bubbles can be drawn in the great_spirit skins (B for normal users, C for devil users), with portrait expressions, a rapport meter,
a gift slot, taste hints, a stance selector, a 9-slot pouch with a heal-ready ring and a bond-story log. The client option `companions.screenStyle` (AUTO, GREAT_SPIRIT, CLASSIC) picks
the style; CLASSIC is the old screen, unchanged. No new packet and no server change. Details, costs and previews: `docs/gui_redesign/companion_gui.md`.
