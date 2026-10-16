package com.newuniverse.nusmp.quests.pack;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Why: the installer must know later which files in the quest folder are ours (to update them without touching the owner's own chapters, and
 * to uninstall only them). The answer is a small text file, the marker {@value #MARKER_NAME}, written next to the quest files: pack version,
 * a content hash and the list of files. This class is the pure text form of that marker plus the hash, with no file access, so JUnit can test it.
 *
 * <p>Text form: lines {@code version=...}, {@code hash=...} and one {@code file=relative/path} per file (forward slashes, sorted).
 */
public record PackManifest(String version, String hash, List<String> files) {

    public static final String MARKER_NAME = ".nusmp_pack";

    public PackManifest {
        files = List.copyOf(files);
    }

    /** Serialised marker content. */
    public String toText() {
        StringBuilder sb = new StringBuilder();
        sb.append("version=").append(version).append('\n');
        sb.append("hash=").append(hash).append('\n');
        for (String f : files) sb.append("file=").append(f).append('\n');
        return sb.toString();
    }

    /** Parses a marker; unknown lines are ignored. Returns null when the text has no version line (not one of our markers). */
    public static PackManifest parse(String text) {
        if (text == null) return null;
        String version = null, hash = "";
        List<String> files = new ArrayList<>();
        for (String raw : text.split("\\R")) {
            String line = raw.trim();
            if (line.startsWith("version=")) version = line.substring(8);
            else if (line.startsWith("hash=")) hash = line.substring(5);
            else if (line.startsWith("file=") && line.length() > 5) files.add(line.substring(5));
        }
        return version == null ? null : new PackManifest(version, hash, files);
    }

    /** A short content hash (first 16 hex chars of SHA-256 over sorted names and bytes), so two packs compare by content, not by file time. */
    public static String hash(Map<String, byte[]> contents) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (Map.Entry<String, byte[]> e : new TreeMap<>(contents).entrySet()) {
                md.update(e.getKey().getBytes(StandardCharsets.UTF_8));
                md.update((byte) 0);
                md.update(e.getValue());
                md.update((byte) 0);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) sb.append(String.format("%02x", b));
            return sb.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
