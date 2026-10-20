package com.newuniverse.nusmp.labyrinth.mega.lock;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: the escape the owner asked for must be VISIBLE: "a clearly labelled exit rune". A rune is two vanilla entities and no new block, item or texture: an {@code interaction}
 * entity (an invisible box that can be right-clicked or hit, 1.4 wide and 2.2 tall) and a {@code text_display} floating over it with the label in the System's gold, turned
 * to face whoever looks; a ring of end-rod or portal particles rises from it once a second so it can be seen across a dark room. Both carry the scoreboard tags
 * {@code nusmp_release_rune} and {@code nusmp_rune_key:<lock key>}, which is how a click finds its lock again even after a restart, and how a rune that outlived its lock (the
 * server stopped while it stood) is found and discarded: {@link #sweep} removes every tagged entity near a player whose key is not a live lock. Using no new block means nothing
 * has to be restored in a guarded dimension (the Dungeon Guard refuses block changes there), and the label is a translatable component with an English fallback, so it
 * reads properly before the lang fragment is merged. Server thread only.
 */
public final class LockRunes {
    private LockRunes() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** The scoreboard tag on both entities of a rune. */
    public static final String TAG = "nusmp_release_rune";
    /** The tag prefix that carries the lock key. */
    public static final String KEY_PREFIX = "nusmp_rune_key:";

    private record Rune(UUID hit, UUID label, boolean exit) {}

    private static final Map<String, Rune> RUNES = new HashMap<>();

    public static boolean has(String key) { return RUNES.containsKey(key); }

    public static int count() { return RUNES.size(); }

    /** Is this entity one of the two parts of a rune? */
    public static boolean isRune(Entity e) { return e != null && e.getTags().contains(TAG); }

    /** The lock key a rune entity carries, or null. */
    public static String keyOf(Entity e) {
        if (e == null) return null;
        for (String t : e.getTags()) if (t.startsWith(KEY_PREFIX)) return t.substring(KEY_PREFIX.length());
        return null;
    }

    /** Makes sure the rune of this lock stands at {@code pos}, and gives it a puff of light (call it once a second while the rune is wanted). Never throws. */
    public static void ensure(ServerLevel level, String key, Vec3 pos, boolean exit) {
        try {
            Rune r = RUNES.get(key);
            boolean alive = r != null && level.getEntity(r.hit()) != null && level.getEntity(r.label()) != null;
            if (!alive) {
                if (r != null) discard(level, r);
                RUNES.put(key, create(level, key, pos, exit));
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.9f, exit ? 1.4f : 1.1f);
            }
            glow(level, pos, exit);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] The release rune for {} could not be placed: {}", key, e.toString());
        }
    }

    /** Removes the rune of a lock (when it opened). */
    public static void remove(ServerLevel level, String key) {
        Rune r = RUNES.remove(key);
        if (r != null) discard(level, r);
        if (level != null) sweepKey(level, key);
    }

    private static void discard(ServerLevel level, Rune r) {
        Entity a = level.getEntity(r.hit());
        if (a != null) a.discard();
        Entity b = level.getEntity(r.label());
        if (b != null) b.discard();
    }

    /** Discards every rune entity near a player whose key is not in {@code live} (a rune left over from a stopped server, a lock that opened while its chunk was away). */
    public static void sweep(ServerLevel level, Set<String> live) {
        try {
            for (ServerPlayer p : level.players()) {
                AABB box = p.getBoundingBox().inflate(64.0, 32.0, 64.0);
                for (Entity e : level.getEntities((Entity) null, box, LockRunes::isRune)) {
                    String k = keyOf(e);
                    if (k == null || !live.contains(k)) e.discard();
                }
            }
            RUNES.keySet().removeIf(k -> !live.contains(k));
        } catch (RuntimeException e) {
            LOG.debug("[nusmp] rune sweep: {}", e.toString());
        }
    }

    private static void sweepKey(ServerLevel level, String key) {
        try {
            String tag = KEY_PREFIX + key;
            for (ServerPlayer p : level.players()) {
                AABB box = p.getBoundingBox().inflate(64.0, 32.0, 64.0);
                for (Entity e : level.getEntities((Entity) null, box, x -> x.getTags().contains(tag))) e.discard();
            }
        } catch (RuntimeException e) {
            LOG.debug("[nusmp] rune sweep: {}", e.toString());
        }
    }

    public static void clear() { RUNES.clear(); }

    // ---------------------------------------------------------------- building a rune
    private static Rune create(ServerLevel level, String key, Vec3 pos, boolean exit) {
        Interaction hit = EntityType.INTERACTION.create(level);
        Display.TextDisplay label = EntityType.TEXT_DISPLAY.create(level);
        if (hit == null || label == null) throw new IllegalStateException("the rune entities could not be created");

        hit.setPos(pos.x, pos.y, pos.z);
        CompoundTag t = hit.saveWithoutId(new CompoundTag());
        t.putFloat("width", 1.4f);
        t.putFloat("height", 2.2f);
        t.putBoolean("response", true);
        hit.load(t);
        hit.addTag(TAG);
        hit.addTag(KEY_PREFIX + key);

        label.setPos(pos.x, pos.y + 2.5, pos.z);
        CompoundTag l = label.saveWithoutId(new CompoundTag());
        l.putString("text", labelJson(exit));
        l.putString("billboard", "center");
        l.putInt("line_width", 260);
        l.putInt("background", 0xA0061C26);
        l.putBoolean("shadow", true);
        l.putFloat("view_range", 2.0f);
        label.load(l);
        label.addTag(TAG);
        label.addTag(KEY_PREFIX + key);

        level.addFreshEntity(hit);
        level.addFreshEntity(label);
        return new Rune(hit.getUUID(), label.getUUID(), exit);
    }

    /** The label as a JSON text component (1.21.1 stores a text display's text as a JSON string): the System tag, the rune's name, and what a click does. */
    static String labelJson(boolean exit) {
        JsonObject root = new JsonObject();
        root.addProperty("text", "");
        JsonArray extra = new JsonArray();
        extra.add(translatable("message.nusmp.mega.system.ui.header", "[ SYSTEM ]", "aqua", false));
        extra.add(plain("\n"));
        String nameKey = exit ? LockText.RUNE_EXIT : LockText.RUNE_RELEASE;
        extra.add(translatable(nameKey, LockText.english(nameKey), "gold", true));
        extra.add(plain("\n"));
        String subKey = exit ? LockText.RUNE_EXIT_SUB : LockText.RUNE_RELEASE_SUB;
        extra.add(translatable(subKey, LockText.english(subKey), "gray", false));
        root.add("extra", extra);
        return root.toString();
    }

    private static JsonObject plain(String s) {
        JsonObject o = new JsonObject();
        o.addProperty("text", s);
        return o;
    }

    private static JsonObject translatable(String key, String fallback, String color, boolean bold) {
        JsonObject o = new JsonObject();
        o.addProperty("translate", key);
        o.addProperty("fallback", fallback);
        o.addProperty("color", color);
        if (bold) o.addProperty("bold", true);
        return o;
    }

    private static void glow(ServerLevel level, Vec3 pos, boolean exit) {
        var ring = exit ? Fx.options(FxThemes.DUNGEON_LOCK, FxOld.REVERSE_PORTAL) : Fx.options(FxThemes.DUNGEON_LOCK, FxOld.END_ROD);
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI * 2.0 / 12.0;
            level.sendParticles(ring, pos.x + Math.cos(a) * 0.9, pos.y + 0.1, pos.z + Math.sin(a) * 0.9, 1, 0.0, 0.4, 0.0, 0.03);
        }
        Fx.spray(level, FxThemes.DUNGEON_LOCK, FxOld.ENCHANT, pos.x, pos.y + 1.6, pos.z, 10, 0.5, 0.6, 0.5, 0.2);
    }

    /** All runes' keys (for the status command and the tests). */
    public static List<String> keys() { return new ArrayList<>(RUNES.keySet()); }
}
