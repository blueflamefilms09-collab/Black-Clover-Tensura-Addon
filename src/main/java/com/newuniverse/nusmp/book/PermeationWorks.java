package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.book.ext.AttributeEvents;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import com.newuniverse.nusmp.vfx.WikiStyles;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 1.93.0 Permeation Magic rebuild. Permeation is a wiki attribute: its book is the shared wiki book (a bolt, a line, a field, a domain and a guard, WikiBooks.attribute), whose
 * five pages are KEPT as they are (ids, order, behaviour). The Black Clover wiki describes the magic as letting its user and what they touch pass through matter; no named canon
 * user or spell list could be verified offline, so the pages below are the nearest honest spells to that description (each is named in the doc as an adaptation). They are
 * appended after the generic five through {@code ExclusiveExt} (the one-line dispatch of 1.43.0):
 * <ul>
 *   <li><b>Phase Step</b>: for 3 s you are out of step with the world: blows and shots pass through you (a spell takes half), you run faster, and a phase echo of you stays
 *       where you were ({@link PropKind#PERMEATION_1}).</li>
 *   <li><b>Phase Through</b>: step through a wall in front of you (up to 3 blocks away) to the first free space behind it within 8 blocks, through two phase doors
 *       ({@link PropKind#PERMEATION_2}); it fails when there is no wall or no room on the other side.</li>
 *   <li><b>Permeating Strike</b>: a blow that goes through armour and barriers: 15 x EP scale, 90 percent of the armour ignored, the barriers within 3 blocks cut down.</li>
 *   <li><b>Ghost Zone</b>: for 8 s everything outside the world's rules around you: you and your allies within 5 blocks take 40 percent less from everything, foes there are slowed.</li>
 * </ul>
 * The generic guard page still sets the shared wiki armour; a tick hook turns it into the Permeation set (Aura.PERMEATION) so the set is the same however it was called.
 */
public final class PermeationWorks {
    private PermeationWorks() {}

    static final String K_PHASE = "nusmp_perm_phase_until", K_ZONE = "nusmp_perm_zone_until";
    private static final int CYAN = 0xFF65D7E8;
    private static final int WIKI_STYLE = WikiStyles.styleOf("PERMEATION");

    static void fx(ServerLevel level, VfxShape shape, Vec3 from, Vec3 to, int ticks, float power) { VfxSpawn.send(level, shape, from, to, CYAN, ticks, power); }

    public static void initHooks() {
        AttributeEvents.incoming(e -> {
            if (!(e.getEntity() instanceof ServerPlayer p)) return;
            long now = p.level().getGameTime();
            var tag = p.getPersistentData();
            if (tag.getLong(K_PHASE) > now && !e.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                boolean physical = e.getSource().is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE) || e.getSource().getDirectEntity() instanceof LivingEntity;
                if (physical) {
                    e.setCanceled(true);
                    fx(p.serverLevel(), VfxShape.PERMEATION_FX3, p.position().add(0, 1, 0), p.position().add(0, 1, 1), 12, 0.5f);
                    return;
                }
                e.setAmount(e.getAmount() * 0.5f);
            }
            if (tag.getLong(K_ZONE) > now) e.setAmount(e.getAmount() * 0.6f);
        });
        AttributeEvents.playerTick(p -> {                                                       // the generic guard's wiki armour becomes the Permeation set
            if (!PlayerAuras.active(p, Aura.WIKI) || PlayerAuras.style(p, Aura.WIKI) != WIKI_STYLE) return;
            PlayerAuras.clear(p, Aura.WIKI);
            PlayerAuras.set(p, Aura.PERMEATION, 180, 0);
        });
        AttributeEvents.logout(p -> { p.getPersistentData().remove(K_PHASE); p.getPersistentData().remove(K_ZONE); });
    }

    static BookPage.Cast armoured(BookPage.Cast c, int style) { return SpatialArmourCast.armoured(c, Aura.PERMEATION, style); }

    /** A phase door standing at {@code at}, facing {@code lookAt}. */
    static void doorProp(ServerLevel sl, Vec3 at, Vec3 lookAt, int life, float scale, ServerPlayer owner) {
        MagicProps.spawn(sl, PropKind.PERMEATION_2, at, SpatialWorks.yawToward(at, lookAt), scale, life, 0, owner);
    }

    // ================================================================ the pages
    static boolean phaseStep(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel sl = p.serverLevel();
        p.getPersistentData().putLong(K_PHASE, sl.getGameTime() + 60);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
        MagicProps.spawn(sl, PropKind.PERMEATION_1, p.position(), p.getYRot(), 1f, 40, 0, p);
        fx(sl, VfxShape.PERMEATION_FX3, p.position().add(0, 1, 0), p.position().add(p.getViewVector(1f)).add(0, 1, 0), 22, 1.0f);
        b.castCircle(p, 1f);
        sl.playSound(null, p.blockPosition(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.PLAYERS, 1.2f, 0.6f);
        return true;
    }

    static boolean phaseThrough(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 flat = p.getViewVector(1f).multiply(1, 0, 1);
        if (flat.lengthSqr() < 1.0E-4) { GrimoireBook.fail(p, "Look at a wall."); return false; }
        flat = flat.normalize();
        ServerLevel sl = p.serverLevel();
        AABB box = p.getBoundingBox();
        double wall = -1, dest = -1;
        for (double d = 0.5; d <= 8.01; d += 0.5) {
            AABB at = box.move(flat.scale(d));
            if (wall < 0) {
                if (!sl.noCollision(p, at)) { if (d > 3.01) break; wall = d; }
            } else if (sl.noCollision(p, at) && sl.isLoaded(p.blockPosition().offset((int) Math.round(flat.x * d), 0, (int) Math.round(flat.z * d)))) { dest = d; break; }
        }
        if (wall < 0) { GrimoireBook.fail(p, "There is no wall to pass through."); return false; }
        if (dest < 0) { GrimoireBook.fail(p, "There is no room on the other side."); return false; }
        Vec3 from = p.position(), to = from.add(flat.scale(dest));
        float pw = EnergyBridge.scale(p);
        doorProp(sl, from.add(flat.scale(0.6)), from, 30, 0.9f * pw, p);
        doorProp(sl, to.subtract(flat.scale(0.2)), to.add(flat), 30, 0.9f * pw, p);
        fx(sl, VfxShape.PERMEATION_FX3, from.add(0, 1.2, 0), from.add(flat).add(0, 1.2, 0), 22, 0.9f * pw);
        fx(sl, VfxShape.PERMEATION_FX3, to.add(0, 1.2, 0), to.add(flat).add(0, 1.2, 0), 22, 0.9f * pw);
        p.teleportTo(to.x, to.y, to.z);
        p.fallDistance = 0;
        sl.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2f, 0.7f);
        b.castCircle(p, 0.8f);
        return true;
    }

    static boolean permeatingStrike(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 6);
        if (t == null) { GrimoireBook.fail(p, "Too far to strike."); return false; }
        ServerLevel sl = p.serverLevel();
        float pw = EnergyBridge.scale(p);
        Vec3 c = t.getBoundingBox().getCenter(), dir = c.subtract(p.getEyePosition()).normalize();
        Nullification.shatterBarriers(sl, c, 3, p);
        float raw = 15f * pw;
        raw *= EnergyBridge.armourBypass(t, p.damageSources().generic(), raw, 0.9f);
        b.hurt(i, p, t, mode, raw);
        MagicProps.spawn(sl, PropKind.PERMEATION_1, t.position().add(dir.multiply(1, 0, 1).scale(1.8)), p.getYRot(), 1f, 14, 0, p);
        fx(sl, VfxShape.PERMEATION_FX1, p.getEyePosition().add(0, -0.25, 0), c.add(dir.scale(1.5)), 12, 1.1f * pw);
        fx(sl, VfxShape.PERMEATION_FX3, c, c.add(dir), 22, 0.8f * pw);
        sl.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 1.5f);
        b.castCircle(p, 0.8f);
        return true;
    }

    static boolean ghostZone(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel sl = p.serverLevel();
        float r = 5f * GrimoireBook.size(i, p);
        b.castCircle(p, 1.3f);
        VfxSpawn.sendFollowing(sl, VfxShape.PERMEATION_FX2, p, p.position(), CYAN, 160, r);
        SpellRuntime.zone(sl, 160, 10, age -> {
            if (!p.isAlive()) return;
            long until = sl.getGameTime() + 30;
            p.getPersistentData().putLong(K_ZONE, until);
            for (Player a : sl.getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(r), x -> x != p && x.isAlive() && x.isAlliedTo(p) && x.distanceToSqr(p) <= r * r))
                a.getPersistentData().putLong(K_ZONE, until);
            for (LivingEntity t : GrimoireBook.around(p, p.position(), r)) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0));
        });
        sl.playSound(null, p.blockPosition(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.PLAYERS, 1.2f, 0.5f);
        return true;
    }

    /** The appended pages (ExclusiveExt hands them to the wiki book through PermeationExt). */
    public static List<BookPage> pages() {
        return List.of(
                BookPage.mid("phase_step", "Phase Step", armoured(PermeationWorks::phaseStep, 0)).withCooldown(240),
                BookPage.mid("phase_through", "Phase Through", armoured(PermeationWorks::phaseThrough, 0)).withCooldown(120),
                BookPage.mid("permeating_strike", "Permeating Strike", armoured(PermeationWorks::permeatingStrike, 0)),
                BookPage.zone("ghost_zone", "Ghost Zone", armoured(PermeationWorks::ghostZone, 1)).withCooldown(600));
    }
}
