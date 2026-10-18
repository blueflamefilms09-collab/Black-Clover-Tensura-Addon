package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the timed RULES a primitive can switch on (a shield on the monsters, a tether that protects followers, reflected blows, shared damage, a cursed hand, no natural healing,
 * the downed state, monsters that split) are not things that happen once; they change how damage and death are handled while they last. The primitive sets a flag on its instance;
 * the Director's damage / heal / death handlers call into here, which finds the instances whose area holds the creature and applies whichever flags are up. Every rule is a small
 * pure-looking function over the flag values so the event handler stays one screenful. A guard flag stops the rules from reacting to the damage they themselves cause.
 */
final class Rules {
    private Rules() {}

    private static boolean busy;

    /** The incoming-damage rules for a victim. May cancel the event or change its amount. */
    static void onIncoming(List<Instance> active, LivingIncomingDamageEvent e) {
        if (busy) return;
        LivingEntity victim = e.getEntity();
        DamageSource src = e.getSource();
        for (Instance in : active) {
            if (in.state == Instance.State.ENDED || !in.area.contains(victim)) continue;
            // a player is hurt
            if (victim instanceof ServerPlayer p) {
                in.onDamage(p, e.getAmount());
                if (Safety.affects(p)) {
                    if (in.hasFlag("downed") && e.getAmount() >= p.getHealth() && !in.downed.containsKey(p.getUUID())) {
                        e.setCanceled(true);
                        goDown(in, p);
                        return;
                    }
                    if (in.hasFlag("downed") && in.downed.containsKey(p.getUUID())) { e.setCanceled(true); return; }
                    double share = in.flag("share");
                    if (share > 0 && e.getAmount() > 0) {
                        List<ServerPlayer> mates = in.inside.stream().filter(x -> x != p && Safety.affects(x)).toList();
                        if (!mates.isEmpty()) {
                            float part = (float) (e.getAmount() * share / 100.0);
                            e.setAmount(e.getAmount() - part);
                            float each = part / mates.size();
                            busy = true;
                            try { for (ServerPlayer o : mates) o.hurt(in.level.damageSources().magic(), each); } finally { busy = false; }
                        }
                    }
                }
            }
            // a monster of the instance is hurt
            if (victim instanceof Mob mob && concerns(in, mob)) {
                if (in.hasFlag("shield")) {
                    e.setCanceled(true);
                    Rukh.at(in, mob.blockPosition(), "dark", "burst", 3);
                    if (src.getEntity() instanceof ServerPlayer a) a.displayClientMessage(Messages.text("shielded", "The foe is shielded."), true);
                    return;
                }
                double tether = in.flag("tether");
                if (tether > 0 && in.leader != null && !in.leader.equals(mob.getUUID()) && in.level.getEntity(in.leader) instanceof LivingEntity ld && ld.isAlive()) {
                    e.setAmount((float) (e.getAmount() * (1.0 - tether / 100.0)));
                    Rukh.at(in, mob.blockPosition(), "dark", "burst", 2);
                }
                double reflect = in.flag("reflect");
                if (reflect > 0 && src.getEntity() instanceof ServerPlayer a && e.getAmount() > 0) {
                    busy = true;
                    try { a.hurt(in.level.damageSources().magic(), (float) (e.getAmount() * reflect / 100.0)); } finally { busy = false; }
                    Rukh.at(in, a.blockPosition(), "light", "burst", 3);
                }
            }
            // a cursed hand
            if (src.getEntity() instanceof ServerPlayer atk && in.area.contains(atk) && in.hasFlag("curse") && victim != atk) {
                busy = true;
                try { atk.hurt(in.level.damageSources().magic(), (float) in.flag("curse")); } finally { busy = false; }
            }
        }
    }

    static boolean concerns(Instance in, Mob m) {
        return m.getTags().contains(in.mobTag()) || ((MegaDungeon.isDungeonMonster(m) || m instanceof com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity) && in.area.contains(m));
    }

    /** No natural healing in an area with the flag: cancels the heal. */
    static void onHeal(List<Instance> active, LivingHealEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        for (Instance in : active) {
            if (in.state != Instance.State.ENDED && in.hasFlag("noregen") && in.area.contains(p) && Safety.affects(p)) {
                e.setCanceled(true);
                return;
            }
        }
    }

    // ---------------------------------------------------------------- the downed state
    private static void goDown(Instance in, ServerPlayer p) {
        long until = in.clock() + (long) (in.flag("downed") * 20);
        in.downed.put(p.getUUID(), new long[] {until, 0});
        p.setHealth(1.0f);
        PrimUtil.addEffect(p, "minecraft:slowness", (int) (in.flag("downed") * 20), 3);
        PrimUtil.addEffect(p, "minecraft:glowing", (int) (in.flag("downed") * 20), 0);
        p.displayClientMessage(Messages.text("downed", "You are down! A friend must kneel beside you."), true);
        in.level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0f, 0.5f);
    }

    /** Ticks the downed players of an instance: a mate sneaking beside them lifts them up; out of time they die. */
    static void tickDowned(Instance in, long now) {
        if (in.downed.isEmpty()) return;
        for (Map.Entry<UUID, long[]> e : new java.util.ArrayList<>(in.downed.entrySet())) {
            ServerPlayer p = in.level.getServer().getPlayerList().getPlayer(e.getKey());
            if (p == null || !p.isAlive()) { in.downed.remove(e.getKey()); continue; }
            boolean mate = false;
            for (ServerPlayer o : in.inside) mate |= o != p && o.isShiftKeyDown() && o.distanceToSqr(p) < 6.25 && !in.downed.containsKey(o.getUUID());
            long[] st = e.getValue();
            st[1] = mate ? st[1] + 1 : Math.max(0, st[1] - 2);
            if (st[1] >= 40) {
                in.downed.remove(e.getKey());
                p.setHealth(Math.max(4.0f, p.getMaxHealth() * 0.3f));
                PrimUtil.removeEffect(p, "minecraft:slowness");
                PrimUtil.removeEffect(p, "minecraft:glowing");
                Rukh.at(in, p.blockPosition(), "light", "column", 8);
                p.displayClientMessage(Messages.text("revived", "You are back on your feet."), true);
            } else if (now >= st[0]) {
                in.downed.remove(e.getKey());
                busy = true;
                try { p.hurt(in.level.damageSources().genericKill(), Float.MAX_VALUE); } finally { busy = false; }
            }
        }
    }

    // ---------------------------------------------------------------- splitting
    /** A monster of the instance died with the split rule up: two smaller ones of its kind stand where it fell (and do not split again). */
    static void onMobDeath(Instance in, Mob dead) {
        double n = in.flag("split");
        if (n <= 0 || dead.getTags().contains("nusmp_mech_split") || !concerns(in, dead)) return;
        String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType()).toString();
        BlockPos at = dead.blockPosition();
        for (int i = 0; i < (int) n; i++) {
            Mob c = Mobs.oneId(in, id, at.offset(i - 1, 0, i % 2), 0.5, false, true);
            if (c != null) c.addTag("nusmp_mech_split");
        }
    }
}
