package com.diggydwarff.deepdiving.sign;

/** Bounds editable captions while preserving complete Unicode code points. */
public final class DiveSignText {
    public static final int MAX_LENGTH = 32;

    public static String clean(String value) {
        StringBuilder result = new StringBuilder();
        int count = 0;
        for (int index = 0; index < value.length() && count < MAX_LENGTH; ) {
            int code = value.codePointAt(index);
            index += Character.charCount(code);
            if (Character.isISOControl(code)
                    || code == 0x00a7
                    || Character.getType(code) == Character.FORMAT) continue;
            result.appendCodePoint(code);
            count++;
        }
        return result.toString().strip();
    }

    private DiveSignText() {}
}
