package com.newuniverse.nusmp.antimagic;

import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * Anti-Magic Power (AMP) helpers. 0.48: the Anti-Magic Spirit Lord is a state of the Anti-Magic grimoire (book.AntiMagicBook), so
 * AMP lives on that grimoire's skill instance. Players who still have the old separate Lord skill are folded in by {@link #migrate}.
 */
public final class AntiMagic {
    private AntiMagic() {}
    public static final int MAX_AMP = 1000;

    /** The player's Anti-Magic grimoire skill (any stage), if they have it. */
    public static Optional<ManasSkillInstance> book(Player p) {
        return SkillAPI.getSkillsFrom(p).getSkill(NUSkills.BOOK_ANTI_MAGIC.getId());
    }

    /** The Anti-Magic Lord: the awakened Anti-Magic grimoire, or (until folded in) the old separate Lord skill. */
    public static Optional<ManasSkillInstance> lord(Player p) {
        var b = book(p);
        if (b.isPresent() && com.newuniverse.nusmp.book.AntiMagicBook.isLord(b.get())) return b;
        return SkillAPI.getSkillsFrom(p).getSkill(NUSkills.ANTI_MAGIC_LORD.getId());
    }

    /** True for either the current Anti-Magic grimoire or a legacy Lord skill. */
    public static boolean isUser(Player p) { return book(p).isPresent() || lord(p).isPresent(); }

    /** Where AMP is kept: the Anti-Magic grimoire, else the old Lord skill. */
    static Optional<ManasSkillInstance> ampHolder(Player p) {
        var b = book(p);
        return b.isPresent() ? b : SkillAPI.getSkillsFrom(p).getSkill(NUSkills.ANTI_MAGIC_LORD.getId());
    }

    /**
     * Awakens the Anti-Magic Lord (admin command, or mastery through AntiMagicBook). Needs the Anti-Magic grimoire. Returns false if
     * there is no Anti-Magic grimoire or it was already awakened.
     */
    public static boolean awaken(net.minecraft.server.level.ServerPlayer p) {
        var b = book(p);
        if (b.isEmpty() || com.newuniverse.nusmp.book.AntiMagicBook.isLord(b.get())) return false;
        com.newuniverse.nusmp.book.AntiMagicBook.awaken(p, b.get(), true);
        return true;
    }

    /**
     * 0.48: folds an old Anti-Magic Spirit Lord skill into the Anti-Magic grimoire: the AMP and the awakening carry over and the
     * separate skill is removed (it showed as its own ability). Without an Anti-Magic grimoire the old skill is left alone.
     * Runs from a player tick, outside the skill tick.
     */
    public static void migrate(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) || p.tickCount % 100 != 37) return;
        var skills = SkillAPI.getSkillsFrom(p);
        var old = skills.getSkill(NUSkills.ANTI_MAGIC_LORD.getId());
        var b = book(p);
        if (old.isEmpty() || b.isEmpty()) return;
        setAmp(b.get(), Math.max(amp(b.get()), amp(old.get())));
        boolean form = old.get().isToggled();
        com.newuniverse.nusmp.book.AntiMagicBook.awaken(p, b.get(), false);
        skills.forgetSkill(NUSkills.ANTI_MAGIC_LORD.getId());
        if (form) com.newuniverse.nusmp.book.AntiMagicBook.setForm(p, b.get(), true);
        p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Your Anti-Magic Spirit Lord has merged into your Anti-Magic grimoire: "
                + "Black Asta is now your Black Form, and your Anti-Magic Power carried over.").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }

    public static int amp(ManasSkillInstance i) { return i.getOrCreateTag().getInt("AMP"); }

    public static void setAmp(ManasSkillInstance i, int v) {
        i.getOrCreateTag().putInt("AMP", Math.max(0, Math.min(MAX_AMP, v)));
        i.markDirty();
    }

    public static void addAmp(Player p, int amount) { ampHolder(p).ifPresent(i -> setAmp(i, amp(i) + amount)); }

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
