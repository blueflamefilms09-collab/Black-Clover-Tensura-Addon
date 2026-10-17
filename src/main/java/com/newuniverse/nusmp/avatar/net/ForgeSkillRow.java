package com.newuniverse.nusmp.avatar.net;

import com.newuniverse.nusmp.avatar.aptitude.ForgeSkill;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Why: one skill of page 4 as it crosses the network: the id (the screen finds the skill's own name and icon in the game's skill registry, which the client
 * has), the kind the SERVER decided (a Unique or an Ultimate; see {@code ForgeSkillResolver}), the EP tax percent and the EP gate from the data, the tint the
 * preview takes and a line of flavour. A skill the server cannot offer is never sent, so the grid cannot show something the validator would refuse.
 */
public record ForgeSkillRow(String id, boolean ultimate, String name, float epTaxPercent, float minEp, int tint, String icon, String description) {
    public static final int MAX_ROWS = 96;

    public static ForgeSkillRow of(ForgeSkill s) {
        return new ForgeSkillRow(s.id(), s.kind() == ForgeSkill.Kind.ULTIMATE, s.name(), (float) s.epTaxPercent(), (float) s.minEp(), s.tint(), s.icon(), s.description());
    }

    /** The record the client's pure maths reads. */
    public ForgeSkill toSkill() {
        return new ForgeSkill(id, ultimate ? ForgeSkill.Kind.ULTIMATE : ForgeSkill.Kind.UNIQUE, name, epTaxPercent, minEp, tint, icon, description);
    }

    public void write(FriendlyByteBuf b) {
        b.writeUtf(NetText.clean(id, NetText.ID_MAX), NetText.ID_MAX);
        b.writeBoolean(ultimate);
        b.writeUtf(NetText.clean(name, NetText.NAME_MAX), NetText.NAME_MAX);
        b.writeFloat(epTaxPercent);
        b.writeFloat(minEp);
        b.writeInt(tint);
        b.writeUtf(NetText.clean(icon, 128), 128);
        b.writeUtf(NetText.clean(description, 200), 200);
    }

    public static ForgeSkillRow read(FriendlyByteBuf b) {
        return new ForgeSkillRow(b.readUtf(NetText.ID_MAX), b.readBoolean(), b.readUtf(NetText.NAME_MAX), b.readFloat(), b.readFloat(), b.readInt(), b.readUtf(128), b.readUtf(200));
    }
}
