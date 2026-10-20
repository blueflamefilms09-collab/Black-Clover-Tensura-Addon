package com.newuniverse.nusmp.balance;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: THE HUNGER BAN of the owner's spec ("absolutely no accelerated hunger or exhaustion mechanics": nothing may make the food bar drain faster or force the
 * player to eat more often; rests only recover health, mana and cooldowns). It is enforced by removing the writers, so this pure source scan keeps them removed:
 * it fails the build when any file under {@code src/main/java} (comments and strings aside where noted)
 * <ol>
 *   <li>calls {@code causeFoodExhaustion} or {@code addExhaustion} (no allowlist at all);</li>
 *   <li>calls {@code setFoodLevel}, {@code setSaturation} or {@code setExhaustion} outside the small justified allowlist below (feeding, restoring, lowering exhaustion,
 *       or repairing in the guard), with the allowlist pinned to a call count per file, and never with a subtraction inside a {@code setFoodLevel}/{@code setSaturation} argument;</li>
 *   <li>puts the Hunger effect on anything: a statement that names {@code MobEffects.HUNGER} or the id {@code minecraft:hunger} must be a cleanse, a detection or the
 *       central refusal; the Hunger effect may only ever be REMOVED;</li>
 *   <li>uses the config keys or helpers of the removed clocks ({@code HUNGER_DRAIN}, {@code hungerDrain}, {@code extraExhaustion}, {@code GloomClock.starving},
 *       {@code STARVING_FOOD}), in code or in the resources;</li>
 *   <li>hurts with the starve damage type outside the creature that has its own hunger meter, or feeds a negative amount;</li>
 *   <li>ships a damage type whose {@code exhaustion} is above zero (every hit it lands would drain food).</li>
 * </ol>
 * Server self-tests ({@code *SelfTests.java}) may set a fake player's food as a fixture (they never ship a mechanic), but they too may not call the two exhaustion adders.
 * See {@code docs/labyrinth/hunger_ban.md}.
 */
class NoHungerDrainTest {
    private static final Path MAIN = Path.of("src/main/java");
    private static final Path RES = Path.of("src/main/resources");

    /** file (suffix of the path under src/main/java) -> {setFoodLevel, setSaturation, setExhaustion} calls allowed there, and why each is restorative. */
    private static final Map<String, int[]> WRITERS = Map.ofEntries(
            Map.entry("balance/HungerBan.java", new int[]{1, 1, 1}),                       // the guard's repair: writes back values never worse than the armed reading
            Map.entry("dungeon/nexus/relic/ChaliceHandler.java", new int[]{1, 1, 0}),     // the Chalice's stew: raises the food level after a meal of raw meat
            Map.entry("skilltree/line/PassiveExecutors.java", new int[]{0, 0, 1}),        // the hunger-saver passive: LOWERS the exhaustion
            Map.entry("relics/wave/Wave7Events.java", new int[]{0, 0, 1}),                // the Sentinel's armour: keeps only half of the exhaustion gained (lowers it)
            Map.entry("relics/wave/Wave16Events.java", new int[]{0, 1, 0}),               // a relic that raises saturation
            Map.entry("relics/wave/Wave25Acts.java", new int[]{1, 1, 0}),                 // the Ring of Rock Eating: eats rocks to FILL the bar
            Map.entry("relics/wave/Wave30Vecna.java", new int[]{1, 1, 0}),                // the Wish: a full stomach
            Map.entry("relics/wave/Wave30Stones.java", new int[]{1, 0, 0}),               // the Greater Wish: a full stomach
            Map.entry("relics/wave/Wave18Events.java", new int[]{1, 0, 0}),               // a slow top-up of one point
            Map.entry("relics/wave/Wave17Events.java", new int[]{1, 0, 0}),               // the cupcake: a floor of 17, raises only
            Map.entry("relics/wave/Wave17Acts.java", new int[]{1, 1, 0}),                 // a meal: Math.max with the current value
            Map.entry("relics/wave/Wave3Belt.java", new int[]{1, 0, 0}),                  // a floor of 14, raises only
            Map.entry("relics/wave/Wave22Events.java", new int[]{1, 0, 0}),               // the lich's floor, raises only
            Map.entry("relics/wave/Wave5Safeguard.java", new int[]{1, 1, 0}));            // the restored record: Math.max with the record

