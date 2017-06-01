/* 
 * Copyright (C) 2017 kilian
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package Server;

import helpers.Information;
import java.io.Serializable;
import java.util.LinkedList;

/**
 *
 * @author kilian
 */
public class Computer implements Serializable {

    private ComputerType type;
    private int id;
    private boolean isConnected = false;

    private CheckConnection connection;
    private int port;
    private String serverIpAddres = "";
    private LinkedList<ConnectionThread> allConnections = new LinkedList<>(); //only for clients

    private static final Object synchronizer = new Object();

    private int renderingThreads;
    private int waitingThreads;

    private int framesRendered;
    private int framesRemaining;

    private int computersConnected = 0; //only for server or global Scomputers

    /**
     * Gives the connection-check socket to the computer. IP-Address and Port
     * will be set to 'Computer' automatically.
     *
     * @param ipAddress
     * @param port
     */
    public void setData(String ipAddress, int port) {
        this.port = port;
        if (type == ComputerType.client) {
            this.serverIpAddres = ipAddress;
//            this.serverIpAddres = s.getInetAddress().getHostAddress();
        }
    }

    public void connectTo(CheckConnection checkThread) {
        this.isConnected = true;
        this.connection = checkThread;
    }

    void addConnection(ConnectionThread thread) {
        synchronized (synchronizer) {
            allConnections.add(thread);
        }
    }

    public enum ComputerType {
        client,
        server,
        standalone,
        global
    }

    public Computer(ComputerType type) {
        switch (type) {
            case global:
                id = 0;
                break;
            case standalone:
                id = -1;
                break;
            default:
                id = Information.getNextComputerId();
                break;
        }
        this.type = type;
    }

    public void setIPAddress(String address) {
        this.serverIpAddres = address;
    }

//    public LinkedList<RenderTask> getAllTasks() {
//        return tasksToRenderLocal;
//    }
    public int getId() {
        return id;
    }

    public int getPort() {
        return port;
    }

    public String getServerIpAddress() {
        return serverIpAddres;
    }

    public int getRenderingThreads() {
        return renderingThreads;
    }

    public void setRenderingThreads(int renderingThreads) {
        this.renderingThreads = renderingThreads;
    }

    /**
     *
     * @param reset resets frames rendered to 0
     * @return Frames already rendered by this computer
     */
    public int getFramesRendered(boolean reset) {
        synchronized (synchronizer) {
            int tmp = framesRendered;
            if (reset) {
                framesRendered = 0;
            }
            return tmp;
        }
    }

    public int getFramesRendered() {
        synchronized (synchronizer) {
            return getFramesRendered(false);
        }
    }

    public void setFramesRendered(int framesRendered) {
        synchronized (synchronizer) {
            this.framesRendered = framesRendered;
        }
    }

    public void removeSocket(ConnectionThread s) {
        synchronized (synchronizer) {
            allConnections.remove(s);
        }
    }

    public int getWaitingThreads() {
        return waitingThreads;
    }

    public void setWaitingThreads(int waitingThreads) {
        this.waitingThreads = waitingThreads;
    }

    public int getFramesRemaining() {
        return framesRemaining;
    }

    public void setFramesRemaining(int framesRemaining) {
        this.framesRemaining = framesRemaining;
    }

    public int getComputersConnected() {
        return computersConnected;
    }

    public void setComputersConnected(int computersConnected) {
        this.computersConnected = computersConnected;
    }

    public boolean isIsConnected() {
        return isConnected;
    }

    public void shutdownConnection() {

        if (connection == null) {
            return;
        }

        if (connection.isConnected()) {
            connection.shutdown();
        }

        while (connection.isConnected()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }
        
        type = ComputerType.standalone;

        isConnected = false;

    }

    /**
     * Also cleans closed Sockets from the allSockets list
     *
     * @return Returns the number of active sockets -> number of connection
     * threads and also removes inactive Threads (own garbage collector)
     */
    public int getConnectionThreads() {
        int tmp = 0;
        for (int i = 0; i < allConnections.size();) {
            if (!allConnections.get(i).isActive()) {
                allConnections.remove(i);
            } else {
                i++;
                tmp++;
            }
        }
        return allConnections.size();
    }

    public ComputerType getType() {
        return type;
    }

    public void setType(ComputerType type) {
        this.type = type;
    }

    public void setId(int id) {
        this.id = id;
    }

//    public static void addTaskToRender(RenderTask task) {
//        synchronized (synchronizer) {
//            if (Information.getLocalComputer() != null) {
//                Information.getLocalComputer().getAllTasks().addLast(task);
//            }
//        }
//    }
//
//    public static void removeTaskToRender(RenderTask task) {
//        synchronized (synchronizer) {
//            if (Information.getLocalComputer() != null) {
//                Information.getLocalComputer().getAllTasks().remove(task);
//            }
//        }
//    }
    public CheckConnection getConnection() {
        return connection;
    }

    public void setConnection(CheckConnection connection) {
        this.connection = connection;
    }

    /**
     * Sets the allConnectinos to 'null' to be able to make the object
     * synchronizable
     */
    public void deleteConnectionList() {
        allConnections = null;
    }

}
