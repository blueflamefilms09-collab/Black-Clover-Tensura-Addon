package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.layer.FlameMagicLayer;
import com.newuniverse.nusmp.vfx.client.layer.MagicCircleLayer;
import com.newuniverse.nusmp.vfx.client.layer.WindMagicLayer;
import com.newuniverse.nusmp.vfx.client.layer.WaterMagicLayer;
import com.newuniverse.nusmp.vfx.client.layer.RuneCircleLayer;
import com.newuniverse.nusmp.vfx.client.layer.AntiMagicLayer;
import com.newuniverse.nusmp.vfx.client.layer.ElementShapesLayer;
import com.newuniverse.nusmp.vfx.client.layer.AuraLayer;
import com.newuniverse.nusmp.vfx.client.layer.ArcaneSpellLayer;
import com.newuniverse.nusmp.vfx.client.layer.ArcaneSpellLayer2;
import com.newuniverse.nusmp.vfx.client.layer.DreamPaintLayer;
import com.newuniverse.nusmp.vfx.client.layer.MirrorLayer;
import com.newuniverse.nusmp.vfx.client.layer.PaintStudioLayer;
import com.newuniverse.nusmp.vfx.client.layer.LightTreeLayer;
import com.newuniverse.nusmp.vfx.client.layer.DiceLayer;
import com.newuniverse.nusmp.vfx.client.layer.SlashCompassMercuryLayer;
import com.newuniverse.nusmp.vfx.client.layer.KotodamaLayer;
import com.newuniverse.nusmp.vfx.client.layer.DemonSlayerLayer;
import com.newuniverse.nusmp.vfx.client.layer.GameLayer;
import com.newuniverse.nusmp.vfx.client.layer.BeastLayer;
import com.newuniverse.nusmp.vfx.client.layer.DemonBeastLayer;
import com.newuniverse.nusmp.vfx.client.layer.BodyLayer;
import com.newuniverse.nusmp.vfx.client.layer.BloodLayer;
import com.newuniverse.nusmp.vfx.client.layer.BoneLayer;
import com.newuniverse.nusmp.vfx.client.layer.EyeLayer;
import com.newuniverse.nusmp.vfx.client.layer.EyeballLayer;
import com.newuniverse.nusmp.vfx.client.layer.CurseLayer;
import com.newuniverse.nusmp.vfx.client.layer.CurseWardingLayer;
import com.newuniverse.nusmp.vfx.client.layer.DemonFireLayer;
import com.newuniverse.nusmp.vfx.client.layer.DemonIceLayer;
import com.newuniverse.nusmp.vfx.client.layer.DemonLightLayer;
import com.newuniverse.nusmp.vfx.client.layer.DemonWaterLayer;
import com.newuniverse.nusmp.vfx.client.layer.GravityLayer;
import com.newuniverse.nusmp.vfx.client.layer.SealLayer;
import com.newuniverse.nusmp.vfx.client.layer.DarkSlashLayer;
import com.newuniverse.nusmp.vfx.client.layer.DarkMagicLayer;
import com.newuniverse.nusmp.vfx.client.layer.SealSecreLayer;
import com.newuniverse.nusmp.vfx.client.layer.BarrierLayer;
import com.newuniverse.nusmp.vfx.client.layer.ImitationLayer;
import com.newuniverse.nusmp.vfx.client.layer.KeyLayer;
import com.newuniverse.nusmp.vfx.client.layer.ChainLayer;
import com.newuniverse.nusmp.vfx.client.layer.ButohLayer;
import com.newuniverse.nusmp.vfx.client.layer.BriarLayer;
import com.newuniverse.nusmp.vfx.client.layer.CherryBlossomLayer;
import com.newuniverse.nusmp.vfx.client.layer.FungusLayer;
import com.newuniverse.nusmp.vfx.client.layer.FoodLayer;
import com.newuniverse.nusmp.vfx.client.layer.CrystalLayer;
import com.newuniverse.nusmp.vfx.client.layer.CorundumLayer;
import com.newuniverse.nusmp.vfx.client.layer.BronzeLayer;
import com.newuniverse.nusmp.vfx.client.layer.CopperLayer;
import com.newuniverse.nusmp.vfx.client.layer.IronLayer;
import com.newuniverse.nusmp.vfx.client.layer.BlackOilLayer;
import com.newuniverse.nusmp.vfx.client.layer.GelLayer;
import com.newuniverse.nusmp.vfx.client.layer.GlassLayer;
import com.newuniverse.nusmp.vfx.client.layer.BubbleLayer;
import com.newuniverse.nusmp.vfx.client.layer.IceLayer;
import com.newuniverse.nusmp.vfx.client.layer.IceWedgeLayer;
import com.newuniverse.nusmp.vfx.client.layer.LegionLayer;
import com.newuniverse.nusmp.vfx.client.layer.LightLayer;
import com.newuniverse.nusmp.vfx.client.layer.PaintingLayer;
import com.newuniverse.nusmp.vfx.client.layer.KotodamaFxLayer;
import com.newuniverse.nusmp.vfx.client.layer.WorldTreeLayer;
import com.newuniverse.nusmp.vfx.client.layer.DiceFxLayer;
import com.newuniverse.nusmp.vfx.client.layer.SlashLayer;
import com.newuniverse.nusmp.vfx.client.layer.CompassLayer;
import com.newuniverse.nusmp.vfx.client.layer.MercuryLayer;
import com.newuniverse.nusmp.vfx.client.layer.TimeMagicLayer;
import com.newuniverse.nusmp.vfx.client.layer.FireSpellLayer;
import com.newuniverse.nusmp.vfx.client.layer.WaterSpellLayer;
import com.newuniverse.nusmp.vfx.client.layer.WindSpellLayer;
import com.newuniverse.nusmp.vfx.client.layer.EarthSpellLayer;
import com.newuniverse.nusmp.perf.PerfProbe;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.perf.VfxQuality;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Client-side owner of all running effects. Layers are registered here.
 *
 * <p>1.99.0 (performance pass, docs/performance_1_99.md): {@link #render} gives each effect a level of detail by its distance from the camera ({@link VfxQuality}: full detail near, 3/4 and 1/2
 * further out, not drawn beyond the far limit; the Time Magic layer is never thinned), caps the vertices of the frame when the quality asks for it (nearest effects first), and the number of effects
 * that may run at once follows the quality (64 on high, as before). On {@code high} (the default) nothing near the camera changes. Sections of the performance probe ({@code c.vfx.*}) time the tick,
 * the render and, while the probe is on, every layer.
 */
