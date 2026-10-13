package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;
import com.newuniverse.nusmp.relics.Relics;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Amulet of Safeguarding. Once a month (one full-moon night in eight days) the wearer meditates over it for thirty seconds without moving or
 * being hurt, and it records them: body, hunger, experience, their Tensura existence (magicules and aura) and what they carry. If a lethal
 * blow comes, the amulet breaks in a flash and the wearer is restored to the record, back where they sat, with every effect gone. Two changes from the tabletop card so nothing is ever
 * duplicated or destroyed: items they gained since the record are left where they fell (the record never creates an item), and the amulet
 * does not vanish: it cracks and mends after two more full moons.
 */
final class Wave5Safeguard {
    private Wave5Safeguard() {}

    static final String ID = "amulet_of_safeguarding";
    static final String FLAG = "w5_safeguard";
    private static final int COLOUR = 0xFF7FCB;

    // ---------------------------------------------------------------- taking the record
    static void meditate(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack s = Wave5Data.live(p, ID, c.stack());
        if (s.isEmpty()) return;
        CompoundTag st = Wave5Data.stack(s);
        long dayTime = p.serverLevel().getServer().overworld().getDayTime();
        if (st.getLong("med_start") > 0L) {
            Wave5Aim.msg(p, "You are already meditating. Stay still.", ChatFormatting.GRAY);
            return;
        }
        if (!Wave5Pure.fullMoonNight(dayTime)) {
            Wave5Aim.msg(p, "The amulet can only take its record under the full moon.", ChatFormatting.GRAY);
            return;
        }
        long last = st.contains("last_cycle") ? st.getLong("last_cycle") : -1L;
        if (!Wave5Pure.canRecord(dayTime, last, st.getLong("mend"))) {
            Wave5Aim.msg(p, Wave5Pure.cycle(dayTime) <= last ? "It has already recorded you this month." : "The amulet is still mending from its last breaking.", ChatFormatting.GRAY);
            return;
        }
        st.putLong("med_start", p.level().getGameTime());
        st.putDouble("mx", p.getX());
        st.putDouble("my", p.getY());
        st.putDouble("mz", p.getZ());
        st.putFloat("mhp", p.getHealth());
        st.putString("mdim", p.level().dimension().location().toString());
        Wave5Data.save(s, st);
        Fx.ring(p.serverLevel(), p.position().add(0, 0.1, 0), 1.4, COLOUR);
        Wave5Aim.msg(p, "You sink into stillness. Stay where you are for 30 seconds and let nothing touch you.", ChatFormatting.LIGHT_PURPLE);
    }

    /** Once a second while worn: keeps the meditation honest and finishes it. */
    static void tick(ServerPlayer p) {
        if (!RelicFlags.has(p, FLAG)) return;
        ItemStack s = Wave5Data.live(p, ID);
        if (s.isEmpty()) return;
        CompoundTag st = Wave5Data.stack(s);
        long start = st.getLong("med_start");
        if (start <= 0L) return;
        long now = p.level().getGameTime();
        long dayTime = p.serverLevel().getServer().overworld().getDayTime();
        String why = null;
        if (!p.level().dimension().location().toString().equals(st.getString("mdim"))) why = "you left the place";
        else if (!Wave5Pure.heldStill(p.getX() - st.getDouble("mx"), p.getY() - st.getDouble("my"), p.getZ() - st.getDouble("mz"))) why = "you moved";
        else if (p.getHealth() < st.getFloat("mhp") - 0.01f) why = "you were hurt";
        else if (!Wave5Pure.fullMoonNight(dayTime)) why = "the night ended";
        else if (now - start > Wave5Pure.MEDITATE_TICKS + 100L) why = "you were away";
        if (why != null) {
            st.remove("med_start");
            Wave5Data.save(s, st);
            Wave5Aim.msg(p, "The meditation is broken: " + why + ".", ChatFormatting.RED);
            return;
        }
        long elapsed = now - start;
        if (elapsed >= Wave5Pure.MEDITATE_TICKS) {
            record(p, st);
            st.remove("med_start");
            Wave5Data.save(s, st);
            return;
        }
        ServerLevel l = p.serverLevel();
        Wave5Aim.puff(l, p.position().add(0, 1.0, 0), ParticleTypes.ENCHANT, 12, 0.7);
        Wave5Aim.msg(p, "Meditating... " + (Wave5Pure.MEDITATE_TICKS - elapsed) / 20 + " s", ChatFormatting.LIGHT_PURPLE);
    }

