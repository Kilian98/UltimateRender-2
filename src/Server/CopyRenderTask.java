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
import helpers.Constants;
import helpers.Storage;
import java.io.IOException;
import java.net.Socket;
import objects.RenderTask;

/**
 *
 * @author kilian
 */
public class CopyRenderTask extends ConnectionThread {

    RenderTask task;
    boolean transfered = false;

    public CopyRenderTask(Socket s) throws IOException {
        super(s);
    }

    public CopyRenderTask(String ip, int port) {
        super(ip, port);
    }

    /**
     * Gets a Render Task without starting a new Thread. (starts the run Method)
     *
     * @return A new RenderTask, transmitted from the server
     */
    public RenderTask getRenderTask() {

        try {
            establishClientConnection(Constants.requestNewJob);
            System.out.println("getting task...");
            return (RenderTask) readObject();

        } catch (IOException | ClassNotFoundException | TimeoutException ex) {
        }finally{
            close();
        }

        return null;

    }

    @Override
    public void run() {

        if (isServer) {
            if (!Storage.getRenderTasks("is save").isEmpty()) {
                try {
                    System.out.println("Sending task...");
                    sendObject(Storage.removeFirstRenderTask());
                } catch (IOException ex) {
                }
            } else {
                try {
                    sendObject(null);
                } catch (IOException ex) {
                }
            }
        }

        close();

//        if (!ipAddress.equals("")) {    //Part foir Client Connection
//
//            try {
//                establishClientConnction(Constants.requestNewJob);
//
//                task = (RenderTask) readObject();
//
//                transfered = true;
//
//            } catch (NetworkException ex) {
//            }
//
//        } else {
//
//            synchronized (Information.getSynchronizer()) {
//                if (!Storage.getRenderTasks().isEmpty()) {
//                    try {
//                        sendObject(Storage.getRenderTasks().removeFirst());
//                    } catch (NetworkException ex) {
//                    }
//                } else {
//                    try {
//                        sendObject(null);
//                    } catch (NetworkException ex) {
//                    }
//                }
//            }
//
//        }
//
//        close();
    }

}
