package com.newuniverse.nusmp.elite.skills.evolution;

import java.util.List;

/**
 * Why: some evolutions are only open to someone who currently carries a particular title ("wear the title, then the skill answers"). The active title comes from the records and
 * titles subsystem through {@code EliteHooks.activeTitle}. Params: {@code title} (one id) or {@code titles} (any of these). With no parameter it passes for any non-empty
 * active title.
 */
public final class ActiveTitleEvolution implements EvolutionTrigger {
    @Override public String id() { return "active_title_evolution"; }

    @Override
    public boolean test(EvolutionContext ctx, Params p, String skillId) {
        String active = ctx.activeTitle();
        if (active == null || active.isEmpty()) return false;
        List<String> wanted = p.strings("titles");
        if (p.has("title")) wanted.add(p.str("title", ""));
        return wanted.isEmpty() || wanted.contains(active);
    }

    @Override
    public String describe(Params p, String skillId) {
        List<String> wanted = p.strings("titles");
        if (p.has("title")) wanted.add(p.str("title", ""));
        return wanted.isEmpty() ? "wear a title" : "wear the title " + String.join(" or ", wanted);
    }
}
