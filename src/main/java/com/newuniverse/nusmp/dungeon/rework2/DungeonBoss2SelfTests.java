package com.newuniverse.nusmp.dungeon.rework2;

import com.newuniverse.nusmp.boss.rules.BossRulesConfig;
import com.newuniverse.nusmp.dungeon.rework.GuardianBrain;
import com.newuniverse.nusmp.dungeon.rework.GuardianRules;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.labyrinth.mega.guard.GuardianScaling;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import com.newuniverse.nusmp.particle.fx.ThemeKey;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: dungeon-boss rework 2 (docs/dungeon_bosses_rework2.md) adds a sound set, nine Guardian kits and a colourway per sector trait, and nobody watches a fight in CI, so the server smoke test checks the
 * pieces under key {@code dboss2}: every cue of the catalogue is a registered sound event; the nine kits exist and each resolves to its own particle theme; a Guardian of each of the nine themes (built, never
 * added to the world, so nothing needs removing) syncs the colourway the setting says; the Guardian's move book is consistent and its worst-case damage is under the ceiling the rules test also holds; a
 * brain builds for every kit; and a boss's sound limiter lets a cue through once and then holds it for its cooldown. Server safe and cheap.
 */
public final class DungeonBoss2SelfTests {
    private DungeonBoss2SelfTests() {}

    public static void register() { SelfTests.register("dboss2", DungeonBoss2SelfTests::run); }

    private static Result run(MinecraftServer server) {
        ServerLevel level = server.overworld();
        List<String> bad = new ArrayList<>();
        for (DungeonBossSoundRules.Cue c : DungeonBossSoundRules.CUES) if (DungeonBossSounds.event(c.id()) == null) bad.add("cue " + c.id() + " has no registered sound event");
        if (GuardianRules.KITS.size() != GuardianScaling.Trait.values().length) bad.add("there are " + GuardianRules.KITS.size() + " kits for " + GuardianScaling.Trait.values().length + " traits");
        for (GuardianRules.Kit k : GuardianRules.KITS) {
            String id = ThemeKey.of(k.theme()).theme().id();
            if (!k.theme().equals(id)) bad.add("kit " + k.id() + " resolves to the theme " + id + " instead of its own");
            if (DungeonBossSoundRules.cue(k.beginCue()) == null || DungeonBossSoundRules.cue(k.hitCue()) == null || DungeonBossSoundRules.cue(k.accentCue()) == null) bad.add("kit " + k.id() + " names a cue that does not exist");
            if (new GuardianBrain(k).busy()) bad.add("a new brain of " + k.id() + " is busy");
        }
        boolean on = BossRulesConfig.dungeonGuardians();
        RegionTheme[] themes = {RegionTheme.GRAVITY, RegionTheme.BLOOD, RegionTheme.SPATIAL, RegionTheme.FLAME, RegionTheme.FROST, RegionTheme.VERDANT, RegionTheme.STORM, RegionTheme.SHADOW, RegionTheme.ASTRAL};
        for (RegionTheme theme : themes) {
            LabyrinthGuardianEntity g = LabyrinthGuardianEntity.create(level, BlockPos.ZERO, null, null);
            if (g == null) {
                bad.add("no Guardian for " + theme);
                continue;
            }
            g.configure(-1, 4, theme, null);
            int want = on ? GuardianScaling.traitOf(theme).ordinal() + 1 : 0;
            if (g.themeKit() != want) bad.add(theme.id().toUpperCase(Locale.ROOT) + " Guardian syncs colourway " + g.themeKit() + ", expected " + want);
        }
        double worst = GuardianRules.worstCaseSharePerSecond(3, 5);
        if (worst > 1.0) bad.add("the Guardian's worst-case damage is " + worst + " attack shares per second");
        for (var m : GuardianRules.BOOK) if (m.windup() <= 0 || m.cooldown() < com.newuniverse.nusmp.dungeon.rework.DungeonReworkRules.MIN_COOLDOWN) bad.add(m.id() + " has no wind-up or too short a cooldown");
        DungeonBossSoundRules.Limiter lim = new DungeonBossSoundRules.Limiter();
        if (!lim.allow("slam", 100) || lim.allow("slam", 101)) bad.add("the sound limiter does not hold a cue for its cooldown");
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(6, bad.size()))));
        return Result.ok(DungeonBossSoundRules.CUES.size() + " cues, " + GuardianRules.KITS.size() + " guardian kits, guardians " + (on ? "on" : "off") + ", sounds " + (BossRulesConfig.dungeonSounds() ? "on" : "off")
                + ", worst case " + String.format(Locale.ROOT, "%.2f", worst) + " attack shares per second");
    }
}
