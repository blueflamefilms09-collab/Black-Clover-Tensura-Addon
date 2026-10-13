package com.newuniverse.nusmp.relics.wave;

import java.util.function.IntUnaryOperator;
import java.util.function.ToIntFunction;

/** Wave 6: the nine charge pools of the wave: a data key, a maximum and what one dawn gives back (pure; the dice are the tabletop's). */
public enum Wave6Pools {
    SKY("sky", Wave6Tune.SKY_MAX, Wave6Tune::skyDawnGain),
    SLIME("slime", Wave6Tune.SLIME_USES, r -> Wave6Tune.SLIME_USES),
    DAWN("dawn", Wave6Tune.DAWN_MAX, Wave6Tune::dawnGain),
    VOID("void", Wave6Tune.ASTRAL_VOID_USES, r -> Wave6Tune.ASTRAL_VOID_USES),
    SHADOW("shadow", Wave6Tune.SHADOW_USES, r -> Wave6Tune.SHADOW_USES),
    MIST("mist", Wave6Tune.MIST_MAX, Wave6Tune::mistGain),
    MAGE("mage", Wave6Tune.MAGE_MAX, r -> 1),
    PLOT("plot", Wave6Tune.PLOT_MAX, r -> 1),
    ZAR("zar", Wave6Tune.ZAR_USES, r -> Wave6Tune.ZAR_USES);

    private final String key;
    private final int max;
    private final ToIntFunction<IntUnaryOperator> gain;

    Wave6Pools(String key, int max, ToIntFunction<IntUnaryOperator> gain) { this.key = key; this.max = max; this.gain = gain; }

    public String key() { return key; }

    public int max() { return max; }

    /** What one dawn gives back. */
    public int gain(IntUnaryOperator roll) { return gain.applyAsInt(roll); }
}
