package net.minecraft.server.packs.resources;

import java.io.BufferedReader;
import java.io.IOException;

/** Preview stub of Resource (never reached: the preview replaces the geo file source). */
public class Resource {
    public BufferedReader openAsReader() throws IOException { throw new IOException("the preview reads geo files from disk"); }
}
