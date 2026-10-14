package com.newuniverse.nusmp.relics.socket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.newuniverse.nusmp.relics.socket.Boon.*;

/**
 * The table of everything a socket can hold: six Spirits (the Black Clover spirits Salamander, Undine, Sylph and Gnome, Tensura's Ifrit, and the two
 * elements both worlds share, light and dark) and the nine Underworld Devils of the mod ({@code blackclover.Devil}: Liebe, Zagred, Megicula, Lucifero,
 * Beelzebub, Astaroth, Lilith, Nahamah, Walgner). Pure data: the skill, attribute and effect ids are verified Tensura or vanilla ids (the skill ids
 * against the Tensura id dump), and a missing id is skipped silently at run time like every other relic passive.
 * <p>
 * Balance in one place: passives are modest and permanent while the relic is equipped; the real power is the active ability, which costs magicules and
 * has a cooldown. Entries are only appended.
 */
public final class SocketCatalog {
    private SocketCatalog() {}

    private static final Map<String, SocketDef> BY_ID = new LinkedHashMap<>();
    private static final List<SocketDef> SPIRITS = new ArrayList<>(), DEVILS = new ArrayList<>();
    /** Relic ids that get a socket although their slot or grade would give none (see {@link SocketRules#slots}); a wave adds its own with {@link #allowSocket}. */
    private static final Set<String> OPT_IN = Collections.synchronizedSet(new HashSet<>());

    private static void spirit(String key, String name, SocketElement el, List<Boon> boons, ActSpec act) {
        add(new SocketDef(SocketKind.SPIRIT, key, name, el, el.rgb(), el.fx(), List.copyOf(boons), act));
    }

    private static void devil(String key, String name, SocketElement look, int rgb, SocketFxKind fx, List<Boon> boons, ActSpec act) {
        add(new SocketDef(SocketKind.DEVIL, key, name, look, rgb, fx, List.copyOf(boons), act));
    }

    private static void add(SocketDef d) {
        BY_ID.put(d.id(), d);
        (d.kind() == SocketKind.SPIRIT ? SPIRITS : DEVILS).add(d);
    }

