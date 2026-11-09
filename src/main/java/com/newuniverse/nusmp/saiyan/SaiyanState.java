package com.newuniverse.nusmp.saiyan;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Why: the per-player progress of the Saiyan system (3.63.0, Stage 1) as plain data with the rules that change it, so Battle Power, the daily budget, the repeat-kill memory and the cooldowns can be
 * tested without a player. Ki is NOT stored here: for a Saiyan Ki is the player's Tensura magicule pool ({@link SaiyanKi}), so Tensura saves and regenerates it. Saved as an attachment
 * ({@link SaiyanStore}, copied on death); times are absolute game ticks. A player who is not a Saiyan has nothing saved.
 */
public final class SaiyanState {
    public static final int MAX_RECENT = 24;
    public static final int MAX_COOLDOWNS = 16;

    public boolean saiyan, awakened;
    /** Stamina; negative = never set (full on the first look). */
    public double stamina = -1.0;
    public double battlePower = SaiyanRules.START_BP;
    /** The game day the counter below belongs to, and the Battle Power gained on it. */
    public long bpDay = -1;
    public double bpToday;
    /** Kills of one creature type in the current window: type -> {count, window start tick}. */
    public final Map<String, long[]> recent = new LinkedHashMap<>();
    /** When each Ki attack is ready again (game tick). */
    public final Map<String, Long> readyAt = new HashMap<>();

    // ------------------------------------------------------------------ Stage 2 and the Goku path (3.65.0), all saved, all bounded
    public static final int MAX_USES = 16, MAX_FORMS = 32, MAX_FLAGS = 16;
    /** Uses of each technique: hits for an attack, seconds for flight and Ki Suppression, scans for the senses (see {@link SaiyanTraining#masteryNeed}). */
    public final Map<String, Integer> uses = new LinkedHashMap<>();
    /** Zenkai received (the emotional breakthroughs the forms ask for), when the last one came, and how many on the current game day. */
    public int zenkai, zenkaiToday;
    public long zenkaiAt = -1, zenkaiDay = -1;
    /** The primary Saiyan Ultimate the player has awakened ("" = none; see {@link SaiyanUltimates}). */
    public String ultimate = "";
    /** Challenging foes beaten with martial arts (the awakening) and the victories of the Trial of the Martial King. */
    public int hardMartialKills, trialKills;
    /** Training points per category (never lost), and what the current game day has given so far. */
    public final Map<String, Double> training = new LinkedHashMap<>();
    public final Map<String, Double> trainToday = new LinkedHashMap<>();
    public long trainDay = -1;
    /** Mastery of each form, 0..1. */
    public final Map<String, Double> formMastery = new LinkedHashMap<>();
    /** The forms this Saiyan has earned (kept even if the server changes a requirement later), the special unlocks ("limit_breaker"), and the form chosen for the toggle key. */
    public final Set<String> unlocked = new LinkedHashSet<>();
    public final Set<String> flags = new LinkedHashSet<>();
    public String selectedForm = "";
    /** Kaio-ken strain 0..100 (it fades while the form is off) and the seconds of form mastery earned on the current game day. */
    public double strain;
    public double formSecondsToday;
    public long formDay = -1;

    public int uses(String tech) { return uses.getOrDefault(tech, 0); }

    /** Adds uses of a technique (bounded). */
    public void use(String tech, int n) {
        if (n <= 0 || tech == null || tech.isEmpty()) return;
        if (!uses.containsKey(tech) && uses.size() >= MAX_USES) return;
        uses.merge(tech, n, (a, b) -> (int) Math.min(1_000_000L, (long) a + b));
    }

    /** How many of the five universal techniques are mastered. */
    public int mastered() {
        int n = 0;
        for (String t : SaiyanTraining.UNIVERSAL) if (SaiyanTraining.mastered(t, uses(t))) n++;
        return n;
    }

    public double trained(SaiyanTraining.Cat c) { return training.getOrDefault(c.id(), 0.0); }

    private void rollTrainDay(long now) {
        long d = SaiyanRules.dayOf(now);
        if (d != trainDay) {
            trainDay = d;
            trainToday.clear();
        }
    }

    /** Adds training points under the day's cap ({@code scale} is the server's multiplier); returns what was really added. */
    public double train(SaiyanTraining.Cat c, double amount, long now, double scale) {
        if (!(amount > 0.0)) return 0.0;
        rollTrainDay(now);
        double give = Math.min(amount, SaiyanTraining.room(c, trainToday.getOrDefault(c.id(), 0.0), scale));
        if (give <= 0.0) return 0.0;
        trainToday.merge(c.id(), give, Double::sum);
        training.merge(c.id(), give, Double::sum);
        return give;
    }

    public double mastery(String form) { return formMastery.getOrDefault(form, 0.0); }

    /** Adds mastery to a form (0..1, bounded map). */
    public void addMastery(String form, double delta) {
        if (!(delta > 0.0) || form == null) return;
        if (!formMastery.containsKey(form) && formMastery.size() >= MAX_FORMS) return;
        formMastery.merge(form, delta, (a, b) -> Math.min(1.0, a + b));
    }

