package com.newuniverse.nusmp.core.magic.profile;

import com.newuniverse.nusmp.blackclover.MagicType.HitEffect;
import com.newuniverse.nusmp.vfx.VfxShape;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MagicAuditTest {
    /** A profile that has everything. */
    private static MagicProfile good(String id) {
        return new MagicProfile() {
            @Override public String id() { return id; }
            @Override public String displayName() { return id + " Magic"; }
            @Override public String word() { return id; }
            @Override public String soulType() { return "FLAME"; }
            @Override public HitEffect hit() { return HitEffect.BURN; }
            @Override public int color() { return 0xFFFF8800; }
        };
    }

    private static final class FakeWorld implements MagicAudit.Probe {
        final Set<String> resources = new HashSet<>(), lang = new HashSet<>();
        boolean books = true;
        int pages = 5;
        @Override public boolean hasResource(String p) { return resources.contains(p); }
        @Override public boolean hasLang(String k) { return lang.contains(k); }
        @Override public String bookPath(String id) { return books ? MagicKeys.bookPath(id) : null; }
        @Override public int pageCount(String id) { return books ? pages : -1; }
        List<String> pageIds = List.of();
        @Override public List<String> familyPageIds(String id) { return pageIds; }
    }

    private static FakeWorld complete(String... ids) {
        FakeWorld w = new FakeWorld();
        w.resources.add(MagicKeys.FALLBACK_ICON);
        for (String id : ids) {
            w.resources.add(MagicKeys.iconPath(id));
            w.lang.add(MagicKeys.bookNameKey(MagicKeys.bookPath(id)));
            w.lang.add(MagicKeys.bookDescriptionKey(MagicKeys.bookPath(id)));
        }
        return w;
    }

    private static List<MagicProfile> with(String... ids) {
        List<MagicProfile> l = new ArrayList<>();
        for (String id : ids) l.add(good(id));
        return l;
    }

    private static List<MagicProfile> restrictedToo(String... ids) {
        List<MagicProfile> l = with(ids);
        for (String r : MagicKeys.RESTRICTED) l.add(good(r));
        return l;
    }

    @Test
    void aCompleteWorldIsClean() {
        MagicAudit.Report r = MagicAudit.run(restrictedToo("FLAME", "WATER"), complete("FLAME", "WATER", "ANTI_MAGIC", "KOTODAMA", "DEMON_BEAST", "CURSE",
                "DEMON_FIRE", "DEMON_ICE", "DEMON_LIGHT", "DEMON_WATER"), 10);
        assertTrue(r.clean(), r.lines().toString());
        assertEquals(0, r.warnings(), r.lines().toString());
        assertEquals(10, r.checked());
    }

    @Test
    void missingIconLangAndBookAreReportedPerMagic() {
        FakeWorld w = complete("FLAME");                  // WATER has nothing
        MagicAudit.Report r = MagicAudit.run(restrictedToo("FLAME", "WATER"), w, 0);
        assertTrue(r.has("WATER", "NO_ICON"));
        assertTrue(r.has("WATER", "NO_LANG_NAME"));
        assertTrue(r.has("WATER", "NO_LANG_DESC"));
        assertFalse(r.has("FLAME", "NO_ICON"));
        assertFalse(r.has("FLAME", "NO_LANG_NAME"));
        assertTrue(r.summary().contains("NO_ICON"));
        w.books = false;
        MagicAudit.Report r2 = MagicAudit.run(with("FLAME"), w, 0);
        assertTrue(r2.has("FLAME", "NO_BOOK"));
        assertFalse(r2.has("FLAME", "NO_PAGES"), "no book: the missing book is the finding, not the pages");
        w.books = true;
        w.pages = 0;
        assertTrue(MagicAudit.run(with("FLAME"), w, 0).has("FLAME", "NO_PAGES"));
    }

    @Test
    void pageDescriptionsAreChecked() {
        FakeWorld w = complete("FLAME");
        w.pageIds = List.of("a", "b", "c");
        w.lang.add(MagicKeys.pageKey("book_fire", "b"));
        MagicAudit.Report r = MagicAudit.run(with("FLAME"), w, 0);
        assertTrue(r.has("FLAME", "NO_LANG_PAGES"));
        assertTrue(r.lines().toString().contains("2 of 3 pages"), r.lines().toString());
        w.lang.add(MagicKeys.pageKey("book_fire", "a"));
        w.lang.add(MagicKeys.pageKey("book_fire", "c"));
        assertFalse(MagicAudit.run(with("FLAME"), w, 0).has("FLAME", "NO_LANG_PAGES"));
        assertEquals("nusmp.page.book_fire.a", MagicKeys.pageKey("book_fire", "a"));
    }

    @Test
    void missingFallbackIconIsAnError() {
        FakeWorld w = complete("FLAME");
        w.resources.remove(MagicKeys.FALLBACK_ICON);
        MagicAudit.Report r = MagicAudit.run(with("FLAME"), w, 0);
        assertFalse(r.clean());
        assertTrue(r.has("*", "NO_FALLBACK_ICON"));
    }

    @Test
    void anIncompleteProfileIsAnError() {
        MagicProfile bad = new MagicProfile() {
            @Override public String id() { return "BAD"; }
            @Override public String displayName() { return " "; }
            @Override public String word() { return null; }
            @Override public String soulType() { return ""; }
            @Override public HitEffect hit() { return null; }
            @Override public VfxShape castShape() { return null; }
            @Override public int color() { return 0x00112233; }
            @Override public double safeCostPercent() { return Double.NaN; }
        };
        MagicAudit.Report r = MagicAudit.run(List.of(bad), null, 0);
        for (String code : new String[]{"NO_NAME", "NO_WORD", "NO_SOUL", "NO_HIT", "NO_VFX_SHAPE", "NO_COLOR", "BAD_SAFE_NUMBERS"})
            assertTrue(r.has("BAD", code), code + " in " + r.lines());
        assertFalse(r.clean());
    }

    @Test
    void duplicatesNullsAndRestrictedTyposAreErrors() {
        List<MagicProfile> l = new ArrayList<>(with("FLAME", "flame"));
        l.add(null);
        MagicAudit.Report r = MagicAudit.run(l, null, 0);
        assertTrue(r.has("FLAME", "DUPLICATE_ID"));
        assertEquals(1, r.count("NULL_PROFILE"));
        assertTrue(r.has("ANTI_MAGIC", "RESTRICTED_UNKNOWN"), "a restricted name with no magic behind it");
        MagicAudit.Report r2 = MagicAudit.run(restrictedToo("FLAME"), null, 0);
        assertEquals(0, r2.count("RESTRICTED_UNKNOWN"));
    }

    @Test
    void theCardSeedLimitIsWarnedAbout() {
        assertEquals(1, MagicAudit.run(with("FLAME"), null, 105).count("CARD_SEED_LIMIT"));
        assertEquals(0, MagicAudit.run(with("FLAME"), null, 99).count("CARD_SEED_LIMIT"));
    }

    @Test
    void nullInputsDoNotThrow() {
        assertNotNull(MagicAudit.run(null, null, 0));
        assertTrue(MagicAudit.run(null, null, 0).clean());
    }

    @Test
    void mergedProfilesSkipTheOutsideChecks() {
        MagicProfile merged = new MagicProfile() {
            @Override public String id() { return "merged:flame+water"; }
            @Override public String soulType() { return "FLAME"; }
        };
        MagicAudit.Report r = MagicAudit.run(List.of(merged), complete(), 0);
        assertEquals(0, r.count("NO_ICON"));
        assertEquals(0, r.count("NO_BOOK"));
        assertTrue(r.clean(), r.lines().toString());
    }
}
