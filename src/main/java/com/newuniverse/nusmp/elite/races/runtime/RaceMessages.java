package com.newuniverse.nusmp.elite.races.runtime;

import com.newuniverse.nusmp.elite.races.state.EntryCondition.Reason;
import com.newuniverse.nusmp.elite.races.state.RaceEnglish;
import com.newuniverse.nusmp.elite.races.state.TransformMachine;
import com.newuniverse.nusmp.elite.races.state.TransformState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

/**
 * Why: every sentence the player reads about their transformations goes through one place and is built with {@code translatableWithFallback}, so the game works with no language
 * entry at all and the lead can merge {@code tools/elite/races_lang.json} (the same keys) to make the text editable and translatable. Keys: {@code nusmp.elite.race.state.<id>}
 * (a state's name), {@code nusmp.elite.race.why.<reason>} (why a state cannot start), {@code nusmp.elite.race.end.<event>} (why one ended), {@code nusmp.elite.race.msg.<key>}.
 */
public final class RaceMessages {
    private RaceMessages() {}

    public static MutableComponent stateName(TransformState s) { return Component.translatableWithFallback("nusmp.elite.race.state." + s.id, RaceEnglish.name(s)); }

    public static MutableComponent why(Reason r) { return Component.translatableWithFallback("nusmp.elite.race.why." + r.name().toLowerCase(Locale.ROOT), RaceEnglish.why(r)); }

    public static MutableComponent end(TransformMachine.Event e, TransformState was) {
        return Component.translatableWithFallback("nusmp.elite.race.end." + e.name().toLowerCase(Locale.ROOT), RaceEnglish.end(e), stateName(was));
    }

    public static MutableComponent msg(String key, String english, Object... args) {
        return Component.translatableWithFallback("nusmp.elite.race.msg." + key, english, args);
    }
}
