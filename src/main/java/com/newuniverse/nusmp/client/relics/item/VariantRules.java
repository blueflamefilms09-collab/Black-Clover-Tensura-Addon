package com.newuniverse.nusmp.client.relics.item;

import java.util.ArrayList;
import java.util.List;

/**
 * The pure half of the NBT / state driven item layers: which variants apply (the decision table), which mesh ids to try, and the colour and glow
 * maths. No Minecraft types, so every row is tested. {@link ItemStateVariants} feeds it from the stack and the holder.
 * <ul>
 *   <li><b>GENESIS</b>: the stack is a Genesis-grade relic, or its holder carries the Genesis body flag. Mesh &lt;id&gt;_genesis if the weapon ships one,
 *       otherwise the same mesh with a glow that pulses {@link #GENESIS_BOOST} times faster and wider, plus an extra additive emissive pass.</li>
 *   <li><b>FIVE LEAF</b>: the holder owns a grimoire of five leaves or more and the weapon is a Black Clover one. Mesh &lt;id&gt;_5leaf if shipped,
 *       otherwise a violet-black tint of the glow (and a dimmed body).</li>
 * </ul>
 * The two are independent and may both apply. A holder-based variant needs a known holder (a hand context); the stack's own grade needs none.
 */
public final class VariantRules {
    private VariantRules() {}

    public static final float GENESIS_BOOST = 1.6f;
    public static final int LEAVES_FOR_FIVE_LEAF = 5;
    /** Body multiplier of a five-leaf weapon (ARGB). */
    public static final int BODY_TINT_5LEAF = 0xFFB8A2D8;
    /** The violet the glow is pushed toward, and how far (0 to 1); then it is darkened. */
    public static final int VIOLET = 0x5A1E9E;
    public static final float VIOLET_MIX = 0.65f;
    public static final float VIOLET_DARKEN = 0.8f;

    public enum Tag { NONE, GENESIS, FIVE_LEAF }

    /** What the decision needs to know. */
    public record Inputs(boolean stackGenesis, boolean holderGenesis, int holderLeaves, boolean blackCloverWeapon, boolean holderKnown) {}

    /** Which variants apply. */
    public record Decision(boolean genesis, boolean fiveLeaf) {
        public static final Decision NONE = new Decision(false, false);
        public boolean any() { return genesis || fiveLeaf; }
    }

    public static Decision decide(Inputs in) {
        boolean genesis = in.stackGenesis() || (in.holderKnown() && in.holderGenesis());
        boolean five = in.blackCloverWeapon() && in.holderKnown() && in.holderLeaves() >= LEAVES_FOR_FIVE_LEAF;
        return new Decision(genesis, five);
    }

    /** One mesh id to try and the variant it embodies. */
    public record Candidate(String meshId, Tag tag) {}

    /**
     * The meshes to try, best first. For every id in the state chain ({@code RangedStage.chain}) the variant meshes come before the plain one:
     * x_pull2_genesis, x_pull2_5leaf, x_pull2, x_pull1_genesis, ... x_genesis, x_5leaf, x. So a drawn bow keeps its draw stage when only the rest
     * pose has a variant mesh.
     */
    public static List<Candidate> candidates(List<String> stateChain, Decision d) {
        List<Candidate> out = new ArrayList<>(stateChain.size() * 3);
        for (String id : stateChain) {
            if (d.genesis()) out.add(new Candidate(id + "_genesis", Tag.GENESIS));
            if (d.fiveLeaf()) out.add(new Candidate(id + "_5leaf", Tag.FIVE_LEAF));
            out.add(new Candidate(id, Tag.NONE));
        }
        return out;
    }

    /** The glow has to be intensified (the weapon ships no genesis mesh of its own). */
    public static boolean intensify(Decision d, Tag chosen) { return d.genesis() && chosen != Tag.GENESIS; }

    /** The glow has to be tinted violet-black (no five-leaf mesh of its own). */
    public static boolean tint(Decision d, Tag chosen) { return d.fiveLeaf() && chosen != Tag.FIVE_LEAF; }

    // ---------------------------------------------------------------- colour and glow maths
    /**
     * The glow brightness factor at a time: the mesh's own pulse {@code min + (max - min) * (0.5 + 0.5 sin(time * speed))}; with a boost the speed
     * is multiplied and the swing about the middle widened by the same factor. Never below 0, never above 1.5 (the channel clamp does the rest).
     */
    public static float pulse(float time, float speed, float min, float max, float boost) {
        float mid = 0.5f * (min + max);
        float base = min + (max - min) * (0.5f + 0.5f * (float) Math.sin(time * speed * boost));
        float k = boost == 1f ? base : mid + (base - mid) * boost;
        return Math.max(0f, Math.min(1.5f, k));
    }

    /** The violet-black glow colour (0xRRGGBB) for a glow colour. */
    public static int tintGlow(int rgb) {
        int r = mix(rgb >> 16 & 255, VIOLET >> 16 & 255), g = mix(rgb >> 8 & 255, VIOLET >> 8 & 255), b = mix(rgb & 255, VIOLET & 255);
        return (int) (r * VIOLET_DARKEN) << 16 | (int) (g * VIOLET_DARKEN) << 8 | (int) (b * VIOLET_DARKEN);
    }

    private static int mix(int from, int to) { return Math.round(from + (to - from) * VIOLET_MIX); }

    /** Alpha (0 to 255) of the extra additive emissive pass of an intensified glow: it breathes between about 60 and 160. */
    public static int emissiveAlpha(float time) { return Math.round(110f + 50f * (float) Math.sin(time * 0.21f)); }

    /** A glow channel after the brightness factor, clamped to a byte. */
    public static int channel(int value, float k) { return Math.max(0, Math.min(255, (int) (value * k))); }
}
