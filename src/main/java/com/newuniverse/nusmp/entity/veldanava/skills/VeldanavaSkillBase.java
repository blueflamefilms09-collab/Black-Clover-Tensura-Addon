package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.avatar.TensuraBridge;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the five Veldanava ultimate skills share one shape (ten modes: four toggles, then actives that each cost Magicule and cool down, and a mastery active), so the shape lives here once and a
 * skill only says what its actives DO. The base owns: the mode table ({@link JudgmentRules.Spec}), the icon, the toggle bits (kept in the skill instance and copied once a second and at every
 * change into the player's mask, {@link JudgmentState}), the crouch rule, the cooldown (the shorter mastered one once mastered, times the server's {@code cooldownScale}), the cost (charged through
 * {@link TensuraBridge}, refunded when the active refuses), the gap of {@link JudgmentRules#ACTIVE_GAP_TICKS} ticks between two actives, the mastery-only tenth mode and the mastery point.
 *
 * <p>Cost to the server: a press does its own bounded work (documented per skill); the tick runs once a second per owner and touches only what the skill's {@link #secondTick} touches. All state is
 * the player's own (the skill instance and the player's data), so any number of players can hold the same skill.
 */
public abstract class VeldanavaSkillBase extends Skill {
    protected static final String GAP = "nusmp_judgment_gap";

    protected final JudgmentRules.Spec spec;
    protected final int code;
    private final ResourceLocation icon;

