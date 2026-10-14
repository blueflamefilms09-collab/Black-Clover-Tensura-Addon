package com.newuniverse.nusmp.boss.refill;

import net.minecraft.nbt.CompoundTag;

/** Save-data glue of {@link PhaseRefill}: one compound under one key, so a boss adds two lines (write / read) and nothing else. */
public final class RefillTag {
    private RefillTag() {}

    public static final String KEY = "NuRefill";

    public static void write(PhaseRefill r, CompoundTag tag) {
        PhaseRefill.Saved s = r.save();
        CompoundTag c = new CompoundTag();
        c.putString("done", s.doneCsv());
        c.putString("mode", s.activeMode());
        c.putInt("total", s.total());
        c.putInt("left", s.left());
        c.putFloat("from", s.from());
        c.putFloat("to", s.to());
        tag.put(KEY, c);
    }

    public static void read(PhaseRefill r, CompoundTag tag) {
        if (!tag.contains(KEY)) { r.reset(); return; }
        CompoundTag c = tag.getCompound(KEY);
        r.load(new PhaseRefill.Saved(c.getString("done"), c.getString("mode"), c.getInt("total"), c.getInt("left"), c.getFloat("from"), c.getFloat("to")));
    }
}
