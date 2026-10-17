package com.newuniverse.nusmp.avatar.net;

import com.newuniverse.nusmp.avatar.aptitude.Aptitude;
import com.newuniverse.nusmp.avatar.aptitude.AptitudeLook;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: one Magic Aptitude as it crosses the network to the Soul Forge screen: the data the server loaded (name, rarity, the tax percent as WRITTEN in the
 * data, the magic family, the Tensura skills it lends) and how it looks (tint, icon, one line of flavour). The data files live on the server only, so the
 * screen draws its grid, its tooltips and its live cost panel from these rows; the tax here is the raw value and the config scale travels once in the state
 * packet, so the client's quote and the server's are the same arithmetic ({@code ForgeCosts}). Plain values only, every string cut to a safe length on both sides.
 */
public record ForgeAptitudeRow(String id, String name, int rarity, float taxPercent, String magic, List<String> skills, int tint, String icon, String description) {
    public static final int MAX_ROWS = 64;

    public static ForgeAptitudeRow of(Aptitude a, AptitudeLook look) {
        return new ForgeAptitudeRow(a.id(), a.name(), a.rarity().ordinal(), (float) a.taxPercent(), a.magic(), a.skills(), look.tint(), look.icon(), look.description());
    }

    /** The contract record for the client's pure maths (the rarity ordinal is clamped into the enum). */
    public Aptitude toAptitude() {
        Aptitude.Rarity[] all = Aptitude.Rarity.values();
        return new Aptitude(id, name, all[Math.max(0, Math.min(all.length - 1, rarity))], taxPercent, magic, skills);
    }

    public void write(FriendlyByteBuf b) {
        b.writeUtf(NetText.clean(id, NetText.ID_MAX), NetText.ID_MAX);
        b.writeUtf(NetText.clean(name, NetText.NAME_MAX), NetText.NAME_MAX);
        b.writeVarInt(rarity);
        b.writeFloat(taxPercent);
        b.writeUtf(NetText.clean(magic, NetText.ID_MAX), NetText.ID_MAX);
        int n = Math.min(skills.size(), 8);
        b.writeVarInt(n);
        for (int i = 0; i < n; i++) b.writeUtf(NetText.clean(skills.get(i), NetText.ID_MAX), NetText.ID_MAX);
        b.writeInt(tint);
        b.writeUtf(NetText.clean(icon, 128), 128);
        b.writeUtf(NetText.clean(description, 200), 200);
    }

    public static ForgeAptitudeRow read(FriendlyByteBuf b) {
        String id = b.readUtf(NetText.ID_MAX), name = b.readUtf(NetText.NAME_MAX);
        int rarity = b.readVarInt();
        float tax = b.readFloat();
        String magic = b.readUtf(NetText.ID_MAX);
        int n = Math.min(b.readVarInt(), 8);
        List<String> skills = new ArrayList<>(n);
        for (int i = 0; i < n; i++) skills.add(b.readUtf(NetText.ID_MAX));
        int tint = b.readInt();
        String icon = b.readUtf(128), description = b.readUtf(200);
        return new ForgeAptitudeRow(id, name, rarity, tax, magic, skills, tint, icon, description);
    }
}
