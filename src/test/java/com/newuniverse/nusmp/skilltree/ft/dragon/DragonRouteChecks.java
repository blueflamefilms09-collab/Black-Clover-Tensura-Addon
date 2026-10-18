package com.newuniverse.nusmp.skilltree.ft.dragon;

import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.ft.dragon.DragonBudget;
import com.newuniverse.nusmp.skilltree.ft.dragon.DragonTables;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Why: the two Dragon Slayer route tests ask the same questions of different files, so the questions are written once: does the file obey the schema of design.md section 2
 * (the rules the core's loader will enforce, so a route that the loader would skip is caught here), does it obey this job's shape rules (a root, branches, a Secret Art that
 * needs two branches, the price bands), does it say the same numbers as the code table and does that table obey the budget, and does every number the skill classes read
 * exist (and every number in a table get read), so that a typo between the code and the data cannot survive a build. The last check is a source scan on purpose: it is the only
 * way to prove "the code reads keys that exist" without running the game.
 */
final class DragonRouteChecks {
    private DragonRouteChecks() {}

    private static final Pattern SNAKE = Pattern.compile("[a-z][a-z0-9_]*");
    private static final Pattern RL = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    /** The schema of design.md section 2 plus this job's shape rules. */
    static List<String> structure(SkillRoute r, String colorText) {
        List<String> bad = new ArrayList<>();
        if (!"fairy_tail".equals(r.magic())) bad.add("magic is not fairy_tail");
        if (!RL.matcher(r.book()).matches()) bad.add("book '" + r.book() + "' is not a lower-case resource location");
        if (!SNAKE.matcher(r.classId()).matches()) bad.add("class_id '" + r.classId() + "' is not lower snake");
        if (!colorText.matches("#[0-9A-Fa-f]{6}")) bad.add("color '" + colorText + "' is not #RRGGBB");
        if (!RL.matcher(r.icon()).matches()) bad.add("icon '" + r.icon() + "' is not a resource location");
        if (r.nodes().size() < 8 || r.nodes().size() > 10) bad.add(r.nodes().size() + " nodes, the brief asks for 8 to 10");
        Map<String, Integer> index = new HashMap<>();
        Set<String> cells = new HashSet<>();
        int secretArts = 0;
        for (int i = 0; i < r.nodes().size(); i++) {
            SkillNode n = r.nodes().get(i);
            String at = "node " + n.id();
            if (!SNAKE.matcher(n.id()).matches()) bad.add(at + ": id is not lower snake");
            if (index.put(n.id(), i) != null) bad.add(at + ": id used twice");
            if (n.name().isBlank() || n.desc().isBlank()) bad.add(at + ": name or desc is empty");
            if (n.cost() < 1 || n.cost() > 50) bad.add(at + ": cost " + n.cost() + " is outside 1..50");
            if (n.maxLevel() < 1 || n.maxLevel() > 5) bad.add(at + ": max_level " + n.maxLevel() + " is outside the brief's 1..5");
            if (n.costs().size() > n.maxLevel()) bad.add(at + ": more costs than levels");
            if (n.x() < 0 || n.x() > 12 || n.y() < 0 || n.y() > 9) bad.add(at + ": position " + n.x() + "," + n.y() + " is off the 13 x 10 grid");
            if (!cells.add(n.x() + "," + n.y())) bad.add(at + ": two nodes on the cell " + n.x() + "," + n.y());
            if (!RL.matcher(n.icon()).matches()) bad.add(at + ": icon '" + n.icon() + "' is not a resource location");
            if (n.grantsSkill() == !n.passives().isEmpty()) bad.add(at + ": a node either grants a skill or names passives, never both or neither");
            if (n.grantsSkill() && !n.skill().startsWith("nusmp:ft_")) bad.add(at + ": skill '" + n.skill() + "' does not start with nusmp:ft_");
            boolean secret = n.parents().size() >= 2;
            if (secret) secretArts++;
            int hi = 0;
            for (int l = 1; l <= n.maxLevel(); l++) {
                int c = n.costOf(l);
                hi = Math.max(hi, c);
                if (secret ? (c < 4 || c > 8) : (c < 1 || c > 3)) bad.add(at + ": level " + l + " costs " + c + " SP, outside the " + (secret ? "4..8 of a Secret Art" : "1..3 of an ordinary node"));
            }
            for (SkillNode.Parent p : n.parents()) {
                Integer pi = index.get(p.node());
                if (pi == null || pi >= i) { bad.add(at + ": parent '" + p.node() + "' does not come before it in the file"); continue; }
                SkillNode pn = r.nodes().get(pi);
                if (p.level() < 1 || p.level() > pn.maxLevel()) bad.add(at + ": parent '" + p.node() + "' level " + p.level() + " is out of 1.." + pn.maxLevel());
            }
        }
        if (r.nodes().stream().filter(SkillNode::isRoot).count() != 1) bad.add("the route must have exactly one root");
        if (secretArts != 1) bad.add(secretArts + " nodes with two parents, expected exactly one Secret Art");
        if (r.nodes().size() > 40) bad.add("more than 40 nodes");
        return bad;
    }

    /** The branch of the root a node belongs to: the child of the root on its way up (its own id for a child of the root, via the first parent). */
    static String branchOf(SkillRoute r, String nodeId) {
        SkillNode n = r.node(nodeId);
        while (!n.isRoot() && !r.node(n.parents().get(0).node()).isRoot()) n = r.node(n.parents().get(0).node());
        return n.id();
    }

    /** The shipped file against the code table and the budget. */
    static List<String> consistency(SkillRoute r, DragonTables t) {
        List<String> bad = new ArrayList<>();
        if (!r.id().equals(t.route())) bad.add("route id " + r.id() + " differs from the table's " + t.route());
        Set<String> inFile = new HashSet<>();
        for (SkillNode n : r.nodes()) {
            inFile.add(n.id());
            if (!t.hasNode(n.id())) { bad.add("node " + n.id() + " is in the file but not in the code table"); continue; }
            DragonBudget.Kind kind = t.kind(n.id());
            if (n.grantsSkill() != (kind != DragonBudget.Kind.PASSIVE)) bad.add(n.id() + ": the file and the table disagree on active versus passive");
            if ((n.parents().size() >= 2) != (kind == DragonBudget.Kind.SECRET_ART)) bad.add(n.id() + ": the file and the table disagree on the Secret Art");
            if (n.maxLevel() != t.maxLevel(n.id())) bad.add(n.id() + ": max level " + n.maxLevel() + " in the file, " + t.maxLevel(n.id()) + " in the table");
            Map<String, List<Double>> code = t.valuesOf(n.id());
            if (!code.keySet().equals(n.values().keySet())) bad.add(n.id() + ": value keys differ, file " + n.values().keySet() + ", table " + code.keySet());
            for (Map.Entry<String, List<Double>> e : n.values().entrySet()) {
                List<Double> c = code.get(e.getKey());
                if (c != null && !c.equals(e.getValue())) bad.add(n.id() + "." + e.getKey() + ": file " + e.getValue() + ", table " + c);
            }
            bad.addAll(DragonBudget.check(n.id(), kind, n.maxLevel(), n.values()));
        }
        for (String id : t.nodeIds()) if (!inFile.contains(id)) bad.add("node " + id + " is in the code table but not in the file");
        return bad;
    }

    /** The budget over the code table itself. */
    static List<String> tableBudget(DragonTables t) { return t.violations(); }

    /**
     * The source scan: every {@code num("key"...)} / {@code inum("key"...)} of a skill class, and every {@code num(Numbers.NODE, "key"...)} of the passives class, names a table
     * that exists for that node; and every table of a node is read by its class (the cooldown and the cost are read by the shared base).
     *
     * @param dir          the folder of the route's classes
     * @param numbersClass the pure numbers class (its public String constants name the nodes)
     * @param passivesFile the file name of the passives class
     */
    static List<String> sourceKeys(Path dir, Class<?> numbersClass, DragonTables t, String passivesFile) throws Exception {
        List<String> bad = new ArrayList<>();
        Map<String, Set<String>> read = new HashMap<>();
        Pattern superCall = Pattern.compile("super\\(\\s*" + numbersClass.getSimpleName() + "\\.([A-Z_]+)");
        Pattern key = Pattern.compile("\\bi?num\\(\\s*\"([a-z_]+)\"");
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.filter(p -> p.getFileName().toString().endsWith("Skill.java")).sorted().toList()) {
                String src = Files.readString(f);
                if (src.contains("abstract class")) continue;                       // the route's shared base class serves no single node
                Matcher m = superCall.matcher(src);
                if (!m.find()) { bad.add(f.getFileName() + ": no super(" + numbersClass.getSimpleName() + ".NODE ...) call, cannot tell which node it serves"); continue; }
                String node = (String) numbersClass.getField(m.group(1)).get(null);
                if (!t.hasNode(node)) { bad.add(f.getFileName() + ": node " + node + " is not in the table"); continue; }
                Set<String> keys = read.computeIfAbsent(node, k -> new HashSet<>());
                Matcher km = key.matcher(src);
                while (km.find()) keys.add(km.group(1));
                keys.add("cost_pct");
                keys.add("cooldown");
            }
        }
        Pattern passiveKey = Pattern.compile("\\bnum\\(\\s*" + numbersClass.getSimpleName() + "\\.([A-Z_]+)\\s*,\\s*\"([a-z_]+)\"");
        Matcher pm = passiveKey.matcher(Files.readString(dir.resolve(passivesFile)));
        while (pm.find()) read.computeIfAbsent((String) numbersClass.getField(pm.group(1)).get(null), k -> new HashSet<>()).add(pm.group(2));
        for (String node : t.nodeIds()) {
            Set<String> used = read.getOrDefault(node, Set.of());
            if (used.isEmpty()) bad.add("node " + node + ": no class reads any of its numbers");
            for (String k : used) if (!t.has(node, k)) bad.add("node " + node + ": the code reads '" + k + "', which the table does not have");
            for (String k : t.valuesOf(node).keySet()) if (!used.contains(k)) bad.add("node " + node + ": the table has '" + k + "', which no code reads");
        }
        return bad;
    }

    /**
     * The register class against the route file: every {@code SKILLS.register("id", Class::new)} registers the skill id that exactly one node of the route names in its
     * {@code skill}, the class it creates serves that very node (its {@code super(Numbers.NODE ...)}), and every node that grants a skill is registered.
     *
     * @param registerFile the register class ({@code FireDragonSkills.java})
     * @param classDir     the folder of the skill classes
     */
    static List<String> registrations(SkillRoute r, Path registerFile, Path classDir, Class<?> numbersClass) throws Exception {
        List<String> bad = new ArrayList<>();
        Set<String> registered = new HashSet<>();
        String code = Files.readString(registerFile).replaceAll("(?s)/\\*.*?\\*/", "");          // the comments quote the registration syntax as an example
        Matcher m = Pattern.compile("SKILLS\\.register\\(\"([a-z_]+)\",\\s*(\\w+)::new\\)").matcher(code);
        Pattern superCall = Pattern.compile("super\\(\\s*" + numbersClass.getSimpleName() + "\\.([A-Z_]+)");
        while (m.find()) {
            String id = m.group(1);
            if (!registered.add(id)) bad.add("skill id " + id + " is registered twice");
            if (!id.startsWith("ft_")) bad.add("skill id " + id + " does not start with ft_");
            SkillNode node = r.bySkill("nusmp:" + id);
            if (node == null) { bad.add("skill id " + id + " is registered but no node of the route names nusmp:" + id); continue; }
            Path cls = classDir.resolve(m.group(2) + ".java");
            if (!Files.exists(cls)) { bad.add("skill id " + id + ": class file " + cls + " not found"); continue; }
            Matcher sm = superCall.matcher(Files.readString(cls));
            if (!sm.find()) { bad.add(m.group(2) + ": no super(Numbers.NODE ...) call"); continue; }
            String served = (String) numbersClass.getField(sm.group(1)).get(null);
            if (!served.equals(node.id())) bad.add(m.group(2) + " serves node " + served + " but skill id " + id + " is the one of node " + node.id());
        }
        for (SkillNode n : r.nodes()) if (n.grantsSkill() && !registered.contains(n.skill().substring("nusmp:".length()))) bad.add("node " + n.id() + " names " + n.skill() + ", which the register class does not register");
        return bad;
    }

    /** Every concrete skill class names an icon file that exists under textures/skill/ft/ and is a 32 x 32 picture (the size of the owner's other skill icons). */
    static List<String> icons(Path dir) throws Exception {
        List<String> bad = new ArrayList<>();
        Pattern icon = Pattern.compile("super\\(\\s*\\w+Numbers\\.[A-Z_]+\\s*,\\s*\"([a-z_]+)\"\\)");
        Path textures = Path.of("src/main/resources/assets/nusmp/textures/skill/ft");
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.filter(p -> p.getFileName().toString().endsWith("Skill.java")).sorted().toList()) {
                String src = Files.readString(f);
                if (src.contains("abstract class")) continue;
                Matcher m = icon.matcher(src);
                if (!m.find()) { bad.add(f.getFileName() + ": no icon name in its constructor"); continue; }
                Path png = textures.resolve(m.group(1) + ".png");
                if (!Files.exists(png)) { bad.add(f.getFileName() + ": the icon " + png + " does not exist"); continue; }
                java.awt.image.BufferedImage im = javax.imageio.ImageIO.read(png.toFile());
                if (im == null || im.getWidth() != 32 || im.getHeight() != 32) bad.add(png + " is not a 32 x 32 picture");
            }
        }
        return bad;
    }

    static String join(List<String> problems) { return problems.isEmpty() ? "" : problems.size() + " problem(s): " + String.join(" | ", problems); }

    static void noProblems(List<String> problems) {
        org.junit.jupiter.api.Assertions.assertTrue(problems.isEmpty(), join(problems));
    }

    /** The folder of a route's classes, relative to the working directory (the tests run from the project root, like the other data tests). */
    static Path dir(String pkg) { return Path.of("src/main/java/com/newuniverse/nusmp/skilltree/ft/" + pkg); }
}
