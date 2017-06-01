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
package objects;

import Graphic_board.Graphicboard;
import Server.CopyBlenderFileThread;
import Server.CopyImage;
import Server.CopyRenderTask;
import helpers.FileHelpers;
import helpers.Information;
import helpers.Paths;
import helpers.Storage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import javafx.stage.Window;

/**
 *
 * @author Kilian visit me on <aklio.de>
 */
public class RenderThread extends Thread {

    private Graphicboard board;
    private final Window window;
    private ThreadState threadState;

    private static final List<BlenderFile> transferring = new ArrayList<>();

    public enum ThreadState {
        renderingGPU,
        renderingCPU,
        waitingGPU,
        waitingCPU
    }

    public void waitForTransfer(RenderTask task) {
        boolean hasToWait = true;
        synchronized (transferring) {
            if (!new File(BlenderFile.getFilenameById(task.getFile().getId())).exists() && !transferring.contains(task.getFile())) {
                transferring.add(task.getFile());
                new CopyBlenderFileThread(Information.getLocalComputer().getServerIpAddress(), Information.getLocalComputer().getPort(),
                        task.getFile().getId()).requestBlenderFileTransfer();
                transferring.remove(task.getFile());
            } else {
                hasToWait = false;
            }
        }

        while (transferring.contains(task.getFile()) && hasToWait) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }

        System.out.println("finished waiting");

    }

    public RenderThread(Graphicboard board, Window window) {
        this.board = board;
        this.window = window;
    }

    public Graphicboard getBoard() {
        return board;
    }

    public void setBoard(Graphicboard board) {
        this.board = board;
    }

    @Override
    public void run() {

        while (!Information.isStopRendering()) {

            if (board == null) {
                if (!Storage.getSettings().isAllowCPU()) {
                    try {
                        threadState = ThreadState.waitingCPU;
                        Thread.sleep(1000);
                        continue;
                    } catch (InterruptedException ex) {
                    }
                }
            } else if (!board.isAllowed()) {
                try {
                    threadState = ThreadState.waitingGPU;
                    Thread.sleep(1000);
                    continue;
                } catch (InterruptedException ex) {
                }
            }

            if (board == null) {
                threadState = ThreadState.renderingCPU;
            } else {
                threadState = ThreadState.renderingGPU;
            }

            RenderTask task;

            if (Information.isClient()) {

                task = new CopyRenderTask(Information.getLocalComputer().getServerIpAddress(), Information.getLocalComputer().getPort()).getRenderTask();
                if (task == null) {
                    System.out.println("No new Tasks available, stopping thread " + this.getId());
                    break;
                }

                waitForTransfer(task);

            } else {
                synchronized (Information.getSynchronizer()) {
                    if (!Storage.getRenderTasks("reading is save").isEmpty()) {
                        task = Storage.removeFirstRenderTask();
                    } else {
                        System.out.println("Thread stopped (empty task List): " + this.getId());
                        break;
                    }
                }
            }

            if ((task.getFrame() > task.getFile().getEndFrame() || task.getFrame() < task.getFile().getStartFrame())) {
                System.out.println("Skipped Frame: " + task.getFrame());
                Information.increaseFramesRendered(1);
                continue;
            }

            if (!Storage.getFilesToRender().contains(task.getFile()) && !Information.isClient()) {
                System.out.println("File not in Files to Render (skipping): " + task.getFile().getPath().getName());
                Information.fixTaskList(task.getFile());
                continue;
            }

            String pythonContent = "";

            if (board == null) {
                pythonContent = "import bpy\n"
                        + "bpy.context.scene.cycles.device = \"CPU\"";
            } else {
                pythonContent = "import bpy\n"
                        + "bpy.context.user_preferences.system.compute_device_type = \"" + board.getType() + "\"\n"
                        + "bpy.context.scene.cycles.device = \"GPU\"\n"
                        + "bpy.context.user_preferences.system.compute_device = \"" + board.getSystemName() + "\"";
            }

            File pythonFile;

            String pathToRender;
            String blenderFilePath;
            String pythonPath;

            if (Information.isClient()) {
                pathToRender = Information.getCurrentDir() + File.separator + 
                        Paths.getWorkingDir() + File.separator + Information.getRandomString(6);
                blenderFilePath = Information.getCurrentDir() + File.separator + 
                        BlenderFile.getFilenameById(task.getFile().getId());
                pythonFile = new File(new File(blenderFilePath).getParent() + File.separator + new File(blenderFilePath).getName().split("\\.")[0] + task.getFrame() + ".py");
                pythonPath = Information.getCurrentDir() + File.separator + 
                        pythonFile.toString();
            } else {
                pathToRender = task.getFile().getPathToRender().toString();
                blenderFilePath = task.getFile().getPath().toString();
                pythonFile = new File(task.getFile().getPath().getParent() + File.separator + task.getFile().getPath().getName().split("\\.")[0] + task.getFrame() + ".py");
                pythonPath = pythonFile.toString();
            }

            FileHelpers.writeFile(pythonFile, pythonContent);

            ProcessBuilder pb = new ProcessBuilder(
                    Storage.getSettings().getPathToBlenderExe(window).toString(),
                    "-b",
                    blenderFilePath,
                    "-o",
                    pathToRender + "####",
                    "-F",
                    task.getFile().getFileFormat(),
                    "--python",
                    pythonPath,
                    "-f",
                    task.getFrame() + "");

            pb.redirectErrorStream(true);

            try {
                Process p = pb.start();

                BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String output;

                while ((output = bri.readLine()) != null) {
                    if (Thread.interrupted()) {
                        p.destroy();
                        p.waitFor();
                        System.out.println("Thread " + this.getId() + " was destroyed");
                        Storage.addFirstRenderTask(task); //TODO: hanle client shutdown
                        System.out.println(output);
                        return;
                    }
                }

                System.out.println("rendered task: " + task);
                Information.increaseFramesRendered(1);

            } catch (IOException | InterruptedException ex) {
                synchronized (Information.getSynchronizer()) {
                    if (!Information.isClient()) {
                        Storage.addFirstRenderTask(task);
                    }
                }
                ex.printStackTrace();
            } finally {
                pythonFile.delete();
            }

            System.out.println(pathToRender + String.format("%04d", task.getFrame()) + "." + task.getFile().getFileFormat().toLowerCase());
            
            if (Information.isClient()) {
                CopyImage cpImg = new CopyImage(Information.getLocalComputer().getServerIpAddress(), Information.getLocalComputer().getPort(),
                        new File(pathToRender + String.format("%04d", task.getFrame()) + "." + task.getFile().getFileFormat().toLowerCase()),
                        new File(task.getFile().getPathToRender().toString() + String.format("%04d", task.getFrame()) + "." + task.getFile().getFileFormat().toLowerCase())
                );
                cpImg.start();

            }

        }

        System.out.println("Thread " + this.getId() + " finished (stop signal given)");

    }

    public ThreadState getThreadState() {
        return threadState;
    }

}
