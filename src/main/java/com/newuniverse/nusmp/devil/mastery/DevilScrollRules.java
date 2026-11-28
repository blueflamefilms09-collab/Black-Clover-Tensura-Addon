package com.newuniverse.nusmp.devil.mastery;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why (5.7.0, owner request): a player who gets a devil must be told how that devil works, and what each step of its mastery needs. This pure class (no Minecraft type, unit tested) writes the text of the
 * "Scroll of the Devil" book and the {@code /devil requirements} list from the devil's kit ({@link DevilKit}) and the server's mastery numbers. It only builds strings: nothing here reads the game, so the
 * cost is a few hundred characters once, when a contract is signed or a player asks.
 */
public final class DevilScrollRules {
    private DevilScrollRules() {}

    /** A book page holds about this many characters before the text overflows (the pages are cut at a line, never in the middle of one). */
    public static final int PAGE_CHARS = 230;
    /** The most pages a scroll can have (a hand-edited kit can not make a huge book). */
    public static final int MAX_PAGES = 12;
    /** The book title limit of the game. */
    public static final int TITLE_MAX = 32;

    /** What the scroll says about one devil. {@code kit} is null for the old pact devils (they have no arts). */
    public record Info(String name, String title, String world, String tier, String attribute, String blurb, String price, DevilKit.Kit kit,
                       float start, double rate, int dailyMinutes) {}

    public static String titleFor(String devilName) {
        String t = "Scroll of " + (devilName == null || devilName.isBlank() ? "the Devil" : devilName.trim());
        return t.length() <= TITLE_MAX ? t : t.substring(0, TITLE_MAX);
    }

    // ---------------------------------------------------------------- the requirements list
    private static String pct(float v) { return Math.round(v) + "%"; }

    private static void add(List<String> out, float at, String what) { out.add("Mastery " + pct(at) + ": " + what); }

    /** One line per step of mastery: what opens at 10, 20, 25, 50, 75 and 100 percent (the numbers are {@link MasteryRules#TIER_AT} plus the horns at 20). */
    public static List<String> requirementLines(DevilKit.Kit kit, float start) {
        List<String> out = new ArrayList<>(8);
        out.add("Start: " + pct(start) + " mastery. It only grows by use.");
        String[] stages = kit == null || kit.stages().size() < 3 ? new String[] {"Horns", "Wings and flight", "Devil Union"} : kit.stages().toArray(new String[0]);
        for (int t = 0; t < MasteryRules.TIERS; t++) {
            float at = MasteryRules.TIER_AT[t];
            if (t == 1) add(out, 20f, stages[0] + " (assimilation level 20)");
            StringBuilder sb = new StringBuilder();
            if (kit != null) {
                for (DevilKit.Art a : kit.arts()) if (a.tier() == t) sb.append("art ").append(a.name()).append(", ");
                for (DevilKit.Passive p : kit.passives()) if (p.tier() == t) sb.append("passive ").append(p.name()).append(", ");
            }
            if (t == 0) sb.append("the first art and passive, level up to the mastery, ");
            if (t == 2) sb.append(stages[1]).append(" (level 50), ");
            if (t == 4) sb.append(kit != null && !kit.unionName().isEmpty() ? kit.unionName() : stages[2]).append(" (level 100, 5 minutes), ");
            if (sb.length() == 0) sb.append("more power");
            else sb.setLength(sb.length() - 2);
            add(out, at, sb.toString());
        }
        return out;
    }

    // ---------------------------------------------------------------- how mastery grows
    /** About how many hours of active play (4 arts, 3 kills and 20 blows a minute while awakened) the climb to 100 percent takes at this rate. */
    public static double activeHours(double rate) { return MasteryRules.hoursToMax(Math.max(0.1, Math.min(10.0, rate)), 4, 3, 20, 1.0); }

    public static List<String> growthLines(double rate, int dailyMinutes) {
        List<String> out = new ArrayList<>(8);
        out.add("How it grows (effective seconds of use):");
        out.add("- each second awakened while you fight: 1");
        out.add("- an art that hits: " + Math.round(MasteryRules.CAST_HIT) + ", that misses: " + Math.round(MasteryRules.CAST_MISS));
        out.add("- a kill (victim with 8+ health): " + Math.round(MasteryRules.KILL));
        out.add("- a blow dealt: " + MasteryRules.HIT);
        out.add("Standing still counts for nothing. At most " + dailyMinutes + " minutes of use count a day (UTC).");
        out.add(String.format(Locale.ROOT, "This server: growth x%.1f, about %.0f hours of active play to 100%%.", rate, activeHours(rate)));
        return out;
    }

    // ---------------------------------------------------------------- the book
    /** The pages of the scroll (never more than {@link #MAX_PAGES}, each about {@link #PAGE_CHARS} long). */
    public static List<String> pages(Info i) {
        List<String> lines = new ArrayList<>(40);
        lines.add(i.name() + (i.title() == null || i.title().isEmpty() ? "" : ", " + i.title()));
        lines.add("World: " + i.world() + ". A " + i.tier() + " devil of " + i.attribute() + ".");
        if (i.blurb() != null && !i.blurb().isEmpty()) lines.add(i.blurb());
        if (i.price() != null && !i.price().isEmpty()) lines.add("Price: " + i.price());
        lines.add("");
        lines.add("HOW IT WORKS");
        lines.add("The devil lives in the extra skill Devil Awakening (press the Devil key, N). Modes: Awaken, Devil Art I to IV, Read.");
        lines.add("Awaken lets the devil in for a while, then it rests. Your assimilation level can not pass your mastery. Dying or leaving ends it.");
        if (i.kit() == null) lines.add("This old pact devil has no arts: only the assimilation level.");
        lines.add("");
        lines.add("REQUIREMENTS");
        lines.addAll(requirementLines(i.kit(), i.start()));
        lines.add("");
        lines.addAll(growthLines(i.rate(), i.dailyMinutes()));
        if (i.kit() != null && !i.kit().arts().isEmpty()) {
            lines.add("");
            lines.add("ARTS");
            for (DevilKit.Art a : i.kit().arts()) lines.add(a.name() + ": " + a.canon());
        }
        return paginate(lines, PAGE_CHARS, MAX_PAGES);
    }

    /** Packs the lines into pages of at most {@code chars} characters, filling each page (a line is cut at a space where the page ends). Stops at {@code maxPages}. */
    public static List<String> paginate(List<String> lines, int chars, int maxPages) {
        List<String> pages = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String raw : lines) {
            String line = raw == null ? "" : raw;
            while (true) {
                int room = chars - cur.length();
                if (line.length() < room) { cur.append(line).append('\n'); break; }
                int cut = room > 20 ? line.lastIndexOf(' ', room - 1) : -1;
                if (cut > 0) {
                    cur.append(line, 0, cut);
                    line = line.substring(cut).stripLeading();
                } else if (cur.length() == 0) {                                                  // one word longer than a page
                    cur.append(line, 0, chars - 1);
                    line = line.substring(chars - 1);
                }
                pages.add(cur.toString().stripTrailing());
                cur.setLength(0);
                if (line.isEmpty()) break;
            }
        }
        if (cur.length() > 0) pages.add(cur.toString().stripTrailing());
        if (pages.size() > maxPages) return new ArrayList<>(pages.subList(0, Math.max(1, maxPages)));
        return pages;
    }
}