public final class VfxManager {
    private static final VfxManager INSTANCE = new VfxManager();
    private static final int MAX_ACTIVE = 64;
    private static final PerfProbe.Section TICK_SECTION = PerfProbe.section("c.vfx.tick"), RENDER_SECTION = PerfProbe.section("c.vfx.render"), SPAWN_SECTION = PerfProbe.section("c.vfx.spawn");
    private static final java.util.Map<AbstractVfxLayer, PerfProbe.Section> LAYER_SECTIONS = new java.util.HashMap<>();

    private final Map<VfxShape, AbstractVfxLayer> layers = new EnumMap<>(VfxShape.class);
    private final List<VfxInstance> active = new ArrayList<>();
    private final VfxVertexBuffer buffer = new VfxVertexBuffer();
    /** Per-frame scratch of {@link #render} (one slot per running effect): its distance, its level of detail, whether the frame budget leaves it out. */
    private double[] distance = new double[MAX_ACTIVE];
    private float[] lod = new float[MAX_ACTIVE];
    private boolean[] left = new boolean[MAX_ACTIVE];

    public static VfxManager get() { return INSTANCE; }

    private VfxManager() {
        // ---- register layers here (one line per new magic type) ----
        register(new MagicCircleLayer());
        register(new FlameMagicLayer());
        register(new WindMagicLayer());
        register(new WaterMagicLayer());
        register(new RuneCircleLayer());
        register(new AntiMagicLayer());
        register(new ElementShapesLayer());
        register(new AuraLayer());
        register(new TimeMagicLayer());
        register(new FireSpellLayer());
        register(new WaterSpellLayer());
        register(new WindSpellLayer());
        register(new EarthSpellLayer());
        register(new ArcaneSpellLayer());     // 0.34
        register(new ArcaneSpellLayer2());
        register(new DreamPaintLayer());      // 0.41
        register(new MirrorLayer());          // 0.42
        register(new PaintStudioLayer());     // 0.44
        register(new LightTreeLayer());       // 0.45
        register(new DiceLayer());
        register(new SlashCompassMercuryLayer());
        register(new KotodamaLayer());        // 0.47
        register(new DemonSlayerLayer());     // 0.48
        register(new GameLayer());            // 0.49
        // 0.53: the Black Clover Magic and VFX expansion
        register(new BeastLayer());
        register(new DemonBeastLayer());
        register(new BodyLayer());
        register(new BloodLayer());
        register(new BoneLayer());
        register(new EyeLayer());
        register(new EyeballLayer());
        register(new CurseLayer());
        register(new CurseWardingLayer());
        register(new DemonFireLayer());
        register(new DemonIceLayer());
        register(new DemonLightLayer());
        register(new DemonWaterLayer());
        register(new GravityLayer());
        register(new SealLayer());
        register(new DarkSlashLayer());
        register(new DarkMagicLayer());
        register(new SealSecreLayer());
        register(new BarrierLayer());
        register(new ImitationLayer());
        register(new KeyLayer());
        register(new ChainLayer());
        register(new ButohLayer());
        register(new BriarLayer());
        register(new CherryBlossomLayer());
        register(new FungusLayer());
        register(new FoodLayer());
        register(new CrystalLayer());
        register(new CorundumLayer());
        register(new BronzeLayer());
        register(new CopperLayer());
        register(new IronLayer());
        register(new BlackOilLayer());
        register(new GelLayer());
        register(new GlassLayer());
        register(new BubbleLayer());
        register(new IceLayer());
        register(new IceWedgeLayer());
        register(new LegionLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.ElementIdentityLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.WikiMagicLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.EarthFamilyLayer());          // 1.93.0: Sandstone / Rock / Red Ochre / Stone / Earth / Mineral
        register(new LightLayer());
        register(new PaintingLayer());
        register(new KotodamaFxLayer());
        register(new WorldTreeLayer());
        register(new DiceFxLayer());
        register(new SlashLayer());
        register(new CompassLayer());
        register(new MercuryLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.BornMagicLayer());     // 1.16.0
        register(new com.newuniverse.nusmp.vfx.client.layer.CastHoldLayer());      // 1.27.0: the held cast key's one continuous circle
        // 1.35.0: the Zogratis bosses' own effects
        register(new com.newuniverse.nusmp.vfx.client.layer.ZogratisGravityLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.ZogratisBloodLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.ZogratisSpaceLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.ZogratisDivineLayer());
        // 1.35.0: floor / relic / Avatar bosses (b2) and Zagred / Commander / Juggernaut (b3)
        register(new com.newuniverse.nusmp.vfx.client.layer.DevilBossLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.StoneBossLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.TideBossLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.OrderBossLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.ZagredBossLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.CommanderBossLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.JuggernautBossLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.LuciusCoreGemLayer());      // 1.42.0: the chest gem of Lucius's final arc
        register(new com.newuniverse.nusmp.vfx.client.layer.ThreadFateLayer());          // 1.44.0
        register(new com.newuniverse.nusmp.vfx.client.layer.SongNotesLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.SongRingLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.CorpseLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.ShakudoLayer());             // 1.93.0: the rebuilt wiki magics
        register(new com.newuniverse.nusmp.vfx.client.layer.ScaleLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.SpikeLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.NailLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.HairLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.SongFxLayer());          // 1.93.0: the rebuilt Song, Sound, Soul, Soul Corpse and Memory effects (FX1 cast, FX2 zone, FX3 impact)
        register(new com.newuniverse.nusmp.vfx.client.layer.SoundFxLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.SoulFxLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.SoulCorpseFxLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.MemoryFxLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.SmokeLayer());                 // 1.93.0: Smoke, Snow, Wing, Vortex, Air and Ash Magic rebuild
        register(new com.newuniverse.nusmp.vfx.client.layer.AshLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.AirLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.VortexLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.WingLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.SnowLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.SlashRipFxLayer());          // 1.93.0 magic rebuild: takes SLASH_FX1..3 over from SlashLayer (which stays registered above)
        register(new com.newuniverse.nusmp.vfx.client.layer.SpatialFxLayer());           // 1.93.0 magic rebuild: Spatial, Mirror and Permeation own effects
        register(new com.newuniverse.nusmp.vfx.client.layer.MirrorFxLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.PermeationFxLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.TransmutationLayer());     // 1.93.0 magic rebuild
        register(new com.newuniverse.nusmp.vfx.client.layer.RecombinationLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.ModificationLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.WaterFxLayer());           // 1.93.0: the Water / Dark / Lightning / Flame rebuild
        register(new com.newuniverse.nusmp.vfx.client.layer.DarkFxLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.LightningFxLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.FlameFxLayer());
        // 1.93.0 nature-magic rebuild: Tree, Vine, Plant, Poison Plant, Mud and Mucus Magic
        register(new com.newuniverse.nusmp.vfx.client.layer.TreeLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.VineLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.PlantLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.PoisonPlantLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.MudLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.MucusLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.ManaZoneLayer());              // 1.99.0: the Mana Zone and the Grand Magic Zones
        register(new com.newuniverse.nusmp.vfx.client.layer.UpgradeSignatureLayer());      // 1.102.0: the signature effects of the upgraded weapons' acts
        register(new com.newuniverse.nusmp.vfx.client.layer.DeckSummonLayer());              // 1.110.0: the card summon of the Deck of Many Things
        com.newuniverse.nusmp.client.storm.StormClientFx.install();                              // Storm Magic: the splash particles (custom particles only) of a running storm
        register(new com.newuniverse.nusmp.vfx.client.layer.StormLayer());                    // Storm Magic (Fairy Tail): the storm cell, its rain and hail, the bolts, the gust front and the surge
        // Wind in the Minecraft style (pixel-anime): registered after WindMagicLayer / WindSpellLayer on purpose, it takes Slash, Ring, Emperor, Gale and Gust Lane over from them (V1 hands them back); Tornado and Zephyr stay with WindSpellLayer
        register(new com.newuniverse.nusmp.vfx.client.layer.WindMcLayer());
        // Water in the hybrid style (3D toon cores wrapped in glass): registered after WaterFxLayer / WaterMagicLayer on purpose, it takes Roar / Javelin, Cradle / Nest, Burst, Splash and Ring over from them (V1 hands them back); the old WaterSpellLayer shapes are not in it
        register(new com.newuniverse.nusmp.vfx.client.layer.WaterBcLayer());
        register(new com.newuniverse.nusmp.vfx.client.layer.FireV2Layer());
        // Star Magic nodes (anime cel + volumetric glow): its own seven shapes (STAR_NODE ... STAR_DOME), appended after STAR_MAGIC; STAR_MAGIC and ElementIdentityLayer are untouched. V1 draws them as the old starlight beam.
        register(new com.newuniverse.nusmp.vfx.client.layer.StarMagicLayer());
    }

