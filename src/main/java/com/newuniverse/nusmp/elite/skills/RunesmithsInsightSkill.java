package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.elite.skills.rules.ExtraRules;
import com.newuniverse.nusmp.skill.EliteBridge;
import com.newuniverse.nusmp.skill.SkillMsg;
import com.newuniverse.nusmp.skill.SkillText;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

/**
 * Runesmith's Insight (Extra, designed for this mod; the original addon's source was not available): you see the runes in things. Mode 1 READS the item in your hand for free (enchantments,
 * durability, any rune on it and how long it lasts). Modes 2 to 5 INSCRIBE a rune on the damageable item in your hand for five minutes (ten once mastered): FIRE sets what you hit alight,
 * FROST slows it, THUNDER adds a fifth of the blow as lightning damage, VITALITY returns a twentieth of the blow to you as health. The rune lives on the item (its own data), so it is
 * only active while you hold that item and it fades by itself. The on-hit effects are applied by {@link EliteSkillEvents} (a damage handler) through {@link #applyRune}. Numbers: {@link ExtraRules.Runesmith}.
 */
public class RunesmithsInsightSkill extends EliteSkill {
    static final SkillText TEXT = SkillText.of("runesmiths_insight", "Runesmith's Insight",
            "Extra. Read the runes and enchantments of the item in your hand, or inscribe a Fire, Frost, Thunder or Vitality rune on your weapon for a few minutes.");

    static final String RUNE = "nusmp_rune", RUNE_UNTIL = "nusmp_rune_until", RUNE_MASTERED = "nusmp_rune_mastered";

    public RunesmithsInsightSkill() { super(SkillType.EXTRA, TEXT, false); }

    @Override
    protected List<EliteMode> modes() {
        return List.of(EliteMode.of("runesmiths_insight.read", "Read Runes", ExtraRules.Runesmith.READ_COST, 20),
                EliteMode.of("runesmiths_insight.fire", "Inscribe: Fire", ExtraRules.Runesmith.INSCRIBE_COST, ExtraRules.Runesmith.INSCRIBE_COOLDOWN),
                EliteMode.of("runesmiths_insight.frost", "Inscribe: Frost", ExtraRules.Runesmith.INSCRIBE_COST, ExtraRules.Runesmith.INSCRIBE_COOLDOWN),
                EliteMode.of("runesmiths_insight.thunder", "Inscribe: Thunder", ExtraRules.Runesmith.INSCRIBE_COST, ExtraRules.Runesmith.INSCRIBE_COOLDOWN),
                EliteMode.of("runesmiths_insight.vitality", "Inscribe: Vitality", ExtraRules.Runesmith.INSCRIBE_COST, ExtraRules.Runesmith.INSCRIBE_COOLDOWN));
    }

    @Override protected int color() { return 0xFFE8B84A; }

