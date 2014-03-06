package gr.iti.mklab.visual.examples;

import gr.iti.mklab.visual.vectorization.ImageVectorization;
import gr.iti.mklab.visual.vectorization.ImageVectorizationResult;
import ij.gui.Roi;
import ij.io.RoiDecoder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kandreadou on 3/6/14.
 */
public class CreateWekaFile extends AbstractTest {

    private static double[] getVector(String imageFolder, String imageFilename, Roi[] rois, BufferedWriter bw) throws Exception {

        ImageVectorization imvec = new ImageVectorization(imageFolder, imageFilename, targetLengthMax, maxNumPixels);

        ImageVectorizationResult imvr = imvec.call();
        double[] vector = imvr.getImageVector();

        return vector;
    }

    public static void main(String[] args) throws Exception {
        init();
        String imageFolder = "/home/kandreadou/Desktop/trainingset/";
        String roiFolder = "/home/kandreadou/Desktop/trainingset/rois/";
        File arffFile = new File("/home/kandreadou/Desktop/trainingset/descriptors.arff");
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
                System.out.println("Starting for image " + imageName);
                List<Roi> roiArray = new ArrayList<Roi>();

                for (File roiFile : new File(roiFolder).listFiles()) {
                    String imageNameWithoutSuffix = imageName.substring(0, imageName.indexOf('.'));
                    System.out.println(roiFile.getName() + " " + imageNameWithoutSuffix);
                    if (roiFile.getName().startsWith(imageNameWithoutSuffix)) {
                        //extract rois for image
                        System.out.println("Adding roi " + roiFile.getName());
                        RoiDecoder decoder = new RoiDecoder(roiFile.getPath());
                        roiArray.add(decoder.getRoi());
                    }
                }
                double[] vector = getVector(imageFolder, file.getName(), roiArray.toArray(new Roi[roiArray.size()]), bw);

            }
        }

        bw.close();

    }
}
