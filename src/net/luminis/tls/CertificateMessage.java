package net.luminis.tls;

import java.nio.ByteBuffer;

public class CertificateMessage {

    public void parse(ByteBuffer buffer, int length, TlsState state) {
        for (int i = 0; i < length; i++)
            buffer.get();
        System.out.println("Got Certificate message (" + length + " bytes)");
    }
}
