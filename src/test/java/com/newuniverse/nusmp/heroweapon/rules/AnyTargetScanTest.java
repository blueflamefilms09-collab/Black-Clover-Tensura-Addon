package com.newuniverse.nusmp.heroweapon.rules;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the owner wants hero and vassal techniques to work on any living target. Every "who is a foe" filter must read the one server setting, so a new filter cannot silently stay hostile-only. */
class AnyTargetScanTest {
    private static final String ROOT = "src/main/java/com/newuniverse/nusmp/";

    private static String read(String rel) throws Exception { return Files.readString(Path.of(ROOT + rel)); }

    @Test
    void everyFoeFilterReadsTheSetting() throws Exception {
        for (String f : new String[]{"heroweapon/weapon/vassalkit/Foes.java", "heroweapon/weapon/pierce/Foes.java", "heroweapon/weapon/sword/SwordTargets.java",
                "heroweapon/weapon/shield/ShieldTargets.java", "heroweapon/weapon/glassworld/GlassKit.java"})
            assertTrue(read(f).contains("HeroConfig.anyTarget()"), f + " must read heroweapon.anyTarget");
    }

    @Test
    void strikeScanSwitchesOnTheSettingAndKeepsTheOldScan() throws Exception {
        String s = read("heroweapon/weapon/strike/Strike.java");
        assertTrue(s.contains("anyTarget() ? Fx.targetsAround(p, radius) : Fx.foesAround(p, radius)"));
        assertTrue(read("relics/Fx.java").contains("public static List<LivingEntity> foesAround"), "the old hostile scan stays");
    }

    @Test
    void settingExistsAndDefaultsOn() throws Exception {
        String c = read("heroweapon/HeroConfig.java");
        assertTrue(c.contains("define(\"anyTarget\", true)"));
    }
}
