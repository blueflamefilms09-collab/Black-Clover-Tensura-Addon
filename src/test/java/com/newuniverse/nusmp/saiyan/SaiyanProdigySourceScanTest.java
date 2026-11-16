package com.newuniverse.nusmp.saiyan;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reads the source of the Saiyan Prodigy (docs/saiyan_prodigy.md) for the rules that keep it multiplayer safe and cheap: server config only, no client class, no hunger, no static player, no packet of its own
 * (so nothing to validate), the mixin is listed, every message and mode key is in the language fragment, and the old Dragon Ball removal pins still hold. Nothing here can run Minecraft.
 */
class SaiyanProdigySourceScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path MAIN = ROOT.resolve("src/main/java/com/newuniverse/nusmp");
    private static final List<String> MINE = List.of("saiyan/SaiyanProdigyConfig.java", "saiyan/SaiyanProdigyHub.java", "saiyan/SaiyanProdigyModes.java", "saiyan/SaiyanBlood.java", "saiyan/SaiyanBloodRules.java",
            "saiyan/SaiyanProdigySelfTests.java", "skill/SaiyanProdigySkill.java", "skill/ProdigySkills.java", "skill/modes/ModeSpec.java", "skill/modes/ModeBundle.java", "skill/modes/MultiModeSkill.java",
            "mixin/HumanRaceEvolutionMixin.java");

    private static String read(String rel) throws IOException { return Files.readString(MAIN.resolve(rel), StandardCharsets.UTF_8); }

    private static JsonObject fragment() throws IOException { return JsonParser.parseString(Files.readString(ROOT.resolve("tools/lang/prodigy.json"), StandardCharsets.UTF_8)).getAsJsonObject(); }

    @Test
    void noClientClassNoHungerNoStaticPlayer() throws IOException {
        Pattern client = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(");
        Pattern hunger = Pattern.compile("FoodData|setFoodLevel|getFoodData|addExhaustion|MobEffects\\.HUNGER|MobEffects\\.SATURATION|FOOD_LEVEL");
        Pattern staticPlayer = Pattern.compile("static\\s+(?:final\\s+|volatile\\s+)*(?:ServerPlayer|Player|FakePlayer)\\s+\\w+");
        Pattern world = Pattern.compile("addFreshEntity|\\.setBlock\\(|setBlockAndUpdate|destroyBlock|getChunk\\(|forceChunk|loadChunk|\\.explode\\(|Explosion|LightningBolt");
        for (String f : MINE) {
            String c = read(f);
            assertFalse(client.matcher(c).find(), f + " names a client class");
            assertFalse(hunger.matcher(c).find(), f + " touches hunger");
            assertFalse(staticPlayer.matcher(c).find(), f + " has a static player");
            assertFalse(world.matcher(c).find(), f + " places a block or spawns an entity (the Prodigy only asks the old, bounded systems)");
        }
    }

    @Test
    void thePureFilesStayPure() throws IOException {
        for (String f : List.of("saiyan/SaiyanBloodRules.java", "saiyan/SaiyanProdigyModes.java", "skill/modes/ModeSpec.java", "skill/modes/ModeBundle.java")) {
            assertFalse(Pattern.compile("(?m)^import (net\\.minecraft|net\\.neoforged|io\\.github|com\\.mojang)").matcher(read(f)).find(), f + " must stay pure (it is unit tested without a game)");
        }
    }

    @Test
    void everySettingIsAServerSettingAndHasItsOwnFile() throws IOException {
        String boot = read("saiyan/SaiyanBoot.java");
        assertTrue(boot.contains("container.registerConfig(ModConfig.Type.SERVER, SaiyanProdigyConfig.SPEC, \"nusmp-saiyan-prodigy-server.toml\")"), "a server config with a file name of its own");
        assertFalse(read("saiyan/SaiyanProdigyConfig.java").contains("ModConfig.Type.CLIENT"), "no gameplay setting is a client setting");
        assertTrue(boot.contains("SaiyanProdigySelfTests.register()") && boot.contains("SaiyanBlood.register(NeoForge.EVENT_BUS)") && boot.contains("ProdigySkills.init()"), "the self-tests, the deeds and the skill are wired");
    }

    @Test
    void theHubHasASwitchThatRestoresTheOldBehaviour() throws IOException {
        String hub = read("saiyan/SaiyanProdigyHub.java");
        assertTrue(hub.contains("public static boolean enabled() { return SaiyanProdigyConfig.hub(); }"));
        for (String m : List.of("public static void act(", "public static boolean grant(", "public static void onLogin(", "public static void onSaiyan(", "static void second("))
            assertTrue(hub.contains(m), m);
        assertTrue(hub.contains("if (!enabled()"), "the entry points check the switch");
        String blood = read("saiyan/SaiyanBlood.java");
        assertTrue(blood.contains("SaiyanProdigyConfig.bloodEvolution()"), "the evolution has its switch");
        String skill = read("skill/SaiyanProdigySkill.java");
        assertTrue(skill.contains("SaiyanProdigyHub.enabled()"));
        assertFalse(skill.contains("PlayerEvent") || skill.contains("PacketDistributor"), "the skill sends no packet and listens to no event of its own: the old, validated entry does the work");
    }

    @Test
    void theOldSkillsAreKeptAndForwarded() throws IOException {
        String skills = read("skill/DbSkills.java");
        assertTrue(skills.contains("legacy()) return;"), "the old removal pin stays");
        assertTrue(skills.contains("SaiyanProdigyHub.enabled()) return;"), "a hub-on server no longer takes the old ids from a player");
        assertTrue(skills.contains("public static List<DeferredHolder<ManasSkill, DbSkill>> allHolders()"));
        String skill = read("skill/DbSkill.java");
        assertTrue(skill.contains("SaiyanProdigyHub.forwarded(p)"), "the old skill points at the Prodigy");
        assertTrue(read("skill/NUSkills.java").contains("SKILLS.register(\"ki\", KiSkill::new)"), "the Ki skill is untouched");
    }

    @Test
    void theMixinIsListedAndOptional() throws IOException {
        String json = Files.readString(ROOT.resolve("src/main/resources/anime_multiverse.mixins.json"), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"HumanRaceEvolutionMixin\""));
        String m = read("mixin/HumanRaceEvolutionMixin.java");
        assertTrue(m.contains("require = 0") && m.contains("@Pseudo"), "a Tensura change must never stop the game");
        assertTrue(json.contains("\"required\": false"));
    }

    @Test
    void everyKeyTheCodeUsesIsInTheFragment() throws IOException {
        JsonObject lang = fragment();
        Pattern msg = Pattern.compile("SkillMsg\\.t\\(\"([a-z_.]+)\"");
        for (String f : List.of("skill/SaiyanProdigySkill.java", "skill/modes/MultiModeSkill.java")) {
            Matcher m = msg.matcher(read(f));
            while (m.find()) assertTrue(lang.has("anime_multiverse.skill.msg." + m.group(1)), "the fragment lacks the message " + m.group(1));
        }
        Matcher modes = Pattern.compile("ModeSpec\\.of\\(\"([a-z_]+)\"").matcher(read("saiyan/SaiyanProdigyModes.java"));
        int n = 0;
        while (modes.find()) { n++; assertTrue(lang.has("anime_multiverse.skill.mode.saiyan_prodigy." + modes.group(1)), "the fragment lacks the mode " + modes.group(1)); }
        assertTrue(n == SaiyanProdigyModes.BUNDLE.size());
        Matcher arts = Pattern.compile("new Art\\(\"([a-z_]+)\"").matcher(read("saiyan/SaiyanProdigyModes.java"));
        while (arts.find()) assertTrue(lang.has("anime_multiverse.skill.art." + arts.group(1)), "the fragment lacks the art " + arts.group(1));
        for (String k : List.of("anime_multiverse.skill.saiyan_prodigy", "anime_multiverse.skill.saiyan_prodigy.description", "anime_multiverse.saiyan.prodigy.granted", "anime_multiverse.saiyan.prodigy.forwarded",
                "anime_multiverse.saiyan.blood.kills", "anime_multiverse.saiyan.blood.near"))
            assertTrue(lang.has(k), k);
        assertTrue(Files.exists(ROOT.resolve("src/main/resources/assets/anime_multiverse/textures/skill/saiyan_prodigy.png")), "the skill has its icon");
    }
}
