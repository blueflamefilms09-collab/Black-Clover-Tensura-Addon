package com.newuniverse.nusmp.song;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.WikiBooks;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.mana.ManaLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: this is what one pulse of a song actually does to the world. The ticker decides WHEN a pulse happens; here the mana is paid
 * (a singer with nothing left stops being heard) and the four modes act on allies or foes within the radius, each through the
 * mod's own balance helpers: healing through BalanceLaw.heal (never on bosses), sleep through BalanceLaw.beginControl and
 * controlTicks (bosses and players clamped, the 12 second lock shared with every other control spell), damage through the book's
 * hurt (weak on purpose), and knock-back plus a short Absorption for the barrier. Nothing here changes a block.
 */
public final class SongEffects {
    /** Persistent-data key on a sleeping creature: game time at which the lullaby ends. */
    public static final String SLEEP_UNTIL = "nusmp_song_sleep_until";
    private static final int MAX_TARGETS = 24;

    private SongEffects() {}

    /** Pays the mana for one pulse. False = the singer cannot afford it. */
    static boolean pay(ServerPlayer p, SongMode mode, boolean crescendo) {
        double cost = BalanceLaw.cost(p, mode.pulseCostPercent(crescendo), 20) * ManaLaw.costMult(p) * SongConfig.costMult();
        if (cost > 0 && !p.isCreative()) {
            var ex = TensuraStorages.getExistenceFrom(p);
            if (ex == null || ex.getMagicule() < cost) return false;
            ex.setMagicule(ex.getMagicule() - cost);
            ex.markDirty();
        }
        ManaLaw.exhaust(p);
        return true;
    }

    /** Colour of each mode inside the Song palette (pink base, tinted so the four are told apart). */
    public static int color(SongMode m) {
        int base = WikiBooks.color(MagicType.SONG);
        return switch (m) {
            case HEAL -> 0xFFBFF5D8;
            case LULLABY -> 0xFFB9B4FF;
            case BARRIER -> 0xFFFFE6A8;
            case REQUIEM -> 0xFFC070D8;
            default -> base;
        };
    }

    /**
     * One pulse. strength is 0.3..1.0 from the voice; big = the Crescendo release (3x the cost, wider, twice as strong).
     * Returns false when the singer could not pay (nothing happened).
     */
    public static boolean pulse(ServerPlayer p, ManasSkillInstance inst, SongMode mode, float strength, boolean big) {
        if (mode == SongMode.NONE) return false;
        if (!pay(p, mode, big)) return false;
        ServerLevel level = p.serverLevel();
        int skillLevel = SongMode.skillLevel(inst.getSkill() instanceof com.newuniverse.nusmp.book.GrimoireBook b ? b.masteryFrac(inst) : 0.0);
        double radius = SongConfig.radius() * (big ? SongConfig.crescendoRadiusMult() : 1.0);
        float power = Math.min(1.0f, strength) * (big ? 2.0f : 1.0f);
        Vec3 c = p.position().add(0, 0.2, 0);
        switch (mode) {
            case HEAL -> heal(p, c, radius, skillLevel, power);
            case LULLABY -> lullaby(p, c, radius, power);
            case BARRIER -> barrier(p, c, radius, skillLevel, power);
            case REQUIEM -> requiem(p, inst, c, radius, skillLevel, power);
            default -> { }
        }
        fx(level, p, mode, radius, big);
        return true;
    }

    private static void heal(ServerPlayer p, Vec3 c, double r, int skillLevel, float power) {
        int n = 0;
        for (LivingEntity e : SongTargets.allies(p, c, r)) {
            if (n++ >= MAX_TARGETS) break;
            if (BalanceLaw.isBoss(e)) continue;                                  // bosses keep their rules: no healing
            float amount = e.getMaxHealth() * SongMode.healFraction(skillLevel, power);
            if (amount > 0f) BalanceLaw.heal(e, amount);
        }
    }

    private static void lullaby(ServerPlayer p, Vec3 c, double r, float power) {
        List<LivingEntity> foes = SongTargets.foes(p, c, r);
        if (foes.isEmpty() || !BalanceLaw.beginControl(p)) return;               // the shared hard-control lock
        long now = p.level().getGameTime();
        int n = 0;
        for (LivingEntity t : foes) {
            if (n++ >= MAX_TARGETS) break;
            int ticks = BalanceLaw.controlTicks(t, SongMode.sleepTicks(power));
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6));
            t.getPersistentData().putLong(SLEEP_UNTIL, now + ticks);
            if (t instanceof Mob m) { m.setTarget(null); m.getNavigation().stop(); }
        }
    }

    private static void barrier(ServerPlayer p, Vec3 c, double r, int skillLevel, float power) {
        int n = 0;
        for (LivingEntity t : SongTargets.foes(p, c, r)) {
            if (n++ >= MAX_TARGETS) break;
            Vec3 away = t.position().subtract(p.position());
            Vec3 flat = new Vec3(away.x, 0, away.z);
            if (flat.lengthSqr() < 1.0E-4) flat = new Vec3(1, 0, 0);
            flat = flat.normalize();
            // knockback() takes a strength and the direction it pushes AGAINST; ~3 blocks at full strength
            t.knockback(SongMode.knockDistance(power) * 0.55, -flat.x, -flat.z);
            t.hurtMarked = true;
        }
        n = 0;
        for (LivingEntity a : SongTargets.allies(p, c, r)) {
            if (n++ >= MAX_TARGETS) break;
            a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, SongMode.ABSORPTION_TICKS, SongMode.absorptionAmplifier(skillLevel), false, true));
        }
    }

    private static void requiem(ServerPlayer p, ManasSkillInstance inst, Vec3 c, double r, int skillLevel, float power) {
        var book = SongSkill.book(inst);
        if (book == null) return;
        int mode = Math.max(0, inst.getOrCreateTag().getInt(SongPages.TAG_PAGE));
        int n = 0;
        for (LivingEntity t : SongTargets.foes(p, c, r)) {
            if (n++ >= MAX_TARGETS) break;
            book.hurt(inst, p, t, mode, SongMode.requiemDamage(skillLevel, power));
        }
    }

    private static void fx(ServerLevel level, ServerPlayer p, SongMode mode, double radius, boolean big) {
        int col = color(mode);
        Vec3 c = p.position().add(0, 0.1, 0);
        VfxSpawn.send(level, VfxShape.SONG_RING, c, c, col, big ? 40 : 24, (float) radius);
        VfxSpawn.sendFollowing(level, VfxShape.SONG_NOTES, p, p.position().add(0, 1.6, 0), col, big ? 60 : 40, big ? 1.6f : 1.0f);
        Fx.spray(level, FxThemes.MAGIC_SONG, FxTone.LIGHT, p.getX(), p.getY() + 1.4, p.getZ(), big ? 24 : 8, 0.6, 0.5, 0.6, 0.02);
    }
}
