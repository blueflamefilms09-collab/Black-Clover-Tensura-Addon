package com.newuniverse.nusmp.client.skilltree.layout;

import com.newuniverse.nusmp.skilltree.data.RouteSchema;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Why: the Tensura-menu tests need two kinds of route: a tiny hand-built line whose every number is known (so a test can say exactly which rows must appear), and the REAL route files (so the rules are
 * proved on all 283 routes the player will meet, not only on a toy). Test helper only.
 */
final class MenuFixtures {
    private MenuFixtures() {}

    static SkillNode node(String id, String type, String kind, int max) {
        return new SkillNode(id, "Name " + id, "Desc " + id, List.of(), 1, List.of(), max, "", List.of(), Map.of(), 0, 0, "", type, kind, Map.of());
    }

    /** A line with a stance, two arts, two passives, a stat bonus and a connector node that only unlocks others. */
    static SkillRoute line() {
        return new SkillRoute("nusmp:t", "tensura", "Test Line", "A test line.", "nusmp:t_book", "Book", "", 0x336699, "minecraft:stick", "t", 1, 1,
                List.of(node("stance", "active_ability", "stance", 3), node("art_a", "active_ability", "cone_strike", 3), node("art_b", "active_ability", "nova", 2),
                        node("pas_a", "passive_toggle", "lifesteal", 3), node("pas_b", "passive_toggle", "thorns", 2), node("stat_a", "attribute_modifier", "attr", 4),
                        node("link", "", "", 1)),
                "tensura", "", "", "");
    }

    /** Every shipped route file, parsed. */
    static List<SkillRoute> realRoutes() {
        List<SkillRoute> out = new ArrayList<>();
        try (Stream<Path> files = Files.list(Path.of("src/main/resources/data/nusmp/skill_trees"))) {
            for (Path p : files.filter(f -> f.toString().endsWith(".json")).sorted().toList()) {
                String stem = p.getFileName().toString().replace(".json", "");
                RouteSchema.Parsed parsed = RouteSchema.parse("nusmp:" + stem, Files.readString(p, StandardCharsets.UTF_8));
                if (parsed.route() == null) throw new AssertionError(stem + " does not parse: " + parsed.problems());
                out.add(parsed.route());
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return out;
    }
}
