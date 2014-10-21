package io.github.gelx_.wifiusermanagement;

import android.content.Context;
import android.util.Log;
import android.util.TimeFormatException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeoutException;

import io.github.gelx_.wifiusermanagement.database.DB_users;
import io.github.gelx_.wifiusermanagement.net.Connection;
import io.github.gelx_.wifiusermanagement.net.Protocol;

/**
 * Created by Gelx on 20.10.2014.
 */
public class ConnectionHelper {

    private Connection connection;

    public ConnectionHelper(InetSocketAddress address, Context context) throws IOException {
        this.connection = new Connection(address, context);
    }

    public DB_users[] getUsers() throws TimeoutException {
        Protocol.GetUsersPacket getUsersPacket = new Protocol.GetUsersPacket(connection.getAddress());
        connection.queuePacketForWrite(getUsersPacket);
        Protocol.RespUsersPacket respUsersPacket = null;
        try {
            synchronized (connection.getHandler().respUsersLock) {
                connection.getHandler().respUsersLock.wait(5 * 1000);
                respUsersPacket = connection.getHandler().lastRespUsersPacket;
            }
        } catch (InterruptedException e) {
            throw new TimeoutException("Server did not respond in time!");
        }
        return respUsersPacket.getUsers();
    }

    public DB_users getUser(String name) throws TimeoutException {
        Protocol.GetUserPacket getUserPacket = new Protocol.GetUserPacket(connection.getAddress(), name);
        connection.queuePacketForWrite(getUserPacket);
        Protocol.RespUserPacket respUserPacket = null;
        try {
            synchronized (connection.getHandler().respUserLock) {
                connection.getHandler().respUserLock.wait(5 * 1000);
                respUserPacket = connection.getHandler().lastRespUserPacket;
            }
        } catch (InterruptedException e) {
            throw new TimeoutException("Server did not respond in time!");
        }
        return respUserPacket.getUser();
    }

    public void closeConnection(){
        connection.close();
    }
}
