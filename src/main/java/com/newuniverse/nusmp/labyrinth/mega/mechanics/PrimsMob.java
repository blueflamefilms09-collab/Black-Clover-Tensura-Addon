package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.newuniverse.nusmp.labyrinth.mega.enc.EncounterDirector;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Why: the primitives that put monsters into a mechanic or change how the monsters of its area behave: a band, an ambush from the dark, guards that hold their post, a swarm, one
 * elite, a double of a player, waves on a timer, buffs, a shield that holds until a switch is thrown, a tether that protects followers while their leader lives, a reflection of
 * blows, monsters that split when they die, a swap of places, the Dungeon Master asked for an ambush of his own. Spawning is delegated to {@link Mobs}; the rules that need to
 * read an incoming blow (shield, tether, reflect, split) are FLAGS on the instance that the Director's damage and death handlers consult. Eighteen of the eighty primitives.
 */
final class PrimsMob {
    private PrimsMob() {}

    private static int count(Run r) { return Math.max(1, (int) Math.round(r.num("count") * Math.max(0.5, r.inst.difficulty))); }

    static void register(Map<String, Prim> m) {
        m.put("spawn_group", Prims.once(r -> Mobs.band(r.inst, r.str("group"), count(r), (int) r.num("ring"), r.num("hp"), r.bool("hunt"), false)));
        m.put("spawn_ambush", Prims.once(r -> {
            Mobs.band(r.inst, r.str("group"), count(r), 9, r.num("hp"), true, false);
            Rukh.burst(r.inst, "dark", "ring", 8);
        }));
        m.put("spawn_guard", Prims.once(r -> Mobs.band(r.inst, r.str("group"), count(r), 5, r.num("hp"), false, r.bool("still"))));
        m.put("spawn_swarm", Prims.once(r -> {
            List<BlockPos> cells = Mobs.cells(r.inst, 6, 8);
            if (cells.isEmpty()) return;
            for (int i = 0; i < count(r); i++) Mobs.oneId(r.inst, "minecraft:" + r.str("kind"), cells.get(i % cells.size()), 0.6, false, true);
        }));
        m.put("spawn_elite", Prims.once(r -> {
            List<BlockPos> cells = Mobs.cells(r.inst, 5, 1);
            if (cells.isEmpty()) return;
            Mob e = Mobs.one(r.inst, r.str("group"), cells.get(0), r.num("hp"), false, true);
            if (e != null) {
                e.setGlowingTag(true);
                e.setCustomName(Messages.text("elite", "Elite"));
                if (r.inst.leader == null) r.inst.leader = e.getUUID();
            }
        }));
        m.put("buff_mobs", Prims.once(r -> {
            for (Mob b : r.inst.mobsInArea()) PrimUtil.addEffect(b, r.str("id"), r.ticks("secs"), (int) r.num("amp"));
        }));
        m.put("shield_mobs", Prims.timed(r -> r.ticks("secs"), r -> r.inst.setFlag("shield", 1, r.num("secs") <= 0 ? PrimsCreature.FOREVER : r.inst.clock() + r.ticks("secs")),
                r -> { }, r -> r.inst.clearFlag("shield")));
        m.put("unshield", Prims.once(r -> {
            r.inst.clearFlag("shield");
            Rukh.burst(r.inst, "light", "burst", 8);
        }));
        m.put("kill_mobs", Prims.once(r -> {
            for (Mob b : r.inst.ownMobs()) b.discard();
            r.inst.mobs.clear();
        }));
        m.put("heal_mobs", Prims.once(r -> {
            for (Mob b : r.inst.mobsInArea()) b.heal((float) (b.getMaxHealth() * r.num("pct") / 100.0));
        }));
        m.put("enrage", Prims.once(r -> {
            for (Mob b : r.inst.mobsInArea()) {
                PrimUtil.addEffect(b, "minecraft:speed", r.ticks("secs"), (int) Math.round(r.num("speed") * 3));
                PrimUtil.addEffect(b, "minecraft:strength", r.ticks("secs"), 0);
            }
        }));
        m.put("tether", Prims.timed(r -> Prims.FOREVER_TICKS, r -> r.inst.setFlag("tether", r.num("pct"), PrimsCreature.FOREVER), r -> { }, r -> r.inst.clearFlag("tether")));
        m.put("reflect", Prims.timed(r -> r.ticks("secs"), r -> r.inst.setFlag("reflect", r.num("pct"), r.inst.clock() + r.ticks("secs")), r -> { }, r -> r.inst.clearFlag("reflect")));
        m.put("summon_wave", Prims.timed(r -> (int) (r.num("waves") * r.num("period")) + 20, r -> {
            r.mem.put("w", 1);
            Mobs.band(r.inst, r.str("group"), count(r), 7, 1.0, true, false);
        }, r -> {
            int period = (int) r.num("period");
            int w = (Integer) r.mem.get("w");
            if (r.age() > 0 && r.age() % period == 0 && w < (int) r.num("waves")) {
                r.mem.put("w", w + 1);
                Mobs.band(r.inst, r.str("group"), count(r), 7, 1.0, true, false);
                Rukh.burst(r.inst, "dark", "ring", 8);
            }
        }, r -> { }));
        m.put("split_on_death", Prims.timed(r -> Prims.FOREVER_TICKS, r -> r.inst.setFlag("split", r.num("count"), PrimsCreature.FOREVER), r -> { }, r -> r.inst.clearFlag("split")));
        m.put("swap_mob", Prims.once(r -> {
            List<Mob> mobs = r.inst.mobsInArea();
            List<ServerPlayer> ps = r.players();
            if (mobs.isEmpty() || ps.isEmpty()) return;
            Mob mob = mobs.get(r.inst.rng.nextInt(mobs.size()));
            ServerPlayer p = ps.get(r.inst.rng.nextInt(ps.size()));
            Vec3 a = mob.position(), b = p.position();
            PrimUtil.teleport(mob, b.x, b.y, b.z);
            PrimUtil.teleport(p, a.x, a.y, a.z);
        }));
        m.put("doppelganger", Prims.once(r -> {
            List<ServerPlayer> ps = r.players();
            if (ps.isEmpty()) return;
            List<BlockPos> cells = Mobs.cells(r.inst, 5, (int) r.num("count"));
            for (int i = 0; i < Math.min(cells.size(), (int) r.num("count")); i++) {
                ServerPlayer p = ps.get(r.inst.rng.nextInt(ps.size()));
                Mob d = Mobs.oneId(r.inst, "minecraft:zombie", cells.get(i), 1.0, false, true);
                if (d == null) continue;
                d.setCustomName(p.getName().copy());
                d.setCustomNameVisible(true);
                for (EquipmentSlot s : EquipmentSlot.values()) {
                    if (s == EquipmentSlot.BODY) continue;
                    d.setItemSlot(s, p.getItemBySlot(s).copy());
                    d.setDropChance(s, 0.0f);
                }
                AttributeInstance hp = d.getAttribute(Attributes.MAX_HEALTH);
                if (hp != null) {
                    hp.setBaseValue(Math.max(hp.getBaseValue(), p.getMaxHealth()));
                    d.setHealth(d.getMaxHealth());
                }
                Rukh.at(r.inst, d.blockPosition(), "dark", "burst", 8);
            }
        }));
        m.put("call_dm", Prims.once(r -> {
            List<ServerPlayer> ps = r.players();
            if (!ps.isEmpty()) EncounterDirector.roll(r.level(), new ArrayList<>(ps), EncounterDirector.Force.admin());
        }));
    }
}
