package com.newuniverse.nusmp.spiritcontrol;

import com.newuniverse.nusmp.book.SpiritBond;
import com.newuniverse.nusmp.book.SpiritSlots;
import com.newuniverse.nusmp.entity.ElementalSpiritLordEntity;
import com.newuniverse.nusmp.entity.SpiritLordEntity;
import com.newuniverse.nusmp.multiverse.SpiritLordSkill;
import com.newuniverse.nusmp.relics.Severance;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Why: the old generic Spirit Lord on the control plane: the small floating orb of the Spirit Lord skill and the true body a Spirit Charm gives (docs/spirit_lords.md). It is called the way the skill's
 * "Call Spirit" mode and the charm already call it, so the rules are the same: the spirit must answer (trust, no Severance), it needs a body (the Incarnation) except the Anti-Magic lord, and a mage who holds the
 * element's spirit slot gets the true body, any other the orb. Release sends it back. While combat mode is on, the Lord's own free casts give way to {@link SpiritCombat} (so its spells cost mana, its own and
 * the owner's); with combat mode off it casts for nothing as it always did. It belongs to the Fire, Water, Wind and Earth books by the type of its bond (and Anti-Magic for the Anti-Magic lord).
 */
public final class LordSpirit extends AbstractSpirit {
    /** The persistent-data keys the entities themselves use for "my orb" and "my body". */
    private static final String ORB_KEY = "nusmp_spirit_lord", BODY_KEY = "nusmp_spirit_lord_body";

    public LordSpirit() { super(SpiritCatalog.LORD); }

    @Override
    protected boolean packageAllows(ServerPlayer p) { return SpiritLordSkill.instance(p).isPresent() || SpiritBond.bonded(p); }

    /** The bond type ("Salamander", "Undine", "Sylph", "Gnome", "Anti") or "-". */
    private static String type(ServerPlayer p) { return SpiritLordSkill.bondedType(p); }

    @Override
    public boolean eligibleFor(ServerPlayer p, String magic) {
        return magic != null && eligible(p) && magic.equals(SpiritCatalog.lordMagic(type(p)));
    }

    @Override public List<SpiritSpellDef> spellsFor(ServerPlayer p) { return SpiritCatalog.lordSpells(type(p)); }

    private static Entity find(ServerPlayer p, String key) {
        CompoundTag d = p.getPersistentData();
        if (!d.hasUUID(key)) return null;
        UUID id = d.getUUID(key);
        MinecraftServer server = p.getServer();
        if (server == null) return null;
        for (ServerLevel l : server.getAllLevels()) {
            Entity e = l.getEntity(id);
            if (e != null && e.isAlive()) return e;
        }
        return null;
    }

    private static Entity body(ServerPlayer p) {
        Entity b = find(p, BODY_KEY);
        return b != null ? b : find(p, ORB_KEY);
    }

    @Override public boolean isOut(ServerPlayer p) { return body(p) != null; }

    private static void say(ServerPlayer p, String key, String text, ChatFormatting color, Object... args) {
        p.displayClientMessage(Component.translatableWithFallback(key, text, args).withStyle(color), true);
    }

    @Override
    public boolean summon(ServerPlayer p) {
        if (isOut(p)) return true;
        if (Severance.isSevered(p)) { say(p, "nusmp.spirit_control.severed", "Your spirit is severed from you for a moment.", ChatFormatting.DARK_PURPLE); return false; }
        if (!SpiritBond.willAnswer(p)) { say(p, "nusmp.spirit_control.wont", "Your spirit will not answer you.", ChatFormatting.RED); return false; }
        String type = type(p);
        if ("-".equals(type)) return false;
        boolean anti = "Anti".equalsIgnoreCase(type);
        if (!anti && !SpiritBond.incarnate(p)) {
            say(p, "nusmp.spirit_control.nobody", "%s has no body yet. Earn its full trust and give it a True Name (Spirit Charm).", ChatFormatting.GRAY, type);
            return false;
        }
        ElementalSpiritLordEntity.Kind bodyKind = anti ? null : ElementalSpiritLordEntity.Kind.forSpirit(type);
        if (bodyKind != null && p.getUUID().equals(SpiritSlots.get(p.getServer()).owner(bodyKind.slot))) return ElementalSpiritLordEntity.toggle(p, bodyKind, false);
        SpiritLordEntity.toggle(p, anti ? SpiritLordEntity.Kind.BLACK_DEVIL : SpiritLordEntity.Kind.forSpirit(type));
        return isOut(p);
    }

    @Override
    public void release(ServerPlayer p) {
        String type = type(p);
        boolean anti = "Anti".equalsIgnoreCase(type);
        if (find(p, BODY_KEY) != null) {
            ElementalSpiritLordEntity.Kind k = ElementalSpiritLordEntity.Kind.forSpirit(type);
            if (k != null) ElementalSpiritLordEntity.toggle(p, k, false);
        }
        if (find(p, ORB_KEY) != null) SpiritLordEntity.toggle(p, anti ? SpiritLordEntity.Kind.BLACK_DEVIL : SpiritLordEntity.Kind.forSpirit(type));
    }

    @Override public boolean ability(ServerPlayer p, String abilityId, int arg) { return false; }

    @Override
    public Vec3 origin(ServerPlayer p) {
        Entity e = body(p);
        return e != null ? at(e) : shoulder(p);
    }
}
