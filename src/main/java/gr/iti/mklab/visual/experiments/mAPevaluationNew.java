package gr.iti.mklab.visual.experiments;

import gr.iti.mklab.visual.utilities.Answer;
import gr.iti.mklab.visual.utilities.Result;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by kandreadou on 4/22/14.
 */
public class mAPevaluationNew extends AbstractTest {

    private final static String imageFolder = "/home/kandreadou/datasets/holidays_queries_with_fonts/dataset/";
    private static List<String> allImagesInDataset = new ArrayList<String>();

    private final static String queryFolder = "/home/kandreadou/datasets/holidays_queries_with_fonts/queries/";

    private static double mean = 0;
    private static double queries = 0;

    public static void main(String[] args) throws Exception {
        experiment();
    }

    public static void newImplementation() throws Exception {
        init(false);
        String imageFolder = "/home/kandreadou/datasets/holidays/jpg/";
        File folder = new File(imageFolder);

        for (File file : folder.listFiles()) {
            if (file.isFile()) {
                String imageFilename = file.getName();
                allImagesInDataset.add(imageFilename);
            }
        }

        for (File file : folder.listFiles()) {
            String imageFilename = file.getName();
            if (imageFilename.endsWith("00.jpg")) {
                queries++;
                String firstFourLetters = imageFilename.substring(0, 4);
                System.out.println("##  Searching for " + imageFilename + " starting with " + firstFourLetters);
                double[] vector = getVector(imageFolder, imageFilename);


                List<String> truth = new ArrayList<String>();
                for (String name : allImagesInDataset) {
                    if (name.startsWith(firstFourLetters) && !name.equals(imageFilename)) {
                        truth.add(name);
                    }
                }

                Collections.sort(truth);
                Result[] r = index.computeNearestNeighbors(truth.size()+1, vector).getResults();

                List<String> actual = new ArrayList<String>();
                for (Result res : r) {
                    if (!res.getId().equals(imageFilename))
                        actual.add(res.getId());
                }

                double averagePrecision = calculateAP(actual.toArray(new String[actual.size()]), truth.toArray(new String[truth.size()]));
                mean += averagePrecision;
                System.out.println("AveragePrecision " + averagePrecision);
                if (averagePrecision > 1) {
                    System.out.println("############################# ERROR ###################");
                }
            }
        }
        System.out.println("Mean average precision " + mean / queries);

    }

    public static void experiment() throws Exception {
        init(false);

        File folder = new File(imageFolder);

        /*for (File file : folder.listFiles()) {
            if (file.isFile()) {
                String imageFilename = file.getName();
                allImagesInDataset.add(imageFilename);
            }
        }*/
        indexFilesInFolder(folder);

        File queryfolder = new File(queryFolder);

        for (File file : queryfolder.listFiles()) {
            String imageFilename = file.getName();
            String firstFourLetters = imageFilename.substring(0, 4);
            System.out.println("##  Searching for " + imageFilename + " starting with " + firstFourLetters);
            double[] vector = getVector(queryfolder.getPath() + '/', imageFilename);


            List<String> truth = new ArrayList<String>();
            for (String name : allImagesInDataset) {
                if (name.startsWith(firstFourLetters)) {
                    truth.add(name);

                }
            }

            Collections.sort(truth);
            Result[] r = index.computeNearestNeighbors(truth.size(), vector).getResults();

            List<String> actual = new ArrayList<String>();
            for (Result res : r) {
                actual.add(res.getId());
            }

            double averagePrecision = calculateAP(actual.toArray(new String[actual.size()]), truth.toArray(new String[truth.size()]));


            /*double apSum = 0;
            double cnt = 0;
            for (int i = 0, len = truth.size(); i < len; i++) {
                String trueName = truth.get(i);
                System.out.println("ground truth file " + trueName);
                System.out.println("file " + r[i].getId());

                for (int k = 0; k <= i; k++) {
                    if (r[i].getId().equals(trueName)) {
                        cnt++;

                    }
                }
                apSum += cnt / (i + 1);
            }

            double averagePrecision = 0;
            if (cnt > 0) {
                averagePrecision = apSum / cnt;
            }*/
            mean += averagePrecision;
            System.out.println("AveragePrecision " + averagePrecision);
            if (averagePrecision > 1) {
                System.out.println("############################# ERROR ###################");
            }

        }
        System.out.println("Mean average precision " + mean / queryfolder.listFiles().length);

    }

    protected static void indexFilesInFolder(File folder) {
        for (File file : folder.listFiles()) {
            if (file.isFile()) {
                String imageFilename = file.getName();
                allImagesInDataset.add(imageFilename);
                long start = System.currentTimeMillis();
                try {
                    double[] vector = getVector(folder.getPath() + '/', imageFilename);
                    boolean indexed = index.indexVector(imageFilename, vector);
                    long time = System.currentTimeMillis() - start;
                    //System.out.println("indexed " + indexed + " in " + time + " milliseconds");
                } catch (Exception ex) {
                    System.out.println("#### Error when doing ->Folder path " + folder.getPath() + " imageFilename " + imageFilename);
                }

            } else {
                indexFilesInFolder(file);
            }
        }
    }

    protected static double calculateAP(String[] actualResult, String[] trueResult) {
        double avgP = 0;

        double relevant = 0;
        for (int i = 0; i < trueResult.length; i++) {
            String label = trueResult[i];
            String[] temp = Arrays.copyOfRange(actualResult, 0, i + 1);
            Arrays.sort(temp);
            int found = Arrays.binarySearch(temp, label);
            if (found >= 0) {
                relevant++;
                avgP += relevant / (i + 1);
            }
        }
        if (relevant > 0)
            avgP /= relevant;
        return avgP;
    }

}
