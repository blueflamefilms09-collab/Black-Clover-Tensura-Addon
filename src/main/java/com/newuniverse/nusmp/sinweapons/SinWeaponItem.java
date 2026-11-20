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
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
    private static final String K_COVET = "nusmp_sin_covet", K_PASSIVE = "nusmp_sin_passive", K_ABILITY = "nusmp_sin_cd_";
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
        boolean passive = SinWeaponConfig.abilities() && SinWeaponConfig.techniques() && SinSkillService.mayUse(sp, stack);     // the passive of the owner's weapon (docs/sin_weapons_abilities.md)
        boolean atTarget = false, shined = false, fed = false;                                  // which picture the passive's proc gets
        switch (kind) {
            case SLOTH -> {
                if (SinWeaponConfig.techniques()) {
                    boolean slowed = passive && target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN);     // Sinking Weight: slowed already = a deeper, longer slow
                    Fx.addEffect(target, "minecraft:slowness", SinRules.hitSlowTicks(slowed), SinRules.hitSlowAmp(slowed));
                    atTarget = slowed;
                }
            }
            case ENVY -> {
                if (SinWeaponConfig.covet()) covet(sp, target);
                if (passive) {                                                                  // Glaring Eyes
                    Fx.addEffect(target, "minecraft:glowing", SinRules.GLARE_TICKS, 0);
                    atTarget = true;
                }
            }
            case VANITY -> {
                if (passive && passiveReady(sp)) {                                              // Vain Shine
                    Fx.shield(sp, SinRules.SHINE_ABSORB, SinRules.SHINE_TICKS);
                    shined = true;
                }
            }
            case CHAKRAMS -> {
                GreedForms.offBlow(sp, target);                                                 // the twin blow: empty off hand, 60%, one per 12 ticks, checked there
                if (passive && passiveReady(sp)) {                                              // Hungry Rings
                    Fx.heal(sp, SinRules.HUNGRY_HEAL);
                    fed = true;
                }
            }
            case HOLY -> {
                if (passive && target.getType().is(EntityTypeTags.UNDEAD)) {                    // Smiting Light
                    target.setRemainingFireTicks(SinRules.SMITE_FIRE_TICKS);
                    atTarget = true;
                }
            }
        }
        if (SinWeaponConfig.themedVfx() && SinWeaponConfig.techniques()) {                      // a plain hit: one small flash and at most one proc, two packets at most
            ServerLevel lv = (ServerLevel) sp.level();
            Vec3 at = SinFx.chest(target);
            if (shined) {
                SinFx.send(lv, kind, SinVfx.HIT, at, at);
                SinFx.send(lv, kind, SinVfx.PASSIVE, SinFx.chest(sp), SinFx.chest(sp));
            } else if (fed) {
                SinFx.send(lv, kind, SinVfx.HIT, at, at);
                SinFx.send(lv, kind, SinVfx.PASSIVE, at, SinFx.chest(sp));
            } else {
                SinFx.send(lv, kind, atTarget ? SinVfx.PASSIVE : SinVfx.HIT, at, at);
            }
        }
        return r;
    }

    /** The passive's own clock (one number in the wielder's persistent data): true, and the clock is set, when the kind's gap has passed. */
    private boolean passiveReady(ServerPlayer p) {
        int gap = SinRules.passiveGap(kind);
        long now = p.level().getGameTime();
        if (!SinRules.clockReady(now, p.getPersistentData().getLong(K_PASSIVE), gap)) return false;
        p.getPersistentData().putLong(K_PASSIVE, now + gap);
        return true;
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
        if (SinWeaponConfig.themedVfx() && SinWeaponConfig.techniques()) SinFx.send((ServerLevel) p.level(), SinRules.Kind.ENVY, SinVfx.STEAL, SinFx.chest(target), SinFx.chest(p));       // a thread from the victim to the wielder
        else spray((ServerLevel) p.level(), target.position().add(0, target.getBbHeight() * 0.6, 0), SinRules.PARTICLES_PER_TARGET, FxTone.LIGHT);
    }

    // ---------------------------------------------------------------- the right click
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp) || !SinWeaponConfig.techniques() || sp.isSpectator()) return InteractionResultHolder.pass(stack);
        if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (!SinSkillService.mayUse(sp, stack)) {                                              // a weapon bound to another player is a plain melee weapon (setting soulBound)
            SinSkillService.refuse(sp, this);
            return InteractionResultHolder.fail(stack);
        }
        if (technique(sp)) sp.getCooldowns().addCooldown(this, kind.cooldown);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    /** The technique as the skill's Technique mode asks for it: the same gates, cooldown and caps as the right click, but the owner check is the skill's own. Returns whether it ran. */
    public boolean fire(ServerPlayer sp) {
        if (!SinWeaponConfig.techniques() || sp.isSpectator() || sp.getCooldowns().isOnCooldown(this)) return false;
        if (technique(sp)) sp.getCooldowns().addCooldown(this, kind.cooldown);
        return true;
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
        boolean themed = SinWeaponConfig.themedVfx();
        if (themed) {                                                                           // one cast picture and a small puff for players who hide it; the old trail otherwise
            SinFx.cast(level, p, kind, SinVfx.TECH, kind == SinRules.Kind.SLOTH || kind == SinRules.Kind.CHAKRAMS, kind.reach);
            spray(level, eye.add(look), 3, FxTone.DARK);
        } else trail(level, eye, look);
        if (hit.isEmpty()) return kind != SinRules.Kind.SLOTH && kind != SinRules.Kind.CHAKRAMS;     // a shot or a thrust at nothing still costs; a slam with no one near does not
        int shown = 0;
        for (LivingEntity t : hit) {
            if (kind == SinRules.Kind.SLOTH) {
                Fx.addEffect(t, "minecraft:slowness", SinRules.WEIGHT_TICKS, SinRules.WEIGHT_SLOW_AMP);
                Fx.addEffect(t, "minecraft:mining_fatigue", SinRules.WEIGHT_TICKS, SinRules.WEIGHT_FATIGUE_AMP);
            } else {
                boolean extra = kind == SinRules.Kind.HOLY && t.getType().is(EntityTypeTags.UNDEAD);
                Fx.damage(p, t, SinRules.techniqueDamage(kind.techDamage, SinWeaponConfig.damagePercent(), extra), "minecraft:player_attack");
            }
            hitFx(level, t, themed, shown++);
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

    /** The picture of one creature hurt: a small themed flash (at most {@link SinRules#HIT_FX_MAX} per use) or, with the setting themedVfx off, the old puff. */
    private void hitFx(ServerLevel level, LivingEntity t, boolean themed, int shown) {
        Vec3 at = t.position().add(0, t.getBbHeight() * 0.55, 0);
        if (!themed) spray(level, at, SinRules.PARTICLES_PER_TARGET, FxTone.DARK);
        else if (shown < SinRules.HIT_FX_MAX) SinFx.send(level, kind, SinVfx.HIT, at, at);
    }

    // ---------------------------------------------------------------- the extra techniques (skill modes 3 and 4, docs/sin_weapons_abilities.md)
    /** The persistent-data key of one extra technique's own clock (the self-test resets it). */
    static String abilityKey(SinRules.Kind k, int slot) { return K_ABILITY + k.id + "_" + slot; }

    /**
     * An extra technique as the skill's modes ask for it: its OWN cooldown (a clock in the wielder's persistent data, the item cooldown of the right click is not touched), the setting {@code abilities} and the
     * owner check are the caller's and this method's. Returns whether it ran (and so started its cooldown). One box query, at most {@link SinRules.Ability#cap} creatures.
     */
    public boolean ability(ServerPlayer p, int slot) {
        SinRules.Ability a = SinRules.ability(kind, slot);
        if (a == null || !SinWeaponConfig.techniques() || !SinWeaponConfig.abilities() || p.isSpectator()) return false;
        long now = p.level().getGameTime();
        String key = abilityKey(kind, slot);
        long ready = p.getPersistentData().getLong(key);
        if (!SinRules.clockReady(now, ready, a.cooldown)) {
            p.displayClientMessage(Component.translatableWithFallback("sinweapons.ability.wait", "%s is not ready yet (%s s).", Component.translatableWithFallback(a.key(), a.english),
                    (SinRules.ticksLeft(now, ready, a.cooldown) + 19) / 20).withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        if (!runAbility(p, a)) return false;
        p.getPersistentData().putLong(key, now + a.cooldown);
        return true;
    }

    /** Runs one extra technique on the server (no cooldown check). Returns whether it did anything that costs its cooldown. */
    boolean runAbility(ServerPlayer p, SinRules.Ability a) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        boolean area = a.shape == SinRules.Shape.AREA;
        List<LivingEntity> foes = Fx.foesAround(p, area ? a.reach : a.reach + 1.0);
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 vert = right.cross(look).normalize();
        List<LivingEntity> hit = new ArrayList<>();
        for (LivingEntity t : foes) {
            if (hit.size() >= Math.min(a.cap, 24)) break;
            Vec3 c = t.getBoundingBox().getCenter().subtract(eye);
            double lateral = c.dot(right), vertical = c.dot(vert);
            if (SinRules.inAbility(a, c.dot(look), Math.sqrt(lateral * lateral + vertical * vertical), c.length(), t.getBbWidth() * 0.5)) hit.add(t);
        }
        if (hit.isEmpty() && !a.emptyCosts) return false;                                       // a slam with no one near costs nothing and shows nothing
        boolean themed = SinWeaponConfig.themedVfx();
        if (themed) {                                                                           // one cast picture and a small puff for players who hide it
            SinFx.cast(level, p, kind, SinVfx.styleOfSlot(a.slot), area, a.reach);
            spray(level, area ? p.position().add(0, 1.0, 0) : eye.add(look), 3, FxTone.DARK);
        } else spray(level, area ? p.position().add(0, 1.0, 0) : eye.add(look), 6, FxTone.DARK);
        if (a == SinRules.Ability.HOLY_PURGE) cleanse(p);
        if (a == SinRules.Ability.HOLY_GROUND) {
            Fx.heal(p, SinRules.GROUND_HEAL);
            Fx.addEffect(p, "minecraft:regeneration", SinRules.GROUND_REGEN_TICKS, 0);
        }
        int stolen = 0, shown = 0;
        for (LivingEntity t : hit) {
            boolean undeadHoly = kind == SinRules.Kind.HOLY && t.getType().is(EntityTypeTags.UNDEAD);
            switch (a) {
                case SLOTH_STILLNESS -> Fx.addEffect(t, "minecraft:slowness", SinRules.STILL_TICKS, SinRules.STILL_AMP);
                case SLOTH_GRAVITY -> {
                    abilityDamage(p, t, a, false);
                    shove(t, 0.0, -SinRules.push(SinRules.GRAVITY_DOWN), 0.0);
                }
                case ENVY_EYES -> {
                    Fx.addEffect(t, "minecraft:glowing", SinRules.EYES_TICKS, 0);
                    Fx.addEffect(t, "minecraft:weakness", SinRules.EYES_TICKS, 0);
                    stolen += steal(p, t, SinRules.EYES_STEAL_MAX - stolen);
                }
                case ENVY_LIFE, VANITY_REFLECT, CHAKRAMS_THROW -> abilityDamage(p, t, a, false);
                case VANITY_NARCISSUS -> {
                    Fx.addEffect(t, "minecraft:weakness", SinRules.NARCISSUS_WEAK_TICKS, 1);
                    Fx.addEffect(t, "minecraft:slowness", SinRules.NARCISSUS_SLOW_TICKS, SinRules.NARCISSUS_SLOW_AMP);
                }
                case CHAKRAMS_BIND -> {
                    Fx.addEffect(t, "minecraft:slowness", SinRules.BIND_TICKS, 1);
                    Fx.addEffect(t, "minecraft:weakness", SinRules.BIND_TICKS, 0);
                    Vec3 to = p.position().subtract(t.position());
                    Vec3 pull = to.lengthSqr() < 1.0e-6 ? Vec3.ZERO : to.normalize().scale(SinRules.push(SinRules.BIND_PULL));
                    shove(t, pull.x, 0.1, pull.z);
                }
                case HOLY_PURGE -> {
                    if (undeadHoly) abilityDamage(p, t, a, true);
                    else Fx.addEffect(t, "minecraft:glowing", SinRules.PURGE_GLOW_TICKS, 0);
                }
                case HOLY_GROUND -> {
                    if (undeadHoly) abilityDamage(p, t, a, true);
                    Vec3 away = t.position().subtract(p.position());
                    Vec3 push = away.lengthSqr() < 1.0e-6 ? Vec3.ZERO : away.normalize().scale(SinRules.push(SinRules.GROUND_PUSH));
                    shove(t, push.x, 0.3, push.z);
                }
            }
            hitFx(level, t, themed, shown++);
        }
        if (a == SinRules.Ability.ENVY_LIFE) Fx.heal(p, SinRules.lifeHeal(hit.size()));          // Stolen Life: health for every foe it drained
        return true;
    }

    /** The damage of an extra technique, only through the balance law. */
    private static void abilityDamage(ServerPlayer p, LivingEntity t, SinRules.Ability a, boolean undeadHoly) {
        if (a.damage > 0f) Fx.damage(p, t, SinRules.abilityDamage(a, SinWeaponConfig.damagePercent(), undeadHoly), "minecraft:player_attack");
    }

    private static void shove(LivingEntity t, double x, double y, double z) {
        t.push(x, y, z);
        t.hurtMarked = true;                                                                    // the client is told its new motion, as for a knock-back
    }

    /** Takes up to {@code room} beneficial effects from {@code t} and lends them to the wielder (each at most 10 s). Returns how many. */
    private static int steal(ServerPlayer p, LivingEntity t, int room) {
        int n = 0;
        for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects())) {
            if (n >= room) break;
            if (!e.getEffect().value().isBeneficial()) continue;
            t.removeEffect(e.getEffect());
            p.addEffect(new MobEffectInstance(e.getEffect(), SinRules.covetTicks(e.getDuration()), Math.min(e.getAmplifier(), 4)));
            n++;
        }
        return n;
    }

    /** Purge: removes up to {@link SinRules#CLEANSE_MAX} harmful effects from the wielder. */
    private static void cleanse(ServerPlayer p) {
        int n = 0;
        for (MobEffectInstance e : new ArrayList<>(p.getActiveEffects())) {
            if (n >= SinRules.CLEANSE_MAX) break;
            if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                p.removeEffect(e.getEffect());
                n++;
            }
        }
    }

    // ---------------------------------------------------------------- text
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatableWithFallback("item.anime_multiverse." + kind.id + ".lore", loreFallback()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatableWithFallback("sinweapons.tooltip.technique", "Right click: %s (cooldown %s s)",
                Component.translatableWithFallback(kind.techniqueKey(), kind.technique), kind.cooldown / 20).withStyle(ChatFormatting.DARK_GRAY));
        SinRules.Ability a1 = SinRules.ability(kind, 1), a2 = SinRules.ability(kind, 2);
        if (a1 != null && a2 != null) {                                                        // the extra techniques are the skill's modes 3 and 4; the passive works on every plain hit of the owner
            lines.add(Component.translatableWithFallback("sinweapons.tooltip.abilities", "Skill modes: %s (%s s), %s (%s s)", Component.translatableWithFallback(a1.key(), a1.english), a1.cooldown / 20,
                    Component.translatableWithFallback(a2.key(), a2.english), a2.cooldown / 20).withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.translatableWithFallback("sinweapons.tooltip.passive", "Passive: %s", Component.translatableWithFallback(SinRules.passiveKey(kind), SinRules.passiveName(kind))).withStyle(ChatFormatting.DARK_GRAY));
        }
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
