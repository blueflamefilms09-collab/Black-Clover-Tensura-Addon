package com.newuniverse.nusmp.client.ui.codex;

import com.newuniverse.nusmp.boss.budget.BossBudgetConfig;
import com.newuniverse.nusmp.core.discovery.DiscoveryConfig;
import com.newuniverse.nusmp.labyrinth.LabyrinthConfig;
import com.newuniverse.nusmp.labyrinth.dnd.DndConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Why: a rule entry of the Codex ({@code codex/rules/<id>.json}) can show what the rule is set to right now. This small registry maps a rule id
 * (the file name) to a supplier of the value text, so a rule that has a config switch gets a live "Now: ON" line with no Java in the page. The
 * server configs are not synced by this mod, so a value that cannot be read on the client (a dedicated server) falls back to the config's default
 * and says "(default)" ({@link CodexRuleValue}); no packet is invented. Another mod or a later release registers more rules with {@link #register}.
 * An entry with no registered supplier simply shows no value line.
 */
public final class CodexRuleState {
    private CodexRuleState() {}

    private static final Map<String, Supplier<String>> STATE = new ConcurrentHashMap<>();

    public static void register(String ruleId, Supplier<String> value) { STATE.put(ruleId, value); }

    /** The value text of a rule entry given its "namespace:name" id (or the bare name); null when the rule has none. */
    public static String valueFor(String entryId) {
        if (STATE.isEmpty()) defaults();
        String name = CodexEntryParser.nameOf(entryId == null ? "" : entryId);
        Supplier<String> s = STATE.get(name);
        if (s == null) return null;
        try { return s.get(); } catch (RuntimeException | LinkageError e) { return null; }
    }

    // ---------------------------------------------------------------- reading a config value on the client
    public static String bool(ModConfigSpec.BooleanValue v) {
        try { return CodexRuleValue.bool(v.get(), true); } catch (IllegalStateException notLoaded) { return CodexRuleValue.bool(v.getDefault(), false); }
    }

    public static String percent(ModConfigSpec.DoubleValue v) {
        try { return CodexRuleValue.percent(v.get(), true); } catch (IllegalStateException notLoaded) { return CodexRuleValue.percent(v.getDefault(), false); }
    }

    public static String times(ModConfigSpec.DoubleValue v) {
        try { return CodexRuleValue.number(v.get(), "x", true); } catch (IllegalStateException notLoaded) { return CodexRuleValue.number(v.getDefault(), "x", false); }
    }

    public static String seconds(ModConfigSpec.IntValue v) {
        try { return CodexRuleValue.number(v.get(), "s", true); } catch (IllegalStateException notLoaded) { return CodexRuleValue.number(v.getDefault(), "s", false); }
    }

    /** The stock rules that map to a config switch. Ids are the file names under assets/nusmp/codex/rules/. */
    private static synchronized void defaults() {
        if (!STATE.isEmpty()) return;
        register("relics_exclusive", () -> bool(LabyrinthConfig.RELICS_EXCLUSIVE));
        register("labyrinth_unbreakable", () -> bool(LabyrinthConfig.UNBREAKABLE));
        register("boss_damage_cap", () -> bool(BossBudgetConfig.ENABLED) + ", " + percent(BossBudgetConfig.FRACTION) + " of max health per second");
        register("hp_buffer", () -> bool(DndConfig.HP_BUFFER) + ", " + times(DndConfig.HP_SCALE) + " health");
        register("vital_strike", () -> bool(DndConfig.VITAL));
        register("hunger_and_gloom", () -> bool(DndConfig.GLOOM) + ", gloom after " + seconds(DndConfig.GLOOM_SECONDS) + " in the dark");
        register("discovery_hides", () -> DiscoveryConfig.showAll() ? "everything shown (showAll is on)" : "hidden until found");
    }
}
