package io.github.gelx_.wifiusermanagement.net;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import io.github.gelx_.wifiusermanagement.database.DB_users;
import io.github.gelx_.wifiusermanagement.net.Protocol.GetUserPacket;
import io.github.gelx_.wifiusermanagement.net.Protocol.Packet;
import io.github.gelx_.wifiusermanagement.net.Protocol.RegisterUserPacket;
import io.github.gelx_.wifiusermanagement.net.Protocol.RespUserPacket;
import io.github.gelx_.wifiusermanagement.net.Protocol.RespUsersPacket;

/**
 * Created by Falk on 08.10.2014.
 */
public class PacketHandler implements Runnable{

    public static class WaitingObj{}

    private BlockingDeque<Packet> packetQueue = new LinkedBlockingDeque<Packet>(50);
    private Thread thread;
    private Connection client;

    public final WaitingObj respUserLock = new WaitingObj();
    public final WaitingObj respUsersLock = new WaitingObj();

    public RespUserPacket lastRespUserPacket = null;
    public RespUsersPacket lastRespUsersPacket = null;

    public PacketHandler(Connection client){
        this.thread = new Thread(this);
        this.client = client;
        thread.start();
    }

    public void queuePacket(Packet packet){
        if(!packetQueue.offer(packet)){
            System.out.println("Could not handle packet: Queue overflow!");
        }
    }

    public void run(){
        while(!Thread.interrupted()){
            try {
                this.handlePacket(packetQueue.take());
            } catch (InterruptedException e) {
                System.out.println("PacketHandler interrupted!");
            }
        }
    }

    public void stop(){
        this.thread.interrupt();
    }

    public void handlePacket(Packet packet){

        switch (packet.getID()){
            case 4: RespUserPacket respUserPacket = (RespUserPacket) packet;
                    synchronized (respUserLock) {
                        this.lastRespUserPacket = respUserPacket;
                        this.respUserLock.notifyAll();
                    }
                    break;
            case 5: RespUsersPacket respUsersPacket = (RespUsersPacket) packet;
                    synchronized (respUsersLock) {
                        this.lastRespUsersPacket = respUsersPacket;
                        this.respUsersLock.notifyAll();
                    }
                    break;
            default:
                System.out.println("No handling for packet with ID " + packet.getID() + " implemented!");
        }
    }

    public void handlePacketDebug(Packet packet){

        switch (packet.getID()){
            case 1: RegisterUserPacket registerUserPacket = (RegisterUserPacket) packet;
                DB_users registerUser = registerUserPacket.getUser();
                System.out.println("Received registerUserPacket. User: " + registerUser.toString());
                break;
            case 2: GetUserPacket getUserPacket = (GetUserPacket) packet;
                String name = getUserPacket.getName();
                System.out.println("Received getUserPacket. Name: " + name);
                client.queuePacketForWrite(new RespUserPacket(packet.getAddress(), new DB_users(name, "AA:AA:AA:AA:AA:AA", System.currentTimeMillis() + 3600000)));
                break;
            case 3:
                System.out.println("Received getUsersPacket");
                break;
            case 4: RespUserPacket respUserPacket = (RespUserPacket) packet;
                DB_users respUser = respUserPacket.getUser();
                System.out.println("Received respUserPacket. User: " + respUser.toString());
                break;
            case 5: RespUsersPacket respUsersPacket = (RespUsersPacket) packet;
                DB_users[] respUsers = respUsersPacket.getUsers();
                System.out.println("Received respUsersPacket! Users: ");
                for (DB_users respUsersUser : respUsers){
                    System.out.println(respUsersUser.toString());
                }
                break;
            default: System.out.println("No handling for packet with ID " + packet.getID() + " implemented!");
        }
    }

}