    public void register(AbstractVfxLayer layer) { for (VfxShape s : layer.shapes()) layers.put(s, layer); }

    /**
     * Called from the network handler on the client thread. 1.27.0: guarded: one effect's layer that throws is logged once per shape
     * (core.magic.profile.SafeCast) and skipped; it can no longer crash the client.
     */
    public void spawn(VfxPayload p) {
        long t0 = PerfProbe.begin();
        try {
            spawnUnguarded(p);
        } catch (RuntimeException | LinkageError e) {
            com.newuniverse.nusmp.core.magic.profile.SafeCast.clientFault("vfx:spawn:" + p.shapeType().name(), e);
        }
        PerfProbe.end(SPAWN_SECTION, t0);
    }

    private void spawnUnguarded(VfxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        VfxShape shape = p.shapeType();
        if (FxRenderer.handles(shape)) {
            if (mc.options.particles().get() == ParticleStatus.MINIMAL) dustFallback(p, 0xFFFFFFFF);
            else FxRenderer.spawn(p);
            return;
        }
        VfxDefinitions.Def def = VfxDefinitions.get(shape);
        AbstractVfxLayer layer = layers.get(shape);
        if (layer != null && !layer.handles(shape)) layer = null;                  // wind pixel look off (V1): FX_WIND_GUST is a shape without a layer again
        int color = p.color() != 0 ? p.color() : def != null && def.color() != 0 ? def.color()
                : layer != null ? layer.defaultColor(shape) : 0xFFFFFFFF;

        // Minimal particles (or a shape with no layer yet): cheap vanilla dust only.
        if (mc.options.particles().get() == ParticleStatus.MINIMAL || layer == null) {
            dustFallback(p, color);
            return;
        }
        int duration = p.duration() > 0 ? p.duration() : def != null && def.duration() > 0 ? def.duration() : layer.defaultDuration(shape);
        int cap = Math.min(MAX_ACTIVE, PerfSettings.vfxQuality().maxActive);      // 64 on high (as before), fewer on medium and low
        while (active.size() >= cap) active.remove(0);
        VfxInstance inst = new VfxInstance(p, layer, color, duration, mc.level);
        active.add(inst);
        layer.onSpawn(inst);
        VfxSounds.play(shape, p.from(), inst.power);
        if (def != null && def.shake() > 0 && inst.power >= 1.5f && shape != VfxShape.MAGIC_CIRCLE_EXPLOSION && shape != VfxShape.FLAME_EXPLOSION) {
            VfxShake.add(p.to(), def.shake() * inst.power, 8);
        }
    }

