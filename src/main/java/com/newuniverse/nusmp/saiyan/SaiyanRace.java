package com.newuniverse.nusmp.saiyan;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: Saiyan as a race with four stages (Dragon Ball rework, phase 2), the server glue of {@link SaiyanRaceRules}. It is built NEXT TO the existing Saiyan flag, which stays the source of truth: a player
 * is a Saiyan if {@code SaiyanState.saiyan} says so (the questionnaire, {@code /saiyan} and the race check all keep working), and the stage is one more saved number. With the server switch
 * {@code phase.race.realRace} off (the default) nothing here changes anything. A real Tensura race entry could not be registered because the race API could not be read in this build (see
 * docs/dragon_ball/phase_report.md). Cost: once a second per Saiyan, one small pure check and at most five attribute lookups; God Ki is read by the radar through {@link #hasGodKi}.
 */
final class SaiyanRace {
    private SaiyanRace() {}

    /** True for a player who carries God Ki: a Super Saiyan God branch form is on, or the Divine stage was reached (race on). */
    static boolean hasGodKi(LivingEntity e) {
        if (!(e instanceof ServerPlayer sp)) return false;
        SaiyanState s = SaiyanStore.peek(sp);
        if (s == null || !s.saiyan) return false;
        if (SaiyanPhaseConfig.realRace() && s.raceStage >= SaiyanRaceRules.DIVINE) return true;
        SaiyanManager.Run r = SaiyanManager.peekRun(sp);
        SaiyanForms.Form f = r == null ? null : SaiyanFormEngine.active(r);
        return f != null && f.branch() == SaiyanForms.Branch.GOD;
    }

    /** The stage this Saiyan would be at now (the saved stage with the server's numbers). */
    static int stageNow(SaiyanState s) {
        return SaiyanRaceRules.next(s.raceStage, SaiyanPhaseConfig.raceNeed(), new SaiyanRaceRules.Progress(s.battlePower, s.zenkai, s.unlocked, s.flags));
    }

    /** Once a second: the stage can only rise, and its small buff is put on (or taken off again when the race is switched off). */
    static void second(ServerPlayer p, SaiyanState s, SaiyanManager.Run r) {
        SaiyanTechniques4.State t = r.tech4;
        if (!SaiyanPhaseConfig.realRace()) {
            if (t.raceApplied != 0) {
                SaiyanAttributes.applyRace(p, SaiyanRaceRules.bonus(SaiyanRaceRules.NONE, 0.0));
                t.raceApplied = 0;
            }
            return;
        }
        int stage = stageNow(s);
        if (stage != s.raceStage) {
            s.raceStage = stage;
            r.dirty = true;
            p.displayClientMessage(Component.translatable("anime_multiverse.saiyan.msg.race_stage", Component.translatable(SaiyanRaceRules.nameKey(stage))), false);
        }
        SaiyanAttributes.applyRace(p, SaiyanRaceRules.bonus(stage, SaiyanPhaseConfig.raceScale()));
        t.raceApplied = stage;
    }

    /** The operator sets a stage by hand (kept between 1 and 4; it can also go down here, because it is an operator's decision). */
    static void set(ServerPlayer p, SaiyanState s, int stage) {
        s.raceStage = Math.max(SaiyanRaceRules.SAIYAN, SaiyanRaceRules.clampStage(stage));
        SaiyanManager.markDirty(p);
    }
}
