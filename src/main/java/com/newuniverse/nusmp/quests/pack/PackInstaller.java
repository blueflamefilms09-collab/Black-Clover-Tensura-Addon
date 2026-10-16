package com.newuniverse.nusmp.quests.pack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Why: the generated questline ships inside the mod jar, but FTB Quests reads its quests from {@code config/ftbquests/quests} on disk. This
 * class copies the bundled pack there, safely: it refuses to overwrite quests the owner made unless forced, always keeps a backup of anything
 * it changes, and remembers what it wrote (the marker {@value PackManifest#MARKER_NAME}) so an update or uninstall only ever touches its own
 * files. Every method takes the two folders as {@link Path}s, so tests run on a temp directory and the command class only supplies the real ones.
 *
 * <p>The pack folder is searched for FTB's layout: if it holds a {@code quests} subfolder with {@code data.snbt} that is the content root,
 * otherwise the folder itself. A top-level {@code README*} file is documentation and is never copied.
 */
public final class PackInstaller {
    private PackInstaller() {}

    public record Result(boolean ok, String message) {
        static Result ok(String m) { return new Result(true, m); }
        static Result fail(String m) { return new Result(false, m); }
    }

    /** What {@code status} reports. */
    public record Status(boolean folderExists, boolean folderEmpty, PackManifest installed, boolean packAvailable, String packHash, int packFiles) {}

    // ---------------------------------------------------------------- reading the bundled pack
    /** The folder inside {@code packRoot} that corresponds to the quest folder. */
    public static Path contentRoot(Path packRoot) {
        Path nested = packRoot.resolve("quests");
        if (Files.isRegularFile(nested.resolve("data.snbt")) && !Files.isRegularFile(packRoot.resolve("data.snbt"))) return nested;
        return packRoot;
    }

    /** Relative path (forward slashes) to bytes, sorted. Empty when the folder is missing. */
    public static Map<String, byte[]> readPack(Path packRoot) throws IOException {
        Map<String, byte[]> out = new TreeMap<>();
        if (packRoot == null || !Files.isDirectory(packRoot)) return out;
        Path root = contentRoot(packRoot);
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path p : (Iterable<Path>) walk.filter(Files::isRegularFile)::iterator) {
                String rel = root.relativize(p).toString().replace('\\', '/');
                if (rel.indexOf('/') < 0 && rel.toLowerCase().startsWith("readme")) continue;
                out.put(rel, Files.readAllBytes(p));
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- status
    public static Status status(Path packRoot, Path questsDir) {
        boolean exists = Files.isDirectory(questsDir);
        boolean empty = !exists || isEmpty(questsDir);
        PackManifest installed = readMarker(questsDir);
        Map<String, byte[]> pack;
        try { pack = readPack(packRoot); } catch (IOException e) { pack = Map.of(); }
        return new Status(exists, empty, installed, !pack.isEmpty(), pack.isEmpty() ? "" : PackManifest.hash(pack), pack.size());
    }

    public static PackManifest readMarker(Path questsDir) {
        try {
            Path m = questsDir.resolve(PackManifest.MARKER_NAME);
            return Files.isRegularFile(m) ? PackManifest.parse(Files.readString(m, StandardCharsets.UTF_8)) : null;
        } catch (IOException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------- install
    public static Result install(Path packRoot, Path questsDir, String version, boolean force, long nowMillis) {
        try {
            Map<String, byte[]> pack = readPack(packRoot);
            if (pack.isEmpty()) return Result.fail("The quest pack is not bundled in this build (no quest_pack folder in the jar).");
            boolean exists = Files.isDirectory(questsDir);
            PackManifest marker = readMarker(questsDir);
            PackPlan.InstallDecision d = PackPlan.decideInstall(exists, !exists || isEmpty(questsDir), marker != null, force);
            String backupNote = "";
            switch (d) {
                case REFUSE -> {
                    return Result.fail("The quest folder " + questsDir + " already holds quests that are not from this pack. Nothing was changed. "
                            + "Use '/nusmp quests install force' to move them to a backup folder and install the pack.");
                }
                case FORCED_BACKUP -> {
                    Path backup = uniqueBackup(questsDir, nowMillis);
                    moveDir(questsDir, backup);
                    backupNote = " Your old quests were moved to " + backup.getFileName() + ".";
                }
                case OVER_OURS -> {
                    Path backup = uniqueBackup(questsDir, nowMillis);
                    copyDir(questsDir, backup);
                    backupNote = " The previous state was copied to " + backup.getFileName() + ".";
                    deleteOwned(questsDir, marker, pack.keySet());
                }
                case FRESH -> { }
            }
            write(questsDir, pack, version);
            return Result.ok("Installed " + pack.size() + " quest files (pack " + PackManifest.hash(pack) + ")." + backupNote);
        } catch (IOException | RuntimeException e) {
            return Result.fail("Install failed: " + e);
        }
    }

    // ---------------------------------------------------------------- update
    public static Result update(Path packRoot, Path questsDir, String version, long nowMillis) {
        try {
            PackManifest marker = readMarker(questsDir);
            if (marker == null) return Result.fail("Nothing to update: this quest folder was not installed by /nusmp quests install.");
            Map<String, byte[]> pack = readPack(packRoot);
            if (pack.isEmpty()) return Result.fail("The quest pack is not bundled in this build (no quest_pack folder in the jar).");
            String newHash = PackManifest.hash(pack);
            if (newHash.equals(marker.hash()) && version.equals(marker.version()) && allPresent(questsDir, pack.keySet())) {
                return Result.ok("Already up to date (pack " + newHash + ").");
            }
            Path backup = uniqueBackup(questsDir, nowMillis);
            copyDir(questsDir, backup);
            PackPlan.UpdatePlan plan = PackPlan.planUpdate(new HashSet<>(marker.files()), pack.keySet(), existingFiles(questsDir));
            for (String rel : plan.toDelete()) Files.deleteIfExists(questsDir.resolve(rel));
            write(questsDir, pack, version);
            return Result.ok("Updated to pack " + newHash + ": " + plan.toWrite().size() + " files written, " + plan.toDelete().size()
                    + " retired, " + plan.untouched().size() + " of your own files left alone. Backup: " + backup.getFileName() + ".");
        } catch (IOException | RuntimeException e) {
            return Result.fail("Update failed: " + e);
        }
    }

    // ---------------------------------------------------------------- uninstall
    public static Result uninstall(Path questsDir, long nowMillis) {
        try {
            PackManifest marker = readMarker(questsDir);
            if (marker == null) return Result.fail("Nothing to uninstall: this quest folder was not installed by /nusmp quests install.");
            Path backup = uniqueBackup(questsDir, nowMillis);
            copyDir(questsDir, backup);
            List<String> gone = PackPlan.planUninstall(new HashSet<>(marker.files()), existingFiles(questsDir));
            for (String rel : gone) Files.deleteIfExists(questsDir.resolve(rel));
            Files.deleteIfExists(questsDir.resolve(PackManifest.MARKER_NAME));
            pruneEmptyDirs(questsDir);
            return Result.ok("Removed " + gone.size() + " files of the pack; anything you added yourself stays. Backup: " + backup.getFileName() + ".");
        } catch (IOException | RuntimeException e) {
            return Result.fail("Uninstall failed: " + e);
        }
    }

    // ---------------------------------------------------------------- file helpers
    private static void write(Path questsDir, Map<String, byte[]> pack, String version) throws IOException {
        Files.createDirectories(questsDir);
        for (Map.Entry<String, byte[]> e : pack.entrySet()) {
            if (!PackPlan.safe(e.getKey())) continue;
            Path target = questsDir.resolve(e.getKey()).normalize();
            if (!target.startsWith(questsDir.normalize())) continue;
            Files.createDirectories(target.getParent());
            Files.write(target, e.getValue());
        }
        PackManifest m = new PackManifest(version, PackManifest.hash(pack), new ArrayList<>(pack.keySet()));
        Files.writeString(questsDir.resolve(PackManifest.MARKER_NAME), m.toText(), StandardCharsets.UTF_8);
    }

    /** On a reinstall over our own files: drop the files the old marker owned that the new pack no longer has. */
    private static void deleteOwned(Path questsDir, PackManifest old, Set<String> keep) throws IOException {
        for (String rel : old.files()) if (!keep.contains(rel) && PackPlan.safe(rel)) Files.deleteIfExists(questsDir.resolve(rel));
    }

    private static boolean allPresent(Path questsDir, Set<String> rels) {
        for (String r : rels) if (!Files.isRegularFile(questsDir.resolve(r))) return false;
        return true;
    }

    static Set<String> existingFiles(Path dir) throws IOException {
        Set<String> out = new HashSet<>();
        if (!Files.isDirectory(dir)) return out;
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : (Iterable<Path>) walk.filter(Files::isRegularFile)::iterator) out.add(dir.relativize(p).toString().replace('\\', '/'));
        }
        return out;
    }

    private static boolean isEmpty(Path dir) {
        try (Stream<Path> s = Files.list(dir)) { return s.findAny().isEmpty(); } catch (IOException e) { return false; }
    }

    private static Path uniqueBackup(Path questsDir, long nowMillis) {
        Path parent = questsDir.toAbsolutePath().getParent();
        String base = PackPlan.backupName(nowMillis);
        Path p = parent.resolve(base);
        for (int i = 2; Files.exists(p); i++) p = parent.resolve(base + "_" + i);
        return p;
    }

    private static void copyDir(Path from, Path to) throws IOException {
        try (Stream<Path> walk = Files.walk(from)) {
            for (Path src : (Iterable<Path>) walk::iterator) {
                Path dst = to.resolve(from.relativize(src).toString());
                if (Files.isDirectory(src)) Files.createDirectories(dst);
                else { Files.createDirectories(dst.getParent()); Files.copy(src, dst, StandardCopyOption.COPY_ATTRIBUTES); }
            }
        }
    }

    private static void moveDir(Path from, Path to) throws IOException {
        try {
            Files.move(from, to);
        } catch (IOException e) {
            copyDir(from, to);
            try (Stream<Path> walk = Files.walk(from)) {
                for (Path p : (Iterable<Path>) walk.sorted(java.util.Comparator.reverseOrder())::iterator) Files.delete(p);
            }
        }
    }

    private static void pruneEmptyDirs(Path root) throws IOException {
        if (!Files.isDirectory(root)) return;
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path p : (Iterable<Path>) walk.sorted(java.util.Comparator.reverseOrder())::iterator) {
                if (!p.equals(root) && Files.isDirectory(p) && isEmpty(p)) Files.delete(p);
            }
        }
    }
}