    static {
        // ---- the six Spirits
        spirit("fire", "Ifrit, Spirit of Fire", SocketElement.FIRE,
                List.of(effect("minecraft:fire_resistance", 0), skill("tensura:flame_attack_resistance")),
                new ActSpec("hellflare", "Hellflare", 400, 6, 14));
        spirit("water", "Undine, Spirit of Water", SocketElement.WATER,
                List.of(effect("minecraft:water_breathing", 0), skill("tensura:water_attack_resistance")),
                new ActSpec("tidal_surge", "Tidal Surge", 360, 5, 10));
        spirit("wind", "Sylph, Spirit of Wind", SocketElement.WIND,
                List.of(effect("minecraft:slow_falling", 0), attr("minecraft:generic.movement_speed", 0.06, OP_MULTIPLY_TOTAL), skill("tensura:wind_attack_resistance")),
                new ActSpec("gale_step", "Gale Step", 240, 4, 6));
        spirit("earth", "Gnome, Spirit of Earth", SocketElement.EARTH,
                List.of(attr("minecraft:generic.knockback_resistance", 0.5, OP_ADD), skill("tensura:earth_attack_resistance")),
                new ActSpec("stone_bulwark", "Stone Bulwark", 500, 5, 10));
        spirit("light", "the Spirit of Light", SocketElement.LIGHT,
                List.of(effect("minecraft:night_vision", 0), skill("tensura:light_attack_resistance")),
                new ActSpec("radiant_mend", "Radiant Mend", 600, 6, 10));
        spirit("dark", "the Spirit of Shadow", SocketElement.DARK,
                List.of(effect("minecraft:night_vision", 0), skill("tensura:darkness_attack_resistance")),
                new ActSpec("umbral_veil", "Umbral Veil", 500, 5, 0));

        // ---- the nine Devils (the mod's own list: blackclover.Devil)
        devil("liebe", "Liebe, Devil of Anti-Magic", SocketElement.LIGHT, 0xE8E8FF, SocketFxKind.LIGHT,
                List.of(resist("minecraft:magic", 0.15), resist("minecraft:indirect_magic", 0.15)),
                new ActSpec("null_field", "Null Field", 600, 6, 0));
        devil("zagred", "Zagred, Devil of the Word", SocketElement.DARK, 0xB06AFF, SocketFxKind.WISP,
                List.of(attr("tensura:chant_speed", 0.15, OP_MULTIPLY_TOTAL)),
                new ActSpec("word_of_halt", "Word of Halt", 500, 6, 0));
        devil("megicula", "Megicula, Devil of Curses", SocketElement.DARK, 0x7A2AB8, SocketFxKind.SMOKE,
                List.of(resist("minecraft:wither", 0.5), resist("minecraft:magic", 0.10)),
                new ActSpec("curse_warding_magic", "Curse-Warding Magic", 700, 7, 0));
        devil("lucifero", "Lucifero, Devil of Gravity", SocketElement.DARK, 0x4A1A78, SocketFxKind.SMOKE,
                List.of(skill("tensura:gravity_attack_resistance"), resist("minecraft:fall", 0.5)),
                new ActSpec("crushing_weight", "Crushing Weight", 560, 7, 10));
        devil("beelzebub", "Beelzebub, Devil of Space", SocketElement.WIND, 0xA060FF, SocketFxKind.WISP,
                List.of(skill("tensura:spatial_attack_resistance")),
                new ActSpec("rend_space", "Rend Space", 300, 5, 0));
        devil("astaroth", "Astaroth, Devil of Time", SocketElement.LIGHT, 0xF5D76E, SocketFxKind.LIGHT,
                List.of(attr("minecraft:generic.attack_speed", 0.10, OP_MULTIPLY_TOTAL)),
                new ActSpec("time_slip", "Time Slip", 640, 7, 0));
        devil("lilith", "Lilith, Devil of Ice", SocketElement.WATER, 0xA0E8FF, SocketFxKind.DROPLET,
                List.of(skill("tensura:cold_resistance")),
                new ActSpec("freezing_aura", "Freezing Aura", 480, 6, 8));
        devil("nahamah", "Nahamah, Devil of Fire", SocketElement.FIRE, 0xFF3A10, SocketFxKind.EMBER,
                List.of(effect("minecraft:fire_resistance", 0), skill("tensura:flame_attack_resistance")),
                new ActSpec("scorching_aura", "Scorching Aura", 440, 6, 12));
        devil("walgner", "Walgner, Devil of Blood", SocketElement.FIRE, 0xB01020, SocketFxKind.DROPLET,
                List.of(lifesteal(0.08)),
                new ActSpec("blood_tithe", "Blood Tithe", 520, 6, 12));
    }

    public static List<SocketDef> all() { return List.copyOf(BY_ID.values()); }

    public static List<SocketDef> spirits() { return List.copyOf(SPIRITS); }

    public static List<SocketDef> devils() { return List.copyOf(DEVILS); }

    /** By stored id ("spirit:fire"); null if unknown. */
    public static SocketDef byId(String id) { return id == null ? null : BY_ID.get(id); }

    /** A bare key ("fire", "megicula") or a full id; null if unknown. Spirits win when a key would be both. */
    public static SocketDef byKey(String keyOrId) {
        if (keyOrId == null) return null;
        SocketDef d = BY_ID.get(keyOrId);
        if (d != null) return d;
        for (SocketDef s : BY_ID.values()) if (s.key().equals(keyOrId)) return s;
        return null;
    }

    /** Makes a relic (by registry path) carry a socket whatever its slot or grade (at LEGEND and above it still gets two). */
    public static void allowSocket(String relicId) { if (relicId != null) OPT_IN.add(relicId); }

    public static boolean isOptIn(String relicId) { return relicId != null && OPT_IN.contains(relicId); }
}