    public void tick() {
        long t0 = PerfProbe.begin();
        VfxShake.tick();
        Iterator<VfxInstance> it = active.iterator();
        while (it.hasNext()) {
            VfxInstance inst = it.next();
            try {
                inst.layer.onTick(inst);
            } catch (RuntimeException | LinkageError e) {            // 1.27.0: a layer that throws is dropped, not fatal
                com.newuniverse.nusmp.core.magic.profile.SafeCast.clientFault("vfx:tick:" + inst.shape.name(), e);
                it.remove();
                continue;
            }
            inst.tick();
            if (inst.done()) it.remove();
        }
        PerfProbe.end(TICK_SECTION, t0);
    }

    public void clear() { active.clear(); }

    /** How many effects are running (for the report). */
    public int activeCount() { return active.size(); }

    /** The reach of an effect around its end points (blocks): the part of it that can be near the camera when its points are not. Generous, so a big zone is never taken for far away. */
    private static double reach(VfxInstance inst) { return Math.min(192.0, 4.0 + 4.0 * inst.power); }

    /** Draw everything (render thread). */
    public void render(net.minecraft.client.Camera camera, Matrix4f modelView, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (active.isEmpty() || mc.level == null) return;
        long t0 = PerfProbe.begin();
        float base = mc.options.particles().get() == ParticleStatus.DECREASED ? 0.5f : 1f;
        VfxRenderContext ctx = new VfxRenderContext(camera, modelView, partialTick, mc.level, base);
        VfxQuality quality = PerfSettings.vfxQuality();
        int n = active.size();
        if (distance.length < n) {
            distance = new double[n * 2];
            lod = new float[n * 2];
            left = new boolean[n * 2];
        }
        int culled = plan(ctx, quality, n, base);
        WaterBcFrame.plan(active, distance, lod, left, n, quality.frameVertices);          // the water hybrid: tiers under the per-frame ceilings, empties the mesh buffer of the last frame
        int drawn = 0, thinned = 0, skipped = 0;
        for (int i = 0; i < n; i++) {
            VfxInstance inst = active.get(i);
            if (lod[i] <= 0f) continue;                                  // too far to see
            if (left[i]) {                                               // the vertex budget of the frame went to nearer effects
                skipped++;
                continue;
            }
            ctx.detail = base * lod[i];
            if (lod[i] < 1f) thinned++;
            buffer.beginActivation(inst.layer.vertexBudget(inst.shape));
            long lt = PerfProbe.begin();
            try {
                inst.layer.render(inst, ctx, buffer);
            } catch (RuntimeException | LinkageError e) {           // 1.27.0: a layer that throws ends its effect (logged once per shape), the frame goes on
                com.newuniverse.nusmp.core.magic.profile.SafeCast.clientFault("vfx:render:" + inst.shape.name(), e);
                inst.expire();
            }
            if (lt != 0L) PerfProbe.end(layerSection(inst.layer), lt);
            drawn++;
        }
        // Vertices are pre-transformed into view space, so the model-view uniform must be identity.
        var mvStack = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        mvStack.pushMatrix();
        mvStack.identity();
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        WaterBcDraw.flush(modelView);                                          // the water hybrid's 3D cores first (they write the depth the glass sprites are tested against)
        buffer.flush(modelView);
        mvStack.popMatrix();
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        VfxStats.frame(n, drawn, thinned, culled, skipped, buffer.lastFlushVertices());
        PerfProbe.end(RENDER_SECTION, t0);
    }

