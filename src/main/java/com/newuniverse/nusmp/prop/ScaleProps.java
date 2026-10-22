package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.book.ShakudoKit;
import com.newuniverse.nusmp.prop.ShakudoPropKit.*;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;

/**
 * 1.93.0 Scale Magic: the server behaviours of this magic's two constructs (the animation and the look are in client.prop.ScalePropPainter; the
 * numbers and the shared behaviours are in {@link ShakudoPropKit}).
 * <ul>
 *   <li>SCALE_1, the serpent: a wyrm of ten scaled segments that follows its owner, slithers to the nearest hostile and bites (Weakness and Slowness).</li>
 *   <li>SCALE_2, the shield: a kite shield of scales that hovers in front of its owner and turns back every projectile that flies at them, owned by the caster.</li>
 * </ul>
 * Also registers the armour's damage hook ({@link ShakudoKit#hooks}). Scale casting (docs/magic/scale_casting.md) adds SCALE_BALANCE, the one floating balance scale of the caster, and the
 * suppression aura's hooks (scalecast.ScaleCastKit).
 */
public final class ScaleProps {
    private ScaleProps() {}

    public static final int COLOR = 0xFF63BDB5;

    /** Called once by PropRegistry. */
    public static void init() {
        ShakudoKit.hooks();
        MagicProps.register(PropKind.SCALE_1, new ShakudoPropKit.Companion(new CompanionCfg("scale-serpent", 1.0f, 0.9f, 0.30, 2.8, 6, 18, 26, 6f, 1.7, TensuraDamageTypes.WATER_ELEMENTAL, ShakudoPropKit.all(ShakudoPropKit.effect(net.minecraft.world.effect.MobEffects.WEAKNESS, 80, 0), ShakudoPropKit.slow(40, 1)), 14, COLOR, VfxShape.SCALE_FX3)));
        MagicProps.register(PropKind.SCALE_2, new ShakudoPropKit.Guard(new GuardCfg("scale-shield", 1.5f, 1.9f, 1.4, false, 0f, 1.9, true, 0f, COLOR, VfxShape.SCALE_FX3)));
        MagicProps.register(PropKind.SCALE_BALANCE, com.newuniverse.nusmp.scalecast.ScaleCastKit.BEHAVIOR);       // Scale casting: the floating balance scale
        com.newuniverse.nusmp.scalecast.ScaleCastKit.register();                                                  // its damage / spell power hooks and clean-ups
        com.newuniverse.nusmp.scalecast.ScaleCastSelfTests.register();                                           // the CI server smoke test runs magic.scale_casting
    }
}
