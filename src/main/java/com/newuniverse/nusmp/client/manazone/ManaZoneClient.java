package com.newuniverse.nusmp.client.manazone;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.newuniverse.nusmp.mana.zone.GrandZoneMath;
import com.newuniverse.nusmp.mana.zone.GrandZoneRegion;
import com.newuniverse.nusmp.mana.zone.ManaZoneMath;
import com.newuniverse.nusmp.mana.zone.ManaZoneNet;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.VfxManager;
import com.newuniverse.nusmp.vfx.client.VfxTextures;
import com.newuniverse.nusmp.vfx.client.layer.ManaZoneGeometry;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The client half of the Mana Zone and the Grand Magic Zones. It holds the TINY state the server synced (a caster's zone: centre, cap, style, tint, start; the
 * region the local player stands in: its shape, theme, effects, harm and boon) and turns it into drawing:
 * <ul>
 *   <li>the zone's shell and ground are two {@code VfxInstance}s of {@code ManaZoneLayer} (200 ticks each, re-spawned just before they end with the ticks already
 *       elapsed, so the growth, computed from the start tick, never restarts); a zone that ends is marked so its instances draw nothing at once, and the finisher plays;</li>
 *   <li>the CASTER'S PERCEPTION: while the local player's own zone is open, the nearest {@value #PERCEPTION_MAX} living things and foreign projectiles inside it
 *       are marked every few ticks and drawn as glowing marks THROUGH WALLS (the depth test is off for this one pass);</li>
 *   <li>the Grand Magic Zone's weather: the scenery instance round the viewer, a fog that closes in, a tint of the fog colour and of the screen, and the storm's bolts.</li>
 * </ul>
 * Budgets: one tick handler that returns at once with no state, one scan every {@value #PERCEPTION_INTERVAL} ticks over an AABB, at most {@value #PERCEPTION_MAX} marks of 4
 * vertices, at most two instances of 400 vertices per zone (the layer stops at its own cap), level of detail by camera distance, nothing drawn beyond 8 radii + 96 blocks.
 */
public final class ManaZoneClient {
    private ManaZoneClient() {}

    private static final int PERCEPTION_MAX = ManaZoneMath.PERCEPTION_MAX, PERCEPTION_INTERVAL = 5, CHUNK = 200;
    private static final ResourceLocation PING = VfxTextures.byName("mana_zone_ping");

    /** The client's copy of one caster's zone. */
    private static final class CZone {
        long serial;
        int ownerId, grow, style, tint;
        float cx, cy, cz, cap;
        long startClient, lastSeen, nextSpawn;
    }

    private static final List<CZone> ZONES = new ArrayList<>();

    // the region the local player stands in
    private static GrandZoneRegion region;
    private static double harm, boon;
    private static long regionSeen, nextEnv;
    private static float lastIntensity;

    // perception
    private static final Entity[] MARKS = new Entity[PERCEPTION_MAX];
    private static final float[] MARK_D2 = new float[PERCEPTION_MAX];
    private static int markCount;
    private static long nextPerceive;
    private static final Predicate<Entity> PERCEIVABLE = e -> e instanceof LivingEntity le ? le.isAlive() : e instanceof Projectile;
    private static final Vector3f TMP = new Vector3f();

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(ManaZoneClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(ManaZoneClient::onRenderStage);
        NeoForge.EVENT_BUS.addListener(ManaZoneClient::onFogColor);
        NeoForge.EVENT_BUS.addListener(ManaZoneClient::onFog);
        NeoForge.EVENT_BUS.addListener(ManaZoneClient::onGuiLayer);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> reset());
    }

    private static void reset() {
        ZONES.clear();
        region = null;
        markCount = 0;
        ManaZoneGeometry.clearEnded();
        com.newuniverse.nusmp.mana.zone.ManaZoneClientGate.clear();
    }

    // ---------------------------------------------------------------- packets
    public static void onZone(ManaZoneNet.ZonePayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        CZone z = null;
        for (CZone c : ZONES) if (c.ownerId == p.ownerId()) { z = c; break; }
        if (p.op() == ManaZoneNet.END || p.op() == ManaZoneNet.COLLAPSE) {
            ManaZoneGeometry.markEnded(p.serial());
            if (z != null) ZONES.remove(z);
            markCount = 0;
            if (p.op() == ManaZoneNet.COLLAPSE) burst(p, true);
            return;
        }
        boolean fresh = z == null || z.serial != p.serial();
        if (z == null) { z = new CZone(); ZONES.add(z); }
        if (fresh) z.nextSpawn = 0;
        z.serial = p.serial();
        z.ownerId = p.ownerId();
        z.grow = p.growTicks();
        z.style = p.style();
        z.tint = p.tint();
        z.cx = p.cx();
        z.cy = p.cy();
        z.cz = p.cz();
        z.cap = p.radiusCap();
        z.startClient = now - p.elapsed();
        z.lastSeen = now;
        if (p.op() == ManaZoneNet.START) burst(p, false);
    }

    public static void onGrandZone(ManaZoneNet.GrandZonePayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (!p.present()) { region = null; return; }
        GrandZoneRegion.Theme[] themes = GrandZoneRegion.Theme.values();
        region = new GrandZoneRegion(p.id(), p.name(), "", p.box(), p.cx(), p.cy(), p.cz(), p.radius(), p.minX(), p.minY(), p.minZ(), p.maxX(), p.maxY(), p.maxZ(),
                themes[Mth.clamp(p.theme(), 0, themes.length - 1)], p.densityTenths() / 10.0, 0, GrandZoneRegion.fromMask(p.effects()), p.tint(), null, false);
        harm = p.harmHundredths() / 100.0;
        boon = p.boonHundredths() / 100.0;
        regionSeen = mc.level.getGameTime();
    }

    private static void burst(ManaZoneNet.ZonePayload p, boolean collapse) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.options.particles().get() == ParticleStatus.MINIMAL) return;
        Entity owner = mc.level.getEntity(p.ownerId());
        Vec3 at = owner != null ? owner.position() : new Vec3(p.cx(), p.cy() - 0.9, p.cz());
        VfxManager.get().spawn(new VfxPayload(VfxShape.MANA_ZONE_BURST.ordinal(), at, new Vec3(collapse ? 1 : 0, p.style(), 0), p.tint(), collapse ? ManaZoneMath.COLLAPSE_TICKS : 24,
                Math.max(4f, p.radiusCap() * (collapse ? 1f : 1f)), -1, p.serial() * 31 + (collapse ? 7 : 3)));
    }

    // ---------------------------------------------------------------- the tick (returns at once with nothing to do)
    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (ZONES.isEmpty() && region == null) { markCount = 0; return; }
        long now = mc.level.getGameTime();
        boolean showParticles = mc.options.particles().get() != ParticleStatus.MINIMAL;
        for (int i = ZONES.size() - 1; i >= 0; i--) {
            CZone z = ZONES.get(i);
            Entity owner = mc.level.getEntity(z.ownerId);
            if (owner == null && now - z.lastSeen > 220) { ZONES.remove(i); continue; }     // out of range and not refreshed: let it go
            if (showParticles && now >= z.nextSpawn) {
                z.nextSpawn = now + CHUNK - 1;
                spawnField(z, owner, now);
            }
            if (z.ownerId == mc.player.getId() && now >= nextPerceive) {
                nextPerceive = now + PERCEPTION_INTERVAL;
                perceive(mc, z, now);
            }
        }
        if (ZONES.isEmpty()) markCount = 0;
        if (region != null) {
            if (now - regionSeen > 300) { region = null; return; }                          // no refresh for 15 s: the server stopped sending (left the dimension)
            double depth = region.depth(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            float fade = (float) GrandZoneMath.edgeFade(depth);
            float intensity = region.has(GrandZoneRegion.Effect.CALM) ? fade * 0.8f : fade;
            if (showParticles && intensity > 0.02f && (now >= nextEnv || Math.abs(intensity - lastIntensity) > 0.15f)) {
                nextEnv = now + 60;
                lastIntensity = intensity;
                VfxManager.get().spawn(new VfxPayload(VfxShape.GRAND_ZONE_ENV.ordinal(), mc.player.position(), new Vec3(now % 1_000_000L, region.theme().ordinal(), region.effectMask()),
                        region.tint(), 62, intensity, mc.player.getId(), region.id().hashCode() * 2654435761L));
            }
        }
    }

    private static void spawnField(CZone z, Entity owner, long now) {
        Vec3 at = owner != null ? owner.position() : new Vec3(z.cx, z.cy - 0.9, z.cz);
        int id = owner != null ? owner.getId() : -1;
        Vec3 data = new Vec3(now - z.startClient, z.style, z.grow);
        VfxManager vfx = VfxManager.get();
        vfx.spawn(new VfxPayload(VfxShape.MANA_ZONE_SHELL.ordinal(), at, data, z.tint, CHUNK, z.cap, id, z.serial));
        vfx.spawn(new VfxPayload(VfxShape.MANA_ZONE_GROUND.ordinal(), at, data, z.tint, CHUNK, z.cap, id, z.serial));
    }

    // ---------------------------------------------------------------- the caster's perception
    /** Marks the nearest living things and foreign projectiles inside the zone: one AABB query, then a squared-distance check, the nearest kept in a fixed array. */
    private static void perceive(Minecraft mc, CZone z, long now) {
        double r = ManaZoneMath.radiusAt(now - z.startClient, z.grow, z.cap);
        markCount = 0;
        if (r < 2.0) return;
        double px = mc.player.getX(), py = mc.player.getY() + 0.9, pz = mc.player.getZ();
        List<Entity> found = mc.level.getEntities(mc.player, new AABB(px - r, py - r, pz - r, px + r, py + r, pz + r), PERCEIVABLE);
        for (int i = 0, n = found.size(); i < n; i++) {
            Entity e = found.get(i);
            if (e instanceof Projectile pr && pr.getOwner() == mc.player) continue;
            double dx = e.getX() - px, dy = e.getY() - py, dz = e.getZ() - pz;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > r * r) continue;
            insert(e, (float) d2);
        }
    }

    private static void insert(Entity e, float d2) {
        int at = markCount;
        if (markCount == PERCEPTION_MAX) {
            if (d2 >= MARK_D2[PERCEPTION_MAX - 1]) return;
            at = PERCEPTION_MAX - 1;
        } else {
            markCount++;
        }
        while (at > 0 && MARK_D2[at - 1] > d2) {
            MARKS[at] = MARKS[at - 1];
            MARK_D2[at] = MARK_D2[at - 1];
            at--;
        }
        MARKS[at] = e;
        MARK_D2[at] = d2;
    }

    private static int markColour(Entity e, int tint) {
        if (e instanceof Projectile) return 0xFFFFE08A;
        if (e instanceof Player || (e instanceof TamableAnimal t && t.isTame())) return 0xFF8CFFB0;
        if (e instanceof Enemy || (e instanceof Mob m && m.getTarget() != null)) return 0xFFFF6A4A;
        return tint;
    }

    private static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || markCount == 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        int tint = 0xFF8A7CFF;
        for (CZone z : ZONES) if (z.ownerId == mc.player.getId()) { tint = z.tint; break; }
        Camera camera = event.getCamera();
        Matrix4f modelView = event.getModelViewMatrix();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = camera.getPosition();
        Vector3f right = new Vector3f(camera.getLeftVector()).negate(), up = new Vector3f(camera.getUpVector());
        float time = (mc.level.getGameTime() + partial) * 0.12f;

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, PING);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();                                                  // through walls: this is the one pass that ignores the terrain
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        var stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        stack.identity();
        RenderSystem.applyModelViewMatrix();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < markCount; i++) {
            Entity e = MARKS[i];
            if (e == null || e.isRemoved()) continue;
            boolean proj = e instanceof Projectile;
            double ex = Mth.lerp(partial, e.xo, e.getX()), ey = Mth.lerp(partial, e.yo, e.getY()) + e.getBbHeight() * 0.5, ez = Mth.lerp(partial, e.zo, e.getZ());
            float size = proj ? 0.5f : Math.max(0.7f, e.getBbHeight() * 0.85f + 0.5f) * (1f + 0.07f * Mth.sin(time + i));
            int argb = markColour(e, tint);
            float dist = (float) Math.sqrt(MARK_D2[i]);
            int a = (int) (255 * Mth.clamp(1.0f - dist / 90f, 0.35f, 0.95f));
            float h = size * 0.5f;
            quad(bb, modelView, (float) (ex - cam.x), (float) (ey - cam.y), (float) (ez - cam.z), right, up, h, (argb >> 16) & 255, (argb >> 8) & 255, argb & 255, a);
        }
        MeshData mesh = bb.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
        stack.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    private static void quad(BufferBuilder bb, Matrix4f mv, float x, float y, float z, Vector3f right, Vector3f up, float h, int r, int g, int b, int a) {
        corner(bb, mv, x - right.x * h - up.x * h, y - right.y * h - up.y * h, z - right.z * h - up.z * h, 0f, 1f, r, g, b, a);
        corner(bb, mv, x + right.x * h - up.x * h, y + right.y * h - up.y * h, z + right.z * h - up.z * h, 1f, 1f, r, g, b, a);
        corner(bb, mv, x + right.x * h + up.x * h, y + right.y * h + up.y * h, z + right.z * h + up.z * h, 1f, 0f, r, g, b, a);
        corner(bb, mv, x - right.x * h + up.x * h, y - right.y * h + up.y * h, z - right.z * h + up.z * h, 0f, 0f, r, g, b, a);
    }

    private static void corner(BufferBuilder bb, Matrix4f mv, float x, float y, float z, float u, float v, int r, int g, int b, int a) {
        mv.transformPosition(x, y, z, TMP);
        bb.addVertex(TMP.x, TMP.y, TMP.z).setUv(u, v).setColor(r, g, b, a);
    }

    // ---------------------------------------------------------------- the weather of a Grand Magic Zone
    private static float edgeFade(Camera camera) {
        if (region == null) return 0f;
        Vec3 c = camera.getPosition();
        return (float) GrandZoneMath.edgeFade(region.depth(c.x, c.y, c.z));
    }

    private static void onFogColor(ViewportEvent.ComputeFogColor e) {
        if (region == null) return;
        float fade = edgeFade(e.getCamera());
        if (fade <= 0f) return;
        float k = (float) GrandZoneMath.skyTint(fade, region.has(GrandZoneRegion.Effect.MANA_STORM), region.density());
        int t = region.tint();
        e.setRed(Mth.lerp(k, e.getRed(), ((t >> 16) & 255) / 255f * 0.8f));
        e.setGreen(Mth.lerp(k, e.getGreen(), ((t >> 8) & 255) / 255f * 0.8f));
        e.setBlue(Mth.lerp(k, e.getBlue(), (t & 255) / 255f * 0.8f));
    }

    private static void onFog(ViewportEvent.RenderFog e) {
        if (region == null || e.getType() != FogType.NONE || e.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        float fade = edgeFade(e.getCamera());
        if (fade <= 0f) return;
        float scale = (float) GrandZoneMath.fogScale(fade, region.has(GrandZoneRegion.Effect.MANA_STORM), region.has(GrandZoneRegion.Effect.WITCH_GLOW));
        if (scale >= 0.999f) return;
        e.setFarPlaneDistance(e.getFarPlaneDistance() * scale);
        e.setNearPlaneDistance(e.getNearPlaneDistance() * scale);
        e.setCanceled(true);                                                              // NeoForge applies the event's fog values only when the event is cancelled
    }

    /** A thin tint of the whole screen in the zone's colour (the "sky tint" of a mana storm), drawn before the HUD. */
    private static void onGuiLayer(RenderGuiLayerEvent.Pre e) {
        if (region == null || !VanillaGuiLayers.CAMERA_OVERLAYS.equals(e.getName())) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        float fade = (float) GrandZoneMath.edgeFade(region.depth(mc.player.getX(), mc.player.getY(), mc.player.getZ()));
        float k = (float) GrandZoneMath.skyTint(fade, region.has(GrandZoneRegion.Effect.MANA_STORM), region.density()) * 0.16f;
        int a = (int) (255 * Mth.clamp(k, 0f, 0.2f));
        if (a <= 1) return;
        int t = region.tint() & 0xFFFFFF;
        e.getGuiGraphics().fill(0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), (a << 24) | t);
    }

    /** The harm and boon the server last sent for the local player (for a HUD or a test), 0 outside a zone. */
    public static double harm() { return region == null ? 0 : harm; }

    public static double boon() { return region == null ? 0 : boon; }

    public static boolean inGrandZone() { return region != null; }
}
