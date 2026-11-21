package com.newuniverse.nusmp.stones;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.item.CuriosLink;
import com.newuniverse.nusmp.relics.ActBody;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.order.UniqueLedger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Why: the acts every carrier shares. {@link #socket} sets the first stone of the player's pack that fits an empty socket (nothing leaves the pack before the carrier has really taken the stone: the write
 * is read back first), with the surge of socketing (inverse to the wearer's strength, never lethal); {@link #unsocket} gives every stone back; {@link #cast} runs a socketed stone's own act through the
 * carrier with the circuit's +25% effect; {@link #comboMend} (Soul + Time + Reality: a bounded mend of allies) and {@link #comboRift} (Space + Power: a rift along the look) are the two combos;
 * {@link #status} reads the sockets. All at most 12 targets, no spawn, no block change, no chunk load. Server side.
 */
final class CarrierActs {
    private CarrierActs() {}

    /** The first stone in the pack that fits an empty socket of this carrier, with the pack slot it sits in. */
    record Found(int slot, InfinityStone stone) {}

    static Found nextSocketable(ServerPlayer p, ItemStack carrier) {
        var def = Relics.defOf(carrier);
        if (def == null) return null;
        int allowed = CarrierRules.allowedMask(def.id()), have = CarrierData.mask(carrier);
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack in = inv.items.get(i);
            if (in.isEmpty() || in == carrier) continue;
            InfinityStone s = StoneSlot.stoneOf(in);
            if (s == null || !CarrierRules.has(allowed, s) || CarrierRules.has(have, s)) continue;
            if (!UniqueLedger.isCanonical(in)) continue;          // a duplicate stone is never socketed
            return new Found(i, s);
        }
        return null;
    }

    private static void refreshCaches(ServerPlayer p) {
        Relics.invalidate(p);
        CuriosLink.forget(p.getUUID());
    }

    // ================================================================ socket / unsocket
    static void socket(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack carrier = c.stack();
        Found f = nextSocketable(p, carrier);
        if (f == null) {
            Fx.say(p, "No stone in your pack fits an empty socket.");
            return;
        }
        ItemStack in = p.getInventory().items.get(f.slot());
        if (!CarrierData.put(carrier, f.stone(), in)) {
            Fx.say(p, "The socket would not take the stone.");
            return;
        }
        p.getInventory().items.set(f.slot(), ItemStack.EMPTY);          // only now, with the stone read back from the carrier
        refreshCaches(p);
        boolean nano = CarrierRules.isNano(c.def().id());
        if (StoneConfig.carrierSurge() && !p.isCreative() && !p.isSpectator()) {
            double frac = CarrierRules.surgeFraction(StoneSlot.baseEp(p), StoneConfig.epGate(), nano);
            float recoil = StoneRules.recoil(p.getMaxHealth(), p.getHealth(), frac);
            if (recoil > 0f) p.setHealth(p.getHealth() - recoil);
        }
        StoneActs.flash(p, f.stone(), p.position().add(0, 1.0, 0), 2.5);
        int mask = CarrierData.mask(carrier);
        Fx.say(p, "The " + f.stone().id() + " stone snaps into place (" + CarrierRules.count(mask) + " of " + CarrierRules.count(CarrierRules.allowedMask(c.def().id())) + ")"
                + (CarrierRules.boosted(mask, f.stone()) && StoneConfig.synergy() ? "; the circuit answers (+25%, -15% cooldown)." : "."));
    }

    static void unsocket(ActCtx c) {
        ServerPlayer p = c.player();
        List<ItemStack> back = CarrierData.takeAll(c.stack());
        for (ItemStack s : back) {
            if (!p.getInventory().add(s)) p.drop(s, false);
        }
        refreshCaches(p);
        CarrierState.forgetMotion(p.getUUID());
        Fx.say(p, back.isEmpty() ? "Nothing was socketed." : "The stones come out of the sockets (" + back.size() + ").");
    }

    // ================================================================ casting a socketed stone's act
    /** Runs the stone's own act body with the circuit's boost around it (the boost is read by {@link StoneActs#scale}). */
    static void cast(ActCtx c, InfinityStone s, ActBody body) {
        int mask = CarrierData.mask(c.stack());
        double boost = CarrierRules.effectFactor(mask, s, StoneConfig.synergy());
        CarrierState.setBoost(boost);
        try {
            if (boost > 1.0) Fx.say(c.player(), "The circuit lifts the " + s.id() + " stone (+25%).");
            body.run(c);
        } finally {
            CarrierState.setBoost(1.0);
        }
    }

    // ================================================================ combos
    /** The allies of a bearer: themselves, other players the bearer may not fight, the bearer's own pets. At most {@code cap}, nearest first. */
    static List<LivingEntity> allies(ServerPlayer p, double radius, int cap) {
        List<LivingEntity> out = new ArrayList<>(cap + 1);
        out.add(p);
        List<LivingEntity> near = p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(radius),
                e -> e != p && e.isAlive() && !e.isSpectator() && e.distanceToSqr(p) <= radius * radius && isAlly(p, e));
        near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        for (LivingEntity e : near) {
            if (out.size() >= cap) break;
            out.add(e);
        }
        return out;
    }

    static boolean isAlly(ServerPlayer p, LivingEntity e) {
        if (e instanceof Player o) return !o.isCreative() && (o.isAlliedTo(p) || !p.canHarmPlayer(o));
        if (e instanceof OwnableEntity own) return p.getUUID().equals(own.getOwnerUUID());
        return e.getPersistentData().hasUUID("nusmp_relic_owner") && p.getUUID().equals(e.getPersistentData().getUUID("nusmp_relic_owner"));
    }

    /** Combo Mend: allies within twelve blocks (at most twelve) are healed by 40% of their maximum health through the balance law and cleansed. */
    static void comboMend(ActCtx c) {
        ServerPlayer p = c.player();
        double boost = CarrierRules.effectFactor(CarrierData.mask(c.stack()), InfinityStone.SOUL, StoneConfig.synergy());
        float frac = (float) Math.min(0.5, CarrierRules.MEND_FRACTION * boost);
        int n = 0;
        for (LivingEntity t : allies(p, CarrierRules.MEND_RADIUS, CarrierRules.MEND_TARGETS)) {
            BalanceLaw.heal(t, t.getMaxHealth() * frac);
            SnapActs.cleanse(t);
            n++;
        }
        StoneActs.flash(p, InfinityStone.SOUL, p.position(), CarrierRules.MEND_RADIUS);
        StoneActs.flash(p, InfinityStone.TIME, p.position(), 6.0);
        Fx.say(p, "Soul, Time and Reality mend " + n + (n == 1 ? " life." : " lives."));
    }

    /** Combo Rift: Space and Power tear a line along the look, twenty-four blocks long; up to eight foes on it are hurt through the balance law (never moved). */
    static void comboRift(ActCtx c) {
        ServerPlayer p = c.player();
        double boost = CarrierRules.effectFactor(CarrierData.mask(c.stack()), InfinityStone.POWER, StoneConfig.synergy());
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        int n = 0;
        for (LivingEntity t : Fx.foesAround(p, CarrierRules.RIFT_RANGE)) {
            if (n >= CarrierRules.RIFT_TARGETS) break;
            Vec3 to = t.getBoundingBox().getCenter().subtract(eye);
            double along = to.dot(look);
            if (along < 0 || along > CarrierRules.RIFT_RANGE) continue;
            if (to.subtract(look.scale(along)).length() > 1.8) continue;
            StoneActs.hit(p, t, (float) (22.0 * StoneSlot.scale(p) * boost));
            n++;
        }
        for (int i = 1; i <= 3; i++) StoneActs.flash(p, i % 2 == 0 ? InfinityStone.SPACE : InfinityStone.POWER, eye.add(look.scale(6.0 * i)), 1.5);
        Fx.say(p, n == 0 ? "The rift tears the air and finds nothing." : "The rift cuts through " + n + (n == 1 ? " foe." : " foes."));
    }

    // ================================================================ reading
    static void status(ActCtx c) {
        ServerPlayer p = c.player();
        int mask = CarrierData.mask(c.stack());
        StringBuilder sb = new StringBuilder();
        for (InfinityStone s : InfinityStone.values()) {
            if (!CarrierRules.has(CarrierRules.allowedMask(c.def().id()), s)) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(s.id()).append(CarrierRules.has(mask, s) ? (CarrierRules.boosted(mask, s) ? " (circuit)" : " (set)") : " (empty)");
        }
        String extra = CarrierRules.isGauntlet(c.def().id()) ? " Snap mode: " + CarrierData.mode(c.stack()).title + "." : "";
        Fx.say(p, c.def().cardName() + ": " + sb + "." + extra);
    }
}
