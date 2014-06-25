package gr.iti.mklab.visual.experiments;

import eu.socialsensor.framework.client.dao.MediaItemDAO;
import eu.socialsensor.framework.client.dao.impl.MediaItemDAOImpl;
import eu.socialsensor.framework.client.search.visual.VisualIndexHandler;
import gr.iti.mklab.visual.aggregation.AbstractFeatureAggregator;
import gr.iti.mklab.visual.aggregation.VladAggregatorMultipleVocabularies;
import gr.iti.mklab.visual.datastructures.AbstractSearchStructure;
import gr.iti.mklab.visual.datastructures.IVFPQ;
import gr.iti.mklab.visual.datastructures.Linear;
import gr.iti.mklab.visual.datastructures.PQ;
import gr.iti.mklab.visual.dimreduction.PCA;
import gr.iti.mklab.visual.extraction.AbstractFeatureExtractor;
import gr.iti.mklab.visual.extraction.SURFExtractor;
import gr.iti.mklab.visual.utilities.Answer;
import gr.iti.mklab.visual.utilities.Result;
import gr.iti.mklab.visual.vectorization.ImageVectorization;
import gr.iti.mklab.visual.vectorization.ImageVectorizationResult;
import gr.iti.mklab.visual.vectorization.ImageVectorizationTrain;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by kandreadou on 5/22/14.
 */
public class PopescuEvaluation {

    protected static int maximumNumVectors = 100000;
    protected static int maxNumPixels = 768 * 512;
    protected static int targetLengthMax = 1024;
    protected static IVFPQ ivfpq_1;
    private static int count = 0;
    private static long sum = 0;

    public static void main(String[] args) throws Exception {

        init();
        String imageFolder = "/home/kandreadou/Pictures/popescu/copy_dataset/";
        //indexFilesInFolder(new File(imageFolder));
        //System.out.println("Average time: " + average / count);
        int akr = 0;

        File resultFile = new File("/home/kandreadou/Pictures/popescu/results.txt");
        FileWriter fw = new FileWriter(resultFile.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        File folder = new File(imageFolder);
        File[] files = folder.listFiles();
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File f1, File f2) {
                try {
                    int i1 = Integer.parseInt(f1.getName().split("_")[0]);
                    int i2 = Integer.parseInt(f2.getName().split("_")[0]);
                    return i1 - i2;
                } catch (NumberFormatException e) {
                    //ignore
                    return 0;
                }
            }
        });

        for (File file : files) {
            if (akr>50000) break;
            akr ++;
            String imageFilename = file.getName();
            if (imageFilename.endsWith("_tr_0.jpg")) {
                System.out.println("searching for " + imageFilename);
                long start = System.currentTimeMillis();
                double[] vector = getVector(folder.getPath() + '/', imageFilename);
                long duration = System.currentTimeMillis() - start;
                System.out.println("Search duration "+duration);
                sum += duration;
                count++;
                Answer r = ivfpq_1.computeNearestNeighbors(14, vector);

                for (Result result : r.getResults()) {
                    bw.write(convertName(result.getId()) + " ");
                }
                bw.newLine();
            }
        }

        bw.flush();
        bw.close();
        System.out.println("Average search time "+ (sum/count));
    }

    private static void sortFiles() {
        try {
            File resultFile = new File("/home/kandreadou/Pictures/popescu/results.txt");
            File newFile = new File("/home/kandreadou/Pictures/popescu/newres.txt");
            BufferedWriter bw = new BufferedWriter(new FileWriter(newFile.getAbsoluteFile()));
            BufferedReader br = new BufferedReader(new FileReader(resultFile.getAbsoluteFile()));
            String line = br.readLine();
            while (line != null) {
                String[] names = line.split(" ");
                Arrays.sort(names, new Comparator<String>() {
                    public int compare(String f1, String f2) {
                        try {
                            int i1 = Integer.parseInt(f1.split("_")[2]);
                            int i2 = Integer.parseInt(f2.split("_")[2]);
                            return i1 - i2;
                        } catch (NumberFormatException e) {
                            throw new AssertionError(e);
                        }
                    }
                });
                for (String n : names) {
                    bw.write(n + " ");
                }
                line = br.readLine();
                bw.newLine();
            }
            bw.flush();
            bw.close();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static int convertName(String input) {
        String withoutSuffix = input.substring(0, input.lastIndexOf("."));
        String[] items = withoutSuffix.split("_");
        return Integer.parseInt(items[0])*14+Integer.parseInt(items[2]);
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

        int m2 = 64;
        int k_c = 256;
        int numCoarseCentroids = 8192;
        String coarseQuantizerFile2 = learningFolder + "qcoarse_1024d_8192k.csv";
        String productQuantizerFile2 = learningFolder + "pq_1024_64x8_rp_ivf_8192k.csv";
        String ivfpqIndexFolder = learningFolder + "popescu_" + targetLengthMax;
        ivfpq_1 = new IVFPQ(targetLengthMax, maximumNumVectors, false, ivfpqIndexFolder, m2, k_c, PQ.TransformationType.RandomPermutation, numCoarseCentroids, true, 0);
        ivfpq_1.loadCoarseQuantizer(coarseQuantizerFile2);
        ivfpq_1.loadProductQuantizer(productQuantizerFile2);
        int w = 64; // larger values will improve results/increase seach time
        ivfpq_1.setW(w); // how many (out of 8192) lists should be visited during search.*/
        int loadCounter = ivfpq_1.getLoadCounter();
        System.out.println("Load counter: " + loadCounter);
    }


    protected static void indexFilesInFolder(File folder) {
        for (File file : folder.listFiles()) {
            if (file.isFile()) {
                String imageFilename = file.getName();
                long start = System.currentTimeMillis();
                try {
                    double[] vector = getVector(folder.getPath() + '/', imageFilename);
                    boolean indexed = ivfpq_1.indexVector(imageFilename, vector);
                    long time = System.currentTimeMillis() - start;
                    System.out.println("indexed " + indexed + " in " + time + " milliseconds");
                } catch (Exception ex) {
                    System.out.println("#### Error when doing ->Folder path " + folder.getPath() + " imageFilename " + imageFilename);
                }

            } else {
                indexFilesInFolder(file);
            }
        }
    }

    protected static double[] getVector(String imageFolder, String imageFilename) throws Exception {

        ImageVectorization imvec = new ImageVectorization(imageFolder, imageFilename, targetLengthMax, maxNumPixels);
        ImageVectorizationResult imvr = imvec.call();
        double[] vector = imvr.getImageVector();

        return vector;
    }


}
