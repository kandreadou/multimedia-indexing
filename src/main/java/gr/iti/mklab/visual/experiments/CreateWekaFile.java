package gr.iti.mklab.visual.experiments;

import gr.iti.mklab.visual.experiments.AbstractTest;
import gr.iti.mklab.visual.vectorization.ImageVectorizationResult;
import gr.iti.mklab.visual.vectorization.ImageVectorizationTrain;
import ij.gui.Roi;
import ij.io.RoiDecoder;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kandreadou on 3/6/14.
 */
public class CreateWekaFile extends AbstractTest {

    public static int NUM_FONT_DESCRIPTORS = 0;
    public static int NUM_CONTENT_DESCRIPTORS = 0;

    private static double[] getVector(String imageFolder, String imageFilename, Roi[] rois, BufferedWriter bw) throws Exception {

        ImageVectorizationTrain imvec = new ImageVectorizationTrain(imageFolder, imageFilename, targetLengthMax, maxNumPixels, rois, bw);

        ImageVectorizationResult imvr = imvec.call();
        double[] vector = imvr.getImageVector();

        return vector;
    }

    public static void main(String[] args) throws Exception {
        init(true);
        String imageFolder = "/home/kandreadou/Desktop/classifier_training/";
        String roiFolder = "/home/kandreadou/Desktop/classifier_training/rois/";
        File arffFile = new File("/home/kandreadou/Desktop/classifier_training/descriptorsJunk.arff");
        FileWriter fw = new FileWriter(arffFile.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write("@relation banners");
        bw.newLine();
        bw.newLine();
        for (int i = 0; i < 64; i++) {
            bw.write("@attribute surf" + i + " real");
            bw.newLine();
        }
        bw.write("@attribute class {'OUT','IN'}");
        bw.newLine();
        bw.newLine();
        bw.write("@data");
        bw.newLine();

        for (File file : new File(imageFolder).listFiles()) {
            if (file.isFile() && !file.getName().endsWith(".arff")) {

                String imageName = file.getName();
                //System.out.println("Starting for image " + imageName);
                List<Roi> roiArray = new ArrayList<Roi>();

                for (File roiFile : new File(roiFolder).listFiles()) {
                    //System.out.println(roiFile.getName() + " " + imageName);
                    if (roiFile.getName().startsWith(imageName)) {
                        //extract rois for image
                        //System.out.println("Adding roi " + roiFile.getName());
                        RoiDecoder decoder = new RoiDecoder(roiFile.getPath());
                        roiArray.add(decoder.getRoi());
                    }
                }

                // Prints all rois
                /*for (Roi rois : roiArray) {
                    Rectangle rec = rois.getBounds();
                    System.out.println("Rois for "+imageName+" x " + rec.x + " y " + rec.y + " width" + rec.width + " height " + rec.height);
                }*/

                double[] vector = getVector(imageFolder, file.getName(), roiArray.toArray(new Roi[roiArray.size()]), bw);

            }
        }

        bw.close();
        System.out.println("Num content descriptors: " + NUM_CONTENT_DESCRIPTORS);
        System.out.println("Num font descriptors: " + NUM_FONT_DESCRIPTORS);

    }
}
