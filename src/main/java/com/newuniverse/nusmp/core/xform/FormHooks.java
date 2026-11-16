package com.newuniverse.nusmp.core.xform;

import com.newuniverse.nusmp.blackclover.ModeArmor;
import com.newuniverse.nusmp.book.AntiMagicBook;
import com.newuniverse.nusmp.book.ArmourState;
import com.newuniverse.nusmp.book.DevilFormLimit;
import com.newuniverse.nusmp.book.NoelleFormState;
import com.newuniverse.nusmp.core.xform.TransformPolicy.Reason;
import com.newuniverse.nusmp.shadow.UniteState;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Why: what each existing form system registers with the {@link TransformGuard}, in one list (docs/transformation_fixes.md has the per-system table). Only form-only state is listed: the persistent
 * flags and transient modifiers a form puts on a body, and the skills whose own switch (a tag, a toggle) would bring the form back after a respawn. Racial bonuses, permanent prices (the devil's
 * lost health, the Rewind sliver) and passives are NOT listed: they are not forms and must survive a death.
 */
final class FormHooks {
    private FormHooks() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("anime_multiverse", path); }

    static void registerAll() {
        // ---- persistent-data flags a form leaves in the player's data (stripped from the dying body, from the clone and from the respawned body)
        TransformGuard.key(DevilFormLimit.SINCE);                    // Devil Union (Black Form): when it began
        TransformGuard.key(DevilFormLimit.AFTER);                    // ... and the muscle-pain aftermath (the class says death clears it)
        TransformGuard.key(NoelleFormState.AFTER);                   // the Black Dragon Form's exhaustion
        TransformGuard.key(ArmourState.KEY);                         // the worn armour / form (ArmourState ends it itself; this is the net under it)
        TransformGuard.key(ModeArmor.KEY);                           // the mode armour overlay
        TransformGuard.key(ModeArmor.UNTIL);
        TransformGuard.key(ModeArmor.YUNO);
        TransformGuard.key(UniteState.KIND);                         // the Devil Unite modes (Slotos, Gimodelo, Plumede, Walgner)
        TransformGuard.key(UniteState.UNTIL);
        TransformGuard.key(UniteState.OLD);
        TransformGuard.key("nusmp_devil_until");                     // Devil Union / Devil Dive of the element books (GrimoireBook.startDevilState)
        TransformGuard.key("nusmp_devil_src");

        // ---- transient form modifiers (removed by id; the owning system re-applies nothing of these after a death)
        TransformGuard.modifier(Attributes.MOVEMENT_SPEED, id("saiyan/form_speed"));              // the Saiyan forms and the timed techniques
        TransformGuard.modifier(Attributes.ATTACK_SPEED, id("saiyan/form_attack_speed"));
        TransformGuard.modifier(Attributes.MOVEMENT_SPEED, id("saiyan/tech_speed"));
        TransformGuard.modifier(Attributes.ATTACK_SPEED, id("saiyan/tech_attack_speed"));
        TransformGuard.modifier(Attributes.JUMP_STRENGTH, id("saiyan/tech_jump"));
        TransformGuard.modifier(Attributes.ATTACK_DAMAGE, id("black_form"));                      // Black Form's attack bonus (AntiMagicBook and the old AntiMagicLordSkill)
        TransformGuard.modifier(Attributes.MOVEMENT_SPEED, id("armour_speed"));                   // the worn armour / form
        TransformGuard.modifier(Attributes.ATTACK_DAMAGE, id("armour_attack"));
        TransformGuard.modifier(Attributes.KNOCKBACK_RESISTANCE, id("armour_knockback"));
        TransformGuard.modifier(Attributes.ARMOR, id("unite_armor"));                             // the Devil Unite modes
        TransformGuard.modifier(Attributes.ARMOR_TOUGHNESS, id("unite_toughness"));
        TransformGuard.modifier(Attributes.MOVEMENT_SPEED, id("unite_speed"));
        TransformGuard.modifier(Attributes.KNOCKBACK_RESISTANCE, id("unite_knockback"));
        TransformGuard.modifier(Attributes.JUMP_STRENGTH, id("unite_jump"));
        TransformGuard.modifier(Attributes.SAFE_FALL_DISTANCE, id("unite_fall"));
        TransformGuard.modifier(Attributes.ATTACK_DAMAGE, id("unite_attack"));

        // ---- forms that live in a skill's own switch: the switch is part of the skill data that is copied to the new body, so the form would come back at the next upkeep
        TransformGuard.hook(new TransformGuard.Hook() {
            @Override public String id() { return "anti_magic_black_form"; }

            @Override public void end(ServerPlayer p, Reason why) {
                SkillAPI.getSkillsFrom(p).getSkill(NUSkills.BOOK_ANTI_MAGIC.getId()).ifPresent(i -> endBlackForm(p, i));
                SkillAPI.getSkillsFrom(p).getSkill(NUSkills.ANTI_MAGIC_LORD.getId()).ifPresent(i -> endLordForm(p, i));
            }
        });
    }

    /** The Anti-Magic grimoire's Black Form (a flag in the skill's tag): off, its time limit cleared, its modifier and mode armour taken away. Idempotent. */
    static void endBlackForm(ServerPlayer p, ManasSkillInstance i) {
        if (AntiMagicBook.inBlackForm(i)) AntiMagicBook.setForm(p, i, false);
    }

    /** The older Anti-Magic Lord skill's Black Form (the skill's toggle): off, with the skill's own switch-off. Idempotent. */
    static void endLordForm(ServerPlayer p, ManasSkillInstance i) {
        if (!i.isToggled()) return;
        i.setToggled(false);
        i.markDirty();
        NUSkills.ANTI_MAGIC_LORD.get().onToggleOff(i, p);
    }
}
