package com.newuniverse.nusmp.fairytail;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.rules.Tier;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;

/**
 * Why: nothing here can be played by the author, so the CI server smoke test runs this in the real game ({@code /multiverse mega selftest
 * skill.fairytail}). It checks what only the game can: every line is registered as a ManasCore skill with the right type and mode count,
 * the Fire Dragon King repeats the Fire Dragon Slayer's spells in the same order (a stored mode keeps its meaning through the evolution),
 * every form has its armour style, the book item exists and teaches each line, and a real cast of a ring spell by a stand-in player goes
 * through the whole pipeline (balance law, effect packets, fault guard) and reports that it happened.
 */
public final class FairyTailSelfTests {
    private FairyTailSelfTests() {}

    public static void register() { SelfTests.register("skill.fairytail", FairyTailSelfTests::run); }

    static Result run(MinecraftServer server) {
        Set<String> ids = new HashSet<>();
        for (FtLine line : FtLine.values()) {
            ManasSkill s = FairyTailSystem.skill(line);
            if (s == null) return Result.fail(line.id + ": not registered");
            if (!(s instanceof FtSkill ft) || ft.line() != line) return Result.fail(line.id + ": wrong skill class");
            if (!ids.add(line.id)) return Result.fail(line.id + ": duplicate id");
            if (line.modes() < 6 || line.modes() > 12) return Result.fail(line.id + ": " + line.modes() + " modes");
            Set<String> moveIds = new HashSet<>();
            for (FtMove mv : line.moves()) {
                if (!moveIds.add(mv.id())) return Result.fail(line.id + ": duplicate spell " + mv.id());
                if (mv.hurts() && mv.damage() <= 0) return Result.fail(line.id + "." + mv.id() + ": hurts but has no damage");
                if (mv.kind() == FtKind.FORM && FtForms.get(mv.form()) == null) return Result.fail(line.id + "." + mv.id() + ": no armour style");
                if (FtWorld.shape(line.element.cast) == null) return Result.fail(line.id + ": no cast effect");
            }
        }
        FtLine base = FtLine.FIRE_DRAGON_SLAYER, king = FtLine.FIRE_DRAGON_KING;
        for (int i = 0; i < base.modes(); i++) {
            if (!base.move(i).id().equals(king.move(i).id())) return Result.fail("the Fire Dragon King changed spell " + i + " of the Fire Dragon Slayer");
        }
        if (king.modes() <= base.modes()) return Result.fail("the Fire Dragon King adds no forms");
        if (Aura.valueOf("FAIRY_TAIL").ordinal() < 0) return Result.fail("no armour aura");
        if (FairyTailSystem.BOOK.get() == null) return Result.fail("no skill book item");
        for (FtLine line : FtLine.values()) {
            if (FtBookItem.lineOf(FtBookItem.of(line)) != line) return Result.fail(line.id + ": the book does not name its skill");
        }
        ServerLevel level = server.overworld();
        ServerPlayer fake = SelfTests.player(level, "ft_selftest", 0.5, level.getMaxBuildHeight() - 10, 0.5);
        FtMove ring = base.move(3);
        FtCast.Outcome out = FtCast.cast(base, ring, new CompoundTag(), fake, Tier.BASE, 1.0);
        if (!out.done()) return Result.fail("a ring spell by a stand-in player did not happen");
        return Result.ok(FtLine.values().length + " skills checked, ring cast ok");
    }
}
