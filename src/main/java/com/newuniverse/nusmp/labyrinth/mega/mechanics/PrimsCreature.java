package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the primitives that act on creatures (mostly the players of the area): effects, damage and healing, shoves, teleports, item and inventory tricks, experience, magicules, and the
 * timed RULES (gravity, burden, walking speed, the tie between players, shared damage, the downed state, a cursed hand, no natural healing). The timed rules that other events must
 * read (reflect, shared damage, the cursed hand, downed, no regeneration) are FLAGS on the instance: the primitive sets the flag for its duration and the Director's event handlers
 * consult it. Nothing here touches a block. Thirty-five of the library's eighty primitives.
 */
final class PrimsCreature {
    private PrimsCreature() {}

    static final long FOREVER = Long.MAX_VALUE / 4;

    static void register(Map<String, Prim> m) {
        m.put("effect", Prims.once(r -> {
            for (LivingEntity t : r.targets("players")) PrimUtil.addEffect(t, r.str("id"), r.ticks("secs"), (int) r.num("amp"));
        }));
        m.put("damage", Prims.once(r -> {
            for (LivingEntity t : r.targets("players")) PrimUtil.hurt(r.level(), t, r.hard("amount") + t.getMaxHealth() * r.hard("pct") / 100.0, r.str("kind"));
        }));
        m.put("heal", Prims.once(r -> {
            for (LivingEntity t : r.targets("players")) t.heal((float) (r.num("amount") + t.getMaxHealth() * r.num("pct") / 100.0));
        }));
        m.put("knockback", Prims.once(r -> {
            double power = r.num("power"), up = r.num("up");
            String from = r.str("from");
            for (LivingEntity t : r.targets("players")) {
                Vec3 d;
                if (from.equals("random")) {
                    double a = r.inst.rng.nextDouble() * Math.PI * 2;
                    d = new Vec3(Math.cos(a), 0, Math.sin(a));
                } else if (from.equals("player") && !r.players().isEmpty()) {
                    LivingEntity o = r.players().get(0) == t && r.players().size() > 1 ? r.players().get(1) : r.players().get(0);
                    d = PrimUtil.flat(t, o.getX(), o.getZ(), r.inst.rng);
                } else d = PrimUtil.flat(t, r.inst.anchor.getX() + 0.5, r.inst.anchor.getZ() + 0.5, r.inst.rng);
                PrimUtil.push(t, d.x * power, up, d.z * power);
            }
        }));
        m.put("pull", Prims.once(r -> {
            double power = r.num("power");
            for (LivingEntity t : r.targets("players")) {
                Vec3 d = PrimUtil.flat(t, r.inst.anchor.getX() + 0.5, r.inst.anchor.getZ() + 0.5, r.inst.rng);
                PrimUtil.push(t, -d.x * power, 0.12, -d.z * power);
            }
        }));
        m.put("launch", Prims.once(r -> {
            for (LivingEntity t : r.targets("players")) PrimUtil.push(t, 0, r.num("power"), 0);
        }));
        m.put("dash", Prims.once(r -> {
            double power = r.num("power");
            String dir = r.str("dir");
            for (LivingEntity t : r.targets("players")) {
                Vec3 d = switch (dir) {
                    case "north" -> new Vec3(0, 0, -1);
                    case "south" -> new Vec3(0, 0, 1);
                    case "east" -> new Vec3(1, 0, 0);
                    case "west" -> new Vec3(-1, 0, 0);
                    case "toward" -> PrimUtil.flat(t, r.inst.anchor.getX() + 0.5, r.inst.anchor.getZ() + 0.5, r.inst.rng).scale(-1);
                    case "away" -> PrimUtil.flat(t, r.inst.anchor.getX() + 0.5, r.inst.anchor.getZ() + 0.5, r.inst.rng);
                    default -> {
                        double a = r.inst.rng.nextDouble() * Math.PI * 2;
                        yield new Vec3(Math.cos(a), 0, Math.sin(a));
                    }
                };
                PrimUtil.push(t, d.x * power, 0.2, d.z * power);
            }
        }));
        m.put("ignite", Prims.once(r -> {
            for (LivingEntity t : r.targets("players")) t.igniteForSeconds((float) r.num("secs"));
        }));
        m.put("freeze", Prims.once(r -> {
            for (LivingEntity t : r.targets("players")) t.setTicksFrozen(t.getTicksRequiredToFreeze() + r.ticks("secs"));
        }));
        m.put("starve", Prims.once(r -> {
            for (ServerPlayer p : r.players()) p.getFoodData().setFoodLevel(Math.max(0, p.getFoodData().getFoodLevel() - (int) Math.round(r.num("amount"))));
        }));
        m.put("drain_mana", Prims.once(r -> {
            for (ServerPlayer p : r.players()) Mana.add(p, -r.hard("pct") / 100.0);
        }));
        m.put("restore_mana", Prims.once(r -> {
            for (ServerPlayer p : r.players()) Mana.add(p, r.num("pct") / 100.0);
        }));
        m.put("xp", Prims.once(r -> {
            int n = (int) Math.round(r.num("levels"));
            for (ServerPlayer p : r.players()) p.giveExperienceLevels(n < 0 ? -Math.min(-n, p.experienceLevel) : n);
        }));
        m.put("gravity", Prims.timed(r -> r.ticks("secs"), r -> { }, r -> {
            String mode = r.str("mode");
            for (LivingEntity t : r.targets("players")) {
                Vec3 v = t.getDeltaMovement();
                switch (mode) {
                    case "up" -> { t.setDeltaMovement(v.x * 0.9, Math.min(0.45, v.y + 0.07), v.z * 0.9); t.fallDistance = 0; }
                    case "float" -> { t.setDeltaMovement(v.x, v.y * 0.6 + 0.02, v.z); t.fallDistance = 0; }
                    case "heavy" -> t.setDeltaMovement(v.x * 0.8, Math.min(v.y, 0.0) - 0.15, v.z * 0.8);
                    default -> t.setDeltaMovement(v.x - v.z * 0.12, v.y, v.z + v.x * 0.12);
                }
                t.hurtMarked = true;
            }
        }, r -> { }));
        m.put("root", Prims.once(r -> {
            for (LivingEntity t : r.targets("players")) {
                PrimUtil.addEffect(t, "minecraft:slowness", r.ticks("secs"), 6);
                PrimUtil.addEffect(t, "minecraft:jump_boost", r.ticks("secs"), 128);
            }
        }));
        m.put("tp_random", Prims.once(r -> {
            int rad = (int) r.num("radius");
            for (LivingEntity t : r.targets("players")) {
                for (int tries = 0; tries < 16; tries++) {
                    BlockPos p = r.inst.anchor.offset(r.inst.rng.nextInt(rad * 2 + 1) - rad, 0, r.inst.rng.nextInt(rad * 2 + 1) - rad);
                    if (Mobs.fit(r.inst, p)) {
                        PrimUtil.teleport(t, p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
                        Rukh.at(r.inst, p, "auto", "burst", 6);
                        break;
                    }
                }
            }
        }));
        m.put("tp_anchor", Prims.once(r -> {
            BlockPos a = r.inst.anchor;
            for (LivingEntity t : r.targets("players")) PrimUtil.teleport(t, a.getX() + 0.5, a.getY(), a.getZ() + 0.5);
        }));
        m.put("tp_swap", Prims.once(r -> {
            List<ServerPlayer> ps = r.players();
            if (ps.size() < 2) return;
            int i = r.inst.rng.nextInt(ps.size()), j = (i + 1 + r.inst.rng.nextInt(ps.size() - 1)) % ps.size();
            ServerPlayer a = ps.get(i), b = ps.get(j);
            Vec3 pa = a.position(), pb = b.position();
            PrimUtil.teleport(a, pb.x, pb.y, pb.z);
            PrimUtil.teleport(b, pa.x, pa.y, pa.z);
        }));
        m.put("tp_entry", Prims.once(r -> {
            for (ServerPlayer p : r.players()) {
                BlockPos e = r.inst.entry.getOrDefault(p.getUUID(), r.inst.anchor);
                PrimUtil.teleport(p, e.getX() + 0.5, e.getY(), e.getZ() + 0.5);
            }
        }));
        m.put("tp_room", Prims.once(r -> {
            if (r.inst.sector == null) return;
            List<Room> rooms = new ArrayList<>();
            for (Room x : r.inst.sector.rooms) {
                if (x.kind == RoomKind.ARENA || x.kind == RoomKind.VAULT || x.kind == RoomKind.ANTECHAMBER || x.kind == RoomKind.ONWARD || x.kind == RoomKind.CAP) continue;
                rooms.add(x);
            }
            rooms.sort(Comparator.comparingInt(x -> x.id));
            if (rooms.isEmpty()) return;
            String mode = r.str("mode");
            for (ServerPlayer p : r.players()) {
                Room cur = r.inst.sector.roomAt(p.blockPosition());
                int at = -1;
                for (int i = 0; i < rooms.size(); i++) if (cur != null && rooms.get(i).id == cur.id) at = i;
                Room dest = switch (mode) {
                    case "next" -> rooms.get(Math.floorMod(at + 1, rooms.size()));
                    case "prev" -> rooms.get(Math.floorMod(at - 1, rooms.size()));
                    case "first" -> rooms.get(0);
                    case "last" -> rooms.get(rooms.size() - 1);
                    default -> rooms.get(r.inst.rng.nextInt(rooms.size()));
                };
                BlockPos c = dest.center();
                BlockPos spot = null;
                for (int[] o : new int[][] {{0, 0}, {2, 0}, {-2, 0}, {0, 2}, {0, -2}, {3, 3}, {-3, -3}}) {
                    BlockPos q = c.offset(o[0], 0, o[1]);
                    if (r.level().hasChunkAt(q) && !r.level().getBlockState(q.below()).getCollisionShape(r.level(), q.below()).isEmpty() && r.level().getBlockState(q).getCollisionShape(r.level(), q).isEmpty()
                            && r.level().getBlockState(q.above()).getCollisionShape(r.level(), q.above()).isEmpty()) { spot = q; break; }
                }
                if (spot != null) PrimUtil.teleport(p, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
            }
        }));
        m.put("mark", Prims.once(r -> {
            for (LivingEntity t : r.targets("players")) PrimUtil.addEffect(t, "minecraft:glowing", r.ticks("secs"), 0);
        }));
        m.put("shuffle_hotbar", Prims.once(r -> {
            for (ServerPlayer p : r.players()) {
                int a = r.inst.rng.nextInt(9), b = (a + 1 + r.inst.rng.nextInt(8)) % 9;
                ItemStack x = p.getInventory().items.get(a);
                p.getInventory().items.set(a, p.getInventory().items.get(b));
                p.getInventory().items.set(b, x);
                p.getInventory().setChanged();
            }
        }));
        m.put("item_cooldown", Prims.once(r -> {
            for (ServerPlayer p : r.players()) {
                Set<Item> seen = new HashSet<>();
                for (ItemStack s : p.getInventory().items) if (!s.isEmpty() && seen.add(s.getItem())) p.getCooldowns().addCooldown(s.getItem(), r.ticks("secs"));
            }
        }));
        m.put("curse_hand", Prims.timed(r -> r.ticks("secs"), r -> r.inst.setFlag("curse", r.hard("dmg"), r.inst.clock() + r.ticks("secs")), r -> { }, r -> r.inst.clearFlag("curse")));
        m.put("take_item", Prims.once(r -> {
            Item it = Counters.item(r.str("item"));
            if (it == null) return;
            for (ServerPlayer p : r.players()) {
                int have = 0;
                for (ItemStack s : p.getInventory().items) if (s.is(it)) have += s.getCount();
                Counters.removeItems(p, it, Math.min(have, (int) r.num("count")));
            }
        }));
        m.put("give_item", Prims.once(r -> {
            Item it = Counters.item(r.str("item"));
            if (it == null) return;
            for (ServerPlayer p : r.players()) {
                ItemStack s = new ItemStack(it, (int) r.num("count"));
                if (!p.getInventory().add(s)) p.drop(s, false);
            }
        }));
        m.put("give_loot", Prims.once(r -> {
            ResourceLocation t = ResourceLocation.tryParse(r.str("table"));
            if (t == null) return;
            ServerPlayer lucky = r.players().isEmpty() ? null : r.players().get(0);
            Rewards.loot(r.level(), t, (int) r.num("rolls"), r.inst.anchor, lucky, r.inst.rng.nextLong());
        }));
        m.put("xp_orbs", Prims.once(r -> Rewards.xp(r.level(), r.inst.anchor, (int) (r.num("amount") * (1.0 + 0.4 * Scaling.s(r.floor()))))));
        m.put("burden", Prims.timed(r -> r.ticks("secs"), r -> { }, r -> {
            if (r.age() % 20 != 0) return;
            for (ServerPlayer p : r.players()) {
                int filled = 0;
                for (ItemStack s : p.getInventory().items) if (!s.isEmpty()) filled++;
                int amp = Math.min(3, filled / Math.max(1, (int) r.num("per")) - 1);
                if (amp >= 0) PrimUtil.addEffect(p, "minecraft:slowness", 30, amp);
            }
        }, r -> { }));
        m.put("speed", Prims.timed(r -> r.ticks("secs"), r -> { }, r -> {
            @SuppressWarnings("unchecked")
            Set<UUID> have = (Set<UUID>) r.mem.computeIfAbsent("have", k -> new HashSet<UUID>());
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("nusmp", "mech_speed_" + Integer.toHexString(r.inst.key.hashCode()));
            for (LivingEntity t : r.targets("players")) {
                if (!have.add(t.getUUID())) continue;
                AttributeInstance a = t.getAttribute(Attributes.MOVEMENT_SPEED);
                if (a != null) a.addOrUpdateTransientModifier(new AttributeModifier(id, r.num("factor"), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }, r -> {
            @SuppressWarnings("unchecked")
            Set<UUID> have = (Set<UUID>) r.mem.get("have");
            if (have == null) return;
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("nusmp", "mech_speed_" + Integer.toHexString(r.inst.key.hashCode()));
            for (UUID u : have) {
                LivingEntity t = r.level().getEntity(u) instanceof LivingEntity found ? found : null;
                for (ServerPlayer p : r.inst.inside) if (p.getUUID().equals(u)) t = p;
                if (t != null) {
                    AttributeInstance a = t.getAttribute(Attributes.MOVEMENT_SPEED);
                    if (a != null) a.removeModifier(id);
                }
            }
        }));
        m.put("disorient", Prims.once(r -> {
            for (ServerPlayer p : r.players()) {
                float yaw = r.inst.rng.nextFloat() * 360f - 180f;
                if (p.connection != null) p.connection.teleport(p.getX(), p.getY(), p.getZ(), yaw, p.getXRot());
                else p.setYRot(yaw);
            }
        }));
        m.put("link", Prims.timed(r -> r.ticks("secs"), r -> { }, r -> {
            if (r.age() % 10 != 0) return;
            List<ServerPlayer> ps = r.players();
            double rad = r.num("radius");
            for (int i = 0; i < ps.size(); i++) {
                for (int j = i + 1; j < ps.size(); j++) {
                    ServerPlayer a = ps.get(i), b = ps.get(j);
                    if (a.distanceTo(b) <= rad) continue;
                    PrimUtil.hurt(r.level(), a, r.hard("dmg"), "magic");
                    PrimUtil.hurt(r.level(), b, r.hard("dmg"), "magic");
                    Vec3 d = b.position().subtract(a.position()).normalize().scale(0.35);
                    PrimUtil.push(a, d.x, 0.1, d.z);
                    PrimUtil.push(b, -d.x, 0.1, -d.z);
                    com.newuniverse.nusmp.particle.rukh.RukhFx.stream(r.level(), a, b, Rukh.align(r.inst, "auto"), 3);
                }
            }
        }, r -> { }));
        m.put("share_damage", Prims.timed(r -> r.ticks("secs"), r -> r.inst.setFlag("share", r.num("pct"), r.inst.clock() + r.ticks("secs")), r -> { }, r -> r.inst.clearFlag("share")));
        m.put("downed", Prims.timed(r -> Prims.FOREVER_TICKS, r -> r.inst.setFlag("downed", r.num("secs"), FOREVER), r -> { }, r -> r.inst.clearFlag("downed")));
        m.put("no_regen", Prims.timed(r -> r.ticks("secs"), r -> r.inst.setFlag("noregen", 1, r.inst.clock() + r.ticks("secs")), r -> { }, r -> r.inst.clearFlag("noregen")));
    }
}
