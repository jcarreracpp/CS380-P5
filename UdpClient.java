
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Random;
import java.net.Socket;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Jorge
 */
public class UdpClient {
    public static void main(String[] args) throws Exception {
        try (Socket socket = new Socket("codebank.xyz", 38005)) {
            int payload;
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
            
            os.write(recalculatePacket(4, mix, true, input, (byte)6, false));
            
            int[] read = new int[6];
            read[0]  = is.read();
            read[1] = is.read();
            read[2] = is.read();
            read[3] = is.read();
            read[4] = is.read();
            read[5] = is.read();
            
            for(int mn = 0; mn < 4; mn++){
                System.out.printf("%X", (read[mn]));
            }
            System.out.println();
            payload = (read[4] << 8);
            payload += (read[5]);
            System.out.println("Port number received: "+payload);
            
            for(int i = 0; i < 12; i++ ){
                payload = 2;
                for(int j = i; j > 0; j--){
                    payload *=2;
                }
            os.write(recalculatePacket(payload, mix, false, input, (byte)17, true));
            System.out.println("data length: "+payload);
            System.out.println(br.readLine()+"\n");
            }
        }
    }
    public static byte[] recalculatePacket(int size,Random random, boolean overwrite, byte[] custom, byte protocol, boolean udp) {
        byte versionAndIHL = 69;
        byte tos = 0;
        short length = (short)(28 + size);
        short ident = 0;
        short flagsAndFrag = 1;
        flagsAndFrag <<= 14;
        byte ttl = 50;
        int checksum = 0;
        int sourceAddr = 1824010952;
        int destAddr = 874862746;
        short sourcePort = 0;
        short destPort = (short)38005;
        short udplength = (short)(8 + size);
        int udpchecksum = 0;
        int[] checksumCount = new int[22];
        int sizeroundup;
        if(size % 2 == 0){
            sizeroundup = size/2;
        }else{ 
            sizeroundup = size/2;
            sizeroundup++;
        }
        int[] udpchecksumCount = new int[10 + (sizeroundup)];
        byte[] temp = new byte[(28+size)];
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
        for(int k = 20; k < length; k++){
            temp[k] = 0;
        }
        for (int j = 0; j < 20; j += 2) {
            checksumCount[(j / 2)] += (short) (temp[j] << 8);
            checksumCount[(j / 2)] += (short) (temp[(j + 1)] & 0xFF);
        }
        for (int i = 0; i < 10; i++) {
            checksum += checksumCount[i];
        }
        temp[20] = (byte) (sourcePort);
        temp[21] = (byte) (sourcePort);
        temp[22] = (byte) (destPort >> 8);
        temp[23] = (byte) (destPort);
        temp[24] = (byte) (udplength >> 8);
        temp[25] = (byte) (udplength);
        
        udpchecksumCount[0] += (short) (temp[12] << 8);
        udpchecksumCount[0] += (short) (temp[13]);
        udpchecksumCount[1] += (short) (temp[14] << 8);
        udpchecksumCount[1] += (short) (temp[15]);
        udpchecksumCount[2] += (short) (temp[16] << 8);
        udpchecksumCount[2] += (short) (temp[17]);
        udpchecksumCount[3] += (short) (temp[18] << 8);
        udpchecksumCount[3] += (short) (temp[19]);
        udpchecksumCount[4] += 0;
        udpchecksumCount[4] += protocol;
        udpchecksumCount[5] += udplength;
        udpchecksumCount[6] += sourcePort;
        udpchecksumCount[7] += destPort;
        udpchecksumCount[8] += length;
        udpchecksumCount[9] += 0;
        
        byte[] randomData = new byte[size];
        random.nextBytes(randomData);
        
        if(overwrite){
            for (int m = 10; m < (10 + (sizeroundup)); m++){
                udpchecksumCount[m] += (randomData[(m-10)] << 8);
                udpchecksumCount[m] += (randomData[(m-10)]);
            }
        }else{
            for (int m = 10; m < (10 + (sizeroundup)); m++){
                udpchecksumCount[m] += (custom[(m-10)] << 8);
                udpchecksumCount[m] += (custom[(m-10)]);
            }            
        }
        
        for(int n = 0; n < (10 + (size/2)); n++){
            udpchecksum += udpchecksumCount[n];
        }
        udpchecksum++;
        udpchecksum = ~udpchecksum;
        checksum++;
        checksum = ~checksum;
        temp[10] = (byte) (checksum >> 8);
        temp[11] = (byte) (checksum & 0xFF);
        temp[26] = (byte) (udpchecksum >> 8);
        temp[27] = (byte) (udpchecksum & 0xFF);
        
        return temp;
    }
}
