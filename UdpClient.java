
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Random;
import java.net.Socket;

/**
 *
 * @author Jorge
 */
public class UdpClient {
    public static void main(String[] args) throws Exception {
        try (Socket socket = new Socket("codebank.xyz", 38005)) {
            int payload;
            int port;
            long timerStart;
            long timerStop;
            float timerAccrue = 0;
            byte[] input = new byte[4];
            
            input[0] = (byte)222;
            input[1] = (byte)173;
            input[2] = (byte)190;
            input[3] = (byte)239;
            
            Random mix = new Random();
            InputStream is = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            OutputStream os = socket.getOutputStream();
            
            //First packet sent, gets the port.
            os.write(recalculatePacket(4, mix, true, input, (byte)17, false, (short) 0));
            
            //Initialize array to recieve magic characters.
            int[] read = new int[6];
            read[0] = is.read();
            read[1] = is.read();
            read[2] = is.read();
            read[3] = is.read();
            read[4] = is.read();
            read[5] = is.read();
            
            //Prints received message.
            System.out.print("Handshake Response: 0x");
            for(int mn = 0; mn < 4; mn++){
                System.out.printf("%X", (read[mn]));
            }


            payload = (read[4] << 8);
            payload += (read[5] & 0xFF);
            System.out.println("\n\nPort number received: "+payload);
            port = (short) payload;
            
            //Main loop, doubles payload size every time.
            for(int i = 0; i < 12; i++ ){
                payload = 2;
                for(int j = i; j > 0; j--){
                    payload *=2;
                }
            timerStart = System.nanoTime();    
            os.write(recalculatePacket(payload, mix, false, input, (byte)17, true, port));
            System.out.println("Sending packet with "+ payload + " bytes of data.");
            read[0] = is.read();
            read[1] = is.read();
            read[2] = is.read();
            read[3] = is.read();
            timerStop = System.nanoTime();
            
            System.out.print("Response: 0x");
            for(int mn = 0; mn < 4; mn++){
                System.out.printf("%X", (read[mn]));
            }
            System.out.println("\nRTT: " + ((timerStop-timerStart)/1000000) + "ms\n");
            timerAccrue += ((timerStop-timerStart)/1000000);
            }
            timerAccrue /= 12;
            System.out.printf("Average RTT: %.2fms\n", timerAccrue);
        }
    }
    
    //Heres the mess.
    public static byte[] recalculatePacket(int size,Random random, boolean overwrite, byte[] custom, byte protocol, boolean udp, int udpport) {
        byte versionAndIHL = 69;
        byte tos = 0;
        short length;
        if(udp){
            length = (short)(28 + size);
        }else{
            length = (short)(20 + size);
        }
        short ident = 0;
        short flagsAndFrag = 1;
        flagsAndFrag <<= 14;
        byte ttl = 50;
        int checksum = 0;
        int sourceAddr = 1824010952;
        int destAddr = 874862746;
        short sourcePort = 0;
        short destPort = (short) udpport;
        short udplength = (short)(8 + size);
        int udpchecksum = 0;
        int[] checksumCount = new int[22];
        byte[] udpchecksumCount = new byte[(20 + size)];
        int[] udpcompressor = new int[(10 + (size/2))];
        byte[] temp;
        if(udp){
            temp = new byte[(28+size)];
        }else{
            temp = new byte[(20+size)];
        }
        //IPv4 Initializing section.
        temp[0] = versionAndIHL;
        temp[1] = tos;
        temp[2] = (byte) (length >> 8);
        temp[3] = (byte) (length);
        temp[4] = (byte) (ident >> 8);
        temp[5] = (byte) (ident);
        temp[6] = (byte) (flagsAndFrag >> 8);
        temp[7] = (byte) (flagsAndFrag);
        temp[8] = ttl;
        temp[9] = protocol;
        temp[10] = 0;
        temp[11] = 0;
        temp[12] = (byte) (sourceAddr >> 24);
        temp[13] = (byte) (sourceAddr >> 16);
        temp[14] = (byte) (sourceAddr >> 8);
        temp[15] = (byte) (sourceAddr);
        temp[16] = (byte) (destAddr >> 24);
        temp[17] = (byte) (destAddr >> 16);
        temp[18] = (byte) (destAddr >> 8);
        temp[19] = (byte) (destAddr);
        
        for (int j = 0; j < 20; j += 2) {
            checksumCount[(j / 2)] += (short) (temp[j] << 8);
            checksumCount[(j / 2)] += (short) (temp[(j + 1)] & 0xFF);
        }
        for (int i = 0; i < 10; i++) {
            checksum += checksumCount[i];
        }
        
        if(overwrite && !udp){
            for(int n = 20; n < (20+size); n++){
                temp[n] = custom[(n-20)];
            }
        }
        
        checksum++;
        checksum = ~checksum;
        temp[10] = (byte) (checksum >> 8);
        temp[11] = (byte) (checksum & 0xFF);        
        
        
        if(udp){
            
        temp[20] = (byte) (sourcePort >> 8);
        temp[21] = (byte) (sourcePort);
        temp[22] = (byte) (destPort >> 8);
        temp[23] = (byte) (destPort);
        temp[24] = (byte) (udplength >> 8);
        temp[25] = (byte) (udplength);
        
        udpchecksumCount[0] = (byte)(sourceAddr >> 24);
        udpchecksumCount[1] = (byte)(sourceAddr >> 16);
        udpchecksumCount[2] = (byte)(sourceAddr >> 8);
        udpchecksumCount[3] = (byte)(sourceAddr);
        udpchecksumCount[4] = (byte)(destAddr >> 24);
        udpchecksumCount[5] = (byte)(destAddr >> 16);
        udpchecksumCount[6] = (byte)(destAddr >> 8);
        udpchecksumCount[7] = (byte)(destAddr);
        udpchecksumCount[8] = 0;
        udpchecksumCount[9] = protocol;
        udpchecksumCount[10] = (byte)(udplength >> 8);
        udpchecksumCount[11] = (byte)(udplength);
        udpchecksumCount[12] = (byte)(sourcePort >> 8);
        udpchecksumCount[13] = (byte)(sourcePort);
        udpchecksumCount[14] = (byte)(destPort >> 8);
        udpchecksumCount[15] = (byte)(destPort);
        udpchecksumCount[16] = (byte)(udplength >> 8);
        udpchecksumCount[17] = (byte)(udplength);
        udpchecksumCount[18] = 0;
        udpchecksumCount[19] = 0;
        
        byte[] randomData = new byte[size];
        random.nextBytes(randomData);
        
        //Assigning random values
        for(int p = 0; p < size; p++){
            temp[p+28] = randomData[p];
            udpchecksumCount[p+20] = randomData[p];
        }
        
        //Compressing one short per every two bytes.
        for (int m = 0; m < (20 + size); m += 2){
            udpcompressor[(m/2)] += ((udpchecksumCount[m] << 8) & 0xFF00);
            udpcompressor[(m/2)] += (udpchecksumCount[m+1] & 0xFF);
        }
        
        for (int d = 0; d < (10+(size/2)); d++) {
            udpchecksum += udpcompressor[d];
            if((udpchecksum & 0xFFFF0000) != 0){
                udpchecksum &= 0x0000FFFF;
                udpchecksum ++;
            }
        }
        
        udpchecksum = (short)~udpchecksum;

        temp[26] = (byte) (udpchecksum >> 8);
        temp[27] = (byte) (udpchecksum & 0xFF);
        
        }
        
        return temp;
    }
}
