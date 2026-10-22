package com.newuniverse.nusmp.vfx;

import java.util.List;
import java.util.Locale;

/**
 * Why (owner bug report, builds 1.110 / 1.111, docs/visual/flat_props.md): spells drew pale, flat, single-colour, translucent cut-outs of objects in the world (a sword, an axe with two half-moon
 * blades on a stick, a shield: the sprites of {@code WEAPON_CONSTRUCTS}; the paper figure of the Mirror doubles). The owner: "remove this from any and all spell that have it, its ugly".
 *
 * <p>This is the pure half of the fix (no Minecraft class, so it runs in the unit tests and on a dedicated server): the list of the flat object sprites, and the switch {@code visual.flatProps}
 * (CLIENT config, default OFF). With the switch off the VFX vertex buffer refuses to draw any quad of these textures (one gate for every layer, every spell, every other builder's layer too) and the
 * layers that owned such a sprite draw a solid shaded model instead (see {@code SolidParts}). With the switch on, the old sprites come back exactly as they were: nothing is deleted, the textures
 * and the old drawing code stay (owner rule 2).
 *
 * <p>The switch is a mirror of the client config: only {@code FlatPropsConfig} ever writes it, on the client; the dedicated server never reads it (it draws nothing).
 */
public final class FlatProps {
    private FlatProps() {}

    /** One flat object sprite: the texture name (under textures/particle), what it stands in for, and the spells that drew it. */
    public record Entry(String texture, String standsFor, List<String> spells) {}

    /** Every flat, single-colour object cut-out that the gate removes. Append only. */
    public static final List<Entry> FLAT = List.of(
            new Entry("construct_sword", "a sword (white cut-out)", List.of("Bone Armor", "Iron Maiden", "Creation: Arsenal", "Pack Hunt", "Hero sword float", "Hero shield ward", "Weapon arts constructs")),
            new Entry("construct_axe", "a double-bit axe (two white half-moons on a stick)", List.of("Bone Armor", "Iron Maiden", "Creation: Arsenal", "Pack Hunt", "Hero sword float", "Hero shield ward", "Weapon arts constructs")),
            new Entry("construct_shield", "a shield (a big white slab)", List.of("Bone Armor", "Iron Maiden", "Creation: Arsenal", "Pack Hunt", "Hero sword float", "Hero shield ward", "Weapon arts constructs")),
            new Entry("arc_silhouette", "a person (white paper figure of the caster)", List.of("Mirror: Real Double", "Mirror: Real Double (step out of the glass)")),
            new Entry("scorpse_hand", "a hand (flat white billboard)", List.of("Soul Corpse: Grave Field")),
            new Entry("scorpse_grave", "a grave (flat white billboard)", List.of("Soul Corpse: Grave Field")),
            new Entry("scorpse_shard", "a bone shard (flat white billboard)", List.of("Soul Corpse: bone shards")),
            new Entry("soul_skull", "a skull (flat white billboard)", List.of("Soul Magic", "Soul Corpse")));

    private static volatile boolean drawFlat = false;

    /** True when the player turned {@code visual.flatProps} on: the flat sprites are drawn again. Default false. */
    public static boolean drawFlat() { return drawFlat; }

    /** Written by the client config only. */
    public static void setDrawFlat(boolean on) { drawFlat = on; }

    /** The bare name of a texture path or location: {@code textures/particle/construct_axe.png} -> {@code construct_axe}. */
    public static String nameOf(String texturePath) {
        if (texturePath == null) return "";
        String p = texturePath;
        int colon = p.indexOf(':');
        if (colon >= 0) p = p.substring(colon + 1);
        int slash = p.lastIndexOf('/');
        if (slash >= 0) p = p.substring(slash + 1);
        if (p.endsWith(".png")) p = p.substring(0, p.length() - 4);
        return p.toLowerCase(Locale.ROOT);
    }

    /** True for a texture of the flat list (a particle texture: the path must be under textures/particle, or a bare name). */
    public static boolean isFlat(String texturePath) {
        if (texturePath == null) return false;
        String p = texturePath;
        int colon = p.indexOf(':');
        if (colon >= 0) p = p.substring(colon + 1);
        if (p.contains("/") && !p.startsWith("textures/particle/")) return false;
        String n = nameOf(p);
        for (Entry e : FLAT) if (e.texture().equals(n)) return true;
        return false;
    }

    /** The one decision: may a quad of this texture be drawn, given the switch? Anything not on the list is always drawn. */
    public static boolean shouldDraw(String texturePath, boolean flatPropsOn) {
        return flatPropsOn || !isFlat(texturePath);
    }
}
