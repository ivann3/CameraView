package com.arvelm.cameraview;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by lidh on 16-11-3.
 */

public class ImgSensorInfo {

    private static final String MAIN_CAMERA = "/sys/bus/platform/drivers/factoryapi/mcamera";

    private static final String SUB_CAMERA = "/sys/bus/platform/drivers/factoryapi/scamera";

    public static String[] getImgSensorInfo(){
        String[] pathStr = {MAIN_CAMERA,SUB_CAMERA};
        for(int i=0; i<2;i++){
            if (!new File(pathStr[i]).exists()){
                pathStr[i] = "unknow";
            }else{
                try {
                    FileReader fileReader = new FileReader(pathStr[i]);
                    BufferedReader bufferedReader = new BufferedReader(fileReader);
                    String readStr = bufferedReader.readLine();
                    if (readStr != null){
                        pathStr[i] = readStr;
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return pathStr;
    }
}
