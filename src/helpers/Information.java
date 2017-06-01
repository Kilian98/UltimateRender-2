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
package helpers;

import FXML.MainFormController;
import FXMLContainer.Container_InformationController;
import FXMLContainer.Container_TCPController;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import objects.BlenderFile;
import Server.Computer;
import Server.CopyInformation;
import objects.InformationThread;
import objects.RenderTask;
import objects.RenderThread;

/**
 *
 * @author Kilian Brenner visit me on <aklio.de>
 */
public class Information {

    public static String Version = "2.0";

    static private Container_InformationController infoController;
    static private MainFormController mainController;
    private static InformationThread infoThread;
    private static int computerID = 1; //for getting the next computerId only (globalComputer is always id:0)
    private static boolean tasksInUse;

    public enum Status {
        Rendering,
        Pausing,
        Stopped,
        Stopping
    }

    static private boolean stopRendering = false;
    static private RenderThread[] threads;
    private static final Object synchronizer = new Object();

    static private Computer localComputer = new Computer(Computer.ComputerType.standalone);
    static private Computer globalComputer = new Computer(Computer.ComputerType.global); //same object for every server & client

    //variables for the Renderfarm
    static private boolean stopServer_or_Client = false;
    private static final List<Computer> CONNECTED_COMPUTERS = new ArrayList<>();
    static private ServerSocket sSocket = null; //for stoping the server, close the server socket
    static private Container_TCPController TCPController;
    private static final List<String> RANDOM_STRINGS = new ArrayList<>();
    static private String serverState = "OFF";

    //status information
    static Status status = Status.Stopped;
    static long timeRendering;

    /**
     * *
     * You may check, if there are threads running in the background e.g. before
     * closing the program
     *
     * @return boolean if there are still background tasks running
     */
    public static boolean threadsRunning() {
        if (threads == null) {
            return false;
        }
        for (Thread t : threads) {
            if (t != null && t.isAlive()) {
                return true;
            }
        }
        return false;
    }

    public static String getRandomString(int count) {

        Random random = new Random();
        String alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String rnd;

        do {
            rnd = "";

            for (int i = 0; i < count; i++) {
                rnd += alpha.charAt(random.getRandom(0, 25));
            }

        } while (RANDOM_STRINGS.contains(rnd));

        RANDOM_STRINGS.add(rnd);

        return rnd;

    }

    public static int getMaxCpuCernels() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static boolean isStopRendering() {
        return stopRendering;
    }

    public static void setStopRendering(boolean stopRendering) {
        Information.stopRendering = stopRendering;
    }

    public static void setThreads(RenderThread[] thread) {
        Information.threads = thread;
    }

    public static void abortRendering() {

        if (threads == null || threads[0] == null) {
            return;
        }

        stopRendering = true;

        for (Thread p : threads) {
            p.interrupt();
        }

        for (Thread p : threads) {
            try {
                p.join();
            } catch (InterruptedException ex) {
            }
        }

        System.out.println("stopped all threads");

    }

    public static void updateInformation() {

        localComputer.setRenderingThreads(0);
        localComputer.setWaitingThreads(0);

        if (!threadsRunning()) {
            status = Status.Stopped;
            localComputer.setRenderingThreads(0);
            localComputer.setWaitingThreads(0);
            mainController.setControlButtonsDisable(false);
        } else {
            for (RenderThread t : threads) {
                if (t != null && (t.getThreadState() == RenderThread.ThreadState.renderingCPU || t.getThreadState() == RenderThread.ThreadState.renderingGPU) && t.isAlive()) {
                    localComputer.setRenderingThreads(localComputer.getRenderingThreads() + 1);
                } else if (t != null && t.isAlive()) {
                    localComputer.setWaitingThreads(localComputer.getWaitingThreads() + 1);
                }
            }
        }

        globalComputer.setRenderingThreads(localComputer.getRenderingThreads());

        if (!isClient()) { //only server

            for (Computer c : CONNECTED_COMPUTERS) {
                globalComputer.setFramesRendered(globalComputer.getFramesRendered() + c.getFramesRendered(true));
                globalComputer.setRenderingThreads(globalComputer.getRenderingThreads() + c.getRenderingThreads());
            }
            globalComputer.setFramesRendered(globalComputer.getFramesRendered() + localComputer.getFramesRendered(true));
            globalComputer.setFramesRemaining(Storage.getQueue().getTasks().size());
            globalComputer.setComputersConnected(getComputersConnected());
        }

        if (isClient()) { //only client
            CopyInformation cpInfo;
            cpInfo = new CopyInformation(localComputer.getServerIpAddress(), localComputer.getPort(), localComputer);

            cpInfo.getInformationComp();

        }

    }

    public static int getRenderedFrames() {
        return globalComputer.getFramesRendered();
    }

    public static int getExtRenderingThreads() {
        return globalComputer.getRenderingThreads() - localComputer.getRenderingThreads();
    }

