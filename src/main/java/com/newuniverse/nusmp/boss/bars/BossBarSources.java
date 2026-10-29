package com.newuniverse.nusmp.boss.bars;

import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.boss.bar.BossBarThemes;
import com.newuniverse.nusmp.entity.ZagredBossEntity;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialBossController;
import com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialLayer;
import com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialProfile;
import com.newuniverse.nusmp.labyrinth.mega.guard.DndProfile;
import com.newuniverse.nusmp.labyrinth.mega.guard.GuardianScaling;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import com.newuniverse.nusmp.labyrinth.mega.guard.PocketBossBridge;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: {@link BossBarSource} is the one interface, and this is where each family of bosses of the mod gets its answers WITHOUT its class being rewritten: an adapter per family reads what
 * the boss already exposes (the power curve and the wind-up of the Zogratis bosses and the arcs, the phase machine and lair clock of the Decennial layer, the D&D phase of a pocket's mega
 * boss, the phases of the Spirit King and of the old Zagred) and a default one reads any {@link LivingEntity}. A boss that implements {@link BossBarSource} itself is used as it is. Server thread
 * only; the adapters are cheap (a few reads) because the hub asks each one at most five times a second.
 */
public final class BossBarSources {
    private BossBarSources() {}

    /** The source of a boss: the entity itself, else the adapter of its family, else the generic one. */
    public static BossBarSource of(LivingEntity e) {
        if (e instanceof BossBarSource s) return s;
        if (e instanceof ZogratisBoss z) return new Zogratis(z);
        if (e instanceof LabyrinthGuardianEntity g) return new Guardian(g);
        if (e instanceof ZagredBossEntity z) return new OldZagred(z);
        return new Generic(e);
    }

    /** The style of an entity from its type alone (null when the type is not one of the named bosses). */
    public static String styleOfType(LivingEntity e) {
        return BarStyleResolver.forEntityPath(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath());
    }

    // ================================================================ the generic one
    /** Any living entity: the style of its type (the Warden frame when the type is not a named boss), the shield from its absorption, the fighters from the players near it. */
    public static class Generic implements BossBarSource {
        protected final LivingEntity e;

        public Generic(LivingEntity e) { this.e = e; }

        @Override public String styleId() {
            String s = styleOfType(e);
            return s != null ? s : BarStyleIds.WARDEN;
        }

        @Override public float shield() {
            float max = e.getMaxHealth();
            return max <= 0f ? 0f : Math.max(0f, Math.min(1f, e.getAbsorptionAmount() / max));
        }

        @Override public boolean invulnerable() { return e.isInvulnerable(); }

        @Override public int fighters() { return playersNear(e, 48.0); }
    }

    /** Players (alive, not spectating) within {@code range} of the entity. */
    public static int playersNear(LivingEntity e, double range) {
        if (!(e.level() instanceof ServerLevel sl)) return -1;
        double r2 = range * range;
        int n = 0;
        for (Player p : sl.players()) if (p.isAlive() && !p.isSpectator() && p.distanceToSqr(e) <= r2) n++;
        return n;
    }

    // ================================================================ the Zogratis family and the arcs
    private static final List<Integer> POWER_THRESHOLDS = List.of(85, 65, 45, 25);

    /** The Zogratis bosses (Dante, Vanica, Zenon, the Devil Avatar, Lucius, Goliath, Charybdis, the God of Order) and every character of the arcs. */
    public static final class Zogratis extends Generic {
        private final ZogratisBoss z;

        public Zogratis(ZogratisBoss z) {
            super(z);
            this.z = z;
        }

        @Override public String styleId() {
            if (z instanceof ArcBoss a) {
                String byType = styleOfType(z);
                if (byType != null) return byType;
                ArcCharacter c = a.character();
                return BarStyleResolver.forArc(c.saga().id(), c.id());
            }
            String s = styleOfType(z);
            return s != null ? s : BarStyleIds.ZOGRATIS;
        }

        @Override public String name() { return z.bossName(); }

        @Override public String title() {
            if (z instanceof ArcBoss a) return a.character().title();
            return z.devil().displayName;
        }

        @Override public int phase() { return PowerCurve.stage(z.power()) + 1; }

        @Override public int phaseCount() { return 5; }

