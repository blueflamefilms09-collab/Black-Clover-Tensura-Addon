package com.newuniverse.nusmp.labyrinth.mega.party;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.dimension.DimensionType;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

public final class LanePurge {
    private static final Logger LOG = LogUtils.getLogger();

    private static final String[] FOLDERS = {"region", "entities", "poi"};

    private LanePurge() {}

    public static Set<Integer> readRetired(Path worldRoot) {
        Set<Integer> out = new TreeSet<>();
        Path file = worldRoot.resolve("data").resolve(MegaParties.NAME + ".dat");
        if (!Files.isRegularFile(file)) return out;
        try {
            CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            for (int r : root.getCompound("data").getIntArray("retired")) out.add(r);
        } catch (IOException | RuntimeException e) {
            LOG.warn("[anime_multiverse] The list of freed dungeon lanes could not be read: {}", e.toString());
        }
        return out;
    }

    private static Path dimensionRoot(Path worldRoot) { return DimensionType.getStorageFolder(Labyrinth.MEGA_DIMENSION, worldRoot); }

    public static Set<Integer> run(Path worldRoot, LaneReclaim.Mode mode, Collection<Integer> retired) {
        Set<Integer> done = new TreeSet<>(retired);
        if (mode == LaneReclaim.Mode.OFF || retired.isEmpty()) return new TreeSet<>();
        Path dim = dimensionRoot(worldRoot);
        for (String folder : FOLDERS) {
            Path dir = dim.resolve(folder);
            if (!Files.isDirectory(dir)) continue;
            List<Path> files = new ArrayList<>();
            try (Stream<Path> s = Files.list(dir)) {
                s.forEach(files::add);
            } catch (IOException e) {
                LOG.warn("[anime_multiverse] The dungeon folder {} could not be listed: {}", dir, e.toString());
                done.clear();
                return done;
            }
            for (Path f : files) {
                int party = LaneReclaim.partyOfFile(f.getFileName().toString());
                if (party <= 0 || !retired.contains(party)) continue;
                try {
                    if (mode == LaneReclaim.Mode.DELETE) {
                        Files.deleteIfExists(f);
                    } else {
                        Path to = worldRoot.resolve("dungeon_archive").resolve("lane-" + party).resolve(folder);
                        Files.createDirectories(to);
                        Files.move(f, to.resolve(f.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    done.remove(party);
                    LOG.warn("[anime_multiverse] Lane {}: {} could not be {}: {}", party, f.getFileName(), mode == LaneReclaim.Mode.DELETE ? "deleted" : "archived", e.toString());
                }
            }
        }
        return done;
    }

    public static Map<Integer, Long> sizes(Path worldRoot) {
        Map<Integer, Long> out = new TreeMap<>();
        Path dim = dimensionRoot(worldRoot);
        for (String folder : FOLDERS) {
            Path dir = dim.resolve(folder);
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> s = Files.list(dir)) {
                s.forEach(f -> {
                    int party = LaneReclaim.partyOfFile(f.getFileName().toString());
                    if (party < 0) return;
                    try {
                        out.merge(party, Files.size(f), Long::sum);
                    } catch (IOException ignored) {
                    }
                });
            } catch (IOException ignored) {
            }
        }
        return out;
    }
}
