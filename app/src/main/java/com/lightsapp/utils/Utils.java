package com.lightsapp.utils;

import java.text.Normalizer;
import java.util.List;

public class Utils {

    public static long[] ListToPrimitiveArray(List<Long> input) {
        long output[] = new long[input.size()];
        int index = 0;
        for(Long val : input) {
            output[index] =  val;
            index++;
        }
        return output;
    }

    public static String CleanString(String str) {
        return Normalizer.normalize(str, Normalizer.Form.NFD).replaceAll("[^\\x00-\\x7F]", "");
    }

    public static String StripString(String str, int n) {
        int r = str.length() - n;
        return (r > 0) ? str.substring(r) : str;
    }

    public static void ForcedSleep(int msec) {
        final long endingTime = System.currentTimeMillis() + msec;
        long remainingTime = msec;
        while (remainingTime > 0) {
            try {
                Thread.sleep(remainingTime);
            } catch (InterruptedException ignore) {
            }
            remainingTime = endingTime - System.currentTimeMillis();
        }
    }
}
