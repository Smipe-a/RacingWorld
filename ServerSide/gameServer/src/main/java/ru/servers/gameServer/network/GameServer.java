/*
 * Copyright 2018 Vladimir Balun
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.servers.gameServer.network;

import lombok.extern.log4j.Log4j;
import ru.servers.gameServer.network.protocol.fromServer.PacketFromServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Log4j
public class GameServer implements Server {

    private boolean isListening;
    private DatagramSocket serverSocket;

    public GameServer(int port) throws IOException {
        serverSocket = new DatagramSocket(port);
    }

    @Override
    public void startServer(int maxCountConnections){
        ExecutorService threadPool = Executors.newFixedThreadPool(maxCountConnections);
        Semaphore semaphore = new Semaphore(maxCountConnections);
        log.info("Game server started successfully.");
        isListening = true;

        while (isListening) {
           threadPool.execute(() -> {
                try {
                    semaphore.acquire();
                    byte[] buffer = new byte[1024];
                    DatagramPacket packetFromClient = new DatagramPacket(buffer, buffer.length);
                    serverSocket.receive(packetFromClient);
                    handleRequest(packetFromClient);
                    log.debug("Request was handled successfully.");
                } catch (IOException | InterruptedException e) {
                    log.error("Error during handling of request. Cause:" + e.getMessage());
                } finally {
                    semaphore.release();
                }
            });
        }
    }

    private void handleRequest(DatagramPacket packetFromClient) throws IOException {
        InetAddress clientAddress = packetFromClient.getAddress();
        int clientPort = packetFromClient.getPort();
        NetworkManager networkManager = new NetworkManager();
        PacketFromServer packet = networkManager.onReceive(packetFromClient.getData());
        DatagramPacket packetToClient = new DatagramPacket(packet.getBuffer(), packet.getBuffer().length, clientAddress, clientPort);
        serverSocket.send(packetToClient);
    }

    @Override
    public void stopServer() {
        isListening = false;
        serverSocket.close();
    }

}