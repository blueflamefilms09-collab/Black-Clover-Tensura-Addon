package com.newuniverse.nusmp.entity.riven;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoModelData;
import com.newuniverse.nusmp.skill.codex.AnimeSkill;
import com.newuniverse.nusmp.skill.codex.SkillForge;
import com.newuniverse.nusmp.skill.codex.SkillSandbox;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Riven Remake's shipped data: every codex skill passes the sandbox, every clip the server can name exists, and both models parse. */
class RivenAssetsTest {
    private static final File ASSETS = new File("src/main/resources/assets/nusmp");
    private static final File CODEX = new File("src/main/resources/data/nusmp/skills/codex");

    private static JsonElement read(File f) throws Exception {
        try (Reader r = new FileReader(f)) { return JsonParser.parseReader(r); }
    }

    @Test
    void everyShippedSkillPassesTheSandbox() throws Exception {
        Set<String> ids = new HashSet<>();
        File[] files = CODEX.listFiles((d, n) -> n.endsWith(".json"));
        assertNotNull(files);
        assertTrue(files.length >= 7, "one file per franchise plus the bard kit and the generic pool");
        for (File f : files) {
            for (JsonElement e : read(f).getAsJsonObject().getAsJsonArray("skills")) {
                AnimeSkill s = AnimeSkill.parse(e.getAsJsonObject());
                assertNull(SkillSandbox.validate(s), f.getName() + " " + s.id());
                assertTrue(ids.add(s.id()), "duplicate " + s.id());
            }
        }
        for (String need : new String[]{"nusmp:eldritch_blast", "nusmp:hex", "nusmp:bardic_inspiration", "nusmp:shadow_step",
                "nusmp:story_manifestation", "nusmp:soul_bond", "nusmp:grimoire_manipulation", "nusmp:song_of_valor", "nusmp:final_chapter"})
            assertTrue(ids.contains(need), "first-party skill " + need);
    }

    @Test
    void theSandboxRejectsWhatItDoesNotKnow() {
        JsonObject o = new JsonObject();
        o.addProperty("id", "nusmp:evil");
        o.addProperty("name", "Evil");
        com.google.gson.JsonArray p = new com.google.gson.JsonArray();
        p.add("run_shell_command");
        o.add("primitives", p);
        assertNotNull(SkillSandbox.validate(AnimeSkill.parse(o)));
        p = new com.google.gson.JsonArray();
        p.add("projectile");
        o.add("primitives", p);
        o.addProperty("range", 500);
        assertNotNull(SkillSandbox.validate(AnimeSkill.parse(o)), "numbers are bounded too");
    }

    @Test
    void researchCompilesOnlyAllowListedPrimitives() {
        AnimeSkill s = SkillForge.compile("Naruto", "Rasengan Blast", "A spinning beam of energy that you slash with. Summon a clone and teleport.");
        assertNull(SkillSandbox.validate(s));
        assertTrue(s.primitives().contains("projectile") && s.primitives().contains("summon_construct") && s.primitives().contains("blink"));
    }

    @Test
    void everyClipExistsInTheAnimationFile() throws Exception {
        GeoAnim a = GeoAnim.parse(read(new File(ASSETS, "animations/entity/riven_remake.animation.json")).getAsJsonObject());
        for (String name : RivenClips.NAMES) assertNotNull(a.clip(name), "clip " + name);
        assertEquals(RivenClips.NAMES.length, RivenClips.TICKS.length);
        for (String anim : SkillSandbox.ANIMATIONS) assertNotNull(a.clip(anim), "skill animation " + anim);
        assertFalse(a.clip("idle").bones.isEmpty());
    }

    @Test
    void bothModelsParseAndTheFinalFormAddsWingsAndCrown() throws Exception {
        GeoModelData base = GeoModelData.parse("riven_remake", read(new File(ASSETS, "geo/entity/riven_remake.geo.json")).getAsJsonObject());
        GeoModelData fin = GeoModelData.parse("riven_remake_final", read(new File(ASSETS, "geo/entity/riven_remake_final.geo.json")).getAsJsonObject());
        for (String bone : new String[]{"head", "torso", "armR", "armL", "legR", "legL", "hair", "tailBack"}) assertTrue(base.bones.containsKey(bone), bone);
        assertFalse(base.bones.containsKey("wingL"));
        assertTrue(fin.bones.containsKey("wingL") && fin.bones.containsKey("wingR") && fin.bones.containsKey("crown"));
        assertTrue(fin.cubeCount() > base.cubeCount());
        for (String png : new String[]{"riven_remake", "riven_remake_glow", "riven_remake_final", "riven_remake_final_glow"})
            assertTrue(new File(ASSETS, "textures/entity/" + png + ".png").isFile(), png);
    }
}
