package com.newuniverse.nusmp.particle.fx;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.perf.PerfBudgets;
import com.newuniverse.nusmp.perf.PerfSettings;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Why: what the unit tests cannot prove (they run without the game), {@code particles.custom} proves inside the booted mod on the CI server: every particle of the catalog is registered under
 * {@code nusmp:<id>} and bound; the options of a type survive the real network codec (encode, decode, same colour and size); the theme table references only registered particles and
 * resolves an unknown key to the default; the facade sends with the switch on and with it off (the old vanilla particle of the mapping); and the per-tick budget is respected however many
 * hundred particles are asked for. It touches no client class, so it runs on the dedicated server, and it puts the switch back the way it found it.
 */
public final class FxSelfTests {
    private FxSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("particles.custom", FxSelfTests::custom);
    }

    private static SelfTests.Result custom(MinecraftServer server) {
        // ---- types registered and bound
        if (FxCatalog.all().size() < 60) return SelfTests.Result.fail("the catalog has only " + FxCatalog.all().size() + " particles");
        for (FxCatalog.Def d : FxCatalog.all()) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("nusmp", d.id());
            if (!BuiltInRegistries.PARTICLE_TYPE.containsKey(id)) return SelfTests.Result.fail("particle type not registered: " + id);
            if (FxTypes.type(d.id()) == null) return SelfTests.Result.fail("particle type not bound: " + id);
        }
        // ---- the real stream codec round trip
        TintedParticleType t = FxTypes.type("ember");
        TintedParticleOptions in = t.of(0x33AAFF, 1.5f);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess());
        @SuppressWarnings("unchecked") net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, TintedParticleOptions> sc = (net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, TintedParticleOptions>) t.streamCodec();
        sc.encode(buf, in);
        TintedParticleOptions out = sc.decode(buf);
        if (out.rgb() != 0x33AAFF || Math.abs(out.scale() - 1.5f) > 0.001f || out.getType() != t) return SelfTests.Result.fail("the options changed on the wire: " + out.rgb() + " " + out.scale());
        // ---- the theme table
        for (ParticleThemes.Theme th : ParticleThemes.table().themes().values()) {
            for (String p : th.particles()) if (FxTypes.type(p) == null) return SelfTests.Result.fail("theme " + th.id() + " names an unregistered particle " + p);
            for (String p : th.darkParticles()) if (FxTypes.type(p) == null) return SelfTests.Result.fail("theme " + th.id() + " (dark) names an unregistered particle " + p);
        }
        if (ThemeKey.of("no.such.theme.at.all").theme() != ParticleThemes.table().fallback()) return SelfTests.Result.fail("an unknown theme did not resolve to the default");
        if (!(Fx.options(ThemeKey.of("magic.FLAME"), FxSlot.MOTE, FxTone.LIGHT) instanceof TintedParticleOptions)) return SelfTests.Result.fail("magic.FLAME did not resolve to a custom particle");
        // ---- the facade with the switch on and off, and the budget
        ServerLevel level = server.overworld();
        double x = level.getSharedSpawnPos().getX() + 0.5, y = level.getSharedSpawnPos().getY() + 1.0, z = level.getSharedSpawnPos().getZ() + 0.5;
        ThemeKey flame = ThemeKey.of("magic.FLAME");
        try {
            FxConfig.force(true, true);
            int on = Fx.spray(level, flame, FxOld.END_ROD, x, y, z, 8, 0.2, 0.2, 0.2, 0.01);
            FxConfig.force(true, false);
            int off = Fx.spray(level, flame, FxOld.END_ROD, x, y, z, 8, 0.2, 0.2, 0.2, 0.01);
            if (on <= 0 || off <= 0) return SelfTests.Result.fail("the facade sent nothing (switch on: " + on + ", off: " + off + ")");
            if (!(FxOld.END_ROD.vanilla() instanceof net.minecraft.core.particles.SimpleParticleType)) return SelfTests.Result.fail("the vanilla fallback of END_ROD is not a vanilla particle");
            FxConfig.force(true, true);
            long now = level.getGameTime();
            int cap = PerfSettings.maxParticlesPerTick();
            for (int i = 0; i < 40; i++) Fx.spray(level, flame, FxOld.END_ROD, x, y, z, 400, 1.0, 1.0, 1.0, 0.05);
            if (level.getGameTime() == now && cap > 0 && PerfBudgets.PARTICLES.used() > cap) return SelfTests.Result.fail("the particle budget was exceeded: " + PerfBudgets.PARTICLES.used() + " of " + cap);
        } finally {
            FxConfig.force(true, null);
        }
        return SelfTests.Result.ok(FxCatalog.all().size() + " particle types registered and bound, codec round trip ok, " + ParticleThemes.table().themes().size() + " themes valid, facade on/off ok, budget respected");
    }
}
