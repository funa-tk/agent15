package net.luminis.tls.extension;

import net.luminis.tls.DecodeErrorException;
import net.luminis.tls.TlsConstants;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * TLS server name extension: RFC 6066
 * https://tools.ietf.org/html/rfc6066#section-3
 */
public class ServerNameExtension extends Extension {

    private final String serverName;

    public ServerNameExtension(String serverName) {
        this.serverName = serverName;
    }

    public ServerNameExtension(ByteBuffer buffer) throws DecodeErrorException {
        int extensionDataLength = parseExtensionHeader(buffer, TlsConstants.ExtensionType.server_name, 0);
        if (extensionDataLength > 0) {
            if (extensionDataLength < 2) {
                throw new DecodeErrorException("incorrect extension length");
            }
            int serverNameListLength = buffer.getShort();
            if (extensionDataLength != serverNameListLength + 2) {
                throw new DecodeErrorException("inconsistent length");
            }

            int startPosition = buffer.position();
            serverName = parseServerName(buffer);
            if (buffer.position() - startPosition != serverNameListLength) {
                throw new DecodeErrorException("inconsistent length");
            }
        }
        else {
            // https://tools.ietf.org/html/rfc6066#section-3
            // " A server that receives a client hello containing the "server_name" extension (...). In this event,
            // the server SHALL include an extension of type "server_name" in the (extended) server hello.
            // The "extension_data" field of this extension SHALL be empty."
            serverName = null;
        }
    }

    private String parseServerName(ByteBuffer buffer) {
        int nameType = buffer.get();
        switch (nameType) {
            case 0:
                // host_name
                int hostNameLength = buffer.getShort();
                byte[] hostNameBytes = new byte[hostNameLength];
                buffer.get(hostNameBytes);
                // "The hostname is represented as a byte string using ASCII encoding without a trailing dot. "
                return new String(hostNameBytes, Charset.forName("ASCII"));
        }
        // unsupported type, RFC 6066 only defines hostname
        throw new RuntimeException();
    }

    @Override
    public byte[] getBytes() {
        short hostnameLength = (short) serverName.length();
        short extensionLength = (short) (hostnameLength + 2 + 1 + 2);

        ByteBuffer buffer = ByteBuffer.allocate(4 + extensionLength);
        buffer.putShort(TlsConstants.ExtensionType.server_name.value);
        buffer.putShort(extensionLength);  // Extension data length (in bytes)

        // https://tools.ietf.org/html/rfc6066#section-3
        buffer.putShort((short) (hostnameLength + 1 + 2));  // Length of server_name_list
        buffer.put((byte) 0x00);  // list entry is type 0x00 "DNS hostname"
        buffer.putShort(hostnameLength);  // Length of hostname
        buffer.put(serverName.getBytes(Charset.forName("ASCII")));

        return buffer.array();
    }

    public String getHostName() {
        return serverName;
    }
}
