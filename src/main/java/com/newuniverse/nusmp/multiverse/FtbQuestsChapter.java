package com.newuniverse.nusmp.multiverse;

import com.newuniverse.nusmp.blackclover.Kingdom;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 0.39: with FTB Quests installed, the secret quests also appear in the quest book, as a hidden chapter "The Convergence"
 * (config/ftbquests/quests/chapters/nusmp_convergence.snbt, written only when the file is missing, so a pack maker can edit it).
 *
 * <p>Every quest has one "advancement" task on its hidden advancement ({@code nusmp:convergence/<id>}), which {@link SecretQuests}
 * awards, so the book can never disagree with the mod and nothing can be ticked off by hand. The first quest (Unknown Magic
 * Detected) and the stage / kingdom markers are invisible until completed, and the chapter hides every quest until its
 * dependencies are done: a Tensura-origin player never completes the first quest, so the chapter never shows for them.
 * Without FTB Quests nothing is written; the quests still run through chat and the Multiverse status panel.
 */
public final class FtbQuestsChapter {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final String PREFIX = "6E75736D70";            // "nusmp", keeps every id a positive hex long

    private FtbQuestsChapter() {}

    public static void install() {
        try {
            if (!ModList.get().isLoaded("ftbquests") || !MultiverseConfig.get(MultiverseConfig.QUESTS_FTB_CHAPTER)) return;
            Path file = FMLPaths.CONFIGDIR.get().resolve("ftbquests/quests/chapters/nusmp_convergence.snbt");
            if (Files.exists(file)) return;
            Files.createDirectories(file.getParent());
            Files.writeString(file, snbt(), StandardCharsets.UTF_8);
            LOGGER.info("Wrote the hidden FTB Quests chapter 'The Convergence' to {}", file);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not write the FTB Quests Convergence chapter: {}", e.toString());
        }
    }

    private static String id(int kind, int n) { return String.format(Locale.ROOT, "%s%02X%04X", PREFIX, kind, n); }

    private static String str(String s) { return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }

    /** The chapter file, built from {@link SecretQuests.Quest} so it always matches the mod. */
    static String snbt() {
        StringBuilder q = new StringBuilder();
        // markers: stages reached (row 0) and the anomaly's kingdom (row 1), invisible until completed
        Convergence.Stage[] stages = Convergence.Stage.values();
        for (int i = 1; i < stages.length; i++) {
            Convergence.Stage s = stages[i];
            quest(q, id(3, i), "The Convergence: " + s.title, "The bleed has reached a new stage.", List.of(),
                    "convergence/stage_" + s.name().toLowerCase(Locale.ROOT), id(4, i), true, -3.0 + i * 1.5, -3.0);
        }
        Kingdom[] kingdoms = Kingdom.values();
        for (int i = 0; i < kingdoms.length; i++) {
            Kingdom k = kingdoms[i];
            quest(q, id(3, 16 + i), "Origin: " + k.displayName, "Your magic comes from the " + k.displayName + ".", List.of(),
                    "convergence/kingdom_" + k.name().toLowerCase(Locale.ROOT), id(4, 16 + i), true, -1.5 + i * 1.5, -1.5);
        }
        // the quests
        SecretQuests.Quest[] all = SecretQuests.Quest.values();
        String root = id(1, 0);
        for (int i = 0; i < all.length; i++) {
            SecretQuests.Quest sq = all[i];
            List<String> deps = new ArrayList<>();
            if (sq != SecretQuests.Quest.UNKNOWN_MAGIC) deps.add(root);
            if (sq.stage.ordinal() > 0) deps.add(id(3, sq.stage.ordinal()));
            if (sq.kingdom != null) deps.add(id(3, 16 + sq.kingdom.ordinal()));
            double x = sq == SecretQuests.Quest.UNKNOWN_MAGIC ? 0 : ((i - 1) % 5) * 1.5 - 3.0;
            double y = sq == SecretQuests.Quest.UNKNOWN_MAGIC ? 0.5 : 2.0 + ((i - 1) / 5) * 1.5;
            String reward = (sq.goldStars > 0 ? "Reward: " + sq.goldStars + " gold star" + (sq.goldStars > 1 ? "s" : "") : "")
                    + (sq.mastery > 0 ? (sq.goldStars > 0 ? ", " : "Reward: ") + sq.mastery + " grimoire mastery" : "");
            quest(q, id(1, i), sq.title, sq.hint + (reward.isEmpty() ? "" : "\n" + reward), deps, "convergence/" + sq.id, id(2, i),
                    sq == SecretQuests.Quest.UNKNOWN_MAGIC, x, y);
        }
        return "{\n"
                + "\tdefault_hide_dependency_lines: false\n"
                + "\tdefault_quest_shape: \"\"\n"
                + "\tfilename: \"nusmp_convergence\"\n"
                + "\tgroup: \"\"\n"
                + "\thide_quest_until_deps_complete: true\n"
                + "\tid: \"" + id(0, 1) + "\"\n"
                + "\torder_index: 99\n"
                + "\tquest_links: [ ]\n"
                + "\tquests: [\n" + q + "\t]\n"
                + "\tsubtitle: [" + str("Only those touched by another world's magic can read this chapter.") + "]\n"
                + "\ttitle: \"The Convergence\"\n"
                + "}\n";
    }

    private static void quest(StringBuilder b, String id, String title, String text, List<String> deps, String advancement, String taskId,
                              boolean invisible, double x, double y) {
        b.append("\t\t{\n");
        if (!deps.isEmpty()) {
            b.append("\t\t\tdependencies: [");
            for (int i = 0; i < deps.size(); i++) b.append(i > 0 ? " " : "").append('"').append(deps.get(i)).append('"');
            b.append("]\n");
        }
        b.append("\t\t\tdescription: [");
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) b.append(i > 0 ? " " : "").append(str(lines[i]));
        b.append("]\n");
        b.append("\t\t\tid: \"").append(id).append("\"\n");
        if (invisible) b.append("\t\t\tinvisible: true\n");
        b.append("\t\t\ttasks: [{\n")
                .append("\t\t\t\tadvancement: \"nusmp:").append(advancement).append("\"\n")
                .append("\t\t\t\tcriterion: \"\"\n")
                .append("\t\t\t\tid: \"").append(taskId).append("\"\n")
                .append("\t\t\t\ttype: \"advancement\"\n")
                .append("\t\t\t}]\n");
        b.append("\t\t\ttitle: ").append(str(title)).append('\n');
        b.append(String.format(Locale.ROOT, "\t\t\tx: %.1fd\n\t\t\ty: %.1fd\n", x, y));
        b.append("\t\t}\n");
    }
}
