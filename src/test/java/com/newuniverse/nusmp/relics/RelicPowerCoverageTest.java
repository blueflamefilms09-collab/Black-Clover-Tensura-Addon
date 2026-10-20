package com.newuniverse.nusmp.relics;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 1.104.0, source and data test: the attune lock is ONE central rule, and every main-hand relic that needs attunement falls under it. The relic cards are read
 * from the wave sources (grade, slot, {@code .attune()}); each one that needs attunement by the grade rule must come out below full power when it is not the
 * wielder's, and no relic id is exempted anywhere in the rule's code.
 */
class RelicPowerCoverageTest {
    private static final Path SRC = Paths.get("src/main/java/com/newuniverse/nusmp");
    private static final Pattern CARD = Pattern.compile("RelicDef\\.of\\(([^,]+),[^,]+,\\s*Grade\\.([A-Z]+),\\s*RelicSlot\\.MAIN_HAND\\)");

    private static String read(String rel) throws IOException { return Files.readString(SRC.resolve(rel)); }

    @Test void everyMainHandRelicThatNeedsAttunementIsCutWhenUnattuned() throws IOException {
        int cards = 0, attuned = 0;
        try (Stream<Path> files = Files.walk(SRC.resolve("relics"))) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                String text = Files.readString(f);
                Matcher m = CARD.matcher(text);
                while (m.find()) {
                    cards++;
                    int end = text.indexOf("RelicDef.of(", m.end());
                    String chain = text.substring(m.end(), end < 0 ? text.length() : end);
                    int stop = chain.indexOf("out.accept");
                    if (stop >= 0) chain = chain.substring(0, stop);
                    boolean written = chain.contains(".attune()");
                    Grade g = Grade.valueOf(m.group(2));
                    boolean needs = AttuneRules.requires(written, g);
                    double unattuned = RelicPower.factor(m.group(1).trim(), false, needs, RelicPower.Config.DEFAULT);
                    double owner = RelicPower.factor(m.group(1).trim(), true, needs, RelicPower.Config.DEFAULT);
                    assertEquals(1.0, owner, 0.0, f.getFileName() + " " + m.group(1));
                    if (needs) {
                        attuned++;
                        assertTrue(unattuned < 1.0, f.getFileName() + " " + m.group(1) + " needs attunement but the lock does not cut it");
                    } else {
                        assertEquals(1.0, unattuned, 0.0, f.getFileName() + " " + m.group(1) + " needs no attunement and must stay at full power");
                    }
                }
            }
        }
        assertTrue(cards > 50, "main-hand relic cards found: " + cards);
        assertTrue(attuned > 5, "main-hand relics that need attunement found: " + attuned);
    }

    @Test void theRuleHasNoRelicIdInIt() throws IOException {
        assertTrue(RelicPower.EXEMPT_IDS.isEmpty());
        for (String file : new String[]{"relics/RelicPower.java", "relics/RelicPowerEvents.java"}) {
            String code = read(file).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
            assertFalse(code.contains("equals(\""), file + " compares against a literal id");
            assertFalse(code.toLowerCase().contains("elsdocia"), file + " names a relic");
        }
        String events = read("relics/RelicPowerEvents.java");
        assertTrue(events.contains("Relics.defOf") && events.contains("needsAttunement()"), "the hook asks the relic registry, not a list");
    }

    @Test void theRuleIsWiredOnceInTheEngine() throws IOException {
        String engine = read("relics/RelicEngine.java");
        assertTrue(engine.contains("RelicPowerEvents.register(bus)"));
        assertTrue(engine.contains("RelicPowerEvents.actFactor(stack, p)"), "the act-power helper is multiplied by the same rule");
        assertTrue(read("relics/RelicCommands.java").contains("Commands.literal(\"power\")"));
        assertTrue(read("relics/RelicSystem.java").contains("RelicPowerSelfTests.register()"));
        assertTrue(read("NUSMP.java").contains("RelicPowerConfig.SPEC"));
        assertTrue(read("relics/RelicPowerSelfTests.java").contains("\"relic.attune_lock\""));
    }

    @Test void everyRelicWeaponItemClassIsARelicBearer() throws IOException {
        for (String f : new String[]{"relics/RelicWeaponItem.java", "relics/RelicBowItem.java", "relics/RelicCrossbowItem.java"})
            assertTrue(read(f).contains("implements RelicBearer"), f + " must be a RelicBearer so Relics.defOf finds its def");
    }

    @Test void djinnAndOtherWeaponItemsAreNotRelicBearers() throws IOException {
        // the lock asks Relics.defOf(stack): items that are not relic cards (Djinn weapons, Requip, hero weapons, loot gear) never answer
        try (Stream<Path> files = Files.walk(SRC.resolve("relics/djinn/weapon"))) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith("Item.java"))::iterator)
                assertFalse(Files.readString(f).contains("implements RelicBearer"), f.getFileName() + " would fall under the lock");
        }
        assertFalse(read("heroweapon/HeroWeaponItem.java").contains("RelicBearer"));
    }

    @Test void langFragmentHasTheThreeLines() throws IOException {
        String json = Files.readString(Paths.get("tools/lang/attune_lock.json"));
        for (String k : new String[]{"tooltip.nusmp.relic.unattuned_power", "tooltip.nusmp.relic.attuned_other", "message.nusmp.relic.attune_lock.hit"})
            assertTrue(json.contains("\"" + k + "\""), k);
    }
}
