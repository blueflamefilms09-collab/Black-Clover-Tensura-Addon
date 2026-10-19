package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.TransmutationArts;
import com.newuniverse.nusmp.book.TransmutationRules;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Transmutation Magic (Grey), 1.93.0 rebuild: the server behaviours of the constructs.
 * <ul>
 *   <li>TRANSMUTATION_1, an iron spike: stands where Iron Spikes put it (the spell does the damage); its model rises over the first 8 ticks and sinks over the last 8.</li>
 *   <li>TRANSMUTATION_2, the Transmuted Sentinel: follows its owner, walks to the nearest hostile within 12 blocks and strikes it with a fist, rises out of the ground and crumbles over its
 *       last 26 ticks. The state word is synced in {@code param}: bits 0-1 the state (0 idle or walking, 1 strike), bits 2-17 the game time (mod 65536) at which it began.</li>
 *   <li>TRANSMUTATION_3, an alchemic blade: circles the owner at 1.6 blocks (its index and the count are in bits 18-25 of param), lunges at the nearest hostile within 12 blocks, cuts it and
 *       comes back (state 1 = lunging, 2 = returning; the state word as above).</li>
 * </ul>
 * Everything is capped (one hit per blade per lunge, a sentinel strikes once every 22 ticks) and bosses are knocked back a third as far.
 */
public final class TransmutationProps {
    private TransmutationProps() {}

    public static final int IDLE = 0, ACT = 1, ACT2 = 2;

    /** Called once by PropRegistry. */
    public static void init() {
        TransmutationArts.hooks();
        MagicProps.register(PropKind.TRANSMUTATION_1, new Spike());
        MagicProps.register(PropKind.TRANSMUTATION_2, new Sentinel());
        MagicProps.register(PropKind.TRANSMUTATION_3, new Blade());
        SelfTests.register("magic.transmutation_rebuild", TransmutationProps::selfTest);
    }

    // ------------------------------------------------------------------ the synced state word (bits 0-1 state, bits 2-17 start time) and the blade's slot (bits 18-25)
    public static int state(int param) { return param & 3; }
    public static int since(int param, long gameTime) { return (int) ((gameTime - ((param >> 2) & 0xFFFF)) & 0xFFFF); }
    public static int bladeIndex(int param) { return (param >> 18) & 15; }
    public static int bladeCount(int param) { return Math.max(1, (param >> 22) & 15); }

    private static void setState(MagicPropEntity e, int st) {
        long now = e.level().getGameTime();
        e.setParam((e.param() & ~0x3FFFF) | st | (int) ((now & 0xFFFF) << 2));
    }

    // ------------------------------------------------------------------ helpers
    private static boolean hostile(LivingEntity owner, LivingEntity t) {
        if (t == owner || !t.isAlive() || t.isSpectator() || t.isAlliedTo(owner) || t instanceof Player) return false;
        if (t instanceof Enemy) return true;
        return t instanceof Mob m && m.getTarget() == owner;
    }

