package com.newuniverse.nusmp.elite.races.runtime;

import com.newuniverse.nusmp.elite.races.EliteRacesConfig;
import com.newuniverse.nusmp.elite.races.state.TransformSpec;
import com.newuniverse.nusmp.elite.races.state.TransformState;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Why: a transformation must be SEEN. The look is built only from what the owner allows: the mod's own VFX shapes (the spirit aura that follows the body, the shadow-dark aura for
 * the Great Ape, a light flare when a bright form begins) and Light / Dark Rukh through {@code RukhFx} (never a vanilla particle). Light Rukh for the ki, ascended and divine
 * forms, Dark Rukh for the Great Ape; the colour comes from the template. Everything goes through the VFX packet and the Rukh budget of the server, so a crowd of transformed
 * players cannot flood it, and the whole thing is off when the config says {@code vfx = false}. The renderer for the aura shapes is the mod's existing one: no new client class.
 */
public final class RaceAura {
    private RaceAura() {}

    private static float power(TransformState s) {
        return switch (s) {
            case GREAT_APE -> 2.2f;
            case DIVINE -> 1.9f;
            case ASCENDED -> 1.5f;
            case BOOSTED -> 1.1f;
            case BASE -> 1.0f;
        };
    }

    private static Alignment align(TransformSpec spec) { return Alignment.of(spec.darkRukh()); }

    private static void follow(ServerPlayer p, TransformSpec spec, int ticks) {
        ServerLevel l = p.serverLevel();
        VfxShape shape = spec.darkRukh() ? VfxShape.SHADOW_UNITE : VfxShape.SPIRIT_AURA;
        float pw = power(spec.state());
        VfxSpawn.sendFollowing(l, shape, p, p.position().add(0, 1.0 * pw, 0), spec.color(), ticks, pw);
    }

    /** The moment a form begins. */
    public static void enter(ServerPlayer p, TransformSpec spec) {
        if (!EliteRacesConfig.vfx()) return;
        ServerLevel l = p.serverLevel();
        follow(p, spec, 70);
        RukhFx.burst(l, p, align(spec), 12);
        if (!spec.darkRukh()) {
            Vec3 at = p.position().add(0, 1.0, 0);
            VfxSpawn.send(l, VfxShape.LIGHT_FLARE, at, at, spec.color(), 24, power(spec.state()));
        }
    }

    /** Once a second while the form lasts: keeps the aura alive (its VFX lives 70 ticks) and a little Rukh round the body. */
    public static void pulse(ServerPlayer p, TransformSpec spec) {
        if (!EliteRacesConfig.vfx()) return;
        follow(p, spec, 70);
        RukhFx.aura(p.serverLevel(), p, align(spec));
    }

    /** The moment a form ends. */
    public static void leave(ServerPlayer p, TransformSpec spec) {
        if (!EliteRacesConfig.vfx() || spec == null) return;
        RukhFx.burst(p.serverLevel(), p, align(spec), 6);
    }
}
