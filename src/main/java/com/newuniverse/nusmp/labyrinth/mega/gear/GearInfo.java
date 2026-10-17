package com.newuniverse.nusmp.labyrinth.mega.gear;

/**
 * Why: this is the whole of what the data component {@code nusmp:gear_grade} carries, and the component is what makes an ordinary vanilla item STANDARD
 * GEAR: its grade (so the name colour, the lore and the quest can say Normal, Special or Rare), its slot (so the magic resistance counts it only where
 * it belongs: armour worn, a weapon or shield held) and its magic resistance in percent. It is a plain record of values with value equality, so two
 * stacks stamped alike stack alike and a stack copy is equal to its original. It sits in the pure part (no Minecraft import) because the resistance sum
 * and the roller's output are tested without a server; the codecs that save and sync it live in {@link GearComponents}. The factory
 * methods never fail on bad data (an unknown grade becomes NORMAL, an unknown slot SWORD, a resistance is clamped to 0..100): a stack saved by a future
 * version must still load in this one, and a stack that cannot load is an item lost.
 */
public record GearInfo(GearGrade grade, GearSlot slot, int magicResistPercent) {
    public GearInfo {
        if (grade == null) grade = GearGrade.NORMAL;
        if (slot == null) slot = GearSlot.SWORD;
        magicResistPercent = Math.max(0, Math.min(100, magicResistPercent));
    }

    /** Builds from the saved strings, tolerating anything (see the class comment). */
    public static GearInfo of(String gradeId, String slotId, int magicResistPercent) {
        return new GearInfo(GearGrade.fromId(gradeId), GearSlot.fromId(slotId), magicResistPercent);
    }

    public String gradeId() { return grade.id(); }

    public String slotId() { return slot.id(); }
}
