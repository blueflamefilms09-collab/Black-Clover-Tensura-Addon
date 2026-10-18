package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Why: every pure test of the mechanics library needs the shipped data (ops.json and the eight category files) parsed with the production parser; this loads them once from the
 * source tree (the way the other data tests do) so a test never re-implements the reading and a problem in the parser or the data shows up in all of them.
 */
final class TestLibrary {
    private TestLibrary() {}

    static final Path DIR = Path.of("src/main/resources/data/nusmp/mega/mechanics");
    private static OpCatalog catalog;
    private static List<MechanicDef> defs;
    private static List<String> problems;

    static synchronized OpCatalog catalog() {
        load();
        return catalog;
    }

    static synchronized List<MechanicDef> defs() {
        load();
        return defs;
    }

    /** Every problem the production parser reported while reading the shipped files. */
    static synchronized List<String> problems() {
        load();
        return problems;
    }

    private static void load() {
        if (catalog != null) return;
        try {
            catalog = OpCatalog.parse(Files.readString(DIR.resolve("ops.json"), StandardCharsets.UTF_8));
            defs = new ArrayList<>();
            problems = new ArrayList<>();
            for (MechCategory c : MechCategory.values()) {
                Path f = DIR.resolve(c.id() + ".json");
                if (!Files.exists(f)) continue;
                MechanicParser.Parsed p = MechanicParser.parseFile(f.getFileName().toString(), Files.readString(f, StandardCharsets.UTF_8), catalog);
                defs.addAll(p.defs());
                problems.addAll(p.problems());
            }
            defs.sort((a, b) -> a.id().compareTo(b.id()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
