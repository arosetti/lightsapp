/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lightsapp.morse;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class MorseConverter {
    private final String TAG = "MorseConverter";
    private long SPEED_BASE;
    private long DOT;
    private long DASH;
    private long GAP;
    private long LETTER_GAP;
    private long WORD_GAP;

    private long[][] LETTERS;
    private long[][] NUMBERS;
    private long[] ERROR_GAP;
    private char[] charArray;
    private char[] simbolArray;

    public void updateValues(long speed) {
        DOT = SPEED_BASE = speed;
        DASH = SPEED_BASE * 3;
        GAP = -SPEED_BASE;
        LETTER_GAP = -SPEED_BASE * 3;
        WORD_GAP = -SPEED_BASE * 7;

        /** The characters from 'A' to 'Z' */
        LETTERS = new long[][]{
        /* A */ new long[]{DOT, GAP, DASH},
        /* B */ new long[]{DASH, GAP, DOT, GAP, DOT, GAP, DOT},
        /* C */ new long[]{DASH, GAP, DOT, GAP, DASH, GAP, DOT},
        /* D */ new long[]{DASH, GAP, DOT, GAP, DOT},
        /* E */ new long[]{DOT},
        /* F */ new long[]{DOT, GAP, DOT, GAP, DASH, GAP, DOT},
        /* G */ new long[]{DASH, GAP, DASH, GAP, DOT},
        /* H */ new long[]{DOT, GAP, DOT, GAP, DOT, GAP, DOT},
        /* I */ new long[]{DOT, GAP, DOT},
        /* J */ new long[]{DOT, GAP, DASH, GAP, DASH, GAP, DASH},
        /* K */ new long[]{DASH, GAP, DOT, GAP, DASH},
        /* L */ new long[]{DOT, GAP, DASH, GAP, DOT, GAP, DOT},
        /* M */ new long[]{DASH, GAP, DASH},
        /* N */ new long[]{DASH, GAP, DOT},
        /* O */ new long[]{DASH, GAP, DASH, GAP, DASH},
        /* P */ new long[]{DOT, GAP, DASH, GAP, DASH, GAP, DOT},
        /* Q */ new long[]{DASH, GAP, DASH, GAP, DOT, GAP, DASH},
        /* R */ new long[]{DOT, GAP, DASH, GAP, DOT},
        /* S */ new long[]{DOT, GAP, DOT, GAP, DOT},
        /* T */ new long[]{DASH},
        /* U */ new long[]{DOT, GAP, DOT, GAP, DASH},
        /* V */ new long[]{DOT, GAP, DOT, GAP, DOT, GAP, DASH},
        /* W */ new long[]{DOT, GAP, DASH, GAP, DASH},
        /* X */ new long[]{DASH, GAP, DOT, GAP, DOT, GAP, DASH},
        /* Y */ new long[]{DASH, GAP, DOT, GAP, DASH, GAP, DASH},
        /* Z */ new long[]{DASH, GAP, DASH, GAP, DOT, GAP, DOT},
        };

        charArray = new char[]{ 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'w', 'x', 'y', 'z' };

        /** The characters from '0' to '9' */
        NUMBERS = new long[][]{
        /* 0 */ new long[]{DASH, GAP, DASH, GAP, DASH, GAP, DASH, GAP, DASH},
        /* 1 */ new long[]{DOT, GAP, DASH, GAP, DASH, GAP, DASH, GAP, DASH},
        /* 2 */ new long[]{DOT, GAP, DOT, GAP, DASH, GAP, DASH, GAP, DASH},
        /* 3 */ new long[]{DOT, GAP, DOT, GAP, DOT, GAP, DASH, GAP, DASH},
        /* 4 */ new long[]{DOT, GAP, DOT, GAP, DOT, GAP, DOT, GAP, DASH},
        /* 5 */ new long[]{DOT, GAP, DOT, GAP, DOT, GAP, DOT, GAP, DOT},
        /* 6 */ new long[]{DASH, GAP, DOT, GAP, DOT, GAP, DOT, GAP, DOT},
        /* 7 */ new long[]{DASH, GAP, DASH, GAP, DOT, GAP, DOT, GAP, DOT},
        /* 8 */ new long[]{DASH, GAP, DASH, GAP, DASH, GAP, DOT, GAP, DOT},
        /* 9 */ new long[]{DASH, GAP, DASH, GAP, DASH, GAP, DASH, GAP, DOT},
        };

        simbolArray = new char[]{ '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };

        ERROR_GAP = new long[]{GAP};
    }

    public MorseConverter(int speed) {
        updateValues(speed);
    }

    public long get(String val) {
        if (val.equals("SPEED_BASE"))
            return SPEED_BASE;
        else if (val.equals("GAP"))
            return Math.abs(GAP);
        else if (val.equals("DASH"))
            return DASH;
        else if (val.equals("DOT"))
            return DOT;
        else if (val.equals("LETTER_GAP"))
            return Math.abs(LETTER_GAP);
        else if (val.equals("WORD_GAP"))
            return Math.abs(WORD_GAP);
        else
            return GAP;
    }

    long[] pattern(char c) {
        if (c >= 'A' && c <= 'Z') {
            return LETTERS[c - 'A'];
        }
        if (c >= 'a' && c <= 'z') {
            return LETTERS[c - 'a'];
        } else if (c >= '0' && c <= '9') {
            return NUMBERS[c - '0'];
        } else {
            return ERROR_GAP;
        }
    }

    public String getMorse(String str) {
        long[] l = pattern(str);
        String tmpStr = new String();
        for (int i = 0; i < l.length; i++) {
            if (i % 2 != 0) {
                if (l[i] == DOT) {
                    tmpStr = tmpStr.concat(".");
                } else if (l[i] == DASH) {
                    tmpStr = tmpStr.concat("-");
                }
            } else {
                if (l[i] == LETTER_GAP) {
                    tmpStr = tmpStr.concat(" ");
                } else if (l[i] == WORD_GAP) {
                    tmpStr = tmpStr.concat("   ");
                }
            }
        }
        return tmpStr;
    }

    private long[] ListToPrimitiveArray(List<Long> input) {
        long output[] = new long[input.size()];
        int index = 0;
        for(Long val : input) {
            output[index] =  val;
            index++;
        }
        return output;
    }

    public String getText(long data[]) {
        String str = "";
        List<Long> lchar = new ArrayList<Long>();

        for (int i = 0; i < data.length; i++) {
            if (data[i] == DOT || data[i] == GAP || data[i] == DASH) {
                lchar.add(data[i]);
            }
            else if (data[i] == LETTER_GAP) {
                str += getChar(ListToPrimitiveArray(lchar));
                lchar.clear();
            }
            else if (data [i] == WORD_GAP) {
                str += getChar(ListToPrimitiveArray(lchar));
                str += ' ';
                lchar.clear();
            }
            else {
                Log.e(TAG, "malformed morse data");
            }
        }

        if (lchar.size() != 0) {
            str += getChar(ListToPrimitiveArray(lchar));
        }

        return str;
    }

    private char getChar(long sequence[])
    {
        boolean found = false;

        for (int i = 0; i < LETTERS.length; i++) {
            if (sequence.length != LETTERS[i].length)
                continue;
            try {
                for (int j = 0; j < LETTERS[i].length; j++) {
                    if (sequence[j] != LETTERS[i][j]) {
                        found = false;
                        break;
                    } else {
                        found = true;
                    }
                }
            }
            catch (ArrayIndexOutOfBoundsException e) {
            }
            if (found)
                return charArray[i];
        }

        for (int i = 0; i < NUMBERS.length; i++) {
            if (sequence.length != NUMBERS[i].length)
                continue;
            try {
                for (int j = 0; j < NUMBERS[i].length; j++) {
                    if (sequence[j] != NUMBERS[i][j]) {
                        found = false;
                        break;
                    } else {
                        found = true;
                    }
                }
            }
            catch (ArrayIndexOutOfBoundsException e) {
            }
            if (found)
                return simbolArray[i];
        }

        return '*';
    }

    public long[] pattern(String str) {
        boolean lastWasWhitespace;
        int strlen = str.length();

        int len = 1;
        lastWasWhitespace = true;
        for (int i = 0; i < strlen; i++) {
            char c = str.charAt(i);
            if (Character.isWhitespace(c)) {
                if (!lastWasWhitespace) {
                    len++;
                    lastWasWhitespace = true;
                }
            } else {
                if (!lastWasWhitespace) {
                    len++;
                }
                lastWasWhitespace = false;
                len += pattern(c).length;
            }
        }

        long[] result = new long[len + 1];
        result[0] = 0;
        int pos = 1;
        lastWasWhitespace = true;
        for (int i = 0; i < strlen; i++) {
            char c = str.charAt(i);
            if (Character.isWhitespace(c)) {
                if (!lastWasWhitespace) {
                    result[pos] = WORD_GAP;
                    pos++;
                    lastWasWhitespace = true;
                }
            } else {
                if (!lastWasWhitespace) {
                    result[pos] = LETTER_GAP;
                    pos++;
                }
                lastWasWhitespace = false;
                long[] letter = pattern(c);
                System.arraycopy(letter, 0, result, pos, letter.length);
                pos += letter.length;
            }
        }
        return result;
    }
}
