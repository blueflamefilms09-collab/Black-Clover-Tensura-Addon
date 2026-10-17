package com.newuniverse.nusmp.avatar.menu;

import com.newuniverse.nusmp.avatar.AvatarPersonality;
import com.newuniverse.nusmp.avatar.aptitude.Aptitude;
import com.newuniverse.nusmp.avatar.aptitude.ForgeChoices;
import com.newuniverse.nusmp.avatar.aptitude.ForgeContext;
import com.newuniverse.nusmp.avatar.aptitude.ForgeCosts;
import com.newuniverse.nusmp.avatar.aptitude.ForgeRules;
import com.newuniverse.nusmp.avatar.aptitude.ForgeSkill;
import com.newuniverse.nusmp.avatar.aptitude.ForgeValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Why: the Soul Forge screen has five pages whose choices depend on each other (a skill costs tax, two aptitude slots are shared between hand picks and the
 * free roll, the roll cannot repeat a hand pick, Finalize needs a rolled set of stats, a race and a personality). Which button is lit and what the cost panel
 * says is rule logic, so it lives here, pure and unit tested, away from the widgets, exactly as {@link BuilderState} does for the classic screen. It holds the
 * creator's CHOICES only; it never decides a price: {@link #ledger} and {@link #problems} call the very same {@link ForgeCosts} and {@link ForgeValidator} the
 * server uses, fed with the rules and reserves the server sent. Whatever this says, the server re-checks everything on Finalize and its answer is the one that
 * counts; a problem that depends on a number which can change while the screen is open (the magicule one holds) is therefore only a warning, not a lock.
 */
public final class ForgeState {
    public static final int PAGE_IDENTITY = 0, PAGE_APPEARANCE = 1, PAGE_TRAITS = 2, PAGE_SKILLS = 3, PAGE_APTITUDES = 4, PAGES = 5;

    /** What toggling an aptitude on the grid did. */
    public enum Pick { ADDED, REMOVED, FULL, ROLLED }

    /** Something between the creator and the Finalize button. {@code page} is where it can be fixed (-1 = nowhere); a {@code soft} one is a warning only. */
    public record Problem(int page, String code, String text, boolean soft) {}

    /** The bill and the creator's means, in the numbers the cost panel prints. */
    public record Ledger(ForgeCosts.Quote quote, double raceMagicule, double magiculeNeeded, double magiculeHeld, double existingTax, double totalTax, double taxCap,
                         int coresNeeded, int coresHeld, double awakeningChance, boolean creative) {
        public boolean magiculeShort() { return !creative && magiculeHeld + 1.0e-6 < magiculeNeeded; }

        public boolean taxOver() { return quote.taxed() && totalTax > taxCap + 1.0e-9; }

        public boolean coresShort() { return !creative && coresHeld < coresNeeded; }
    }

    private int page = PAGE_IDENTITY;
    private boolean rolled, busy, reelBusy, rollPending;
    private String name = "";
    private String race = "";
    private boolean raceAvailable;
    private AvatarPersonality primary, secondary;
    private String unique = "", ultimate = "";
    private final List<String> picks = new ArrayList<>();
    private String rolledId = "";
    private long rolledToken;
    private boolean useRoll, paidAwakening;

    // ---- plain getters
    public int page() { return page; }
    public boolean rolled() { return rolled; }
    public boolean busy() { return busy; }
    public String name() { return name; }
    public String race() { return race; }
    public boolean raceAvailable() { return raceAvailable; }
    public AvatarPersonality primary() { return primary; }
    public AvatarPersonality secondary() { return secondary; }
    public String unique() { return unique; }
    public String ultimate() { return ultimate; }
    public List<String> picks() { return List.copyOf(picks); }
    public String rolledId() { return rolledId; }
    public long rolledToken() { return rolledToken; }
    public boolean useRoll() { return useRoll; }
    public boolean paidAwakening() { return paidAwakening; }
    public boolean rollPending() { return rollPending; }

    // ---- pages: the five round buttons may be used in any order (the problems list says what is still missing)
    public boolean goTo(int target) {
        if (busy || target < 0 || target >= PAGES) return false;
        page = target;
        return true;
    }

    public boolean next() { return page < PAGES - 1 && goTo(page + 1); }

    public boolean back() { return page > 0 && goTo(page - 1); }

