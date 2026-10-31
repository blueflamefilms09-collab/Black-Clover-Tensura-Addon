package com.newuniverse.nusmp.beastwill;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Map;
import java.util.Optional;

/**
 * Why: where the Beast Will progress of a player is saved (3.45.0): a NeoForge attachment, written with the player, copied to the new body on death, never synced by NeoForge (the client gets
 * {@link BeastWillNet.Sync}). Reading never CREATES the attachment for a player who has no will ({@link #peek}), so the system costs nothing for everyone else. The tag format is hand-written and clamped
 * on read, so a damaged or edited save can not give a value outside the rules. Register {@link #ATTACHMENTS} on the mod bus.
 */
public final class BeastWillStore {
    private BeastWillStore() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);

    public static final Codec<BeastWillState> CODEC = CompoundTag.CODEC.xmap(BeastWillStore::fromTag, BeastWillStore::toTag);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<BeastWillState>> STATE = ATTACHMENTS.register("beast_will",
            () -> AttachmentType.builder(BeastWillState::new).serialize(CODEC, s -> !s.isEmpty()).copyOnDeath().build());

    /** The state of the player, created empty if there is none (use for writing). */
    public static BeastWillState of(Player p) { return p.getData(STATE); }

    /** The state if the player has one, else null (use for reading: it never creates an attachment). */
    public static BeastWillState peek(Player p) {
        if (!p.hasData(STATE)) return null;
        Optional<BeastWillState> s = Optional.of(p.getData(STATE));
        return s.get();
    }

    // ---------------------------------------------------------------- tag format
    public static CompoundTag toTag(BeastWillState s) {
        CompoundTag t = new CompoundTag();
        t.putInt("v", 1);
        t.putString("active", s.active);
        t.putInt("phase", s.phase);
        t.putFloat("strain", s.strain);
        t.putLong("lock", s.lockUntil);
        t.putLong("swap", s.swapLockUntil);
        ListTag list = new ListTag();
        for (Map.Entry<String, BeastWillState.Progress> e : s.wills.entrySet()) {
            CompoundTag w = new CompoundTag();
            BeastWillState.Progress p = e.getValue();
            w.putString("id", e.getKey());
            w.putFloat("a", p.assimilation);
            w.putFloat("m", p.mastery);
            w.putLong("day", p.day);
            w.putFloat("da", p.dayAssimilation);
            w.putFloat("dm", p.dayMastery);
            CompoundTag cd = new CompoundTag();
            for (Map.Entry<String, Long> c : p.readyAt.entrySet()) cd.putLong(c.getKey(), c.getValue());
            w.put("cd", cd);
            list.add(w);
        }
        t.put("wills", list);
        CompoundTag cores = new CompoundTag();
        for (Map.Entry<String, Long> e : s.coreDay.entrySet()) cores.putLong(e.getKey(), e.getValue());
        t.put("cores", cores);
        return t;
    }

    public static BeastWillState fromTag(CompoundTag t) {
        BeastWillState s = new BeastWillState();
        s.active = t.getString("active");
        s.phase = (int) BeastWillRules.clamp(t.getInt("phase"), 0, 2);
        s.strain = BeastWillRules.clampStrain(t.getFloat("strain"));
        s.lockUntil = Math.max(0, t.getLong("lock"));
        s.swapLockUntil = Math.max(0, t.getLong("swap"));
        ListTag list = t.getList("wills", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && s.wills.size() < BeastWillState.MAX_WILLS; i++) {
            CompoundTag w = list.getCompound(i);
            String id = w.getString("id");
            if (id.isEmpty() || id.length() > 40) continue;
            BeastWillState.Progress p = new BeastWillState.Progress();
            p.assimilation = (float) BeastWillRules.clamp(w.getFloat("a"), 0, 1);
            p.mastery = (float) BeastWillRules.clamp(w.getFloat("m"), 0, 1);
            p.day = w.contains("day") ? w.getLong("day") : -1;
            p.dayAssimilation = (float) BeastWillRules.clamp(w.getFloat("da"), 0, 1);
            p.dayMastery = (float) BeastWillRules.clamp(w.getFloat("dm"), 0, 1);
            CompoundTag cd = w.getCompound("cd");
            for (String k : cd.getAllKeys()) if (p.readyAt.size() < 24) p.readyAt.put(k, Math.max(0, cd.getLong(k)));
            s.wills.put(id, p);
        }
        CompoundTag cores = t.getCompound("cores");
        for (String k : cores.getAllKeys()) if (s.coreDay.size() < 32) s.coreDay.put(k, cores.getLong(k));
        if (!s.wills.containsKey(s.active)) {
            s.active = "";
            s.phase = BeastWillRules.OFF;
        }
        return s;
    }
}
