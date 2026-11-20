package com.newuniverse.nusmp.heroweapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Why: the duration scale only stays honest if a length is scaled at exactly one place. These scans keep the chokepoints scaled, keep the unscaled twins unscaled,
 * stop a length from being scaled twice, keep the bow package and the pure classes out of it, and keep the server setting server-side.
 */
class DurSourceScanTest {
    private static final Path MAIN = Path.of("src/main/java/com/newuniverse/nusmp/heroweapon");

    private static String read(String rel) throws IOException { return Files.readString(MAIN.resolve(rel), StandardCharsets.UTF_8); }

    private static List<Path> weaponFiles() throws IOException {
        try (Stream<Path> s = Files.walk(MAIN.resolve("weapon"))) { return s.filter(p -> p.toString().endsWith(".java")).sorted().toList(); }
    }

    @Test
    void theChokepointsScaleAndTheirTwinsDoNot() throws IOException {
        String strike = read("weapon/strike/Strike.java");
        assertTrue(Pattern.compile("static void effect\\([^)]*\\)\\s*\\{[^}]*Dur\\.t\\(ticks\\)").matcher(strike).find(), "Strike.effect must scale");
        assertTrue(Pattern.compile("static void hold\\([^)]*\\)\\s*\\{[^}]*\\}").matcher(strike).find(), "Strike.hold exists");
        Matcher hold = Pattern.compile("static void hold\\([^)]*\\)\\s*\\{([^}]*)\\}").matcher(strike);
        assertTrue(hold.find());
        assertFalse(hold.group(1).contains("Dur."), "Strike.hold must stay unscaled");
        String status = read("weapon/kit/Status.java");
        assertTrue(status.contains("addRaw(t, effect, Dur.t(ticks), amp)"), "Status.add must scale");
        Matcher raw = Pattern.compile("static void addRaw\\([^)]*\\)\\s*\\{([^}]*)\\}").matcher(status);
        assertTrue(raw.find());
        assertFalse(raw.group(1).contains("Dur."), "Status.addRaw must stay unscaled");
    }

    @Test
    void noLengthIsScaledTwice() throws IOException {
        Pattern twice = Pattern.compile("(Strike\\.effect|Status\\.(add|slow|weaken))\\([^;]*(Dur\\.|controlTicks)");
        Pattern nested = Pattern.compile("Dur\\.(t|task|seconds)\\(\\s*Dur\\.");
        List<String> bad = new ArrayList<>();
        for (Path f : weaponFiles()) {
            String s = Files.readString(f, StandardCharsets.UTF_8);
            if (twice.matcher(s).find()) bad.add(f.getFileName() + " passes an already scaled or law-capped length to a scaled helper");
            if (nested.matcher(s).find()) bad.add(f.getFileName() + " nests Dur");
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void theBowPackageIsUntouched() throws IOException {
        for (Path f : weaponFiles()) if (f.toString().contains("/weapon/bow/")) assertFalse(Files.readString(f, StandardCharsets.UTF_8).contains("Dur."), f.toString());
    }

    @Test
    void theHelpersArePureAndTheTaskCapMatchesTheWeapons() throws IOException {
        for (String h : new String[] {"skill/Dur.java", "skill/DurText.java"}) {
            String s = read(h);
            assertFalse(s.contains("import net.minecraft"), h);
            assertFalse(s.contains("import com.newuniverse.nusmp.client"), h);
            assertFalse(s.contains("Minecraft.getInstance"), h);
        }
        assertTrue(read("skill/Dur.java").contains("TASK_CAP = 400"));
        assertTrue(read("weapon/vassalkit/Safe.java").contains("MAX_TICKS = 400"));
        assertTrue(read("weapon/sword/SwordNumbers.java").contains("MAX_TASK_TICKS = 400"));
        assertTrue(read("weapon/shield/ShieldNumbers.java").contains("MAX_TASK_TICKS = 400"));
    }

    @Test
    void theSettingIsAServerConfigWithTheAgreedRange() throws IOException {
        String cfg = read("HeroConfig.java");
        assertTrue(cfg.contains("defineInRange(\"durationScale\", 2.0, 0.25, 6.0)"));
        assertTrue(cfg.contains("double durationScale()"));
        assertTrue(read("HeroCore.java").contains("Dur.source(HeroConfig::durationScale)"));
        assertTrue(read("skill/Dur.java").contains("DEFAULT = 2.0, MIN = 0.25, MAX = 6.0"));
    }

    @Test
    void noDurationIsScaledOnTheClientSideOfAWeapon() throws IOException {
        // the book tooltip rewrites text only; the lengths themselves are decided on the server
        for (Path f : weaponFiles()) {
            String s = Files.readString(f, StandardCharsets.UTF_8);
            if (s.contains("Dur.")) assertFalse(s.contains("net.minecraft.client"), f.toString());
        }
    }
}