    /** Statements that name the Hunger effect and are fine: cleanses, detections, the central refusal, the word table and the data of what a mirror may send back. */
    private static final Pattern CLEANSE = Pattern.compile("removeEffect|hasEffect|Util\\.remove\\(|\\.lift\\(|Wave6Kit\\.lift|TIME_EFFECTS|HARMFUL|FATIGUE|refuses|isHungerEffectId|HUNGER_EFFECT"
            + "|Map\\.entry\\(\"hunger\"|case \"poison\", \"wither\"");

    /** The one file that may TRY to apply Hunger (the in-game self-test {@code dungeon.nohunger} does it on purpose, to prove the refusal), and how many such statements it may hold. */
    private static final String PROBES_FILE = "labyrinth/mega/mechanics/MechanicsSelfTests.java";
    private static final int MAX_PROBES = 4;

    private static final Pattern STATEMENT_WITH_HUNGER = Pattern.compile("MobEffects\\.HUNGER|\"minecraft:hunger|'minecraft:hunger|[\"']hunger:\\d");
    private static final Pattern APPLY_PLAIN_HUNGER = Pattern.compile("(addEffect|give|giveQuiet|effect|MobEffectInstance)\\s*\\([^;]*\"hunger\"");

    @Test
    void noFileAddsFoodExhaustion() throws IOException {
        List<String> bad = new ArrayList<>();
        Pattern p = Pattern.compile("\\b(causeFoodExhaustion|addExhaustion)\\s*\\(");
        for (Path f : sources()) {
            Matcher m = p.matcher(code(f));
            while (m.find()) bad.add(rel(f) + ": " + m.group(1));
        }
        assertTrue(bad.isEmpty(), "the hunger ban: nothing may raise the food exhaustion: " + bad);
    }

    @Test
    void foodWritersAreOnlyTheJustifiedRestoringOnes() throws IOException {
        List<String> bad = new ArrayList<>();
        Pattern call = Pattern.compile("\\b(setFoodLevel|setSaturation|setExhaustion)\\s*\\(");
        for (Path f : sources()) {
            String r = rel(f);
            String src = code(f);
            int[] seen = new int[3];
            Matcher m = call.matcher(src);
            while (m.find()) {
                int idx = switch (m.group(1)) { case "setFoodLevel" -> 0; case "setSaturation" -> 1; default -> 2; };
                seen[idx]++;
                String arg = argument(src, m.end());
                if (idx < 2 && arg.contains(" - ")) bad.add(r + ": " + m.group(1) + "(" + arg.trim() + ") subtracts");
                if (idx < 2 && arg.matches("\\s*0(\\.0)?f?\\s*") && !r.endsWith("SelfTests.java")) bad.add(r + ": " + m.group(1) + "(" + arg.trim() + ") empties the bar");
            }
            if (r.endsWith("SelfTests.java")) continue;                                  // fixtures of the server self-tests (a fake player's food is set up there)
            int[] allowed = WRITERS.get(r);
            for (int i = 0; i < 3; i++) {
                int max = allowed == null ? 0 : allowed[i];
                if (seen[i] > max) bad.add(r + ": " + seen[i] + " call(s) of " + new String[]{"setFoodLevel", "setSaturation", "setExhaustion"}[i] + ", the allowlist says " + max);
            }
        }
        assertTrue(bad.isEmpty(), "the hunger ban: food writers outside the allowlist (add the file only if it RESTORES the bar, and say why in WRITERS): " + bad);
    }

