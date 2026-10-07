package com.newuniverse.nusmp.entity;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.book.KotodamaWords;
import com.newuniverse.nusmp.item.MagicWeaponItem;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/**
 * 0.52: Zagred's defences (docs/zagred_boss_gdd.md, sections 4.1 to 4.3).
 * <ul>
 *   <li>{@link #classify}: what a blow is, for the nullifications. Plain physical blows do nothing; mythical weapons,
 *       anti-magic, conceptual damage and very high EP get through.</li>
 *   <li>{@link Barrier}: the Multilayer Barrier. 3 to 5 layers of 6% of his health; each layer takes full damage from its own
 *       grain (kinetic or arcane) and 40% from the other, anti-magic pierces at 150%. Layers refresh on their own (twice as
 *       fast while an Arch Daemon anchors him). When every layer is gone he spends 3 s writing the barrier again and takes 150%.</li>
 *   <li>{@link Reflex}: Thought Acceleration. He spends tokens to sidestep projectiles and single blows (with a counter), and
 *       regains them slowly. Two threats at once, a cheap bait, anti-magic or a far hit beat it; at zero tokens he is out of
 *       thought for 2 s and takes 20% more.</li>
 * </ul>
 */
public final class ZagredDefense {
    private ZagredDefense() {}

    /** The boss's body takes (almost) nothing from a hit of this class unless it is mythical, magic or anti-magic. */
    public enum Kind { PHYSICAL, MAGIC, ANTI_MAGIC, MYTHICAL }

    public enum Grain { KINETIC, ARCANE }

    /** Max EP at which a plain attacker's blows count as mythical. */
    static final double MYTHIC_EP = 5_000_000;

    static boolean mythical(DamageSource s) {
        String path = s.typeHolder().unwrapKey().map(k -> k.location().getPath()).orElse("");
        if (path.contains("conceptual") || path.contains("severance")) return true;
        if (s.getEntity() instanceof LivingEntity a) {
            if (a.getMainHandItem().getItem() instanceof MagicWeaponItem) return true;
            return Nullification.maxEP(a) >= MYTHIC_EP;
        }
        return false;
    }

    /** 0.53: a summoned being (a Tensura summon, a tamed or owned creature, a grimoire daemon): its blows are conjured, so they are never "plain physical". */
    static boolean isSummon(net.minecraft.world.entity.Entity e) {
        if (!(e instanceof LivingEntity) || e instanceof net.minecraft.world.entity.player.Player || e instanceof ZagredBossEntity) return false;
        if (e instanceof net.minecraft.world.entity.OwnableEntity o && o.getOwnerUUID() != null) return true;
        try { return io.github.manasmods.tensura.storage.ep.ExistenceStorage.isSummon((LivingEntity) e); } catch (Throwable ignored) { return false; }
    }

    static Kind classify(DamageSource s, String element) {
        if (ZagredBossEntity.antiMagic(s)) return Kind.ANTI_MAGIC;
        if (element.equals("physical") || element.equals("projectile")) return mythical(s) ? Kind.MYTHICAL : Kind.PHYSICAL;
        return mythical(s) ? Kind.MYTHICAL : Kind.MAGIC;
    }

    /** Which grain a blow wears down: magic and elements are arcane, everything else kinetic. */
    static Grain grainOf(Kind k, String element) {
        if (k == Kind.MAGIC) return element.equals("explosion") ? Grain.KINETIC : Grain.ARCANE;
        return k == Kind.ANTI_MAGIC ? Grain.ARCANE : Grain.KINETIC;
    }

    /** Resistances stacked under adaptation: holy and demonic half, the natural four 60%. */
    static float resistance(String element) {
        return switch (element) {
            case "light", "dark" -> 0.65f;
            case "fire", "water", "wind", "earth" -> 0.75f;
            default -> 1f;
        };
    }

    static int layersFor(int phase) { return phase <= 2 ? 3 : phase == 3 ? 4 : 5; }
    static int tokensFor(int phase) { return phase == 1 ? 3 : phase == 2 ? 4 : phase == 3 ? 5 : 6; }
    static int regenTicks(int phase) { return phase == 1 ? 56 : phase == 2 ? 44 : phase == 3 ? 34 : 28; }

