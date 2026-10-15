package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Optional;

/**
 * Why: wave 23's game-bus rules: what the three longbows add to an arrow that lands, the Sentence of Divine Mercy, the lute's rhythm, the four shields'
 * guard (spells swallowed, weapons softened, lightning refused, the death save), the ring's rolled boon and the horn's tally of the battle. Per-tick
 * work is staggered (a worn-relic check every 40 ticks, the horn's clock every 20) and bounded; every timer is an absolute game time in the player's
 * persistent data, so nothing here keeps static state at all.
 */
final class Wave23Events {
    private Wave23Events() {}

    static final String K_RIPOSTE = "nusmp_w23_riposte", K_DEATHSAVE = "nusmp_w23_deathsave", K_AEGIS_DAY = "nusmp_w23_aegis_day", K_AEGIS_USED = "nusmp_w23_aegis_used",
            K_AEGIS_BROKEN = "nusmp_w23_aegis_broken";

    private static final ResourceLocation EYE_ARMOR = ResourceLocation.fromNamespaceAndPath("nusmp", "relic_w23_eye_hide"),
            EYE_REGEN = ResourceLocation.fromNamespaceAndPath("nusmp", "relic_w23_eye_mind"),
            LEROY_LUCK = ResourceLocation.fromNamespaceAndPath("nusmp", "relic_w23_leroy_charisma");

    static void register() {
        NeoForge.EVENT_BUS.addListener(Wave23Events::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave23Events::onDamage);
        NeoForge.EVENT_BUS.addListener(Wave23Events::onDeath);
        NeoForge.EVENT_BUS.addListener(Wave23Events::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Wave23Events::onWake);
    }

    private static String pathOf(DamageSource src) { return src.typeHolder().unwrapKey().map(k -> k.location().getPath()).orElse(""); }

    private static Vec3 centre(LivingEntity e) { return new Vec3(e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ()); }

    private static void puff(LivingEntity e, RukhFx.Alignment a, int n) {
        if (!(e.level() instanceof ServerLevel l)) return;
        Vec3 c = centre(e);
        RukhFx.spray(l, a, c.x, c.y, c.z, n, 0.3, 0.4, 0.3, 0.05);
    }

