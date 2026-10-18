package com.newuniverse.nusmp.compat.maps;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceKey;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Why: players must not be able to claim chunks in the dungeons with FTB Chunks (a claim keeps everybody else from using blocks, which would lock altars and NPCs of the Hub and let a
 * party wall off a room of the Labyrinth). FTB Chunks has exactly one supported hook for this: the cancellable {@code ClaimedChunkEvent.BEFORE_CLAIM} (and {@code BEFORE_LOAD}, force
 * loading), an Architectury event whose handler returns a failed {@code ClaimResult}; FTB then refuses the claim in the map GUI and the command with that result's message. This mod
 * has no compile-time dependency on FTB Chunks or Architectury (and must keep loading without them), so the handler is registered by reflection with a {@link Proxy} of FTB's
 * {@code Before} interface, once, when the server is about to start and only when {@code ftbchunks} is loaded. The decision itself is {@link ClaimBlockRules} over the config list
 * ({@code compat.maps.ftbClaimBlockedDimensions}, default every dimension of this mod); anything that goes wrong inside the handler lets the claim through (FTB Chunks must never break because of this
 * mod) and anything that goes wrong while registering leaves {@link #active()} false, so the operators' warning of {@link FtbChunksGuard} stays on. Unclaiming is never blocked.
 */
public final class FtbClaimBlock {
    private FtbClaimBlock() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** The translation key FTB shows for a blocked claim (FTB translates {@code ClaimResult.getResultId()} on the client, so the key is in this mod's lang file). */
    public static final String MESSAGE_KEY = "message.nusmp.maps.ftb_claims_blocked";

    private static volatile boolean registered, active;
    private static volatile boolean warnedHandler;
    private static Method customProblem, pass, interruptFalse;

    /** Did the handler get registered with FTB Chunks? */
    public static boolean active() { return active; }

    public static void onServerAboutToStart(ServerAboutToStartEvent e) { register(); }

    /** Registers the handler (once). Never throws. */
    public static synchronized void register() {
        if (registered) return;
        registered = true;
        try {
            ModList l = ModList.get();
            if (l == null || !l.isLoaded("ftbchunks")) return;
            Class<?> events = Class.forName("dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent");
            Class<?> before = Class.forName("dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent$Before");
            Class<?> result = Class.forName("dev.architectury.event.CompoundEventResult");
            Class<?> claimResult = Class.forName("dev.ftb.mods.ftbchunks.api.ClaimResult");
            Class<?> event = Class.forName("dev.architectury.event.Event");
            customProblem = claimResult.getMethod("customProblem", String.class);
            pass = result.getMethod("pass");
            interruptFalse = result.getMethod("interruptFalse", Object.class);
            Method register = event.getMethod("register", Object.class);
            Object handler = Proxy.newProxyInstance(before.getClassLoader(), new Class<?>[]{before}, (proxy, method, args) -> {
                if (method.getName().equals("before") && args != null && args.length == 2) return decide((CommandSourceStack) args[0], args[1]);
                return switch (method.getName()) {
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "nusmp FTB Chunks claim block";
                    default -> null;
                };
            });
            for (String field : new String[]{"BEFORE_CLAIM", "BEFORE_LOAD"}) register.invoke(events.getField(field).get(null), handler);
            active = true;
            LOG.info("[nusmp] FTB Chunks: claiming and force-loading are blocked in {}", MapsConfig.get(MapsConfig.FTB_BLOCKED_DIMENSIONS));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            LOG.warn("[nusmp] Could not hook FTB Chunks' claim events (claims in the dungeons are NOT blocked; add \"nusmp:*\" to claim_dimension_blacklist instead): {}", ex.toString());
        }
    }

    /** The handler: the CompoundEventResult FTB expects (pass = no objection, interruptFalse = refuse with this result). */
    private static Object decide(CommandSourceStack source, Object claimedChunk) throws ReflectiveOperationException {
        try {
            if (MapsConfig.get(MapsConfig.BLOCK_FTB_CLAIMS)) {
                String dim = dimensionOf(source, claimedChunk);
                if (dim != null && ClaimBlockRules.blocked(dim, MapsConfig.get(MapsConfig.FTB_BLOCKED_DIMENSIONS)))
                    return interruptFalse.invoke(null, customProblem.invoke(null, MESSAGE_KEY));
            }
        } catch (InvocationTargetException | RuntimeException ex) {
            if (!warnedHandler) {
                warnedHandler = true;
                LOG.warn("[nusmp] The FTB Chunks claim check failed (the claim is allowed; further failures are not logged)", ex);
            }
        }
        return pass.invoke(null);
    }

    /** The dimension of the chunk being claimed: from the chunk's position, else from where the source stands; null when neither can be read. */
    static String dimensionOf(CommandSourceStack source, Object claimedChunk) {
        try {
            Object pos = claimedChunk.getClass().getMethod("getPos").invoke(claimedChunk);
            Object key = pos.getClass().getMethod("dimension").invoke(pos);
            if (key instanceof ResourceKey<?> rk) return rk.location().toString();
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // fall through to the source
        }
        try {
            return source == null ? null : source.getLevel().dimension().location().toString();
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
