package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.ModificationArts;
import com.newuniverse.nusmp.book.ModificationRules;
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
 * Modification Magic (Morris Libardirt), 1.93.0 rebuild: the server behaviours of the constructs.
 * <ul>
 *   <li>MODIFICATION_1, a clawed tendril: out of the ground where Clawed Tendrils aims it; it rears and strikes at tick 12 (state 1 plays the strike clip; the spell does the damage). In
 *       the Dissection Theatre (the spell stores the ring's centre, angle and length in the prop's data) it also walks the ring inward, from 8 blocks to 3, slower than a walker.</li>
 *   <li>MODIFICATION_2, a test subject: follows its owner, walks to the nearest hostile within 12 blocks and cuts it with its blade arm (a blow every 18 ticks, a claw swipe between),
 *       rises out of the ground and collapses over its last 20 ticks. State word in param: bits 0-1 the state (1 = strike), bits 2-17 the game time (mod 65536) it began.</li>
 *   <li>MODIFICATION_3, a dissection needle: flies along its direction (yaw and pitch of the prop) at 1.8 blocks a tick for its 16 ticks; the spell does the damage.</li>
 * </ul>
 */
public final class ModificationProps {
    private ModificationProps() {}

    public static final int IDLE = 0, ACT = 1;

    public static void init() {
        ModificationArts.hooks();
        MagicProps.register(PropKind.MODIFICATION_1, new Tendril());
        MagicProps.register(PropKind.MODIFICATION_2, new Subject());
        MagicProps.register(PropKind.MODIFICATION_3, new Needle());
        SelfTests.register("magic.modification_rebuild", ModificationProps::selfTest);
    }

    public static int state(int param) { return param & 3; }
    public static int since(int param, long gameTime) { return (int) ((gameTime - ((param >> 2) & 0xFFFF)) & 0xFFFF); }

    private static void setState(MagicPropEntity e, int st) {
        long now = e.level().getGameTime();
        e.setParam((e.param() & ~0x3FFFF) | st | (int) ((now & 0xFFFF) << 2));
    }

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

    // ------------------------------------------------------------------ the tendril
    private static final class Tendril implements MagicProps.Behavior {
        @Override
        public void init(MagicPropEntity e, ServerLevel sl) {
            e.setSize(1.0f * e.scale(), 3.0f * e.scale());
            e.setNoGravity(true);
            VfxSpawn.send(sl, VfxShape.MODIFICATION_FX3, e.position(), e.position().add(0, 1, 0), ModificationArts.ORANGE, 14, 0.4f);
        }

        @Override
        public void tick(MagicPropEntity e, ServerLevel sl) {
            var d = e.getPersistentData();
            if (d.contains("tc_life")) {                                                                  // a tendril of the Dissection Theatre's ring
                double r = ModificationRules.theatreRadius(e.life(), d.getInt("tc_life")), a = d.getDouble("tc_a");
                e.setPos(d.getDouble("tc_x") + Math.cos(a) * r, e.getY(), d.getDouble("tc_z") + Math.sin(a) * r);
                e.setYRot((float) Math.toDegrees(Math.atan2(-(d.getDouble("tc_x") - e.getX()), d.getDouble("tc_z") - e.getZ())));
            } else if (e.life() == 12) setState(e, ACT);
            e.setDeltaMovement(Vec3.ZERO);
        }

        @Override
        public void end(MagicPropEntity e, ServerLevel sl) {
            VfxSpawn.send(sl, VfxShape.MODIFICATION_FX3, e.position(), e.position().add(0, 1, 0), ModificationArts.ORANGE, 14, 0.4f);
        }
    }

    // ------------------------------------------------------------------ the test subject
    private static final class Subject implements MagicProps.Behavior {
        @Override
        public void init(MagicPropEntity e, ServerLevel sl) {
            e.setSize(0.8f * e.scale(), 2.0f * e.scale());
            setState(e, IDLE);
        }

