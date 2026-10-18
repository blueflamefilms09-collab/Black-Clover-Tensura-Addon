package com.newuniverse.nusmp.elite.nations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.elite.nations.core.NationText;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Why: every message key the code uses must have an English text, and the lang fragment must be exactly the fallback map (so the lead can merge it without surprises). */
class LangFragmentTest {
    private static final Pattern KEY = Pattern.compile("\"((?:nusmp\\.nation\\.|key\\.nusmp\\.nation)[A-Za-z0-9_.]*)\"");
    /** Families built at run time: prefix + an enum value; each member is listed in NationText. */
    private static final String[] DYNAMIC_PREFIXES = {"nusmp.nation.m.war_refused.", "nusmp.nation.m.ally_refused.", "nusmp.nation.m.pvp_", "nusmp.nation.perk.", "nusmp.nation.perm.", "nusmp.nation.tier.", "nusmp.nation.war.phase."};

    private static List<Path> sources() throws IOException {
        List<Path> out = new ArrayList<>();
        for (String dir : new String[]{"src/main/java/com/newuniverse/nusmp/elite/nations", "src/main/java/com/newuniverse/nusmp/client/elite/nations"}) {
            try (Stream<Path> s = Files.walk(Paths.get(dir))) { s.filter(p -> p.toString().endsWith(".java") && !p.getFileName().toString().equals("NationText.java")).forEach(out::add); }
        }
        return out;
    }

    @Test
    void everyKeyUsedInTheCodeHasAnEnglishText() throws IOException {
        int checked = 0;
        for (Path p : sources()) {
            Matcher m = KEY.matcher(Files.readString(p, StandardCharsets.UTF_8));
            while (m.find()) {
                String key = m.group(1);
                boolean dynamic = false;
                for (String prefix : DYNAMIC_PREFIXES) if (key.equals(prefix)) dynamic = true;
                if (dynamic) continue;
                assertTrue(NationText.has(key), p.getFileName() + " uses a key without a text: " + key);
                checked++;
            }
        }
        assertTrue(checked > 150, "found only " + checked + " keys");
    }

    @Test
    void dynamicFamiliesAreComplete() {
        for (String r : new String[]{"self", "unknown_nation", "allied", "already_at_war", "truce", "too_many_wars", "level_too_low", "declarer_cooldown"}) assertTrue(NationText.has("nusmp.nation.m.war_refused." + r), r);
        for (String r : new String[]{"self", "already_allied", "at_war", "ally_at_war", "limit_self", "limit_other", "cooldown", "duplicate", "no_proposal", "expired", "not_allied"}) assertTrue(NationText.has("nusmp.nation.m.ally_refused." + r), r);
        for (String r : new String[]{"deny_same_nation", "deny_allied", "deny_not_at_war"}) assertTrue(NationText.has("nusmp.nation.m.pvp_" + r), r);
        for (com.newuniverse.nusmp.elite.nations.core.Permission p : com.newuniverse.nusmp.elite.nations.core.Permission.values()) assertTrue(NationText.has("nusmp.nation.perm." + p.id()), p.id());
        for (com.newuniverse.nusmp.elite.nations.rep.ReputationTier t : com.newuniverse.nusmp.elite.nations.rep.ReputationTier.values()) assertTrue(NationText.has("nusmp.nation.tier." + t.id()), t.id());
        for (com.newuniverse.nusmp.elite.nations.war.WarPhase p : com.newuniverse.nusmp.elite.nations.war.WarPhase.values()) assertTrue(NationText.has("nusmp.nation.war.phase." + p.id()), p.id());
        for (String t : new String[]{"attribute", "shop_discount", "treasury_cap", "member_cap", "guard_count", "contract_bonus", "board_slots"}) {
            assertTrue(NationText.has("nusmp.nation.perk." + t), t);
            assertTrue(com.newuniverse.nusmp.elite.nations.core.PerkTable.knownType(t), t);
        }
        for (com.newuniverse.nusmp.elite.nations.war.AllianceBook.Result r : com.newuniverse.nusmp.elite.nations.war.AllianceBook.Result.values()) {
            if (r == com.newuniverse.nusmp.elite.nations.war.AllianceBook.Result.OK) continue;
            // NO_PROPOSAL .. NOT_ALLIED are all reachable through the service
            assertTrue(NationText.has("nusmp.nation.m.ally_refused." + r.name().toLowerCase(java.util.Locale.ROOT)), r.name());
        }
        for (com.newuniverse.nusmp.elite.nations.war.WarRules.Refusal r : com.newuniverse.nusmp.elite.nations.war.WarRules.Refusal.values()) {
            if (r == com.newuniverse.nusmp.elite.nations.war.WarRules.Refusal.NONE) continue;
            assertTrue(NationText.has("nusmp.nation.m.war_refused." + r.name().toLowerCase(java.util.Locale.ROOT)), r.name());
        }
    }

    @Test
    void theLangFragmentIsExactlyTheFallbackMap() throws IOException {
        JsonObject frag = JsonParser.parseString(Files.readString(Paths.get("tools/elite/nations_lang.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(NationText.all().size(), frag.size());
        for (var e : NationText.all().entrySet()) {
            assertTrue(frag.has(e.getKey()), "fragment lacks " + e.getKey());
            assertEquals(e.getValue(), frag.get(e.getKey()).getAsString(), e.getKey());
        }
    }

    @Test
    void placeholdersAreSane() {
        for (var e : NationText.all().entrySet()) {
            String v = e.getValue();
            assertTrue(!v.isBlank(), e.getKey());
            // a lone % must be doubled; %s and %n$s are the only specifiers
            String stripped = v.replace("%%", "").replaceAll("%\\d?\\$?s", "");
            assertTrue(!stripped.contains("%"), "bad format in " + e.getKey() + ": " + v);
        }
    }
}