    // ================================================================ the barrier
    public static final class Barrier {
        public static final int MAX = 5;
        final float[] hp = new float[MAX];
        int count = 3;
        float cap = 30;
        long nextRefresh, reformUntil, penaltyUntil, lockUntil;
        /** Set by absorb(): layers broken by the last hit, and whether it opened the reform window. */
        int broken;
        boolean reformStarted;

        void set(int layers, float maxHealth, long now) {
            count = Math.max(1, Math.min(MAX, layers));
            cap = Math.max(1, maxHealth * 0.045f);
            reformUntil = 0;
            penaltyUntil = 0;
            for (int i = 0; i < MAX; i++) hp[i] = i < count ? cap : 0;
            nextRefresh = now + 220;
        }

        int effective(long now) { return now < penaltyUntil ? Math.max(1, count - 1) : count; }

        public boolean reforming(long now) { return now < reformUntil; }

        public int alive(long now) {
            int n = 0;
            for (int i = 0; i < effective(now); i++) if (hp[i] > 0.01f) n++;
            return n;
        }

        public int layers(long now) { return reforming(now) ? 0 : effective(now); }

        static Grain grainOfLayer(int i) { return i % 2 == 0 ? Grain.ARCANE : Grain.KINETIC; }

        /** Damage left for the body after the layers took theirs (outermost first). */
        float absorb(float amount, Grain g, boolean piercing, long now) {
            broken = 0;
            reformStarted = false;
            if (now < reformUntil) return amount * 1.5f;
            int n = effective(now);
            float remaining = amount;
            for (int i = n - 1; i >= 0 && remaining > 0; i--) {
                if (hp[i] <= 0.01f) continue;
                float k = piercing ? 1.5f : grainOfLayer(i) == g ? 1f : 0.4f;
                float need = hp[i] / k;                                     // the damage this layer can still take
                if (remaining >= need) { remaining -= need; hp[i] = 0; broken++; lockUntil = now + 60; }
                else { hp[i] -= remaining * k; remaining = 0; }
            }
            if (remaining > 0 && alive(now) == 0) { startReform(now); return remaining * 1.5f; }
            return 0f;
        }

        void startReform(long now) {
            reformUntil = now + 60;
            reformStarted = true;
            lockUntil = now + 60;
            for (int i = 0; i < MAX; i++) hp[i] = 0;
        }

        /** An Arch Daemon fell: the outermost layer breaks at once. */
        boolean breakOne(long now) {
            for (int i = effective(now) - 1; i >= 0; i--)
                if (hp[i] > 0.01f) { hp[i] = 0; lockUntil = now + 60; if (alive(now) == 0) startReform(now); return true; }
            return false;
        }

        /** Per tick: finish a reform, and refresh the weakest layer on schedule (not while he channels). */
        void tick(long now, boolean channeling, boolean anchored) {
            if (reformUntil > 0 && now >= reformUntil) {                     // the barrier is written again, one layer short for 20 s
                reformUntil = 0;
                penaltyUntil = now + 400;
                int n = effective(now);
                for (int i = 0; i < MAX; i++) hp[i] = i < n ? cap : 0;
                nextRefresh = now + 220;
                return;
            }
            if (now < reformUntil || channeling) return;
            if (now >= nextRefresh) {
                int n = effective(now), weakest = -1;
                for (int i = 0; i < n; i++) if (hp[i] < cap - 0.01f && (weakest < 0 || hp[i] < hp[weakest])) weakest = i;
                if (weakest >= 0) hp[weakest] = Math.min(cap, hp[weakest] + cap * 0.35f);
                nextRefresh = now + (anchored ? 120 : 220);
            }
        }

        void save(CompoundTag t) {
            t.putInt("BCount", count);
            t.putFloat("BCap", cap);
            for (int i = 0; i < MAX; i++) t.putFloat("BHp" + i, hp[i]);
        }

        void load(CompoundTag t) {
            if (!t.contains("BCount")) return;
            count = t.getInt("BCount");
            cap = t.getFloat("BCap");
            for (int i = 0; i < MAX; i++) hp[i] = t.getFloat("BHp" + i);
        }
    }

    // ================================================================ thought acceleration
    public static final class Reflex {
        int tokens = 4, max = 4;
        long nextRegen, noDodgeUntil, outUntil;

        void set(int max) { this.max = max; this.tokens = max; }

