package com.newuniverse.nusmp.charter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The world's drop lock as plain data (pure): for each boss id, who got the Charter Grimoire first, under which name and on which day. It is
 * immutable by construction: there is no public way to unset a claim, and {@link #markDropped} answers true for the first caller only, so two
 * kills in one tick can never both award it. The one exception is {@link #adminClear}, package-private, which only the operator command reaches.
 */
public final class DropLedger {
    /** The first recipient of a grimoire. {@code day} counts from 1. */
    public record Claim(UUID id, String name, long day) {}

    private final Map<String, Claim> claims = new LinkedHashMap<>();

    public synchronized boolean hasDropped(String bossId) { return bossId != null && claims.containsKey(bossId); }

    public synchronized Claim claim(String bossId) { return bossId == null ? null : claims.get(bossId); }

    /** A snapshot (a copy, in claim order): safe to walk while another thread claims. */
    public synchronized Map<String, Claim> all() { return Collections.unmodifiableMap(new LinkedHashMap<>(claims)); }

    /** Sets the lock. True only if this call set it (the boss was unclaimed); a second call changes nothing. Atomic: of any number of racing callers exactly one gets true. */
    public synchronized boolean markDropped(String bossId, UUID id, String name, long day) {
        if (bossId == null || bossId.isEmpty() || claims.containsKey(bossId)) return false;
        claims.put(bossId, new Claim(id, name == null ? "" : name, day));
        return true;
    }

    /** Loading a saved claim; same rules as {@link #markDropped} (a saved claim is never overwritten). */
    void restore(String bossId, UUID id, String name, long day) { markDropped(bossId, id, name, day); }

    /** The operator escape hatch for a lost grimoire. Returns the claim that was removed, or null. */
    synchronized Claim adminClear(String bossId) { return bossId == null ? null : claims.remove(bossId); }
}
