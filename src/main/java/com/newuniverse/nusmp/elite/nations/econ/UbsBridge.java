package com.newuniverse.nusmp.elite.nations.econ;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.elite.nations.world.NationsConfig;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * Why: the OPTIONAL soft bridge to the UBS (Ultimate Banking System) mod ("UBS banking integration, UBS Cash & Interest hooks"). It is reflection only, with no compile-time dependency: it looks the mod up
 * by id ({@code economy.ubsModIds}) and, only if one is loaded AND the owner named the API class and its static methods in the config, calls them: the player's UBS cash (to pay a treasury deposit from UBS cash and to
 * pay a withdrawal into it) and an optional interest calculator. If UBS is absent, the class or a method is not found, or a call throws, every method answers "not available" (0 / false / -1) and
 * the treasury uses its own wallet and built-in interest. The UBS API could not be inspected when this was written (the original source and the mod were not available), so the method names
 * are configuration, not assumptions: nothing here is verified against a real UBS jar.
 */
public final class UbsBridge {
    private UbsBridge() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static boolean resolved, ok;
    private static Method balance, deposit, withdraw, interest;
    private static String resolvedKey = "";

    /** Is any configured UBS mod id loaded? */
    public static boolean modLoaded() {
        try {
            List<? extends String> ids = NationsConfig.get(NationsConfig.UBS_MOD_IDS);
            ModList ml = ModList.get();
            if (ml == null) return false;
            for (String id : ids) if (ml.isLoaded(id)) return true;
        } catch (RuntimeException ignored) { /* config or mod list not ready */ }
        return false;
    }

    private static synchronized boolean resolve() {
        String cls = NationsConfig.get(NationsConfig.UBS_API_CLASS).trim();
        String key = cls + "|" + NationsConfig.get(NationsConfig.UBS_BALANCE_METHOD) + "|" + NationsConfig.get(NationsConfig.UBS_DEPOSIT_METHOD) + "|"
                + NationsConfig.get(NationsConfig.UBS_WITHDRAW_METHOD) + "|" + NationsConfig.get(NationsConfig.UBS_INTEREST_METHOD);
        if (resolved && key.equals(resolvedKey)) return ok;
        resolved = true;
        resolvedKey = key;
        ok = false;
        balance = deposit = withdraw = interest = null;
        if (cls.isEmpty() || !modLoaded()) return false;
        try {
            Class<?> api = Class.forName(cls);
            balance = api.getMethod(NationsConfig.get(NationsConfig.UBS_BALANCE_METHOD), UUID.class);
            deposit = api.getMethod(NationsConfig.get(NationsConfig.UBS_DEPOSIT_METHOD), UUID.class, long.class);
            withdraw = api.getMethod(NationsConfig.get(NationsConfig.UBS_WITHDRAW_METHOD), UUID.class, long.class);
            String im = NationsConfig.get(NationsConfig.UBS_INTEREST_METHOD).trim();
            if (!im.isEmpty()) {
                try { interest = api.getMethod(im, long.class); } catch (NoSuchMethodException e) { LOG.warn("[nusmp] UBS bridge: interest method '{}' not found, using the built-in rate", im); }
            }
            ok = true;
            LOG.info("[nusmp] UBS bridge connected to {}", cls);
        } catch (ReflectiveOperationException | LinkageError e) {
            LOG.warn("[nusmp] UBS bridge: could not reach {} ({}); the treasury uses its own wallet and interest", cls, e.toString());
        }
        return ok;
    }

    public static boolean available() { return resolve(); }

    /** The player's UBS cash, or -1 when the bridge is not available. */
    public static long cash(UUID player) {
        if (!resolve()) return -1L;
        try { return ((Number) balance.invoke(null, player)).longValue(); } catch (ReflectiveOperationException | RuntimeException e) { return -1L; }
    }

    /** Takes UBS cash; false when not available, not enough, or on any error. */
    public static boolean take(UUID player, long amount) {
        if (amount <= 0 || !resolve()) return false;
        try {
            Object r = withdraw.invoke(null, player, amount);
            return !(r instanceof Boolean b) || b;
        } catch (ReflectiveOperationException | RuntimeException e) { return false; }
    }

    /** Gives UBS cash back (refund or withdrawal into UBS). */
    public static boolean give(UUID player, long amount) {
        if (amount <= 0 || !resolve()) return false;
        try {
            deposit.invoke(null, player, amount);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) { return false; }
    }

    /** The interest policy backed by UBS: asks its calculator, or -1 (then the caller falls back) when there is none. */
    public static InterestPolicy policy() {
        return (nationId, bal) -> {
            if (!resolve() || interest == null) return -1L;
            try { return Math.max(0L, ((Number) interest.invoke(null, bal)).longValue()); } catch (ReflectiveOperationException | RuntimeException e) { return -1L; }
        };
    }
}