        @Override public List<Integer> phaseThresholds() { return POWER_THRESHOLDS; }

        @Override public String phaseName() { return z.gatesOpen() ? "@nusmp.bossbar.gates_open" : z.power() + "%"; }

        @Override public boolean invulnerable() { return z.refilling() || z.transforming(); }

        @Override public boolean enraged() { return BossBarThemes.rage(z.power()); }

        @Override public String castName() {
            if (z.transforming()) return "@nusmp.bossbar.transforming";
            return z.state() == ZogratisBoss.STATE_CASTING ? z.castLabel() : "";
        }

        @Override public float castProgress() {
            if (z.transforming() || z.state() == ZogratisBoss.STATE_CASTING) return z.phaseProgress();
            return -1f;
        }

        @Override public int fighters() { return z.fightActive() ? Math.max(1, z.fighters().size()) : 0; }
    }

    // ================================================================ the Labyrinth Guardian: Decennial bosses, D&D bosses, pocket mega-bosses
    public static final class Guardian extends Generic {
        private final LabyrinthGuardianEntity g;

        public Guardian(LabyrinthGuardianEntity g) {
            super(g);
            this.g = g;
        }

        private DecennialLayer layer() { return g.decennial(); }

        private boolean decennial() { return layer().active() && layer().controller() != null; }

        private boolean pocket() { return g.getTags().contains("nusmp_tomb_boss"); }

        @Override public String styleId() {
            if (decennial()) {
                DecennialProfile p = layer().profile();
                return BarStyleResolver.forDecennial(p.side(), p.archetype(), p.theme(), p.creature(), p.devil());
            }
            return BarStyleResolver.forGuardian(pocket());
        }

        @Override public String name() { return decennial() ? layer().profile().name() : ""; }

        @Override public String title() { return decennial() ? layer().profile().title() : ""; }

        @Override public int phase() {
            if (decennial()) return layer().controller().phase();
            return PocketBossBridge.phase(g);
        }

        @Override public int phaseCount() {
            if (decennial()) return layer().profile().phaseCount();
            DndProfile p = PocketBossBridge.profile(g);
            return p != null && p.mythic() ? 3 : 2;
        }

        @Override public List<Integer> phaseThresholds() {
            if (!decennial()) return List.of((int) Math.round(GuardianScaling.PHASE_TWO_AT * 100.0));
            DecennialProfile p = layer().profile();
            double[] at = new double[p.phaseCount()];
            for (int i = 0; i < at.length; i++) at[i] = p.phase(i + 1).at();
            return PhaseMarkers.fromShares(at);
        }

        @Override public String phaseName() {
            if (decennial()) return layer().profile().phase(layer().controller().phase()).name();
            DndProfile p = PocketBossBridge.profile(g);
            return p == null ? "" : p.form(PocketBossBridge.phase(g));
        }

        @Override public boolean invulnerable() { return decennial() && layer().controller().invulnerable(g.level().getGameTime()); }

        @Override public boolean enraged() { return decennial() && "enrage".equals(layer().profile().signature()) && layer().controller().isFinalPhase(); }

        @Override public String lairId() {
            DecennialBossController.NextLair n = nextLair();
            return n == null ? "" : n.id();
        }

        @Override public int lairTicks() {
            DecennialBossController.NextLair n = nextLair();
            return n == null ? -1 : (int) Math.max(0, n.at() - g.level().getGameTime());
        }

        private DecennialBossController.NextLair nextLair() { return decennial() ? layer().controller().nextLair() : null; }

        @Override public int adds() { return decennial() && layer().profile().adds().maxAlive() > 0 ? layer().addsAlive() : -1; }

        @Override public int fighters() { return decennial() && !layer().controller().fighting() ? 0 : playersNear(g, 48.0); }
    }

    // ================================================================ the old Zagred
    public static final class OldZagred extends Generic {
        private final ZagredBossEntity z;

        public OldZagred(ZagredBossEntity z) {
            super(z);
            this.z = z;
        }

        @Override public String styleId() { return BarStyleIds.ZAGRED; }

        @Override public int phase() { return z.phase(); }

        @Override public int phaseCount() { return 4; }

        @Override public List<Integer> phaseThresholds() { return new ArrayList<>(List.of(75, 50, 25)); }
    }
}
