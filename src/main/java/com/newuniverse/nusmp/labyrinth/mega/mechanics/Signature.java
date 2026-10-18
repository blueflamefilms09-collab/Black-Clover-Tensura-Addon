package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Why: the owner wants a thousand DIFFERENT mechanics, not renamed copies. The behaviour signature is the definition of "different" the guard test enforces: the scope, the
 * trigger (kind and arguments), the counter (kind and arguments), the reward, and every effect in order (primitive, delay, chance, repeats and ALL arguments with the
 * defaults filled in), with numbers reduced to {@link Scaling#bucket buckets} at the first and the last floor, so "damage 6" and "damage 7" are the same behaviour but "damage 2"
 * and "damage 8" are not. Name, text, source, tiers, themes, road, cost and weight are NOT in the signature: two mechanics that differ only there behave the same. The same rule
 * is written in {@code tools/mechanics/gen_mechanics.py} (which stores the hash in each mechanic as {@code sig}); a test compares them so the generator and the game agree. Pure.
 */
public final class Signature {
    private Signature() {}

    /** The signature text of a mechanic. */
    public static String of(MechanicDef d, OpCatalog cat) {
        StringBuilder b = new StringBuilder();
        b.append(d.scope().id()).append(';');
        b.append("t:").append(kind(d.trigger().kind(), d.trigger().args(), cat.trigger(d.trigger().kind())));
        b.append(";c:").append(kind(d.counter().kind(), d.counter().args(), cat.counter(d.counter().kind())));
        b.append(";r:").append(d.reward());
        for (MechanicDef.Effect e : d.effects()) {
            OpCatalog.OpSpec spec = cat.op(e.op());
            b.append(";e:").append(e.op()).append('[').append(num(e.delay())).append(',').append(num(e.chance() * 10.0)).append(',').append(e.times()).append(',').append(num(e.every())).append("](");
            b.append(args(e.args(), spec == null ? List.of() : spec.args())).append(')');
        }
        return b.toString();
    }

    /** The first 12 hex digits of the SHA-1 of the signature text: what the generator stores as {@code sig}. */
    public static String hash(String signature) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-1").digest(signature.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) sb.append(String.format("%02x", h[i] & 0xff));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String kind(String kind, Args a, OpCatalog.KindSpec spec) {
        return kind + "(" + args(a, spec == null ? List.of() : spec.args()) + ")";
    }

    private static String num(double x) { return Integer.toString(Scaling.bucket(x)); }

    /** Arguments with the defaults filled in, sorted by name, numbers as "bucket at floor 1 : bucket at floor 1000". */
    static String args(Args a, List<OpCatalog.ArgSpec> specs) {
        TreeSet<String> names = new TreeSet<>(a.raw().keySet());
        for (OpCatalog.ArgSpec s : specs) names.add(s.name());
        List<String> parts = new ArrayList<>();
        for (String n : names) {
            OpCatalog.ArgSpec s = null;
            for (OpCatalog.ArgSpec x : specs) if (x.name().equals(n)) s = x;
            Object v = a.raw().containsKey(n) ? a.raw().get(n) : (s == null ? null : s.def());
            if (v instanceof Double dd) parts.add(n + "=" + Scaling.bucket(dd) + ":" + Scaling.bucket(dd));
            else if (v instanceof double[] ab) parts.add(n + "=" + Scaling.bucket(Scaling.value(ab[0], ab[1], 1)) + ":" + Scaling.bucket(Scaling.value(ab[0], ab[1], Scaling.MAX_FLOOR)));
            else if (v instanceof Boolean bo) parts.add(n + "=" + (bo ? "T" : "F"));
            else if (v instanceof String st) parts.add(n + "=" + st);
        }
        return String.join(",", parts);
    }
}