    /**
     * Decides, for every running effect, how detailed it is drawn this frame: {@code lod[i]} (0 = culled), and, when the quality caps the vertices of a frame and the effects would not fit,
     * which of the farther ones are left out ({@code left[i]}; the drawing order is not changed, only the set). The Time Magic layer is exempt from all of it: it is the standard the others are
     * built to. Returns how many effects were culled.
     */
    private int plan(VfxRenderContext ctx, VfxQuality quality, int n, float base) {
        int culled = 0;
        int estimate = 0;
        for (int i = 0; i < n; i++) {
            VfxInstance inst = active.get(i);
            left[i] = false;
            if (inst.layer instanceof TimeMagicLayer) {
                lod[i] = 1f;
                distance[i] = 0.0;
                continue;
            }
            Vec3 cam = ctx.camPos;
            double d = Math.min(inst.from(ctx).distanceToSqr(cam), inst.to(ctx).distanceToSqr(cam));
            double eff = Math.max(0.0, Math.sqrt(d) - reach(inst));
            distance[i] = eff;
            float l = quality.detailAt(eff);
            lod[i] = l;
            if (l <= 0f) culled++;
            else estimate += Math.min(inst.layer.vertexBudget(inst.shape), VfxVertexBuffer.MAX_VERTICES_PER_ACTIVATION);
        }
        if (estimate > quality.frameVertices) {                         // the effects do not all fit: the nearest ones are kept until the allowance is spent
            int room = quality.frameVertices;
            boolean[] taken = new boolean[n];
            for (int round = 0; round < n; round++) {
                int best = -1;
                for (int i = 0; i < n; i++) if (!taken[i] && lod[i] > 0f && (best < 0 || distance[i] < distance[best])) best = i;
                if (best < 0) break;
                taken[best] = true;
                VfxInstance inst = active.get(best);
                if (inst.layer instanceof TimeMagicLayer) continue;
                int cost = Math.min(inst.layer.vertexBudget(inst.shape), VfxVertexBuffer.MAX_VERTICES_PER_ACTIVATION);
                if (cost > room) left[best] = true;
                else room -= cost;
            }
        }
        return culled;
    }

    private static PerfProbe.Section layerSection(AbstractVfxLayer layer) {
        return LAYER_SECTIONS.computeIfAbsent(layer, l -> PerfProbe.section("c.vfx.layer." + l.getClass().getSimpleName()));
    }

    private void dustFallback(VfxPayload p, int color) {
        var level = Minecraft.getInstance().level;
        Vector3f rgb = new Vector3f(((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f);
        DustParticleOptions dust = new DustParticleOptions(rgb, 1.2f);
        Vec3 a = p.from(), b = p.to();
        for (int i = 0; i <= 6; i++) {
            Vec3 q = a.lerp(b, i / 6.0);
            level.addParticle(dust, q.x, q.y + 0.5, q.z, 0, 0, 0);
        }
    }
}
