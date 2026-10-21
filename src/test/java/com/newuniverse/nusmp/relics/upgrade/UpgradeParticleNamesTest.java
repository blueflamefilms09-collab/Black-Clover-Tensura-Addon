package com.newuniverse.nusmp.relics.upgrade;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Since 1.108.0 the effect tables of the upgraded relics reach the game as the mod's own custom particles, never as vanilla ones ({@link UpgradeParticleNames}, used by
 * {@code client/relics/fx/UpgradeFxClient} and {@link UpgradeActFx}): every particle name in every shipped table must resolve to a constant of the generated {@code FxOld} enum (the slot of the
 * theme that replaces that vanilla particle), or be "dust" (a custom mote dyed by the colour of its layer), or the mod's own "fx" token.
 */
class UpgradeParticleNamesTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Pattern NAME = Pattern.compile("\"particle\"\\s*:\\s*\"([^\"]*)\"");

    private static Set<String> fxOldConstants() throws IOException {
        Set<String> out = new LinkedHashSet<>();
        Pattern c = Pattern.compile("^\\s{4}([A-Z_]+)\\(FxSlot\\.", Pattern.MULTILINE);
        Matcher m = c.matcher(Files.readString(ROOT.resolve("src/main/java/com/newuniverse/nusmp/particle/fx/FxOld.java"), StandardCharsets.UTF_8));
        while (m.find()) out.add(m.group(1));
        return out;
    }

    @Test
    void theDecisionOnNames() {
        assertTrue(UpgradeParticleNames.isDust("dust") && UpgradeParticleNames.isDust("") && UpgradeParticleNames.isDust(null) && UpgradeParticleNames.isDust("minecraft:dust"));
        assertNull(UpgradeParticleNames.oldName("dust"));
        assertEquals("END_ROD", UpgradeParticleNames.oldName("minecraft:end_rod"));
        assertEquals("END_ROD", UpgradeParticleNames.oldName("end_rod"));
        assertEquals("FLAME", UpgradeParticleNames.oldName("Minecraft:FLAME"));
        assertEquals("ENCHANT", UpgradeParticleNames.oldName("minecraft:note"));
        assertEquals("BUBBLE", UpgradeParticleNames.oldName("minecraft:nautilus"));
        assertTrue(UpgradeParticleNames.isVanilla("minecraft:flame") && UpgradeParticleNames.isVanilla("flame"));
        assertFalse(UpgradeParticleNames.isVanilla("nusmp:mote"));
        assertEquals("flame", UpgradeParticleNames.path("minecraft:flame"));
    }

    @Test
    void everyNameInEveryShippedTableResolvesToACustomParticle() throws IOException {
        Set<String> constants = fxOldConstants();
        assertTrue(constants.size() > 30 && constants.contains("END_ROD"), "FxOld constants parsed: " + constants.size());
        Path dir = ROOT.resolve("src/main/resources/assets/nusmp/visual_upgrade");
        int names = 0;
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".json"))::iterator) {
                Matcher m = NAME.matcher(Files.readString(f, StandardCharsets.UTF_8));
                while (m.find()) {
                    String n = m.group(1);
                    names++;
                    if (n.equals("fx") || UpgradeParticleNames.isDust(n)) continue;
                    if (n.startsWith("nusmp:")) {                                          // 1.110.0: the shipped tables name the mod's own particles directly
                        assertTrue(com.newuniverse.nusmp.particle.fx.FxCatalog.has(n.substring(6)), f.getFileName() + ": '" + n + "' is not a particle of the catalog");
                        continue;
                    }
                    assertTrue(UpgradeParticleNames.isVanilla(n), f.getFileName() + ": '" + n + "' names another namespace; the tables use vanilla names, dust or fx");
                    String old = UpgradeParticleNames.oldName(n);
                    assertTrue(constants.contains(old), f.getFileName() + ": particle '" + n + "' resolves to " + old + ", which FxOld does not have (add it to UpgradeParticleNames.ALIAS)");
                }
            }
        }
        assertTrue(names > 500, "the tables name their particles: " + names);
    }

    @Test
    void theTwoClassesNoLongerBuildVanillaDust() throws IOException {
        for (String f : new String[]{"client/relics/fx/UpgradeFxClient.java", "relics/upgrade/UpgradeActFx.java"}) {
            String src = Files.readString(ROOT.resolve("src/main/java/com/newuniverse/nusmp/" + f), StandardCharsets.UTF_8);
            assertFalse(src.contains("DustParticleOptions"), f + " builds vanilla dust again");
            // 1.110.0: both classes build their options through UpgradeParticleOptions (the alias table of UpgradeParticleAlias); UpgradeParticleNames stays as the 1.108.0 name decision
            assertTrue(src.contains("UpgradeParticleOptions.of("), f + " does not go through the custom particle options");
            assertFalse(src.contains("BuiltInRegistries.PARTICLE_TYPE"), f + " reads the vanilla particle registry");
        }
    }
}
