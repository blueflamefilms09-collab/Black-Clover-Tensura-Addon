package com.newuniverse.nusmp.attune;

import com.newuniverse.nusmp.beastwill.BeastWillCatalog;
import com.newuniverse.nusmp.beastwill.BeastWillDef;
import com.newuniverse.nusmp.beastwill.BeastWillState;
import com.newuniverse.nusmp.beastwill.BeastWillStore;
import com.newuniverse.nusmp.devil.DevilContract;
import com.newuniverse.nusmp.devil.DevilRules;
import com.newuniverse.nusmp.devil.DevilStore;
import com.newuniverse.nusmp.devil.WorldDevil;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import com.newuniverse.nusmp.relics.djinn.DjinnAttachments;
import com.newuniverse.nusmp.relics.djinn.DjinnPhaseRules;
import com.newuniverse.nusmp.relics.djinn.DjinnState;
import com.newuniverse.nusmp.spiritcontrol.SpiritControl;
import com.newuniverse.nusmp.spiritcontrol.SpiritHandle;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Why: the four systems keep their own saves; this is the one place that reads them as {@link AttuneRules.Power}s (an id, a system, the elements, a strength), so the rules never learn a system's types.
 * {@link #owned} is what the meditation screen offers (asked on /meditate and on a confirm only); {@link #out} is what is out right now (asked per blow the player deals, so it only reads: attachment
 * lookups that never create one, one map lookup per registered spirit). Server thread only.
 */
public final class AttunePowers {
    private AttunePowers() {}

    /** A power plus the name the screen shows. */
    public record View(AttuneRules.Power power, String label) {}

    /** Every power the player holds now: each bound Beast Will, their Djinn (own or lent), their Devil contract, every Spirit that answers them. Capped at {@link AttuneRules#MAX_OWNED}. */
    public static List<View> owned(ServerPlayer p) {
        List<View> out = new ArrayList<>();
        BeastWillState b = BeastWillStore.peek(p);
        if (b != null) for (String id : b.wills.keySet()) out.add(new View(beast(id), label(id)));
        Djinn d = djinn(p);
        if (d != null) out.add(new View(djinnPower(p, d), d.displayName()));
        DevilContract c = DevilStore.peek(p);
        if (c != null && c.hasContract()) out.add(new View(devil(c), devilLabel(c)));
        for (SpiritHandle h : SpiritControl.eligible(p)) out.add(new View(spirit(h), h.display()));
        return out.size() > AttuneRules.MAX_OWNED ? new ArrayList<>(out.subList(0, AttuneRules.MAX_OWNED)) : out;
    }

    /** The powers that are out right now (a held Beast Will form, a Djinn from the Lesser Summon up, a Devil let in, a Spirit beside the player). */
    public static List<AttuneRules.Power> out(ServerPlayer p) {
        List<AttuneRules.Power> out = new ArrayList<>(4);
        BeastWillState b = BeastWillStore.peek(p);
        if (b != null && !b.active.isEmpty() && b.phase > 0) out.add(beast(b.active));
        if (p.hasData(DjinnAttachments.STATE)) {
            DjinnState s = DjinnState.of(p);
            Djinn d = s.active();
            if (d != null && s.phase() >= DjinnPhaseRules.LESSER) out.add(djinnPower(p, d));
        }
        DevilContract c = DevilStore.peek(p);
        if (c != null && c.hasContract() && DevilRules.effectPower(c.tier, c.assimilation, c.inUnion()) > 0) out.add(devil(c));
        for (SpiritHandle h : SpiritControl.REG.all()) if (h.isOut(p)) out.add(spirit(h));
        return out;
    }

    /** The ids of {@link #owned}, for the server's check of a confirm. */
    public static Set<String> ids(List<View> views) {
        Set<String> s = new java.util.HashSet<>();
        for (View v : views) s.add(v.power().id());
        return s;
    }

    // ------------------------------------------------------------------ one power of each system
    static AttuneRules.Power beast(String willId) {
        BeastWillDef def = BeastWillCatalog.get(willId);
        return new AttuneRules.Power("beast:" + clean(willId), AttuneRules.BEAST, def == null ? 0 : AttuneRules.elementsOf(def.affinity()), def != null && def.legendary() ? 3 : 2);
    }

    private static String label(String willId) {
        BeastWillDef def = BeastWillCatalog.get(willId);
        return def == null ? willId : def.name();
    }

    private static Djinn djinn(ServerPlayer p) { return p.hasData(DjinnAttachments.STATE) ? DjinnState.of(p).active() : null; }

    private static AttuneRules.Power djinnPower(ServerPlayer p, Djinn d) {
        boolean dark = DjinnState.of(p).dark();
        return new AttuneRules.Power("djinn:" + clean(d.id()), AttuneRules.DJINN, AttuneRules.elementsOf(d.affinity().name() + (dark ? " dark" : "")), 3);
    }

    static AttuneRules.Power devil(DevilContract c) {
        String name = c.devil == null || c.devil.isEmpty() ? c.attribute.key() : c.devil;
        return new AttuneRules.Power("devil:" + clean(name), AttuneRules.DEVIL, AttuneRules.elementsOf(c.attribute.name()), Math.max(1, c.tier.ordinal()));
    }

    private static String devilLabel(DevilContract c) {
        WorldDevil w = WorldDevil.byId(c.devil);
        return w != null ? w.name() : "Devil (" + c.attribute.key() + ")";
    }

    static AttuneRules.Power spirit(SpiritHandle h) {
        // shortcut: a spirit of more than two magics (the Spirit Lord: fire, water, wind, earth, anti-magic) counts as neutral; read its bond's element if the owner wants it to clash
        int el = 0;
        if (h.magics().size() <= 2) for (String m : h.magics()) el |= AttuneRules.elementsOf(m);
        return new AttuneRules.Power("spirit:" + clean(h.id()), AttuneRules.SPIRIT, el, "lord".equals(h.id()) ? 3 : 2);
    }

    private static String clean(String s) {
        String c = s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        if (c.isEmpty()) c = "x";
        return c.length() > 56 ? c.substring(0, 56) : c;
    }
}
