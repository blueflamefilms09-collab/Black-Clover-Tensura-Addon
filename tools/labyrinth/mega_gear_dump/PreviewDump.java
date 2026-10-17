package com.newuniverse.nusmp.labyrinth.mega.gear;

import com.newuniverse.nusmp.dungeon.sentient.Dice;

import java.util.List;

/**
 * Why: {@code tools/labyrinth/mega_gear_preview.py} is a second, independent implementation of the gear roller, and an independent implementation is only worth
 * something if it is compared with the real one. This is the real one's side of that comparison: it prints, as JSON, the chances the Java {@link GearRoller}
 * gives at eleven floors (with and without Looting III), the chances {@link ChestGear} gives for standard and treasure chests at the same floors (with and
 * without Luck III), and 162 pieces made with fixed seeds (every grade, every slot, six seeds), so
 * {@code mega_gear_preview.py --verify dump.json} can replay the same seeds and report any difference. It is a developer tool, not part of the mod: it lives
 * outside {@code src}, uses the pure classes of the gear package and the test helpers {@code GearFx} / {@code MiniJson} (so it reads the shipped
 * {@code tables.json}), and needs no game.
 *
 * <pre>
 * javac -d /tmp/gd -sourcepath src/main/java:src/test/java:tools/labyrinth/mega_gear_dump tools/labyrinth/mega_gear_dump/PreviewDump.java
 * java -cp /tmp/gd com.newuniverse.nusmp.labyrinth.mega.gear.PreviewDump &gt; dump.json
 * python3 tools/labyrinth/mega_gear_preview.py --verify dump.json
 * </pre>
 */
public final class PreviewDump {
    private PreviewDump() {}

    public static void main(String[] args) {
        GearTables t = GearFx.tables();
        GearRates r = GearRates.DEFAULT;
        StringBuilder sb = new StringBuilder("{\n\"chances\": [");
        int[] floors = {1, 2, 5, 10, 11, 20, 26, 30, 40, 51, 60};
        for (int i = 0; i < floors.length; i++) {
            double[] c0 = GearRoller.chances(r, t, floors[i], 0), c3 = GearRoller.chances(r, t, floors[i], 3);
            sb.append(i == 0 ? "\n" : ",\n").append(String.format(java.util.Locale.ROOT,
                    "{\"floor\":%d,\"x\":%.4f,\"n\":%.6f,\"s\":%.6f,\"r\":%.6f,\"n3\":%.6f,\"s3\":%.6f,\"r3\":%.6f}",
                    floors[i], GearRoller.progress(r, floors[i]), c0[0], c0[1], c0[2], c3[0], c3[1], c3[2]));
        }
        sb.append("\n],\n\"chests\": [");
        for (int i = 0; i < floors.length; i++) {
            sb.append(i == 0 ? "\n" : ",\n").append("{\"floor\":").append(floors[i]);
            for (ChestRules.Kind k : new ChestRules.Kind[] {ChestRules.Kind.STANDARD, ChestRules.Kind.TREASURE}) {
                double[] c0 = ChestGear.chances(k, GearRates.CHEST_DEFAULT, GearRates.DEFAULT_TREASURE_FACTOR, t, floors[i], 0);
                double[] c3 = ChestGear.chances(k, GearRates.CHEST_DEFAULT, GearRates.DEFAULT_TREASURE_FACTOR, t, floors[i], 3);
                sb.append(String.format(java.util.Locale.ROOT, ",\"%s\":{\"n\":%.6f,\"s\":%.6f,\"r\":%.6f,\"n3\":%.6f,\"s3\":%.6f,\"r3\":%.6f}",
                        k.name().toLowerCase(java.util.Locale.ROOT), c0[0], c0[1], c0[2], c3[0], c3[1], c3[2]));
            }
            sb.append("}");
        }
        sb.append("\n],\n\"examples\": [");
        boolean first = true;
        for (GearGrade g : GearGrade.values()) {
            for (int seed = 1; seed <= 6; seed++) {
                for (GearSlot s : GearSlot.values()) {
                    Dice d = GearFx.dice(1000L * g.ordinal() + 37L * seed + s.ordinal());
                    GearSpec sp = GearRoller.make(d, g, s, t).orElseThrow();
                    sb.append(first ? "\n" : ",\n");
                    first = false;
                    sb.append("{\"grade\":\"").append(g.id()).append("\",\"slot\":\"").append(s.id()).append("\",\"seed\":").append(seed)
                      .append(",\"item\":\"").append(sp.item()).append("\",\"power\":").append(sp.powerPercent()).append(",\"dur\":").append(sp.durabilityPercent())
                      .append(",\"resist\":").append(sp.resistPercent()).append(",\"ench\":").append(list(sp.enchants())).append(",\"eng\":").append(list(sp.engravings())).append("}");
                }
            }
        }
        sb.append("\n]\n}\n");
        System.out.print(sb);
    }

    private static String list(List<GearSpec.Ench> l) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < l.size(); i++) b.append(i == 0 ? "" : ",").append("[\"").append(l.get(i).id()).append("\",").append(l.get(i).level()).append("]");
        return b.append("]").toString();
    }
}
