package com.newuniverse.nusmp.quests.pack;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Why: every decision of the quest installer (may we install here, what must be backed up, which files does an update write or remove,
 * which does an uninstall delete) is made here from plain sets of relative paths, with no file access, so the rules that protect the owner's
 * own quests can be tested without a disk. {@link PackInstaller} only carries the plans out.
 */
public final class PackPlan {
    private PackPlan() {}

    public enum InstallDecision {
        /** Target missing or empty: copy straight in. */
        FRESH,
        /** Target already holds our marker: this is a reinstall (same as update, backup first). */
        OVER_OURS,
        /** Target holds foreign files and the operator forced it: back them up, then copy. */
        FORCED_BACKUP,
        /** Target holds foreign files and no force: do nothing, explain. */
        REFUSE
    }

    /** What to do for {@code install}. */
    public static InstallDecision decideInstall(boolean targetExists, boolean targetEmpty, boolean hasMarker, boolean force) {
        if (!targetExists || targetEmpty) return InstallDecision.FRESH;
        if (hasMarker) return InstallDecision.OVER_OURS;
        return force ? InstallDecision.FORCED_BACKUP : InstallDecision.REFUSE;
    }

    /** Folder name of a backup, e.g. {@code quests_backup_20261016_120000} (UTC). */
    public static String backupName(long epochMillis) {
        return "quests_backup_" + DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneOffset.UTC).format(Instant.ofEpochMilli(epochMillis));
    }

    /** Files an update writes ({@code toWrite}) and our own old files it deletes because the new pack no longer has them ({@code toDelete}). */
    public record UpdatePlan(List<String> toWrite, List<String> toDelete, List<String> untouched) {}

    /**
     * @param oldOwned   files named by the installed marker
     * @param newFiles   files of the pack being installed
     * @param existing   files that exist now in the quest folder
     */
    public static UpdatePlan planUpdate(Set<String> oldOwned, Set<String> newFiles, Set<String> existing) {
        List<String> write = new ArrayList<>(new TreeSet<>(newFiles));
        List<String> delete = new ArrayList<>();
        for (String f : new TreeSet<>(oldOwned)) if (!newFiles.contains(f) && existing.contains(f) && safe(f)) delete.add(f);
        Set<String> keep = new LinkedHashSet<>(new TreeSet<>(existing));
        keep.removeAll(newFiles);
        keep.removeAll(delete);
        keep.remove(PackManifest.MARKER_NAME);
        return new UpdatePlan(write, delete, new ArrayList<>(keep));
    }

    /** Files an uninstall deletes: exactly the ones the marker lists that still exist. Everything else stays. */
    public static List<String> planUninstall(Set<String> owned, Set<String> existing) {
        List<String> out = new ArrayList<>();
        for (String f : new TreeSet<>(owned)) if (existing.contains(f) && safe(f)) out.add(f);
        return out;
    }

    /** A relative path from a marker is only trusted when it stays inside the folder: no parent steps, no absolute path, not the marker itself. */
    public static boolean safe(String rel) {
        if (rel == null || rel.isEmpty() || rel.startsWith("/") || rel.startsWith("\\") || rel.indexOf(':') >= 0) return false;
        if (rel.equals(PackManifest.MARKER_NAME)) return false;
        for (String part : rel.split("[/\\\\]")) if (part.equals("..")) return false;
        return true;
    }
}
