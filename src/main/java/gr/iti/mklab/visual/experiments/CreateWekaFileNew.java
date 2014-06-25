package gr.iti.mklab.visual.experiments;

import eu.socialsensor.framework.client.dao.MediaItemDAO;
import eu.socialsensor.framework.client.dao.impl.MediaItemDAOImpl;
import eu.socialsensor.framework.client.search.visual.VisualIndexHandler;
import gr.iti.mklab.visual.aggregation.VladAggregatorMultipleVocabularies;
import gr.iti.mklab.visual.datastructures.AbstractSearchStructure;
import gr.iti.mklab.visual.datastructures.Linear;
import gr.iti.mklab.visual.dimreduction.PCA;
import gr.iti.mklab.visual.extraction.AbstractFeatureExtractor;
import gr.iti.mklab.visual.extraction.SURFExtractor;
import gr.iti.mklab.visual.vectorization.ImVecNew;
import gr.iti.mklab.visual.vectorization.ImageVectorization;
import gr.iti.mklab.visual.vectorization.ImageVectorizationResult;
import gr.iti.mklab.visual.vectorization.ImageVectorizationTrain;
import ij.gui.Roi;
import ij.io.RoiDecoder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kandreadou on 6/25/14.
 */
public class CreateWekaFileNew {

    public static int NUM_FONT_DESCRIPTORS = 0;
    public static int NUM_CONTENT_DESCRIPTORS = 0;

    protected static int maxNumPixels = 768 * 512;
    protected static int targetLengthMax = 1024;

    protected static void init() throws Exception {
        String learningFolder = "/home/kandreadou/webservice/learning_files/";


        int[] numCentroids = {128, 128, 128, 128};
        int initialLength = numCentroids.length * numCentroids[0] * AbstractFeatureExtractor.SURFLength;

        String[] codebookFiles = {
                learningFolder + "surf_l2_128c_0.csv",
                learningFolder + "surf_l2_128c_1.csv",
                learningFolder + "surf_l2_128c_2.csv",
                learningFolder + "surf_l2_128c_3.csv"
        };

        String pcaFile = learningFolder + "pca_surf_4x128_32768to1024.txt";

        //Initialize the ImageVectorization
        SURFExtractor extractor = new SURFExtractor();

        ImVecNew.setFeatureExtractor(extractor);
        ImVecNew.setVladAggregator(new VladAggregatorMultipleVocabularies(codebookFiles,
                numCentroids, AbstractFeatureExtractor.SURFLength));
        ImVecNew.loadClassifier();


        if (targetLengthMax < initialLength) {
            System.out.println("targetLengthMax : " + targetLengthMax + " initialLengh " + initialLength);
            PCA pca = new PCA(targetLengthMax, 1, initialLength, true);
            pca.loadPCAFromFile(pcaFile);
            ImVecNew.setPcaProjector(pca);
        }
    }


    private static double[] getVector(String imageFolder, String imageFilename, Roi[] rois, BufferedWriter bw) throws Exception {

        ImVecNew imvec = new ImVecNew(imageFolder, imageFilename, targetLengthMax, maxNumPixels, rois, bw);

        ImageVectorizationResult imvr = imvec.call();
        double[] vector = imvr.getImageVector();

        return vector;
    }

    public static void main(String[] args) throws Exception {
        init();
        String imageFolder = "/home/kandreadou/Desktop/classifier_training/";
        String roiFolder = "/home/kandreadou/Desktop/classifier_training/rois/";
        File arffFile = new File("/home/kandreadou/Desktop/classifier_training/descriptors.arff");
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
