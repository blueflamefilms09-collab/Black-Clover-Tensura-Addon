package com.newuniverse.nusmp.client.ex;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client options of the EX-Singularity look (the grade above Genesis, docs/relics/ex_singularity.md): config/nusmp-ex-singularity-client.toml. They change how things LOOK only (rule 9), so they
 * are CLIENT settings; the grade itself, what it is worth and what never rolls it are server facts and have no switch.
 * <ul>
 *   <li>{@code exSingularity.prismatic} (default true): the whole prismatic look: the animated rainbow tooltip text and border, the sword's cycling glow, the blade veil and the orbiting shards.
 *       Off = the look Shishou had before the grade existed (plain grade colour in the tooltip, the one blue glow, nothing extra on the blade).</li>
 *   <li>{@code exSingularity.slotFrame} (default true): the cycling frame round the item in the inventory and hotbar.</li>
 *   <li>{@code exSingularity.particles} (default true): the prismatic motes round the carrier, the sword's body and the sword on the ground (they also follow the effect quality and the client
 *       particle budget of nusmp-performance-client.toml).</li>
 * </ul>
 * Registered from {@code NUSMP} (one line); every read is guarded, so a file that is not loaded yet gives the defaults.
 */
public final class ExSingularityOptions {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue PRISMATIC, SLOT_FRAME, PARTICLES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("exSingularity");
        PRISMATIC = b.comment("The prismatic look of the EX-Singularity grade (Shishou): the rainbow tooltip text and border, the sword's cycling glow, the blade veil and the orbiting shards. Off = the old plain look.")
                .define("prismatic", true);
        SLOT_FRAME = b.comment("The cycling prismatic frame round the EX-Singularity item in the inventory and the hotbar. Needs exSingularity.prismatic.")
                .define("slotFrame", true);
        PARTICLES = b.comment("The prismatic motes round the carrier, the sword's body and the sword on the ground. They follow the effect quality and the client particle budget. Needs exSingularity.prismatic.")
                .define("particles", true);
        b.pop();
        SPEC = b.build();
    }

    private ExSingularityOptions() {}

    public static boolean prismatic() {
        try { return PRISMATIC.get(); } catch (IllegalStateException notLoaded) { return true; }
    }

    public static boolean slotFrame() {
        try { return prismatic() && SLOT_FRAME.get(); } catch (IllegalStateException notLoaded) { return true; }
    }

    public static boolean particles() {
        try { return prismatic() && PARTICLES.get(); } catch (IllegalStateException notLoaded) { return true; }
    }
}
