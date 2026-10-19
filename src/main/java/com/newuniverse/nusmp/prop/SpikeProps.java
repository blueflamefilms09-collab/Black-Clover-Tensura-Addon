package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.book.ShakudoKit;
import com.newuniverse.nusmp.prop.ShakudoPropKit.*;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;

/**
 * 1.93.0 Spike Magic: the server behaviours of this magic's two constructs (the animation and the look are in client.prop.SpikePropPainter; the
 * numbers and the shared behaviours are in {@link ShakudoPropKit}).
 * <ul>
 *   <li>SPIKE_1, the pillar: a cluster of spikes that thrusts out of the ground after 8 ticks under whoever stood there (impale, Slowness) and keeps pricking whatever stays on it.</li>
 *   <li>SPIKE_2, the urchin: a spiked ball that homes in on a target, bursts on it in a 3 block ring and throws its spikes (Slowness).</li>
 * </ul>
 * Also registers the armour's damage hook ({@link ShakudoKit#hooks}).
 */
public final class SpikeProps {
    private SpikeProps() {}

    public static final int COLOR = 0xFF9BA8B9;

    /** Called once by PropRegistry. */
    public static void init() {
        ShakudoKit.hooks();
        MagicProps.register(PropKind.SPIKE_1, new ShakudoPropKit.Strike(new StrikeCfg("spike-pillar", 1.2f, 2.4f, 8, 1.1, 8f, TensuraDamageTypes.EARTH_ELEMENTAL, false, 0, 10, 3f, ShakudoPropKit.slow(40, 1), COLOR, VfxShape.SPIKE_FX3)));
        MagicProps.register(PropKind.SPIKE_2, new ShakudoPropKit.Missile(new MissileCfg("spike-urchin", 0.9f, 0.9f, 0.8, 6f, 3.0, TensuraDamageTypes.EARTH_ELEMENTAL, 0.12, false, ShakudoPropKit.slow(40, 1), COLOR, VfxShape.SPIKE_FX3)));
    }
}
