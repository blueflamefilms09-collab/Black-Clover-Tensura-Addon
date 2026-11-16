package com.newuniverse.nusmp.sinweapons;

import com.newuniverse.nusmp.greed.GreedForms;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: one item class for the five Berserk of Gluttony weapons ({@link SinRules.Kind}): the Black Axe of Sloth, the Black Gunblade of Envy, the Black Spear of Vanity, the Black Chakrams and the Seifort Holy
 * Sword. Each is a sword-class melee weapon (a Better Combat file gives it its swings) with one right-click technique. All rules run on the server: the click is only a request, the cooldown is the game's own per-player
 * item cooldown, the targets are one box query of hostile creatures ({@code relics.Fx.foesAround}, at most 24, cut to the kind's cap) and every point of damage goes through the balance law. Nothing is stored per
 * tick; Covet's one timestamp lives in the wielder's persistent data. The Chakrams are a twin weapon: with the off hand empty each blow is followed by the smaller off-hand blow of the Greed twin forms.
 * Not creative-only by accident: like the Valkyrie lance they are in the creative tab and nowhere else (no loot, no recipe).
 */
public class SinWeaponItem extends SwordItem {
    private static final String K_COVET = "nusmp_sin_covet";
    private final SinRules.Kind kind;

    public SinWeaponItem(SinRules.Kind kind) {
        super(Tiers.NETHERITE, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant().attributes(SwordItem.createAttributes(Tiers.NETHERITE, kind.base, kind.speed)));
        this.kind = kind;
    }

    public SinRules.Kind kind() { return kind; }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatableWithFallback("item.anime_multiverse." + kind.id, kind.english).withStyle(Style.EMPTY.withColor(kind.rgb));
    }

    // ---------------------------------------------------------------- a plain hit
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean r = super.hurtEnemy(stack, target, attacker);
        if (!(attacker instanceof ServerPlayer sp) || sp.level().isClientSide) return r;
        switch (kind) {
            case SLOTH -> {
                if (SinWeaponConfig.techniques()) Fx.addEffect(target, "minecraft:slowness", SinRules.HIT_SLOW_TICKS, SinRules.HIT_SLOW_AMP);
            }
            case ENVY -> {
                if (SinWeaponConfig.covet()) covet(sp, target);
            }
            case CHAKRAMS -> GreedForms.offBlow(sp, target);                                  // the twin blow: empty off hand, 60%, one per 12 ticks, checked there
            default -> { }
        }
        return r;
    }

    /** Takes one beneficial effect from the target and lends it to the wielder (at most 10 s, at most one theft per 2 s). */
    private static void covet(ServerPlayer p, LivingEntity target) {
        if (target == p) return;
        long now = p.level().getGameTime();
        if (!SinRules.covetReady(now, p.getPersistentData().getLong(K_COVET))) return;
        MobEffectInstance taken = null;
        for (MobEffectInstance e : target.getActiveEffects()) {
            if (e.getEffect().value().isBeneficial()) { taken = e; break; }
        }
        if (taken == null) return;
        p.getPersistentData().putLong(K_COVET, now + SinRules.COVET_GAP_TICKS);
        target.removeEffect(taken.getEffect());
        p.addEffect(new MobEffectInstance(taken.getEffect(), SinRules.covetTicks(taken.getDuration()), Math.min(taken.getAmplifier(), 4)));
        spray((ServerLevel) p.level(), target.position().add(0, target.getBbHeight() * 0.6, 0), SinRules.PARTICLES_PER_TARGET, FxTone.LIGHT);
    }

    // ---------------------------------------------------------------- the right click
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp) || !SinWeaponConfig.techniques() || sp.isSpectator()) return InteractionResultHolder.pass(stack);
        if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (technique(sp)) sp.getCooldowns().addCooldown(this, kind.cooldown);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    /** Runs the kind's technique on the server. Returns whether it did anything (and so costs its cooldown). One box query at most; at most {@link SinRules#targetCap} creatures. */
    boolean technique(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        List<LivingEntity> foes = Fx.foesAround(p, kind == SinRules.Kind.SLOTH || kind == SinRules.Kind.CHAKRAMS ? kind.reach : kind.reach + 1.0);
        List<LivingEntity> hit = new ArrayList<>();
        Vec3 up = new Vec3(0, 1, 0), right = look.cross(up);
        right = right.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 vert = right.cross(look).normalize();
        for (LivingEntity t : foes) {
            if (hit.size() >= SinRules.targetCap(kind)) break;
            Vec3 c = t.getBoundingBox().getCenter().subtract(eye);
            double along = c.dot(look), lateral = c.dot(right), vertical = c.dot(vert), hw = t.getBbWidth() * 0.5;
            boolean in = switch (kind) {
                case SLOTH, CHAKRAMS -> c.lengthSqr() <= (kind.reach + hw) * (kind.reach + hw);
                case ENVY -> SinRules.onLine(along, Math.sqrt(lateral * lateral + vertical * vertical), kind.reach, 0.6, hw);
                case VANITY -> SinRules.onLine(along, Math.sqrt(lateral * lateral + vertical * vertical), kind.reach, SinRules.THRUST_RADIUS, hw);
                case HOLY -> SinRules.onCross(along, lateral, vertical, hw);
            };
            if (in) hit.add(t);
        }
        if (kind == SinRules.Kind.ENVY && hit.size() > 1) {                                    // one shot, one target: the nearest on the line
            LivingEntity first = hit.get(0);
            hit.clear();
            hit.add(first);
        }
        trail(level, eye, look);
        if (hit.isEmpty()) return kind != SinRules.Kind.SLOTH && kind != SinRules.Kind.CHAKRAMS;     // a shot or a thrust at nothing still costs; a slam with no one near does not
        for (LivingEntity t : hit) {
            if (kind == SinRules.Kind.SLOTH) {
                Fx.addEffect(t, "minecraft:slowness", SinRules.WEIGHT_TICKS, SinRules.WEIGHT_SLOW_AMP);
                Fx.addEffect(t, "minecraft:mining_fatigue", SinRules.WEIGHT_TICKS, SinRules.WEIGHT_FATIGUE_AMP);
            } else {
                boolean extra = kind == SinRules.Kind.HOLY && t.getType().is(EntityTypeTags.UNDEAD);
                Fx.damage(p, t, SinRules.techniqueDamage(kind.techDamage, SinWeaponConfig.damagePercent(), extra), "minecraft:player_attack");
            }
            spray(level, t.position().add(0, t.getBbHeight() * 0.55, 0), SinRules.PARTICLES_PER_TARGET, FxTone.DARK);
        }
        return true;
    }

    /** The picture of the technique: a few sprays along the line (shot, thrust, cross stem) or a ring (the others). Bounded by {@link SinRules#worstPacketsPerUse}. */
    private void trail(ServerLevel level, Vec3 eye, Vec3 look) {
        FxTone tone = FxTone.DARK;
        switch (kind) {
            case ENVY, VANITY, HOLY -> {
                for (int i = SinRules.TRAIL_STEP; i <= kind.reach; i += SinRules.TRAIL_STEP) {
                    Vec3 c = eye.add(look.scale(i));
                    spray(level, c, 1, tone);
                }
            }
            default -> {
                Vec3 c = eye.add(look.scale(1.0));
                spray(level, c, 6, tone);
            }
        }
    }

    private static void spray(ServerLevel level, Vec3 at, int count, FxTone tone) {
        com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.ITEM_ROTDOOM, tone, at.x, at.y, at.z, count, 0.15, 0.15, 0.15, 0.03);
    }

    // ---------------------------------------------------------------- text
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatableWithFallback("item.anime_multiverse." + kind.id + ".lore", loreFallback()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatableWithFallback("sinweapons.tooltip.technique", "Right click: %s (cooldown %s s)",
                Component.translatableWithFallback(kind.techniqueKey(), kind.technique), kind.cooldown / 20).withStyle(ChatFormatting.DARK_GRAY));
        if (kind == SinRules.Kind.CHAKRAMS) lines.add(Component.translatableWithFallback("greed.tooltip.twin", "Twin form: one in each hand. With the off hand empty every blow is followed by a second one from it (60%).").withStyle(ChatFormatting.DARK_GRAY));
    }

    private String loreFallback() {
        return switch (kind) {
            case SLOTH -> "A Weapon of Mortal Sin: a heavy black axe that drags its victims down.";
            case ENVY -> "A Weapon of Mortal Sin: a black gunblade that covets what its prey has.";
            case VANITY -> "A Weapon of Mortal Sin: a black spear with a mirror that admires itself.";
            case CHAKRAMS -> "A Weapon of Mortal Sin: a pair of black rings, one for each hand.";
            case HOLY -> "The holy sword Seifort: it cuts the dark and the dead.";
        };
    }
}
