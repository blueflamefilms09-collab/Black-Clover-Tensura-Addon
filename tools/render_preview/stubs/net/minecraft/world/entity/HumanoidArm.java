package net.minecraft.world.entity;

/** Preview stub of HumanoidArm. */
public enum HumanoidArm {
    LEFT,
    RIGHT;

    public HumanoidArm getOpposite() { return this == LEFT ? RIGHT : LEFT; }
}
