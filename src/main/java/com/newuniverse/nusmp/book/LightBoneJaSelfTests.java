package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.skill.NUSkills;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: nothing of the Light and Bone Magic remake can be played by its author, so what can be proved without a person is proved by the real game: the CI server smoke test boots the mod and
 * runs this (key {@code magic.light_bone_ja}, pure and server safe, no client class). It checks the two registered books against the table of {@link LightBoneJa} (the old page ids at their old
 * places, the new ones appended, unique ids, the family cap), the caps of the casts, and the prop budget on the real server: every variant of both prop kinds is spawned for a stand-in player
 * and ticked, then far more props than the caps allow are asked for and the live number must stay under the per-player and the server caps, and every prop must end.
 */
public final class LightBoneJaSelfTests {
    private LightBoneJaSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.light_bone_ja", LightBoneJaSelfTests::run);
    }

    private static Result run(MinecraftServer server) {
        if (!LightBoneJa.capsHold()) return Result.fail("a count of the casts is over its cap");
        StringBuilder sb = new StringBuilder();
        for (String magic : new String[]{"LIGHT", "BONE"}) {
            GrimoireBook b = (magic.equals("LIGHT") ? NUSkills.BOOK_LIGHT : NUSkills.BOOK_BONE).get();
            List<BookPage> pages = b.pages();
            List<String> want = LightBoneJa.ids(magic, LightBoneJaKit.enabled());
            for (int k = 0; k < want.size(); k++)
                if (k >= pages.size() || !pages.get(k).id().equals(want.get(k))) return Result.fail(magic + ": page " + k + " should be " + want.get(k) + " but is " + (k < pages.size() ? pages.get(k).id() : "missing"));
            Set<String> ids = new HashSet<>();
            for (BookPage p : pages) if (!ids.add(p.id())) return Result.fail(magic + ": the page id " + p.id() + " is used twice");
            if (b.familyCount() < want.size()) return Result.fail(magic + ": " + b.familyCount() + " family pages, expected " + want.size() + " (character pages may follow them: builder gold, Karna)");
            for (String old : LightBoneJa.ids(magic, false))
                if (!ids.contains(old)) return Result.fail(magic + ": the old page id " + old + " is gone");
            for (LightBoneJa.Spell s : LightBoneJa.spells(magic))
                if (!ids.contains(s.id()) && LightBoneJaKit.enabled()) return Result.fail(magic + ": the table spell " + s.id() + " has no page");
            sb.append(magic.toLowerCase()).append(' ').append(b.familyCount()).append(" pages; ");
        }
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 3, 0);
        ServerPlayer p = SelfTests.player(level, "light_bone_ja", at.x, at.y, at.z);
        List<MagicPropEntity> made = new ArrayList<>();
        int[] light = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, bone = {0, 1, 2, 3};
        for (int v : light) {
            MagicPropEntity e = LightBoneJaKit.prop(p, PropKind.LIGHT_JA, v | LightBoneJa.speedBits(2.0), at, new Vec3(0, 0, 1), 1f, 20);
            if (e == null) return Result.fail("LIGHT_JA variant " + v + ": the spawn was refused");
            made.add(e);
        }
        for (int v : bone) {
            MagicPropEntity e = LightBoneJaKit.prop(p, PropKind.BONE_JA, v | LightBoneJa.speedBits(2.0), at, new Vec3(0, 0, 1), 1f, 20);
            if (e == null) return Result.fail("BONE_JA variant " + v + ": the spawn was refused");
            made.add(e);
        }
        for (int k = 0; k < 3 * LightBoneJa.MAX_PROPS_PER_CASTER; k++) {                       // far more than the cap: the live number must stay under it
            MagicPropEntity e = LightBoneJaKit.prop(p, PropKind.LIGHT_JA, LightBoneJa.speedBits(2.0), at, new Vec3(0, 0, 1), 1f, 40);
            if (e != null) made.add(e);
        }
        int live = LightBoneJaBudget.live(p.getUUID());
        if (live > LightBoneJa.MAX_PROPS_PER_CASTER) return Result.fail(live + " live props for one caster, the cap is " + LightBoneJa.MAX_PROPS_PER_CASTER);
        if (LightBoneJaBudget.liveTotal() > LightBoneJa.MAX_PROPS_GLOBAL) return Result.fail("the server holds " + LightBoneJaBudget.liveTotal() + " props, the cap is " + LightBoneJa.MAX_PROPS_GLOBAL);
        for (MagicPropEntity e : made) if (!e.isRemoved()) e.tick();
        int stuck = 0;
        for (MagicPropEntity e : made) if (!e.isRemoved()) { e.expire(); if (!e.isRemoved()) stuck++; }
        if (stuck > 0) return Result.fail(stuck + " of " + made.size() + " props did not end");
        if (LightBoneJaBudget.live(p.getUUID()) != 0) return Result.fail("props are still counted after they ended");
        return Result.ok(sb + "props capped at " + live + " for one caster; " + made.size() + " spawned, ticked and ended");
    }
}