    /** The real fighting seconds that may still count toward form mastery today. */
    public double formRoom(long now, double cap) {
        long d = SaiyanRules.dayOf(now);
        if (d != formDay) {
            formDay = d;
            formSecondsToday = 0.0;
        }
        return Math.max(0.0, cap - formSecondsToday);
    }

    public boolean unlock(String form) { return unlocked.size() < MAX_FORMS && unlocked.add(form); }

    public boolean grantFlag(String flag) { return flags.size() < MAX_FLAGS && flags.add(flag); }

    /** The unlock rules' view of this Saiyan. */
    public SaiyanForms.Progress progress() {
        Map<String, Double> m = new HashMap<>(formMastery);
        return new SaiyanForms.Progress(battlePower, zenkai, SaiyanUltimates.GOKU.equals(ultimate), flags, training, m);
    }

    /** Counts a Zenkai on the game day (rolls the day over first). */
    public int zenkaiDayCount(long now) {
        long d = SaiyanRules.dayOf(now);
        if (d != zenkaiDay) {
            zenkaiDay = d;
            zenkaiToday = 0;
        }
        return zenkaiToday;
    }

    public SaiyanRules.Rank rank() { return SaiyanRules.rankOf(battlePower); }

    /** What is left to gain today (rolls the day over first). */
    public double roomToday(long now, double fraction, double floor) {
        rollDay(now);
        return SaiyanRules.dailyRoom(battlePower, fraction, floor, bpToday);
    }

    private void rollDay(long now) {
        long d = SaiyanRules.dayOf(now);
        if (d != bpDay) {
            bpDay = d;
            bpToday = 0.0;
        }
    }

    /** Adds Battle Power up to the day's room and the hard cap; returns what was really added. */
    public double addBattlePower(double gain, long now, double fraction, double floor) {
        double give = Math.min(Math.max(0.0, gain), roomToday(now, fraction, floor));
        give = Math.min(give, SaiyanRules.BP_HARD_CAP - battlePower);
        if (give <= 0.0) return 0.0;
        battlePower += give;
        bpToday += give;
        return give;
    }

    /** Sets Battle Power directly (a command): clamped, and the day's counter is left alone. */
    public void setBattlePower(double bp) { battlePower = SaiyanRules.clampBp(bp); }

    // ------------------------------------------------------------------ the repeat-kill memory
    /** How many kills of this type fall inside the window that started at the first of them (a stale window counts as none). */
    public int recentKills(String type, long now, long windowTicks) {
        long[] r = recent.get(type);
        return r == null || now - r[1] > windowTicks || now < r[1] ? 0 : (int) r[0];
    }

    /** Remembers one more kill of the type; the memory is bounded and the oldest entry goes first. */
    public void noteKill(String type, long now, long windowTicks) {
        long[] r = recent.get(type);
        if (r == null || now - r[1] > windowTicks || now < r[1]) r = new long[] {0, now};
        r[0]++;
        recent.remove(type);
        recent.put(type, r);
        while (recent.size() > MAX_RECENT) recent.remove(recent.keySet().iterator().next());
    }

    // ------------------------------------------------------------------ cooldowns
    public boolean ready(String kind, long now) {
        Long t = readyAt.get(kind);
        return t == null || now >= t;
    }

    public long left(String kind, long now) {
        Long t = readyAt.get(kind);
        return t == null ? 0 : Math.max(0, t - now);
    }

    public void startCooldown(String kind, long now, int ticks) {
        if (readyAt.size() >= MAX_COOLDOWNS && !readyAt.containsKey(kind)) {
            readyAt.values().removeIf(t -> t <= now);
            while (readyAt.size() >= MAX_COOLDOWNS) {                                  // still full: the one that ends soonest goes
                String soonest = null;
                long least = Long.MAX_VALUE;
                for (Map.Entry<String, Long> e : readyAt.entrySet()) if (e.getValue() < least) { least = e.getValue(); soonest = e.getKey(); }
                readyAt.remove(soonest);
            }
        }
        readyAt.put(kind, now + Math.max(0, ticks));
    }

    /** A game clock that was reset (a restored world) must not leave a cooldown of years. */
    public void clampClock(long now, int maxTicks) { readyAt.replaceAll((k, v) -> Math.min(v, now + maxTicks)); }

    /** True when nothing is worth saving (not a Saiyan and no progress): the attachment is then not written. */
    public boolean isEmpty() {
        return !saiyan && !awakened && battlePower <= SaiyanRules.START_BP + 1.0e-9 && uses.isEmpty() && zenkai == 0 && ultimate.isEmpty() && hardMartialKills == 0 && trialKills == 0
                && training.isEmpty() && formMastery.isEmpty() && unlocked.isEmpty() && flags.isEmpty() && strain <= 0.0;
    }
}
