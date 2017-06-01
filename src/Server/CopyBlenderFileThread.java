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
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import objects.BlenderFile;

/**
 *
 * @author kilian
 */
public class CopyBlenderFileThread extends ConnectionThread {

    File fileToTransfer = null; //only set for the Server
    private Object blenderFile;
    long blenderFileId;

    public CopyBlenderFileThread(String ipAddress, int port, long fileID) {
        super(ipAddress, port);
        blenderFileId = fileID;
    }

    public CopyBlenderFileThread(Socket s) throws IOException {
        super(s);
    }

    public void requestBlenderFileTransfer() {

        try {
            establishClientConnection(Constants.requestBlenderFiles);
            sendLine(blenderFileId + "");
            readFile(new File(BlenderFile.getFilenameById(blenderFileId)));
        } catch (IOException | ClassNotFoundException | TimeoutException ex) {
            ex.printStackTrace();
        }
        System.out.println("transmitted file to: " + BlenderFile.getFilenameById(blenderFileId));
        close();

    }

    @Override
    public void run() {

        if (isServer) {
            try {
                blenderFileId = Long.parseLong(readLine());
                for (BlenderFile f : Storage.getFilesToRender()) {
                    if (f.getId() == blenderFileId) {
                        blenderFile = f;
                        fileToTransfer = f.getPath();
                        System.out.println(blenderFile);
                        break;
                    }
                }

                sendFile(fileToTransfer);

            } catch (IOException | ClassNotFoundException | TimeoutException ex) {
            }finally{
                close();
            }
        }
    }

}
