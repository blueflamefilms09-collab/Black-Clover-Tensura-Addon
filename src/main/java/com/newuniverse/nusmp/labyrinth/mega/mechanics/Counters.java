package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: HOW a player beats a mechanic. Twenty counter kinds: nothing (it must be survived), kill every warden, kill the keystone, make an offering, hold a plate, sneak, carry a
 * light, use pedestals in order, pull a lever, last out the time, stand together, split onto plates, carry a key, take no damage, keep quiet, pay experience, blood or items,
 * light the pillars in step, repeat a shown pattern. Time-based ones are polled each tick ({@link #poll}); use-based ones are driven by a right click on a prop
 * ({@link #onUse}); kill-based ones by the monsters' deaths ({@link #onMobDeath}). Beating a counter calls {@link Instance#solve}, which pays the reward and lifts the effects.
 * A counter whose props could not be placed is {@code broken}: the mechanic then behaves as if it had no counter.
 */
final class Counters {
    private Counters() {}

    static final class Mem {
        int hold, step;
        int[] order = new int[0];
        long[] litAt = new long[0];
        boolean broken, spawned, shown;
        long showStart = -1, nextShow;
    }

    private static OpCatalog.KindSpec spec(Instance in) {
        OpCatalog cat = MechanicLibrary.catalog();
        return cat == null ? null : cat.counter(in.def.counter().kind());
    }

    static double num(Instance in, String k) {
        OpCatalog.KindSpec ks = spec(in);
        OpCatalog.ArgSpec a = ks == null ? null : ks.arg(k);
        return in.def.counter().args().num(k, a != null && a.def() instanceof Double d ? d : 0.0, in.floor);
    }

    static String str(Instance in, String k) {
        OpCatalog.KindSpec ks = spec(in);
        OpCatalog.ArgSpec a = ks == null ? null : ks.arg(k);
        return in.def.counter().args().str(k, a != null && a.def() instanceof String s ? s : "");
    }

    /** Fixes the order of a sequence / mirror from the instance's seed. */
    static void prepare(Instance in) {
        String k = in.def.counter().kind();
        if (k.equals("sequence") || k.equals("mirror")) {
            int n = Math.max(2, (int) num(in, "steps"));
            int[] o = new int[n];
            for (int i = 0; i < n; i++) o[i] = i;
            for (int i = n - 1; i > 0; i--) {
                int j = in.rng.nextInt(i + 1);
                int t = o[i];
                o[i] = o[j];
                o[j] = t;
            }
            in.ctr.order = o;
        }
        if (k.equals("pillars_sync")) in.ctr.litAt = new long[Math.max(2, (int) num(in, "steps"))];
    }

    private static boolean onPlate(Instance in, ServerPlayer p) {
        for (Prop pr : in.props) {
            if (!pr.kind().equals("plate")) continue;
            if (Math.abs(p.getX() - (pr.pos().getX() + 0.5)) < 0.9 && Math.abs(p.getZ() - (pr.pos().getZ() + 0.5)) < 0.9 && Math.abs(p.getY() - pr.pos().getY()) < 1.6) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- polled counters
    static void poll(Instance in, long now) {
        Mem m = in.ctr;
        String k = in.def.counter().kind();
        if (k.equals("none") || m.broken || in.state != Instance.State.ARMED) return;
        boolean any = !in.inside.isEmpty();
        switch (k) {
            case "stand_plate" -> {
                boolean on = false;
                for (ServerPlayer p : in.inside) on |= onPlate(in, p);
                m.hold = on ? m.hold + 1 : Math.max(0, m.hold - 2);
                if (m.hold >= num(in, "secs") * 20) in.solve(first(in));
            }
            case "sneak_through" -> {
                boolean s = false;
                for (ServerPlayer p : in.inside) s |= p.isShiftKeyDown();
                if (s) m.hold++;
                if (m.hold >= num(in, "secs") * 20) in.solve(first(in));
            }
            case "hold_light" -> {
                boolean h = false;
                for (ServerPlayer p : in.inside) h |= Triggers.holdsLight(p);
                if (h) m.hold++;
                if (m.hold >= num(in, "secs") * 20) in.solve(first(in));
            }
            case "survive" -> {
                if (any) m.hold++;
                if (m.hold >= num(in, "secs") * 20) in.solve(first(in));
            }
            case "all_present" -> {
                List<ServerPlayer> near = new ArrayList<>();
                for (ServerPlayer p : in.level.players()) if (Safety.affects(p) && p.distanceToSqr(in.anchor.getX(), in.anchor.getY(), in.anchor.getZ()) < 48 * 48) near.add(p);
                boolean all = !near.isEmpty();
                for (ServerPlayer p : near) all &= in.area.contains(p) && closeToPlate(in, p);
                m.hold = all ? m.hold + 1 : 0;
                if (m.hold >= num(in, "secs") * 20) in.solve(first(in));
            }
            case "split_plates" -> {
                int n = (int) num(in, "n");
                java.util.Set<Prop> used = new java.util.HashSet<>();
                java.util.Set<java.util.UUID> who = new java.util.HashSet<>();
                for (ServerPlayer p : in.inside) {
                    for (Prop pr : in.props) {
                        if (used.contains(pr) || who.contains(p.getUUID())) continue;
                        if (Math.abs(p.getX() - (pr.pos().getX() + 0.5)) < 0.9 && Math.abs(p.getZ() - (pr.pos().getZ() + 0.5)) < 0.9 && Math.abs(p.getY() - pr.pos().getY()) < 1.6) { used.add(pr); who.add(p.getUUID()); }
                    }
                }
                m.hold = used.size() >= n ? m.hold + 1 : 0;
                if (m.hold >= 40) in.solve(first(in));
            }
            case "no_damage" -> {
                if (!any) return;
                m.hold = now - in.lastDamageAt < 2 ? 0 : m.hold + 1;
                if (m.hold >= num(in, "secs") * 20) in.solve(first(in));
            }
            case "quiet" -> {
                if (!any) return;
                m.hold = now - in.lastNoiseAt < 2 ? 0 : m.hold + 1;
                if (m.hold >= num(in, "secs") * 20) in.solve(first(in));
            }
            case "sequence", "mirror" -> {
                if (!any || in.props.isEmpty()) return;
                if (k.equals("sequence") && !m.shown) { m.shown = true; m.showStart = now; }
                if (k.equals("mirror") && m.showStart < 0 && now >= m.nextShow && m.step == 0) m.showStart = now;
                if (m.showStart >= 0) {
                    long t = now - m.showStart;
                    int idx = (int) (t / 14);
                    if (t % 14 == 0 && idx < m.order.length) flash(in, m.order[idx], true);
                    if (idx >= m.order.length) { m.showStart = -1; m.nextShow = now + 220; }
                }
            }
            case "pillars_sync" -> {
                // a lit pillar goes out after the window; nothing else to poll
                long window = (long) (num(in, "secs") * 20);
                for (int i = 0; i < m.litAt.length; i++) if (m.litAt[i] != 0 && now - m.litAt[i] > window) m.litAt[i] = 0;
            }
            default -> { }
        }
    }

    private static boolean closeToPlate(Instance in, ServerPlayer p) {
        for (Prop pr : in.props) if (pr.kind().equals("plate") && p.distanceToSqr(pr.pos().getX() + 0.5, pr.pos().getY(), pr.pos().getZ() + 0.5) < 25) return true;
        return false;
    }

    private static ServerPlayer first(Instance in) { return in.inside.isEmpty() ? null : in.inside.get(0); }

    /** A flash and a note on the prop with this index (good = Light Rukh and a bright note, else Dark Rukh and a low one). */
    static void flash(Instance in, int index, boolean good) {
        for (Prop pr : in.props) {
            if (pr.index() != index) continue;
            Rukh.at(in, pr.pos(), good ? "light" : "dark", "column", 6);
            in.level.playSound(null, pr.pos(), good ? SoundEvents.NOTE_BLOCK_CHIME.value() : SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0f, good ? 0.7f + 0.15f * index : 0.6f);
        }
    }

    // ---------------------------------------------------------------- use-driven counters
    static void onUse(Instance in, ServerPlayer p, Prop prop) {
        Mem m = in.ctr;
        if (in.state != Instance.State.ARMED || m.broken) return;
        long now = in.clock();
        switch (in.def.counter().kind()) {
            case "lever" -> { if (prop.kind().equals("lever")) in.solve(p); }
            case "item_offer" -> {
                Item want = item(str(in, "item"));
                int n = (int) num(in, "count");
                ItemStack held = p.getMainHandItem();
                if (want != null && held.is(want) && held.getCount() >= n) {
                    if (!p.isCreative()) held.shrink(n);
                    in.solve(p);
                } else p.displayClientMessage(Messages.text("need_item", "The altar wants %s x%s.", Component.translatable(want == null ? "item.minecraft.air" : want.getDescriptionId()), n), true);
            }
            case "pay_item" -> {
                Item want = item(str(in, "item"));
                int n = (int) num(in, "count");
                if (want != null && removeItems(p, want, n)) in.solve(p);
                else p.displayClientMessage(Messages.text("need_item", "The altar wants %s x%s.", Component.translatable(want == null ? "item.minecraft.air" : want.getDescriptionId()), n), true);
            }
            case "pay_xp" -> {
                int n = (int) num(in, "levels");
                if (p.experienceLevel >= n) {
                    p.giveExperienceLevels(-n);
                    in.solve(p);
                } else p.displayClientMessage(Messages.text("need_xp", "The altar wants %s levels of experience.", n), true);
            }
            case "pay_hp" -> {
                float cost = (float) (num(in, "hearts") * 2.0);
                if (p.getHealth() > cost + 1.0f) {
                    p.setHealth(p.getHealth() - cost);
                    p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0f, 0.8f);
                    in.solve(p);
                } else p.displayClientMessage(Messages.text("need_hp", "You are too weak to pay in blood."), true);
            }
            case "sequence", "mirror" -> {
                if (m.order.length == 0) return;
                if (prop.index() == m.order[m.step]) {
                    flash(in, prop.index(), true);
                    m.step++;
                    in.counters.put("wrong", 0);                                    // "wrong" is 1 while the last press was a mistake: effects may be gated on it
                    if (m.step >= m.order.length) in.solve(p);
                } else {
                    m.step = 0;
                    in.counters.put("wrong", 1);
                    in.counters.merge("wrongs", 1, Integer::sum);
                    flash(in, prop.index(), false);
                }
            }
            case "pillars_sync" -> {
                if (prop.index() >= m.litAt.length) return;
                m.litAt[prop.index()] = now;
                flash(in, prop.index(), true);
                boolean all = true;
                for (long t : m.litAt) all &= t != 0;
                if (all) in.solve(p);
            }
            case "carry" -> {
                if (prop.index() == 0 && !hasKey(in, p) && !anyoneHasKey(in)) {
                    p.getInventory().add(keyStack(in));
                    p.displayClientMessage(Messages.text("key_taken", "You take the key. Carry it to the other pedestal."), true);
                } else if (prop.index() == 1 && hasKey(in, p)) {
                    removeKey(in, p);
                    in.solve(p);
                }
            }
            default -> { }
        }
    }

    // ---------------------------------------------------------------- kill counters
    static void onMobDeath(Instance in, LivingEntity victim, boolean mine) {
        if (in.state != Instance.State.ARMED || in.ctr.broken) return;
        String k = in.def.counter().kind();
        if (k.equals("kill_all") && mine && in.mobs.isEmpty() && in.ctr.spawned) in.solve(first(in));
        else if (k.equals("kill_marked") && victim.getUUID().equals(in.leader)) in.solve(first(in));
    }

    // ---------------------------------------------------------------- items
    static Item item(String id) {
        try {
            return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    static boolean removeItems(ServerPlayer p, Item item, int n) {
        int have = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(item)) have += s.getCount();
        if (have < n) return false;
        if (p.isCreative()) return true;
        int left = n;
        for (ItemStack s : p.getInventory().items) {
            if (left <= 0) break;
            if (!s.is(item)) continue;
            int take = Math.min(left, s.getCount());
            s.shrink(take);
            left -= take;
        }
        return true;
    }

    static ItemStack keyStack(Instance in) {
        Item it = item(str(in, "item"));
        ItemStack s = new ItemStack(it == null ? net.minecraft.world.item.Items.AMETHYST_SHARD : it);
        CompoundTag t = new CompoundTag();
        t.putString("nusmp_mech_key", in.key);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        s.set(DataComponents.CUSTOM_NAME, Messages.text("key", "Key of the mechanism"));
        return s;
    }

    private static boolean isKey(Instance in, ItemStack s) {
        if (s.isEmpty()) return false;
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d != null && in.key.equals(d.copyTag().getString("nusmp_mech_key"));
    }

    static boolean hasKey(Instance in, ServerPlayer p) {
        for (ItemStack s : p.getInventory().items) if (isKey(in, s)) return true;
        return false;
    }

    private static boolean anyoneHasKey(Instance in) {
        for (ServerPlayer p : in.level.players()) if (hasKey(in, p)) return true;
        return false;
    }

    private static void removeKey(Instance in, ServerPlayer p) {
        for (ItemStack s : p.getInventory().items) if (isKey(in, s)) { s.shrink(s.getCount()); return; }
    }
}
