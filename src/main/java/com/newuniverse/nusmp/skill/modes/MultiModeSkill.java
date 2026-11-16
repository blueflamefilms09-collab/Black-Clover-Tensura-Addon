package com.newuniverse.nusmp.skill.modes;

import com.newuniverse.nusmp.skill.NUSkill;
import com.newuniverse.nusmp.skill.SkillMsg;
import com.newuniverse.nusmp.skill.SkillText;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the reusable half of "one skill, one slot, many abilities" (the Saiyan Prodigy is the first user, docs/saiyan_prodigy.md). A subclass hands over a {@link ModeBundle} (the data table) and answers three
 * things: {@link #modeOpen} (is this mode unlocked for this wearer; the mode key skips a closed one), {@link #press} (the key went down on a mode) and, for a HOLD mode ({@link ModeSpec#isHold}),
 * {@link #hold} (each tick the key stays down: true = keep going) and {@link #release}. The base class does the Tensura plumbing: the mode count, the wheel walk, the mode id and the translated mode name,
 * the server-only guard and the "locked" message. It holds no state of its own (a subclass keeps what it needs in the skill instance tag, per player), so one instance of it serves every player.
 * Cost: a few array reads per press; nothing per tick.
 */
public abstract class MultiModeSkill extends NUSkill {
    private final ModeBundle bundle;

    protected MultiModeSkill(SkillType type, SkillText text, ModeBundle bundle) {
        super(type, text);
        this.bundle = bundle;
    }

    public ModeBundle bundle() { return bundle; }

    /** True when the mode may be used by this wearer now. Called on the server (a press) and on the client (the mode key), so it reads only what the skill instance tag mirrors. */
    protected boolean modeOpen(ManasSkillInstance instance, LivingEntity wearer, ModeSpec mode) { return true; }

    /** The key went down on an open mode (server, living player). */
    protected abstract void press(ManasSkillInstance instance, ServerPlayer player, ModeSpec mode, int modeNumber);

    /** A hold mode, each tick the key stays down: true = keep holding. */
    protected boolean hold(ManasSkillInstance instance, ServerPlayer player, ModeSpec mode, int heldTicks) { return false; }

    /** The key of a hold mode was let go (or the hold was interrupted). */
    protected void release(ManasSkillInstance instance, ServerPlayer player, ModeSpec mode, int heldTicks) { }

    /** What the player is told when a locked mode is pressed. */
    protected Component lockedMessage(ModeSpec mode) { return SkillMsg.t("mode.locked", "%s is not unlocked yet.", modeName(mode)); }

    protected final Component modeName(ModeSpec m) { return SkillMsg.mode(bundle.id() + "." + m.id(), m.name()); }

    @Override public int getModes(ManasSkillInstance i) { return bundle.size(); }

    @Override
    public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean reverse) {
        return bundle.next(mode, reverse, c -> modeOpen(i, e, bundle.at(c)));
    }

    @Override public String getModeId(ManasSkillInstance i, int mode) { return bundle.id() + "." + bundle.at(mode).id(); }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        Component name = modeName(bundle.at(mode));
        int cd = i.getCoolDown(Math.floorMod(mode, bundle.size()));
        return cd > 0 ? name.copy().append(Component.literal(" (" + cd + "s)")) : name;
    }

    @Override
    public final void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer p) || !p.isAlive()) return;
        ModeSpec m = bundle.at(mode);
        if (!modeOpen(instance, p, m)) { p.displayClientMessage(lockedMessage(m), true); return; }
        press(instance, p, m, Math.floorMod(mode, bundle.size()));
    }

    @Override
    public final boolean onHeld(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) {
        if (!(entity instanceof ServerPlayer p) || !p.isAlive()) return false;
        ModeSpec m = bundle.at(mode);
        return m.isHold() && hold(instance, p, m, heldTicks);
    }

    @Override
    public final void onRelease(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int keyNumber, int mode) {
        if (!(entity instanceof ServerPlayer p)) return;
        ModeSpec m = bundle.at(mode);
        if (m.isHold()) release(instance, p, m, heldTicks);
    }

    @Override
    public boolean shouldTriggerReleaseOnHeldInterrupt(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) { return bundle.at(mode).isHold(); }
}
