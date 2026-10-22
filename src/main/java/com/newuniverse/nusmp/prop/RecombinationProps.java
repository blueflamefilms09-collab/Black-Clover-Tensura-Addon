package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.RecombinationArts;
import com.newuniverse.nusmp.book.RecombinationRules;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Recombination Magic (Henry Legolant), 1.93.0 rebuild: the server behaviours of the magic house's constructs (the old ones were two floating plank props).
 * <ul>
 *   <li>RECOMBINATION_1, a recombined room: stands where it was put (Bulwark's corners, a Room Swap's two ends, Room Rebuild); param 1 = a swap marker (plays the swap clip).</li>
 *   <li>RECOMBINATION_2, a ring of boards: the flourish every spell effect of this magic raises (ElementBook.vfx spawns it for the effects drawn through the book) and the Plank Snare's
 *       cage. It does nothing on the server.</li>
 *   <li>RECOMBINATION_3, THE RAGING BLACK BULL: follows its owner, walks to the nearest hostile within 20 blocks, RAMS it (state 1, a lunge of 14 ticks and a blow in a circle round the Bull)
 *       and fires its CANNONS (state 2, a volley at the nearest foes every 120 ticks). The Ultra Giant Bull (bit 18 of param, the start time in bits 19-29) is 1.6 times as big, fires four
 *       cannons every 40 ticks and rams harder. Assembles over 26 ticks and collapses over its last 24. The state word is synced in {@code param}: bits 0-1 the state, bits 2-17 the game
 *       time (mod 65536) at which it began.</li>
 *   <li>RECOMBINATION_4, the drill of Mana Corkscrew: flies along the caster's view at 1.7 blocks a tick for its 14 ticks (the spell does the damage).</li>
 * </ul>
 * Bosses are knocked back a third as far; every blow goes through the balance law; the volleys are capped at five targets.
 */
public final class RecombinationProps {
    private RecombinationProps() {}

    public static final int IDLE = 0, RAM = 1, FIRE = 2;

    public static void init() {
        RecombinationArts.hooks();
        MagicProps.register(PropKind.RECOMBINATION_1, new Room());
        MagicProps.register(PropKind.RECOMBINATION_2, new Boards());
        MagicProps.register(PropKind.RECOMBINATION_3, new Bull());
        MagicProps.register(PropKind.RECOMBINATION_4, new Drill());
        SelfTests.register("magic.recombination_rebuild", RecombinationProps::selfTest);
        RecombAnimeSelfTest.register();                                                  // magic.recomb_anime: the anime look's assets, the older look kept, the rules
    }

    // ------------------------------------------------------------------ the synced state word
    public static int state(int param) { return param & 3; }
    public static int since(int param, long gameTime) { return (int) ((gameTime - ((param >> 2) & 0xFFFF)) & 0xFFFF); }
    public static boolean ultra(int param) { return (param & RecombinationRules.ULTRA_BIT) != 0; }
    /** Ticks since the Ultra form began (0 when it is not on). */
    public static int ultraSince(int param, long gameTime) { return ultra(param) ? (int) ((gameTime - ((param >> 19) & 0x7FF)) & 0x7FF) : 0; }

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

    private static List<LivingEntity> foes(MagicPropEntity e, LivingEntity owner, ServerLevel sl, double range) {
        List<LivingEntity> list = new ArrayList<>(sl.getEntitiesOfClass(LivingEntity.class, new AABB(e.position(), e.position()).inflate(range, 5, range), t -> hostile(owner, t)));
        list.sort((a, b) -> Double.compare(a.distanceToSqr(e), b.distanceToSqr(e)));
        return list;
    }

    private static void hit(MagicPropEntity e, ServerLevel sl, LivingEntity owner, LivingEntity t, float raw) {
        DamageSource src = sl.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(TensuraDamageTypes.MAGIC_GENERIC)
                .<DamageSource>map(h -> new DamageSource(h, e, owner)).orElseGet(() -> owner.damageSources().indirectMagic(e, owner));
        t.hurt(src, BalanceLaw.damage(owner, t, raw, 0.5));
    }

    private static void knock(LivingEntity t, Vec3 from, double k) {
        Vec3 out = t.position().subtract(from).multiply(1, 0, 1);
        if (out.lengthSqr() < 1e-4) return;
        t.setDeltaMovement(t.getDeltaMovement().add(out.normalize().scale(BalanceLaw.isBoss(t) ? k / 3 : k)).add(0, 0.2, 0));
        t.hurtMarked = true;
    }

    private static void face(MagicPropEntity e, Vec3 to) {
        double dx = to.x - e.getX(), dz = to.z - e.getZ();
        if (dx * dx + dz * dz < 1e-4) return;
        float want = (float) (Mth.atan2(dz, dx) * 57.29577951308232) - 90f, cur = e.getYRot();
        e.setYRot(cur + Mth.clamp(Mth.wrapDegrees(want - cur), -14f, 14f));
    }

