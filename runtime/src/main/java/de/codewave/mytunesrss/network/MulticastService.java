/*
 * Copyright (c) 2007, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.network;

import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.utils.network.NetworkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.*;

/**
 * de.codewave.mytunesrss.network.MulticastService
 */
public class MulticastService extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(MulticastService.class);

    private static final String QUERY = "mtrqs";
    private static final String MULTICAST_IP = "225.24.2.72";
    private static final int MULTICAST_PORT = 8072;
    private static MulticastService THREAD;

    private boolean myStopRequested;


    public MulticastService() {
        super(MyTunesRss.THREAD_PREFIX + "Multicast Server Discovery Listener");
    }

    public static synchronized void startListener() {
        if (THREAD == null) {
            THREAD = new MulticastService();
            THREAD.start();
        }
    }

    public static synchronized void stopListener() {
        if (THREAD != null) {
            THREAD.myStopRequested = true;
            while (THREAD.isAlive()) {
                try {
                    MulticastService.class.wait(200);
                } catch (InterruptedException ignored) {
                    // intentionally left blank
                }
            }
            THREAD = null;
        }
    }

    public static Collection<RemoteServer> getOtherInstances() {
        Set<RemoteServer> otherInstances = new HashSet<>();
        MulticastSocket socket = null;
        try {
            socket = new MulticastSocket();
            byte[] buffer = QUERY.getBytes("UTF-8");
            DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(MULTICAST_IP), MULTICAST_PORT);
            socket.setLoopbackMode(true);
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = interfaces.nextElement();
                    try {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Sending discovery packet to \"" + networkInterface.getDisplayName() + "\"");
                        }
                        socket.setNetworkInterface(networkInterface);
                        socket.send(sendPacket);
                    } catch (IOException e) {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn("Could not send discovery packet to \"" + networkInterface.getDisplayName() + "\"", e);
                        }
                    }
                }
                try {
                    Collection<String> localAddresses =
                            new ArrayList(Arrays.asList(getLocalAddresses(String.valueOf(MyTunesRss.CONFIG.getPort()))));
                    localAddresses.add("http://127.0.0.1:" + MyTunesRss.CONFIG.getPort());
                    socket.setSoTimeout(2000);
                    buffer = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                    //noinspection InfiniteLoopStatement
                    while (true) {
                        socket.receive(receivePacket);
                        String answer = new String(receivePacket.getData(), 0, receivePacket.getLength(), "UTF-8");
                        String name = answer.substring(answer.indexOf(':') + 1);
                        int port = Integer.parseInt(answer.substring(0, answer.indexOf(':')));
                        if (!localAddresses.contains("http://" + receivePacket.getAddress().getHostAddress() + ":" + port)) {
                            otherInstances.add(new RemoteServer(name, receivePacket.getAddress().getHostName(), port));
                        }
                    }
                } catch (SocketTimeoutException ignored) {
                    // intentionally left blank
                }
            }
        } catch (IOException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Could not get multicast answers.", e);
            }
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
        return otherInstances;
    }

    public static String[] getLocalAddresses(String serverPort) {
        String[] addresses = NetworkUtils.getLocalNetworkAddresses();
        for (int i = 0; i < addresses.length; i++) {
            addresses[i] = "http://" + addresses[i] + ":" + serverPort;
        }
        return addresses;
    }

    @Override
    public void run() {
        MulticastSocket socket = null;
        try {
            socket = new MulticastSocket(MULTICAST_PORT);
            socket.setSoTimeout(1000);
            socket.joinGroup(InetAddress.getByName(MULTICAST_IP));
            byte[] buffer = new byte[1024];
            while (!myStopRequested) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                    socket.receive(receivePacket);
                    String data = new String(receivePacket.getData(), 0, receivePacket.getLength(), "UTF-8");
                    if (QUERY.equals(data)) {
                        byte[] answer = (MyTunesRss.CONFIG.getPort() + ":" + MyTunesRss.CONFIG.getServerName()).getBytes("UTF-8");
                        DatagramPacket sendPacket = new DatagramPacket(answer, answer.length, receivePacket.getAddress(), receivePacket.getPort());
                        socket.send(sendPacket);
                    }
                } catch (SocketTimeoutException ignored) {
                    // intentionally left blank
                } catch (IOException e) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Could not send multicast answer.", e);
                    }
                }
            }
        } catch (IOException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Could not initialize multicast service.", e);
            }
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}
