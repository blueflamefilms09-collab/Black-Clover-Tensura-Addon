package com.newuniverse.nusmp.skill.icons;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/**
 * Why: the icon rework changes 121 files and a data index that nobody can look at in a game here, so the CI server smoke test checks it: {@code skillicons.index} (the index parses, stays inside its
 * bounds, every styled id is a registered skill and ships its picture and its classic copy, every animated id ships a strip whose height is frames x 32 and a .mcmeta) and {@code skillicons.bounds}
 * (the worst-case client cost numbers). Reads the jar's resources and the skill registry only: no world, no player, no client class, no tick work.
 */
public final class SkillIconSelfTests {
    private SkillIconSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("skillicons.index", SkillIconSelfTests::index);
        SelfTests.register("skillicons.bounds", s -> bounds());
    }

    private static boolean has(String path) { return SkillIconSelfTests.class.getResource("/assets/anime_multiverse/" + path) != null; }

    private static Result index(MinecraftServer server) {
        SkillIconIndex.Index idx = SkillIconIndex.load();
        if (idx.isEmpty()) return Result.fail("skill_icons.json is missing or invalid");
        for (String id : idx.styled()) {
            if (SkillAPI.getSkillRegistry().get(ResourceLocation.fromNamespaceAndPath("anime_multiverse", id)) == null) return Result.fail(id + " is styled but no such skill is registered");
            if (!has(SkillIconIndex.stillPath(id))) return Result.fail(id + ": the icon picture is missing");
            if (!has(SkillIconIndex.classicPath(id))) return Result.fail(id + ": the classic copy is missing");
        }
        for (String id : idx.animated()) {
            if (!has(SkillIconIndex.stripPath(id))) return Result.fail(id + ": the animated strip is missing");
            if (!has(SkillIconIndex.stripPath(id) + ".mcmeta")) return Result.fail(id + ": the strip's .mcmeta is missing");
        }
        return Result.ok(idx.styled().size() + " icons, " + idx.animated().size() + " animated, " + idx.frames() + " frames at " + idx.frameTime() + " ticks");
    }

    private static Result bounds() {
        SkillIconIndex.Index idx = SkillIconIndex.load();
        if (idx.frames() > SkillIconIndex.MAX_FRAMES || idx.styled().size() > SkillIconIndex.MAX_STYLED) return Result.fail("index out of bounds");
        // worst case upload rate: every animated icon changes frame every frameTime ticks, each upload is one 32 x 32 RGBA square (4 KiB)
        double uploadsPerSecond = idx.animated().size() * (20.0 / idx.frameTime());
        if (uploadsPerSecond > 400) return Result.fail("too many texture uploads a second: " + uploadsPerSecond);
        return Result.ok(String.format("at most %.0f uploads of 4 KiB a second (%.0f KiB/s), one blit per icon", uploadsPerSecond, uploadsPerSecond * 4));
    }
}
