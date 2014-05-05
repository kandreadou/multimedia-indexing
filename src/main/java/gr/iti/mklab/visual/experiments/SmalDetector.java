package gr.iti.mklab.visual.experiments;

import eu.socialsensor.framework.client.dao.impl.MediaItemDAOImpl;
import eu.socialsensor.framework.client.search.visual.VisualIndexHandler;
import gr.iti.mklab.visual.aggregation.VladAggregatorMultipleVocabularies;
import gr.iti.mklab.visual.datastructures.Linear;
import gr.iti.mklab.visual.dimreduction.PCA;
import gr.iti.mklab.visual.extraction.AbstractFeatureExtractor;
import gr.iti.mklab.visual.extraction.SURFExtractor;
import gr.iti.mklab.visual.vectorization.ImageVectorization;
import gr.iti.mklab.visual.vectorization.ImageVectorizationResult;
import gr.iti.mklab.visual.vectorization.ImageVectorizationTrain;
import smal.smalDetector;

import com.mathworks.extern.java.MWStructArray;
import com.mathworks.toolbox.javabuilder.MWArray;
import com.mathworks.toolbox.javabuilder.MWException;
import com.mathworks.toolbox.javabuilder.MWNumericArray;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by kandreadou on 4/24/14.
 */
public class SmalDetector {

    protected static int maxNumPixels = 768 * 512;
    protected static int targetLengthMax = 1024;
    static smalDetector smal = null;
    static Object[] classificationResult = null;
    static Object[] accuracyResult = null;

    static MWStructArray Classifierparams = null;
    static MWStructArray Accuracyparams = null;

    static double[][] mAP = null;
    static double[][] AP = null;

    //scene categories home
    private final static String schome = "/home/kandreadou/Pictures/scene_categories/";
    private final static String idFileName = "TrainTestImageIDs.txt";
    private final static String labelsFileName = "TrainTestLabels.txt";
    private final static String vectorsFileName = "TrainTestVectors.txt";

    //nuswide home
    private final static String nuswidehome = "/home/kandreadou/Pictures/NUSIMAGES/NUSWIDE/";
    private final static String nusTestImages = "/home/kandreadou/Pictures/nuswide/TestImagesList.txt";
    private final static String nusTestLabels = "/home/kandreadou/Pictures/nuswide/TestLabels.txt";
    private final static String nusTrainImages = "/home/kandreadou/Pictures/nuswide/TrainImageList.txt";
    private final static String nusTrainLabels = "/home/kandreadou/Pictures/nuswide/TrainLabels.txt";


    public SmalDetector(double[][] mAP, double[][] AP) {
        this.mAP = mAP;
        this.AP = AP;
    }


    public static SmalDetector ComputeSmalDetector(double[][] vtrain, double[][] vtest, double[][] trainLabels, double[][] testLabels) {

        try {

            smal = new smalDetector();
            System.out.println("smalDetector created");
            String[] ParamsStructFields = {"vtrain", "vtest", "trainLabels", "method"};
            Classifierparams = new MWStructArray(1, 1, ParamsStructFields);

            Classifierparams.set("vtrain", 1, vtrain);
            Classifierparams.set("vtest", 1, vtest);
            Classifierparams.set("trainLabels", 1, trainLabels);
            Classifierparams.set("method", 1, "1"); // 1 for linear 2 for "smooth" instead of linear, for the smooth function
            /**
             * optional parameters
             */
            //			Classifierparams.set("NUMEVECS",1,500); // number of total eigenvectors, default 500
            //			Classifierparams.set("SIGMA", 1, 0.2); // controls affinity in graph Laplacian, default 0.2
            //			Classifierparams.set("parameter", 1, "5"); // c: the SVM trade-off parameter, default 5


            classificationResult = smal.smal(1, Classifierparams);

            MWNumericArray MWscore = (MWNumericArray) classificationResult[0];

            double[][] score = (double[][]) MWscore.toArray();


            //String[] ParamsStructFieldsAccuracy = {"score", "testLabels"};
            //Accuracyparams = new MWStructArray(1, 1, ParamsStructFieldsAccuracy);
            //Accuracyparams.set("score", 1, score);
            //Accuracyparams.set("testLabels", 1, testLabels);


            accuracyResult = smal.metrics(2, score, testLabels);

			/*
             * the prediction scores for test set and the predicted concept
			 */

            MWNumericArray meanaveragePrecision = (MWNumericArray) accuracyResult[0];
            MWNumericArray averagePrecision = (MWNumericArray) accuracyResult[1];

            // convert to int to use it in java functions
            mAP = (double[][]) meanaveragePrecision.toArray();
            AP = (double[][]) averagePrecision.toArray();

        } catch (MWException e) {
            System.out.println("before calculating " + e);
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception ge) {
            System.out.println("ex " + ge);
        } finally {
            // free native resources
            if (smal != null) {
                smal.dispose();
            }
            MWArray.disposeArray(Classifierparams);
            MWArray.disposeArray(accuracyResult);
            MWArray.disposeArray(Accuracyparams);
            MWArray.disposeArray(classificationResult);

        }

        return new SmalDetector(mAP, AP);
    }

