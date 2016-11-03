package com.arvelm.cameraview;
import android.os.Environment;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import de.mindpipe.android.logging.log4j.LogConfigurator;


import java.io.File;

/**
 * Created by lidh on 16-11-2.
 */

public class Logging {
    private static final File PICTURE_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
    private static final String FILENAME = "CameraView.log";

    public static Logger configureLogger(Class clazz){
        final LogConfigurator logConfigurator = new LogConfigurator();
        logConfigurator.setFileName(PICTURE_PATH + File.separator + FILENAME);
        logConfigurator.setRootLevel(Level.DEBUG);
        logConfigurator.setLevel("org.apache",Level.ERROR);
        logConfigurator.setUseFileAppender(true);
//        logConfigurator.setFilePattern("%d{ISO8601} %m%n");
        logConfigurator.setFilePattern("  %m%n");
//        logConfigurator.setImmediateFlush(true);
        logConfigurator.configure();

        Logger log = Logger.getLogger(clazz);
        return log;

    }

}