    protected VeldanavaSkillBase(JudgmentRules.Spec spec) {
        super(SkillType.ULTIMATE);
        this.spec = spec;
        this.code = JudgmentRules.code(spec.id());
        this.icon = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "textures/skill/veldanava/" + spec.id() + ".png");
    }

    @Override public ResourceLocation getSkillIcon() { return icon; }
    @Override public int getModes(ManasSkillInstance i) { return spec.count(); }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean reverse) { return Math.floorMod(mode + (reverse ? -1 : 1), spec.count()); }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return spec.mode(mode).id(); }
    @Override public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) { return 0; }
    @Override public boolean canTick(ManasSkillInstance i, LivingEntity e) { return true; }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        JudgmentRules.Mode m = spec.mode(mode);
        Component name = Component.translatable("nusmp.skill.mode." + spec.id() + "." + m.id());
        if (m.toggle()) return name.copy().append(Component.literal(JudgmentRules.on(JudgmentState.toggles(i), m.index()) ? " [ON]" : " [OFF]"));
        int cd = i.getCoolDown(m.index());
        return cd > 0 ? name.copy().append(Component.literal(" (" + cd + "s)")) : name;
    }

    // ---------------------------------------------------------------- learn / forget / master / tick
    @Override
    public void onLearnSkill(ManasSkillInstance i, LivingEntity e) {
        super.onLearnSkill(i, e);
        if (e instanceof ServerPlayer p) syncMask(i, p);
    }

    @Override
    public void onForgetSkill(ManasSkillInstance i, LivingEntity e) {
        super.onForgetSkill(i, e);
        if (e instanceof ServerPlayer p) {
            JudgmentState.write(p, 0);
            JudgmentFields.clearOf(p.getUUID());
            forgotten(p);
        }
    }

    @Override
    public void onSkillMastered(ManasSkillInstance i, LivingEntity e) {
        super.onSkillMastered(i, e);
        if (e instanceof ServerPlayer p) {
            syncMask(i, p);
            p.sendSystemMessage(Component.translatable("nusmp.skill." + spec.id() + ".mastered").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
    }

    protected final void syncMask(ManasSkillInstance i, ServerPlayer p) {
        JudgmentState.write(p, JudgmentState.compose(code, JudgmentState.toggles(i), i.isMastered(p)));
    }

    @Override
    public void onTick(ManasSkillInstance i, LivingEntity e) {
        if (!(e instanceof ServerPlayer p) || p.tickCount % JudgmentRules.GAZE_PERIOD_TICKS != 0) return;
        syncMask(i, p);
        if (!JudgmentConfig.enabled() || !p.isAlive()) return;
        secondTick(i, p, JudgmentState.toggles(i), i.isMastered(p));
    }

    /** Once a second per owner, with the toggles and the mastered flag: the passives that need a regular look (a scan, a refresh of an attribute). Keep it bounded. */
    protected void secondTick(ManasSkillInstance i, ServerPlayer p, int toggles, boolean mastered) {}

    /** The skill was forgotten: drop everything of this player that the skill kept outside the instance (a held state, a modifier). */
    protected void forgotten(ServerPlayer p) {}

    // ---------------------------------------------------------------- the press
    @Override
    public void onPressed(ManasSkillInstance i, LivingEntity e, int key, int mode) {
        if (!(e instanceof ServerPlayer p) || !JudgmentConfig.enabled()) return;
        JudgmentRules.Mode m = spec.mode(mode);
        if (m.toggle()) { toggle(i, p, m); return; }
        boolean mastered = i.isMastered(p);
        if (m.masteryOnly() && !mastered) { fail(p, m.name() + " answers only a mastered " + spec.title() + "."); return; }
        if (i.onCoolDown(m.index())) { fail(p, m.name() + " is recharging (" + i.getCoolDown(m.index()) + "s)."); return; }
        if (m.kind() == JudgmentRules.Kind.HELD) { startHeld(i, p, m, mastered); return; }
        if (m.crouch() && !p.isShiftKeyDown()) { fail(p, "Crouch and press to use " + m.name() + "."); return; }
        long now = p.level().getGameTime();
        if (now < p.getPersistentData().getLong(GAP)) return;

        double cost = JudgmentRules.cost(m, mastered, JudgmentConfig.costScale());
        boolean free = p.isCreative();
        if (!free && !TensuraBridge.spendMagicule(p, cost)) { fail(p, "Not enough magicules (need " + (long) cost + ")."); return; }
        boolean done = activate(i, p, m, mastered);
        if (!done) {
            if (!free) TensuraBridge.refundMagicule(p, cost);
            return;
        }
        p.getPersistentData().putLong(GAP, now + JudgmentRules.ACTIVE_GAP_TICKS);
        startCooldown(i, m, mastered);
        addMasteryPoint(i, p);
        i.markDirty();
    }

    /** Puts a mode on its cooldown: the mastered one when mastered, times the server's {@code cooldownScale}. */
    protected final void startCooldown(ManasSkillInstance i, JudgmentRules.Mode m, boolean mastered) {
        int seconds = JudgmentRules.cooldownSeconds(m, mastered, JudgmentConfig.cooldownScale());
        if (seconds > 0) i.setCoolDown(seconds, m.index());
    }

    private void toggle(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m) {
        int t = JudgmentRules.flip(JudgmentState.toggles(i), m.index());
        JudgmentState.setToggles(i, t);
        syncMask(i, p);
        boolean on = JudgmentRules.on(t, m.index());
        toggled(i, p, m, on);
        p.displayClientMessage(Component.translatable("nusmp.skill.mode." + spec.id() + "." + m.id()).append(Component.literal(on ? " on" : " off"))
                .withStyle(on ? ChatFormatting.GOLD : ChatFormatting.GRAY), true);
    }

    /** A toggle just changed (an attribute that depends on it can be added or removed now rather than at the next second). */
    protected void toggled(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, boolean on) {}

    /** Runs a press or crouch + press active whose cost is already paid. Returns false when it did nothing (the cost is given back and no cooldown starts). */
    protected abstract boolean activate(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, boolean mastered);

    /** A held mode was pressed (its cooldown and the mastery gate are already checked). Prepare the hold, or refuse with {@link #fail}. */
    protected void startHeld(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, boolean mastered) {}

    /** Each tick of a held mode. Return false to end the hold. */
    protected boolean held(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, int heldTicks, boolean mastered) { return false; }

    /** The key of a held mode was let go. */
    protected void released(ManasSkillInstance i, ServerPlayer p, JudgmentRules.Mode m, int heldTicks) {}

    @Override
    public boolean onHeld(ManasSkillInstance i, LivingEntity e, int heldTicks, int mode) {
        JudgmentRules.Mode m = spec.mode(mode);
        if (!(e instanceof ServerPlayer p) || m.kind() != JudgmentRules.Kind.HELD) return true;
        return held(i, p, m, heldTicks, i.isMastered(p));
    }

    @Override
    public void onRelease(ManasSkillInstance i, LivingEntity e, int heldTicks, int keyNumber, int mode) {
        JudgmentRules.Mode m = spec.mode(mode);
        if (e instanceof ServerPlayer p && m.kind() == JudgmentRules.Kind.HELD) released(i, p, m, heldTicks);
    }

    // ---------------------------------------------------------------- helpers
    protected static void fail(ServerPlayer p, String text) { p.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.RED), true); }

    /** The mastery fraction the Balance Law scales an active by: full once mastered, a little under half before. */
    protected static double mastery(ServerPlayer p, ManasSkillInstance i) { return i.isMastered(p) ? 1.0 : 0.4; }
}
