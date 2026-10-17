package com.newuniverse.nusmp.avatar.net;

import com.newuniverse.nusmp.avatar.aptitude.ForgeContext;
import com.newuniverse.nusmp.avatar.aptitude.ForgeRules;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: everything the Soul Forge screen needs that the SERVER knows and the client cannot: the balance rules in force (how heavy the taxes are, how many
 * aptitudes fit, the awakening's price), the creator's own reserves read just now (luck, magicule, maximum magicule, EP, the tax they already carry, their
 * Genesis Cores), how many free rolls are left and the aptitude already rolled, and the two catalogs themselves (the aptitude rows and the skill rows the
 * game can really offer). With these the screen draws the grids and tooltips and runs the SAME pure cost maths as the server to light the Finalize button;
 * the server's own check on Finalize is still the only one that counts. Sent when the screen opens and whenever the server's numbers change.
 */
public record S2C_SoulForgeStatePayload(ForgeRules rules, ForgeContext reserves, int rollsLeft, List<ForgeAptitudeRow> aptitudes, List<ForgeSkillRow> skills)
        implements CustomPacketPayload {
    public static final Type<S2C_SoulForgeStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "forge_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_SoulForgeStatePayload> CODEC = StreamCodec.of(S2C_SoulForgeStatePayload::write, S2C_SoulForgeStatePayload::read);

    private static void write(RegistryFriendlyByteBuf b, S2C_SoulForgeStatePayload p) {
        ForgeRules r = p.rules;
        b.writeVarInt(r.maxAptitudes());
        b.writeDouble(r.aptitudeTaxScale()); b.writeDouble(r.skillTaxScale()); b.writeDouble(r.maxTotalTax()); b.writeDouble(r.upfrontFactor());
        b.writeDouble(r.awakeningBase()); b.writeDouble(r.awakeningMaxChance()); b.writeDouble(r.awakeningTax());
        b.writeVarInt(r.awakeningExtraCores()); b.writeVarInt(r.rollsMax()); b.writeDouble(r.luckStrength()); b.writeBoolean(r.chosenSkillsSecure());
        ForgeContext c = p.reserves;
        b.writeDouble(c.luck()); b.writeDouble(c.magicule()); b.writeDouble(c.maxMagicule()); b.writeDouble(c.maxEp()); b.writeDouble(c.existingTax());
        b.writeVarInt(c.cores()); b.writeBoolean(c.creative());
        b.writeUtf(NetText.clean(c.rolledAptitude(), NetText.ID_MAX), NetText.ID_MAX);
        b.writeLong(c.rolledToken());
        b.writeVarInt(p.rollsLeft);
        int na = Math.min(p.aptitudes.size(), ForgeAptitudeRow.MAX_ROWS);
        b.writeVarInt(na);
        for (int i = 0; i < na; i++) p.aptitudes.get(i).write(b);
        int ns = Math.min(p.skills.size(), ForgeSkillRow.MAX_ROWS);
        b.writeVarInt(ns);
        for (int i = 0; i < ns; i++) p.skills.get(i).write(b);
    }

    private static S2C_SoulForgeStatePayload read(RegistryFriendlyByteBuf b) {
        ForgeRules rules = new ForgeRules(b.readVarInt(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(),
                b.readVarInt(), b.readVarInt(), b.readDouble(), b.readBoolean());
        double luck = b.readDouble(), magicule = b.readDouble(), maxMagicule = b.readDouble(), maxEp = b.readDouble(), tax = b.readDouble();
        int cores = b.readVarInt();
        boolean creative = b.readBoolean();
        String rolled = b.readUtf(NetText.ID_MAX);
        long token = b.readLong();
        int rollsLeft = b.readVarInt();
        int na = Math.min(b.readVarInt(), ForgeAptitudeRow.MAX_ROWS);
        List<ForgeAptitudeRow> apt = new ArrayList<>(na);
        for (int i = 0; i < na; i++) apt.add(ForgeAptitudeRow.read(b));
        int ns = Math.min(b.readVarInt(), ForgeSkillRow.MAX_ROWS);
        List<ForgeSkillRow> skl = new ArrayList<>(ns);
        for (int i = 0; i < ns; i++) skl.add(ForgeSkillRow.read(b));
        return new S2C_SoulForgeStatePayload(rules, new ForgeContext(luck, magicule, maxMagicule, maxEp, tax, cores, creative, rolled, token, 0.0), rollsLeft, apt, skl);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