        @Override
        public void tick(MagicPropEntity e, ServerLevel sl) {
            LivingEntity owner = e.owner();
            if (owner == null) { e.expire(); return; }
            long now = sl.getGameTime();
            int st = state(e.param());
            if (st != IDLE && since(e.param(), now) > 18) { setState(e, IDLE); st = IDLE; }
            if (e.life() < 20 || e.maxLife() - e.life() < 20) { e.setDeltaMovement(Vec3.ZERO); return; }
            if ((e.life() + e.seed()) % 8 == 0 || !(e.target() instanceof LivingEntity tl && tl.isAlive() && hostile(owner, tl))) e.setTarget(nearest(e, owner, sl, 12));
            LivingEntity target = e.target() instanceof LivingEntity l && l.isAlive() ? l : null;
            float s = e.scale();
            if (target != null) {
                double d = Math.sqrt(target.distanceToSqr(e));
                if (d > 1.8 * s + target.getBbWidth() * 0.5) { walk(e, target.position(), 0.27); return; }
                walk(e, e.position(), 0);
                face(e, target.position());
                if (st == IDLE && (e.life() + e.seed()) % 18 == 0) {
                    setState(e, ACT);
                    float raw = e.getPersistentData().getFloat("nusmp_subject_dmg");
                    final LivingEntity tt = target;
                    com.newuniverse.nusmp.book.SpellRuntime.later(sl, 9, () -> {
                        if (e.isRemoved() || !tt.isAlive() || tt.distanceToSqr(e) > 16 * s * s) return;
                        hit(e, sl, owner, tt, raw <= 0 ? 7f : raw);
                        VfxSpawn.send(sl, VfxShape.MODIFICATION_FX3, tt.position(), tt.position().add(0, 1, 0), ModificationArts.ORANGE, 12, 0.35f);
                    });
                }
                return;
            }
            double d = Math.sqrt(owner.distanceToSqr(e));
            if (d > 28) { e.setPos(owner.getX() + 1.5, owner.getY(), owner.getZ() + 1.5); e.setDeltaMovement(Vec3.ZERO); return; }
            if (d > 3.0) walk(e, owner.position().add(Math.cos(e.seed()) * 1.8, 0, Math.sin(e.seed()) * 1.8), 0.24);
            else walk(e, e.position(), 0);
        }

        @Override
        public void end(MagicPropEntity e, ServerLevel sl) {
            VfxSpawn.send(sl, VfxShape.MODIFICATION_FX3, e.position().add(0, 0.3, 0), e.position().add(0, 1.5, 0), ModificationArts.ORANGE, 24, 0.8f);
        }
    }

    // ------------------------------------------------------------------ the needle
    private static final class Needle implements MagicProps.Behavior {
        @Override
        public void init(MagicPropEntity e, ServerLevel sl) {
            e.setSize(0.4f, 0.4f);
            e.setNoGravity(true);
        }

        @Override
        public void tick(MagicPropEntity e, ServerLevel sl) {
            float yaw = e.getYRot() * Mth.DEG_TO_RAD, pitch = e.getXRot() * Mth.DEG_TO_RAD;
            Vec3 dir = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch));
            e.setPos(e.position().add(dir.scale(1.8)));
            e.setDeltaMovement(Vec3.ZERO);
        }
    }

    // ------------------------------------------------------------------ CI self-test
    private static SelfTests.Result selfTest(net.minecraft.server.MinecraftServer server) {
        var holder = com.newuniverse.nusmp.skill.NUSkills.WIKI_ATTRIBUTE_BOOKS.get(com.newuniverse.nusmp.blackclover.MagicType.MODIFICATION);
        if (holder == null) return SelfTests.Result.fail("no book registered for Modification");
        var book = holder.get();
        if (!(book instanceof com.newuniverse.nusmp.book.GrimoireBook gb)) return SelfTests.Result.fail("book_modification is not a grimoire book");
        var pages = gb.pages();
        int old = ModificationRules.OLD_IDS.size(), total = old + ModificationRules.NEW_IDS.size();
        if (pages.size() < total) return SelfTests.Result.fail("only " + pages.size() + " pages");
        for (int k = 0; k < total; k++) {
            String want = k < old ? ModificationRules.OLD_IDS.get(k) : ModificationRules.NEW_IDS.get(k - old);
            if (!pages.get(k).id().equals(want)) return SelfTests.Result.fail("page " + k + " is " + pages.get(k).id() + ", expected " + want);
        }
        for (PropKind k : new PropKind[]{PropKind.MODIFICATION_1, PropKind.MODIFICATION_2, PropKind.MODIFICATION_3})
            if (MagicProps.behavior(k) == null) return SelfTests.Result.fail("no behaviour for " + k);
        for (String n : new String[]{"helm", "chest", "tasset", "pauldron_r", "pauldron_l", "vambrace_r", "vambrace_l", "greave_r", "greave_l", "back", "halo", "tendril", "needle", "subject"})
            if (ModificationProps.class.getResource("/assets/nusmp/geo/entity/modification_" + n + ".geo.json") == null) return SelfTests.Result.fail("missing geo model " + n);
        var ledger = new ModificationRules.Ledger();
        ledger.note(ModificationRules.Kind.MELEE, 20, 0);
        if (ledger.multiplier(ModificationRules.Kind.MELEE, 0, 0.3f) >= 1f) return SelfTests.Result.fail("the ledger does not adapt to a kind that leads");
        return SelfTests.Result.ok(total + " pages in order, 3 behaviours, 14 geo models, the ledger adapts");
    }
}
