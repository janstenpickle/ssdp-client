package com.vmichalak.protocol.ssdp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Client for discovering UPNP devices with SSDP (Simple Service Discovery Protocol).
 */
public class SSDPClient {
    /**
     * Discover any UPNP device using SSDP (Simple Service Discovery Protocol).
     * @param timeout in milliseconds
     * @param searchTarget if null it use "ssdp:all"
     * @return List of devices discovered
     * @throws IOException
     * @see <a href="https://en.wikipedia.org/wiki/Simple_Service_Discovery_Protocol">SSDP Wikipedia Page</a>
     */
    public static List<Device> discover(int timeout, String searchTarget) throws IOException {
        ArrayList<Device> devices = new ArrayList<Device>();
        byte[] sendData;
        byte[] receiveData = new byte[1024];

        /* Create the search request */
        StringBuilder msearch = new StringBuilder("M-SEARCH * HTTP/1.1\nHost: 239.255.255.250:1900\nMAN: ssdp:discover\n");
        if (searchTarget == null) { msearch.append("ST: ssdp:all\n"); }
        else { msearch.append("ST: ").append(searchTarget).append("\n"); }
        if (timeout >= 1100) { msearch.append("MX: " + Math.floor((timeout-100)/1000) + "\n"); }  // give devices 100ms to respond (in the worst case)
        msearch.append("\r\n");

        /* Send the request */
        sendData = msearch.toString().getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("239.255.255.250"), 1900);
        DatagramSocket clientSocket = new DatagramSocket(1901);
        clientSocket.setSoTimeout(timeout);
        clientSocket.send(sendPacket);

        /* Receive all responses */
        while (true) {
            try {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                clientSocket.receive(receivePacket);
                if (searchTarget == null || new String(receivePacket.getData()).contains(searchTarget)) {
                    devices.add(Device.parse(receivePacket));
                }
            }
            catch (SocketTimeoutException e) { break; }
        }

        clientSocket.close();
        return Collections.unmodifiableList(devices);
    }

    public static Device discoverOne(int timeout, String searchTarget) throws IOException {
        Device device = null;
        byte[] sendData;
        byte[] receiveData = new byte[1024];

        /* Create the search request */
        StringBuilder msearch = new StringBuilder("M-SEARCH * HTTP/1.1\nHost: 239.255.255.250:1900\nMAN: ssdp:discover\n");
        if (searchTarget == null) { msearch.append("ST: ssdp:all\n"); }
        else { msearch.append("ST: ").append(searchTarget).append("\n"); }
        if (timeout >= 1100) { msearch.append("MX: " + Math.floor((timeout-100)/1000) + "\n"); }  // give devices 100ms to respond (in the worst case)
        msearch.append("\r\n");

        /* Send the request */
        sendData = msearch.toString().getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("239.255.255.250"), 1900);
        DatagramSocket clientSocket = new DatagramSocket(1902);
        clientSocket.setSoTimeout(timeout);
        clientSocket.send(sendPacket);

        /* Receive all responses, but pick first matching... */
        do {
            try {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                clientSocket.receive(receivePacket);
                if (searchTarget == null || new String(receivePacket.getData()).contains(searchTarget)) {
                    device = Device.parse(receivePacket);
                }
            }
            catch (SocketTimeoutException e) { break; }
        } while(device == null);

        clientSocket.close();
        return device;
    }
}
