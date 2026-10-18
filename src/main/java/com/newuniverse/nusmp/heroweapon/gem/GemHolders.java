package com.newuniverse.nusmp.heroweapon.gem;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Why: when the Holy Calling Gem says "the legendary weapons have already chosen their heroes", the first question of an operator is "who holds them?", and the
 * players who hold a weapon are not always online. {@code /multiverse hero gem holders} answers: the online players with a bond (weapon, level, form), read from
 * the bond itself, and the weapons held by players who are offline, read from the core's world file {@code nusmp_hero_registry.dat}. That file's layout
 * belongs to the core builder and was not known when this was written, so the reader is the deliberately tolerant, pure {@link RegistryScan}; this class only
 * finds the file, decodes it and adapts the game's saved tree for the reader. A file that is missing or cannot be read adds nothing. It reads only; it never writes
 * the file and never changes a bond.
 */
final class GemHolders {
    private GemHolders() {}

    private static final Logger LOG = LogUtils.getLogger();
    static final String REGISTRY_FILE = "nusmp_hero_registry.dat";

    /** A player who holds a hero weapon. */
    record Holder(String name, HeroKind kind, boolean online, int level, String form) {}

    /** The online players who hold a hero weapon, in the order the server lists them. */
    static List<Holder> online(MinecraftServer server) {
        List<Holder> out = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            HeroBond b = HeroBonds.of(p);
            if (b.isBound()) out.add(new Holder(p.getGameProfile().getName(), b.kind(), true, b.level(), b.form()));
        }
        return out;
    }

    /** The weapons the core's registry file gives to players who are not online (best effort, see the class comment). */
    static List<Holder> offline(MinecraftServer server) {
        List<Holder> out = new ArrayList<>();
        for (Map.Entry<HeroKind, List<UUID>> e : registry(server).entrySet()) {
            for (UUID holder : e.getValue()) {
                if (server.getPlayerList().getPlayer(holder) != null) continue;
                out.add(new Holder(nameOf(server, holder), e.getKey(), false, -1, ""));
            }
        }
        return out;
    }

    private static String nameOf(MinecraftServer server, UUID id) {
        try {
            if (server.getProfileCache() != null) {
                Optional<GameProfile> p = server.getProfileCache().get(id);
                if (p.isPresent()) return p.get().getName();
            }
        } catch (RuntimeException e) {
            LOG.debug("profile lookup failed", e);
        }
        return id.toString();
    }

    /** The holders of each weapon in the core's registry file, or empty when the file is missing or unreadable. */
    static Map<HeroKind, List<UUID>> registry(MinecraftServer server) {
        Path file = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(REGISTRY_FILE);
        try {
            if (!Files.isRegularFile(file)) return new EnumMap<>(HeroKind.class);
            return parse(NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()));
        } catch (java.io.IOException | RuntimeException e) {
            LOG.debug("the hero registry could not be read", e);
            return new EnumMap<>(HeroKind.class);
        }
    }

    /** Reads a saved tree with the tolerant {@link RegistryScan}. */
    static Map<HeroKind, List<UUID>> parse(Tag root) { return RegistryScan.parse(new NbtNode(root)); }

    /** The game's saved-tree values seen through the small view the pure reader needs. */
    private record NbtNode(Tag tag) implements RegistryScan.Node {
        @Override
        public Map<String, RegistryScan.Node> compound() {
            if (!(tag instanceof CompoundTag c)) return null;
            Map<String, RegistryScan.Node> out = new LinkedHashMap<>();
            for (String key : c.getAllKeys()) out.put(key, new NbtNode(c.get(key)));
            return out;
        }

        @Override
        public List<RegistryScan.Node> list() {
            if (!(tag instanceof ListTag l)) return null;
            List<RegistryScan.Node> out = new ArrayList<>();
            for (int i = 0; i < l.size(); i++) out.add(new NbtNode(l.get(i)));
            return out;
        }

        @Override
        public String text() { return tag instanceof StringTag s ? s.getAsString() : null; }

        @Override
        public int[] ints() { return tag instanceof IntArrayTag ia ? ia.getAsIntArray() : null; }
    }
}
