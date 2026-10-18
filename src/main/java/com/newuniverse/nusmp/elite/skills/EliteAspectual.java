package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.skill.EliteBridge;
import com.newuniverse.nusmp.skill.SkillMsg;
import com.newuniverse.nusmp.skill.SkillText;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.magic.aspectual.AspectualMagic;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Why: the three Aspectual magics (Kamehameha, Gravity Well, Static Chain) are real Tensura {@link AspectualMagic}, so they sit in the same list as Fire Ball and Thunder Lance, but they
 * take their name, description and 32x32 icon from this mod's {@link SkillText} (translatable with an English fallback, icon {@code textures/skill/<id>.png}) like every other skill of the
 * mod, and they are cast the mod's way: the magicules are spent by the skill itself, through the same pack-wide multipliers as the rebuilt skills, only after the checks passed. The
 * Tensura casting animation (which would run its own cast-time pipeline) is not used: a magic here is cast when the key is RELEASED, after a short or long hold the subclass decides
 * about. Subclasses give the modes and the {@link #cast}; everything about gating, paying, refunding, the cooldown and mastery is here once.
 */
public abstract class EliteAspectual extends AspectualMagic {
    private static final String BLOCKED = "EliteBlocked";

    private final SkillText text;
    private final ResourceLocation icon;

    protected EliteAspectual(AspectualMagic.AspectualType type, SkillText text) {
        super(type);
        this.text = text;
        this.icon = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/skill/" + text.id() + ".png");
    }

    public SkillText text() { return text; }

    @Override public ResourceLocation getSkillIcon() { return icon; }

    @Override public MutableComponent getName() { return Component.translatableWithFallback(text.nameKey(), text.name()); }

    @Override public MutableComponent getSkillDescription() { return Component.translatableWithFallback(text.descriptionKey(), text.description()); }

    @Override public int getMaxMastery() { return 100; }

    @Override public boolean isInstantCast(ManasSkillInstance instance, LivingEntity entity) { return true; }

    // ---------------------------------------------------------------- the subclass
    protected abstract List<EliteMode> modes();

    /** The spell. Return false if it could not be cast (the cost is refunded, no cooldown starts). */
    protected abstract boolean cast(ManasSkillInstance instance, ServerPlayer player, int mode, int heldTicks);

    /** Cooldown in ticks (before the multipliers) after a successful cast: the mode's own unless the subclass makes it depend on the cast (a charge). */
    protected int cooldownAfter(ManasSkillInstance instance, ServerPlayer player, int mode, int heldTicks) { return mode(mode).cooldown(); }

    /** ARGB colour of the cast circle. */
    protected int color() { return 0xFFE8D8FF; }

    protected final EliteMode mode(int mode) {
        List<EliteMode> m = modes();
        return m.get(Math.max(0, Math.min(m.size() - 1, mode)));
    }

    protected static boolean masteredBy(ManasSkillInstance instance, LivingEntity entity) { return entity != null && instance.isMastered(entity); }

    protected static double masteryFrac(ManasSkillInstance instance) {
        int max = instance.getMaxMastery();
        return max <= 0 ? 0.0 : Math.max(0.0, Math.min(1.0, instance.getMastery() / max));
    }

    protected static void cooldownTicks(ManasSkillInstance instance, int mode, int ticks) {
        instance.setCoolDown(ticks <= 0 ? 0 : Math.max(1, (ticks + 19) / 20), mode);   // ManasCore wants whole seconds
    }

    protected static void grow(ManasSkillInstance instance, ServerPlayer player, double amount) {
        if (amount > 0) instance.addMasteryPoint(player, amount);
        instance.markDirty();
    }

    // ---------------------------------------------------------------- modes
    @Override public int getModes(ManasSkillInstance instance) { return modes().size(); }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) {
        return Math.floorMod(mode + (reverse ? -1 : 1), modes().size());
    }

    @Override public String getModeId(ManasSkillInstance instance, int mode) { return mode(mode).id(); }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        EliteMode m = mode(mode);
        int cd = instance.getCoolDown(mode);
        MutableComponent name = SkillMsg.mode(m.id(), m.name());
        return cd > 0 ? name.append(" (" + cd + "s)") : name;
    }

    @Override
    public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) { return EliteSkillsConfig.cost(mode(mode).cost()); }

    @Override public int getMaxHeldTime(ManasSkillInstance instance, LivingEntity entity) { return 40; }

    // ---------------------------------------------------------------- the key
    /** Can this mode start now? Tells the player why not. Subclasses with a charge call it from their own press. */
    protected final boolean mayStart(ManasSkillInstance instance, ServerPlayer player, int mode, double cost) {
        if (!EliteSkillsConfig.enabled()) {
            EliteBridge.fail(player, SkillMsg.t("elite.off", "The elite skills are switched off on this server."));
            return false;
        }
        if (instance.onCoolDown(mode)) {
            EliteBridge.fail(player, SkillMsg.t("elite.cooldown", "%s is on cooldown (%s s).", mode(mode).name(), instance.getCoolDown(mode)));
            return false;
        }
        if (!EliteBridge.hasMagicules(player, cost)) {
            EliteBridge.fail(player, SkillMsg.t("no_magicules", "Not enough magicules (need %s).", (long) cost));
            return false;
        }
        return true;
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int slot, int mode) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        boolean ok = mayStart(instance, player, mode, EliteSkillsConfig.cost(mode(mode).cost()));
        instance.getOrCreateTag().putBoolean(BLOCKED, !ok);
    }

    @Override
    public boolean onHeld(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) {
        return entity instanceof ServerPlayer && !instance.getOrCreateTag().getBoolean(BLOCKED);
    }

    @Override
    public void onRelease(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int slot, int mode) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        boolean blocked = instance.getOrCreateTag().getBoolean(BLOCKED);
        instance.getOrCreateTag().putBoolean(BLOCKED, false);
        if (blocked) return;
        EliteMode m = mode(mode);
        double price = EliteSkillsConfig.cost(m.cost());
        if (!EliteBridge.spendMagicules(player, price)) return;
        boolean done;
        try {
            done = cast(instance, player, mode, heldTicks);
        } catch (RuntimeException broken) {
            EliteBridge.refund(player, price);
            throw broken;
        }
        if (!done) {
            EliteBridge.refund(player, price);
            return;
        }
        cooldownTicks(instance, mode, EliteSkillsConfig.cooldown(cooldownAfter(instance, player, mode, heldTicks)));
        EliteBridge.castVfx(player, color());
        grow(instance, player, 1);
    }
}
