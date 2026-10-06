package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.blackclover.GrimoireCover;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.Kingdom;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * A grimoire page: a spell written into the player's grimoire. Pages only work while you hold
 * your own grimoire, shout their incantation, and scale with the grimoire's cover.
 */
public abstract class GrimoirePageSkill extends Skill {
    protected final MagicType magic;
    private final ResourceLocation icon;

    protected GrimoirePageSkill(MagicType magic) {
        super(SkillType.EXTRA);
        this.magic = magic;
        this.icon = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/skill/grimoire/" + magic.name().toLowerCase() + ".png");
    }

    @Override public ResourceLocation getSkillIcon() { return icon; }

    /** Holding your own grimoire? Pages are locked without it. */
    protected static boolean ready(ServerPlayer player) {
        for (ItemStack s : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
            if (GrimoireItem.isOwnedBy(s, player.getUUID())) return true;
        }
        SkillUtil.fail(player, "Your pages are sealed shut. Hold your grimoire.");
        return false;
    }

    protected static GrimoireCover cover(ServerPlayer p) {
        return GrimoirePages.grimoireOf(p).map(GrimoirePages::coverOf).orElse(GrimoireCover.THREE_LEAF);
    }

    /** Spell power from cover + global multiplier. */
    protected static double power(ServerPlayer p) { return cover(p).damage * NUConfig.DM_DAMAGE_MULT.get(); }

    /** Magicule cost after cover discount. Returns false (and spends nothing) if too low. */
    protected static boolean pay(ServerPlayer p, double base) {
        return SkillUtil.spendMagicules(p, base * cover(p).cost * NUConfig.DM_COST_MULT.get());
    }

    /** Diamond grimoires learn faster. */
    protected static void mastery(ManasSkillInstance i, ServerPlayer p, double amount) {
        i.addMasteryPoint(p, amount * (cover(p).kingdom == Kingdom.DIAMOND ? 1.5 : 1.0));
        i.markDirty();
    }

    /** Shout the incantation to everyone nearby (action bar), Black Clover style. */
    protected void shout(ServerPlayer p, String incantation, ChatFormatting color) {
        Component line = Component.literal(magic.displayName + ": " + incantation + "!").withStyle(color, ChatFormatting.BOLD);
        for (ServerPlayer other : p.serverLevel().players()) {
            if (other.distanceToSqr(p) < 32 * 32) other.displayClientMessage(line, true);
        }
    }

    /** Magic circle under/at the cast. */
    protected static void circle(ServerPlayer p, Vec3 at, Vec3 facing, int color, float size) {
        VfxSpawn.send(p.serverLevel(), VfxShape.MAGIC_CIRCLE, at, at.add(facing), color, 30, size);
    }
}