    // ---- identity
    public void onRolled() { rolled = true; }
    public void setBusy(boolean b) { busy = b; }
    public void setName(String n) { name = n == null ? "" : n; }

    // ---- race and personality
    public void chooseRace(String id, boolean available) { race = id == null ? "" : id; raceAvailable = available && !race.isEmpty(); }

    public void choosePrimary(AvatarPersonality p) {
        primary = p;
        if (secondary == p) secondary = null;                  // the same trope twice is not a hybrid
    }

    public void chooseSecondary(AvatarPersonality p) { secondary = p == primary ? null : p; }

    // ---- skills
    /** Puts the skill in its slot, or empties the slot if it already holds this skill. True when the skill is chosen afterwards. */
    public boolean toggleSkill(ForgeSkill s) {
        if (s == null) return false;
        if (s.kind() == ForgeSkill.Kind.UNIQUE) {
            boolean same = unique.equals(s.id());
            unique = same ? "" : s.id();
            return !same;
        }
        boolean same = ultimate.equals(s.id());
        ultimate = same ? "" : s.id();
        return !same;
    }

    public void clearSkill(ForgeSkill.Kind kind) {
        if (kind == ForgeSkill.Kind.UNIQUE) unique = ""; else ultimate = "";
    }

    public void setPaidAwakening(boolean on) { paidAwakening = on; }

    // ---- aptitudes
    /** Aptitudes the Avatar would carry: hand picks plus the free roll when it is kept. */
    public int aptitudeCount() { return picks.size() + (useRoll ? 1 : 0); }

    public Pick togglePick(String id, ForgeRules rules) {
        if (id == null || id.isEmpty()) return Pick.FULL;
        if (picks.remove(id)) return Pick.REMOVED;
        if (useRoll && rolledId.equals(id)) return Pick.ROLLED;
        if (aptitudeCount() >= rules.maxAptitudes()) return Pick.FULL;
        picks.add(id);
        return Pick.ADDED;
    }

    /** The "Randomize Aptitude" button is lit: there is a slot a hand pick does not fill, a free roll left, no reel running and no answer awaited. */
    public boolean canRoll(ForgeRules rules, int rollsLeft) {
        return rules.maxAptitudes() > 0 && picks.size() < rules.maxAptitudes() && rollsLeft > 0 && !rollPending && !reelBusy && !busy;
    }

    public void beginRoll() { rollPending = true; }

    /** The roll could not be made (the server refused or the packet never came back). */
    public void rollFailed() { rollPending = false; }

    /** The server answered the roll: the screen is no longer waiting, and the reel animation (which holds Finalize back) takes over. */
    public void rollAnswered() { rollPending = false; }

    /** The server's answer to a roll: the aptitude and its token. A hand pick of the same aptitude (made while the answer was on its way) gives way to it. */
    public void onRolledAptitude(String id, long token) {
        rollPending = false;
        if (id == null || id.isEmpty() || token == 0L) return;
        rolledId = id;
        rolledToken = token;
        useRoll = true;
        picks.remove(id);
    }

    /** Lets the rolled aptitude go (its slot is free again; the roll count the server keeps is not given back). */
    public void discardRoll() {
        useRoll = false;
        rolledId = "";
        rolledToken = 0L;
    }

    /** True while the reel animation is still spinning (the result is known but not yet shown, so Finalize waits). */
    public void setReelBusy(boolean b) { reelBusy = b; }

    public boolean reelBusy() { return reelBusy; }

    /** A fresh server state replaced the roll record (the screen opened or the server changed it): follow it. */
    public void syncRoll(String serverId, long serverToken) {
        if (serverId == null || serverId.isEmpty() || serverToken == 0L) {
            if (useRoll && rolledToken != serverToken) discardRoll();
            return;
        }
        if (!useRoll || rolledToken != serverToken) onRolledAptitude(serverId, serverToken);
    }

    // ---- what Finalize would send
    public ForgeChoices choices() {
        return new ForgeChoices(picks, useRoll ? rolledToken : 0L, unique, ultimate, paidAwakening);
    }

    // ---- the bill
    private static ForgeSkill skillOf(String id, Function<String, ForgeSkill> skills) { return id == null || id.isEmpty() ? null : skills.apply(id); }

