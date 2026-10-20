package com.newuniverse.nusmp.labyrinth.squad;

import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryMobEntity;
import com.newuniverse.nusmp.labyrinth.squad.SquadGeometry.V2;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.item.ProjectileWeaponItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: this is the seam between the Dungeon Master and the squads, and it is deliberately narrow so the Director gains a handful of lines and nothing more. The Director
 * asks {@link #begin} once it knows an encounter will be spawned (that makes the squad, with the plan for the floor), stamps every monster it creates with the squad's id BEFORE
 * the monster joins the world ({@link #stamp}, so the join event can attach the monster and give it the goals, which is also the path a monster takes when it comes back
 * from a saved chunk), and calls {@link #end} with the monsters that were really placed: only then are the roles handed out, because a role is chosen from what the monster
 * IS (its health, armour, speed, ranged attack, Foundry archetype), and a squad that came out smaller than planned (a place was not found) gets the plan trimmed, not a
 * hole. A squad of fewer than three is dissolved: two monsters are not a formation. Nothing here changes an entity type, and with {@code squads.enabled} false none of it runs.
 */
public final class SquadHook {
    private SquadHook() {}

    /** The squad being built by the Director right now: valid for the game tick it began in. */
    private record Pending(UUID id, long seed, long tick, Squad squad, SquadPlan.Plan plan) {}

    private static Pending current;
    private static int counter;

    /** Are squads on? */
    public static boolean on() { return SquadConfig.enabled(); }

    /** The headcount an encounter of this band at this depth with this party wants (5 to 8). */
    public static int headcount(long seed, int depth, int party, int band) { return SquadPlan.sizeFor(seed, depth, party, band); }

    /** Begins a squad for the encounter the Director is about to spawn; the plan comes from the seed, the depth, the party, the band (0..3), the faction and the theme. */
    public static SquadPlan.Plan begin(ServerLevel level, long seed, int depth, int party, int band, String faction, String theme) {
        SquadPlan.Plan plan = SquadPlan.plan(seed, depth, party, band, faction, theme);
        long now = level.getGameTime();
        UUID id = new UUID(seed, now * 31L + (++counter));
        Squad squad = SquadManager.create(id, seed, plan.size(), plan, true, now);
        current = new Pending(id, seed, now, squad, plan);
        return plan;
    }

    /** Called by the spawner just before a monster joins the world: writes the squad's id into its persistent data so the join event attaches it. */
    public static void stamp(Mob mob) {
        Pending p = current;
        if (p == null || mob == null || mob.level().getGameTime() != p.tick()) return;
        CompoundTag d = mob.getPersistentData();
        d.putString(Squad.ID, p.id().toString());
        d.putLong(Squad.SEED, p.seed());
        d.putInt(Squad.SIZE, p.plan().size());
    }

    /** Abandons the squad being built (the encounter found no place for anyone). */
    public static void abort() {
        Pending p = current;
        current = null;
        if (p != null) {
            p.squad().dissolve();
            SquadManager.remove(p.id());
        }
    }

    /**
     * Finishes the squad with the monsters that were really placed: assigns the roles, lays out the formation, tells the party. Returns the squad, or null when it was
     * dissolved (fewer than three monsters, or squads switched off in between).
     */
    public static Squad end(ServerLevel level, List<Mob> made) {
        Pending p = current;
        current = null;
        if (p == null) return null;
        Squad squad = p.squad();
        List<Mob> mobs = new ArrayList<>();
        for (Mob m : made) if (m != null && m.isAlive() && squad.member(m.getUUID()) != null) mobs.add(m);
        if (mobs.size() < 3) {
            squad.dissolve();
            SquadManager.remove(p.id());
            return null;
        }
        squad.resize(mobs.size());
        List<SquadRole> wanted = mobs.size() == p.plan().size() ? p.plan().roles() : SquadPlan.trim(p.plan().roles(), mobs.size());
        List<RoleFit.Traits> traits = new ArrayList<>(mobs.size());
        for (Mob m : mobs) traits.add(traitsOf(m));
        squad.assign(mobs, RoleFit.assign(wanted, traits));
        V2 at = new V2(mobs.get(0).getX(), mobs.get(0).getZ());
        SquadAnnounce.formed(level, squad, at);
        return squad;
    }

    /** What the role fit reads of a mob. A Mob Foundry mob says what it is; any other mob is read from its attributes, its weapon and the ranged-attack interfaces. */
    public static RoleFit.Traits traitsOf(Mob m) {
        double armor = m.getAttributes().hasAttribute(Attributes.ARMOR) ? m.getAttributeValue(Attributes.ARMOR) : 0.0;
        double speed = m.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED) ? m.getAttributeValue(Attributes.MOVEMENT_SPEED) : 0.25;
        double damage = m.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? m.getAttributeValue(Attributes.ATTACK_DAMAGE) : 1.0;
        boolean ranged = m instanceof RangedAttackMob || m instanceof CrossbowAttackMob || m.getMainHandItem().getItem() instanceof ProjectileWeaponItem;
        String arch = "";
        boolean caster = false;
        if (m instanceof FoundryMobEntity f) {
            arch = f.archetype().id();
            caster = arch.equals("caster") || arch.equals("support") || arch.equals("summoner");
            ranged = ranged || arch.equals("archer") || arch.equals("caster");
        }
        return new RoleFit.Traits(m.getMaxHealth(), armor, speed, damage, ranged, caster, arch);
    }

    /**
     * Is this monster a member of a live squad that is NOT its leader? The Director's hunt leaves such a monster to its formation goal (only the leader is sent to find the
     * party; the others follow it). A broken squad, a squad the server forgot, or a monster with no squad answers false, so the old behaviour holds for them.
     */
    public static boolean followsLeader(Mob m) {
        if (!m.getTags().contains(Squad.TAG)) return false;
        Squad s = SquadManager.of(m);
        if (s == null || s.isBroken() || s.leaderId() == null) return false;
        return !s.leaderId().equals(m.getUUID());
    }
}
