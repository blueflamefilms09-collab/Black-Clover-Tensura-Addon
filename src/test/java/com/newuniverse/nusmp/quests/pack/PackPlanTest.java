package com.newuniverse.nusmp.quests.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Why: these are the rules that keep the owner's own quests safe; they must hold without touching a disk. */
class PackPlanTest {
    @Test
    void installDecisions() {
        assertEquals(PackPlan.InstallDecision.FRESH, PackPlan.decideInstall(false, true, false, false));
        assertEquals(PackPlan.InstallDecision.FRESH, PackPlan.decideInstall(true, true, false, false));
        assertEquals(PackPlan.InstallDecision.REFUSE, PackPlan.decideInstall(true, false, false, false));
        assertEquals(PackPlan.InstallDecision.FORCED_BACKUP, PackPlan.decideInstall(true, false, false, true));
        assertEquals(PackPlan.InstallDecision.OVER_OURS, PackPlan.decideInstall(true, false, true, false));
        assertEquals(PackPlan.InstallDecision.OVER_OURS, PackPlan.decideInstall(true, false, true, true));
    }

    @Test
    void backupNameIsUtcAndSortable() {
        assertEquals("quests_backup_19700101_000000", PackPlan.backupName(0));
        assertEquals("quests_backup_20261016_120000", PackPlan.backupName(1792152000000L));
    }

    @Test
    void updateWritesNewRetiresOldKeepsForeign() {
        Set<String> owned = Set.of("data.snbt", "chapters/a.snbt", "chapters/old.snbt");
        Set<String> fresh = Set.of("data.snbt", "chapters/a.snbt", "chapters/b.snbt");
        Set<String> existing = Set.of("data.snbt", "chapters/a.snbt", "chapters/old.snbt", "chapters/mine.snbt", ".nusmp_pack");
        PackPlan.UpdatePlan p = PackPlan.planUpdate(owned, fresh, existing);
        assertEquals(List.of("chapters/a.snbt", "chapters/b.snbt", "data.snbt"), p.toWrite());
        assertEquals(List.of("chapters/old.snbt"), p.toDelete());
        assertEquals(List.of("chapters/mine.snbt"), p.untouched());
    }

    @Test
    void updateNeverDeletesAFileTheOwnerAlreadyRemoved() {
        PackPlan.UpdatePlan p = PackPlan.planUpdate(Set.of("chapters/old.snbt"), Set.of("data.snbt"), Set.of("data.snbt"));
        assertTrue(p.toDelete().isEmpty());
    }

    @Test
    void uninstallDeletesOnlyListedExistingFiles() {
        List<String> gone = PackPlan.planUninstall(Set.of("data.snbt", "chapters/a.snbt", "chapters/gone.snbt"),
                Set.of("data.snbt", "chapters/a.snbt", "chapters/mine.snbt", ".nusmp_pack"));
        assertEquals(List.of("chapters/a.snbt", "data.snbt"), gone);
    }

    @Test
    void unsafePathsAreNeverTrusted() {
        assertFalse(PackPlan.safe("../x"));
        assertFalse(PackPlan.safe("a/../../x"));
        assertFalse(PackPlan.safe("/etc/passwd"));
        assertFalse(PackPlan.safe("C:\\x"));
        assertFalse(PackPlan.safe(".nusmp_pack"));
        assertTrue(PackPlan.safe("chapters/a.snbt"));
        assertEquals(List.of(), PackPlan.planUninstall(Set.of("../x"), Set.of("../x")));
    }

    @Test
    void manifestRoundTrip() {
        PackManifest m = new PackManifest("1.49.0", "abc", List.of("data.snbt", "chapters/a.snbt"));
        PackManifest back = PackManifest.parse(m.toText());
        assertNotNull(back);
        assertEquals(m, back);
        assertNull(PackManifest.parse("hello"));
        assertNull(PackManifest.parse(null));
    }

    @Test
    void hashDependsOnContentAndNamesNotOrder() {
        byte[] a = "a".getBytes(StandardCharsets.UTF_8), b = "b".getBytes(StandardCharsets.UTF_8);
        String h1 = PackManifest.hash(Map.of("x", a, "y", b));
        assertEquals(16, h1.length());
        assertEquals(h1, PackManifest.hash(new java.util.LinkedHashMap<>(Map.of("y", b, "x", a))));
        assertFalse(h1.equals(PackManifest.hash(Map.of("x", b, "y", a))));
    }
}
