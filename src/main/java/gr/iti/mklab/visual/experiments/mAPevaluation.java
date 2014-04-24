package gr.iti.mklab.visual.experiments;

import eu.socialsensor.framework.client.search.visual.JsonResultSet;
import gr.iti.mklab.visual.utilities.Answer;
import gr.iti.mklab.visual.utilities.Result;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 * Created by kandreadou on 3/10/14.
 */
public class mAPevaluation extends AbstractTest {

    private static int count = 0;
    private static long average = 0;

    //String imageFolder = "/home/kandreadou/datasets/oxbuildings/oxbuild_images/";
    //private final static String imageFolder = "/home/kandreadou/datasets/mixed/jpg/";
    private final static String imageFolder = "/home/kandreadou/datasets/holidays/jpg/";
    //private final static String imageFolder = "/home/kandreadou/datasets/parisbuildings/paris/";

    public static void main(String[] args) throws Exception {
        init(false);

        File arffFile = new File("/home/kandreadou/datasets/holidays/holidays_data.dat");
        FileWriter fw = new FileWriter(arffFile.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        File folder = new File(imageFolder);

        indexFilesInFolder(folder);

        System.out.println("Average time: " + average / count);

        for (File file : folder.listFiles()) {
            String imageFilename = file.getName();
            if (imageFilename.endsWith("00.jpg")) {
                System.out.println("searching for " + imageFilename);
                Answer r = index.computeNearestNeighbors(10, imageFilename);
                bw.write(imageFilename);
                int rank = 0;
                for (Result result : r.getResults()) {
                    bw.write(" " + rank + " " + result.getId());
                    rank++;
                }
                bw.newLine();
            }
        }

        bw.flush();
        bw.close();

    }

    protected static void indexFilesInFolder(File folder) {
        for (File file : folder.listFiles()) {
            if (file.isFile()) {
                String imageFilename = file.getName();
                long start = System.currentTimeMillis();
                try {
                    double[] vector = getVector(folder.getPath() + '/', imageFilename);
                    boolean indexed = index.indexVector(imageFilename, vector);
                    long time = System.currentTimeMillis() - start;
                    average += time;
                    count++;
                    System.out.println("indexed " + indexed + " in " + time + " milliseconds");
                } catch (Exception ex) {
                    System.out.println("#### Error when doing ->Folder path " + folder.getPath() + " imageFilename " + imageFilename);
                }

            } else {
                indexFilesInFolder(file);
            }
        }
    }
}
