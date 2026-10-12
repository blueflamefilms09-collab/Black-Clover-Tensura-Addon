package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.blackclover.Devil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * The Dark Triad's bond: Dante (Lucifero), Vanica (Megicula) and Zenon (Beelzebub), the three rulers who hold the three highest
 * devils of the Underworld. A registry of the living members; when one falls the others rage: {@link PowerCurve#rage} adds a
 * 10-point step to each survivor's power, never past 80%, and the boss says so in chat. Lucius (Astaroth) is not part of the bond.
 * Server side only; bosses register themselves on their first tick and leave when they are removed.
 */
public final class TriadBond {
    private TriadBond() {}

    private static final Set<ZogratisBoss> MEMBERS = Collections.newSetFromMap(new IdentityHashMap<>());

    /** Is this boss one of the three siblings (by its devil)? */
    public static boolean isTriad(ZogratisBoss b) {
        Devil d = b.devil();
        return d == Devil.LUCIFERO || d == Devil.MEGICULA || d == Devil.BEELZEBUB;
    }

    public static void register(ZogratisBoss b) {
        if (!b.level().isClientSide && isTriad(b)) MEMBERS.add(b);
    }

    public static void unregister(ZogratisBoss b) { MEMBERS.remove(b); }

    /** The other living siblings in the same world as 'of'. */
    public static List<ZogratisBoss> allies(ZogratisBoss of) {
        List<ZogratisBoss> out = new ArrayList<>();
        for (ZogratisBoss b : new ArrayList<>(MEMBERS)) {
            if (b.isRemoved() || !b.isAlive()) {
                if (b != of) MEMBERS.remove(b);
                continue;
            }
            if (b != of && b.level() == of.level()) out.add(b);
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
        int n = 0;
        for (ZogratisBoss b : new ArrayList<>(MEMBERS)) if (!b.isRemoved() && b.isAlive()) n++;
        return n;
    }
}
