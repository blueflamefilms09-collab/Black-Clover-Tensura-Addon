package com.newuniverse.nusmp.client.ui.codex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Why: the grouping and sorting of the learned-spells tab with no Minecraft types, so it is tested. The game side ({@link SpellSource}) only
 * turns grimoire pages into {@link Spell}s; what the tab shows (spells grouped by magic, magics alphabetical, spells in the book's own page
 * order, one entry per page even if two sources list it, a count of pages still sealed, and the one-line description fallback) is decided here.
 */
public final class SpellModel {
    private SpellModel() {}

    /**
     * One learned page. {@code descKey} is the lang key of its written description (translated by the caller; may be absent from the lang file);
     * {@code masteryPct} is the mastery at which the page opens (0 for the starter page, -1 unknown).
     */
    public record Spell(String magicId, String magicName, String pageId, String name, int mode, int costPct, int cooldownSec, int masteryPct, String descKey) {}

    /** The spells of one magic and how many of its pages are still sealed (shown as "???", never named). */
    public record Group(String magicId, String magicName, List<Spell> spells, int sealed) {}

    public static List<Group> group(List<Spell> spells, Map<String, Integer> sealedByMagic) {
        Map<String, List<Spell>> byMagic = new TreeMap<>();
        Map<String, String> names = new TreeMap<>();
        Set<String> seen = new HashSet<>();
        for (Spell s : spells) {
            if (s == null || s.magicId() == null || s.pageId() == null) continue;
            if (!seen.add(s.magicId() + "\u0000" + s.pageId())) continue;
            byMagic.computeIfAbsent(s.magicId(), k -> new ArrayList<>()).add(s);
            names.putIfAbsent(s.magicId(), s.magicName() == null || s.magicName().isBlank() ? s.magicId() : s.magicName());
        }
        if (sealedByMagic != null) for (String id : sealedByMagic.keySet()) if (sealedByMagic.get(id) > 0 && !byMagic.containsKey(id)) { byMagic.put(id, new ArrayList<>()); names.putIfAbsent(id, id); }
        List<Group> out = new ArrayList<>();
        for (Map.Entry<String, List<Spell>> e : byMagic.entrySet()) {
            List<Spell> l = e.getValue();
            l.sort(Comparator.comparingInt(Spell::mode).thenComparing(Spell::name));
            int sealed = sealedByMagic == null ? 0 : Math.max(0, sealedByMagic.getOrDefault(e.getKey(), 0));
            out.add(new Group(e.getKey(), names.get(e.getKey()), List.copyOf(l), sealed));
        }
        out.sort(Comparator.comparing((Group g) -> g.magicName().toLowerCase(Locale.ROOT)).thenComparing(Group::magicId));
        return out;
    }

    /** The fallback line when a page has no written description: what it costs and how long it rests. */
    public static String generatedLine(Spell s) {
        String name = s.name() == null || s.name().isBlank() ? "This page" : s.name();
        if (s.costPct() <= 0 && s.cooldownSec() <= 0) return name + " is free to use and needs no rest.";
        StringBuilder b = new StringBuilder(name).append(" costs ");
        b.append(s.costPct() <= 0 ? "no magicule" : s.costPct() + "% of your magicule");
        b.append(s.cooldownSec() <= 0 ? " and has no cooldown." : " and rests for " + s.cooldownSec() + " s.");
        return b.toString();
    }

    /** "Cost 12%  Cooldown 8 s  Opens at 40% mastery": the numbers line under a spell, parts left out when unknown or zero. */
    public static String statsLine(Spell s) {
        List<String> parts = new ArrayList<>();
        if (s.costPct() > 0) parts.add("Cost " + s.costPct() + "%");
        if (s.cooldownSec() > 0) parts.add("Cooldown " + s.cooldownSec() + " s");
        if (s.masteryPct() > 0) parts.add("Opens at " + s.masteryPct() + "% mastery");
        return String.join("  ", parts);
    }
}
