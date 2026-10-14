package com.newuniverse.nusmp.core.magic.profile;

import com.newuniverse.nusmp.blackclover.MagicType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The audit over the REAL enum and the REAL resource folder of this checkout: every MagicType resolves a complete profile, the restricted
 * flags are what they were before the profile existed, and nothing that would throw at runtime is missing. Cosmetic gaps (lang names) are
 * printed, not asserted, so adding a magic never breaks the build for a missing sentence.
 */
class MagicEnumAuditTest {
    private static final Path MAIN = Path.of("src/main");
    private static final Path RES = MAIN.resolve("resources/assets/nusmp");

    /** The switch that MagicType.restricted() contained before 1.27.0. */
    private static final Set<MagicType> LEGACY_RESTRICTED = Set.of(MagicType.ANTI_MAGIC, MagicType.KOTODAMA, MagicType.DEMON_BEAST, MagicType.CURSE,
            MagicType.DEMON_FIRE, MagicType.DEMON_ICE, MagicType.DEMON_LIGHT, MagicType.DEMON_WATER);

    @Test
    void restrictedAnswersExactlyAsBefore() {
        for (MagicType t : MagicType.values()) assertEquals(LEGACY_RESTRICTED.contains(t), t.restricted(), t.name());
        Set<String> names = new HashSet<>();
        for (MagicType t : MagicType.values()) names.add(t.name());
        for (String r : MagicKeys.RESTRICTED) assertTrue(names.contains(r), r + " is a restricted name but not a magic");
    }

    @Test
    void everyConstantResolvesACompleteProfile() {
        for (MagicType t : MagicType.values()) {
            MagicProfile p = MagicProfiles.of(t);
            assertNotNull(p, t.name());
            assertEquals(t.name(), p.id());
            assertEquals(t.displayName, p.displayName(), t.name());
            assertEquals(t.word, p.word(), t.name());
            assertEquals(t.soulType, p.soulType(), t.name());
            assertSame(t.hit, p.hit(), t.name());
            assertNotNull(p.castShape(), t.name());
            assertNotNull(p.fieldShape(), t.name());
            assertNotNull(p.burstShape(), t.name());
            assertNotNull(p.vfxStyle(), t.name());
            assertSame(p, MagicProfiles.of(t.name()), "the same answer by name");
            assertSame(p, MagicProfiles.of(t.name().toLowerCase()), "any case");
            assertEquals(t.displayName, t.displayName(), "the accessor goes through the profile");
        }
    }

    @Test
    void unknownIdsNeverThrowAndNeverReturnNull() {
        for (String id : new String[]{null, "", "  ", "NOT_A_MAGIC", "merged:nothing+here", "FLAME ", "ice wedge"}) {
            MagicProfile p = MagicProfiles.of(id);
            assertNotNull(p, String.valueOf(id));
            assertNotNull(p.displayName());
            assertFalse(p.displayName().isBlank());
            assertNotNull(p.castShape());
            assertNotNull(p.soulType());
        }
        assertSame(MagicProfiles.of(MagicType.ICE_WEDGE), MagicProfiles.of("ice wedge"));
        assertNotNull(MagicProfiles.of((MagicType) null));
        assertNull(MagicType.lookup("NOT_A_MAGIC"));
        assertNull(MagicType.lookup(null));
        assertSame(MagicType.ICE_WEDGE, MagicType.lookup("ice-wedge"));
        assertSame(MagicType.FLAME, MagicType.byName("NOT_A_MAGIC"), "byName keeps its old behaviour");
    }

    @Test
    void explicitOverridesWinAndUnregisterRestoresTheDerivedAnswer() {
        MagicProfile derived = MagicProfiles.of(MagicType.AIR);
        MagicProfiles.override(MagicType.AIR, o -> o.color(0xFF123456).displayName("Gale Test"));
        try {
            assertEquals(0xFF123456, MagicProfiles.of(MagicType.AIR).color());
            assertEquals("Gale Test", MagicProfiles.of("air").displayName());
            assertEquals("Gale Test", MagicType.AIR.displayName(), "the old enum API sees the override");
            assertEquals(derived.soulType(), MagicProfiles.of(MagicType.AIR).soulType(), "everything not set keeps its default");
        } finally {
            MagicProfiles.unregister("AIR");
        }
        assertEquals(derived.displayName(), MagicProfiles.of(MagicType.AIR).displayName());
        assertFalse(MagicProfiles.hasOverride("AIR"));
    }

    @Test
    void theAuditOfThisCheckoutHasNoErrors() throws IOException {
        MagicAudit.Report r = MagicAudit.run(MagicProfiles.allEnum(), new FileProbe(), MagicType.values().length);
        for (MagicAudit.Finding f : r.of(MagicAudit.Severity.ERROR)) System.out.println("  " + f);
        System.out.println("  [audit of this checkout] " + r.checked() + " magics, " + r.errors() + " errors, " + r.warnings() + " warnings: " + r.summary());
        assertEquals(0, r.errors(), r.lines().toString());
        assertEquals(0, r.count("NO_ICON"), "every magic has its grimoire icon");
        assertEquals(0, r.count("NO_BOOK"), "every magic has a registered Unique book");
    }

    // ---------------------------------------------------------------- the checkout as a probe (no game classes)
    private static final class FileProbe implements MagicAudit.Probe {
        private final Set<String> lang = new HashSet<>(), bookIds = new HashSet<>(), wiki = new HashSet<>();

        FileProbe() throws IOException {
            Matcher m = Pattern.compile("^\\s*\"([^\"]+)\"\\s*:", Pattern.MULTILINE).matcher(read(RES.resolve("lang/en_us.json")));
            while (m.find()) lang.add(m.group(1));
            String nus = read(MAIN.resolve("java/com/newuniverse/nusmp/skill/NUSkills.java"));
            m = Pattern.compile("book\\(\"([a-z_]+)\"").matcher(nus);
            while (m.find()) bookIds.add(m.group(1));
            m = Pattern.compile("SKILLS\\.register\\(\"grimoire_\" \\+ m\\.name\\(\\)\\.toLowerCase\\(\\)").matcher(nus);
            assertTrue(m.find(), "NUSkills registers one legacy grimoire skill per constant");
            String wb = read(MAIN.resolve("java/com/newuniverse/nusmp/book/WikiBooks.java"));
            String list = wb.substring(wb.indexOf("EnumSet.of(") + 11, wb.indexOf(");", wb.indexOf("EnumSet.of(")));
            m = Pattern.compile("MagicType\\.([A-Z_]+)").matcher(list);
            while (m.find()) wiki.add(m.group(1));
        }

        private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

        @Override public boolean hasResource(String assetPath) { return Files.exists(RES.resolve(assetPath)); }
        @Override public boolean hasLang(String key) { return lang.contains(key); }

        @Override public String bookPath(String id) {
            String path = MagicKeys.bookPath(id);
            return bookIds.contains(path) || wiki.contains(MagicKeys.enumKey(id)) ? path : null;
        }

        @Override public int pageCount(String id) { return bookPath(id) == null ? -1 : 1; }
    }
}
