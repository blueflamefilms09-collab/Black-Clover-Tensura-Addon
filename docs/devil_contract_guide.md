# Devil Contract System: implementation guide for this repo (NeoForge 21.1.234, Minecraft 1.21.1)

Status: **guide only, nothing is built.** It answers the owner's `claude_prompt_devil_system.md` against the code that is already in the mod, and
corrects the places where the prompt's generic advice does not fit NeoForge 1.21.1 or this repo. Numbers are proposals. Wiki lookups were not possible
from this session (rule 10: the English and Japanese wiki hosts are not reachable), so no canon claim is made: the names Low / Mid / High / Supreme,
"Weg" and "Devil Union" are used exactly as the owner's file uses them.

## 0. What the mod already has, and what would be new (rule 5)

| Part of the prompt | In the mod today | Verdict |
|---|---|---|
| A devil bound to a player | `book/ForbiddenMagic.bind`: four original devils picked from the grimoire's magic (`PactDevil.forMagic`), one per player in `SavedData` `nusmp_devil_pacts`, one bond at a time (`Bonds.refuse`) | **kept**; the new data points at it |
| `ContractSacrifice` (price) | three prices chosen at bind time: `maxhp` (-2 hearts per bind, permanent `AttributeModifier` `nusmp:devil_price_hp`, count in `nusmp_devil_hp`), `seal` (a page sealed for good), `tax` (+15% cost) | **kept**; new sacrifices are added beside them |
| Devil Union | `book/DevilFormLimit` + `AntiMagicBook.setForm`: the Anti-Magic Black Form (Asta + Liebe), strict 5 minutes (`armour.devilFormLimitSeconds` = 300), then ~30 minutes of muscle pain (`devilFormAftermathSeconds` = 1800), no mana cost | **kept**; the new union is a separate, generic one for any devil (decision 1 below) |
| `DevilTier`, `DevilAttribute`, `AssimilationLevel` 0-100 | nothing | **new** |
| Assimilation key + scaling mana drain | nothing (the Beast Will assimilation of 3.45.0 is a different system and a different file set) | **new** |
| Strength / Speed / Flight scaling by tier and percent | nothing | **new** |
| 24 in-game hour cooldown | nothing (the old union has the 30-minute aftermath) | **new** |
| Weg markings, horns, wings | nothing | **new** |
| Half max health, lost eye | only the flat -2 hearts per bind | **new**, added next to it |

## 1. Corrections to the prompt

1. **No Forge Capabilities for player data.** In NeoForge 1.21.1 the tool is a **Data Attachment** (`AttachmentType`). The repo already does exactly this
   in `beastwill/BeastWillStore` (`.serialize(codec, nonEmpty).copyOnDeath()`), so the devil data copies this pattern.
2. **`V` is taken.** `client/relics/RelicActKeys.USE` defaults to `V`; `G` = grimoire summon, `B` = hero weapon, `J`/`K` = djinn, and `L`/`Q`/`T` are vanilla keys. I would
   default the new key to **`N`** (rebindable, category `key.categories.nusmp`), after checking its one existing use at build time.
3. **`RenderPlayerEvent` is the wrong tool for horns and wings.** It wraps the whole player render (good for scaling or cancelling it) but gives no handle on the head and body bones.
   Attach with a **`RenderLayer`** added in `EntityRenderersEvent.AddLayers`; the layer's pose stack is already in player space, and `getParentModel().head.translateAndRotate(pose)` puts
   you on the head bone, animated and sneaking included. The repo already does this in `client/aura/PlayerAuraClient` + `PlayerAuraLayer`.
4. **First person needs `RenderArmEvent`.** Layers do not run for the first-person arm; the repo already hooks this in `client/aura/DiveFirstPerson`.
5. **"24 in-game hours" is 24 000 ticks** (one Minecraft day, 20 real minutes). Count it in `getGameTime()`, never `getDayTime()`: `/time set` and sleeping move the day time, not the game time.
6. **The "mana" of this mod is Tensura's magicules** (`SkillUtil.spendMagicules`). The Anti-Magic "Aura gauge" is a separate pool and stays untouched.
7. **Attachments are not synced for free.** The repo sends its own tiny packets (BeastWillNet pattern) so it controls how often and to whom. Same here.

## 2. Milestone 1: the contract data

New package `com.newuniverse.nusmp.devil` (all server-safe; the client classes live in `client/devil`).

