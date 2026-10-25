package com.newuniverse.nusmp.client.manamethod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.core.magic.grimoire.GrimoireCarry;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.mana.method.BodyRuneRules;
import com.newuniverse.nusmp.mana.method.BrushRules;
import com.newuniverse.nusmp.mana.method.RuneCharge;
import com.newuniverse.nusmp.mana.method.RuneElement;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Frame;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Params;
import com.newuniverse.nusmp.mana.method.RuneGlyphLayout;
import com.newuniverse.nusmp.mana.method.RuneGlyphLayout.Layout;
import com.newuniverse.nusmp.mana.method.RuneHoldView;
import com.newuniverse.nusmp.mana.method.RuneRules;
import com.newuniverse.nusmp.mana.method.RuneNet;
import com.newuniverse.nusmp.mana.method.RuneSpec;
import com.newuniverse.nusmp.particle.fx.FxCatalog;
import com.newuniverse.nusmp.particle.fx.FxConfig;
import com.newuniverse.nusmp.particle.fx.FxMath;
import com.newuniverse.nusmp.particle.fx.FxTypes;
import com.newuniverse.nusmp.particle.fx.TintedParticleOptions;
import com.newuniverse.nusmp.particle.fx.TintedParticleType;
import com.newuniverse.nusmp.perf.PerfBudgets;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.perf.VfxQuality;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.VfxManager;
import com.newuniverse.nusmp.vfx.client.layer.RuneWritingLayer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The client-only holder of the Mana Method rune. The picture of the rune is the vfx layer {@link RuneWritingLayer} (runes written in the air by a pen, strip quads through the VFX vertex buffer,
 * so the effect quality, the distance levels and the frame vertex cap apply); this class keeps the tiny state of the runes near the camera (at most {@link RuneGeneratorUtil#MAX_RUNES_DRAWN}) and
 * adds the two things the layer cannot:
 * <ul>
 *   <li><b>the real grimoire</b>: one open, glowing grimoire per rune, the mod's own book model (the item renderer, as the summoned book of {@code GrimoireFloatClient}: the open V, the glowing pages
 *       toward its owner), hovering at the caster's side while the rune is written; only within {@value RuneGeneratorUtil#BOOK_DISTANCE} blocks of the camera, only at full detail;</li>
 *   <li><b>sparks</b>: while a pen writes, at most {@link RuneGlyphLayout#MAX_SPARKS_PER_TICK} mana motes per rune per tick fly off its tip, each batch asking the client particle budget
 *       ({@code PerfBudgets.clientParticles}), thinned by the effect quality and the distance; none beyond 40 blocks.</li>
 * </ul>
 * Cost: a tick handler that returns at once with no rune; per rune per tick, while it is being written, one layout fill into a preallocated array (the same maths the layer runs) and at most three particles;
 * per frame, one item render per rune near the camera. Nothing is allocated per frame or per tick but what the particle engine takes. The old dot-ring particles of the first version are gone.
 */
public final class RuneClient {
    private RuneClient() {}

    private static final class CRune {
        RuneSpec spec;
        Params params;
        Frame frame;
        long born;
        int charge, lifetime;
        boolean own;
        double yawDeg;
        ItemStack book;
        final Layout scratch = new Layout();
        ParticleOptions spark;
        final RandomSource random = RandomSource.create();
        /** 3.5.0: the view of a HELD rune (null for the fixed rune of the old cast), the entity id of the caster, whether the bolt's cast packet has arrived. */
        RuneHoldView view;
        int ownerId;
        boolean launched;
    }

    private static final List<CRune> RUNES = new ArrayList<>();
    private static final double[] BOOK_AT = new double[3];
    private static final PoseStack POSE = new PoseStack();                              // one stack, reused (push and pop in pairs)
    /** The open grimoire of each element, built once (a render-only copy at the full V). */
    private static final ItemStack[] BOOKS = new ItemStack[RuneElement.values().length];
    private static final double SPARK_DISTANCE = 40.0;
    /** Scratch for the staged drawing (the client tick runs on one thread). */
    private static final double[] PROG = new double[RuneGlyphLayout.RING_GROUPS], SCALE = new double[RuneGlyphLayout.RING_GROUPS], WRITTEN = new double[RuneGlyphLayout.RING_GROUPS];
    /** A released rune whose cast never arrives (the cast failed, or its packet was dropped) goes away this many ticks after the release (the wind-up is 4 to 7). */
    private static final int ORPHAN_TICKS = 14;

    // ---------------------------------------------------------------- 3.6.0: painted runes (the Brush Rune on a surface, the Body Rune on a body)
    /** A rune painted on a surface: it is written once (24 ticks), stays, flares when it fires and fades. Not a book rune: nothing is held and no grimoire is drawn. */
    private static final class CPaint {
        int id;
        RuneHoldView view;
        long born;
        int life;
    }

    /** The runes on one body: the chest rune and, from the second ring tier, the rune at the feet. They follow the body every frame. */
    private static final class CBody {
        int ownerId;
        RuneHoldView chest, feet;
        long born;
        int life;
        double lastX = Double.NaN, lastY, lastZ, lastYaw;
    }

    private static final List<CPaint> PAINTS = new ArrayList<>();
    private static final List<CBody> BODIES = new ArrayList<>();
    private static final double[] LOOK = new double[3], NORMAL = new double[3];
    /** The picture of a painted rune takes this many ticks to be written. */
    private static final int PAINT_TICKS = 24;
    /** The scale of the chest rune and of the rune at the feet, relative to a rune's own size. */
    private static final float CHEST_SCALE = 0.3f, FEET_SCALE = 0.85f;

    public static void init() {
        NeoForge.EVENT_BUS.addListener(RuneClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(RuneClient::onRender);
        NeoForge.EVENT_BUS.addListener(RuneClient::onRenderBodies);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> { RUNES.clear(); PAINTS.clear(); BODIES.clear(); RuneHoldView.forgetAll(); com.newuniverse.nusmp.mana.method.RuneStateGate.clear(); });
    }

    private static int paintedDrawn() {
        int n = PAINTS.size();
        for (CBody b : BODIES) n += b.feet != null ? 2 : 1;
        return n;
    }

    /** A brush packet arrived (3.6.0): a painted rune appeared, fired or is gone. Validated; a rune farther than {@value BrushRules#PAINT_DISTANCE} blocks or past the cap of {@value BrushRules#MAX_PAINTED_DRAWN} is not started. */
    public static void onBrush(RuneNet.BrushPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !RuneNet.validBrush(p)) return;
        long now = mc.level.getGameTime();
        for (int i = PAINTS.size() - 1; i >= 0; i--) if (now - PAINTS.get(i).born > PAINTS.get(i).life + RuneHoldView.FADE) PAINTS.remove(i);
        if (p.phase() != RuneNet.PHASE_APPEAR) {
            for (CPaint c : PAINTS) {
                if (c.id != p.id()) continue;
                float age = (float) (now - c.born);
                if (p.phase() == RuneNet.PHASE_FIRED) {                                       // the bolt left: the rune flares and goes
                    c.view.launchAge = age;
                    c.view.endAge = age + RuneRules.FLARE_TICKS + RuneHoldView.FADE;
                } else {
                    c.view.endAge = Math.min(c.view.endAge, age + RuneHoldView.FADE);
                }
                c.life = (int) Math.ceil(c.view.endAge);
                return;
            }
            return;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double dx = p.x() - cam.x, dy = p.y() - cam.y, dz = p.z() - cam.z;
        if (dx * dx + dy * dy + dz * dz > BrushRules.PAINT_DISTANCE * BrushRules.PAINT_DISTANCE || paintedDrawn() >= BrushRules.MAX_PAINTED_DRAWN) return;
        for (CPaint c : PAINTS) if (c.id == p.id()) return;
        RuneSpec spec = RuneSpec.unpack(p.spec());
        BrushRules.normal(p.face(), NORMAL);
        BrushRules.lookFor(NORMAL[0], NORMAL[1], NORMAL[2], BrushRules.yawDegrees(p.yaw()), LOOK);
        RuneHoldView view = new RuneHoldView(-1, spec, RuneCharge.DEFAULT_MIN_HOLD, RuneCharge.DEFAULT_MAX_HOLD, RuneGeneratorUtil.frameFacing(p.x(), p.y(), p.z(), LOOK[0], LOOK[1], LOOK[2]));
        view.follow = false;
        view.paintTicks = PAINT_TICKS;
        view.endAge = p.ticks();
        CPaint c = new CPaint();
        c.id = p.id();
        c.view = view;
        c.born = now;
        c.life = p.ticks();
        PAINTS.add(c);
        float scale = BrushRules.pictureScale(p.radiusTenths() / 10.0, RuneGeneratorUtil.paramsFor(spec).radius());
        Vec3 from = new Vec3(p.x(), p.y(), p.z());
        VfxManager.get().spawn(new VfxPayload(VfxShape.RUNE_WRITING.ordinal(), from, from.add(LOOK[0], LOOK[1], LOOK[2]), 0xFF000000 | spec.element().tint, p.ticks() + 1, scale, -1, RuneHoldView.seedOf(spec.pack(), RuneHoldView.register(view))));
    }

    /** A body packet arrived (3.6.0): the runes were written on a body (START) or faded (END). The pictures follow the body every frame; at most {@value BrushRules#MAX_PAINTED_DRAWN} painted runes are drawn together. */
    public static void onBody(RuneNet.BodyPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !RuneNet.validBody(p)) return;
        long now = mc.level.getGameTime();
        for (int i = BODIES.size() - 1; i >= 0; i--) {
            CBody b = BODIES.get(i);
            if (now - b.born > b.life + RuneHoldView.FADE) { BODIES.remove(i); continue; }
            if (b.ownerId != p.ownerId()) continue;
            if (p.phase() == RuneNet.PHASE_END) {
                float end = (float) (now - b.born) + RuneHoldView.FADE;
                b.chest.endAge = Math.min(b.chest.endAge, end);
                if (b.feet != null) b.feet.endAge = Math.min(b.feet.endAge, end);
                b.life = (int) Math.ceil(Math.min(b.life, end));
                return;
            }
            BODIES.remove(i);                                                                 // a new cast on the same body: the old pictures end, new ones are written
            b.chest.endAge = Math.min(b.chest.endAge, (float) (now - b.born) + 1f);
            if (b.feet != null) b.feet.endAge = Math.min(b.feet.endAge, (float) (now - b.born) + 1f);
        }
        if (p.phase() != RuneNet.PHASE_START || p.ticks() < 1) return;
        Entity owner = mc.level.getEntity(p.ownerId());
        int pics = BodyRuneRules.pictures(p.tier());
        if (owner == null || paintedDrawn() + pics > BrushRules.MAX_PAINTED_DRAWN) return;
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        if (owner.position().distanceToSqr(cam) > BrushRules.PAINT_DISTANCE * BrushRules.PAINT_DISTANCE) return;
        RuneSpec spec = RuneSpec.unpack(p.spec());
        CBody b = new CBody();
        b.ownerId = p.ownerId();
        b.born = now;
        b.life = p.ticks();
        b.chest = bodyView(owner, spec, p.ticks(), CHEST_SCALE, 1.15);
        if (pics > 1) b.feet = bodyView(owner, spec, p.ticks(), FEET_SCALE, 0.05);
        BODIES.add(b);
    }

    private static RuneHoldView bodyView(Entity owner, RuneSpec spec, int ticks, float scale, double height) {
        RuneHoldView view = new RuneHoldView(owner.getId(), spec, 8, 40, RuneGeneratorUtil.frameFacing(owner.getX(), owner.getY() + height, owner.getZ(), 0, -1, 0));
        view.follow = false;
        view.paintTicks = PAINT_TICKS;
        view.endAge = ticks;
        Vec3 from = owner.position().add(0, height, 0);
        VfxManager.get().spawn(new VfxPayload(VfxShape.RUNE_WRITING.ordinal(), from, from.add(0, -1, 0), 0xFF000000 | spec.element().tint, ticks + 1, scale, owner.getId(), RuneHoldView.seedOf(spec.pack(), RuneHoldView.register(view))));
        return view;
    }

    /** Before the frame's entities are drawn: the pictures on a body are moved to where the body is NOW (partial tick), so they do not lag a tick behind a running player. Only when the body moved or turned. */
    private static void onRenderBodies(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || BODIES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        SafeCast.client("mana-method-body", () -> {
            float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            for (CBody b : BODIES) {
                Entity o = mc.level.getEntity(b.ownerId);
                if (o == null) continue;
                Vec3 pos = o.getPosition(partial);
                double yaw = o instanceof net.minecraft.world.entity.LivingEntity le ? Mth.rotLerp(partial, le.yBodyRotO, le.yBodyRot) : o.getYRot();
                if (!Double.isNaN(b.lastX) && Math.abs(pos.x - b.lastX) < 0.002 && Math.abs(pos.y - b.lastY) < 0.002 && Math.abs(pos.z - b.lastZ) < 0.002 && Math.abs(yaw - b.lastYaw) < 0.2) continue;
                b.lastX = pos.x;
                b.lastY = pos.y;
                b.lastZ = pos.z;
                b.lastYaw = yaw;
                double rad = Math.toRadians(yaw), fx = -Math.sin(rad), fz = Math.cos(rad);        // where the body faces
                b.chest.frame = RuneGeneratorUtil.frameFacing(pos.x + fx * 0.32, pos.y + 1.15, pos.z + fz * 0.32, -fx, 0, -fz);          // seen by someone in front of the body
                if (b.feet != null) {
                    BrushRules.lookFor(0, 1, 0, yaw, LOOK);
                    b.feet.frame = RuneGeneratorUtil.frameFacing(pos.x, pos.y + 0.05, pos.z, LOOK[0], LOOK[1], LOOK[2]);
                }
            }
        });
    }

    /** Once a tick: the painted runes that ran out go; a body whose owner left or died fades. */
    private static void tickPainted(Minecraft mc, Level level) {
        long now = level.getGameTime();
        for (int i = PAINTS.size() - 1; i >= 0; i--) if (now - PAINTS.get(i).born > PAINTS.get(i).life + RuneHoldView.FADE) PAINTS.remove(i);
        for (int i = BODIES.size() - 1; i >= 0; i--) {
            CBody b = BODIES.get(i);
            long age = now - b.born;
            if (age > b.life + RuneHoldView.FADE) { BODIES.remove(i); continue; }
            Entity o = level.getEntity(b.ownerId);
            if (o == null || !o.isAlive()) {
                float end = (float) age + RuneHoldView.FADE;
                if (b.chest.endAge > end) b.chest.endAge = end;
                if (b.feet != null && b.feet.endAge > end) b.feet.endAge = end;
                b.life = (int) Math.min(b.life, Math.ceil(end));
            }
        }
    }

    /** A rune packet arrived: validate it, start the writing effect and keep the small state the book and the sparks need. */
    public static void onRune(RuneNet.RunePayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !RuneNet.valid(p)) return;
        long now = mc.level.getGameTime();
        for (int i = RUNES.size() - 1; i >= 0; i--) if (now - RUNES.get(i).born > RUNES.get(i).lifetime) RUNES.remove(i);
        CRune held = heldRuneOf(p.ownerId());
        if (held != null) {                                                                  // the rune of the hold is the rune of this cast: it stops following, finishes, and flares when the bolt leaves
            long age = now - held.born;
            if (!held.view.released()) held.view.release(age, (int) age);                    // the release packet was dropped: the hold lasted as long as the rune has been up
            held.view.follow = false;
            held.frame = held.view.frame = RuneGeneratorUtil.frameFacing(p.x(), p.y(), p.z(), p.lx(), p.ly(), p.lz());
            held.yawDeg = Math.toDegrees(Math.atan2(-p.lx(), p.lz()));
            held.view.launchAge = age + p.charge();
            held.view.endAge = held.view.launchAge + RuneWritingLayer.TAIL;
            held.lifetime = (int) Math.ceil(held.view.endAge);
            held.launched = true;
            return;
        }
        if (RUNES.size() >= RuneGeneratorUtil.MAX_RUNES_DRAWN) return;                      // at most six at once
        RuneSpec spec = RuneSpec.unpack(p.spec());
        CRune r = new CRune();
        r.spec = spec;
        r.params = RuneGeneratorUtil.paramsFor(spec);
        r.frame = RuneGeneratorUtil.frameFacing(p.x(), p.y(), p.z(), p.lx(), p.ly(), p.lz());
        r.born = now;
        r.charge = p.charge();
        r.lifetime = p.charge() + RuneWritingLayer.TAIL;
        r.own = mc.player != null && mc.player.getId() == p.ownerId();
        r.yawDeg = Math.toDegrees(Math.atan2(-p.lx(), p.lz()));
        r.book = book(spec.element());
        r.spark = options(RuneElement.RING_PARTICLE, spec.element().tint, 0.9f);
        RUNES.add(r);
        Vec3 from = new Vec3(p.x(), p.y(), p.z());
        VfxManager.get().spawn(new VfxPayload(VfxShape.RUNE_WRITING.ordinal(), from, from.add(p.lx(), p.ly(), p.lz()), 0xFF000000 | spec.element().tint, r.lifetime, 1.0f, -1, spec.pack()));
    }

    /** The rune of a hold that is still waiting for its cast (not launched), of one caster; null when there is none. */
    private static CRune heldRuneOf(int ownerId) {
        for (CRune r : RUNES) if (r.view != null && !r.launched && r.ownerId == ownerId && r.view.endAge < 0f) return r;
        return null;
    }

    /**
     * A hold packet arrived (3.5.0): a page is being held (HOLD), was let go after N ticks (RELEASE), or was given up (CANCEL). HOLD starts the rune that is written while the key is down;
     * RELEASE stops its clock (and starts the rune now when the HOLD never arrived, as for a tap shorter than a tick); CANCEL fades it. Validated like the rune packet.
     */
    public static void onHold(RuneNet.HoldPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !RuneNet.validHold(p)) return;
        long now = mc.level.getGameTime();
        for (int i = RUNES.size() - 1; i >= 0; i--) if (now - RUNES.get(i).born > RUNES.get(i).lifetime) RUNES.remove(i);
        CRune r = heldRuneOf(p.ownerId());
        switch (p.phase()) {
            case RuneNet.PHASE_HOLD -> {
                if (r != null) endRune(r, now);                                               // a new hold: the old unfinished one is over
                start(mc, p, now, 0);
            }
            case RuneNet.PHASE_RELEASE -> {
                if (r == null) r = start(mc, p, now, p.held());
                if (r != null && !r.view.released()) r.view.release(now - r.born, p.held());
            }
            default -> { if (r != null) endRune(r, now); }                                   // CANCEL
        }
    }

    private static void endRune(CRune r, long now) {
        r.view.endAge = (float) (now - r.born) + RuneHoldView.FADE;
        r.lifetime = (int) Math.ceil(r.view.endAge);
    }

    /** Starts the rune of a held page in front of its caster; null when the caster is not known here, or six runes are up. */
    private static CRune start(Minecraft mc, RuneNet.HoldPayload p, long now, int held) {
        Entity owner = mc.level.getEntity(p.ownerId());
        if (owner == null || RUNES.size() >= RuneGeneratorUtil.MAX_RUNES_DRAWN) return null;
        RuneSpec spec = RuneSpec.unpack(p.spec());
        RuneHoldView view = new RuneHoldView(p.ownerId(), spec, p.minHold(), p.maxHold(), null);
        CRune r = new CRune();
        r.spec = spec;
        r.params = RuneGeneratorUtil.paramsFor(spec);
        r.born = now;
        r.view = view;
        r.ownerId = p.ownerId();
        r.charge = view.maxHold;
        r.lifetime = view.lifetime();
        r.own = mc.player != null && mc.player.getId() == p.ownerId();
        r.book = book(spec.element());
        r.spark = options(RuneElement.RING_PARTICLE, spec.element().tint, 0.9f);
        follow(mc, r, owner);
        RUNES.add(r);
        Vec3 from = new Vec3(r.frame.cx(), r.frame.cy(), r.frame.cz());
        Vec3 look = owner.getLookAngle();
        VfxManager.get().spawn(new VfxPayload(VfxShape.RUNE_WRITING.ordinal(), from, from.add(look.x, look.y, look.z), 0xFF000000 | spec.element().tint, view.lifetime(), 1.0f, -1, RuneHoldView.seedOf(spec.pack(), RuneHoldView.register(view))));
        return r;
    }

    /** Puts the rune where the server puts it: {@link RuneRules#RUNE_DISTANCE} in front of the caster's eye, nearer when a wall is in the way. One short ray per held rune per tick (at most six). */
    private static void follow(Minecraft mc, CRune r, Entity owner) {
        Vec3 eye = owner.getEyePosition(), look = owner.getLookAngle().normalize();
        Vec3 center = eye.add(look.scale(RuneRules.RUNE_DISTANCE));
        HitResult wall = mc.level.clip(new ClipContext(eye, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        if (wall.getType() != HitResult.Type.MISS) center = eye.add(look.scale(Math.max(0.8, wall.getLocation().distanceTo(eye) - 0.35)));
        r.frame = RuneGeneratorUtil.frameFacing(center.x, center.y, center.z, look.x, look.y, look.z);
        r.view.frame = r.frame;
        r.yawDeg = Math.toDegrees(Math.atan2(-look.x, look.z));
    }

    private static ItemStack book(RuneElement e) {
        ItemStack s = BOOKS[e.ordinal()];
        if (s == null) {
            MagicType magic = switch (e) {
                case FIRE -> MagicType.FLAME;
                case WATER -> MagicType.WATER;
                case WIND -> MagicType.WIND;
                case EARTH -> MagicType.EARTH;
                case LIGHTNING -> MagicType.LIGHTNING;
                case PLANT -> MagicType.PLANT;
            };
            s = GrimoireItem.withOpenView(GrimoireItem.createUnbound(5, magic, null), GrimoireCarry.OPEN_STEPS);
            BOOKS[e.ordinal()] = s;
        }
        return s;
    }

    /** The mana mote of the catalog in the rune's colour; plain dust of that colour when the custom particles are off or not bound yet. */
    private static ParticleOptions options(String id, int rgb, float scale) {
        if (FxConfig.clientCustom()) {
            TintedParticleType type = FxTypes.type(id);
            FxCatalog.Def d = FxCatalog.byId(id);
            if (type != null && d != null) return new TintedParticleOptions(type, d.tint() ? (rgb & 0xFFFFFF) : -1, scale, false, d.tint());
        }
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), 0.8f * scale);
    }

    // ---------------------------------------------------------------- sparks off the pens
    private static void onClientTick(ClientTickEvent.Post e) {
        if (RUNES.isEmpty() && PAINTS.isEmpty() && BODIES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) { RUNES.clear(); PAINTS.clear(); BODIES.clear(); return; }
        if (!PAINTS.isEmpty() || !BODIES.isEmpty()) SafeCast.client("mana-method-painted", () -> tickPainted(mc, level));
        if (RUNES.isEmpty()) return;
        SafeCast.client("mana-method-rune", () -> tick(mc, level));
    }

    private static void tick(Minecraft mc, Level level) {
        long now = level.getGameTime();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        VfxQuality q = PerfSettings.vfxQuality();
        for (int k = RUNES.size() - 1; k >= 0; k--) {
            CRune r = RUNES.get(k);
            long age = now - r.born;
            if (age < 0 || age > r.lifetime) { RUNES.remove(k); continue; }
            if (r.view != null) {
                Entity owner = r.view.follow ? level.getEntity(r.ownerId) : null;
                if (r.view.follow && (owner == null || !owner.isAlive())) { endRune(r, now); continue; }          // the caster left: the rune fades where it is
                if (owner != null) follow(mc, r, owner);
                if (r.view.released() && !r.launched && r.view.endAge < 0f && age - r.view.releaseAge > ORPHAN_TICKS) { endRune(r, now); continue; }      // no cast followed the release
            } else if (age > r.charge) {
                continue;                                                                       // only the pen throws sparks
            }
            double dx = r.frame.cx() - cam.x, dy = r.frame.cy() - cam.y, dz = r.frame.cz() - cam.z, d2 = dx * dx + dy * dy + dz * dz;
            if (!r.own && d2 > SPARK_DISTANCE * SPARK_DISTANCE) continue;
            float detail = Math.min(q.detail, RuneGeneratorUtil.distanceDetailSq(d2, r.own));
            if (!(detail > 0.45f)) continue;                                                    // the lower levels draw no sparks
            if (r.view != null) {
                r.view.progress(age, PROG, SCALE, WRITTEN);
                RuneGlyphLayout.fillStaged(r.scratch, r.spec, r.params, r.frame, age, PROG, SCALE, r.view.flare(age), RuneGlyphLayout.LEVEL_FULL, cam.x, cam.y, cam.z);
            } else {
                RuneGlyphLayout.fill(r.scratch, r.spec, r.params, r.frame, age, r.charge, RuneGlyphLayout.LEVEL_FULL, cam.x, cam.y, cam.z);
            }
            int want = Math.min(RuneGlyphLayout.MAX_SPARKS_PER_TICK, r.scratch.pens);
            int n = PerfBudgets.clientParticles(now, want);
            for (int i = 0; i < n; i++) {
                double x = r.scratch.penPos[i * 3], y = r.scratch.penPos[i * 3 + 1], z = r.scratch.penPos[i * 3 + 2];
                level.addParticle(r.spark, x, y, z, FxMath.clampSpeed((r.random.nextDouble() - 0.5) * 0.06), FxMath.clampSpeed(0.02 + r.random.nextDouble() * 0.04), FxMath.clampSpeed((r.random.nextDouble() - 0.5) * 0.06));
            }
        }
    }

    // ---------------------------------------------------------------- the real grimoire
    private static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || RUNES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        SafeCast.client("mana-method-book", () -> renderBooks(mc, event));
    }

    private static void renderBooks(Minecraft mc, RenderLevelStageEvent event) {
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        long now = mc.level.getGameTime();
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        int drawn = 0;
        boolean started = false;
        MultiBufferSource.BufferSource buffers = null;
        Matrix4fStack modelView = null;
        for (CRune r : RUNES) {
            double age = now - r.born + partial;
            if (age < r.charge * 0.08 || age > r.lifetime - 2) continue;
            double dx = r.frame.cx() - cam.x, dy = r.frame.cy() - cam.y, dz = r.frame.cz() - cam.z, d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > RuneGeneratorUtil.BOOK_DISTANCE * RuneGeneratorUtil.BOOK_DISTANCE) continue;       // the book is drawn within 32 blocks, the caster's own included
            if (!started) {
                started = true;
                modelView = RenderSystem.getModelViewStack();
                modelView.pushMatrix();
                modelView.set(event.getModelViewMatrix());
                RenderSystem.applyModelViewMatrix();
                buffers = mc.renderBuffers().bufferSource();
            }
            RuneGlyphLayout.bookPos(r.frame, r.params.radius(), age, BOOK_AT);
            float in = (float) Math.min(1.0, (age - r.charge * 0.08) / 3.0), out = (float) Math.min(1.0, (r.lifetime - 2 - age) / 3.0);
            float s = GrimoireCarry.HAND.scale() * 0.85f * Mth.clamp(Math.min(in, out), 0f, 1f);
            if (s <= 0.01f) continue;
            POSE.pushPose();
            POSE.translate(BOOK_AT[0] - cam.x, BOOK_AT[1] - cam.y, BOOK_AT[2] - cam.z);
            POSE.mulPose(Axis.YP.rotationDegrees((float) -r.yawDeg + GrimoireCarry.HAND.yaw() + Mth.sin((float) age * 0.05f) * 4f));     // the pages glow toward the caster, as a summoned book's do
            POSE.mulPose(Axis.XP.rotationDegrees(GrimoireCarry.HAND.pitch()));
            POSE.scale(s, s, s);
            mc.getItemRenderer().renderStatic(r.book, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, POSE, buffers, mc.level, 0);
            POSE.popPose();
            if (++drawn >= RuneGeneratorUtil.MAX_RUNES_DRAWN) break;
        }
        if (started) {
            buffers.endBatch();
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }
}
