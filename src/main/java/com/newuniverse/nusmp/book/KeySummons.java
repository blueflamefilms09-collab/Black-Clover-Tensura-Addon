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
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
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
 * Key Magic: the summoned things of the book. "Gate of Keys" opens a tall gate (PropKind.KEY_1) that looses a bolt of one element at the nearest
 * foe for 8 s; "Key Guard" sends three great keys (PropKind.KEY_2) orbiting the caster that stab nearby foes for 15 s (plus Aura.KEY).
 * The movement of the props lives in prop.KeyProps; this class holds the casts and the damage (everything goes through the book's hurt helpers,
 * which apply the EP law), and remembers which book / skill / page each living prop belongs to.
 */
public final class KeySummons {
    private KeySummons() {}

    public static final int GATE_TICKS = 160, GUARD_TICKS = 300, ELEMENTS = 4;
    private static final String ELEMENT_TAG = "KeyGateElement";

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

    // ================================================================ who counts as an enemy
    /** A creature the keys may attack for this owner: monsters, hostile players and whatever fights the owner; never allies, pets or creative players. */
    public static boolean foe(LivingEntity owner, LivingEntity e) {
        if (e == owner || !e.isAlive() || e.isSpectator() || e.isAlliedTo(owner)) return false;
        if (e instanceof Player pl && (pl.isCreative() || pl.isSpectator())) return false;
        if (e instanceof TamableAnimal t && t.isOwnedBy(owner)) return false;
        return e instanceof Enemy || e instanceof Player || (e instanceof Mob m && m.getTarget() == owner)
                || e == owner.getLastHurtByMob() || e == owner.getLastHurtMob();
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
    /** Gate of Keys: a tall gate rises 4 blocks ahead and, for 8 s, opens and looses a bolt every half second at the nearest foe (the element turns each cast). */
    static boolean gateOfKeys(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (foes(p, p.position(), 16).isEmpty()) { GrimoireBook.fail(p, "No foe for the gate."); return false; }
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getViewVector(1f).multiply(1, 0, 1);
        look = look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
        Vec3 at = null;
        for (double d = 4.5; d >= 1.5 && at == null; d -= 1.0) {
            Vec3 c = p.position().add(look.scale(d));
            if (level.noCollision(new AABB(c.x - 0.8, c.y + 0.1, c.z - 0.8, c.x + 0.8, c.y + 3.2, c.z + 0.8))) at = c;
        }
        if (at == null) { GrimoireBook.fail(p, "No room for a gate there."); return false; }
        dismiss(p, PropKind.KEY_1);
        int element = i.getOrCreateTag().getInt(ELEMENT_TAG) % ELEMENTS;
        i.getOrCreateTag().putInt(ELEMENT_TAG, (element + 1) % ELEMENTS);
        i.markDirty();
        int life = span(GATE_TICKS, p);
        MagicPropEntity gate = MagicProps.spawn(level, PropKind.KEY_1, at, p.getYRot(), 0.85f, life, element, p);
        if (gate == null) { GrimoireBook.fail(p, "The gate would not open."); return false; }
        bind(gate, b, i, mode, level.getGameTime() + life);
        b.castCircle(p, 1.2f);
        VfxSpawn.send(level, VfxShape.KEY_FX3, at.add(0, 1.4, 0), at.add(0, 2.4, 0), b.color, 30, 1.3f);
        return true;
    }

    /** Key Guard: three great keys orbit the caster for 15 s and stab foes within 12 blocks; a golden-violet keyhole glows on the caster's back. */
    static boolean keyGuard(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (foes(p, p.position(), 16).isEmpty()) { GrimoireBook.fail(p, "No foe for the guard."); return false; }
        ServerLevel level = p.serverLevel();
        dismiss(p, PropKind.KEY_2);
        int life = span(GUARD_TICKS, p);
        int n = 3;
        for (int k = 0; k < n; k++) {
            Vec3 at = p.position().add(Math.cos(k * 2.094) * 1.9, 1.5, Math.sin(k * 2.094) * 1.9);
            MagicPropEntity key = MagicProps.spawn(level, PropKind.KEY_2, at, p.getYRot(), 0.55f, life, k | (n << 2), p);
            if (key != null) bind(key, b, i, mode, level.getGameTime() + life);
        }
        PlayerAuras.set(p, Aura.KEY, life, 0);
        b.castCircle(p, 1.3f);
        VfxSpawn.sendFollowing(level, VfxShape.KEY_FX3, p, p.position().add(0, 1, 0), b.color, 30, 1.2f);
        return true;
    }

    /** Ends the caster's earlier summons of this kind (a new cast replaces them). */
    private static void dismiss(ServerPlayer p, PropKind kind) {
        for (MagicPropEntity e : p.serverLevel().getEntitiesOfClass(MagicPropEntity.class, p.getBoundingBox().inflate(96),
                x -> x.kind() == kind && p.getUUID().equals(x.ownerId())))
            e.expire();
    }

    // ================================================================ what the props do to a foe
    /** One bolt of the gate at a foe: element 0 fire, 1 ice, 2 lightning, 3 wind. */
    public static void gateBolt(MagicPropEntity gate, ServerPlayer p, LivingEntity t, Vec3 from, int element) {
        Ctx c = CTX.get(gate.getUUID());
        if (c == null) return;
        ServerLevel level = p.serverLevel();
        Vec3 to = t.getBoundingBox().getCenter();
        double dist = from.distanceTo(to);
        int flight = (int) Math.max(4, Math.min(16, dist / 1.6));
        VfxShape shape = switch (element) {
            case 1 -> VfxShape.ICE_FX1;
            case 2 -> VfxShape.LIGHTNING_SPEAR;
            case 3 -> VfxShape.WIND_SLASH;
            default -> VfxShape.FIRE_SPEAR;
        };
        VfxSpawn.send(level, shape, from, to, c.book().color, flight + 2, 0.9f);
        VfxSpawn.send(level, VfxShape.KEY_FX1, from, to, c.book().color, flight, 0.6f);
        Vec3 vel = to.subtract(from).normalize().scale(1.6);
        SpellRuntime.bolt(p, from, vel, 0.6, flight + 8, false, t, (bolt, hit) -> {
            float raw = safe(hit, 4f);
            switch (element) {
                case 1 -> {
                    c.book().hurtAs(c.inst(), p, hit, c.mode(), raw, TensuraDamageTypes.ICE_ELEMENTAL);
                    hit.setTicksFrozen(hit.getTicksRequiredToFreeze() + 40);
                    hit.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                }
                case 2 -> {
                    c.book().hurtAs(c.inst(), p, hit, c.mode(), raw, TensuraDamageTypes.LIGHTNING_ELEMENTAL);
                    EnergyBridge.effect(hit, "silence", 30, 0);
                    hit.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0));
                }
                case 3 -> {
                    c.book().hurtAs(c.inst(), p, hit, c.mode(), raw, TensuraDamageTypes.WIND_ELEMENTAL);
                    hit.setDeltaMovement(hit.getDeltaMovement().add(0, BalanceLaw.isBoss(hit) ? 0.15 : 0.5, 0));
                    hit.hurtMarked = true;
                }
                default -> {
                    c.book().hurtAs(c.inst(), p, hit, c.mode(), raw, TensuraDamageTypes.FIRE_ELEMENTAL);
                    hit.igniteForSeconds(3);
                }
            }
        }, (bolt, at) -> VfxSpawn.send(level, VfxShape.KEY_FX3, at, at, c.book().color, 14, 0.6f));
    }

    /** A great key's stab: a hit of the book's damage, a slow and a golden burst. */
    public static void strike(MagicPropEntity key, ServerPlayer p, LivingEntity t) {
        Ctx c = CTX.get(key.getUUID());
        if (c == null || !t.isAlive()) return;
        c.book().hurt(c.inst(), p, t, c.mode(), safe(t, 5f));
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24, 1));
        Vec3 away = t.position().subtract(key.position()).multiply(1, 0, 1);
        if (away.lengthSqr() > 1e-4) {
            away = away.normalize();
            t.knockback(BalanceLaw.isBoss(t) ? 0.1 : 0.35, -away.x, -away.z);
        }
        ServerLevel level = p.serverLevel();
        Vec3 mid = t.getBoundingBox().getCenter();
        VfxSpawn.send(level, VfxShape.KEY_FX3, mid, mid, c.book().color, 14, 0.5f);
    }
}