```java
// Enums are stored BY NAME so adding one later never shifts a saved value (rule 2: append only).
public enum DevilTier { NONE, LOW, MID, HIGH, SUPREME }
public enum DevilAttribute { NONE, GRAVITY, SPATIAL, CURSE, ANTI_MAGIC /* append only */ }
public enum Sacrifice { HALF_HEALTH, LOST_EYE_LEFT, LOST_EYE_RIGHT /* the three old prices stay in ForbiddenMagic */ }

public final class DevilContract {                       // plain object, like BeastWillState
    public DevilTier tier = DevilTier.NONE;
    public DevilAttribute attribute = DevilAttribute.NONE;
    public float assimilation;                           // 0..100, the live level
    public long unionUntil;                              // game time the union must end (0 = not in the union)
    public long unionReadyAt;                            // game time the next union may begin
    public final EnumSet<Sacrifice> sacrifices = EnumSet.noneOf(Sacrifice.class);
    public boolean assimilating;                         // the key toggle; NOT saved: every login starts "off"
    public boolean isEmpty() { return tier == DevilTier.NONE && sacrifices.isEmpty() && unionReadyAt == 0; }
}

public final class DevilStore {                          // the registration, same shape as BeastWillStore
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);
    public static final Codec<DevilContract> CODEC = CompoundTag.CODEC.xmap(DevilStore::fromTag, DevilStore::toTag);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<DevilContract>> CONTRACT =
        ATTACHMENTS.register("devil_contract",
            () -> AttachmentType.builder(DevilContract::new).serialize(CODEC, c -> !c.isEmpty()).copyOnDeath().build());

    public static DevilContract of(Player p)   { return p.getData(CONTRACT); }                 // creates (use for writing)
    public static DevilContract peek(Player p) { return p.hasData(CONTRACT) ? p.getData(CONTRACT) : null; }   // never creates (use for reading)
    // toTag/fromTag: hand-written, version number "v", every value clamped on read (assimilation 0..100, times >= 0,
    // unknown enum names -> NONE) so an edited or damaged save can not give a value outside the rules.
}
// NUSMP: DevilStore.ATTACHMENTS.register(modBus);
```

