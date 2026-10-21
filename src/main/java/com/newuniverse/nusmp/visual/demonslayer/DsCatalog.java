package com.newuniverse.nusmp.visual.demonslayer;

import java.util.List;

/**
 * The facts about the upgraded Genesis Demon-Slayer that the self-test and the unit tests share (pure, no Minecraft classes): its id, the three acts it has a signature burst for, the
 * animation groups that carry the stack-driven kinds, and the placement of the old renderer that the upgraded mesh must keep (so every display transform of the item model still fits).
 */
public final class DsCatalog {
    private DsCatalog() {}

    public static final String ID = "demon_slayer_sword";
    /** The acts of the item (docs/demon_slayer_genesis_spec.md): a full swing, sneak + right-click, right-click. */
    public static final List<String> ACTS = List.of("conceptual_severance", "black_meteorite", "black_divider");
    /** The shards of the blade (Black Meteorite), the light between them, and the pommel groups that follow the Resonance. */
    public static final List<String> SHARDS = List.of("s1", "s2", "s3"), CRACKS = List.of("crack_1", "crack_2", "crack_3"), BLOOM = List.of("clover", "bloom");
    /** The old renderer's placement (DemonSlayerMesh#draw): translate(0.516, 0.523, 0.5), rotate z -45 degrees, scale 0.98 / 64, translate(0, -11.75, 0) as ox, oy, angle, scale, ymin. */
    public static final float[] PLACE = {0.516f, 0.523f, 45.0f, 0.98f / 64f, 11.75f};
}
