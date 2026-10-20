package com.newuniverse.nusmp.relics.vu;

import com.newuniverse.nusmp.vfx.VfxShape;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: every act of an upgraded relic has a signature effect, named in data/nusmp/visual_upgrade/act_vfx.txt. A typo in a shape or an act key would be an effect that silently never plays,
 * and a Time Magic shape or a zone shape that needs another protocol would be wrong. The table is parsed with the real parser, every shape is a real {@link VfxShape}, none is a Time
 * Magic one, and every act key the table names is an act some relic of the waves really has.
 */
class VuActTableTest {
    static final Path TABLE = Path.of("src/main/resources/data/nusmp/visual_upgrade/act_vfx.txt");
    static final Path WAVES = Path.of("src/main/java/com/newuniverse/nusmp/relics/wave");

    @Test
    void parsesTheStepGrammarAndFallsBackToTheWildcard() {
        VuActTable t = VuActTable.parse(List.of("# c", "act r a LIGHT_FLARE:FFFFFF:1.5:20:self SPACE_PORTAL:3A8BFF:2:40:front", "act r * MAGIC_CIRCLE:FFD27A:3:40:ground", "act s x NOPE:FFFFFF:1:20:self", "act s y LIGHT_FLARE:FFFFFF:1:20:somewhere"));
        assertEquals(2, t.steps("r", "a").size());
        assertEquals("SPACE_PORTAL", t.steps("r", "a").get(1).shape());
        assertEquals(0x3A8BFF, t.steps("r", "a").get(1).rgb());
        assertEquals(1, t.steps("r", "other").size(), "the wildcard line answers for an act with no line of its own");
        assertTrue(t.steps("nobody", "a").isEmpty());
        assertEquals(2, t.problems().size(), "an unknown shape and an unknown place are reported");
        assertTrue(t.has("r"));
        assertFalse(t.has("s"));
    }

    @Test
    void clampsPowerAndDuration() {
        VuActTable t = VuActTable.parse(List.of("act r a LIGHT_FLARE:FFFFFF:99:9999:self"));
        assertEquals(8f, t.steps("r", "a").get(0).power(), 0f);
        assertEquals(160, t.steps("r", "a").get(0).duration());
    }

    @Test
    void theRealTableParsesAndNamesOnlyRealNonTimeShapes() throws IOException {
        VuActTable t = VuActTable.parse(Files.readAllLines(TABLE));
        assertTrue(t.problems().isEmpty(), t.problems().toString());
        assertTrue(t.lineCount() >= 50, "act lines: " + t.lineCount());
        for (String shape : VuActTable.SHAPES) {
            assertNotNull(VfxShape.valueOf(shape), shape);
            assertFalse(shape.startsWith("TIME_"), "the Time Magic effects are not touched: " + shape);
            assertFalse(shape.startsWith("MANA_ZONE") || shape.equals("GRAND_ZONE_ENV"), "a zone shape needs its own protocol: " + shape);
        }
    }

    @Test
    void everyActKeyOfTheTableIsAnActOfARelic() throws IOException {
        Set<String> keys = new HashSet<>();
        Pattern p = Pattern.compile("Act\\.of\\(\"([a-z0-9_]+)\"");
        try (Stream<Path> files = Files.list(WAVES)) {
            for (Path f : files.filter(x -> x.toString().endsWith(".java")).toList()) {
                Matcher m = p.matcher(Files.readString(f));
                while (m.find()) keys.add(m.group(1));
            }
        }
        assertTrue(keys.size() > 300, "act keys found in the wave sources: " + keys.size());
        for (String line : Files.readAllLines(TABLE)) {
            String l = line.trim();
            if (l.isEmpty() || l.startsWith("#")) continue;
            String[] v = l.split("\\s+");
            if (!v[2].equals("*")) assertTrue(keys.contains(v[2]), "act key " + v[2] + " of " + v[1] + " is not an act of any relic");
        }
    }
}