- `copyOnDeath()` copies the data to the new body; **do not copy by hand** in `PlayerEvent.Clone`. In that event only reset the live state: `assimilation = 0`, `assimilating = false`, `unionUntil = 0` (the cooldown `unionReadyAt` stays).
- Who gets a contract: `ForbiddenMagic.bind` stays as it is; after it succeeds, `DevilContracts.grant(p, tier, attribute)` fills the new data. The tier can come from the price chosen
  (the harder the price, the higher the tier) or from the devil: **decision 2**. The attribute comes from the magic type (`GRAVITY`/`SPATIAL` from `PactDevil.GRAVEMAW`'s magics, and so on).
- Players without a contract cost one `hasData` check per second and nothing else.

## 3. Milestone 2: the key, the drain and the stats

### 3.1 Client to server: one tiny packet, the server decides

```java
// common code, no client class named (the BeastWillNet rule)
public record Toggle() implements CustomPacketPayload {
    public static final Type<Toggle> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "devil_toggle"));
    public static final StreamCodec<FriendlyByteBuf, Toggle> CODEC = StreamCodec.unit(new Toggle());
    @Override public Type<Toggle> type() { return TYPE; }
}
// mod bus, RegisterPayloadHandlersEvent
event.registrar("1").optional().playToServer(Toggle.TYPE, Toggle.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
    if (!(ctx.player() instanceof ServerPlayer sp)) return;
    if (!DevilGate.allow(sp)) return;                    // rate limit: one ask per 4 ticks per player (UUID-keyed map, cleaned on logout)
    DevilRules.toggle(sp);                               // validates: has a contract, setting on, not in cooldown-lock, not in the union
}));
```

```java
// client/devil/DevilClient.java (client only)
public static final KeyMapping ASSIMILATE = new KeyMapping("key.nusmp.devil_assimilate",
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories.nusmp");
modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(ASSIMILATE));
NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
    while (ASSIMILATE.consumeClick()) PacketDistributor.sendToServer(new Toggle());   // an ask, never a value
});
```

### 3.2 Pure rules (all numbers here, unit-tested without a world)

```java
public final class DevilRules {
    public static final long UNION_TICKS = 6000;         // 5 minutes, exactly
    public static final long UNION_COOLDOWN_TICKS = 24000; // 24 in-game hours
    // % per second while ON, by tier: a Low devil needs 100 s, a Supreme one 40 s
    static float climb(DevilTier t)  { return switch (t) { case LOW -> 1.0f; case MID -> 1.5f; case HIGH -> 2.0f; case SUPREME -> 2.5f; default -> 0f; }; }
    static final float DECAY = 4.0f;                     // % per second while OFF
    static double drain(DevilTier t, float a) {          // magicules per second; x10 between 0% and 100%
        double x = a / 100.0, base = switch (t) { case LOW -> 20; case MID -> 40; case HIGH -> 70; case SUPREME -> 120; default -> 0; };
        return base * (1 + 9 * x * x);
    }
    static float cap(long now, DevilContract c) { return now >= c.unionReadyAt ? 100f : 99f; }   // never reaches 100 while cooling down
    static int stage(float a) { return a >= 100 ? 3 : a >= 50 ? 2 : a >= 20 ? 1 : 0; }        // 1 horns, 2 wings, 3 union
    // stat bonus at level a (smoothstep so the first percent are cheap and the last ones heavy):
    static double strength(DevilTier t, float a) { return tierStrength(t) * smooth(a / 100f); }       // ATTACK_DAMAGE, ADD_VALUE      (2 / 4 / 7 / 12 at 100%)
    static double speed(DevilTier t, float a)    { return tierSpeed(t) * smooth(a / 100f); }          // MOVEMENT_SPEED, ADD_MULTIPLIED_TOTAL (.10/.20/.30/.45)
    static boolean flight(float a) { return a >= 50; }                                                // wings give flight
}
```

### 3.3 The once-a-second step (server, `PlayerTickEvent.Post`, `tickCount % 20 == 0`, only for players with a contract)

```java
static void second(ServerPlayer p, DevilContract c) {
    long now = p.getServer().overworld().getGameTime();           // one clock for all dimensions
    if (c.unionUntil > 0) {                                       // ---- the Devil Union
        if (now >= c.unionUntil || !SkillUtil.spendMagicules(p, DevilRules.drain(c.tier, 100))) endUnion(p, c, now);   // strict: out of time OR out of mana ends it
    } else if (c.assimilating) {                                  // ---- climbing
        if (!SkillUtil.spendMagicules(p, DevilRules.drain(c.tier, c.assimilation))) { c.assimilating = false; msg(p, "The devil withdraws."); }
        else c.assimilation = Math.min(DevilRules.cap(now, c), c.assimilation + DevilRules.climb(c.tier));
        if (c.assimilation >= 100) beginUnion(p, c, now);         // unionUntil = now + UNION_TICKS
    } else if (c.assimilation > 0) {
        c.assimilation = Math.max(0, c.assimilation - DevilRules.DECAY);
    }
    applyStats(p, c);                                             // only touches attributes when the rounded value changed
    DevilNet.syncIfChanged(p, c);                                 // see section 5
}

static void endUnion(ServerPlayer p, DevilContract c, long now) {
    c.unionUntil = 0; c.assimilation = 0; c.assimilating = false;
    c.unionReadyAt = now + DevilRules.UNION_COOLDOWN_TICKS;       // 24 000 ticks, stored as an absolute time: logging out does not pause it
    applyStats(p, c); DevilNet.syncNow(p, c);
}
```

Strictness: the 5 minutes is stored as an absolute game time (`unionUntil`), checked once a second, and also checked on login, so a log-out can not stretch it. **Logout, death and dimension
change while in the union** all call `endUnion` (the cooldown starts), so none of them is a refresh. Assimilation below 100 just resets to 0 on logout and death.

### 3.4 Dynamic attribute modifiers

```java
private static final ResourceLocation STR = rl("devil_strength"), SPD = rl("devil_speed");
static void applyStats(ServerPlayer p, DevilContract c) {
    double s = DevilRules.strength(c.tier, c.assimilation) * (c.unionUntil > 0 ? 1.5 : 1.0);
    set(p, Attributes.ATTACK_DAMAGE,  STR, s, AttributeModifier.Operation.ADD_VALUE);
    set(p, Attributes.MOVEMENT_SPEED, SPD, DevilRules.speed(c.tier, c.assimilation), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    set(p, NeoForgeMod.CREATIVE_FLIGHT, FLY, DevilRules.flight(c.assimilation) ? 1 : 0, AttributeModifier.Operation.ADD_VALUE);
}
static void set(ServerPlayer p, Holder<Attribute> a, ResourceLocation id, double v, AttributeModifier.Operation op) {
    AttributeInstance i = p.getAttribute(a); if (i == null) return;
    if (v == 0) { i.removeModifier(id); return; }
    AttributeModifier old = i.getModifier(id);
    if (old != null && old.amount() == v) return;                 // no churn: attribute changes are packets
    i.addOrUpdateTransientModifier(new AttributeModifier(id, v, op));   // TRANSIENT: never saved, so a crash can not leave a buff behind
}
```

- Transient modifiers for the live stats (they vanish with the session); **permanent** ones only for sacrifices (section 6).
- Flight uses `NeoForgeMod.CREATIVE_FLIGHT`, the attribute the repo already uses in `mana/zone/ManaZoneManager`. When it is removed (level < 50, union over) also do `abilities.flying = false; p.onUpdateAbilities();` and give 10 s of Slow Falling so nobody drops out of the sky.
- The two unions never run at once: while `AntiMagicBook.inBlackForm` is on, the new key answers "the Anti-Magic union is active" and does nothing.

## 4. Milestone 3: rendering the Weg

### 4.1 What the client needs

Other players' state is needed to draw them, so the server sends a small **visual** packet (section 5) and the client keeps `Int2ObjectMap<Visual>` keyed by **entity id** (`tier`, `stage`, `percent`),
cleared on `ClientPlayerNetworkEvent.LoggingOut` and when the entity leaves. No client class is named by common code (`ServerSafetyTest` stays green).

### 4.2 The layer (markings, horns, wings), one class

```java
// mod bus
modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) -> {
    e.registerLayerDefinition(DevilModels.HORNS, HornsModel::create);        // from Blockbench "Modded Entity" export
    e.registerLayerDefinition(DevilModels.WINGS, WingsModel::create);
    e.registerLayerDefinition(DevilModels.WEG_WIDE, () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.02F), false), 64, 64));
    e.registerLayerDefinition(DevilModels.WEG_SLIM, () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.02F), true), 64, 64));
});
modBus.addListener((EntityRenderersEvent.AddLayers e) -> {
    for (PlayerSkin.Model skin : e.getSkins())                                   // wide AND slim arms
        if (e.getSkin(skin) instanceof PlayerRenderer r) r.addLayer(new WegLayer(r, e.getEntityModels(), skin));
});

public final class WegLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    @Override public void render(PoseStack pose, MultiBufferSource buf, int light, AbstractClientPlayer p,
                                 float limb, float limbAmt, float partial, float age, float yaw, float pitch) {
        Visual v = DevilClient.visual(p.getId());
        if (v == null || v.tier() == DevilTier.NONE || p.isInvisible()) return;
        if (p.distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()) > 32 * 32) return;   // far players: skipped

        // (a) markings: the same pose, a 0.02-inflated copy of the player model so it does not z-fight the skin
        getParentModel().copyPropertiesTo(overlay);                              // head, body, arms, legs (set sleeve/jacket/pants visibility the same way)
        int argb = FastColor.ARGB32.color((int) (255 * Math.min(1f, 0.25f + v.percent() / 130f)), 255, 255, 255);
        overlay.renderToBuffer(pose, buf.getBuffer(RenderType.entityTranslucent(wegTexture(v.tier(), slim))), light, OverlayTexture.NO_OVERLAY, argb);

        // (b) horns on the head bone from 20%
        if (v.stage() >= 1) {
            pose.pushPose(); getParentModel().head.translateAndRotate(pose);
            horns.renderToBuffer(pose, buf.getBuffer(RenderType.entityCutoutNoCull(HORNS_TEX)), light, OverlayTexture.NO_OVERLAY, -1);
            pose.popPose();
        }
        // (c) wings on the torso bone from 50%; folded when standing, spread when flying, full and glowing at 100%
        if (v.stage() >= 2) {
            pose.pushPose(); getParentModel().body.translateAndRotate(pose); pose.translate(0, 0, 0.125);
            wings.setup(p, age, v.stage(), p.getAbilities().flying);             // flap = sin(age * .2) * .15 on top of a rest angle
            wings.renderToBuffer(pose, buf.getBuffer(RenderType.entityCutoutNoCull(WINGS_TEX)), light, OverlayTexture.NO_OVERLAY, -1);
            if (v.stage() >= 3) wings.renderToBuffer(pose, buf.getBuffer(RenderType.eyes(WINGS_GLOW_TEX)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
            pose.popPose();
        }
    }
}
```

- **Markings texture**: one 64x64 player-skin-layout PNG per tier, transparent background, black marks only, with a `_slim` variant for the narrower arms (or draw the marks so the arm UVs are valid for both). Alpha grows with the percent; the file never has to be the player's skin.
- **First person**: `RenderArmEvent` draws the same marking texture on the arm (copy `DiveFirstPerson`).
- **Blockbench**: export "Modded Entity" (Mojang mappings); its model space matches the layer's pose, no manual flip. The repo's own `geo.json` + glow renderer (GeoLite) can load the same model if a Bedrock export is preferred. Rule 7 asks for glow / void effects that fit: the union wings get the emissive second pass above.
- **Do not use `RenderPlayerEvent`** for any of this. Keep it for the one thing it is good at: `RenderPlayerEvent.Pre` to cancel or scale the whole player (not needed here).

## 5. Networking (all of it)

| Packet | Direction | Contents | When | Who gets it |
|---|---|---|---|---|
| `Toggle` | client -> server | nothing (an ask) | key press | server validates sender, contract, setting, 4-tick rate limit |
| `Sync` | server -> client | tier, attribute, percent (byte), union seconds left, cooldown seconds left, sacrifice mask | only when a field changed, at most once a second | that player only (HUD, eye overlay) |
| `Visual` | server -> client | entity id, tier, stage, percent bucket (5% steps) | when tier, stage or the 5% bucket changed; again on `PlayerEvent.StartTracking` | `PacketDistributor.sendToPlayersTrackingEntityAndSelf`, nearby players only |

- Registered once with `event.registrar("1").optional()` (a client without the mod simply never gets them), exactly like `BeastWillNet`, `PlayerAuraPayload`.
- `Visual` for late joiners: `PlayerEvent.StartTracking` handler, the same hook `PlayerAuras.onStartTracking` and `ModeArmor.onStartTracking` already use.
- NeoForge's built-in attachment sync (`AttachmentType.Builder.sync`) exists too, but it would send the whole contract to everybody who tracks the player; the repo prefers the small packets above so the cost is bounded.

## 6. Milestone 4: the sacrifices

```java
private static final ResourceLocation HALF = rl("devil_sacrifice_half_hp");
static void applySacrifices(ServerPlayer p, DevilContract c) {                    // idempotent: safe to call as often as needed
    AttributeInstance hp = p.getAttribute(Attributes.MAX_HEALTH);
    if (hp != null) {
        if (c.sacrifices.contains(Sacrifice.HALF_HEALTH))
            hp.addOrReplacePermanentModifier(new AttributeModifier(HALF, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));   // halves the TOTAL, whatever other buffs there are
        else hp.removeModifier(HALF);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }
    // optional teeth for a lost eye, kept behind a server setting: shorter reach (lost depth perception)
    set(p, Attributes.ENTITY_INTERACTION_RANGE, EYE, c.sacrifices.stream().anyMatch(s -> s.isEye()) ? -0.75 : 0, ADD_VALUE);
}
```

- **Persistence through death**: the contract is a `copyOnDeath()` attachment, and `applySacrifices` is called from `PlayerEvent.PlayerRespawnEvent`, `PlayerLoggedInEvent` and `PlayerChangedDimensionEvent`. I do not rely on vanilla copying permanent modifiers to the new body; the attachment is the source of truth. (The existing `-2 hearts` price of `ForbiddenMagic` was not checked across death in a game; if it turns out to be lost on respawn, a small hook that re-applies it from `nusmp_devil_hp` fixes it, and that would be listed as a fix, not a change.)
- **Undo**: only an admin command (`/multiverse devil sacrifice clear <player> <name>`). Milk, totems, healing and Tensura resets do not touch it (modifiers are not effects).
- **Lost eye = client look, server state**: the server only stores and syncs the mask; the client draws.

```java
// client/devil (looks only, client config: devilEyeOverlayStrength 0..1 so nobody is forced into a screen they can not play with)
modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(EYE_LAYER, (g, dt) -> {
    int mask = DevilClient.self().sacrificeMask(); if ((mask & EYES) == 0) return;
    float k = ClientConfig.EYE_STRENGTH.get();                                      // 0 disables the look
    boolean left = (mask & LEFT) != 0, right = (mask & RIGHT) != 0;
    int w = g.guiWidth(), h = g.guiHeight(), a = (int) (0xE0 * k) << 24;
    if (left)  g.fillGradient(0, 0, w / (right ? 1 : 3), h, a, a & 0xFF000000);     // a dark gradient over the lost side (a texture works too)
    if (right) g.fillGradient(w - w / (left ? 1 : 3), 0, w, h, a, a & 0xFF000000);
}));
NeoForge.EVENT_BUS.addListener((ViewportEvent.ComputeFov e) -> {                    // narrower field of view for one lost eye
    int mask = DevilClient.self().sacrificeMask();
    if ((mask & EYES) != 0) e.setFOV(e.getFOV() * (1.0 - 0.12 * Integer.bitCount(mask & EYES) * ClientConfig.EYE_STRENGTH.get()));
});
```

## 7. Settings, tests, and the cost (rules 9, 11, 12)

- **Server config** (`NUConfig`, a new section, all default ON so the owner sees it; each restores the old behaviour when off): `devil.enabled`, `devil.unionSeconds` (300), `devil.unionCooldownHours` (24), `devil.halfHealthSacrifice`, `devil.eyeReachPenalty`, `devil.keepAssimilationOnDeath` (false). **Client config**: `devilEyeOverlayStrength`, `devilWegLayer` (off draws nothing extra).
- **Pure tests**: `DevilRulesTest` (drain strictly rises with the percent, stage thresholds 20/50/100, union exactly 6000 ticks, cooldown exactly 24000, cap 99 while cooling down, no stat above its table), `DevilStoreTagTest` (clamping on read, unknown enum names), `DevilSourceScanTest` (no client class, no `Minecraft.getInstance()` outside `client/`, no static player, packet checks present). **Self-tests** (smoke list): `devil.contract.registry`, `devil.rules.bounds`, `devil.net.registry`.
- **Worst-case cost**: server: one `hasData` per player per second, plus a handful of arithmetic and at most three attribute writes per contracted player per second (and only when a value changed). Packets: `Sync` at most 1/s to the owner; `Visual` only on a change (a climb to 100% is at most 20 changes in 40 s or more, so under 0.5 per second per climbing player) to nearby players only. Client: one extra translucent model pass (about 36 quads) + horns (about 2 small boxes) + wings (about 12 boxes) = under 200 quads per devil player, nothing past 32 blocks, a cap of 16 devil players drawn per frame, no per-frame allocation (the model parts are baked once). Eye overlay: two `fillGradient` calls a frame.

## 8. Build order if the owner says go (proposed 3.48.0)

1. `devil` package: enums, `DevilContract`, `DevilStore` (+ registration), `DevilRules` + `DevilRulesTest` + `DevilStoreTagTest`.
2. Server: `DevilContracts.grant` hooked after `ForbiddenMagic.bind`, the once-a-second step, union and cooldown, `Toggle`/`Sync`/`Visual` packets, respawn/login/logout/dimension hooks, settings, self-tests.
3. Client: key, HUD line (percent, union and cooldown), `WegLayer` with generated placeholder textures and Blockbench-style models built by a python tool (like `tools/gen_weapon_models.py`), first-person arm, eye overlay and FOV, previews sent to the owner as soon as each visual is built (rule 12).
4. Docs, lang keys, changelog, version 3.48.0, release with the jar.

**Not included**: hand-painted artwork (the textures and models are generated and previewed, the owner's own Blockbench models can replace them file for file), canon text (wiki not reachable), the devils' own attacks (the existing Forbidden pages stay as they are), anything changing the Anti-Magic Black Form.

## 9. Decisions the owner still has to make

1. **Is the new union a second, separate union, or does it replace the Anti-Magic one?** Default in this guide: separate, and the old one stays (rule 2). If it should replace the old 5-minute / 30-minute rule, say so and the old numbers stay behind a setting.
2. **Where does the tier come from?** (a) the price chosen at the contract (harder price, higher tier), (b) the devil (Ignivar, Verbael, Gravemaw, Kairovore each get a fixed tier), or (c) both: the devil sets the attribute, the price sets the tier. Default: (c).
3. **Does a death during the union start the 24-hour cooldown?** Default: yes.
4. **Key**: `N` by default, rebindable.
