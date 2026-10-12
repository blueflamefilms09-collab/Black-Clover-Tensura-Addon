package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.book.SpellRuntime;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Soul Paladins: powerful hostile mobs that Lucius turns, with Soul Magic, into loyal and immortal knights of white and gold. No new
 * entity: a Paladin is an ordinary mob with a flag in its persistent data, the {@link ZogratisEffects#SOUL_PALADIN} effect (synced, so
 * the client can draw it differently; amplifier 1 means kneeling), more health and attack, and a place in Lucius's
 * {@link PaladinLedger}.
 * <ul>
 *   <li><b>Immortal while Lucius lives</b>: a blow that would kill a Paladin does not. It is <i>struck down</i> instead: one health
 *       point left, no AI, immune to everything, kneeling with a soul tether to Lucius. After {@link SoulMath#reviveTicks} it rises
 *       with full health, unless the soul is cut.</li>
 *   <li><b>Cutting the soul</b> (the counter): while a Paladin kneels, one anti-magic blow (a demon sword, the Anti-Magic Lord, the
 *       Anti-Magic grimoire) or one holy, soul, severance or spirit blow ({@link LuciusFx#soulCutting}) severs the tether: it dies for
 *       real, with its drops. And when Lucius falls (or leaves the world) every Paladin is freed: it is the mob it was before, mortal
 *       again (a kneeling one dies with its tether).</li>
 *   <li><b>Two game-bus handlers</b>, {@link #onDeath} and {@link #onIncomingDamage}, do the rest; both leave every other mob alone
 *       after one tag check.</li>
 * </ul>
 */
public final class SoulPaladins {
    private SoulPaladins() {}

    /** Persistent-data keys: the UUID of the Lucius it serves, and the flags for kneeling and for a severed soul. */
    static final String OWNER = "nusmp_paladin_of", DOWN = "nusmp_paladin_down", CUT = "nusmp_paladin_cut";
    private static final ResourceLocation HP_ID = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "soul_paladin_health");
    private static final ResourceLocation ATK_ID = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "soul_paladin_attack");

    // ---------------------------------------------------------------- questions (client and server)
    /** Is this a Soul Paladin (up or kneeling)? Works on the client too (through the synced effect). */
    public static boolean isPaladin(LivingEntity e) {
        return e.hasEffect(ZogratisEffects.SOUL_PALADIN) || e.getPersistentData().hasUUID(OWNER);
    }

    /** Is this Paladin kneeling (struck down, waiting to rise)? Works on the client too: the effect's amplifier is 1. */
    public static boolean isDowned(LivingEntity e) {
        MobEffectInstance i = e.getEffect(ZogratisEffects.SOUL_PALADIN);
        return i != null && i.getAmplifier() >= 1 || e.getPersistentData().getBoolean(DOWN);
    }

    // ---------------------------------------------------------------- making Paladins
    /** Can Lucius take this mob: a hostile, strong, but not a boss, not family, not a Paladin already. */
    static boolean eligible(Mob m) {
        return m.isAlive() && m instanceof Enemy && !ZogratisEntities.isFamily(m) && !isPaladin(m)
                && SoulMath.worthy(m.getMaxHealth()) && !SoulMath.isBossScale(m.getMaxHealth());
    }

    /**
     * Turns up to 'want' mobs within 40 blocks of Lucius into Paladins (the strongest first), never more Paladins than his power allows.
     * If there is no one to take and there is room, he calls a knight out of the dead: a Vindicator, raised as a Paladin. Returns how many
     * Paladins were made.
     */
    static int call(LuciusBoss lucius, int want) {
        if (!(lucius.level() instanceof ServerLevel sl)) return 0;
        int room = SoulMath.paladinCap(lucius.power()) - lucius.paladins().size();
        int made = 0;
        if (room > 0) {
            List<Mob> pool = new ArrayList<>(sl.getEntitiesOfClass(Mob.class, lucius.getBoundingBox().inflate(40), SoulPaladins::eligible));
            pool.sort(Comparator.comparingDouble((Mob m) -> -m.getMaxHealth()));
            for (Mob m : pool) {
                if (made >= Math.min(want, room)) break;
                if (convert(lucius, m)) made++;
            }
        }
        if (made == 0 && room > 0) {
            Mob knight = EntityType.VINDICATOR.create(sl);
            Vec3 at = LuciusFx.standable(sl, lucius, lucius.position().add(LuciusFx.rotateY(lucius.getViewVector(1f).multiply(1, 0, 1), 0.9).scale(3.5)));
            if (knight != null && at != null) {
                knight.moveTo(at.x, at.y, at.z, lucius.getYRot(), 0);
                knight.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                knight.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                for (EquipmentSlot s : EquipmentSlot.values()) knight.setDropChance(s, 0f);
                sl.addFreshEntity(knight);
                if (convert(lucius, knight)) made++;
                else knight.discard();
            }
        }
        return made;
    }

    /** Makes 'mob' a Paladin of 'lucius'. False if the ledger refused it (cap) or it already is one. */
    static boolean convert(LuciusBoss lucius, Mob mob) {
        if (!(mob.level() instanceof ServerLevel sl) || isPaladin(mob) || !lucius.paladins().add(mob.getUUID(), SoulMath.paladinCap(lucius.power()))) return false;
        CompoundTag d = mob.getPersistentData();
        d.putUUID(OWNER, lucius.getUUID());
        d.putBoolean(DOWN, false);
        d.putBoolean(CUT, false);
        buff(mob, lucius.power());
        mob.setPersistenceRequired();
        mob.setTarget(null);
        Vec3 at = mob.position().add(0, mob.getBbHeight() * 0.5, 0);
        Tells.ring(sl, mob.position(), 1.6, LuciusFx.GOLD);
        LuciusFx.sparkle(sl, at, 24, 0.5);
        LuciusFx.souls(sl, at, 14, 0.6);
        LuciusFx.beam(sl, lucius.position().add(0, lucius.getBbHeight() * 0.8, 0), at, LuciusFx.PALE);
        LuciusFx.sound(sl, mob.position(), SoundEvents.BEACON_ACTIVATE, 1.4f, 1.2f);
        return true;
    }

    /** More health, attack and armour, the marker effect, and a full heal. */
    static void buff(Mob mob, int power) {
        AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            double base = hp.getBaseValue();
            hp.addOrReplacePermanentModifier(new AttributeModifier(HP_ID, SoulMath.paladinHealth(base, power) - base, AttributeModifier.Operation.ADD_VALUE));
        }
        AttributeInstance atk = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) {
            double base = atk.getBaseValue();
            atk.addOrReplacePermanentModifier(new AttributeModifier(ATK_ID, SoulMath.paladinAttack(base, power) - base, AttributeModifier.Operation.ADD_VALUE));
        }
        mob.setHealth(mob.getMaxHealth());
        setKneeling(mob, false);
    }

    /** Puts the marker effect back on a standing Paladin (milk, a cleanse or a purge took it off) without healing it or touching its numbers. */
    static void renew(Mob mob) { setKneeling(mob, false); }

    private static void setKneeling(LivingEntity mob, boolean kneel) {
        mob.removeEffect(ZogratisEffects.SOUL_PALADIN);
        mob.addEffect(new MobEffectInstance(ZogratisEffects.SOUL_PALADIN, MobEffectInstance.INFINITE_DURATION, kneel ? 1 : 0, false, false, false));
    }

    // ---------------------------------------------------------------- down, rise, cut, free
    /** A Paladin was struck down: it kneels, and Lucius's ledger starts its revive timer. */
    static void down(LuciusBoss lucius, Mob mob) {
        mob.setHealth(1f);
        mob.getPersistentData().putBoolean(DOWN, true);
        mob.setTarget(null);
        mob.getNavigation().stop();
        mob.setNoAi(true);
        setKneeling(mob, true);
        lucius.paladinFell(mob);
        if (mob.level() instanceof ServerLevel sl) {
            Vec3 at = mob.position().add(0, mob.getBbHeight() * 0.5, 0);
            LuciusFx.souls(sl, at, 18, 0.5);
            LuciusFx.beam(sl, at, lucius.position().add(0, lucius.getBbHeight() * 0.8, 0), LuciusFx.SOUL);
            LuciusFx.sound(sl, mob.position(), SoundEvents.AMETHYST_BLOCK_BREAK, 1.2f, 0.8f);
        }
    }

    /** A kneeling Paladin gets up with full health. */
    static void rise(Mob mob) {
        mob.getPersistentData().putBoolean(DOWN, false);
        mob.setNoAi(false);
        mob.setHealth(mob.getMaxHealth());
        setKneeling(mob, false);
        if (mob.level() instanceof ServerLevel sl) {
            Vec3 at = mob.position().add(0, mob.getBbHeight() * 0.5, 0);
            Tells.ring(sl, mob.position(), 1.8, LuciusFx.GOLD);
            LuciusFx.sparkle(sl, at, 30, 0.6);
            LuciusFx.sound(sl, mob.position(), SoundEvents.BEACON_POWER_SELECT, 1.4f, 1.0f);
        }
    }

    /** The soul is cut: the Paladin dies for real, drops and all. */
    static void cut(Mob mob) {
        mob.getPersistentData().putBoolean(CUT, true);
        mob.setNoAi(false);
        if (mob.level() instanceof ServerLevel sl) {
            Vec3 at = mob.position().add(0, mob.getBbHeight() * 0.5, 0);
            LuciusFx.souls(sl, at, 30, 0.7);
            LuciusFx.sound(sl, mob.position(), SoundEvents.WITHER_BREAK_BLOCK, 0.9f, 1.6f);
            sl.players().stream().filter(p -> p.distanceToSqr(mob) < 48 * 48)
                    .forEach(p -> p.displayClientMessage(Component.literal("The Paladin's soul is cut. It will not rise.").withStyle(net.minecraft.ChatFormatting.WHITE), true));
        }
        mob.hurt(mob.damageSources().genericKill(), Float.MAX_VALUE);
    }

    /** Lucius is gone: the Paladin is freed. It is the mob it was before (its health, attack and armour back, mortal), and the marker goes. */
    static void free(Mob mob) {
        CompoundTag d = mob.getPersistentData();
        d.remove(OWNER);
        d.putBoolean(DOWN, false);
        mob.setNoAi(false);
        mob.removeEffect(ZogratisEffects.SOUL_PALADIN);
        AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.removeModifier(HP_ID);
        AttributeInstance atk = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) atk.removeModifier(ATK_ID);
        // only ever down to the smaller maximum: free() also runs from inside a death event, where raising the health of a dying mob
        // would leave a body that is flagged dead but has health, and so never finishes dying
        if (mob.getHealth() > mob.getMaxHealth()) mob.setHealth(mob.getMaxHealth());
    }

    /** Frees every Paladin of 'lucius' (when he dies or leaves the world); a kneeling one just dies, its tether gone. */
    static void releaseAll(LuciusBoss lucius, ServerLevel sl) {
        for (UUID id : lucius.paladins().ids()) {
            if (sl.getEntity(id) instanceof Mob m && m.isAlive()) {
                boolean kneeling = isDowned(m);
                free(m);
                if (kneeling) {
                    m.getPersistentData().putBoolean(CUT, true);
                    m.hurt(m.damageSources().genericKill(), Float.MAX_VALUE);
                }
            }
        }
        lucius.paladins().clear();
    }

    // ---------------------------------------------------------------- game-bus handlers
    /** A Paladin that would die is struck down instead, while its Lucius lives and its soul is whole. */
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide || !(v instanceof Mob mob)) return;
        CompoundTag d = mob.getPersistentData();
        if (!d.hasUUID(OWNER)) return;
        if (d.getBoolean(CUT) || e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (mob.level() instanceof ServerLevel sl && sl.getEntity(d.getUUID(OWNER)) instanceof LuciusBoss b) b.paladins().remove(mob.getUUID());
            d.remove(OWNER);
            return;
        }
        if (!(mob.level() instanceof ServerLevel sl) || !(sl.getEntity(d.getUUID(OWNER)) instanceof LuciusBoss b) || !b.isAlive()) {
            free(mob);
            return;
        }
        // a Paladin the ledger lost track of (it had been out of a loaded chunk when the ledger looked) is taken back if there is room under the cap
        if (b.paladins().stateOf(mob.getUUID()) == null && !b.paladins().add(mob.getUUID(), SoulMath.paladinCap(b.power()))) {
            free(mob);
            return;
        }
        e.setCanceled(true);
        down(b, mob);
    }

    /** A kneeling Paladin takes nothing, except the blow that cuts its soul. */
    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide || !(v instanceof Mob mob) || !mob.getPersistentData().getBoolean(DOWN)) return;
        DamageSource src = e.getSource();
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        e.setCanceled(true);
        if (mob.level() instanceof ServerLevel sl) {
            if (LuciusFx.soulCutting(src)) SpellRuntime.later(sl, 1, LuciusFx.safe("soul cut", () -> { if (mob.isAlive() && mob.getPersistentData().getBoolean(DOWN)) cut(mob); }));
            else LuciusFx.sparkle(sl, mob.position().add(0, mob.getBbHeight() * 0.6, 0), 4, 0.3);
        }
    }
}