    // ================================================================ damage in
    static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide() || Wave0Util.busy()) return;
        DamageSource src = e.getSource();
        String path = pathOf(src);
        long now = victim.level().getGameTime();
        if (src.getEntity() instanceof ServerPlayer atk && atk != victim) attacker(e, atk, victim, src, now);
        if (victim instanceof ServerPlayer vp && !e.isCanceled()) wielder(e, vp, src, path, now);
        if (!e.isCanceled()) bystander(e, victim, src, path, now);
    }

    /** What the bows (and the lute) add to a blow their wielder lands. */
    private static void attacker(LivingIncomingDamageEvent e, ServerPlayer atk, LivingEntity victim, DamageSource src, long now) {
        Entity direct = src.getDirectEntity();
        boolean arrow = direct instanceof AbstractArrow;
        if (RelicFlags.has(atk, Wave23.F_WARLUTE) && (arrow || direct == atk)) atk.getPersistentData().putLong(Wave23Acts.K_RHYTHM, now);    // Rhythm of Battle: the beat
        if (!arrow) return;
        if (RelicFlags.has(atk, Wave23.F_MERCY)) mercy(e, atk, victim, now);
        if (RelicFlags.has(atk, Wave23.F_REAVER)) {
            e.setAmount(e.getAmount() + Wave23Rules.REAVER_BONUS);
            puff(victim, RukhFx.Alignment.DARK, 6);
        }
    }

    /** Divine Mercy: +3, radiant damage (more against fiends and the undead), and the Sentence if it is armed. */
    private static void mercy(LivingIncomingDamageEvent e, ServerPlayer atk, LivingEntity victim, long now) {
        e.setAmount(e.getAmount() + Wave23Rules.MERCY_BONUS);
        Wave0Util.extra(atk, victim, Wave23Rules.radiant(Wave23Acts.fiendOrUndead(victim)), "tensura:holy_damage");
        puff(victim, RukhFx.Alignment.LIGHT, 10);
        CompoundTag d = atk.getPersistentData();
        if (d.getLong(Wave23Acts.K_SENTENCE) < now || !victim.isAlive()) return;
        if (!Wave23Rules.canSentence(victim.getMaxHealth())) {
            Wave23Acts.note(atk, victim.getName().getString() + " is too mighty for the Sentence; it stays armed.", ChatFormatting.GRAY);
            return;
        }
        d.remove(Wave23Acts.K_SENTENCE);
        boolean saved = Wave23Rules.sentenceSaved(atk.getRandom().nextDouble());
        float dmg = Wave23Rules.sentenceDamage(victim.getMaxHealth(), saved);
        victim.invulnerableTime = 0;
        Wave0Util.extra(atk, victim, dmg, "tensura:holy_damage");
        puff(victim, RukhFx.Alignment.LIGHT, 12);
        Wave23Acts.note(atk, saved ? victim.getName().getString() + " endures the Sentence." : "The Sentence is passed on " + victim.getName().getString() + ".", ChatFormatting.GOLD);
    }

    /** The shield and ring rules for a wielder who is hit. */
    private static void wielder(LivingIncomingDamageEvent e, ServerPlayer vp, DamageSource src, String path, long now) {
        CompoundTag d = vp.getPersistentData();
        boolean spell = Wave23Rules.isSpell(path);
        if (RelicFlags.has(vp, Wave23.F_AEGIS) && spell) {
            e.setAmount(e.getAmount() * (float) (1.0 - Wave23Rules.SPELLGUARD));                                 // Spellguard: spells strike you softer
            hunger(e, vp, vp, now);
        }
        if (RelicFlags.has(vp, Wave23.F_HEROS) && Wave23Rules.isWeaponPath(path) && !spell) {
            e.setAmount(e.getAmount() * (float) (1.0 - Wave23Rules.NONMAGIC_RESIST));                            // resistance to nonmagical weapons
        }
        if (RelicFlags.has(vp, Wave23.F_EYE) && spell && Wave23Acts.boon(vp) == Wave23Rules.BOON_WARD) e.setAmount(e.getAmount() * 0.9f);
        if (RelicFlags.has(vp, Wave23.F_CELESTIAL) && src.getEntity() instanceof LivingEntity foe && foe != vp && src.getDirectEntity() == foe
                && Wave23Rules.isCritical(e.getAmount(), vp.getMaxHealth()) && now >= d.getLong(K_RIPOSTE)) {
            d.putLong(K_RIPOSTE, now + Wave23Rules.RIPOSTE_COOLDOWN);
            Fx.damage(vp, foe, Wave23Rules.RIPOSTE_DAMAGE, "minecraft:player_attack");                           // the reaction: an attack back
            puff(foe, RukhFx.Alignment.LIGHT, 10);
            Wave23Acts.note(vp, "Celestial Support strikes back.", ChatFormatting.AQUA);
        }
    }

    /** Things that happen to someone beside a wielder: the Aegis swallows a spell aimed at an ally, the Thunder Shield turns lightning from the wielder's side. */
    private static void bystander(LivingIncomingDamageEvent e, LivingEntity victim, DamageSource src, String path, long now) {
        boolean spell = Wave23Rules.isSpell(path), lightning = Wave23Rules.isLightning(path);
        if (!spell && !lightning) return;
        List<? extends Player> players = victim.level().players();
        int looked = 0;
        for (Player pl : players) {
            if (looked++ > 64 || !(pl instanceof ServerPlayer sp) || !sp.isAlive()) continue;
            double d2 = sp.distanceToSqr(victim);
            if (lightning && d2 <= Wave23Rules.THUNDER_RADIUS * Wave23Rules.THUNDER_RADIUS && RelicFlags.has(sp, Wave23.F_THUNDER) && (victim == sp || Wave23Acts.ally(sp, victim))) {
                e.setCanceled(true);
                puff(victim, RukhFx.Alignment.LIGHT, 4);
                return;
            }
            if (spell && victim != sp && d2 <= Wave23Rules.HUNGER_RADIUS * Wave23Rules.HUNGER_RADIUS && RelicFlags.has(sp, Wave23.F_AEGIS) && Wave23Acts.ally(sp, victim)) {
                hunger(e, sp, victim, now);
                if (e.isCanceled()) return;
            }
        }
    }

    /** Arcane Hunger: the shield swallows the spell (twice a day). A spell of more than 90 damage shatters it for the day. */
    private static void hunger(LivingIncomingDamageEvent e, ServerPlayer wielder, LivingEntity victim, long now) {
        CompoundTag d = wielder.getPersistentData();
        if (now < d.getLong(K_AEGIS_BROKEN)) return;
        int left = Wave23Rules.hungerLeft(d.getLong(K_AEGIS_DAY), d.getInt(K_AEGIS_USED), now);
        if (left <= 0) return;
        long day = Wave23Rules.dayOf(now);
        d.putInt(K_AEGIS_USED, d.getLong(K_AEGIS_DAY) == day ? d.getInt(K_AEGIS_USED) + 1 : 1);
        d.putLong(K_AEGIS_DAY, day);
        float amount = e.getAmount();
        float swallowed = Wave23Rules.absorbed(amount);
        if (swallowed >= amount) e.setCanceled(true);
        else e.setAmount(amount - swallowed);
        Fx.shield(wielder, Wave23Rules.absorbGain(swallowed), 600);
        if (victim.level() instanceof ServerLevel l) {
            Vec3 c = centre(victim);
            RukhFx.spray(l, RukhFx.Alignment.DARK, c.x, c.y, c.z, 12, 0.4, 0.5, 0.4, 0.05);
        }
        if (Wave23Rules.shatters(amount)) {
            d.putLong(K_AEGIS_BROKEN, now + Wave23Rules.DAY);
            Fx.say(wielder, "The Aegis of the Abyss cracks under the spell: it hungers no more today.");
        } else {
            Wave23Acts.note(wielder, "Arcane Hunger swallows the spell (" + (left - 1) + " left today).", ChatFormatting.DARK_PURPLE);
        }
    }

    // ================================================================ damage out: the death save
    /** Celestial Support, +2 on death saving throws: a blow that would drop the wielder to zero leaves a half-heart (once every five minutes). */
    static void onDamage(LivingDamageEvent.Pre e) {
        if (!(e.getEntity() instanceof ServerPlayer vp) || vp.level().isClientSide() || !RelicFlags.has(vp, Wave23.F_CELESTIAL)) return;
        DamageSource src = e.getSource();
        if (src.is(DamageTypes.GENERIC_KILL) || src.is(DamageTypes.FELL_OUT_OF_WORLD) || src.is(DamageTypes.OUTSIDE_BORDER)) return;
        long now = vp.level().getGameTime();
        CompoundTag d = vp.getPersistentData();
        float health = vp.getHealth() + vp.getAbsorptionAmount();
        if (e.getNewDamage() < health || now < d.getLong(K_DEATHSAVE)) return;
        e.setNewDamage(Wave23Rules.deathSave(e.getNewDamage(), health));
        d.putLong(K_DEATHSAVE, now + Wave23Rules.DEATH_SAVE_COOLDOWN);
        Fx.addEffect(vp, "minecraft:regeneration", 100, 1);
        Fx.addEffect(vp, "minecraft:resistance", 60, 1);
        puff(vp, RukhFx.Alignment.LIGHT, 12);
        Fx.say(vp, "The winged shield holds you at the edge: a death save. It will not do this again for five minutes.");
    }

    // ================================================================ the horn's tally
    static void onDeath(LivingDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (dead.level().isClientSide()) return;
        long now = dead.level().getGameTime();
        if (dead instanceof ServerPlayer sp) {                                                   // the hornblower fell: the battle is lost where they lie
            if (sp.getPersistentData().getLong(Wave23Acts.K_HORN_UNTIL) > 0) {
                sp.getPersistentData().remove(Wave23Acts.K_HORN_UNTIL);
                sp.getPersistentData().remove(Wave23Acts.K_HORN_KILLS);
                sp.drop(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COOKED_CHICKEN, 8), false);
            }
            return;
        }
        if (!(dead instanceof Enemy) || dead.getPersistentData().hasUUID("nusmp_relic_owner")) return;
        int looked = 0;
        for (Player pl : dead.level().players()) {
            if (looked++ > 64 || !(pl instanceof ServerPlayer sp) || sp.distanceToSqr(dead) > Wave23Rules.HORN_RADIUS * Wave23Rules.HORN_RADIUS) continue;
            CompoundTag d = sp.getPersistentData();
            if (d.getLong(Wave23Acts.K_HORN_UNTIL) >= now) d.putInt(Wave23Acts.K_HORN_KILLS, Math.min(99, d.getInt(Wave23Acts.K_HORN_KILLS) + 1));
        }
    }

    // ================================================================ the ring's boon, charisma, the horn's clock
    static void onWake(PlayerWakeUpEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && !e.wakeImmediately() && RelicFlags.has(p, Wave23.F_EYE)) rollBoon(p, true);
    }

    private static void rollBoon(ServerPlayer p, boolean slept) {
        CompoundTag d = p.getPersistentData();
        long today = Wave23Rules.dayOf(p.level().getGameTime());
        if (!Wave23Rules.rerollDue(d.getLong(Wave23Acts.K_BOON_DAY), today, slept)) return;
        int boon = Wave23Rules.boon(p.getRandom().nextInt(4) + 1);
        d.putInt(Wave23Acts.K_BOON, boon);
        d.putLong(Wave23Acts.K_BOON_DAY, today + 1);
        Fx.say(p, "Ancient Twisted Magic (d4 = " + boon + "): " + Wave23Rules.boonName(boon) + " until your next long rest.");
        puff(p, RukhFx.Alignment.DARK, 12);
    }

    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 7) return;
        CompoundTag d = p.getPersistentData();
        long now = p.level().getGameTime();
        long horn = d.getLong(Wave23Acts.K_HORN_UNTIL);
        if (horn > 0 && now >= horn) Wave23Acts.endBattle(p, true);
        if (p.tickCount % 40 == 7) {
            boolean eye = RelicFlags.has(p, Wave23.F_EYE);
            if (eye) rollBoon(p, false);
            int boon = eye ? d.getInt(Wave23Acts.K_BOON) : 0;
            setMod(p, Optional.of(Attributes.ARMOR), EYE_ARMOR, boon == Wave23Rules.BOON_ARMOR ? 1.0 : 0.0, AttributeModifier.Operation.ADD_VALUE);
            setMod(p, attr("tensura:magicule_regeneration_multiplier"), EYE_REGEN, boon == Wave23Rules.BOON_MIND ? 0.10 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            setMod(p, Optional.of(Attributes.LUCK), LEROY_LUCK, Wave23Rules.charismaLuck(d.getInt(Wave23Acts.K_CHARISMA)), AttributeModifier.Operation.ADD_VALUE);
        }
    }

    private static Optional<Holder<Attribute>> attr(String id) {
        try {
            return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(id)).map(h -> (Holder<Attribute>) h);
        } catch (RuntimeException bad) {
            return Optional.empty();
        }
    }

    /** Keeps one transient modifier at {@code amount} (removed when 0); an attribute that does not exist is skipped. */
    private static void setMod(ServerPlayer p, Optional<Holder<Attribute>> attribute, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        if (attribute.isEmpty()) return;
        AttributeInstance inst = p.getAttribute(attribute.get());
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(id);
        if (cur != null && Math.abs(cur.amount() - amount) < 1.0e-9) return;
        if (cur != null) inst.removeModifier(id);
        if (amount != 0.0) inst.addTransientModifier(new AttributeModifier(id, amount, op));
    }
}
