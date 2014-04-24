package gr.iti.mklab.visual.experiments;

import gr.iti.mklab.visual.utilities.Answer;
import gr.iti.mklab.visual.utilities.Result;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by kandreadou on 3/14/14.
 */
public class CreateMapFilesFromFolder {

    private static void oldFashioned() throws IOException {
        List<String> listOfNames = new ArrayList<String>();
        String imageFolder = "/home/kandreadou/datasets/mixed/jpg/";
        File images = new File("/home/kandreadou/datasets/mixed/eval_holidays/holidays_images.dat");
        File perfectResult = new File("/home/kandreadou/datasets/mixed/eval_holidays/perfect_result.dat");
        FileWriter fw = new FileWriter(images.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        File folder = new File(imageFolder);

        for (File file : folder.listFiles()) {
            if(file.isFile()){
                listOfNames.add( file.getName());
            }
        }

        Collections.sort(listOfNames);

        for(String name:listOfNames){
            bw.write(name);
            bw.newLine();
        }

        bw.flush();
        bw.close();

        fw = new FileWriter(perfectResult.getAbsoluteFile());
        bw = new BufferedWriter(fw);

        int rank=0;

        for(String name:listOfNames){

            if(name.endsWith("00.jpg")){
                bw.newLine();
                bw.write(name);
                rank=0;
            }else{
                bw.write(" "+rank+" "+name);
                rank++;
            }
        }

        bw.flush();
        bw.close();
    }


    public static void main(String[] args) throws Exception {
        oldFashioned();
    }
}
