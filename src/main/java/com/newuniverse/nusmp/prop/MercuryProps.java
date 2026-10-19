package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.book.MercuryRules;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Mercury Magic (Nozel Silva), 1.93.0 rebuild: the server behaviours of the constructs (the damage is done by the spells, the props are the bodies).
 * <ul>
 *   <li>MERCURY_1, a flying body of mercury: param 0 = a Silver Blade, 1 = a Silver Spear. It flies along its yaw and pitch at 2.2 blocks a tick for its life (12 to 14 ticks).</li>
 *   <li>MERCURY_2, the Silver Eagle: stays under its owner for the flight (the spell carries the caster along the line of sight), a little below the feet so that the caster stands
 *       on its back, then dissolves with a splash.</li>
 *   <li>MERCURY_3, the cage of the Silver Star of Execution: stands where the spell put it (its scale is the radius over 1.5 blocks) and does nothing on the server.</li>
 * </ul>
 */
public final class MercuryProps {
    private MercuryProps() {}

    /** The eagle's model scale factor (the painter and this class agree on it): a spawn scale of 1 is a 6 block span times this. */
    public static final float EAGLE_MODEL = 0.55f;

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.MERCURY_1, new Flyer());
        MagicProps.register(PropKind.MERCURY_2, new Eagle());
        MagicProps.register(PropKind.MERCURY_3, new Cage());
        SelfTests.register("magic.mercury_rebuild", MercuryProps::selfTest);
    }

    private static final class Flyer implements MagicProps.Behavior {
        @Override
        public void init(MagicPropEntity e, ServerLevel sl) {
            e.setSize(0.5f, 0.5f);
            e.setNoGravity(true);
        }

        @Override
        public void tick(MagicPropEntity e, ServerLevel sl) {
            float yaw = e.getYRot() * Mth.DEG_TO_RAD, pitch = e.getXRot() * Mth.DEG_TO_RAD;
            Vec3 dir = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch));
            e.setPos(e.position().add(dir.scale(2.2)));
            e.setDeltaMovement(Vec3.ZERO);
        }
    }

    private static final class Eagle implements MagicProps.Behavior {
        @Override
        public void init(MagicPropEntity e, ServerLevel sl) {
            e.setSize(2.4f * e.scale(), 1.2f * e.scale());
            e.setNoGravity(true);
        }

        @Override
        public void tick(MagicPropEntity e, ServerLevel sl) {
            LivingEntity owner = e.owner();
            if (owner != null && e.life() <= MercuryRules.EAGLE_FLIGHT_TICKS + 4)
                e.setPos(owner.getX(), owner.getY() - 1.4 * EAGLE_MODEL * e.scale(), owner.getZ());
            e.setDeltaMovement(Vec3.ZERO);
        }

        @Override
        public void end(MagicPropEntity e, ServerLevel sl) {
            VfxSpawn.send(sl, VfxShape.MERCURY_FX3, e.position().add(0, 1, 0), e.position().add(0, 2, 0), 0xFFC8E8FF, 26, 1.4f);
        }
    }

    private static final class Cage implements MagicProps.Behavior {
        @Override
        public void init(MagicPropEntity e, ServerLevel sl) {
            e.setSize(3.0f * e.scale(), 2.8f * e.scale());
            e.setNoGravity(true);
        }

        @Override public void tick(MagicPropEntity e, ServerLevel sl) { e.setDeltaMovement(Vec3.ZERO); }
    }

    // ------------------------------------------------------------------ CI self-test
    private static SelfTests.Result selfTest(net.minecraft.server.MinecraftServer server) {
        var book = com.newuniverse.nusmp.skill.NUSkills.BOOK_MERCURY.get();
        if (!(book instanceof com.newuniverse.nusmp.book.GrimoireBook gb)) return SelfTests.Result.fail("book_mercury is not a grimoire book");
        var pages = gb.pages();
        int old = MercuryRules.OLD_IDS.size(), total = old + MercuryRules.NEW_IDS.size();
        if (pages.size() < total) return SelfTests.Result.fail("only " + pages.size() + " pages");
        for (int k = 0; k < total; k++) {
            String want = k < old ? MercuryRules.OLD_IDS.get(k) : MercuryRules.NEW_IDS.get(k - old);
            if (!pages.get(k).id().equals(want)) return SelfTests.Result.fail("page " + k + " is " + pages.get(k).id() + ", expected " + want);
        }
        for (PropKind k : new PropKind[]{PropKind.MERCURY_1, PropKind.MERCURY_2, PropKind.MERCURY_3})
            if (MagicProps.behavior(k) == null) return SelfTests.Result.fail("no behaviour for " + k);
        for (String n : new String[]{"helm", "chest", "tasset", "pauldron_r", "pauldron_l", "vambrace_r", "vambrace_l", "greave_r", "greave_l", "back", "halo", "blade", "eagle", "spear", "cage"})
            if (MercuryProps.class.getResource("/assets/nusmp/geo/entity/mercury_" + n + ".geo.json") == null) return SelfTests.Result.fail("missing geo model " + n);
        if (MercuryRules.selfHarmAllowed(MercuryRules.SELF_HARM_FLOOR, false)) return SelfTests.Result.fail("the toxin would hurt at the floor");
        return SelfTests.Result.ok(total + " pages in order, 3 behaviours, 15 geo models, the toxin respects its floor");
    }
}
