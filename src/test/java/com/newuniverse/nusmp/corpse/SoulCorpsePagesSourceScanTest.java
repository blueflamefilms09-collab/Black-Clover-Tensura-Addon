package com.newuniverse.nusmp.corpse;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why (owner report 2.1.0, "none of the new Soul Corpse summon spells work", docs/magic/soul_corpse_fix.md): nothing of the cast can be played here, so the shape that makes a Soul Corpse page work is
 * read from the sources: every page id of the book has a handler that exists, the character locks are exactly Rades's six and no generic summon is locked, the unlock path goes through the
 * rules of {@link SoulCorpseAccess} (the starter pages, the capped mastery, the missing rank, the page slots), the summon wiring is in place (entity type, attributes, renderer, hooks, the server
 * config with the zombies on, the caps), and the self-test that casts through the real grimoire is registered.
 */
class SoulCorpsePagesSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String code(String rel) throws IOException {
        return Files.readString(SRC.resolve(rel), StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
    }

    @Test
    void everyPageIdHasAHandlerThatExists() throws IOException {
        String all = code("book/SoulCorpseBook.java") + "\n" + code("book/ext/SoulCorpseExt.java");
        Pattern def = Pattern.compile("\\b(?:starter|mid|zone|signature)\\(\"([a-z_]+)\",\\s*\"[^\"]*\",\\s*([^\\n]*)");
        Map<String, String> handlers = new LinkedHashMap<>();
        Matcher m = def.matcher(all);
        while (m.find()) assertNull(handlers.put(m.group(1), m.group(2)), "page " + m.group(1) + " is defined twice");
        String book = code("book/SoulCorpseBook.java"), spells = code("corpse/SoulCorpseSpells.java");
        for (String id : SoulCorpseAccess.ORDER) {
            String h = handlers.get(id);
            assertNotNull(h, "page " + id + " is not defined in SoulCorpseBook / SoulCorpseExt");
            Matcher ref = Pattern.compile("(SoulCorpseBook|SoulCorpseSpells)::([A-Za-z]+)").matcher(h);
            if (ref.find()) {
                String owner = ref.group(1).equals("SoulCorpseBook") ? book : spells;
                assertTrue(Pattern.compile("static boolean " + ref.group(2) + "\\(GrimoireBook").matcher(owner).find(), "the handler " + ref.group(0) + " of " + id + " does not exist");
            } else {
                assertTrue(h.contains("->") && h.contains("SoulCorpseSpells.named("), "page " + id + " has no handler: " + h);
            }
        }
        assertTrue(handlers.keySet().containsAll(SoulCorpseAccess.ORDER) && SoulCorpseAccess.ORDER.containsAll(handlers.keySet()),
                "the table of SoulCorpseAccess and the book disagree: " + handlers.keySet());
        for (String id : SoulCorpseAccess.ORDER) assertTrue(indexOfDef(all, id) >= 0, id);
        String late = code("book/ext/SoulCorpseExt.java");
        assertTrue(late.indexOf("\"corpse_david\"") > late.indexOf("latePages()") && late.indexOf("\"corpse_michael_caesal\"") > late.indexOf("\"corpse_david\""), "David and Michael Caesal are the appended last pages");
        assertTrue(book.indexOf("SoulCorpseExt.latePages()") > book.indexOf("\"corpse_legion\""), "the late pages are appended after Legion so no mode number moves");
    }

    private static int indexOfDef(String all, String id) {
        Matcher m = Pattern.compile("\\b(?:starter|mid|zone|signature)\\(\"" + id + "\"").matcher(all);
        return m.find() ? m.start() : -1;
    }

