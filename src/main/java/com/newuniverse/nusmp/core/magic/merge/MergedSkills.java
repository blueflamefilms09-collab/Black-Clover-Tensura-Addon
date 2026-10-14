package com.newuniverse.nusmp.core.magic.merge;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/** Registers the {@link MergeLimits#MAX_MERGED} merged-magic skills (once, from NUSkills) and finds them by slot. */
public final class MergedSkills {
    private MergedSkills() {}

    private static final List<DeferredHolder<ManasSkill, MergedBook>> ALL = new ArrayList<>();

    /** "merged_07" for slot 7. */
    public static String pathOf(int slot) { return String.format(java.util.Locale.ROOT, "merged_%02d", slot); }

    /** Called once from NUSkills' static initialiser with its DeferredRegister. */
    public static List<DeferredHolder<ManasSkill, MergedBook>> registerAll(DeferredRegister<ManasSkill> skills) {
        if (ALL.isEmpty()) {
            for (int k = 0; k < MergeLimits.MAX_MERGED; k++) {
                int slot = k;
                ALL.add(skills.register(pathOf(slot), () -> new MergedBook(slot)));
            }
        }
        return List.copyOf(ALL);
    }

    public static boolean has(int slot) { return slot >= 0 && slot < ALL.size(); }

    /** The skill of a slot (valid once registries are filled). */
    public static MergedBook skill(int slot) { return ALL.get(slot).get(); }

    public static ResourceLocation id(int slot) { return ALL.get(slot).getId(); }
}
