package com.newuniverse.nusmp.avatar.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the creation screen lists every race, greys out the ones the creator cannot use yet, and says why. This is the one row of that list as it
 * crosses the network: plain values only (no registry objects), cut to safe lengths on both sides, so the screen never needs the server's race
 * classes. {@code failed} holds the connection conditions the player does not meet (empty when available).
 */
public record RaceEntry(String id, String name, float scale, float minScale, float maxScale, double ep, double magicule, String evolvesTo,
                        boolean requiresDnd, boolean available, List<String> failed, List<String> statMults) {
    public static final int MAX_FAILED = 12, MAX_RACES = 64;

    public static final StreamCodec<ByteBuf, RaceEntry> CODEC = StreamCodec.of(RaceEntry::write, RaceEntry::read);

    private static void write(ByteBuf b, RaceEntry r) {
        ByteBufCodecs.STRING_UTF8.encode(b, NetText.clean(r.id, NetText.ID_MAX));
        ByteBufCodecs.STRING_UTF8.encode(b, NetText.clean(r.name, NetText.NAME_MAX));
        b.writeFloat(r.scale); b.writeFloat(r.minScale); b.writeFloat(r.maxScale);
        b.writeDouble(r.ep); b.writeDouble(r.magicule);
        ByteBufCodecs.STRING_UTF8.encode(b, NetText.clean(r.evolvesTo, NetText.ID_MAX));
        b.writeBoolean(r.requiresDnd); b.writeBoolean(r.available);
        writeList(b, r.failed); writeList(b, r.statMults);
    }

    private static RaceEntry read(ByteBuf b) {
        String id = NetText.clean(ByteBufCodecs.STRING_UTF8.decode(b), NetText.ID_MAX), name = NetText.clean(ByteBufCodecs.STRING_UTF8.decode(b), NetText.NAME_MAX);
        float s = b.readFloat(), mn = b.readFloat(), mx = b.readFloat();
        double ep = b.readDouble(), mag = b.readDouble();
        String ev = NetText.clean(ByteBufCodecs.STRING_UTF8.decode(b), NetText.ID_MAX);
        boolean dnd = b.readBoolean(), av = b.readBoolean();
        return new RaceEntry(id, name, s, mn, mx, ep, mag, ev, dnd, av, readList(b), readList(b));
    }

    private static void writeList(ByteBuf b, List<String> l) {
        int n = Math.min(l.size(), MAX_FAILED);
        ByteBufCodecs.VAR_INT.encode(b, n);
        for (int i = 0; i < n; i++) ByteBufCodecs.STRING_UTF8.encode(b, NetText.clean(l.get(i), NetText.NAME_MAX));
    }

    private static List<String> readList(ByteBuf b) {
        int n = Math.min(ByteBufCodecs.VAR_INT.decode(b), MAX_FAILED);
        List<String> l = new ArrayList<>(n);
        for (int i = 0; i < n; i++) l.add(NetText.clean(ByteBufCodecs.STRING_UTF8.decode(b), NetText.NAME_MAX));
        return l;
    }
}