    private static void record(ServerPlayer p, CompoundTag st) {
        CompoundTag rec = new CompoundTag();
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM.getKey(it.getItem()).toString();
            rec.putInt(id, rec.getInt(id) + it.getCount());
        }
        st.put("rec", rec);
        st.putBoolean("has", true);
        st.putFloat("hp", p.getHealth());
        st.putInt("food", p.getFoodData().getFoodLevel());
        st.putFloat("sat", p.getFoodData().getSaturationLevel());
        st.putInt("xp", p.totalExperience);
        var ex = TensuraStorages.getExistenceFrom(p);                // the record holds the wearer's existence too: magicules and aura as they are now
        if (ex != null) {
            st.putDouble("mp", ex.getMagicule());
            st.putDouble("ap", ex.getAura());
        }
        st.putFloat("yaw", p.getYRot());
        st.putLong("last_cycle", Wave5Pure.cycle(p.serverLevel().getServer().overworld().getDayTime()));
        ServerLevel l = p.serverLevel();
        Fx.burst(l, p.position().add(0, 1.0, 0), COLOUR);
        Wave5Aim.sound(l, p.position(), SoundEvents.BEACON_ACTIVATE, 0.9f, 1.4f);
        Wave5Aim.msg(p, "The amulet holds a complete record of you: body, belongings, experience and memory.", ChatFormatting.LIGHT_PURPLE);
    }

    // ---------------------------------------------------------------- the lethal blow
    /** A blow that would kill the wearer: the record takes its place. True when it did. */
    static boolean restore(LivingDamageEvent.Pre e, ServerPlayer p) {
        if (!RelicFlags.has(p, FLAG)) return false;
        DamageSource src = e.getSource();
        if (src.is(DamageTypes.GENERIC_KILL)) return false;
        ItemStack s = Wave5Data.live(p, ID);
        if (s.isEmpty()) return false;
        CompoundTag st = Wave5Data.stack(s);
        if (!st.getBoolean("has")) return false;
        e.setNewDamage(0f);

        // where they sat: the record's own place (same or another dimension); if there is no room to stand there they are restored where they are
        ServerLevel from = p.serverLevel();
        ServerLevel to = from;
        try {
            ServerLevel rec2 = p.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(st.getString("mdim"))));
            if (rec2 != null) to = rec2;
        } catch (RuntimeException badId) {
            // an unreadable dimension id: they are restored where they stand
        }
        Vec3 spot = new Vec3(st.getDouble("mx"), st.getDouble("my"), st.getDouble("mz"));
        boolean moves = clear(to, spot);

        // what they carried that the record never held stays behind, where they fell (only when they really do go back to the place of the record)
        if (moves) {
            Inventory inv = p.getInventory();
            int n = inv.getContainerSize();
            String[] ids = new String[n];
            int[] counts = new int[n];
            boolean[] exempt = new boolean[n];
            for (int i = 0; i < n; i++) {
                ItemStack it = inv.getItem(i);
                if (it.isEmpty()) continue;
                ids[i] = BuiltInRegistries.ITEM.getKey(it.getItem()).toString();
                counts[i] = it.getCount();
                exempt[i] = Relics.of(it).isPresent();               // a relic is never taken from its bearer
            }
            CompoundTag rec = st.getCompound("rec");
            Map<String, Integer> recorded = new HashMap<>();
            for (String k : rec.getAllKeys()) recorded.put(k, rec.getInt(k));
            int[] stay = Wave5Pure.excess(ids, counts, exempt, recorded);
            for (int i = 0; i < n; i++) {
                if (stay[i] <= 0) continue;
                ItemStack it = inv.getItem(i);
                ItemStack left = it.copyWithCount(stay[i]);
                it.shrink(stay[i]);
                ItemEntity drop = new ItemEntity(from, p.getX(), p.getY() + 0.5, p.getZ(), left);
                drop.setDefaultPickUpDelay();
                from.addFreshEntity(drop);
            }
        }

        // the body: health, hunger, experience, every magical effect gone
        p.removeAllEffects();
        p.setRemainingFireTicks(0);
        p.fallDistance = 0f;
        p.setHealth(Math.min(p.getMaxHealth(), Math.max(st.getFloat("hp"), p.getMaxHealth() * 0.5f)));
        p.getFoodData().setFoodLevel(Math.max(p.getFoodData().getFoodLevel(), st.getInt("food")));
        p.getFoodData().setSaturation(Math.max(p.getFoodData().getSaturationLevel(), st.getFloat("sat")));
        int xp = st.getInt("xp");
        if (xp > p.totalExperience) p.giveExperiencePoints(xp - p.totalExperience);
        var ex = TensuraStorages.getExistenceFrom(p);                // ...and so do the magicules and the aura
        if (ex != null && st.contains("mp")) {
            ex.setMagicule(Math.max(ex.getMagicule(), st.getDouble("mp")));
            ex.setAura(Math.max(ex.getAura(), st.getDouble("ap")));
            ex.markDirty();
        }

        Fx.burst(from, p.position().add(0, 1.0, 0), COLOUR);
        if (moves) {                                                 // the move itself waits for the end of this tick (never teleport in the middle of a hit)
            CompoundTag d = Wave5Data.player(p);
            d.putString("tp_dim", to.dimension().location().toString());
            d.putDouble("tp_x", spot.x);
            d.putDouble("tp_y", spot.y);
            d.putDouble("tp_z", spot.z);
            d.putFloat("tp_yaw", st.getFloat("yaw"));
        }
        Fx.ring(from, p.position().add(0, 0.2, 0), 2.5, COLOUR);
        Wave5Aim.sound(from, p.position(), SoundEvents.TOTEM_USE, 1.0f, 1.3f);

        // the amulet cracks and needs two more full moons to mend; the record is spent
        st.putBoolean("has", false);
        st.remove("rec");
        st.putLong("mend", Wave5Pure.mendsAt(p.serverLevel().getServer().overworld().getDayTime()));
        Wave5Data.save(s, st);
        Wave5Aim.msg(p, "The amulet breaks in a flash of light, and you are as you were. It cracks: it will mend after two full moons.", ChatFormatting.LIGHT_PURPLE);
        return true;
    }

    /** The end of the tick after a rescue: the wearer goes back to where they sat. */
    static void applyMove(ServerPlayer p) {
        if (!Wave5Data.hasPlayerData(p)) return;
        CompoundTag d = Wave5Data.player(p);
        if (!d.contains("tp_dim", 8)) return;
        String dim = d.getString("tp_dim");
        Vec3 spot = new Vec3(d.getDouble("tp_x"), d.getDouble("tp_y"), d.getDouble("tp_z"));
        float yaw = d.getFloat("tp_yaw");
        for (String k : new String[]{"tp_dim", "tp_x", "tp_y", "tp_z", "tp_yaw"}) d.remove(k);
        if (!p.isAlive()) return;
        ServerLevel to = null;
        try {
            to = p.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dim)));
        } catch (RuntimeException badId) {
            // an unreadable dimension id: they stay where they are
        }
        if (to == null || !clear(to, spot)) return;
        p.teleportTo(to, spot.x, spot.y, spot.z, yaw, p.getXRot());
        p.setDeltaMovement(Vec3.ZERO);
        p.hurtMarked = true;
        Fx.burst(p.serverLevel(), p.position().add(0, 1.0, 0), COLOUR);
        Fx.ring(p.serverLevel(), p.position().add(0, 0.2, 0), 2.5, COLOUR);
    }

    /** Is there room for a player to stand at this point? */
    private static boolean clear(ServerLevel l, Vec3 at) {
        BlockPos feet = BlockPos.containing(at.x, at.y, at.z);
        BlockState a = l.getBlockState(feet);
        BlockState b = l.getBlockState(feet.above());
        return (a.isAir() || a.getCollisionShape(l, feet).isEmpty()) && (b.isAir() || b.getCollisionShape(l, feet.above()).isEmpty());
    }
}
