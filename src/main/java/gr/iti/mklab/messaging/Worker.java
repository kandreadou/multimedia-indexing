package gr.iti.mklab.messaging;

import gr.iti.mklab.visual.experiments.AbstractTest;

/**
 * Created by kandreadou on 3/11/14.
 */
public abstract class Worker extends AbstractTest {

    public Worker(){
        try{
            init(false);
        }catch(Exception ex){

        }
    }

    protected void doWork(String imageName) throws Exception{
        String imageFolder = "/home/kandreadou/Downloads/evaluation/jpg/";
        double[] vector = getVector( imageFolder, imageName);
        System.out.println("Work done for "+imageName);
    }

}
