import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        byte[] frame = {
                (byte) 0x81,    // FIN + Opcode (Text Frame)
                (byte) 0x05,    // Mask (1), Payload length (5)
                (byte) 0x37,    // Masking Key (first byte)
                (byte) 0x4F,    // Masking Key (second byte)
                (byte) 0x5A,    // Masking Key (third byte)
                (byte) 0x56,    // Masking Key (fourth byte)
                (byte) 0x48,    // Payload
                (byte) 0x65,    // Payload
                (byte) 0x6C,    // Payload
                (byte) 0x6C,    // Payload
                (byte) 0x6F     // Payload
        };

        ByteArrayInputStream in = new ByteArrayInputStream(frame);
        parseFrames(in);
    }

    public static void parseFrames(ByteArrayInputStream in) {
        int firstByte = in.read();
        int fin = (firstByte & 0xff) >> 7;
        int opCode = (firstByte & 0xf);

        System.out.println("FIN: " + fin);
        System.out.println("OpCode: " + opCode);

        int secondByte = in.read();
        int mask = (secondByte & 0xff) >> 7;
        int payloadLength = secondByte & 0b1111111;

        if(payloadLength == 126){
            payloadLength = (in.read() << 8) | in.read();
        }else if(payloadLength == 127){
            payloadLength = 0;
            for(int i = 0; i < 8; i++){
                payloadLength = (payloadLength << 8) | in.read();
            }
        }

        byte[] maskKey = new byte[4];
        in.read(maskKey, 0, 4);

        byte[] payload = new byte[payloadLength];
        in.read(payload, 0, payloadLength);

        if(mask == 1){
            for (int i = 0; i < payloadLength; i++){
                payload[i] = (byte) (payload[i] ^ maskKey[i%4]);
            }
        }

        System.out.println("Mask: " + mask);
        System.out.println("Payload Length: " + payloadLength);

        switch (opCode){
            case 0x1:
                String textPayload = new String(payload, StandardCharsets.UTF_8);
                System.out.println("Text frame Payload: " + textPayload);
                break;
            case 0x2:
                System.out.println("Binary frame payload: " + bytesToHex(payload));
                break;
            case 0x8:
                int statusCode = (payload[0] << 8) | (payload[1] & 0xff); // first two bytes are the status code
                String reason = new String(payload, 2, payloadLength - 2, StandardCharsets.UTF_8); // optional reason
                System.out.println("Close frame status code: " + statusCode);
                System.out.println("Close frame reason: " + reason);
                break;
            case 0x9:
                System.out.println("Frame Ping: " + bytesToHex(payload));
                break;
            case 0xa:
                System.out.println("Frame Pong: " + bytesToHex(payload));
                break;
            default:
        }

    }

    public static String bytesToHex(byte[] bytes){
        StringBuilder stringBuilder = new StringBuilder();
        for(byte payloadByte : bytes){
            stringBuilder.append(String.format("%02X", payloadByte));
        }
        return stringBuilder.toString();
    }
}
