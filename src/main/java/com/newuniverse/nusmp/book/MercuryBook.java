package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Mercury Magic (0.45 remake; Nozel Silva). After the wiki: liquid metal, dense and able to take any shape; compressed and
 * solidified it is hard enough to reflect light; like Steel Magic it is weak to fire.
 * <ul>
 *   <li><b>Two states:</b> free-flowing (Silver Guardian's dome, the liquid tail of every construct) and hyper-dense
 *       (spears, blades, the eagle). The dome flows back into spears when it ends: the transition is the attack.</li>
 *   <li><b>EP scaling, aggressive:</b> damage, dome size and eagle wingspan scale with the full EP curve
 *       ({@link EnergyBridge#power}, x0.8..x2.4), not the gentle one.</li>
 *   <li><b>Fire:</b> fire gets through the dome untouched.</li>
 * </ul>
 * Pages: Silver Blade, Silver Guardian (was Mercury Shield), Silver Rain (ids kept, remade) and Silver Eagle (appended).
 */
public class MercuryBook extends GrimoireBook {
    static final int SILVER = 0xFFE4E8F0;

    private final List<BookPage> pages = com.newuniverse.nusmp.book.ext.Ext.join(List.of(
            BookPage.starter("silver_blade", "Silver Blade", MercuryBook::blade),
            BookPage.signature("mercury_shield", "Silver Guardian", MercuryBook::shield),
            // 0.34: Nozel Silva's character spell (0.45: remade, the old WikiSpells version stays in the code)
            BookPage.signature("mercury_rain", "Mercury Magic: Silver Rain", MercuryBook::silverRain),
            // 0.45: appended
            BookPage.signature("silver_eagle", "Silver Eagle", MercuryBook::silverEagle).withCooldown(600)), com.newuniverse.nusmp.book.ext.MercuryExt.pages());

    public MercuryBook() { super(MagicType.MERCURY, 0xFFC9D1DB); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    static float pw(ServerPlayer p) { return EnergyBridge.power(p); }

    /** One hyper-dense spear from 'from' to the target point; a liquid tail; pierces. */
    static void spear(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, Vec3 from, Vec3 to, float raw, LivingEntity homing) {
        Vec3 dir = to.subtract(from).normalize();
        b.vfx(p, VfxShape.MERCURY_SPEAR, from, to, 12, 0.7f + 0.25f * pw(p));
        SpellRuntime.bolt(p, from, dir.scale(2.2), 0.6, 14, true, homing, (bolt, t) -> b.hurt(i, p, t, mode, raw * pw(p)), null);
    }

    /** Silver Blade: liquid mercury pours from your hand and hardens mid-flight into three razor spears. */
    static boolean blade(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = target(p, 28);
        Vec3 look = p.getViewVector(1f), side = new Vec3(-look.z, 0, look.x).normalize();
        Vec3 aim = t != null ? t.getBoundingBox().getCenter() : p.getEyePosition().add(look.scale(24));
        b.castCircle(p, 0.6f);
        for (int k = -1; k <= 1; k++) {
            Vec3 from = p.getEyePosition().add(side.scale(k * 0.7)).add(0, -0.2, 0);
            int kk = k;
            SpellRuntime.later(p.serverLevel(), 2 + (k + 1) * 2, () -> spear(b, i, p, mode, from, aim, 6f, kk == 0 ? t : null));
        }
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1f, 0.6f);
        return true;
    }

    /**
     * Silver Guardian: a dome of flowing mercury closes round you for 8 s. Shots that touch it sink in; blows land at half
     * strength (fire excepted). When it ends the dome draws itself into spears and hurls them at everything near.
     */
    static boolean shield(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        float r = 2.2f + 0.6f * pw(p);
        i.getOrCreateTag().putLong("ShieldUntil", p.level().getGameTime() + 160);
        b.castCircle(p, 1.2f);
        com.newuniverse.nusmp.vfx.VfxSpawn.sendFollowing(level, VfxShape.MERCURY_DOME, p, p.position(), SILVER, 160, r);
        SpellRuntime.zone(level, 160, 2, age -> {
            for (Projectile pr : level.getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(r),
                    x -> x.getOwner() != p && !(x.getOwner() instanceof LivingEntity o && o.isAlliedTo(p)))) {
                b.vfx(p, VfxShape.WATER_SPLASH, pr.position(), pr.position().add(0, 1, 0), 10, 0.4f);
                pr.discard();
            }
        });
        SpellRuntime.later(level, 160, () -> {                                     // the dome flows back into spears
            if (!p.isAlive()) return;
            List<LivingEntity> foes = around(p, p.position(), 12);
            int n = Math.min(6, foes.size());
            for (int k = 0; k < n; k++) {
                LivingEntity t = foes.get(k);
                float a = Mth.TWO_PI * k / Math.max(1, n);
                Vec3 from = p.position().add(Mth.cos(a) * r, 1.2, Mth.sin(a) * r);
                spear(b, i, p, mode, from, t.getBoundingBox().getCenter(), 5f, t);
            }
            level.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 1f, 1.4f);
        });
        level.playSound(null, p.blockPosition(), SoundEvents.BUCKET_FILL_LAVA, SoundSource.PLAYERS, 1f, 1.6f);
        return true;
    }

    /** Silver Rain: a cloud of mercury overhead lets fall a storm of needle-fine spears on the place you aim at. */
    static boolean silverRain(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        Vec3 at = aim(p, 32);
        float r = 4 + 1.5f * pw(p);
        b.castCircle(p, 1.3f);
        b.vfx(p, VfxShape.MERCURY_DOME, at.add(0, 9, 0), at.add(0, 9, 0), 40, r * 0.5f);
        int drops = 10 + 6 * MirrorWorks.tier(pw(p));
        for (int k = 0; k < drops; k++) {
            SpellRuntime.later(level, 6 + k * 2, () -> {
                double a = level.random.nextDouble() * Math.PI * 2, d = Math.sqrt(level.random.nextDouble()) * r;
                Vec3 hit = at.add(Math.cos(a) * d, 0, Math.sin(a) * d);
                b.vfx(p, VfxShape.MERCURY_SPEAR, hit.add(0, 9, 0), hit, 6, 0.6f);
                for (LivingEntity t : around(p, hit, 1.4)) b.hurt(i, p, t, mode, 3.5f * pw(p));
            });
        }
        return true;
    }

    /** Silver Eagle: a giant eagle of living mercury swoops along your line of sight, scattering and cutting all it passes. */
    static boolean silverEagle(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 dir = p.getViewVector(1f).multiply(1, 0.3, 1).normalize();
        Vec3 from = p.position().add(0, 2.5, 0).subtract(dir.scale(2)), to = from.add(dir.scale(28));
        float span = 3 + 1.5f * pw(p);
        b.castCircle(p, 1.5f);
        b.vfx(p, VfxShape.MERCURY_EAGLE, from, to, 26, span);
        SpellRuntime.later(p.serverLevel(), 8, () -> {
            for (LivingEntity t : along(p, from, to, span * 0.6)) {
                b.hurt(i, p, t, mode, 9f * pw(p));
                Vec3 push = t.position().subtract(p.position()).normalize();
                t.knockback(1.2, -push.x, -push.z);
            }
        });
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.5f, 0.6f);
        return true;
    }

    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, net.minecraft.world.damagesource.DamageSource source,
                                 io.github.manasmods.manascore.network.api.util.Changeable<Float> amount) {
        super.onTakenDamage(i, owner, source, amount);   // mana skin
        if (owner.level().getGameTime() < i.getOrCreateTag().getLong("ShieldUntil") && !source.is(DamageTypeTags.IS_FIRE))
            amount.set(amount.get() * 0.5f);                                   // fire melts straight through
        return true;
    }
}
