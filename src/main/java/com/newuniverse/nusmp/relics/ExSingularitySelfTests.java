package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.shishou.ShishouItems;
import com.newuniverse.nusmp.tooltip.UnifiedTooltipItems;
import com.newuniverse.nusmp.tooltip.UnifiedTooltipRules;
import net.minecraft.world.item.ItemStack;

/**
 * The self-test {@code relic.ex_singularity} (run by the CI server smoke test, see {@link SelfTests}): the grade above Genesis is the last and the highest grade; it is never rolled and no
 * registered relic carries it; the registered Shishou item resolves to the Akashic Relic category with that grade and the long star run; its damage range lies above Genesis's; and the
 * prismatic palette stays inside 0xRRGGBB over a sweep of times and glyph positions. Server safe: no client class (the drawing is client only and is not touched here).
 */
public final class ExSingularitySelfTests {
    private ExSingularitySelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("relic.ex_singularity", server -> run());
    }

    static SelfTests.Result run() {
        Grade[] all = Grade.values();
        if (all[all.length - 1] != Grade.AKASHIC || Grade.AKASHIC.ordinal() != Grade.GENESIS.ordinal() + 1) return SelfTests.Result.fail("EX-Singularity is not the grade right above Genesis");
        if (!Grade.AKASHIC.atLeast(Grade.GENESIS) || Grade.GENESIS.atLeast(Grade.AKASHIC)) return SelfTests.Result.fail("the grade order is wrong");
        if (Grade.AKASHIC.rollable() || Grade.AKASHIC.weight() != 0.0) return SelfTests.Result.fail("EX-Singularity can be rolled");
        for (Grade g : all) if (g != Grade.AKASHIC && !g.rollable()) return SelfTests.Result.fail(g + " cannot be rolled");
        int relics = 0;
        for (RelicDef d : Relics.all()) {
            relics++;
            if (d.grade() == Grade.AKASHIC) return SelfTests.Result.fail("the relic " + d.id() + " carries the EX-Singularity grade (it is for hand-named items only)");
        }
        if (Grade.AKASHIC.stars().length() <= Grade.GENESIS.stars().length()) return SelfTests.Result.fail("the EX-Singularity star run is not longer than Genesis's");
        if (!Grade.AKASHIC.label().contains(ExSingularity.NAME)) return SelfTests.Result.fail("the label does not say EX-Singularity: " + Grade.AKASHIC.label());
        if (Grade.byName("EX-Singularity") != Grade.AKASHIC || Grade.byName("akashic") != Grade.AKASHIC) return SelfTests.Result.fail("the grade is not found by name");
        if (GradeDamage.range(Grade.AKASHIC.ordinal())[0] <= GradeDamage.range(Grade.GENESIS.ordinal())[1]) return SelfTests.Result.fail("the EX-Singularity damage range does not lie above Genesis's");
        if (GradeDamage.ordinal("EX-Singularity") != Grade.AKASHIC.ordinal()) return SelfTests.Result.fail("the damage table does not know the grade name");

        ItemStack shishou = new ItemStack(ShishouItems.SHISHOU.get());
        UnifiedTooltipRules.Style style = UnifiedTooltipItems.styleOf(shishou, null);
        if (style.category() != UnifiedTooltipRules.Category.AKASHIC_RELIC || !style.akashic()) return SelfTests.Result.fail("Shishou does not resolve to the Akashic Relic style: " + style.category() + " " + style.grade());
        if (!style.banner().contains(ExSingularity.NAME) || !style.banner().startsWith(ExSingularity.STARS)) return SelfTests.Result.fail("Shishou's banner is not the EX-Singularity one: " + style.banner());

        int checked = 0;
        for (long ms = 0; ms < 20_000L; ms += 37L) {
            for (int i = 0; i < 48; i += 3) {
                int c = ExSingularity.textRgb(i, 48, ms);
                if ((c & 0xFFFFFF) != c) return SelfTests.Result.fail("text colour out of range: " + c);
                checked++;
            }
            int[] cs = {ExSingularity.rowRgb(ms, 0.3f), ExSingularity.borderRgb(ms, true), ExSingularity.frameRgb(5, 8, ms, true), ExSingularity.glowRgb(ms / 50f), ExSingularity.veilRgb((ms % 1000) / 1000f, ms / 50f), ExSingularity.shardRgb(3, 8, ms / 50f)};
            for (int c : cs) if ((c & 0xFFFFFF) != c) return SelfTests.Result.fail("colour out of range: " + c);
            int a = ExSingularity.frameAlpha(false, ms), b = ExSingularity.frameAlpha(true, ms);
            if (a < 0 || a > 255 || b < 0 || b > 255) return SelfTests.Result.fail("alpha out of range");
        }
        return SelfTests.Result.ok("EX-Singularity is grade " + Grade.AKASHIC.tier() + " (above Genesis), never rolled, none of " + relics + " relics carries it, Shishou resolves to it, " + checked + " palette samples in range");
    }
}
