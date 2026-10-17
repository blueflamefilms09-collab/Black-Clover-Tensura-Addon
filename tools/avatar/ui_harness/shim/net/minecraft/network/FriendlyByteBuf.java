package net.minecraft.network;

/** Why: harness shim: the rows only ever write and read in the real game. */
public class FriendlyByteBuf {
    public FriendlyByteBuf writeUtf(String s, int max) { return this; }
    public FriendlyByteBuf writeUtf(String s) { return this; }
    public String readUtf(int max) { return ""; }
    public FriendlyByteBuf writeVarInt(int v) { return this; }
    public int readVarInt() { return 0; }
    public FriendlyByteBuf writeBoolean(boolean v) { return this; }
    public boolean readBoolean() { return false; }
    public FriendlyByteBuf writeFloat(float v) { return this; }
    public float readFloat() { return 0f; }
    public FriendlyByteBuf writeInt(int v) { return this; }
    public int readInt() { return 0; }
}
