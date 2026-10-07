package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * 0.59 Beast Magic: the logic of the two new pages.
 * <ul>
 *   <li>{@code spirit_beast_summon}: calls a spectral beast of beast-fire (PropKind.BEAST_1) that follows you and mauls the nearest hostile for
 *       20 to 30 s (the length grows with your EP). One lion-wolf; from 3.5x EP a bear joins it and from 6x a rhino (prop param 0 / 1 / 2).</li>
 *   <li>{@code primal_beast_form}: the player grows the spectral beast form (Aura.BEAST: fur skin, ears, tail, claws, flames) for 30 s with
 *       Strength II, Speed II and claws that hit harder; cast while sneaking it is the great-beast shape (style 1: a ghost beast head and
 *       shoulders hovering over you, plus Resistance).</li>
 * </ul>
 * The summons hit through the book (balance law, EP scaling, allies spared): {@link #link} stores the caster's strike function for each prop.
 */
public final class BeastSummons {
    private BeastSummons() {}

    /** Ticks a summon may stay linked without being ended (a safety net so the table cannot grow). */
    private static final long LINK_TTL = 20L * 60;

    private record Link(BiConsumer<LivingEntity, Float> strike, long until) {}

    private static final Map<UUID, Link> LINKS = new HashMap<>();

    /** Remembers how a prop hits: (target, raw damage) -> the book's hurt for the caster. */
    static void link(MagicPropEntity e, BiConsumer<LivingEntity, Float> strike) {
        long now = e.level().getGameTime();
        for (Iterator<Link> it = LINKS.values().iterator(); it.hasNext(); ) if (it.next().until < now) it.remove();
        LINKS.put(e.getUUID(), new Link(strike, now + LINK_TTL));
    }

    /** Hurts a target as the prop's caster; false when the prop has no link (it then does nothing). */
    public static boolean strike(MagicPropEntity e, LivingEntity target, float raw) {
        Link l = LINKS.get(e.getUUID());
        if (l == null) return false;
        l.strike.accept(target, raw);
        return true;
    }

    public static void unlink(MagicPropEntity e) { LINKS.remove(e.getUUID()); }

    // ------------------------------------------------------------------ Spirit Beast
    static boolean summon(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel sl = p.serverLevel();
        double ep = BalanceLaw.epScale(p);
        int life = (int) Math.min(600, 400 + Math.max(0, ep - 1) * 20);
        float scale = (float) Math.min(1.6, 1.0 + Math.max(0, ep - 1) * 0.06) * GrimoireBook.size(i, p);
        int count = ep >= 6 ? 3 : ep >= 3.5 ? 2 : 1;
        for (MagicPropEntity old : sl.getEntitiesOfClass(MagicPropEntity.class, p.getBoundingBox().inflate(96),
                m -> m.kind() == PropKind.BEAST_1 && p.getUUID().equals(m.ownerId()))) old.expire();
        Vec3 fwd = p.getViewVector(1f).multiply(1, 0, 1);
        fwd = fwd.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : fwd.normalize();
        b.castCircle(p, 1.2f);
        int made = 0;
        for (int k = 0; k < count; k++) {
            double a = (k - (count - 1) / 2.0) * 0.9;
            Vec3 d = new Vec3(fwd.x * Math.cos(a) - fwd.z * Math.sin(a), 0, fwd.x * Math.sin(a) + fwd.z * Math.cos(a));
            Vec3 at = p.position().add(d.scale(2.4));
            float yaw = (float) (Math.atan2(-d.x, d.z) * 180.0 / Math.PI);
            MagicPropEntity e = MagicProps.spawn(sl, PropKind.BEAST_1, at, yaw, scale, life, k, p);
            if (e == null) continue;
            link(e, (t, raw) -> b.hurt(i, p, t, mode, raw));
            b.vfx(p, VfxShape.BEAST_FX3, at, at.add(0, 1, 0), 22, 0.9f);
            made++;
        }
        if (made == 0) {
            GrimoireBook.fail(p, "The spirit beast cannot take shape here.");
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ Primal Beast Form
    static boolean form(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 600;
        boolean great = p.isShiftKeyDown();
        b.castCircle(p, 1.3f);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 1));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 1));
        if (great) p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 0));
        var tag = i.getOrCreateTag();
        tag.putLong("EmpowerUntil", p.level().getGameTime() + ticks);
        tag.putFloat("EmpowerBonus", great ? 6f : 4.5f);
        PlayerAuras.set(p, Aura.BEAST, ticks, great ? 1 : 0);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.BEAST_FX3, p, p.position().add(0, 1, 0), b.color, 30, 1.4f);
        return true;
    }
}
