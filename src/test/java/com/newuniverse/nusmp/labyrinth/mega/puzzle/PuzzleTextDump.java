package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Why: the lang fragment {@code tools/lang/puzzles_v2.json} is generated from {@link PuzzleText} so the file and the code cannot drift; {@code PuzzlePlanTest} fails when they differ.
 * Run it from the repository root: {@code java -cp <classes> com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleTextDump}.
 */
public final class PuzzleTextDump {
    private PuzzleTextDump() {}

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0] : "tools/lang/puzzles_v2.json");
        Files.createDirectories(out.toAbsolutePath().getParent());
        Files.writeString(out, PuzzleText.json(), StandardCharsets.UTF_8);
        System.out.println("wrote " + out + " (" + PuzzleText.all().size() + " lines)");
    }
}
