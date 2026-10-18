package com.newuniverse.nusmp.skilltree.ft.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.ft.ice.IceRoute;
import com.newuniverse.nusmp.skilltree.ft.sky.SkyRoute;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Why: the core's in-game test {@code tree.core.routes} FAILS when a node's {@code skill} names a skill whose {@code routeId()/nodeId()} are another node, when a registered tree skill is claimed by no
 * node, and the cast gate would silently refuse such a technique forever, yet the classes that spell this agreement (the register, the constant holder, the constructor of each skill) import Minecraft
 * and cannot be loaded by a pure test. So this test reads their SOURCE text, the three places where the agreement is written ({@code SKILLS.register(Route.SKILL_X, XSkill::new)} in the register,
 * {@code NAME = "text"} in the holder, {@code super(Route.ID, Route.NODE, ...)} in the skill), resolves the constants, and asks the very questions the in-game test asks of the loaded game: each
 * registered skill claims a node of the shipped file, that node names exactly this skill, the route id is the file's, and every skill node of the file is claimed by exactly one registered class.
 * It is a text scan, so it relies on the three spellings above (a skill written another way is reported as "not understood", never passed silently), and the last test proves the check can fail by
 * feeding it doctored text. The real registry check stays the in-game test's; this one only moves the commonest mistake (a swapped or misspelt id) from the server start-up to the build.
 */
class RegisterWiringTest {
    private static final Path JAVA = Path.of("src/main/java/com/newuniverse/nusmp/skilltree/ft");

    private static final Pattern REGISTER = Pattern.compile("SKILLS\\.register\\(\\s*(\\w+)\\.(\\w+)\\s*,\\s*(\\w+)::new\\s*\\)");
    private static final Pattern CONSTANT = Pattern.compile("\\b([A-Z][A-Z0-9_]*)\\s*=\\s*\"([^\"]*)\"");
    private static final Pattern SUPER = Pattern.compile("super\\(\\s*(\\w+)\\.(\\w+)\\s*,\\s*(\\w+)\\.(\\w+)\\s*,");

    private static String read(String relative) throws Exception { return Files.readString(JAVA.resolve(relative)); }

    /** The string constants of a holder class: name to text. */
    static Map<String, String> constants(String holderSource) {
        Map<String, String> out = new HashMap<>();
        Matcher m = CONSTANT.matcher(holderSource);
        while (m.find()) out.put(m.group(1), m.group(2));
        return out;
    }

    /**
     * Every disagreement between a register, its skills, the constant holder and the route file, as sentences (empty when all agree).
     *
     * @param registerSource the text of the register class
     * @param holderName     the simple name of the constant holder ({@code SkyRoute})
     * @param holderSource   the text of the holder
     * @param skillSource    the text of a skill class by its simple name (null when there is none)
     * @param route          the shipped route
     * @param namespace      the namespace the register uses ({@code nusmp})
     */
    static List<String> problems(String registerSource, String holderName, String holderSource, Function<String, String> skillSource, SkillRoute route, String namespace) {
        List<String> bad = new ArrayList<>();
        Map<String, String> consts = constants(holderSource);
        Map<String, String> claimedBy = new HashMap<>();
        int registered = 0;
        Matcher reg = REGISTER.matcher(registerSource);
        while (reg.find()) {
            registered++;
            String idConst = reg.group(2), skillClass = reg.group(3);
            if (!holderName.equals(reg.group(1))) {
                bad.add(skillClass + " is registered with an id from " + reg.group(1) + ", not " + holderName);
                continue;
            }
            String path = consts.get(idConst);
            if (path == null) {
                bad.add(holderName + "." + idConst + " is not a text constant of " + holderName);
                continue;
            }
            String registeredId = namespace + ":" + path;
            String source = skillSource.apply(skillClass);
            if (source == null) {
                bad.add(skillClass + " has no source file");
                continue;
            }
            Matcher sup = SUPER.matcher(source);
            if (!sup.find()) {
                bad.add(skillClass + ": no super(" + holderName + ".ID, " + holderName + ".NODE, ...) call was understood");
                continue;
            }
            String routeConst = consts.get(sup.group(2)), nodeConst = consts.get(sup.group(4));
            if (!holderName.equals(sup.group(1)) || !holderName.equals(sup.group(3))) bad.add(skillClass + " takes its route or node from another holder than " + holderName);
            if (routeConst == null || nodeConst == null) {
                bad.add(skillClass + ": " + sup.group(2) + " or " + sup.group(4) + " is not a text constant of " + holderName);
                continue;
            }
            if (!route.id().equals(routeConst)) bad.add(skillClass + " says it belongs to route " + routeConst + ", the file is " + route.id());
            SkillNode node = route.node(nodeConst);
            if (node == null) {
                bad.add(skillClass + " claims node '" + nodeConst + "', which the file does not have");
                continue;
            }
            if (!node.grantsSkill()) bad.add(skillClass + " claims node '" + nodeConst + "', which grants no skill in the file");
            else if (!node.skill().equals(registeredId)) bad.add("node '" + nodeConst + "' grants " + node.skill() + " but " + skillClass + " is registered as " + registeredId);
            String other = claimedBy.put(nodeConst, skillClass);
            if (other != null) bad.add("node '" + nodeConst + "' is claimed by both " + other + " and " + skillClass);
        }
        if (registered == 0) bad.add("no SKILLS.register(...) line was understood in the register");
        Set<String> skillNodes = new HashSet<>();
        for (SkillNode n : route.nodes()) if (n.grantsSkill()) skillNodes.add(n.id());
        for (String id : skillNodes) if (!claimedBy.containsKey(id)) bad.add("the node '" + id + "' grants a skill that no registered class claims");
        return bad;
    }