    @Test
    void theCharacterLocksAreExactlyRadesSixAndNoGenericSummonIsLocked() throws IOException {
        String c = code("book/CharacterSpells.java");
        Matcher m = Pattern.compile("lock\\(\"book_soul_corpse\",\\s*\"([a-z_]+)\",\\s*CanonBook\\.([A-Z_]+)\\)").matcher(c);
        Set<String> locked = new HashSet<>();
        while (m.find()) {
            locked.add(m.group(1));
            assertEquals("RADES", m.group(2), m.group(1) + " is locked to someone besides Rades");
        }
        assertEquals(new HashSet<>(SoulCorpseAccess.RADES_ONLY), locked, "the Rades locks and SoulCorpseAccess.RADES_ONLY disagree");
        for (String id : SoulCorpseAccess.GENERIC_SUMMONS) assertFalse(locked.contains(id), id + " is a generic summon and must not be character-locked");
    }

    @Test
    void theUnlockPathGoesThroughTheRulesOfSoulCorpseAccess() throws IOException {
        String mp = code("multiverse/MasteryPages.java");
        assertTrue(mp.contains("SoulCorpseAccess.needed(") && mp.contains("SoulCorpseAccess.rankWaived(") && mp.contains("SoulCorpseAccess.slotFree("), "MasteryPages lost a rule of SoulCorpseAccess");
        int tryUnlock = mp.indexOf("public static int tryUnlock");
        assertTrue(tryUnlock > 0);
        String body = mp.substring(tryUnlock);
        assertTrue(body.contains("needed(book, page, m, n)") && !body.contains("masteryCost(page, m, n)"), "tryUnlock must ask needed(), not the raw masteryCost()");
        assertTrue(body.contains("slotFree(bookPath, page.id())"), "tryUnlock must let the summon pages past a full slot count");
        String gate = mp.substring(mp.indexOf("public static Gate gate"), tryUnlock);
        assertTrue(gate.indexOf("rankWaived(") > gate.indexOf("PAGE_GATES"), "a rank written in the server config still wins over the waiver");
        String corpses = code("corpse/Corpses.java");
        assertTrue(corpses.contains("SoulCorpseAccess.isStarter(pages.get(m).id())") && corpses.contains("GrimoireBook.unlock(inst, m)"), "the starter pages are no longer opened");
        String hooks = code("corpse/CorpseHooks.java");
        assertTrue(hooks.contains("Corpses.openStarterPage(p)") && hooks.contains("p.tickCount % 40 == 13"), "the starter pages are opened on the player tick");
        assertTrue(code("client/ui/codex/SpellSource.java").contains("MasteryPages.needed("), "the codex must show the mastery the page really asks");
    }

    @Test
    void theSummonPagesTellThePlayerWhenNothingRoseAndTheCastsAreGuarded() throws IOException {
        String book = code("book/SoulCorpseBook.java");
        int marionette = book.indexOf("private static boolean marionette"), swap = book.indexOf("private static boolean swap"), legion = book.indexOf("private static boolean legion");
        assertTrue(book.substring(marionette, swap).contains("SoulZombies.refusal(") && book.substring(marionette, swap).contains("return false"), "the Marionette must say why when nothing rose");
        assertTrue(book.substring(legion).contains("SoulZombies.refusal(") && book.substring(legion).contains("return false"), "Legion must say why when nothing rose");
        assertTrue(book.contains("CorpseConfig.zombiesInsteadOfPuppets()") && book.contains("SoulZombies.summon("), "the raise path lost the zombie switch");
        String z = code("corpse/SoulZombies.java");
        assertTrue(z.contains("fitGlobal(") && z.contains("CorpseConfig.zombieCap()") && z.contains("CorpseConfig.zombieGlobalCap()"), "the per-player and the server caps must stay");
        assertTrue(z.contains("addFreshEntity") && z.contains("ZOMBIES.add(") && z.contains("crumbleById"), "a summon is listed and the oldest is dismissed past the cap");
    }

