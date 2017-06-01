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

import static Graphic_board.BoardFinder.getGraphicBoards;
import Graphic_board.Graphicboard;
import helpers.Actions;
import helpers.Information;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 *
 * @author kilian
 */
public class Settings implements Serializable {

    private Locale locale;
    private long blenderFileId;
    private int maxInstancesPerDevice;

    private File pathToBlenderExe;
    private File pathToWorkingDirectory;
    private File pathToNetFile;

    private int refreshMillis = 500;

    boolean allowCPU;
    List<Graphicboard> gpus;

    int sliderState;

    boolean showTCP = false;

    public Settings() {
        allowCPU = true;
        gpus = new ArrayList<>();
        sliderState = Information.getMaxCpuCernels();
        gpus = getGraphicBoards();
        maxInstancesPerDevice = 2;
        locale = Locale.getDefault();
    }

    public File getPathToBlenderExe(Window window) {

        if ((pathToBlenderExe == null) || !pathToBlenderExe.exists()) {

            setPathToBlenderExe(window);

        }

        return pathToBlenderExe;
    }

    public void setPathToBlenderExe(Window window) {

        Actions.showAlert("No Blender File specified", "The blender.exe file was not found", "Please specify a valid blender.exe");

        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Blender Executable", "blender.exe*"),
                new FileChooser.ExtensionFilter("All Executable Files", "*.exe*"),
                new FileChooser.ExtensionFilter("All Files", "*")
        );

        fc.setTitle("Select the 'blender.exe' on your System");
//        fc.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("Blender Files", ".blend"));

        File file = fc.showOpenDialog(window);

        if (file != null) {
            pathToBlenderExe = file;
            System.out.println(pathToBlenderExe.toString());

        } else {
            Actions.showAlert("Error", "This program is not able to work without a blender executable (shutting down)", "Please specify a valid blender.exe (restart the program)");
            System.exit(0);
        }

    }

    public long getNextBlenderFileId() {
        blenderFileId++;
        return blenderFileId;
    }

    //<editor-fold defaultstate="collapsed" desc="getters and setters">
    public File getPathToWorkingDirectory() {
        return pathToWorkingDirectory;
    }

    public void setPathToWorkingDirectory(File pathToWorkingDirectory) {
        this.pathToWorkingDirectory = pathToWorkingDirectory;
    }

    public File getPathToNetFile() {
        return pathToNetFile;
    }

    public void setPathToNetFile(File pathToNetFile) {
        this.pathToNetFile = pathToNetFile;
    }

    public boolean isAllowCPU() {
        return allowCPU;
    }

    public void setAllowCPU(boolean allowCPU) {
        this.allowCPU = allowCPU;
    }

    public int getSliderState() {
        return sliderState;
    }

    public void setSliderState(int sliderState) {
        this.sliderState = sliderState;
    }

    public List<Graphicboard> getGpus() {
        return gpus;
    }

    public void setGpus(List<Graphicboard> gpus) {
        this.gpus = gpus;
    }

    public int getMaxInstancesPerDevice() {
        return maxInstancesPerDevice;
    }

    public void setMaxInstancesPerDevice(int maxInstancesPerDevice) {
        this.maxInstancesPerDevice = maxInstancesPerDevice;
    }

    public int getRefreshMillis() {
        return refreshMillis;
    }

    public void setRefreshMillis(int refreshMillis) {
        this.refreshMillis = refreshMillis;
    }

    public boolean isShowTCP() {
        return showTCP;
    }

    public void setShowTCP(boolean showTCP) {
        this.showTCP = showTCP;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * *
     * Looks up, if at least one GPU is selected
     *
     * @return true if one or more GPUs are selected
     */
    public boolean gpuAllowed() {

        for (Graphicboard g : gpus) {
            if (g.isAllowed()) {
                return true;
            }
        }
        return false;
    }
//</editor-fold>
}
