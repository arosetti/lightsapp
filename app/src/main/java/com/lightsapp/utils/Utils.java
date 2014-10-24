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
}