    /**
     * *
     * If a Task with a file not contained in the to-Render-List, this method
     * should be executed, to delete all the tasks associated with this file
     *
     * @param file the file that is not in the to-Render-List anymore
     */
    public static void fixTaskList(BlenderFile file) {

        synchronized (synchronizer) {

            Information.tasksInUse = true;
            LinkedList<RenderTask> tasks = Storage.getRenderTasks("save");

            for (int i = 0; i < tasks.size(); i++) {
                if (tasks.get(i).getFile() == file) {
                    System.out.println("Removed task: " + tasks.get(i));
                    tasks.remove(i);
                    i--;
                }
            }
            Information.tasksInUse = false;
        }

    }

    public static Computer getComputer(long id) {
        for (Computer c : CONNECTED_COMPUTERS) {
            if (c.getId() == id) {
                return c;
            }
        }
        throw new NullPointerException("unknown");
    }

    public static int getComputersConnected() {
        if (!isClient()) {
            return CONNECTED_COMPUTERS.size();
        } else {
            return globalComputer.getComputersConnected();
        }
    }

    public static boolean isClient() {
        return localComputer.getType() == Computer.ComputerType.client;
//        return localComputer.isIsConnected() && localComputer.getType() == Computer.ComputerType.client;
    }

    public static boolean isStopServer_or_Client() {
        return stopServer_or_Client;
    }

    public static void setStopServer_or_Client(boolean stopServer_or_Client) {
        Information.stopServer_or_Client = stopServer_or_Client;
    }

    public static String getCurrentDir() {
        java.nio.file.Path currentRelativePath = java.nio.file.Paths.get("");
        String s = currentRelativePath.toAbsolutePath().toString();
        System.out.println(s);
        return s;
    }

    //<editor-fold defaultstate="collapsed" desc="Synchronized methods">
    public static void addComputerToList(Computer c) {
        synchronized (synchronizer) {
            CONNECTED_COMPUTERS.add(c);
        }
    }

    public static void removeComputerFromList(Computer c) {
        synchronized (synchronizer) {
            CONNECTED_COMPUTERS.remove(c);
        }
    }

    public static void increaseFramesRendered(int i) {
        synchronized (synchronizer) {
            localComputer.setFramesRendered(localComputer.getFramesRendered() + 1);
            System.out.println("increased frames rendered");
        }
    }

//</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Simple getters and setters">
    public static Object getSynchronizer() {
        return synchronizer;
    }

    public static RenderThread[] getThreads() {
        return threads;
    }

    public static int getRenderingThreads() {
        return globalComputer.getRenderingThreads();
    }

    public static int getWaitingThreads() {
        return globalComputer.getWaitingThreads();
    }

    public static int getServerThreads() {

        int tmp = 0;
        for (Computer c : CONNECTED_COMPUTERS) {
            tmp += c.getConnectionThreads();
        }
        tmp += localComputer.getConnectionThreads();

        return tmp;
    }

    public static int getFramesRendered() {
        return globalComputer.getFramesRendered();
    }

    public static int getFramesRemaining() {
        return globalComputer.getFramesRemaining();
    }

    public static long getTimeRendering() {
        return timeRendering;
    }

    public static void setInfoController(Container_InformationController infoController) {
        Information.infoController = infoController;
    }

    public static void setTimeRendering(long timeRendering) {
        Information.timeRendering = timeRendering;
    }

    public static Container_InformationController getInfoController() {
        return infoController;
    }

    public static Status getStatus() {
        return status;
    }

    public static void setStatus(Status status) {
        Information.status = status;
    }

    /**
     * Clears only the set-up numbers, this method does not recalculate the
     * frames to render
     */
    public static void clearFramesRendered() {
        globalComputer.setFramesRendered(0);
        Storage.getQueue().setFramesRendered(0);
    }

    public static Computer getLocalComputer() {
        return localComputer;
    }

    public static void setLocalComputer(Computer localComputer) {
        Information.localComputer = localComputer;
    }

    public static boolean isStopServer() {
        return stopServer_or_Client;
    }

    public static void setStopServer(boolean stopServer) {
        Information.stopServer_or_Client = stopServer;
    }

    public static ServerSocket getsSocket() {
        return sSocket;
    }

    public static void setsSocket(ServerSocket sSocket) {
        Information.sSocket = sSocket;
    }

    public static int getNextComputerId() {
        return computerID++;
    }

    public static List<Computer> getConnectedComputers() {
        return CONNECTED_COMPUTERS;
    }

    public static Container_TCPController getTCPController() {
        return TCPController;
    }

    public static void setTCPController(Container_TCPController tcpController) {
        Information.TCPController = tcpController;
    }

    public static String getServerState() {
        return serverState;
    }

    public static void setServerState(String serverState) {
        Information.serverState = serverState;
    }

    public static void setInfoThread(InformationThread infoThread) {
        Information.infoThread = infoThread;
    }

    public static InformationThread getInfoThread() {
        return infoThread;
    }

    public static Computer getGlobalComputer() {
        return globalComputer;
    }

    public static MainFormController getMainController() {
        return mainController;
    }

    public static void setMainController(MainFormController mainController) {
        Information.mainController = mainController;
    }
//</editor-fold>

    public static void setGlobalComputer(Computer globalComputer) {
        Information.globalComputer = globalComputer;
    }

}
