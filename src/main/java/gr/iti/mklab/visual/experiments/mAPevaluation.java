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


    public static void main(String[] args) throws Exception {
        init(false);
        String imageFolder = "/home/kandreadou/Desktop/meme_test_set/";
        //String imageFolder = "/home/kandreadou/Downloads/evaluation/jpg/";
        File arffFile = new File("/home/kandreadou/Desktop/holidays_data.dat");
        FileWriter fw = new FileWriter(arffFile.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        File folder = new File(imageFolder);
        int count = 0;
        long average = 0;

        for (File file : folder.listFiles()) {
            if (file.isFile()) {
                String imageFilename = file.getName();
                long start = System.currentTimeMillis();
                double[] vector = getVector(imageFolder, imageFilename);
                boolean indexed = index.indexVector(imageFilename, vector);
                long time = System.currentTimeMillis() - start;
                System.out.println("indexed " + indexed + " in " + time + " milliseconds");
                average += time;
                count++;
            }
        }

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
}
