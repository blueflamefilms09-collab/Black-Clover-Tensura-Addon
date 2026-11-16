package com.newuniverse.nusmp.core.xform;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.blackclover.ModeArmor;
import com.newuniverse.nusmp.book.AntiMagicBook;
import com.newuniverse.nusmp.book.DevilFormLimit;
import com.newuniverse.nusmp.core.xform.TransformPolicy.Reason;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: nothing here can be watched by its author, so the CI server smoke test runs it (key prefix {@code xformguard}). {@code xformguard.rules}: the policy table, the registrations (every flag
 * and modifier the guard may touch is this mod's own, the Black Form hook is there) and the setting ({@code transformations.resetOnDeath}, default on). {@code xformguard.lifecycle}: with a
 * stand-in player, a body that wears a form (form flags in its data, a form modifier next to one that is not a form, a render layer) goes through the events the guard answers: a death and a
 * respawn end every form and leave the unrelated flag and modifier alone, the setting off leaves everything as it was, a return from the End, a change of dimension, a login and a logout are not
 * touched, a clone strips the copied flags, ending twice is harmless, and the Black Form's skill switch is turned off by its hook. Registered from {@code NUSMP}.
 */
public final class XformGuardSelfTests {
    private XformGuardSelfTests() {}

    private static boolean registered;

    private static final ResourceLocation FORM_MOD = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "unite_speed");
    private static final ResourceLocation KEEP_MOD = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "xformguard_not_a_form");
    private static final String KEEP_FLAG = "nusmp_xformguard_not_a_form";

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("xformguard.rules", s -> rules());
        SelfTests.register("xformguard.lifecycle", XformGuardSelfTests::lifecycle);
    }

    private static Result rules() {
        List<String> bad = new ArrayList<>();
        for (Reason r : Reason.values()) {
            if (TransformPolicy.endsForms(r, false)) bad.add(r + " ends forms with the setting off");
            boolean death = r == Reason.DEATH || r == Reason.RESPAWN;
            if (TransformPolicy.endsForms(r, true) != death) bad.add(r + " has the wrong default policy");
            if (death == TransformPolicy.ownerDecides(r)) bad.add(r + " is both a guard moment and an owner moment");
        }
        if (!TransformPolicy.stripsClone(true, true) || TransformPolicy.stripsClone(false, true) || TransformPolicy.stripsClone(true, false)) bad.add("the clone policy is wrong");
        if (!TransformGuard.hookIds().contains("anti_magic_black_form")) bad.add("the Black Form hook is not registered");
        if (!TransformGuard.keys().contains(DevilFormLimit.SINCE) || !TransformGuard.keys().contains(ModeArmor.KEY)) bad.add("a form flag is missing from the guard");
        for (String k : TransformGuard.keys()) if (!TransformPolicy.strippable(k)) bad.add("a flag that is not ours is registered: " + k);
        if (TransformGuard.modifiers().size() < 10) bad.add("only " + TransformGuard.modifiers().size() + " form modifiers are registered");
        for (TransformGuard.Mod m : TransformGuard.modifiers()) if (!TransformPolicy.sweepable(m.id())) bad.add("a modifier that is not ours is registered: " + m.id());
        if (!NUConfig.transformResetOnDeath()) bad.add("transformations.resetOnDeath is not on by default");
        return bad.isEmpty() ? Result.ok(TransformGuard.hookIds().size() + " hooks, " + TransformGuard.keys().size() + " flags, " + TransformGuard.modifiers().size() + " modifiers; resetOnDeath=" + NUConfig.transformResetOnDeath())
                : Result.fail(String.join("; ", bad.subList(0, Math.min(6, bad.size()))));
    }

    /** Puts a form on the body: flags, a form modifier, an unrelated modifier and flag, a render layer. */
    private static void wear(ServerPlayer p) {
        CompoundTag d = p.getPersistentData();
        d.putLong(DevilFormLimit.SINCE, 5L);
        d.putLong(DevilFormLimit.AFTER, 99_999_999L);
        d.putInt(ModeArmor.KEY, 3);
        d.putLong("nusmp_devil_until", 99_999_999L);
        d.putLong(KEEP_FLAG, 7L);                                                  // a flag nobody registered: must survive
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(FORM_MOD);
            speed.removeModifier(KEEP_MOD);
            speed.addTransientModifier(new AttributeModifier(FORM_MOD, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            speed.addTransientModifier(new AttributeModifier(KEEP_MOD, 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        try {
            PlayerAuras.set(p, Aura.BODY, 6000);
        } catch (RuntimeException noConnection) { /* a stand-in has no network: the table is written, there is nothing to send */ }
    }

    /** What of the form is still on the body (flags, modifier, layer). */
    private static List<String> worn(ServerPlayer p) {
        List<String> on = new ArrayList<>();
        CompoundTag d = p.getPersistentData();
        if (d.contains(DevilFormLimit.SINCE) || d.contains(DevilFormLimit.AFTER) || d.contains(ModeArmor.KEY) || d.contains("nusmp_devil_until")) on.add("flags");
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.hasModifier(FORM_MOD)) on.add("modifier");
        if (PlayerAuras.active(p, Aura.BODY)) on.add("layer");
        return on;
    }

    private static Result lifecycle(MinecraftServer server) {
        ServerPlayer p = SelfTests.player(server.overworld(), "xformguard", 0.5, 100, 0.5);
        List<String> bad = new ArrayList<>();
        try {
            // death ends everything that is a form, and only that
            wear(p);
            if (worn(p).size() != 3) bad.add("the setup did not put the form on: " + worn(p));
            TransformGuard.end(p, Reason.DEATH, true);
            if (!worn(p).isEmpty()) bad.add("death left " + worn(p));
            if (!p.getPersistentData().contains(KEEP_FLAG)) bad.add("death removed a flag that is not a form's");
            AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null && !speed.hasModifier(KEEP_MOD)) bad.add("death removed a modifier that is not a form's");
            TransformGuard.end(p, Reason.DEATH, true);                                  // twice is harmless
            // the respawned body gets the same
            wear(p);
            TransformGuard.end(p, Reason.RESPAWN, true);
            if (!worn(p).isEmpty()) bad.add("respawn left " + worn(p));
            // the setting off: the old behaviour, nothing touched
            wear(p);
            TransformGuard.end(p, Reason.DEATH, false);
            TransformGuard.end(p, Reason.RESPAWN, false);
            if (worn(p).size() != 3) bad.add("resetOnDeath=false still ended something: " + worn(p));
            // not a death: the End, a dimension change, a login, a logout
            for (Reason r : new Reason[]{Reason.END_RETURN, Reason.DIMENSION, Reason.LOGIN, Reason.LOGOUT}) {
                TransformGuard.end(p, r, true);
                if (worn(p).size() != 3) bad.add(r + " ended a form that belongs to its owner: " + worn(p));
            }
            // the Black Form: a flag in a skill's tag that the respawn would otherwise bring back
            ManasSkillInstance black = NUSkills.BOOK_ANTI_MAGIC.get().createDefaultInstance();
            black.getOrCreateTag().putBoolean("BlackForm", true);
            if (!AntiMagicBook.inBlackForm(black)) bad.add("the setup did not switch the Black Form on");
            FormHooks.endBlackForm(p, black);
            if (AntiMagicBook.inBlackForm(black)) bad.add("the Black Form is still on after the hook");
            FormHooks.endBlackForm(p, black);                                           // twice is harmless
            // a clone after a death strips the flags that were copied
            CompoundTag copied = new CompoundTag();
            copied.putLong(DevilFormLimit.AFTER, 1L);
            copied.putLong(KEEP_FLAG, 1L);
            if (TransformGuard.strip(copied) != 1 || copied.contains(DevilFormLimit.AFTER) || !copied.contains(KEEP_FLAG)) bad.add("the clone strip is wrong");
        } finally {
            TransformGuard.end(p, Reason.DEATH, true);
            p.getPersistentData().remove(KEEP_FLAG);
            AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.removeModifier(FORM_MOD);
                speed.removeModifier(KEEP_MOD);
            }
        }
        return bad.isEmpty() ? Result.ok("death and respawn end flags, modifier and layer; the setting off, the End, a dimension change, a login and a logout leave them; the Black Form hook and the clone strip work")
                : Result.fail(String.join("; ", bad.subList(0, Math.min(6, bad.size()))));
    }
}
