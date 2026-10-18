package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.ChantRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the five DanMachi spells are CHANTED. HOLD the key and one line of the chant finishes every few ticks (the line is shown on the
 * action bar and a note rings); the cast happens when you RELEASE after the last line. Letting go early breaks the chant and costs
 * nothing, because the magicules are paid at the cast, not at the start. Mastery chants three times faster. The spell itself is the
 * subclass's {@link #cast}; everything about the chant (timing, messages, cost, cooldown, mastery) is here once.
 */
public abstract class ChantedMagicSkill extends NUSkill {
    protected static final int CAST = 0;
    private static final String CHANT = "Chant", BLOCKED = "Blocked";

    protected final ChantRules.Spell spell;

    protected ChantedMagicSkill(SkillText text, ChantRules.Spell spell) {
        super(SkillType.EXTRA, text, false);
        this.spell = spell;
    }

    /** One line of the chant (1-based), a lang key with an English fallback. */
    protected abstract MutableComponent chantLine(int line);
    /** ARGB colour of the cast circle. */
    protected abstract int color();
    /** The spell. Return false if it could not be cast (the cost is refunded and no cooldown starts). */
    protected abstract boolean cast(ManasSkillInstance instance, ServerPlayer player, ServerLevel level, Tier tier);

    @Override public int getModes(ManasSkillInstance instance) { return 1; }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean reverse) { return CAST; }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return "magic.cast"; }
    @Override public Component getModeName(ManasSkillInstance i, int mode) { return SkillMsg.mode("magic.cast", "Cast"); }

    @Override
    public int getMaxHeldTime(ManasSkillInstance instance, LivingEntity entity) {
        return ChantRules.maxHeldTicks(spell, tier(instance, entity));
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int slot, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        var tag = instance.getOrCreateTag();
        tag.putInt(CHANT, 0);
        tag.putBoolean(BLOCKED, false);
        if (instance.onCoolDown(CAST)) {
            SkillUtil.fail(player, SkillMsg.t("magic.cooldown", "This magic is on cooldown."));
            tag.putBoolean(BLOCKED, true);
        } else if (!SkillUtil.hasMagicules(player, DMUtil.cost(spell.cost))) {
            SkillUtil.fail(player, SkillMsg.t("no_magicules", "Not enough magicules (need %s).", (long) DMUtil.cost(spell.cost)));
            tag.putBoolean(BLOCKED, true);
        }
    }

    @Override
    public boolean onHeld(ManasSkillInstance instance, LivingEntity living, int heldTicks, int mode) {
        if (!(living instanceof ServerPlayer player)) return true;
        var tag = instance.getOrCreateTag();
        if (tag.getBoolean(BLOCKED)) return false;
        Tier tier = tier(instance, player);
        int done = ChantRules.linesDone(heldTicks, spell, tier);
        int shown = tag.getInt(CHANT);
        if (done <= shown) return true;

        tag.putInt(CHANT, done);
        ServerLevel level = player.serverLevel();
        if (shown == 0) {
            // the circle of gathering magicules lasts exactly as long as the chant
            SkillFx.follow(level, VfxShape.MANA_CHARGE, player, player.position().add(0, 1, 0), color(),
                    ChantRules.totalTicks(spell, tier), 1.0f);
        }
        SkillUtil.actionbar(player, chantLine(done).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        SkillFx.sound(level, player.position(), SoundEvents.NOTE_BLOCK_CHIME.value(), 0.8f, 0.7f + 0.3f * done / spell.lines);
        if (done >= spell.lines) {
            SkillUtil.actionbar(player, SkillMsg.t("magic.ready", "The chant is complete: release to cast.").withStyle(ChatFormatting.AQUA));
        }
        instance.markDirty();
        return true;
    }

    @Override
    public void onRelease(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int slot, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        var tag = instance.getOrCreateTag();
        Tier tier = tier(instance, player);
        boolean blocked = tag.getBoolean(BLOCKED);
        int done = Math.max(tag.getInt(CHANT), ChantRules.linesDone(heldTicks, spell, tier));
        tag.putInt(CHANT, 0);
        tag.putBoolean(BLOCKED, false);
        if (blocked) return;
        if (done < spell.lines) {
            SkillUtil.actionbar(player, SkillMsg.t("magic.broken", "The chant was broken.").withStyle(ChatFormatting.GRAY));
            instance.markDirty();
            return;
        }
        if (instance.onCoolDown(CAST)) { SkillUtil.fail(player, SkillMsg.t("magic.cooldown", "This magic is on cooldown.")); return; }

        double price = DMUtil.cost(spell.cost);
        if (!SkillUtil.spendMagicules(player, price)) return;
        if (!cast(instance, player, player.serverLevel(), tier)) {
            SkillUtil.refund(player, price);
            return;
        }
        cooldown(instance, CAST, DMUtil.cooldown(spell.cooldown));
        SkillUtil.castVfx(player, color());
        grow(instance, player, 1);
    }
}
