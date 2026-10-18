package com.newuniverse.nusmp.relics.order;

import com.newuniverse.nusmp.relics.RelicDef;

import java.util.function.Function;

/** Why: {@code RelicDef.UNIQUE_OVERRIDE} is package-private to the relic core (the core must not depend on the Order), so this one-method bridge in the Order's package is the only thing that sets it. */
final class RelicDefHook {
    private RelicDefHook() {}

    static void set(Function<String, Boolean> f) { RelicDef.setUniqueOverride(f); }
}
