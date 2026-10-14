package com.newuniverse.nusmp.relics.socket;

import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.function.ToIntFunction;

/**
 * The effective tier (1 Normal to 7 Genesis) of a relic stack. The tier-ascension system keeps a raised tier on the stack and answers it with
 * {@code Tiers.effectiveGrade}; the socket system must work with and without it, so the answer is looked up once by reflection (the usual homes of
 * that class are tried) and falls back to the def's own grade. {@link #useSource} lets the integration set the source directly instead.
 * Never throws; safe on both sides.
 */
public final class SocketGrades {
    private SocketGrades() {}

    private static volatile ToIntFunction<ItemStack> source;
    private static volatile boolean probed;
    private static volatile Method effective;

    /** Sets where the effective tier comes from (a function from the stack to 1 to 7). */
    public static void useSource(ToIntFunction<ItemStack> fn) { source = fn; }

    public static int tier(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 1;
        RelicDef def = Relics.defOf(stack);
        int base = def == null ? 1 : def.grade().tier();
        ToIntFunction<ItemStack> s = source;
        try {
            if (s != null) return clamp(s.applyAsInt(stack), base);
            Method m = method();
            if (m != null) {
                Object r = m.invoke(null, stack);
                if (r instanceof Grade g) return Math.max(base, g.tier());
                if (r instanceof Integer i) return clamp(i, base);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // fall through to the def's grade
        }
        return base;
    }

    /** The def's grade or higher: a raised tier never lowers a relic. */
    private static int clamp(int v, int base) { return Math.max(base, Math.min(7, v)); }

    private static Method method() {
        if (probed) return effective;
        probed = true;
        for (String cn : new String[]{"com.newuniverse.nusmp.relics.Tiers", "com.newuniverse.nusmp.relics.tier.Tiers"}) {
            try {
                Method m = Class.forName(cn).getMethod("effectiveGrade", ItemStack.class);
                effective = m;
                return m;
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // try the next home
            }
        }
        return null;
    }
}
