package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Corundum Magic: the summoned things of the book. "Ideal Closer Fist" sends a huge gem gauntlet (PropKind.CORUNDUM_1, tools/gen_corundum_geo.py) at
 * the foe the caster looks at; "Gem Plate Armour" grows the faceted gem armour over the caster (Aura.CORUNDUM) and sets gem shards (PropKind.CORUNDUM_2)
 * orbiting that spear nearby foes. The movement of the props lives in prop.CorundumProps; this class holds the casts and the damage (everything goes
 * through the book's hurt helpers, which apply the EP law), and remembers which book / skill / page each living prop belongs to.
 */
public final class CorundumSummons {
    private CorundumSummons() {}

    /** The fist: uses per window and the window in ticks (two heavy blows in 12 s); the armour: ticks. */
    public static final int FIST_USES = 2, FIST_WINDOW = 240, FIST_LIFE = 64, ARMOUR_TICKS = 400;
    private static final String FIST_COUNT = "CorundumFistUses", FIST_START = "CorundumFistStart";
    private static final int RUBY = 0xFFFF6A7A, SAPPHIRE = 0xFF6AA6FF;

    /** What a living prop needs to hurt things in its caster's name. */
    private record Ctx(GrimoireBook book, ManasSkillInstance inst, int mode, long until) {}

    private static final Map<UUID, Ctx> CTX = new HashMap<>();

    /** Remembers the page a prop was cast from (called right after spawning it); stale entries of props that never ended are dropped. */
    public static void bind(MagicPropEntity e, GrimoireBook b, ManasSkillInstance i, int mode, long until) {
        long now = e.level().getGameTime();
        CTX.values().removeIf(c -> c.until() < now);
        CTX.put(e.getUUID(), new Ctx(b, i, mode, until + 40));
    }

    public static boolean bound(MagicPropEntity e) { return CTX.containsKey(e.getUUID()); }

    public static void unbind(MagicPropEntity e) { CTX.remove(e.getUUID()); }

    private static float safe(LivingEntity t, float raw) { return BalanceLaw.isBoss(t) ? raw * 0.5f : raw; }

    /** Ticks of a summon, longer for a mighty caster (up to +50 %). */
    private static int span(int base, ServerPlayer p) { return (int) (base * Math.min(1.5, 1 + 0.04 * (BalanceLaw.epScale(p) - 1))); }

    /** The fist's size: 0.8 for a novice, up to about 1.1 for the mightiest casters. */
    private static float fistScale(ServerPlayer p) { return 0.8f + 0.04f * (float) Math.min(8.0, BalanceLaw.epScale(p) - 1.0); }

    // ================================================================ who counts as an enemy
    /** A creature the summons may attack for this owner: monsters and whatever fights the owner; never allies, pets, creative players or bystander players. */
    public static boolean foe(LivingEntity owner, LivingEntity e) {
        if (e == owner || !e.isAlive() || e.isSpectator() || e.isAlliedTo(owner)) return false;
        if (e instanceof Player pl && (pl.isCreative() || pl.isSpectator())) return false;
        if (e instanceof TamableAnimal t && t.isOwnedBy(owner)) return false;
        return e instanceof Enemy || (e instanceof Mob m && m.getTarget() == owner) || e == owner.getLastHurtByMob() || e == owner.getLastHurtMob();
    }

    public static List<LivingEntity> foes(LivingEntity owner, Vec3 c, double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : owner.level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r), x -> x.distanceToSqr(c) <= r * r))
            if (foe(owner, e)) out.add(e);
        return out;
    }

    public static LivingEntity nearest(LivingEntity owner, Vec3 c, double r) {
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity e : foes(owner, c, r)) {
            double d = e.distanceToSqr(c);
            if (d < bd) { bd = d; best = e; }
        }
        return best;
    }

    // ================================================================ the casts
    /**
     * Ideal Closer Fist: the gem gauntlet rises beside the caster, winds back in front of the foe in the crosshair and punches it (two blows in 12 s:
     * the first a ruby punch, the second a sapphire hammer-fist that also bursts on the ground round the foe).
     */
    static boolean idealCloserFist(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 24);
        if (t == null || !t.isAlive()) { GrimoireBook.fail(p, "No foe in your sights for the fist."); return false; }
        if (t.isAlliedTo(p) || (t instanceof Player pl && (pl.isCreative() || pl.isSpectator()))) { GrimoireBook.fail(p, "The fist will not strike an ally."); return false; }
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        var tag = i.getOrCreateTag();
        int uses = tag.getInt(FIST_COUNT);
        if (now - tag.getLong(FIST_START) >= FIST_WINDOW || now < tag.getLong(FIST_START)) uses = 0;
        if (uses >= FIST_USES) {
            GrimoireBook.fail(p, "The fist needs " + (int) Math.ceil((tag.getLong(FIST_START) + FIST_WINDOW - now) / 20.0) + "s to gather its strength.");
            return false;
        }
        boolean slam = uses > 0;
        int life = span(FIST_LIFE, p);
        float s = fistScale(p);
        Vec3 from = p.getEyePosition().add(p.getViewVector(1f).scale(0.8)).add(0, -0.5, 0);
        MagicPropEntity fist = MagicProps.spawn(level, PropKind.CORUNDUM_1, from, p.getYRot(), s, life, slam ? 3 : 0, p);
        if (fist == null) { GrimoireBook.fail(p, "The gauntlet would not form."); return false; }
        fist.setTarget(t);
        bind(fist, b, i, mode, now + life);
        tag.putInt(FIST_COUNT, uses + 1);
        if (uses == 0) tag.putLong(FIST_START, now);
        i.markDirty();
        b.castCircle(p, 1.3f);
        VfxSpawn.send(level, VfxShape.CORUNDUM_FX3, from, from.add(0, 0.6, 0), slam ? SAPPHIRE : RUBY, 24, 1.1f);
        return true;
    }

    /**
     * Gem Plate Armour: faceted ruby and sapphire plates grow over the caster for 20 s (Aura.CORUNDUM; a crystal crown and wings of shards once the
     * grimoire is well mastered or in a devil state), with Resistance III and Strength, and gem shards (PropKind.CORUNDUM_2) circle the caster and spear foes.
     */
    static boolean gemPlateArmour(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (PlayerAuras.active(p, Aura.CORUNDUM)) { GrimoireBook.fail(p, "Your gem armour still holds."); return false; }
        ServerLevel level = p.serverLevel();
        int ticks = span(ARMOUR_TICKS, p);
        int style = b.masteryFrac(i) >= 0.75 || GrimoireBook.inDevilState(p) ? 1 : 0;
        PlayerAuras.set(p, Aura.CORUNDUM, ticks, style);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 2));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 0));
        dismiss(p, PropKind.CORUNDUM_2);
        double ep = BalanceLaw.epScale(p);
        int n = ep >= 7 ? 5 : ep >= 4 ? 4 : 3;
        float s = 0.4f + 0.02f * (float) Math.min(8.0, ep - 1.0);
        long now = level.getGameTime();
        for (int k = 0; k < n; k++) {
            double a = k * Math.PI * 2 / n;
            Vec3 at = p.position().add(Math.cos(a) * 1.7, 1.3, Math.sin(a) * 1.7);
            int param = k | (n << 3) | ((k % 2) << 6);                                    // slot, count, gem (0 ruby, 1 sapphire)
            MagicPropEntity shard = MagicProps.spawn(level, PropKind.CORUNDUM_2, at, p.getYRot(), s, ticks, param, p);
            if (shard != null) bind(shard, b, i, mode, now + ticks);
        }
        b.castCircle(p, 1.5f);
        VfxSpawn.sendFollowing(level, VfxShape.CORUNDUM_FX3, p, p.position().add(0, 1, 0), b.color, 40, 1.5f);
        return true;
    }

    /** Ends the caster's earlier summons of this kind (a new cast replaces them). */
    private static void dismiss(ServerPlayer p, PropKind kind) {
        for (MagicPropEntity e : p.serverLevel().getEntitiesOfClass(MagicPropEntity.class, p.getBoundingBox().inflate(96),
                x -> x.kind() == kind && p.getUUID().equals(x.ownerId())))
            e.expire();
    }

    // ================================================================ what the props do to a foe
    /**
     * The fist lands: the book's damage (heavy), a knockback that throws the foe away from the gauntlet, a slow, and a burst of gem.
     * The hammer-fist (slam) also bursts on the ground and shoves everything within 3.4 blocks of the foe.
     */
    public static void fistImpact(MagicPropEntity fist, ServerPlayer p, LivingEntity t, boolean slam) {
        Ctx c = CTX.get(fist.getUUID());
        if (c == null || !t.isAlive()) return;
        ServerLevel level = p.serverLevel();
        boolean boss = BalanceLaw.isBoss(t);
        c.book().hurt(c.inst(), p, t, c.mode(), safe(t, slam ? 12f : 14f));
        Vec3 away = t.position().subtract(fist.position()).multiply(1, 0, 1);
        away = away.lengthSqr() < 1e-4 ? p.getViewVector(1f).multiply(1, 0, 1) : away;
        if (away.lengthSqr() > 1e-4) {
            away = away.normalize();
            t.knockback((boss ? 0.5 : 1.9) * fist.scale() / 0.9, -away.x, -away.z);
        }
        if (!slam && !boss) t.setDeltaMovement(t.getDeltaMovement().add(0, 0.25, 0));
        t.hurtMarked = true;
        CorundumArts.weigh(t, boss ? 30 : 60, 2);
        int col = slam ? SAPPHIRE : RUBY;
        Vec3 mid = t.getBoundingBox().getCenter();
        VfxSpawn.send(level, VfxShape.CORUNDUM_FX3, mid, mid.add(0, 0.5, 0), col, 22, slam ? 1.5f : 1.2f);
        VfxSpawn.send(level, VfxShape.CORUNDUM_FX1, mid.subtract(away.scale(2.0)), mid.add(away.scale(1.5)), col, 10, 0.9f);
        if (!slam) return;
        double r = 3.4 * fist.scale();
        VfxSpawn.send(level, VfxShape.CORUNDUM_FX2, t.position(), t.position().add(0, 1, 0), col, 26, (float) r);
        for (LivingEntity o : GrimoireBook.around(p, t.position(), r)) {
            if (o == t) continue;
            c.book().hurt(c.inst(), p, o, c.mode(), safe(o, 7f));
            Vec3 d = o.position().subtract(t.position()).multiply(1, 0, 1);
            if (d.lengthSqr() > 1e-4) {
                d = d.normalize();
                o.knockback(BalanceLaw.isBoss(o) ? 0.3 : 1.0, -d.x, -d.z);
            }
            o.hurtMarked = true;
        }
    }

    /** A gem shard spears a foe: a hit of the book's damage, a short slow, a chip of gem. */
    public static void shardPierce(MagicPropEntity shard, ServerPlayer p, LivingEntity t) {
        Ctx c = CTX.get(shard.getUUID());
        if (c == null || !t.isAlive()) return;
        c.book().hurt(c.inst(), p, t, c.mode(), safe(t, 3.5f));
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.isBoss(t) ? 20 : 40, 1));
        Vec3 mid = t.getBoundingBox().getCenter();
        VfxSpawn.send(p.serverLevel(), VfxShape.CORUNDUM_FX3, mid, mid, ((shard.param() >> 6) & 1) == 0 ? RUBY : SAPPHIRE, 12, 0.45f);
    }
}
