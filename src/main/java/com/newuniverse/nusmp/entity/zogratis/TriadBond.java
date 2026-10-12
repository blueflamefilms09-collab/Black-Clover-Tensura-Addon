package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.blackclover.Devil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * The Dark Triad's bond: Dante (Lucifero), Vanica (Megicula) and Zenon (Beelzebub), the three rulers who hold the three highest
 * devils of the Underworld. A registry of the living members; when one falls the others rage: {@link PowerCurve#rage} adds a
 * 10-point step to each survivor's power, never past 80%, and the boss says so in chat. Lucius (Astaroth) is not part of the bond.
 * Server side only; bosses register themselves on their first tick and leave when they are removed. The registry holds its members
 * weakly and drops every removed, dead or collected one whenever it is touched, so a boss that was unloaded with its chunk (which
 * never reaches {@code remove}) or a world that was closed cannot keep anything alive or be counted in a later world.
 */
public final class TriadBond {
    private TriadBond() {}

    private static final List<WeakReference<ZogratisBoss>> MEMBERS = new ArrayList<>();

    /** Is this boss one of the three siblings (by its devil)? */
    public static boolean isTriad(ZogratisBoss b) {
        Devil d = b.devil();
        return d == Devil.LUCIFERO || d == Devil.MEGICULA || d == Devil.BEELZEBUB;
    }

    public static void register(ZogratisBoss b) {
        if (b.level().isClientSide || !isTriad(b)) return;
        purge(null);
        for (WeakReference<ZogratisBoss> r : MEMBERS) if (r.get() == b) return;
        MEMBERS.add(new WeakReference<>(b));
    }

    public static void unregister(ZogratisBoss b) { purge(b); }

    /** Drops collected, removed and dead members, and 'drop' itself if given. */
    private static void purge(ZogratisBoss drop) {
        MEMBERS.removeIf(r -> {
            ZogratisBoss m = r.get();
            return m == null || m == drop || m.isRemoved();
        });
    }

    /** The other living siblings in the same world as 'of'. */
    public static List<ZogratisBoss> allies(ZogratisBoss of) {
        purge(null);
        List<ZogratisBoss> out = new ArrayList<>();
        for (WeakReference<ZogratisBoss> r : MEMBERS) {
            ZogratisBoss b = r.get();
            if (b != null && b != of && b.isAlive() && b.level() == of.level()) out.add(b);
        }
        return out;
    }

    /** A sibling fell: every other living sibling takes one rage step and says so. Returns how many raged. */
    public static int onFall(ZogratisBoss fallen) {
        if (!isTriad(fallen)) return 0;
        int n = 0;
        for (ZogratisBoss b : allies(fallen)) {
            b.enrage(fallen.bossName());
            n++;
        }
        return n;
    }

    /** Number of registered living siblings (for tests and commands). */
    public static int living() {
        purge(null);
        int n = 0;
        for (WeakReference<ZogratisBoss> r : MEMBERS) {
            ZogratisBoss b = r.get();
            if (b != null && b.isAlive()) n++;
        }
        return n;
    }
}
