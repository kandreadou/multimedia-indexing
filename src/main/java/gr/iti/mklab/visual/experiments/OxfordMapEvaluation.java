package gr.iti.mklab.visual.experiments;

import gr.iti.mklab.visual.utilities.Answer;
import gr.iti.mklab.visual.utilities.Result;

import java.io.*;

/**
 * Created by kandreadou on 3/14/14.
 */
public class OxfordMapEvaluation extends AbstractTest {


    public static void main(String[] args) throws Exception {

        init(false);

        //String queryFolder = "/home/kandreadou/datasets/oxbuildings/gt_files_170407/";
        String queryFolder = "/home/kandreadou/datasets/parisbuildings/paris_120310/";
        //String resultsFolder = "/home/kandreadou/Downloads/oxbuildings/results/";
        File folder = new File(queryFolder);

        for (File file : folder.listFiles()) {
            String fileName = file.getName();
            //Find the query files
            if (fileName.contains("query")){
                String resultFileName = fileName.substring(0, fileName.indexOf("_query"));
                System.out.println("Found query file "+fileName);
                //Extract the name of the image
                FileReader fr = new FileReader(new File(queryFolder+fileName).getAbsoluteFile());
                BufferedReader br = new BufferedReader(fr);
                String line = br.readLine();
                //for parirs
                String imageName = line.split("\\s+")[0]+".jpg";
                // for oxrford
                //String imageName = line.split("\\s+")[0].substring(5)+".jpg";
                br.close();
                fr.close();
                System.out.println("imageName "+imageName);

                //Search and create respective rank list file
                String resultFilePath = queryFolder+resultFileName+"_result.txt";
                FileWriter fw = new FileWriter(new File(resultFilePath).getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);
                Answer r = index.computeNearestNeighbors(5000, imageName);
                //w.write(extractImageName(imageName));
                //bw.newLine();
                for (Result result : r.getResults()) {
                    bw.write(extractImageName(result.getId()));
                    bw.newLine();
                }
                bw.newLine();
                bw.close();
                bw.close();
            }
        }
    }

    private static String extractImageName(String input){
        return input.substring(0, input.indexOf('.'));
    }
}
