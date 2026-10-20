package com.newuniverse.nusmp.labyrinth.mega.guard;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Why: the mega-boss of an exclusive pocket is the Labyrinth Guardian wearing a D&amp;D profile of its own (phases, legendary and lair actions, a mythic second life) instead of the profile of a floor,
 * and the Guardian keeps that machinery inside its package (the state object, the look sync and the name refresh are package-private). This class is the single doorway the pocket code goes through:
 * a NEW file beside the Guardian, so the Decennial bosses' own classes stay exactly as they are. {@link #apply} puts a profile on a Guardian, its look in the synced data and its name over the boss
 * bar, and widens its damage and body by the profile's mega numbers with permanent attribute modifiers (which the entity saves by itself); {@link #ensure} puts the profile back when a load reset it
 * to the floor's (the Guardian reads {@code DndProfiles.forBoss} again whenever it is loaded), which the pocket's one-second sweep calls.
 */
public final class PocketBossBridge {
    private PocketBossBridge() {}

    private static final ResourceLocation DAMAGE = ResourceLocation.fromNamespaceAndPath("nusmp", "pocket_boss_damage"), SCALE = ResourceLocation.fromNamespaceAndPath("nusmp", "pocket_boss_scale");

    /** Puts {@code profile} on the Guardian, with its damage multiplied by {@code damage} and its body by {@code scale} (both 1 or more). */
    public static void apply(LabyrinthGuardianEntity g, DndProfile profile, double damage, double scale) {
        g.dnd().configure(profile);
        g.syncLook();
        g.refreshDndName(profile, g.dnd().phase());
        modifier(g, Attributes.ATTACK_DAMAGE, DAMAGE, damage - 1.0);
        modifier(g, Attributes.SCALE, SCALE, scale - 1.0);
        g.refreshDimensions();
    }

    private static void modifier(LabyrinthGuardianEntity g, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance inst = g.getAttribute(attribute);
        if (inst == null) return;
        inst.removeModifier(id);
        if (amount > 1e-6) inst.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /** Is the Guardian wearing this profile? */
    public static boolean wears(LabyrinthGuardianEntity g, DndProfile profile) { return g.dnd().profile.equals(profile); }

    /** Puts the profile back if a load replaced it; true when it had to. Never touches the health, the phase or a spent mythic trait. */
    public static boolean ensure(LabyrinthGuardianEntity g, DndProfile profile) {
        if (wears(g, profile)) return false;
        int left = g.dnd().resistancesLeft();                                         // what the load restored of the fight is kept: the profile is put back, the resistances already spent stay spent
        g.dnd().configure(profile);
        g.dnd().resistancesLeft = Math.min(left, g.dnd().resistancesLeft);
        g.syncLook();
        g.refreshDndName(profile, g.dnd().phase());
        return true;
    }

    /** The phase the Guardian is in (1 to 3). */
    public static int phase(LabyrinthGuardianEntity g) { return g.dnd().phase(); }

    /** The profile it wears. */
    public static DndProfile profile(LabyrinthGuardianEntity g) { return g.dnd().profile; }
}
