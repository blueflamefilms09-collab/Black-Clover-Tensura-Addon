package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxSpawn;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
    static final int SILVER = 0xFFC8E8FF;

    private final List<BookPage> pages = com.newuniverse.nusmp.book.ext.Ext.join(List.of(
            BookPage.starter(MercuryRules.BLADE, "Silver Blade", MercuryBook::blade),
            BookPage.signature(MercuryRules.GUARDIAN, "Silver Guardian", MercuryBook::shield),
            // 0.34: Nozel Silva's character spell (0.45: remade, the old WikiSpells version stays in the code)
            BookPage.signature(MercuryRules.RAIN, "Mercury Magic: Silver Rain", MercuryBook::silverRain),
            // 0.45: appended
            BookPage.signature(MercuryRules.EAGLE, "Silver Eagle", MercuryBook::silverEagle).withCooldown(600)), com.newuniverse.nusmp.book.ext.MercuryExt.pages());

    public MercuryBook() { super(MagicType.MERCURY, 0xFFC9D1DB); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    static float pw(ServerPlayer p) { return EnergyBridge.power(p); }

    /** One hyper-dense spear from 'from' to the target point; a liquid tail; pierces (1.93.0: the effect is MERCURY_FX1 and the body a spear prop). */
    static void spear(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, Vec3 from, Vec3 to, float raw, LivingEntity homing) {
        MercuryArts.lance(b, i, p, mode, from, to, raw, homing, 1, 0.7f + 0.25f * pw(p));
    }

    /**
     * Silver Blade: after the wiki the user makes a double-edged sword of mercury that floats and moves without contact, several at once. Three blades pour out of the hand, hang in the
     * air a moment and fly at the target one after the other (the blade is the visual body, the damage is the old three spears').
     */
    static boolean blade(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = target(p, 28);
        Vec3 look = p.getViewVector(1f), side = new Vec3(-look.z, 0, look.x).normalize();
        Vec3 aim = t != null ? t.getBoundingBox().getCenter() : p.getEyePosition().add(look.scale(24));
        b.castCircle(p, 0.6f);
        for (int k = -1; k <= 1; k++) {
            Vec3 from = p.getEyePosition().add(side.scale(k * 0.7)).add(0, -0.2, 0);
            int kk = k;
            SpellRuntime.later(p.serverLevel(), 2 + (k + 1) * 2, () -> {
                MercuryArts.body(p.serverLevel(), p, from, aim, 0, 14);
                MercuryArts.lance(b, i, p, mode, from, aim, 6f, kk == 0 ? t : null, -1, 0.7f);
            });
        }
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1f, 0.6f);
        return true;
    }

    /**
     * Silver Guardian: after the wiki the user concentrates their mercury into a defensive coating over the whole body, solid enough to block repeated strikes and to crack when broken, flexible
     * enough not to hinder movement; the mercury is poisonous, so the spell harms the user while it lasts. 1.93.0: the coat is the temporary armour (Aura.MERCURY, the Silver Guardian: chrome
     * plates, a pair of eagle wings) with a dome of flowing mercury round the caster for 8 s. Shots that touch it sink in; blows land at half strength (fire excepted); the toxin costs the wearer
     * half a hit point a second (never below six, never in creative). When it ends the dome draws itself into spears and hurls them at everything near.
     */
    static boolean shield(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        float r = 2.2f + 0.6f * pw(p);
        i.getOrCreateTag().putLong("ShieldUntil", p.level().getGameTime() + MercuryRules.GUARDIAN_TICKS);
        PlayerAuras.set(p, Aura.MERCURY, MercuryRules.GUARDIAN_TICKS, 0);
        b.castCircle(p, 1.2f);
        VfxSpawn.sendFollowing(level, VfxShape.MERCURY_FX2, p, p.position(), SILVER, MercuryRules.GUARDIAN_TICKS, r / 1.5f);
        VfxSpawn.sendFollowing(level, VfxShape.MERCURY_FX3, p, p.position().add(0, 1, 0), SILVER, 30, 1.2f);
        SpellRuntime.zone(level, MercuryRules.GUARDIAN_TICKS, 2, age -> {
            for (Projectile pr : level.getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(r),
                    x -> x.getOwner() != p && !(x.getOwner() instanceof LivingEntity o && o.isAlliedTo(p)))) {
                VfxSpawn.send(level, VfxShape.MERCURY_FX3, pr.position(), pr.position().add(0, 1, 0), SILVER, 10, 0.3f);
                pr.discard();
            }
            if (age % 10 == 0 && p.isAlive() && MercuryRules.selfHarmAllowed(p.getHealth(), p.isCreative())) p.hurt(p.damageSources().magic(), MercuryRules.SELF_HARM_PER_SECOND);   // the toxin
        });
        SpellRuntime.later(level, MercuryRules.GUARDIAN_TICKS, () -> {                                     // the dome flows back into spears
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

    /** Silver Rain: a cloud of mercury overhead lets fall a storm of needle-fine spears on the place you aim at; the user can keep the droplets off whoever stands in the middle. */
    static boolean silverRain(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        Vec3 at = aim(p, 32);
        float r = 4 + 1.5f * pw(p);
        b.castCircle(p, 1.3f);
        VfxSpawn.send(level, VfxShape.MERCURY_FX2, at.add(0, 0.05, 0), at.add(0, 1, 0), SILVER, 50, r / 1.5f);
        b.vfx(p, VfxShape.MERCURY_DOME, at.add(0, 9, 0), at.add(0, 9, 0), 40, r * 0.5f);              // the cloud (the older dome effect, kept)
        int drops = 10 + 6 * MirrorWorks.tier(pw(p));
        for (int k = 0; k < drops; k++) {
            SpellRuntime.later(level, 6 + k * 2, () -> {
                double a = level.random.nextDouble() * Math.PI * 2, d = Math.sqrt(level.random.nextDouble()) * r;
                Vec3 hit = at.add(Math.cos(a) * d, 0, Math.sin(a) * d);
                MercuryArts.body(level, p, hit.add(0, 9, 0), hit, 1, 6);
                VfxSpawn.send(level, VfxShape.MERCURY_FX1, hit.add(0, 9, 0), hit, SILVER, 6, 0.5f);
                VfxSpawn.send(level, VfxShape.MERCURY_FX3, hit, hit, SILVER, 12, 0.3f);
                for (LivingEntity t : around(p, hit, 1.4)) b.hurt(i, p, t, mode, 3.5f * pw(p));
            });
        }
        return true;
    }

    /**
     * Silver Eagle: after the wiki a giant eagle of mercury that can carry the user through the air, large enough for several people. The Eagle (a prop with its own model) forms under the
     * caster and flies along the line of sight for 26 ticks carrying them, cutting and scattering everything it passes; the caster lands where it dissolves, safe from the fall.
     */
    static boolean silverEagle(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        Vec3 dir = p.getViewVector(1f).multiply(1, 0.3, 1).normalize();
        Vec3 from = p.position().add(0, 0.2, 0), to = from.add(dir.scale(28));
        float span = 3 + 1.5f * pw(p);
        b.castCircle(p, 1.5f);
        VfxSpawn.send(level, VfxShape.MERCURY_FX3, from, from.add(0, 1, 0), SILVER, 26, 1.4f);
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z)), pitch = (float) -Math.toDegrees(Math.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)));
        MagicPropEntity eagle = MagicProps.spawn(level, PropKind.MERCURY_2, from, yaw, span / 3.8f, MercuryRules.EAGLE_FLIGHT_TICKS + 8, 0, p);
        if (eagle != null) eagle.setXRot(pitch);
        PlayerAuras.set(p, Aura.MERCURY, MercuryRules.EAGLE_FLIGHT_TICKS + 40, 1);
        Set<UUID> cut = new HashSet<>();
        SpellRuntime.zone(level, MercuryRules.EAGLE_FLIGHT_TICKS, 1, age -> {
            if (!p.isAlive()) return;
            Vec3 pos = from.add(dir.scale(28.0 * Math.min(1.0, (age + 1) / (double) MercuryRules.EAGLE_FLIGHT_TICKS)));
            p.setDeltaMovement(dir.scale(28.0 / MercuryRules.EAGLE_FLIGHT_TICKS).add(0, 0.04, 0));
            p.fallDistance = 0f;
            p.hurtMarked = true;
            for (LivingEntity t : around(p, pos, span * 0.6)) {
                if (!cut.add(t.getUUID())) continue;
                b.hurt(i, p, t, mode, 9f * pw(p));
                Vec3 push = t.position().subtract(p.position()).normalize();
                t.knockback(1.2, -push.x, -push.z);
                VfxSpawn.send(level, VfxShape.MERCURY_FX3, t.position(), t.position().add(0, 1, 0), SILVER, 14, 0.5f);
            }
        });
        SpellRuntime.later(level, MercuryRules.EAGLE_FLIGHT_TICKS + 1, () -> {
            if (!p.isAlive()) return;
            p.fallDistance = 0f;
            VfxSpawn.send(level, VfxShape.MERCURY_FX3, p.position(), p.position().add(0, 1, 0), SILVER, 28, 1.6f);
        });
        level.playSound(null, p.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.5f, 0.6f);
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
