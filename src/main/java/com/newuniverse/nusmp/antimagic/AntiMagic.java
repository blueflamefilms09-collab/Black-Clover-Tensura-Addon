package com.newuniverse.nusmp.antimagic;

import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/** Anti-Magic Power (AMP) helpers. AMP lives on the Anti-Magic Spirit Lord skill instance (saved with the player). */
public final class AntiMagic {
    private AntiMagic() {}
    public static final int MAX_AMP = 1000;

    public static Optional<ManasSkillInstance> lord(Player p) {
        return SkillAPI.getSkillsFrom(p).getSkill(NUSkills.ANTI_MAGIC_LORD.getId());
    }

    public static int amp(ManasSkillInstance i) { return i.getOrCreateTag().getInt("AMP"); }

    public static void setAmp(ManasSkillInstance i, int v) {
        i.getOrCreateTag().putInt("AMP", Math.max(0, Math.min(MAX_AMP, v)));
        i.markDirty();
    }

    public static void addAmp(Player p, int amount) { lord(p).ifPresent(i -> setAmp(i, amp(i) + amount)); }

    /** Is this magic damage (vanilla magic, or a Tensura elemental / magic / holy / curse / spatial type)? */
    public static boolean isMagic(DamageSource s) {
        if (s.is(DamageTypeTags.WITCH_RESISTANT_TO)) return true;
        return s.typeHolder().unwrapKey().map(k -> {
            if (!k.location().getNamespace().equals("tensura")) return false;
            String path = k.location().getPath();
            return path.contains("elemental") || path.contains("magic") || path.contains("holy") || path.contains("curse")
                    || path.contains("spatial") || path.contains("space") || path.contains("dark") || path.contains("light");
        }).orElse(false);
    }
}
