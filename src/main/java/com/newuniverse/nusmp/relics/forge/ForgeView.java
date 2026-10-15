package com.newuniverse.nusmp.relics.forge;

/**
 * Why: the screen must show tier, sockets, cost and chance, yet it may decide nothing and read no server state. The server menu packs its answers
 * into a handful of ints (synced as container data, so no packet class and no client-side lookup of the relic); this pure class unpacks them and
 * formats them into lang keys and arguments, which the screen only translates. Keeping the formatter free of Minecraft types makes the layout of the
 * synced numbers and every displayed line unit-tested, and keeps client and server from drifting apart on what "slot 7" means.
 */
public final class ForgeView {
    private ForgeView() {}

    /** The synced numbers, by index. {@link #COUNT} of them. */
    public static final int D_TIER = 0, D_MAX = 1, D_USED = 2, D_TOTAL = 3, D_TARGET = 4, D_COST = 5, D_CHANCE = 6, D_MASK = 7, D_MSG = 8,
            D_CYCLE = 9, D_FORGE_ENGRAVINGS = 10, D_COOLDOWN_SECONDS = 11, D_SUBJECT = 12, COUNT = 13;

    /** A lang key and its arguments (the screen translates it). */
    public record Line(String key, Object... args) {}

    /** Everything the screen shows, decoded. */
    public record View(int tier, int maxTier, int used, int total, int target, int costPercent, int chancePercent, int mask, ForgeRules.Msg msg,
                       int cycle, int forgeEngravings, int cooldownSeconds, ForgeRules.Subject subject) {

        public boolean hasSubject() { return subject != ForgeRules.Subject.NONE; }

        public boolean isRelic() { return subject == ForgeRules.Subject.RELIC; }

        /** The fill of the tier bar, 0..1 (0 with no relic). */
        public float tierFraction() { return !isRelic() || maxTier <= 0 ? 0f : Math.max(0f, Math.min(1f, tier / (float) maxTier)); }

        public boolean enabled(ForgeRules.Button b) { return (mask & (1 << b.id())) != 0; }

        /** "Tier: 3 / 6" or the no-relic line. */
        public Line tierLine() { return isRelic() ? new Line("nusmp.forge.tier", tier, maxTier) : new Line("nusmp.forge.tier.none"); }

        /** "Sockets: 1 / 2" or the no-sockets line. */
        public Line socketLine() {
            if (!isRelic()) return new Line("nusmp.forge.sockets.none");
            return total <= 0 ? new Line("nusmp.forge.sockets.zero") : new Line("nusmp.forge.sockets", used, total);
        }

        /** "Draw out socket 2 of 2" (the chosen index, 1-based) or empty when nothing is bound. */
        public Line cycleLine() { return used <= 0 ? new Line("nusmp.forge.cycle.none") : new Line("nusmp.forge.cycle", Math.min(cycle, used - 1) + 1, used); }

        /** The cost of the next jump: a share of the magicule pool, or "nothing to pay" at the top / without a relic. */
        public Line costLine() { return isRelic() && target >= 0 ? new Line("nusmp.forge.cost", costPercent) : new Line("nusmp.forge.cost.none"); }

        public Line chanceLine() { return isRelic() && target >= 0 ? new Line("nusmp.forge.chance", chancePercent) : new Line("nusmp.forge.chance.none"); }

        /** "Engravings added here: 1 / 3". */
        public Line engraveLine() { return new Line("nusmp.forge.engraved", forgeEngravings, ForgeRules.ENGRAVING_CAP); }

        /** The ritual cooldown, shown only while it runs. Null when ready. */
        public Line cooldownLine() { return cooldownSeconds > 0 ? new Line("nusmp.forge.cooldown", cooldownSeconds) : null; }

        /** The Great Sage line for the last action or the current refusal. */
        public Line messageLine() { return new Line(msg.langKey()); }
    }

    /** Unpacks the synced numbers (a short array is read as zeros, so a half-synced first frame does not throw). */
    public static View decode(int[] d) {
        return new View(at(d, D_TIER), at(d, D_MAX), at(d, D_USED), at(d, D_TOTAL), at(d, D_TARGET), at(d, D_COST), at(d, D_CHANCE), at(d, D_MASK),
                ForgeRules.Msg.of(at(d, D_MSG)), at(d, D_CYCLE), at(d, D_FORGE_ENGRAVINGS), at(d, D_COOLDOWN_SECONDS), subjectOf(at(d, D_SUBJECT)));
    }

    public static int subjectCode(ForgeRules.Subject s) { return s.ordinal(); }

    static ForgeRules.Subject subjectOf(int code) { return code < 0 || code >= ForgeRules.Subject.values().length ? ForgeRules.Subject.NONE : ForgeRules.Subject.values()[code]; }

    private static int at(int[] d, int i) { return d == null || i >= d.length ? 0 : d[i]; }
}