    public static void main(String[] args) throws Exception {
        //createFileWithVectors();
        nuswide();
    }

    public static void nuswide() throws Exception {

        init();
        //int limitForTesting = 0;
        //Create the train vectors
        List<double[]> trainVectors = new ArrayList<double[]>();
        BufferedReader reader = new BufferedReader(new FileReader(new File(nusTrainImages)));
        String line = reader.readLine();
        //while (line != null && limitForTesting<100) {
        while (line != null) {
            //limitForTesting++;
            //TODO: why line.trim() in this case does not have the same result is a good question
            line = line.substring(0, line.lastIndexOf('g') + 1);
            System.out.println("home: " + nuswidehome + " file name " + line + "##");
            trainVectors.add(getVector(nuswidehome, line));
            System.out.println("after");
            line = reader.readLine();
        }
        reader.close();
        //limitForTesting = 0;
        double[][] vtrain = new double[trainVectors.size()][1024];
        trainVectors.toArray(vtrain);

        //Create the train labels
        double[][] trainLabels = null;
        reader = new BufferedReader(new FileReader(new File(nusTrainLabels)));
        line = reader.readLine();
        int i = 0, k = 0;
        //while (line != null && limitForTesting<100) {
        while (line != null) {
            //limitForTesting++;
            String[] ints = line.trim().split("\\s+");
            if (trainLabels == null) {
                trainLabels = new double[trainVectors.size()][ints.length];
            }
            k = 0;
            for (String integer : ints) {
                trainLabels[i][k] = Double.parseDouble(integer);
                k++;
            }
            i++;
            line = reader.readLine();
        }
        reader.close();
        //limitForTesting = 0;

        //Create the test vectors
        List<double[]> testVectors = new ArrayList<double[]>();
        reader = new BufferedReader(new FileReader(new File(nusTestImages)));
        line = reader.readLine();
        //while (line != null && limitForTesting<100) {
        while (line != null) {
            //limitForTesting++;
            line = line.substring(0, line.lastIndexOf('g') + 1);
            testVectors.add(getVector(nuswidehome, line));
            line = reader.readLine();
        }
        reader.close();
        //limitForTesting = 0;

        double[][] vtest = new double[testVectors.size()][1024];
        testVectors.toArray(vtest);

        //Create the test labels
        double[][] testLabels = null;
        reader = new BufferedReader(new FileReader(new File(nusTestLabels)));
        line = reader.readLine();
        i = 0;
        k = 0;
        //while (line != null && limitForTesting<100) {
        while (line != null) {
            //limitForTesting++;
            String[] ints = line.trim().split("\\s+");
            if (testLabels == null) {
                testLabels = new double[testVectors.size()][ints.length];
            }
            k = 0;
            for (String integer : ints) {
                testLabels[i][k] = Double.parseDouble(integer);
                k++;
            }
            i++;
            line = reader.readLine();
        }
        reader.close();


        System.out.println("before calculating");
        SmalDetector detector = ComputeSmalDetector(vtrain, vtest, trainLabels, testLabels);

        for (int m = 0; m < mAP.length; m++) {
            for (int n = 0; n < mAP[0].length; n++) {
                System.out.println("mAP " + mAP[m][n]);
            }
        }
        for (int m = 0; m < AP.length; m++) {
            for (int n = 0; n < AP[0].length; n++) {
                System.out.println("AP " + AP[m][n]);
            }
        }
    }


