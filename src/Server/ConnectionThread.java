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

import Exceptions.TimeoutException;
import helpers.Actions;
import static helpers.Actions.closeStream;
import helpers.Information;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.net.ServerSocket;
import java.net.Socket;
import objects.SimpleString;

/**
 *
 * @author kilian
 */
public abstract class ConnectionThread extends Thread {

    protected Socket socket;
    protected InputStream sIn;
    protected OutputStream sOut;

    protected boolean active = true;

    String ipAddress = "";
    int port = 0;

    protected final boolean isServer;

    public ConnectionThread(String ip, int port) {
        this.ipAddress = ip;
        this.port = port;
        isServer = false;
    }

    public ConnectionThread(Socket s) throws IOException {

        try {
            this.socket = s;
            sIn = socket.getInputStream();
            sOut = socket.getOutputStream();
        } catch (IOException ex) {
            ex.printStackTrace();
            throw ex;
        }

        isServer = true;

    }

    public void close() {
        Actions.closeStream(socket);
        active = false;
//        if (!(this instanceof CheckConnection)) {
//            Computer.removeSocket(socket);
//        }
    }

    public void establishClientConnection(String messageToServer) throws IOException, ClassNotFoundException, TimeoutException {

        socket = new Socket(ipAddress, port);

        sIn = socket.getInputStream();
        sOut = socket.getOutputStream();

        readLine(); //Server sends line back if he accepted
        sendLine(Information.getLocalComputer().getId() + ""); //sending id
        sendLine(messageToServer); //send action to perform

        if (!(this instanceof CheckConnection)) {
            Information.getLocalComputer().addConnection(this);
        }

    }

    public void sendLine(String line) throws IOException {

        try {
            sendObject(new SimpleString(line));
        } catch (IOException ex) {
            ex.printStackTrace();
            throw ex;
        }

    }

    public String readLine() throws IOException, ClassNotFoundException, TimeoutException {

        try {
            return ((SimpleString) readObject()).toString();
        } catch (IOException ex) {
            ex.printStackTrace();
            throw ex;
        }

    }

    public void sendObject(Object obj) throws IOException {

        try {
            ObjectOutputStream oOut = new ObjectOutputStream(sOut);
            oOut.writeObject(obj);
            oOut.flush();

        } catch (IOException ex) {
            if (obj != null) {
                System.out.println(obj.getClass());
            }
            ex.printStackTrace();
            throw ex;
        }

    }

    public Object readObject() throws IOException, ClassNotFoundException, TimeoutException {
        int timeout = 0;
        while (true) {
            try {
                ObjectInputStream oInput = new ObjectInputStream(sIn);
                return oInput.readObject();
            } catch (StreamCorruptedException e) {
                try {
                    Thread.sleep(100);
                    System.err.println("Waiting for server...(" + timeout + " tries already)");
                    if (timeout >= 50) {
                        throw new TimeoutException("Error reading Object!");
                    }
                } catch (InterruptedException ex) {
                }
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
                throw ex;
            } catch (Exception exc) {
                throw exc;
            }
            timeout++;
        }

    }

    /**
     *
     * @param f
     * @throws IOException
     */
    public void sendFile(File f) throws IOException {

        try {
            byte[] buffer = new byte[4096];
            InputStream inputStream = new FileInputStream(f);
            int len = 0;

            while ((len = inputStream.read(buffer)) > 0) {
                sOut.write(buffer, 0, len);
                sOut.flush();
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            throw ex;
        } finally {
            closeStream(socket);
        }
    }

    public void readFile(File outputFile) throws IOException {

        try {

            OutputStream outputStream = new FileOutputStream(outputFile);
            byte[] buffer = new byte[4096];
            int len = 0;

            while ((len = sIn.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            throw ex;
        } finally {
            closeStream(socket);
        }
    }

    @Override
    public abstract void run();

    public boolean isActive() {
        return active;
    }

}
