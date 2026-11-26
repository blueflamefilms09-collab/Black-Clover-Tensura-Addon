package com.newuniverse.nusmp.labyrinth.mega.party;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LaneReclaim {
    private LaneReclaim() {}

    public enum Mode {
        OFF, ARCHIVE, DELETE;

        public static Mode parse(String text) {
            if (text == null) return OFF;
            return switch (text.trim().toLowerCase(Locale.ROOT)) {
                case "archive" -> ARCHIVE;
                case "delete" -> DELETE;
                default -> OFF;
            };
        }
    }

    private static final Pattern REGION_FILE = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");

    public static long idleTicks(int hours) { return Math.max(1, hours) * 72_000L; }

    public static List<Integer> idle(Map<Integer, Long> used, Collection<Integer> held, long now, long idleTicks) {
        List<Integer> out = new ArrayList<>();
        for (int index : held) {
            if (index <= 0 || index >= MegaGrid.MAX_PARTIES) continue;
            Long at = used.get(index);
            if (at != null && now - at >= idleTicks) out.add(index);
        }
        out.sort(Integer::compare);
        return out;
    }

    public static List<Integer> unstamped(Map<Integer, Long> used, Collection<Integer> held) {
        List<Integer> out = new ArrayList<>();
        for (int index : held) if (index > 0 && index < MegaGrid.MAX_PARTIES && !used.containsKey(index)) out.add(index);
        return out;
    }

    public static int partyOfRegionZ(int regionZ) { return PartyLanes.partyAt(regionZ * 512 + 256, MegaGrid.MAX_PARTIES); }

    public static int regionZ(String fileName) {
        Matcher m = REGION_FILE.matcher(fileName);
        if (!m.matches()) return Integer.MIN_VALUE;
        try {
            return Integer.parseInt(m.group(2));
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }

    public static int partyOfFile(String fileName) {
        int z = regionZ(fileName);
        return z == Integer.MIN_VALUE ? -1 : partyOfRegionZ(z);
    }
}