    @Override
    protected boolean use(ManasSkillInstance instance, ServerPlayer player, int mode) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            EliteBridge.fail(player, SkillMsg.t("runesmiths_insight.empty", "Hold an item in your main hand."));
            return false;
        }
        long now = player.level().getGameTime();
        if (mode == 0) {
            read(player, stack, now);
            return true;
        }
        ExtraRules.Runesmith.Rune rune = ExtraRules.Runesmith.runeForMode(mode);
        if (rune == null) return false;
        if (!stack.isDamageableItem()) {
            EliteBridge.fail(player, SkillMsg.t("runesmiths_insight.bad_item", "Runes can only be inscribed on tools and weapons that wear down."));
            return false;
        }
        boolean mastered = masteredBy(instance, player);
        EliteUtil.editItemTag(stack, t -> {
            t.putString(RUNE, rune.name());
            t.putLong(RUNE_UNTIL, now + ExtraRules.Runesmith.durationTicks(mastered));
            t.putBoolean(RUNE_MASTERED, mastered);
        });
        var level = player.serverLevel();
        EliteBridge.shape(level, VfxShape.ALCHEMY_CIRCLE, player.position().add(0, 0.05, 0), player.getEyePosition(), colorOf(rune), 24, 0.9f);
        EliteBridge.sound(level, player.position(), SoundEvents.SMITHING_TABLE_USE, 1.0f, 1.3f);
        EliteBridge.actionbar(player, SkillMsg.t("runesmiths_insight.inscribed", "A %s rune glows on your %s.", name(rune), stack.getHoverName()).withStyle(ChatFormatting.GOLD));
        return true;
    }

    private void read(ServerPlayer player, ItemStack stack, long now) {
        CompoundTag t = EliteUtil.itemTag(stack);
        String rune = t.getString(RUNE);
        long left = Math.max(0, t.getLong(RUNE_UNTIL) - now) / 20;
        String runeText = rune.isEmpty() || left <= 0 ? "no rune" : rune.toLowerCase(Locale.ROOT) + " rune (" + left / 60 + "m " + left % 60 + "s)";
        int ench = stack.getEnchantments().size();
        String wear = stack.isDamageableItem() ? (100 * (stack.getMaxDamage() - stack.getDamageValue()) / Math.max(1, stack.getMaxDamage())) + "% durability" : "no wear";
        EliteBridge.actionbar(player, SkillMsg.t("runesmiths_insight.read", "%s: %s enchantments, %s, %s.", stack.getHoverName(), ench, wear, runeText).withStyle(ChatFormatting.YELLOW));
    }

    static String name(ExtraRules.Runesmith.Rune r) { return r.name().charAt(0) + r.name().substring(1).toLowerCase(Locale.ROOT); }

    static int colorOf(ExtraRules.Runesmith.Rune r) {
        return switch (r) { case FIRE -> 0xFFFF8A3A; case FROST -> 0xFF8AD8FF; case THUNDER -> 0xFFFFF29A; case VITALITY -> 0xFF8AFF9A; };
    }

    /** The rune on the stack if it is still active, else null. */
    static ExtraRules.Runesmith.Rune activeRune(ItemStack stack, long now) {
        if (stack.isEmpty()) return null;
        CompoundTag t = EliteUtil.itemTag(stack);
        if (!ExtraRules.Runesmith.active(now, t.getLong(RUNE_UNTIL))) return null;
        try { return ExtraRules.Runesmith.Rune.valueOf(t.getString(RUNE)); } catch (IllegalArgumentException none) { return null; }
    }

    /** A hit by a player holding a runed weapon: the rune's effect on the victim (and the wearer). {@code hit} is the damage that landed. */
    static void applyRune(ServerPlayer attacker, LivingEntity victim, ItemStack weapon, float hit) {
        if (applying) return;                                                  // the thunder bonus is itself a hit: it must not trigger the rune again
        long now = attacker.level().getGameTime();
        ExtraRules.Runesmith.Rune rune = activeRune(weapon, now);
        if (rune == null || hit <= 0f) return;
        boolean mastered = EliteUtil.itemTag(weapon).getBoolean(RUNE_MASTERED);
        applying = true;
        try {
            switch (rune) {
                case FIRE -> victim.setRemainingFireTicks(20 * ExtraRules.Runesmith.igniteSeconds(mastered));
                case FROST -> victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ExtraRules.Runesmith.frostSlowTicks(mastered), 1, true, false));
                case THUNDER -> EliteBridge.hit(attacker, victim, ExtraRules.Runesmith.thunderBonus(hit, mastered) * EliteSkillsConfig.damageScale(), 0.5, true);
                case VITALITY -> BalanceLaw.heal(attacker, (float) ExtraRules.Runesmith.vitalityHeal(hit, mastered));
            }
        } finally {
            applying = false;
        }
        EliteBridge.spray(attacker.serverLevel(), false, victim.position().add(0, victim.getBbHeight() * 0.6, 0), 4, 0.25, 0.3, 0.25, 0.03);
    }

    private static boolean applying;
}
