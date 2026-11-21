package com.newuniverse.nusmp.dragonballs.voice;

/**
 * Why: Simple Voice Chat hands over 48 kHz audio, the recogniser wants 16 kHz. A whole-number ratio (48000 / 16000 = 3) averages groups of samples, which is also a crude low-pass filter; any other
 * rate is linearly interpolated. Pure, one allocation per frame (a third of the input).
 */
public final class Resampler {
    private Resampler() {}

    public static final int TARGET = 16000;

    public static short[] to16k(short[] in, int rate) {
        if (in == null || in.length == 0 || rate <= 0) return new short[0];
        if (rate == TARGET) return in;
        if (rate % TARGET == 0) {
            int f = rate / TARGET, len = in.length / f;
            short[] out = new short[len];
            for (int i = 0; i < len; i++) {
                int sum = 0;
                for (int k = 0; k < f; k++) sum += in[i * f + k];
                out[i] = (short) (sum / f);
            }
            return out;
        }
        int len = (int) ((long) in.length * TARGET / rate);   // shortcut: linear interpolation, no low-pass; fine for an odd rate a voice mod never uses, upgrade if one ever does
        short[] out = new short[len];
        double step = (double) rate / TARGET;
        for (int i = 0; i < len; i++) {
            double pos = i * step;
            int a = (int) pos, b = Math.min(in.length - 1, a + 1);
            double fr = pos - a;
            out[i] = (short) Math.round(in[a] * (1 - fr) + in[b] * fr);
        }
        return out;
    }
}