    private static void walk(MagicPropEntity e, Vec3 to, double speed) {
        Vec3 d = to.subtract(e.position()).multiply(1, 0, 1);
        double len = d.length();
        Vec3 dm = e.getDeltaMovement();
        double vy = e.onGround() ? -0.02 : Math.max(-0.6, dm.y - 0.08);
        Vec3 h = len < 1e-4 ? Vec3.ZERO : d.scale(Math.min(speed, len) / len);
        if (e.horizontalCollision && e.onGround()) vy = 0.5;
        e.setDeltaMovement(h.x, vy, h.z);
        e.move(MoverType.SELF, e.getDeltaMovement());
        if (len > 1e-4) face(e, to);
    }

    // ------------------------------------------------------------------ rooms and boards
    private static final class Room implements MagicProps.Behavior {
        @Override
        public void init(MagicPropEntity e, ServerLevel sl) {
            e.setSize(1.4f * e.scale(), 1.6f * e.scale());
            e.setNoGravity(true);
        }

        @Override public void tick(MagicPropEntity e, ServerLevel sl) { e.setDeltaMovement(Vec3.ZERO); }
    }

    private static final class Boards implements MagicProps.Behavior {
        @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setNoGravity(true); }

        @Override public void tick(MagicPropEntity e, ServerLevel sl) { e.setDeltaMovement(Vec3.ZERO); }
    }

    // ------------------------------------------------------------------ the drill
    private static final class Drill implements MagicProps.Behavior {
        @Override
        public void init(MagicPropEntity e, ServerLevel sl) {
            e.setSize(0.6f, 0.6f);
            e.setNoGravity(true);
        }

        @Override
        public void tick(MagicPropEntity e, ServerLevel sl) {
            float yaw = e.getYRot() * Mth.DEG_TO_RAD, pitch = e.getXRot() * Mth.DEG_TO_RAD;
            Vec3 dir = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch));
            e.setPos(e.position().add(dir.scale(1.7)));
            e.setDeltaMovement(Vec3.ZERO);
        }
    }

    // ------------------------------------------------------------------ the Black Bull
    private static final class Bull implements MagicProps.Behavior {
        @Override
        public void init(MagicPropEntity e, ServerLevel sl) {
            float s = e.scale();
            e.setSize(2.2f * s, 3.0f * s);
            setState(e, IDLE);
        }

        private static float size(MagicPropEntity e) {
            return e.scale() * (ultra(e.param()) ? 1.6f : 1f);
        }

        @Override
        public void tick(MagicPropEntity e, ServerLevel sl) {
            LivingEntity owner = e.owner();
            if (owner == null) { e.expire(); return; }
            long now = sl.getGameTime();
            int param = e.param();
            if (ultra(param) && ultraSince(param, now) > RecombinationRules.ULTRA_TICKS) {                       // the Ultra form wears off
                e.setParam(param & 0x3FFFF);
                e.setSize(2.2f * e.scale(), 3.0f * e.scale());
                param = e.param();
            }
            boolean big = ultra(param);
            int st = state(param);
            if (st != IDLE && since(param, now) > (st == RAM ? 28 : 12)) { setState(e, IDLE); st = IDLE; }
            if (e.life() < 26 || e.maxLife() - e.life() < 24) { e.setDeltaMovement(Vec3.ZERO); return; }           // assembling or collapsing
            float s = size(e);
            float dmgScale = Math.max(0.5f, e.getPersistentData().getFloat("nusmp_bull_dmg"));
            List<LivingEntity> foes = (e.life() + e.seed()) % 6 == 0 || e.target() == null ? foes(e, owner, sl, 22) : List.of();
            if (!foes.isEmpty()) e.setTarget(foes.get(0));
            LivingEntity target = e.target() instanceof LivingEntity l && l.isAlive() && hostile(owner, l) ? l : null;
            // the cannons: a volley at the nearest foes
            int cd = big ? RecombinationRules.ULTRA_CANNON_COOLDOWN : RecombinationRules.CANNON_COOLDOWN;
            if (target != null && (e.life() + e.seed()) % cd == 0 && target.distanceToSqr(e) < 24 * 24) {
                List<LivingEntity> shots = foes(e, owner, sl, 24);
                int n = Math.min(5, RecombinationRules.cannonShots(big, com.newuniverse.nusmp.book.EnergyBridge.modifier(owner)));
                setState(e, FIRE);
                st = FIRE;
                Vec3 yawSide = new Vec3(Mth.cos(e.getYRot() * Mth.DEG_TO_RAD), 0, Mth.sin(e.getYRot() * Mth.DEG_TO_RAD));
                for (int k = 0; k < n && !shots.isEmpty(); k++) {
                    LivingEntity t = shots.get(k % shots.size());
                    Vec3 from = e.position().add(yawSide.scale((k % 2 == 0 ? 1 : -1) * 0.95 * s)).add(0, 1.95 * s, 0), to = t.getBoundingBox().getCenter();
                    VfxSpawn.send(sl, VfxShape.RECOMBINATION_FX1, from, to, RecombinationArts.ORANGE, 12, 0.7f);
                    final LivingEntity tt = t;
                    com.newuniverse.nusmp.book.SpellRuntime.later(sl, 6 + k * 2, () -> {
                        if (e.isRemoved() || !tt.isAlive()) return;
                        hit(e, sl, owner, tt, RecombinationRules.cannonDamage(big) * dmgScale);
                        VfxSpawn.send(sl, VfxShape.RECOMBINATION_FX3, tt.position(), tt.position().add(0, 1, 0), RecombinationArts.ORANGE, 14, 0.4f);
                    });
                }
            }
            if (target != null) {
                double d = Math.sqrt(target.distanceToSqr(e));
                if (st == RAM && since(e.param(), now) < 14) { walk(e, target.position(), big ? 0.62 : 0.55); return; }
                if (d > 3.4 * s + target.getBbWidth() * 0.5) { walk(e, target.position(), big ? 0.36 : 0.3); return; }
                walk(e, e.position(), 0);
                face(e, target.position());
                if (st == IDLE && (e.life() + e.seed()) % RecombinationRules.RAM_COOLDOWN == 0) {
                    setState(e, RAM);
                    com.newuniverse.nusmp.book.SpellRuntime.later(sl, 12, () -> {
                        if (e.isRemoved()) return;
                        int n = 0;
                        for (LivingEntity t : foes(e, owner, sl, 3.6 * s)) {
                            if (n++ >= 6) break;
                            hit(e, sl, owner, t, RecombinationRules.ramDamage(big) * dmgScale);
                            knock(t, e.position(), 1.0);
                        }
                        VfxSpawn.send(sl, VfxShape.RECOMBINATION_FX3, e.position(), e.position().add(0, 1, 0), RecombinationArts.ORANGE, 24, 1.0f * s);
                    });
                }
                return;
            }
            double d = Math.sqrt(owner.distanceToSqr(e));
            if (d > 30) { e.setPos(owner.getX() + 2.5, owner.getY(), owner.getZ() + 2.5); e.setDeltaMovement(Vec3.ZERO); return; }
            if (d > 5.0) walk(e, owner.position(), Math.min(0.4, 0.2 + (d - 5.0) * 0.03));
            else { walk(e, e.position(), 0); face(e, owner.position().add(owner.getViewVector(1f).scale(6))); }
        }

        @Override
        public void end(MagicPropEntity e, ServerLevel sl) {
            VfxSpawn.send(sl, VfxShape.RECOMBINATION_FX3, e.position().add(0, 0.5, 0), e.position().add(0, 2, 0), RecombinationArts.ORANGE, 30, 1.6f);
        }
    }

    // ------------------------------------------------------------------ CI self-test
    private static SelfTests.Result selfTest(net.minecraft.server.MinecraftServer server) {
        var book = com.newuniverse.nusmp.skill.NUSkills.BOOK_RECOMBINATION.get();
        if (!(book instanceof com.newuniverse.nusmp.book.GrimoireBook gb)) return SelfTests.Result.fail("book_recombination is not a grimoire book");
        var pages = gb.pages();
        int old = RecombinationRules.OLD_IDS.size(), total = old + RecombinationRules.NEW_IDS.size();
        if (pages.size() < total) return SelfTests.Result.fail("only " + pages.size() + " pages");
        for (int k = 0; k < total; k++) {
            String want = k < old ? RecombinationRules.OLD_IDS.get(k) : RecombinationRules.NEW_IDS.get(k - old);
            if (!pages.get(k).id().equals(want)) return SelfTests.Result.fail("page " + k + " is " + pages.get(k).id() + ", expected " + want);
        }
        for (PropKind k : new PropKind[]{PropKind.RECOMBINATION_1, PropKind.RECOMBINATION_2, PropKind.RECOMBINATION_3, PropKind.RECOMBINATION_4})
            if (MagicProps.behavior(k) == null) return SelfTests.Result.fail("no behaviour for " + k);
        for (String n : new String[]{"helm", "chest", "tasset", "pauldron_r", "pauldron_l", "vambrace_r", "vambrace_l", "greave_r", "greave_l", "back", "halo", "bull", "room", "drill"})
            if (RecombinationProps.class.getResource("/assets/nusmp/geo/entity/recombination_" + n + ".geo.json") == null) return SelfTests.Result.fail("missing geo model " + n);
        return SelfTests.Result.ok(total + " pages in order, 4 behaviours, 14 geo models");
    }
}