    @Test
    void theSummonWiringIsInPlace() throws IOException {
        String nusmp = code("NUSMP.java");
        for (String needle : new String[]{"CorpseEntities.ENTITIES.register(modEventBus)", "modEventBus.addListener(com.newuniverse.nusmp.corpse.CorpseEntities::attributes)",
                "CorpseHooks.register()", "CorpseConfig.SPEC", "CorpseEffects.EFFECTS.register(modEventBus)"})
            assertTrue(nusmp.contains(needle), "NUSMP lost: " + needle);
        String ent = code("corpse/CorpseEntities.java");
        assertTrue(ent.contains("ENTITIES.register(\"soul_zombie\"") && ent.contains("e.put(SOUL_ZOMBIE.get(), SoulZombieEntity.createAttributes().build())"), "the soul zombie type or its attributes are not registered");
        assertTrue(code("client/corpse/CorpseClient.java").contains("registerEntityRenderer(CorpseEntities.SOUL_ZOMBIE.get()"), "the soul zombie has no renderer on the client");
        String cfg = code("corpse/CorpseConfig.java");
        assertTrue(Pattern.compile("define\\(\"zombiesInsteadOfPuppets\",\\s*true\\)").matcher(cfg).find(), "corpse.zombiesInsteadOfPuppets must default to true");
        assertTrue(cfg.contains("defineInRange(\"zombieCap\", 6") && cfg.contains("zombieGlobalCap") && cfg.contains("GLOBAL_CAP_DEFAULT"), "the caps 6 per player / 60 per server changed");
        assertEquals(6, SoulZombieRules.CAP_DEFAULT);
        assertEquals(60, SoulZombieRules.GLOBAL_CAP_DEFAULT);
        String hooks = code("corpse/CorpseHooks.java");
        assertTrue(hooks.contains("SoulCorpseCastSelfTests.register()") && hooks.contains("RadesUndeadSelfTests.register()"), "a self-test is not registered");
        assertTrue(hooks.contains("onLogout") && hooks.contains("Corpses.crumbleAllOf(p.getServer(), p.getUUID())"), "the logout cleanup is gone");
        String corpses = code("corpse/Corpses.java");
        assertTrue(corpses.contains("owner == null || !owner.isAlive() || owner.level() != level"), "the death / dimension cleanup of the sweep is gone");
    }

    @Test
    void theCastSelfTestGoesThroughTheRealCastPath() throws IOException {
        String t = code("corpse/SoulCorpseCastSelfTests.java");
        assertTrue(t.contains("\"magic.soul_corpse_cast\""), "the self-test key changed");
        assertTrue(t.contains("book.castFromStyle(inst, p,"), "the self-test must cast through GrimoireBook.castFromStyle (castPage: checks, mana, SafeCast, the wrappers, cooldown)");
        assertTrue(t.contains("MasteryPages.tryUnlock(") && t.contains("Corpses.openStarterPage(") && t.contains("GrimoireSummon.summon("), "the self-test must unlock and summon the grimoire like a player");
        for (String id : new String[]{"MARIONETTE", "NECROPOLIS", "LEGION", "REANIMATE", "DAVID", "MICHAEL"}) assertTrue(t.contains("SoulCorpseAccess." + id), "the self-test never casts " + id);
        String gb = code("book/GrimoireBook.java");
        assertTrue(gb.contains("public void castFromStyle(") && gb.contains("castPage(instance, player, mode)"), "castFromStyle (the self-test's door) is gone");
    }

    @Test
    void thePageTextsExist() throws IOException {
        String en = Files.readString(Path.of("src/main/resources/assets/nusmp/lang/en_us.json"), StandardCharsets.UTF_8);
        String fragment = Files.readString(Path.of("tools/lang/soul_corpse_fix.json"), StandardCharsets.UTF_8);
        String rades = Files.readString(Path.of("tools/lang/rades_undead.json"), StandardCharsets.UTF_8);
        for (String id : SoulCorpseAccess.ORDER) {
            String key = "\"nusmp.page.book_soul_corpse." + id + "\"";
            assertTrue(en.contains(key) || fragment.contains(key) || rades.contains(key), "no page text for " + id);
        }
    }
}
