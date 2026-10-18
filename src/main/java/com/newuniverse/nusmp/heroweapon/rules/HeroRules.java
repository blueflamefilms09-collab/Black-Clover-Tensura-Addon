package com.newuniverse.nusmp.heroweapon.rules;

import com.newuniverse.nusmp.heroweapon.HeroColors;
import com.newuniverse.nusmp.heroweapon.HeroConfig;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.HeroWeaponItem;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.item.MagicWeaponItem;
import com.newuniverse.nusmp.item.OtherworldTridentItem;
import com.newuniverse.nusmp.relics.RelicBearer;
import com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponItem;
import com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponRules;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.item.weapon.SimpleScytheItem;
import io.github.manasmods.tensura.item.weapon.SimpleSpearItem;
import io.github.manasmods.tensura.item.weapon.custom.VortexSpearItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.UseAnim;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the first restriction of the Holy Weapons (the owner's text: they "reject non-class weapons") needs the game-side half of the rule: reading an item's
 * facts (what Java class it is, which tags it carries, how it is used, how hard it hits, whether this mod declares its class) and handing them to the pure
 * {@link WeaponClassifier}; deciding, for a player and the item in their hand, whether the weapon rejects it; and delivering the shock. The shock is the
 * owner's "small magic hit and a slowing" and nothing more: the blow is cancelled by the caller, the damage is magic, configured, never lethal and at most once a
 * second, the slowness lasts a second by default, and the line "The Legendary X rejects the weapon" shows at most once every three seconds. Tools used as
 * tools are never affected (a pickaxe is not a weapon), creative and spectator players are exempt, and so is every wielder of a Seven Star or Vassal Weapon.
 * The shipped tag {@code #nusmp:hero_allowed} lists items that are never rejected (a datapack can add to it).
 */
public final class HeroRules {
    private HeroRules() {}

    private static final TagKey<Item> ALLOWED = tag("nusmp", "hero_allowed");
    private static final TagKey<Item> C_SPEAR = tag("c", "tools/spear"), C_BOW = tag("c", "tools/bow"), C_CROSSBOW = tag("c", "tools/crossbow"),
            C_SHIELD = tag("c", "tools/shield"), C_MACE = tag("c", "tools/mace");

    /** The shock's damage repeats at most this often (ticks), so holding the attack button cannot drain a wielder to half a heart in a second. */
    private static final int SHOCK_SPACING = 20;
    /** The rejection line shows at most this often (ticks): three seconds. */
    private static final int MESSAGE_SPACING = 60;

    /** Per player: the game time of the last shock damage and of the last rejection line. */
    private static final Map<UUID, long[]> SHOCKED = new HashMap<>();

    /**
     * The wielder whose shock damage is being dealt at this very moment: set and cleared around the one {@code hurt} call, so it never outlives it. The damage
     * listener reads it to give the shock its last word (see {@link #isShocking}).
     */
    private static ServerPlayer shocking;

    private static TagKey<Item> tag(String ns, String path) { return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ns, path)); }

    // ================================================================ reading an item
    /** The weapon class of an item stack ({@link WeaponClass#NONE} for anything that is not a weapon). */
    public static WeaponClass classOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return WeaponClass.NONE;
        Item item = stack.getItem();
        int flags = 0;
        if (stack.is(ALLOWED)) flags |= WeaponClassifier.ALLOWED;
        WeaponClass declared = declared(item);
        UseAnim use = stack.getUseAnimation();
        if (item instanceof ShieldItem || use == UseAnim.BLOCK || stack.is(C_SHIELD)) flags |= WeaponClassifier.SHIELD;
        if (item instanceof ProjectileWeaponItem || use == UseAnim.BOW || use == UseAnim.CROSSBOW || stack.is(C_BOW) || stack.is(C_CROSSBOW)) flags |= WeaponClassifier.BOW;
        if (item instanceof TridentItem || use == UseAnim.SPEAR || stack.is(C_SPEAR)) flags |= WeaponClassifier.SPEAR;
        if (item instanceof SwordItem || stack.is(ItemTags.SWORDS)) flags |= WeaponClassifier.SWORD;
        if (item instanceof AxeItem || stack.is(ItemTags.AXES)) flags |= WeaponClassifier.AXE;
        if (item instanceof MaceItem || stack.is(C_MACE)) flags |= WeaponClassifier.BLUNT;
        if (item instanceof DiggerItem) flags |= WeaponClassifier.TOOL;
        return WeaponClassifier.classify(new WeaponClassifier.Facts(declared, flags, attackBonus(stack)));
    }

    /**
     * The class an item declares for itself: the hero weapons, this mod's own weapons whose Java class says less than their lore does, and the two kinds of Tensura
     * weapon (spears, scythes) that its sword class would mislabel. Null when it declares none.
     */
    private static WeaponClass declared(Item item) {
        if (item instanceof HeroWeaponItem hero) return WeaponClassifier.ownClass(hero.kind());
        if (item instanceof MagicWeaponItem w) {
            return switch (w.kind) {
                case MAGEBANE_SHIELD -> WeaponClass.SHIELD;
                case BOLT_THROWER, CROSSBOW_OF_SWIFT_DEATH -> WeaponClass.BOW;
                case CLUB_OF_ROT, DOOM_HAMMER -> WeaponClass.BLUNT;
                case BUTCHER_CLEAVER -> WeaponClass.AXE;
                case HAND_OF_PROVIDENCE, QUICKLINGS_IVY -> WeaponClass.OTHER_WEAPON;
                default -> null;
            };
        }
        if (item instanceof OtherworldTridentItem) return WeaponClass.SPEAR;
        // Tensura builds its spears and scythes on its sword class, so the Java class alone would call them swords (a Spear hero would be shocked by a spear)
        if (item instanceof SimpleSpearItem || item instanceof VortexSpearItem) return WeaponClass.SPEAR;
        if (item instanceof SimpleScytheItem) return WeaponClass.OTHER_WEAPON;
        if (item instanceof DjinnWeaponItem dj) return djinnClass(DjinnWeaponRules.spec(dj.djinn()).kind());
        if (item instanceof RelicBearer rb && rb.relic().isRanged()) return WeaponClass.BOW;
        return null;
    }

    /** The class of a Djinn weapon by the kind of weapon its table row says it is. */
    static WeaponClass djinnClass(String kind) {
        return switch (kind) {
            case "lance", "trident", "glaive" -> WeaponClass.SPEAR;
            case "greatsword" -> WeaponClass.SWORD;
            case "axe" -> WeaponClass.AXE;
            case "bow" -> WeaponClass.BOW;
            case "staff" -> WeaponClass.BLUNT;
            default -> WeaponClass.OTHER_WEAPON;
        };
    }

    /** The sum of the stack's attack-damage modifiers that add to the damage in the main hand. */
    private static double attackBonus(ItemStack stack) {
        double[] sum = {0.0};
        stack.forEachModifier(EquipmentSlotGroup.MAINHAND, (attribute, modifier) -> {
            if (attribute.value() == Attributes.ATTACK_DAMAGE.value() && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) sum[0] += modifier.amount();
        });
        return sum[0];
    }

    // ================================================================ deciding
    /**
     * The class of weapon that this wielder's weapon rejects in {@code held}, or {@link WeaponClass#NONE} when the item is allowed. Nothing is rejected when the
     * system or the rule is off, for creative and spectator players, for a wielder of a family that does not reject, or for a player with no weapon.
     */
    public static WeaponClass rejectedClass(ServerPlayer p, ItemStack held) {
        if (!HeroConfig.enabled() || !HeroConfig.holyRejectsOthers() || p.isCreative() || p.isSpectator()) return WeaponClass.NONE;
        HeroKind kind = HeroBonds.kindOf(p);
        if (kind == null || !kind.heroClass().rejectsOthers()) return WeaponClass.NONE;
        WeaponClass c = classOf(held);
        return WeaponClassifier.rejects(kind, c) ? c : WeaponClass.NONE;
    }

    // ================================================================ the shock
    /** Delivers the shock to a wielder whose weapon rejects what they tried to use. The caller cancels the blow. Never lethal; never more than once a second. */
    public static void shock(ServerPlayer p) {
        HeroKind kind = HeroBonds.kindOf(p);
        if (kind == null) return;
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        long[] last = SHOCKED.computeIfAbsent(p.getUUID(), k -> new long[]{Spacing.LONG_AGO, Spacing.LONG_AGO});
        if (Spacing.due(now, last[0], SHOCK_SPACING)) {
            last[0] = now;
            float dmg = ShockRules.damage(HeroConfig.shockDamage(), p.getHealth());
            if (dmg > 0.0f) {
                shocking = p;
                try {
                    p.hurt(p.damageSources().magic(), dmg);
                } finally {
                    shocking = null;
                }
            }
            int slow = HeroConfig.shockSlowTicks();
            if (slow > 0) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slow, 1));
            VfxSpawn.send(level, VfxShape.BARRIER_FX3, p.position().add(0, 1.0, 0), p.position().add(0, 2.0, 0), HeroColors.argb(kind), 10, 0.9f);
            level.playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.9f, 0.6f);
        }
        if (Spacing.due(now, last[1], MESSAGE_SPACING)) {
            last[1] = now;
            if (HeroConfig.showMessages()) p.displayClientMessage(Component.translatable("hero.msg.rejects", Component.translatable("item.nusmp." + kind.itemId())), true);
        }
    }

    /**
     * Is this entity the wielder being shocked right now? The shock asks for a small amount, but another mod's listener may raise a magic hit on its way (a
     * damage multiplier, a weakness to magic); the damage listener uses this to cap the final amount so the shock still cannot kill.
     */
    static boolean isShocking(LivingEntity who) { return who != null && who == shocking; }

    /** Forgets one player's shock times (they left). */
    public static void forget(UUID player) { SHOCKED.remove(player); }

    /** Forgets everything (the server stopped). */
    public static void clear() {
        SHOCKED.clear();
        shocking = null;
    }
}
