package com.newuniverse.nusmp.core.magic.profile;

import com.newuniverse.nusmp.blackclover.MagicType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The registry that answers, for ANY magic, a complete {@link MagicProfile}. Order of the answer:
 * <ol>
 *   <li>an explicit profile registered for the id ({@link #register}, {@link #override}), which is how a merged magic and any
 *       hand-tuned magic gets its own presentation;</li>
 *   <li>else the profile derived from the {@link MagicType} constant ({@link EnumMagicProfile});</li>
 *   <li>else the final safe fallback ({@link FallbackMagicProfile}: GLOW particle, generic wiki-style effect, Resistance guard).</li>
 * </ol>
 * It never returns null and never throws, whatever it is asked (null, blank, a stale id from a save file). Thread safe (the client
 * thread reads while the server thread registers).
 */
public final class MagicProfiles {
    private MagicProfiles() {}

    private static final Map<String, MagicProfile> OVERRIDES = new ConcurrentHashMap<>();
    private static final Map<String, MagicProfile> DERIVED = new ConcurrentHashMap<>();
    private static final MagicProfile FALLBACK = new FallbackMagicProfile("UNKNOWN");

    /** The profile of an enum constant. A null constant gives the fallback. */
    public static MagicProfile of(MagicType type) {
        if (type == null) return FALLBACK;
        try {
            MagicProfile o = OVERRIDES.get(type.name());
            if (o != null) return o;
            MagicProfile d = DERIVED.get(type.name());
            if (d == null) { d = new EnumMagicProfile(type); DERIVED.put(type.name(), d); }
            return d;
        } catch (Throwable t) {
            return new FallbackMagicProfile(type.name());     // never cached; keeps the constant's own id, so restricted() still answers by name
        }
    }

    /** The profile of any id: an enum name (any case), a merged id, or something unknown (the fallback profile for that id). */
    public static MagicProfile of(String id) {
        try {
            String c = MagicKeys.canonical(id);
            if (c.isEmpty()) return FALLBACK;
            MagicProfile o = OVERRIDES.get(c);
            if (o != null) return o;
            if (!MagicKeys.isMerged(c)) {
                MagicType t = MagicType.lookup(c);
                if (t != null) return of(t);
            }
            MagicProfile d = DERIVED.get(c);
            if (d == null) { d = new FallbackMagicProfile(c); DERIVED.put(c, d); }
            return d;
        } catch (Throwable t) {
            return FALLBACK;
        }
    }

    /** Registers an explicit profile for its id (replacing an earlier one). A null or id-less profile is ignored. */
    public static void register(MagicProfile profile) {
        if (profile == null) return;
        String c = MagicKeys.canonical(profile.id());
        if (!c.isEmpty()) OVERRIDES.put(c, profile);
    }

    /** Registers an override of the DERIVED profile of {@code type}: only what the lambda sets is replaced. */
    public static void override(MagicType type, Consumer<ProfileOverride> edit) {
        if (type == null) return;
        DERIVED.computeIfAbsent(type.name(), k -> new EnumMagicProfile(type));
        register(ProfileOverride.of(DERIVED.get(type.name()), edit));
    }

    /** Removes the explicit profile of an id (a removed merged magic); the derived / fallback answer applies again. */
    public static void unregister(String id) {
        String c = MagicKeys.canonical(id);
        OVERRIDES.remove(c);
        DERIVED.remove(c);
    }

    public static boolean hasOverride(String id) { return OVERRIDES.containsKey(MagicKeys.canonical(id)); }

    /** The final fallback profile. */
    public static MagicProfile fallback() { return FALLBACK; }

    /** The resolved profile of every enum constant, in enum order (what the audit checks). */
    public static List<MagicProfile> allEnum() {
        List<MagicProfile> l = new ArrayList<>();
        for (MagicType t : MagicType.values()) l.add(of(t));
        return l;
    }

    /** Every explicitly registered profile (merged magics and hand overrides). */
    public static List<MagicProfile> registered() { return new ArrayList<>(OVERRIDES.values()); }
}