    private static LivingEntity nearest(MagicPropEntity e, LivingEntity owner, ServerLevel sl, double range) {
        List<LivingEntity> list = sl.getEntitiesOfClass(LivingEntity.class, new AABB(e.position(), e.position()).inflate(range, 4, range), t -> hostile(owner, t));
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity t : list) {
            double d = t.distanceToSqr(e);
            if (d < bd) { bd = d; best = t; }
        }
        return best;
    }

    private static void hit(MagicPropEntity e, ServerLevel sl, LivingEntity owner, LivingEntity t, float raw) {
        DamageSource src = sl.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(TensuraDamageTypes.MAGIC_GENERIC)
                .<DamageSource>map(h -> new DamageSource(h, e, owner)).orElseGet(() -> owner.damageSources().indirectMagic(e, owner));
        t.hurt(src, BalanceLaw.damage(owner, t, raw, 0.5));
    }

    private static void knock(LivingEntity t, Vec3 from, double k) {
        Vec3 out = t.position().subtract(from).multiply(1, 0, 1);
        if (out.lengthSqr() < 1e-4) return;
        t.setDeltaMovement(t.getDeltaMovement().add(out.normalize().scale(BalanceLaw.isBoss(t) ? k / 3 : k)).add(0, 0.12, 0));
        t.hurtMarked = true;
    }

    private static void face(MagicPropEntity e, Vec3 to) {
        double dx = to.x - e.getX(), dz = to.z - e.getZ();
        if (dx * dx + dz * dz < 1e-4) return;
        float want = (float) (Mth.atan2(dz, dx) * 57.29577951308232) - 90f, cur = e.getYRot();
        e.setYRot(cur + Mth.clamp(Mth.wrapDegrees(want - cur), -28f, 28f));
    }

    private static void walk(MagicPropEntity e, Vec3 to, double speed) {
        Vec3 d = to.subtract(e.position()).multiply(1, 0, 1);
        double len = d.length();
        Vec3 dm = e.getDeltaMovement();
        double vy = e.onGround() ? -0.02 : Math.max(-0.6, dm.y - 0.08);
        Vec3 h = len < 1e-4 ? Vec3.ZERO : d.scale(Math.min(speed, len) / len);
        if (e.horizontalCollision && e.onGround()) vy = 0.42;
        e.setDeltaMovement(h.x, vy, h.z);
        e.move(MoverType.SELF, e.getDeltaMovement());
        if (len > 1e-4) face(e, to);
    }

    // ------------------------------------------------------------------ the spike
    private static final class Spike implements MagicProps.Behavior {
        @Override
        public void init(MagicPropEntity e, ServerLevel sl) {
            float s = e.scale();
            e.setSize(1.0f * s, 1.7f * s);
            VfxSpawn.send(sl, VfxShape.TRANSMUTATION_FX3, e.position(), e.position().add(0, 1, 0), TransmutationArts.TEAL, 16, 0.4f);
        }

        @Override public void tick(MagicPropEntity e, ServerLevel sl) { e.setDeltaMovement(Vec3.ZERO); }
    }

    // ------------------------------------------------------------------ the sentinel
    private static final class Sentinel implements MagicProps.Behavior {
        @Override
        public void init(MagicPropEntity e, ServerLevel sl) {
            float s = e.scale();
            e.setSize(1.0f * s, 2.6f * s);
            setState(e, IDLE);
        }

        @Override
        public void tick(MagicPropEntity e, ServerLevel sl) {
            LivingEntity owner = e.owner();
            if (owner == null) { e.expire(); return; }
            long now = sl.getGameTime();
            int st = state(e.param());
            if (st != IDLE && since(e.param(), now) > 22) { setState(e, IDLE); st = IDLE; }
            if (e.maxLife() - e.life() < 26 || e.life() < 12) { e.setDeltaMovement(Vec3.ZERO); return; }          // rising or crumbling: it stands still
            if ((e.life() + e.seed()) % 8 == 0 || !(e.target() instanceof LivingEntity tl && tl.isAlive() && hostile(owner, tl))) e.setTarget(nearest(e, owner, sl, 12));
            LivingEntity target = e.target() instanceof LivingEntity l && l.isAlive() ? l : null;
            float s = e.scale();
            if (target != null) {
                double d = Math.sqrt(target.distanceToSqr(e));
                if (d > 2.4 * s + target.getBbWidth() * 0.5) { walk(e, target.position(), 0.2); return; }
                walk(e, e.position(), 0);
                face(e, target.position());
                if (st == IDLE && (e.life() + e.seed()) % 24 == 0) {
                    setState(e, ACT);
                    float raw = e.getPersistentData().getFloat("nusmp_sentinel_dmg");
                    final LivingEntity tt = target;
                    com.newuniverse.nusmp.book.SpellRuntime.later(sl, 14, () -> {
                        if (e.isRemoved() || !tt.isAlive() || tt.distanceToSqr(e) > 25 * s * s) return;
                        hit(e, sl, owner, tt, raw <= 0 ? 9f : raw);
                        knock(tt, e.position(), 0.4);
                        VfxSpawn.send(sl, VfxShape.TRANSMUTATION_FX3, tt.position(), tt.position().add(0, 1, 0), TransmutationArts.TEAL, 16, 0.5f);
                    });
                }
                return;
            }
            double d = Math.sqrt(owner.distanceToSqr(e));
            if (d > 28) { e.setPos(owner.getX() + 1.5, owner.getY(), owner.getZ() + 1.5); e.setDeltaMovement(Vec3.ZERO); return; }
            if (d > 3.5) walk(e, owner.position(), Math.min(0.28, 0.14 + (d - 3.5) * 0.02));
            else { walk(e, e.position(), 0); face(e, owner.position().add(owner.getViewVector(1f).scale(4))); }
        }

        @Override
        public void end(MagicPropEntity e, ServerLevel sl) {
            VfxSpawn.send(sl, VfxShape.TRANSMUTATION_FX3, e.position().add(0, 0.3, 0), e.position().add(0, 1.5, 0), TransmutationArts.TEAL, 26, 1.0f);
        }
    }

    // ------------------------------------------------------------------ the blade
    private static final class Blade implements MagicProps.Behavior {
        @Override
        public void init(MagicPropEntity e, ServerLevel sl) {
            e.setSize(0.5f, 0.5f);
            setState(e, IDLE);
        }

        private static Vec3 slot(MagicPropEntity e, LivingEntity owner, float partial) {
            float a = (e.life() + e.seed() % 7) * 0.13f + Mth.TWO_PI * bladeIndex(e.param()) / bladeCount(e.param());
            return owner.position().add(Math.cos(a) * 1.6, 1.3 + 0.25 * Math.sin(a * 2.0), Math.sin(a) * 1.6);
        }

        @Override
        public void tick(MagicPropEntity e, ServerLevel sl) {
            LivingEntity owner = e.owner();
            if (owner == null) { e.expire(); return; }
            long now = sl.getGameTime();
            int st = state(e.param()), since = since(e.param(), now);
            Vec3 home = slot(e, owner, 0f);
            if (st == IDLE) {
                e.setPos(e.position().lerp(home, 0.35));
                if (since > 24 && (e.life() + bladeIndex(e.param()) * 5) % 20 == 0) {
                    LivingEntity t = nearest(e, owner, sl, 12);
                    if (t != null) { e.setTarget(t); setState(e, ACT); }
                }
            } else if (st == ACT) {
                LivingEntity t = e.target() instanceof LivingEntity l && l.isAlive() ? l : null;
                if (t == null || since > 22) { setState(e, ACT2); return; }
                Vec3 c = t.getBoundingBox().getCenter(), d = c.subtract(e.position());
                if (d.length() < 1.1) {
                    float raw = e.getPersistentData().getFloat("nusmp_blade_dmg");
                    hit(e, sl, owner, t, raw <= 0 ? 5f : raw);
                    VfxSpawn.send(sl, VfxShape.TRANSMUTATION_FX3, c, c.add(0, 0.5, 0), TransmutationArts.TEAL, 14, 0.35f);
                    setState(e, ACT2);
                    return;
                }
                e.setPos(e.position().add(d.normalize().scale(Math.min(1.3, d.length()))));
            } else {
                Vec3 d = home.subtract(e.position());
                if (d.length() < 0.8 || since > 20) setState(e, IDLE);
                else e.setPos(e.position().add(d.normalize().scale(Math.min(0.9, d.length()))));
            }
            e.setDeltaMovement(Vec3.ZERO);
        }
    }

    // ------------------------------------------------------------------ CI self-test
    private static SelfTests.Result selfTest(net.minecraft.server.MinecraftServer server) {
        var book = com.newuniverse.nusmp.skill.NUSkills.BOOK_TRANSMUTATION.get();
        if (!(book instanceof com.newuniverse.nusmp.book.GrimoireBook gb)) return SelfTests.Result.fail("book_transmutation is not a grimoire book");
        var pages = gb.pages();
        int old = TransmutationRules.OLD_IDS.size(), total = old + TransmutationRules.NEW_IDS.size();
        if (pages.size() < total) return SelfTests.Result.fail("only " + pages.size() + " pages");
        for (int k = 0; k < total; k++) {
            String want = k < old ? TransmutationRules.OLD_IDS.get(k) : TransmutationRules.NEW_IDS.get(k - old);
            if (!pages.get(k).id().equals(want)) return SelfTests.Result.fail("page " + k + " is " + pages.get(k).id() + ", expected " + want);
        }
        for (PropKind k : new PropKind[]{PropKind.TRANSMUTATION_1, PropKind.TRANSMUTATION_2, PropKind.TRANSMUTATION_3})
            if (MagicProps.behavior(k) == null) return SelfTests.Result.fail("no behaviour for " + k);
        for (String n : new String[]{"helm", "chest", "tasset", "pauldron_r", "pauldron_l", "vambrace_r", "vambrace_l", "greave_r", "greave_l", "back", "halo", "spike", "sentinel", "blade"})
            if (TransmutationProps.class.getResource("/assets/nusmp/geo/entity/transmutation_" + n + ".geo.json") == null) return SelfTests.Result.fail("missing geo model " + n);
        return SelfTests.Result.ok(total + " pages in order, 3 behaviours, 14 geo models");
    }
}
