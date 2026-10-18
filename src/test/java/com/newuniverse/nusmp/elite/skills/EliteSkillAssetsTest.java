package com.newuniverse.nusmp.elite.skills;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the elite skills promise things that are easy to break by accident and invisible until someone opens the skill list: every skill has its own 32x32 icon (no two alike, none like an older
 * skill's), every name, description, mode and message is in the lang fragment, the ids in the registry are the ids the skill texts use and none clashes with an older skill, only Rukh and the mod's own
 * effect shapes are used (no vanilla particle, no Time Magic effect), the wiring is exactly one line in this subsystem's own section of EliteSystem, and the doc exists. It reads sources and assets as files, so it needs no
 * Minecraft classes.
 */
class EliteSkillAssetsTest {
    private static final String SRC = "src/main/java/com/newuniverse/nusmp/elite/skills/";

    private static String read(String path) throws Exception { return Files.readString(Path.of(path), StandardCharsets.UTF_8); }

    private static List<String> registeredIds() throws Exception {
        Matcher m = Pattern.compile("SKILLS\\.register\\(\"([a-z_]+)\"").matcher(read(SRC + "EliteSkills.java"));
        List<String> ids = new ArrayList<>();
        while (m.find()) ids.add(m.group(1));
        return ids;
    }

    private static List<File> javaFiles() throws Exception {
        try (Stream<Path> s = Files.walk(Path.of("src/main/java/com/newuniverse/nusmp/elite/skills"))) {
            return s.filter(p -> p.toString().endsWith(".java")).map(Path::toFile).toList();
        }
    }

    @Test
    void theTwentyOneSkillsAreRegisteredWithTheInventoryNames() throws Exception {
        List<String> ids = registeredIds();
        assertEquals(21, ids.size(), "21 skills: " + ids);
        assertEquals(21, new HashSet<>(ids).size(), "no id twice");
        for (String id : List.of("gravity_well", "kamehameha", "static_chain", "ancient_soul", "iron_resolve", "meditation", "runesmiths_insight", "scavenger", "kaioken",
                "mechanical_eye", "mechanical_hands", "crocodile_skin", "hecate", "void_edge", "basilisk_skin", "beelzebuth", "gaia", "hephaestus_ultimate", "ouroboros", "rapheal", "void_sovereign"))
            assertTrue(ids.contains(id), "missing " + id);
        String older = read("src/main/java/com/newuniverse/nusmp/skill/NUSkills.java");
        for (String id : ids) assertFalse(older.contains("register(\"" + id + "\""), id + " clashes with an older skill id");
    }

    @Test
    void everyClassUsesItsRegisteredIdInItsText() throws Exception {
        String reg = read(SRC + "EliteSkills.java");
        Matcher m = Pattern.compile("SKILLS\\.register\\(\"([a-z_]+)\", ([A-Za-z]+)::new\\)").matcher(reg);
        int n = 0;
        while (m.find()) {
            String id = m.group(1), cls = m.group(2);
            String src = read(SRC + cls + ".java");
            assertTrue(src.contains("SkillText.of(\"" + id + "\""), cls + " must declare SkillText.of(\"" + id + "\", ...)");
            n++;
        }
        assertEquals(21, n);
    }

    @Test
    void everySkillHasItsOwn32pxIconDifferentFromEveryOtherSkill() throws Exception {
        Map<String, String> seen = new HashMap<>();
        File dir = new File("src/main/resources/assets/nusmp/textures/skill");
        // the older icons first, so a clash with them is reported against the elite one
        for (File f : dir.listFiles((d, name) -> name.endsWith(".png"))) {
            BufferedImage img = ImageIO.read(f);
            if (img == null || img.getWidth() != 32 || img.getHeight() != 32) continue;
            seen.putIfAbsent(Arrays.toString(img.getRGB(0, 0, 32, 32, null, 0, 32)), f.getName().replace(".png", ""));
        }
        for (String id : registeredIds()) {
            File f = new File(dir, id + ".png");
            assertTrue(f.isFile(), "icon missing: " + f);
            BufferedImage img = ImageIO.read(f);
            assertNotNull(img, "icon unreadable: " + id);
            assertEquals(32, img.getWidth(), id + " width");
            assertEquals(32, img.getHeight(), id + " height");
            int[] px = img.getRGB(0, 0, 32, 32, null, 0, 32);
            String clash = seen.get(Arrays.toString(px));
            assertTrue(clash == null || clash.equals(id), id + " has the same icon as " + clash);
            long opaque = Arrays.stream(px).filter(p -> (p >>> 24) > 200).count();
            assertTrue(opaque > 32 * 32 * 0.8, id + " icon should fill its frame");
        }
    }

    @Test
    void everyNameDescriptionModeAndMessageIsInTheLangFragment() throws Exception {
        JsonObject frag = JsonParser.parseString(read("tools/elite/skills_lang.json")).getAsJsonObject();
        for (String id : registeredIds()) {
            assertTrue(frag.has("nusmp.skill." + id) && !frag.get("nusmp.skill." + id).getAsString().isBlank(), "name missing for " + id);
            assertTrue(frag.has("nusmp.skill." + id + ".description") && frag.get("nusmp.skill." + id + ".description").getAsString().length() > 30, "description missing for " + id);
        }
        Pattern mode = Pattern.compile("EliteMode\\.of\\(\"([^\"]+)\"");
        Pattern msg = Pattern.compile("SkillMsg\\.t\\(\"([^\"]+)\"");
        int modes = 0, msgs = 0;
        for (File f : javaFiles()) {
            String src = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            Matcher a = mode.matcher(src);
            while (a.find()) { assertTrue(frag.has("nusmp.skill.mode." + a.group(1)), "mode key missing: " + a.group(1) + " in " + f.getName()); modes++; }
            Matcher b = msg.matcher(src);
            while (b.find()) { assertTrue(frag.has("nusmp.skill.msg." + b.group(1)), "message key missing: " + b.group(1) + " in " + f.getName()); msgs++; }
        }
        assertTrue(modes >= 40, "the skills declare their modes with EliteMode.of: " + modes);
        assertTrue(msgs >= 80, "the skills speak through SkillMsg.t: " + msgs);
    }

    @Test
    void onlyRukhAndTheModsOwnShapesAreUsedAndTimeMagicIsUntouched() throws Exception {
        Pattern timeShape = Pattern.compile("VfxShape\\.TIME_");
        for (File f : javaFiles()) {
            String src = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            assertFalse(src.contains("ParticleTypes"), f.getName() + " uses a vanilla particle");
            assertFalse(src.contains("sendParticles"), f.getName() + " sends a vanilla particle");
            assertFalse(timeShape.matcher(src).find(), f.getName() + " uses a Time Magic effect");
            assertFalse(src.contains("TimeMagicLayer") || src.contains("TimeBook"), f.getName() + " touches the Time Magic effects");
        }
    }

    @Test
    void theWiringIsOneLineInTheSkillsSectionAndTheOtherSectionsAreUntouched() throws Exception {
        String sys = read("src/main/java/com/newuniverse/nusmp/elite/EliteSystem.java");
        int line = sys.indexOf("elite.skills.EliteSkills.init(modBus, container);");
        assertTrue(line > 0, "the wiring line is missing");
        assertEquals(line, sys.lastIndexOf("elite.skills.EliteSkills.init"), "wired once");
        int section = sys.indexOf("// ---- skills:"), next = sys.indexOf("// ---- races:");
        assertTrue(section < line && line < next, "the line must sit in the skills section");
        assertEquals(1, sys.split("EliteSkills\\.init", -1).length - 1);
    }

    @Test
    void theDocAndTheSharedFilesThisJobMustNotTouchAreInOrder() throws Exception {
        assertTrue(new File("docs/elite/skills.md").isFile(), "docs/elite/skills.md must exist");
        String doc = read("docs/elite/skills.md");
        for (String id : registeredIds()) assertTrue(doc.contains(id), "the doc does not mention " + id);
        for (String word : List.of("NEW", "ALREADY EXISTS", "REPLACEMENT", "source was not available")) assertTrue(doc.contains(word), "the doc lacks '" + word + "'");
        // 1.86.0: the lead merged tools/elite/skills_lang.json into en_us.json, so every registered skill must have its name there
        String en = read("src/main/resources/assets/nusmp/lang/en_us.json");
        Set<String> unmerged = new HashSet<>();
        for (String id : registeredIds()) if (!en.contains("\"nusmp.skill." + id + "\"")) unmerged.add(id);
        assertTrue(unmerged.isEmpty(), "en_us.json lacks the elite skill names (merge tools/elite/skills_lang.json): " + unmerged);
    }
}
