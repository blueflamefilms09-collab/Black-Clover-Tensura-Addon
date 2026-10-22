package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.grimoire.CanonNaming;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.tensura.ability.TensuraSkillInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Why: the Unique Skill a grimoire teaches is one skill per magic for every player, so its name (a lang key) cannot know whose book it is. The instance can: it carries the player's own tag,
 * and {@link CanonSkillSync} writes the bound character's id into it ("Canon", the same id the grimoire item carries) on the server. The display name the skill list, the selection panel and
 * the learn / forget messages read ({@link #getDisplayName()}) then puts the character in front of the magic: "Rades Spirito's Soul Corpse Magic". The tag travels with the instance to the
 * client like Magic, Cover and Leaves do, so no client state is read; with no character in the tag the name is the plain one, exactly as before. The instance is created by
 * {@link GrimoireBook#createDefaultInstance()} and {@code GrimoireMagicSkill#createDefaultInstance()}, which is also what loading a saved instance calls, so old saves get it too.
 */
public class GrimoireSkillInstance extends TensuraSkillInstance {
    public GrimoireSkillInstance(ManasSkill skill) { super(skill); }

    /** The bound character's id from this instance's tag, or "" for none. */
    public String canonId() {
        CompoundTag t = getTag();
        return t == null ? "" : t.getString(CanonNaming.TAG);
    }

    @Override
    public MutableComponent getDisplayName() {
        return decorate(super.getDisplayName(), canonId());
    }

    /** The character's possessive in front of 'base' in the style of 'base' (gold for a Unique Skill); 'base' itself when the id names no character. */
    public static MutableComponent decorate(MutableComponent base, String canonId) {
        String owner = CanonNaming.ownerOf(canonId);
        if (owner == null) return base;
        return Component.literal(CanonNaming.possessive(owner) + " ").setStyle(base.getStyle()).append(base);
    }
}
