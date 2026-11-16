package com.newuniverse.nusmp.skill;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guide P1 #2 and #5 (docs/guide_fixes_p1.md): rebuilt-skill hits carry their skill instance into Tensura's createSource behind a SERVER switch (the vanilla source stays for calls without one),
 * and SpellRuntime restores its temporary blocks and empties its static lists on server stop, and drops bolts of casters that are gone. Pure (file reads); the working directory is the project root.
 */
class GuideP1SourceScanTest {
    private static final Path MAIN = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    @Test
    void skillDamageUsesTheTensuraSourceBehindTheServerSwitch() throws IOException {
        String d = read(MAIN.resolve("skill/SkillDamage.java"));
        assertTrue(d.contains("skill.createSource(instance, caster, kind == Kind.MAGIC ? TensuraDamageTypes.MAGIC_GENERIC : DamageTypes.PLAYER_ATTACK, 0)"), "the Tensura skill source");
        assertTrue(d.contains("NUConfig.TENSURA_SKILL_DAMAGE_SOURCE.get()"), "the switch is read");
        assertTrue(d.contains("indirectMagic(caster, caster)") && d.contains("playerAttack(caster)"), "the old vanilla sources stay");
        assertTrue(d.contains("BalanceLaw.damage(caster, target"), "the amount still goes through the balance law and its PvP cap");
        String cfg = read(MAIN.resolve("NUConfig.java"));
        assertTrue(cfg.contains("TENSURA_SKILL_DAMAGE_SOURCE = b.comment(") && cfg.contains(".define(\"tensuraSkillDamageSource\", true)"), "SERVER setting tensuraSkillDamageSource");
    }

    /** Every SkillDamage.hit call passes its skill instance last, except DbMoves (a static move table that is given no instance). */
    @Test
    void everyCallWithAnInstancePassesIt() throws IOException {
        List<String> bad = new ArrayList<>();
        int calls = 0;
        try (Stream<Path> files = Files.list(MAIN.resolve("skill"))) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String name = f.getFileName().toString();
                if (name.equals("SkillDamage.java") || name.equals("DbMoves.java")) continue;
                String s = read(f);
                for (int i = s.indexOf("SkillDamage.hit("); i >= 0; i = s.indexOf("SkillDamage.hit(", i + 1)) {
                    int k = i + "SkillDamage.hit(".length(), depth = 1;
                    while (depth > 0) { char c = s.charAt(k++); if (c == '(') depth++; else if (c == ')') depth--; }
                    String args = s.substring(i, k - 1);
                    calls++;
                    if (!(args.endsWith(", instance") || args.endsWith(", inst"))) bad.add(name + ": " + args);
                }
            }
        }
        assertTrue(calls >= 37, "the rebuilt skills' hits moved (" + calls + "); update this scan");
        assertEquals(List.of(), bad, "a SkillDamage.hit call lost its skill instance (Tensura source)");
    }

    @Test
    void spellRuntimeCleansUpOnStopAndDropsGoneCasters() throws IOException {
        String r = read(MAIN.resolve("book/SpellRuntime.java"));
        assertTrue(r.contains("public static void onServerStopping(ServerStoppingEvent event)"), "the stop hook");
        assertTrue(r.contains("for (Temp t : new ArrayList<>(TEMP)) SafeCast.tick(\"spell-temp-restore\", () -> restore(t));"), "pending temp blocks are taken back on stop");
        for (String l : new String[] {"BOLTS.clear();", "ZONES.clear();", "LATER.clear();", "TEMP.clear();"}) assertTrue(r.contains(l), l + " on stop");
        assertTrue(r.contains("owner.isRemoved() || owner.hasDisconnected() || owner.level() != level"), "a gone caster");
        assertTrue(r.contains("if (gone(b.owner, b.level)) { b.dead = true; return; }"), "bolts of gone casters fizzle");
        assertTrue(read(MAIN.resolve("NUSMP.java")).contains("SpellRuntime::onServerStopping"), "the stop hook is registered");
        assertTrue(read(MAIN.resolve("NUSMP.java")).contains("GuideP1SelfTests.register()"), "the self-tests are registered");
        assertTrue(read(Path.of(".github/workflows/smoke.yml")).contains(",guide.p1,"), "guide.p1 is on the smoke list");
    }
}