    private static List<String> skyProblems() throws Exception {
        return problems(read("SkyDragonSkills.java"), "SkyRoute", read("sky/SkyRoute.java"), cls -> source("sky/" + cls + ".java"), RouteFiles.load("sky_dragon_slayer.json"), "nusmp");
    }

    private static List<String> iceProblems() throws Exception {
        return problems(read("IceMakeSkills.java"), "IceRoute", read("ice/IceRoute.java"), cls -> source("ice/" + cls + ".java"), RouteFiles.load("ice_make.json"), "nusmp");
    }

    private static String source(String relative) {
        try {
            return read(relative);
        } catch (Exception missing) {
            return null;
        }
    }

    @Test
    void theSkyRegisterTheSkillsAndTheFileAgree() throws Exception {
        assertEquals(List.of(), skyProblems());
    }

    @Test
    void theIceRegisterTheSkillsAndTheFileAgree() throws Exception {
        assertEquals(List.of(), iceProblems());
    }

    @Test
    void theHoldersSpellTheRouteIdsOfTheFiles() throws Exception {
        assertEquals(RouteFiles.load("sky_dragon_slayer.json").id(), SkyRoute.ID);
        assertEquals(RouteFiles.load("ice_make.json").id(), IceRoute.ID);
        Map<String, String> sky = constants(read("sky/SkyRoute.java"));
        assertEquals(SkyRoute.ID, sky.get("ID"), "the scan reads the same text the compiler does");
        assertEquals(SkyRoute.SKILL_TROIA, sky.get("SKILL_TROIA"));
    }

    @Test
    void theCheckCanFail() throws Exception {
        SkillRoute sky = RouteFiles.load("sky_dragon_slayer.json");
        String register = read("SkyDragonSkills.java"), holder = read("sky/SkyRoute.java");
        Function<String, String> skills = cls -> source("sky/" + cls + ".java");

        // 1. two ids swapped in the register: both skills now sit on each other's node
        String swapped = register.replace("SkyRoute.SKILL_TROIA, SkyTroiaSkill::new", "SkyRoute.SKILL_ENCHANT, SkyTroiaSkill::new").replace("SkyRoute.SKILL_ENCHANT, SkyEnchantSkill::new", "SkyRoute.SKILL_TROIA, SkyEnchantSkill::new");
        assertFalse(swapped.equals(register), "the doctored text must differ");
        assertFalse(problems(swapped, "SkyRoute", holder, skills, sky, "nusmp").isEmpty(), "swapped skill ids must be reported");

        // 2. a skill that claims a node the file does not have
        Function<String, String> wrongNode = cls -> cls.equals("SkyRoarSkill") ? skills.apply(cls).replace("SkyRoute.ROAR", "SkyRoute.CUSHION") : skills.apply(cls);
        List<String> bad = problems(register, "SkyRoute", holder, wrongNode, sky, "nusmp");
        assertFalse(bad.isEmpty(), "a skill on the wrong node must be reported");
        boolean named = false;
        for (String s : bad) named |= s.contains("SkyRoarSkill") || s.contains("'cushion'");
        assertTrue(named, "the report names the class or the node: " + bad);

        // 3. a skill left out of the register: the node it should serve is claimed by nobody
        String shorter = register.replaceAll("(?m)^.*SkyRoute\\.SKILL_DRILL.*$", "");
        bad = problems(shorter, "SkyRoute", holder, skills, sky, "nusmp");
        boolean unclaimed = false;
        for (String s : bad) unclaimed |= s.contains("sky_drill") && s.contains("no registered class");
        assertTrue(unclaimed, "an unregistered skill leaves its node unclaimed: " + bad);

        // 4. text the scan cannot read is reported and never passes silently
        assertFalse(problems("nothing to see here", "SkyRoute", holder, skills, sky, "nusmp").isEmpty());
    }
}