    public static void sceneCategories() throws Exception {
        //Read the ID file
        init();
        List<String> IDs = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new FileReader(new File(schome + idFileName)));
        String line = reader.readLine();
        while (line != null) {
            IDs.add(line);
            line = reader.readLine();
        }
        reader.close();

        //Read the labels file
        double[][] labels = new double[IDs.size()][11];
        reader = new BufferedReader(new FileReader(new File(schome + labelsFileName)));
        line = reader.readLine();
        int i = 0, k = 0;
        while (line != null) {
            String[] ints = line.trim().split("\\s+");
            k = 0;
            for (String integer : ints) {
                labels[i][k] = Double.parseDouble(integer);
                k++;
            }
            i++;
            line = reader.readLine();
        }
        reader.close();

        int trainSize = (IDs.size() / 2) + 1;
        int testSize = IDs.size() - trainSize;

        //Read the vectors file
        //reader = new BufferedReader(new FileReader(new File(schome + vectorsFileName)));

        double[][] vtrain = new double[trainSize][1024];
        double[][] vtest = new double[testSize][1024];
        double[][] trainLabels = new double[trainSize][11];
        double[][] testLabels = new double[testSize][11];
        int trainCount = 0;
        int testCount = 0;

        for (i = 0; i < IDs.size(); i++) {
            //line = reader.readLine();
            if (i % 2 == 0) {
                System.out.println("i = " + i + " trainCount " + trainCount);
                vtrain[trainCount] = getVector(schome, IDs.get(i) + ".jpg");  //Arrays.copyOf(sampleVector, 1024);//
                //vtrain[trainCount] = convertStringToDoubleArray(line);
                trainLabels[trainCount] = labels[i];
                trainCount++;
            } else {
                System.out.println("i = " + i + " testCount " + testCount);
                vtest[testCount] = getVector(schome, IDs.get(i) + ".jpg");  //Arrays.copyOf(sampleVector, 1024); //
                //vtest[testCount] = convertStringToDoubleArray(line);
                testLabels[testCount] = labels[i];
                testCount++;
            }
        }
        //reader.close();
        System.out.println("before calculating");
        SmalDetector detector = ComputeSmalDetector(vtrain, vtest, trainLabels, testLabels);

        for (int m = 0; m < mAP.length; m++) {
            for (int n = 0; n < mAP[0].length; n++) {
                System.out.println("mAP " + mAP[m][n]);
            }
        }
        for (int m = 0; m < AP.length; m++) {
            for (int n = 0; n < AP[0].length; n++) {
                System.out.println("AP " + AP[m][n]);
            }
        }
    }

    private static double[] convertStringToDoubleArray(String str) {

        String[] items = str.split(" ");
        double[] vector = new double[items.length];
        int i = 0;
        for (String item : items) {
            vector[i] = Double.parseDouble(item);
            i++;
        }
        return vector;
    }

    private static void createFileWithVectors() throws Exception {
        init();
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(schome + vectorsFileName)));

        //Read the ID file
        List<String> IDs = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new FileReader(new File(schome + idFileName)));
        String line = reader.readLine();
        while (line != null) {
            double[] vector = getVector(schome, line + ".jpg");
            for (double item : vector) {
                writer.write(item + " ");
            }
            writer.newLine();
            line = reader.readLine();
        }
        reader.close();
        writer.close();
    }

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
        //extractor.setL2Normalization(true);
        //extractor.setPowerNormalization(true);


        ImageVectorization.setFeatureExtractor(extractor);
        ImageVectorization.setVladAggregator(new VladAggregatorMultipleVocabularies(codebookFiles,
                numCentroids, AbstractFeatureExtractor.SURFLength));


        if (targetLengthMax < initialLength) {
            System.out.println("targetLengthMax : " + targetLengthMax + " initialLengh " + initialLength);
            PCA pca = new PCA(targetLengthMax, 1, initialLength, true);
            pca.loadPCAFromFile(pcaFile);
            ImageVectorization.setPcaProjector(pca);
        }
    }

    protected static double[] getVector(String imageFolder, String imageFilename) throws Exception {

        ImageVectorization imvec = new ImageVectorization(imageFolder, imageFilename, targetLengthMax, maxNumPixels);

        ImageVectorizationResult imvr = imvec.call();
        double[] vector = imvr.getImageVector();

        return vector;
    }

}