        public int tokens() { return tokens; }
        public int max() { return max; }
        public boolean out(long now) { return now < outUntil; }

        void tick(ZagredBossEntity b, ServerLevel sl, long now, int phase, boolean busy) {
            if (now >= nextRegen) {
                if (tokens < max) tokens++;
                nextRegen = now + regenTicks(phase);
            }
            if (busy || tokens <= 0 || now < noDodgeUntil || now < outUntil || now % 2 != 0) return;
            Vec3 c = b.getBoundingBox().getCenter();
            for (Projectile p : sl.getEntitiesOfClass(Projectile.class, b.getBoundingBox().inflate(10),
                    pr -> pr.getOwner() != b && !(pr.getOwner() instanceof GrimoireDaemonEntity) && !pr.onGround())) {
                Vec3 v = p.getDeltaMovement();
                double v2 = v.lengthSqr();
                if (v2 < 0.01) continue;
                Vec3 rel = c.subtract(p.position());
                double eta = rel.dot(v) / v2;                                // ticks to the closest approach
                if (eta < 3 || eta > 9) continue;                            // only a real window: too early or too late is no dodge
                double miss = rel.subtract(v.scale(eta)).length();
                if (miss > 1.6 + b.getBbWidth() / 2) continue;
                spend(b, now, v.normalize());
                return;
            }
        }

        /** A single blow that lands: true if he dodged it instead (cancel the damage). */
        boolean dodgeHit(ZagredBossEntity b, DamageSource s, long now, boolean busy, Kind kind) {
            if (busy || tokens <= 0 || now < noDodgeUntil || now < outUntil) return false;
            if (kind == Kind.ANTI_MAGIC) return false;                      // it cannot be read
            if (isSummon(s.getEntity())) return false;                      // 0.53: he does not sidestep a summon's blows (they are the answer to his reflexes)
            if (!(s.getEntity() instanceof LivingEntity a) || a.distanceToSqr(b) > 100) return false;
            if (b.getRandom().nextFloat() > 0.6f) return false;
            Vec3 dir = b.position().subtract(a.position());
            spend(b, now, dir.lengthSqr() < 1e-4 ? b.getViewVector(1f) : dir.normalize());
            if (a.distanceToSqr(b) < 36 && b.getRandom().nextBoolean()) counter(b, a);
            return true;
        }

        private void spend(ZagredBossEntity b, long now, Vec3 incoming) {
            tokens--;
            noDodgeUntil = now + 8;
            b.reflexInvulnUntil = now + 4;
            if (tokens <= 0) outUntil = now + 40;                           // out of thought for 2 s
            sidestep(b, incoming);
        }

        /** A 3-block sidestep across the incoming line, with an afterimage. Never out of the arena. */
        private void sidestep(ZagredBossEntity b, Vec3 incoming) {
            if (!(b.level() instanceof ServerLevel sl)) return;
            Vec3 flat = incoming.multiply(1, 0, 1);
            if (flat.lengthSqr() < 1e-4) flat = new Vec3(0, 0, 1);
            Vec3 side = new Vec3(-flat.z, 0, flat.x).normalize();
            int first = b.getRandom().nextBoolean() ? 1 : -1;
            for (int k = 0; k < 2; k++) {
                Vec3 to = b.position().add(side.scale(3.0 * (k == 0 ? first : -first)));
                if (b.arenaPos() != null && b.arenaPos().getCenter().distanceToSqr(to) > 30 * 30) continue;
                if (!sl.noCollision(b, b.getBoundingBox().move(to.subtract(b.position())))) continue;
                Vec3 from = b.getBoundingBox().getCenter();
                VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, from, from.add(0, 0.4, 0), KotodamaWords.ABYSS, 14, 1.4f);
                b.teleportTo(to.x, to.y, to.z);
                VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, to.add(0, 1.6, 0), to.add(0, 2.0, 0), KotodamaWords.VIOLET, 10, 0.8f);
                return;
            }
        }

        /** A tiny Reverse: a shove and three points of spiritual damage. No stun. */
        private void counter(ZagredBossEntity b, LivingEntity who) {
            KotodamaWords.spirit(b, who, 3);
            Vec3 away = who.position().subtract(b.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1e-4) who.knockback(0.6, -away.x, -away.z);
        }
    }
}