    @Test
    void theHungerEffectIsNeverApplied() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path f : sources()) {
            String src = code(f);
            int probes = 0;
            for (String stmt : src.split(";")) {
                String s = stmt.replace('\n', ' ');
                boolean hit = (STATEMENT_WITH_HUNGER.matcher(s).find() && !CLEANSE.matcher(s).find()) || APPLY_PLAIN_HUNGER.matcher(s).find();
                if (!hit) continue;
                if (rel(f).equals(PROBES_FILE)) probes++;                         // the self-test dungeon.nohunger tries to apply Hunger to prove it is refused
                else bad.add(rel(f) + ": " + shorten(s));
            }
            if (rel(f).equals(PROBES_FILE) && probes > MAX_PROBES) bad.add(PROBES_FILE + ": " + probes + " probes, the self-test needs at most " + MAX_PROBES);
        }
        assertTrue(bad.isEmpty(), "the hunger ban: the Hunger effect may only be removed or refused, never applied: " + bad);
    }

    @Test
    void theRemovedClocksAreGone() throws IOException {
        List<String> bad = new ArrayList<>();
        Pattern p = Pattern.compile("\\b(HUNGER_DRAIN|hungerDrain|extraExhaustion|STARVING_FOOD|GloomClock\\.starving)\\b");
        for (Path f : sources()) {
            Matcher m = p.matcher(code(f));
            while (m.find()) bad.add(rel(f) + ": " + m.group(1));
        }
        try (Stream<Path> res = Files.walk(RES)) {
            for (Path f : (Iterable<Path>) res.filter(x -> x.toString().endsWith(".json") || x.toString().endsWith(".toml") || x.toString().endsWith(".cfg"))::iterator) {
                String text = Files.readString(f);
                if (text.contains("hungerDrain")) bad.add(f + ": hungerDrain");
            }
        }
        assertTrue(bad.isEmpty(), "the removed hunger clocks (and their config key) must not come back: " + bad);
    }

    @Test
    void noDamageTypeOfTheModAddsExhaustion() throws IOException {
        List<String> bad = new ArrayList<>();
        try (Stream<Path> res = Files.walk(RES.resolve("data"))) {
            for (Path f : (Iterable<Path>) res.filter(x -> x.toString().replace('\\', '/').contains("/damage_type/") && x.toString().endsWith(".json"))::iterator) {
                Matcher m = Pattern.compile("\"exhaustion\"\\s*:\\s*([0-9.eE+-]+)").matcher(Files.readString(f));
                if (m.find() && Double.parseDouble(m.group(1)) > 0.0) bad.add(f + ": exhaustion " + m.group(1));
            }
        }
        assertTrue(bad.isEmpty(), "the hunger ban: a damage type's exhaustion is food drained by every hit it lands: " + bad);
    }

    @Test
    void noStarveDamageOnPlayersAndNoNegativeMeals() throws IOException {
        List<String> bad = new ArrayList<>();
        Pattern starve = Pattern.compile("damageSources\\(\\)\\.starve\\(\\)|DamageTypes\\.STARVE");
        Pattern negative = Pattern.compile("\\.eat\\(\\s*-|\\bnutrition\\(\\s*-");
        for (Path f : sources()) {
            String r = rel(f);
            String src = code(f);
            // DungeonCreature has its own 0-100 hunger meter (a mob, not a player); the boss rules and the Warden only IGNORE starve damage
            if (starve.matcher(src).find() && !r.endsWith("dungeon/DungeonCreature.java") && !r.endsWith("boss/rules/BossEnvironment.java") && !r.endsWith("labyrinth/seal/FloorWardenEntity.java")) bad.add(r + ": starve damage");
            if (negative.matcher(src).find()) bad.add(r + ": a negative meal");
        }
        assertTrue(bad.isEmpty(), "the hunger ban: " + bad);
    }

    // ---------------------------------------------------------------- helpers
    private static List<Path> sources() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN)) {
            return files.filter(x -> x.toString().endsWith(".java")).sorted().toList();
        }
    }

    private static String rel(Path f) { return MAIN.relativize(f).toString().replace('\\', '/').replaceFirst("^com/newuniverse/nusmp/", ""); }

    private static String code(Path f) throws IOException { return stripComments(Files.readString(f)); }

    private static String shorten(String s) { String t = s.trim().replaceAll("\\s+", " "); return t.length() > 140 ? t.substring(0, 140) + "..." : t; }

    /** The text of the first argument after the opening parenthesis at {@code from} (up to the matching close). */
    private static String argument(String src, int from) {
        int depth = 1, i = from;
        for (; i < src.length() && depth > 0; i++) {
            char c = src.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
        }
        return src.substring(from, Math.max(from, i - 1));
    }

    /** Source without comments, keeping strings and char literals (and the line breaks, so a statement is still a statement). */
    static String stripComments(String s) {
        StringBuilder out = new StringBuilder(s.length());
        int n = s.length();
        for (int i = 0; i < n; ) {
            char c = s.charAt(i);
            if (c == '"' || c == '\'') {
                int j = i + 1;
                while (j < n && s.charAt(j) != c) j += s.charAt(j) == '\\' ? 2 : 1;
                out.append(s, i, Math.min(n, j + 1));
                i = j + 1;
            } else if (c == '/' && i + 1 < n && s.charAt(i + 1) == '/') {
                while (i < n && s.charAt(i) != '\n') i++;
            } else if (c == '/' && i + 1 < n && s.charAt(i + 1) == '*') {
                int e = s.indexOf("*/", i + 2);
                int stop = e < 0 ? n : e + 2;
                for (int k = i; k < stop; k++) if (s.charAt(k) == '\n') out.append('\n');
                i = stop;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }
}
