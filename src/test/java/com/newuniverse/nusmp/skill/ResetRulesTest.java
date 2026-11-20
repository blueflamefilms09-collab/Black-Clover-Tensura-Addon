package com.newuniverse.nusmp.skill;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 3.42.0 Reset Scroll guard: the pure decision rules, and the source rules the hook rests on (read from the repo's files). */
class ResetRulesTest {
    private static String code(String file) throws IOException {
        String s = Files.readString(Path.of(file));
        return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
    }

    @Test
    void onlyOurNamespaceIsOurs() {
        assertTrue(ResetRules.isOurs("anime_multiverse"));
        assertFalse(ResetRules.isOurs("tensura"));
        assertFalse(ResetRules.isOurs("manascore"));
        assertFalse(ResetRules.isOurs("NUSMP"));
        assertFalse(ResetRules.isOurs(""));
        assertFalse(ResetRules.isOurs(null));
    }

    @Test
    void theScrollClassAndItsNestedClassesAreScrollFrames() {
        assertTrue(ResetRules.isScrollClass("io.github.manasmods.tensura.item.misc.ResetScrollItem"));
        assertTrue(ResetRules.isScrollClass("io.github.manasmods.tensura.item.misc.ResetScrollItem$ResetType"));
        assertTrue(ResetRules.isScrollClass("io.github.manasmods.tensura.item.misc.ResetScrollItem$1"));
        assertFalse(ResetRules.isScrollClass("io.github.manasmods.tensura.item.misc.ResetScrollItemOther"), "a different class that only starts with the same letters");
        assertFalse(ResetRules.isScrollClass("io.github.manasmods.tensura.item.misc.ScrollItem"));
        assertFalse(ResetRules.isScrollClass("com.newuniverse.nusmp.skill.ResetGuard"));
        assertFalse(ResetRules.isScrollClass(null));
    }

    @Test
    void aStackIsAScrollWhenAnyFrameIs() {
        assertTrue(ResetRules.anyScrollFrame(List.of("a.B", "io.github.manasmods.tensura.item.misc.ResetScrollItem", "c.D")));
        assertFalse(ResetRules.anyScrollFrame(List.of("a.B", "io.github.manasmods.manascore.skill.api.Skills", "c.D")));
        assertFalse(ResetRules.anyScrollFrame(List.of()));
    }

    @Test
    void theDecisionTable() {
        // guard on, our skill, from a scroll: refused
        assertTrue(ResetRules.refuses(true, "anime_multiverse", true));
        // guard off: the old behaviour
        assertFalse(ResetRules.refuses(false, "anime_multiverse", true));
        // not from a scroll (a grimoire swap, /multiverse take, revoking Shishou, the admin command): passes
        assertFalse(ResetRules.refuses(true, "anime_multiverse", false));
        // somebody else's skill: the scroll still resets it
        assertFalse(ResetRules.refuses(true, "tensura", true));
        assertFalse(ResetRules.refuses(true, "othermod", true));
        assertFalse(ResetRules.refuses(true, null, true));
    }

    @Test
    void soulTiedSkillsAreTheGrimoireFamily() {
        for (String id : new String[]{"book_fire", "book_time", "grimoire_flame", "grimoire_magic", "page_chrono_stasis", "merged_07", "forbidden_magic",
                "devil_bond", "devil_magic", "anti_magic_spirit_lord", "spirit_lord"})
            assertTrue(ResetRules.soulTied(id), id);
        for (String id : new String[]{"hercules", "shishou", "nen", "ki", "book_maker", "the_almighty", "all_fiction", "", null})
            assertFalse(ResetRules.soulTied(id), String.valueOf(id));
    }

    @Test
    void soulOnlyDecisionTable() {
        // new default: the grimoire stays, an ordinary skill of ours is reset like any other
        assertTrue(ResetRules.refuses(true, true, "anime_multiverse", "book_fire", true));
        assertFalse(ResetRules.refuses(true, true, "anime_multiverse", "hercules", true));
        // old behaviour behind soulOnly=false: every skill of ours stays
        assertTrue(ResetRules.refuses(true, false, "anime_multiverse", "hercules", true));
        // never a removal that is not from a scroll, never with the guard off, never somebody else's skill
        assertFalse(ResetRules.refuses(true, true, "anime_multiverse", "book_fire", false));
        assertFalse(ResetRules.refuses(false, true, "anime_multiverse", "book_fire", true));
        assertFalse(ResetRules.refuses(true, true, "tensura", "book_fire", true));
    }

    @Test
    void theSoulRuleIsWiredAndSwitchedOff() throws IOException {
        String g = code("src/main/java/com/newuniverse/nusmp/skill/ResetGuard.java");
        assertTrue(g.contains("ResetRules.soulTied(") && g.contains("setToggled(false)") && g.contains("onToggleOff("), "a soul-tied skill is kept and switched off");
        assertTrue(g.contains("soulOnly()"), "the setting is read");
        assertTrue(code("src/main/java/com/newuniverse/nusmp/multiverse/MultiverseConfig.java").contains("\"resetSoulOnly\""), "a server setting restores the 3.42.0 behaviour");
    }

    // ---------------------------------------------------------------- source rules
    @Test
    void theHookIsServerSafeAndCancelsThroughTheEvent() throws IOException {
        String g = code("src/main/java/com/newuniverse/nusmp/skill/ResetGuard.java");
        assertTrue(g.contains("SkillEvents.REMOVE_SKILL.register("), "hooked on ManasCore's remove-skill event");
        assertTrue(g.contains("EventResult.interruptFalse()"), "the removal is cancelled through the event");
        assertTrue(g.contains("instanceof ServerPlayer"), "only a server player is guarded");
        assertTrue(g.contains("ResetRules.refuses("), "the decision is the pure rule");
        assertFalse(g.contains("net.minecraft.client") || g.contains("com.mojang.blaze3d") || g.contains("com.newuniverse.nusmp.client") || g.contains("Minecraft.getInstance"), "no client classes");
        assertFalse(g.contains("static ServerPlayer") || g.contains("static Player"), "no static current player");
        assertFalse(g.contains("EVENT_BUS") && g.contains("TickEvent"), "no per-tick work");
    }

    @Test
    void theGuardIsRegisteredAndConfigurable() throws IOException {
        String main = code("src/main/java/com/newuniverse/nusmp/NUSMP.java");
        assertTrue(main.contains("ResetGuard.register()"), "registered at start-up");
        assertTrue(main.contains("ResetGuard.protectScrollLists()"), "the scroll lists are protected once the server starts");
        String cfg = code("src/main/java/com/newuniverse/nusmp/multiverse/MultiverseConfig.java");
        assertTrue(cfg.contains("\"resetScrollGuard\""), "a server setting restores the old behaviour");
        assertTrue(cfg.contains("RESET_SCROLL_GUARD"), "declared");
    }

    @Test
    void theSelfTestIsRegisteredAndInTheSmokeList() throws IOException {
        String t = code("src/main/java/com/newuniverse/nusmp/skill/ResetGuardSelfTests.java");
        assertTrue(t.contains("\"skill.reset_guard\""), "self-test id");
        assertTrue(t.contains("ResetGuard.hooked()") && code("src/main/java/com/newuniverse/nusmp/skill/ResetGuard.java").contains("hooked = true"), "the self-test checks that the hook was taken");
        assertTrue(t.contains("NOT EXERCISED"), "a stand-in that cannot hold skills is told, not failed");
        String smoke = Files.readString(Path.of(".github/workflows/smoke.yml"));
        assertTrue(smoke.contains("skill.reset_guard"), "the smoke list expects the self-test");
        assertTrue(code("src/main/java/com/newuniverse/nusmp/NUSMP.java").contains("ResetGuardSelfTests.register()"), "registered");
    }
}