    public Ledger ledger(ForgeRules rules, ForgeContext ctx, double raceMagicule, Function<String, Aptitude> aptitudes, Function<String, ForgeSkill> skills) {
        List<Aptitude> hand = new ArrayList<>();
        for (String id : picks) { Aptitude a = aptitudes.apply(id); if (a != null) hand.add(a); }
        ForgeCosts.Quote q = ForgeCosts.quote(hand, skillOf(unique, skills), skillOf(ultimate, skills), paidAwakening, rules, ctx.maxMagicule());
        double chance = paidAwakening ? 1.0 : ForgeCosts.awakeningChance(rules, ctx.luck());
        return new Ledger(q, Math.max(0.0, raceMagicule), Math.max(0.0, raceMagicule) + q.upfrontMagicule(), ctx.magicule(), ctx.existingTax(),
                ctx.existingTax() + q.permanentTax(), rules.maxTotalTax(), q.coresNeeded(), ctx.cores(), chance, ctx.creative());
    }

    /** Everything still between the creator and the Finalize button, in page order. */
    public List<Problem> problems(ForgeRules rules, ForgeContext ctx, double raceMagicule, Function<String, Aptitude> aptitudes, Function<String, ForgeSkill> skills) {
        List<Problem> out = new ArrayList<>();
        if (!rolled) out.add(new Problem(PAGE_IDENTITY, "stats", "Roll your stats first.", false));
        if (!BuilderState.nameOk(name)) out.add(new Problem(PAGE_IDENTITY, "name", "That name is not allowed (2 to 24 letters, digits, spaces, ' or -).", false));
        if (race.isEmpty() || !raceAvailable) out.add(new Problem(PAGE_APPEARANCE, "race", "Choose a race you can use.", false));
        if (primary == null) out.add(new Problem(PAGE_TRAITS, "personality", "Choose a primary personality.", false));
        if (reelBusy || rollPending) out.add(new Problem(PAGE_APTITUDES, "reel", "The aptitude roll is still spinning.", false));
        ForgeContext withRace = new ForgeContext(ctx.luck(), ctx.magicule(), ctx.maxMagicule(), ctx.maxEp(), ctx.existingTax(), ctx.cores(), ctx.creative(),
                useRoll ? rolledId : "", useRoll ? rolledToken : 0L, Math.max(0.0, raceMagicule));
        ForgeValidator.Verdict v = ForgeValidator.validate(choices(), withRace, rules, aptitudes, skills);
        if (!v.ok()) {
            ForgeValidator.Refusal r = v.refusal();
            out.add(new Problem(pageFor(r.code()), r.code().name().toLowerCase(java.util.Locale.ROOT), r.text(), r.code() == ForgeValidator.Code.NOT_ENOUGH_MAGICULE));
        }
        return out;
    }

    /** The page on which a validator refusal can be fixed. */
    public static int pageFor(ForgeValidator.Code c) {
        return switch (c) {
            case TOO_MANY_APTITUDES, UNKNOWN_APTITUDE, DUPLICATE_APTITUDE, ROLL_MISMATCH -> PAGE_APTITUDES;
            case UNKNOWN_SKILL, WRONG_SKILL_KIND, NOT_ENOUGH_EP, NEED_CORES -> PAGE_SKILLS;
            case TAX_CAP, NOT_ENOUGH_MAGICULE -> -1;                     // the tax is the sum of several pages; the cost panel shows it
        };
    }

    /** True when no hard problem is left and the screen is not waiting for the server. */
    public boolean canFinalize(List<Problem> problems) {
        if (busy) return false;
        for (Problem p : problems) if (!p.soft()) return false;
        return true;
    }

    /** Does this page still need something from the creator? (the red mark on its round button) */
    public static boolean pageNeedsAttention(int page, List<Problem> problems) {
        for (Problem p : problems) if (p.page() == page && !p.soft()) return true;
        return false;
    }

    /** Has the creator made a choice on this page that counts as done? (the green mark) */
    public boolean pageDone(int page) {
        return switch (page) {
            case PAGE_IDENTITY -> rolled && BuilderState.nameOk(name);
            case PAGE_APPEARANCE -> raceAvailable && !race.isEmpty();
            case PAGE_TRAITS -> primary != null;
            case PAGE_SKILLS -> !unique.isEmpty() || !ultimate.isEmpty() || paidAwakening;
            case PAGE_APTITUDES -> aptitudeCount() > 0;
            default -> false;
        };
    }
}
