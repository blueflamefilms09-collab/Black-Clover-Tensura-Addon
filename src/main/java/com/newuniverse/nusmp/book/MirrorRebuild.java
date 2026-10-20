package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.ext.AttributeEvents;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.93.0 Mirror Magic rebuild (Black Clover: Gauche Adlai's Mirror Magic; the wiki: mirrors of every shape that reflect light and spells, a Real Double that casts
 * with its original, and, in Union with Eye Magic, the Reflect Iris). The nine old pages keep their ids, order and numbers: they were already a front-line
 * kit (MirrorWorks) and are not rewritten, they gain the Glass Garb (the temporary armour, client.aura.MirrorAura, five seconds after a cast), the rebuilt effects
 * (MIRROR_FX1..3, MirrorFxLayer) and the summon models: every mirror MirrorWorks puts in the world is now the model of {@link PropKind#MIRROR_1} (an ornate standing
 * mirror in one of four shapes) and every flying or falling mirror is {@link PropKind#MIRROR_2} (the blade-edged mirror). The Real Doubles wear the garb
 * (client.MirrorDoubleGarbLayer). Two pages are appended:
 * <ul>
 *   <li><b>Reflect Iris</b> (wiki Union spell, nearest honest version without the Eye magic user): a ring of mirrors shows each foe within 14 blocks that is
 *       looking at you its own gaze; those who look are paralysed for 3 s (shortened on bosses, after the shared control gate) and their magic is jammed, and take
 *       6 x EP scale. Foes looking away are left alone.</li>
 *   <li><b>Glass Garb</b>: 20 s of the Glass Garb. 30 percent of every shot or spell that strikes you is thrown back at whoever sent it, and the rest is cut by 15 percent.</li>
 * </ul>
 */
public final class MirrorRebuild {
    private MirrorRebuild() {}

    static final String K_GARB = "nusmp_mirror_garb_until";

    /** The shape (0 oval, 1 round, 2 arch, 3 diamond) a mirror at a place takes: stable for that place. */
    static int shapeAt(Vec3 at) { return (int) (Math.abs(Math.floor(at.x * 3) * 7 + Math.floor(at.z * 3) * 13 + Math.floor(at.y)) % 4); }

    /** A standing mirror whose middle is {@code centre}, facing {@code lookAt}; size = the half-height in blocks (what the old frame effect took). */
    static void mirrorProp(ServerLevel sl, Vec3 centre, Vec3 lookAt, int life, float size, ServerPlayer owner) {
        MagicProps.spawn(sl, PropKind.MIRROR_1, centre, SpatialWorks.yawToward(centre, lookAt), size * 1.33f, life, shapeAt(centre), owner);
    }

    /** The mirror that stays in front of its owner (bit 16 of the param: Full Reflection's great mirror). */
    static void followingMirror(ServerLevel sl, ServerPlayer p, int life, float size) {
        Vec3 look = p.getViewVector(1f).multiply(1, 0, 1);
        if (look.lengthSqr() < 1.0E-4) look = new Vec3(0, 0, 1);
        Vec3 at = p.position().add(look.normalize().scale(1.9)).add(0, 1.2, 0);
        MagicProps.spawn(sl, PropKind.MIRROR_1, at, p.getYRot(), size * 1.33f, life, 16 | 2, p);
    }

    /** A blade-edged mirror (param 0 flies along {@code yaw}, 1 falls). */
    static void bladeProp(ServerLevel sl, Vec3 at, float yaw, int life, float scale, int param, ServerPlayer owner) {
        MagicProps.spawn(sl, PropKind.MIRROR_2, at, yaw, scale, life, param, owner);
    }

    static void fx(ServerLevel level, VfxShape shape, Vec3 from, Vec3 to, int ticks, float power) { VfxSpawn.send(level, shape, from, to, MirrorWorks.VIOLET, ticks, power); }

    public static void initHooks() {
        AttributeEvents.incoming(e -> {
            if (!(e.getEntity() instanceof ServerPlayer p) || p.getPersistentData().getLong(K_GARB) <= p.level().getGameTime()) return;
            LivingEntity att = e.getSource().getEntity() instanceof LivingEntity a && a != p ? a : null;
            if (att != null && e.getSource().getDirectEntity() != att) {                       // a shot or a spell: a third of it goes back
                float back = e.getAmount() * 0.3f;
                att.hurt(p.damageSources().indirectMagic(p, p), back);
                fx(p.serverLevel(), VfxShape.MIRROR_FX1, p.getEyePosition().add(0, -0.2, 0), att.getBoundingBox().getCenter(), 10, 0.8f);
            }
            e.setAmount(e.getAmount() * 0.85f);
        });
        AttributeEvents.logout(p -> p.getPersistentData().remove(K_GARB));
    }

    static BookPage.Cast armoured(BookPage.Cast c, int style) { return SpatialArmourCast.armoured(c, Aura.MIRROR, style); }

    // ================================================================ new pages
    /** Reflect Iris: the mirrors show the gaze back. */
    static boolean reflectIris(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        List<LivingEntity> seen = new ArrayList<>();
        Vec3 eye = p.getEyePosition();
        for (LivingEntity t : GrimoireBook.around(p, p.position(), 14)) {
            Vec3 toMe = eye.subtract(t.getEyePosition());
            if (toMe.lengthSqr() < 1.0E-4) continue;
            if (t.getLookAngle().dot(toMe.normalize()) > 0.35 && p.hasLineOfSight(t)) seen.add(t);
            if (seen.size() >= 6) break;
        }
        if (seen.isEmpty()) { GrimoireBook.fail(p, "No one is looking at you."); return false; }
        if (!GrimoireBook.control(p)) return false;
        ServerLevel sl = p.serverLevel();
        float pw = EnergyBridge.scale(p);
        b.castCircle(p, 1.4f);
        fx(sl, VfxShape.MIRROR_FX2, p.position(), p.position().add(0, 1, 0), 40, 3.2f);
        for (LivingEntity t : seen) {
            int ticks = BalanceLaw.controlTicks(t, 60);
            EnergyBridge.effect(t, "paralysis", ticks, 0);
            EnergyBridge.effect(t, "silence", ticks, 0);
            b.hurt(i, p, t, mode, 6f * pw);
            Vec3 c = t.getBoundingBox().getCenter();
            for (int k = 0; k < 3; k++) {
                double a = k * Math.PI * 2 / 3 + p.getRandom().nextDouble();
                mirrorProp(sl, c.add(Math.cos(a) * 1.6, 0.2, Math.sin(a) * 1.6), c, Math.min(ticks, 60), 0.8f, p);
            }
            fx(sl, VfxShape.MIRROR_FX1, p.getEyePosition().add(0, -0.25, 0), c, 10, 0.9f);
        }
        sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.4f, 1.5f);
        return true;
    }

    /** Glass Garb: 20 s. */
    static boolean glassGarb(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        p.getPersistentData().putLong(K_GARB, p.level().getGameTime() + 400);
        b.castCircle(p, 1.2f);
        fx(p.serverLevel(), VfxShape.MIRROR_FX3, p.position().add(0, 1, 0), p.position().add(0, 1, 1), 24, 1.0f);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.4f);
        return true;
    }

    /** The appended pages (MirrorBook joins them after its own nine). */
    public static List<BookPage> pages() {
        return List.of(
                BookPage.mid("reflect_iris", "Reflect Iris", armoured(MirrorRebuild::reflectIris, 1)).withCooldown(400),
                BookPage.mid("glass_garb", "Glass Garb", MirrorRebuild::glassGarb).withCooldown(600));
    }
}
