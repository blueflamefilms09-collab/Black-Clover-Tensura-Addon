package com.newuniverse.nusmp.labyrinth.mega.gear;

import com.newuniverse.nusmp.dungeon.sentient.Dice;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Why: the gear tests all need the same three things (the REAL shipped tables, a seeded random, and the id lists copied from the dumps), and writing them
 * once keeps every test reading the same facts: if the shipped file is changed, every test sees the change. Working directory = project root (Gradle's
 * default for tests), the same assumption as the faction data test.
 */
final class GearFx {
    private GearFx() {}

    static final Path TABLES = Path.of("src/main/resources/data/nusmp/mega/gear/tables.json");
    private static GearTablesParser.Result shipped;

    /** The shipped file, parsed once with the real parser. */
    static synchronized GearTablesParser.Result shipped() {
        if (shipped == null) {
            try {
                shipped = GearTablesParser.parse(MiniJson.parse(Files.readString(TABLES, StandardCharsets.UTF_8)));
            } catch (IOException e) {
                throw new AssertionError("cannot read " + TABLES + ": " + e);
            }
        }
        return shipped;
    }

    static GearTables tables() { return shipped().tables(); }

    /** A seeded uniform source in [0, 1): the JDK generator's sequence for a seed never changes, so a test that passes once passes always. */
    static Dice dice(long seed) {
        Random r = new Random(seed);
        return r::nextDouble;
    }

    /** The lines of a test resource of this package ({@code /mega/gear/<name>}), trimmed, blanks dropped. */
    static List<String> lines(String name) {
        try (InputStream in = GearFx.class.getResourceAsStream("/mega/gear/" + name)) {
            if (in == null) throw new AssertionError("test resource /mega/gear/" + name + " is missing");
            List<String> out = new ArrayList<>();
            for (String l : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) if (!l.isBlank()) out.add(l.trim());
            return out;
        } catch (IOException e) {
            throw new AssertionError("cannot read /mega/gear/" + name + ": " + e);
        }
    }

    static Set<String> set(String name) { return new HashSet<>(lines(name)); }
}
