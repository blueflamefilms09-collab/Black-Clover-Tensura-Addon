package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.FlatProps;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why (docs/visual/flat_props.md): {@code visual.flatProps} in {@code config/nusmp-flatprops-client.toml} (a CLIENT config: it only changes how things look). Off (the default): the flat, pale, single-colour
 * cut-outs of swords, axes, shields and paper figures are never drawn; the spells that owned them draw solid models. On: the old cut-outs are drawn again, exactly as before. The value is mirrored into the
 * pure {@link FlatProps} switch when the file loads or reloads; the dedicated server never registers this config.
 *
 * <p>{@code visual.bigBows} (default true) and {@code visual.bowScale} (default 1.0, 0.5 to 2.0) are two more keys of the same file: the bigger bows in the hand ({@link com.newuniverse.nusmp.visual.sizes.BowSize}).
 *
 * <p>{@code visual.heroSolidParts} (default true, docs/heroweapon/flat_parts.md) is a second key of the same file: the solid arrows and spear tips of the hero weapons; false restores the old flat look ({@link com.newuniverse.nusmp.vfx.HeroSolid}).
 */
public final class FlatPropsConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue FLAT_PROPS;
    private static final ModConfigSpec.BooleanValue HERO_SOLID_PARTS;
    private static final ModConfigSpec.BooleanValue BIG_BOWS;
    private static final ModConfigSpec.DoubleValue BOW_SCALE;
    private static final ModConfigSpec.BooleanValue SHIELD_STRAPS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("visual");
        FLAT_PROPS = b.comment("Draw the old flat, pale, translucent cut-outs of swords, axes, shields and the paper figure of the Mirror doubles. Off = solid shaded models instead (recommended).")
                .define("flatProps", false);
        HERO_SOLID_PARTS = b.comment("Draw the arrows of the Legendary Bow and the tips of the spears and the harpoon as solid 3D parts (a shaft, a diamond head, three vanes; a leaf blade with a ridge), in the hand and in flight. "
                        + "Off = the old flat look: the hairline arrow, the paper-thin spear head and the plain glow streak (docs/heroweapon/flat_parts.md).")
                .define("heroSolidParts", true);
        BIG_BOWS = b.comment("Draw every bow of the mod bigger in the hand, in third and first person (the Legendary Bow about 1.5 blocks, its forms 1.4, the relic bows 1.35; vanilla's bow is 1.08), "
                        + "held at the same grip point. Off = the sizes of before exactly (docs/heroweapon/bow_size.md).")
                .define("bigBows", true);
        BOW_SCALE = b.comment("A multiplier on top of visual.bigBows (0.5 to 2.0, default 1.0) to tune the bow size without a rebuild. In first person a bow never grows past what the camera allows. Ignored while bigBows is off.")
                .defineInRange("bowScale", 1.0, 0.5, 2.0);
        SHIELD_STRAPS = b.comment("Draw leather straps on the arm of whoever holds a shield: two bands round the forearm, a grip bar and two tabs to the plate (docs/visual_upgrade/shield_fix.md). Off = the shields are drawn without them, as before 4.72.0.")
                .define("shieldStraps", true);
        b.pop();
        SPEC = b.build();
    }

    private FlatPropsConfig() {}

    /** Client side only: registers the file and keeps the pure switch in step with it. */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "nusmp-flatprops-client.toml");
        modBus.addListener((ModConfigEvent.Loading e) -> apply());
        modBus.addListener((ModConfigEvent.Reloading e) -> apply());
    }

    static void apply() {
        try { FlatProps.setDrawFlat(FLAT_PROPS.get()); } catch (IllegalStateException notLoaded) { FlatProps.setDrawFlat(false); }
        try { com.newuniverse.nusmp.vfx.HeroSolid.setSolid(HERO_SOLID_PARTS.get()); } catch (IllegalStateException notLoaded) { com.newuniverse.nusmp.vfx.HeroSolid.setSolid(true); }
        try { com.newuniverse.nusmp.visual.sizes.BowSize.setBig(BIG_BOWS.get()); } catch (IllegalStateException notLoaded) { com.newuniverse.nusmp.visual.sizes.BowSize.setBig(true); }
        try { com.newuniverse.nusmp.visual.sizes.BowSize.setScale(BOW_SCALE.get()); } catch (IllegalStateException notLoaded) { com.newuniverse.nusmp.visual.sizes.BowSize.setScale(1.0); }
        try { com.newuniverse.nusmp.visual.shields.ShieldStraps.setEnabled(SHIELD_STRAPS.get()); } catch (IllegalStateException notLoaded) { com.newuniverse.nusmp.visual.shields.ShieldStraps.setEnabled(true); }
    }
}
