package gr.iti.mklab.messaging;

import gr.iti.mklab.visual.experiments.AbstractTest;
import gr.iti.mklab.visual.extraction.AbstractFeatureExtractor;
import gr.iti.mklab.visual.vectorization.ImageVectorizationResult;
import gr.iti.mklab.visual.vectorization.ImageVectorizer;

import java.util.Date;

/**
 * Created by kandreadou on 3/11/14.
 */
public abstract class Worker extends AbstractTest {

    private static final String IMAGE_FOLDER = "/home/kandreadou/Downloads/evaluation/jpg/";
    private static boolean SYNCHRONOUS_PROCESSING = false;
    private ImageVectorizer vectorizer;

    public Worker() {
        try {
            if (SYNCHRONOUS_PROCESSING)
                init(false);
            else {
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

                vectorizer = new ImageVectorizer("surf", "power+l2", codebookFiles,
                        numCentroids, 1024, pcaFile, 10);
            }
        } catch (Exception ex) {

        }
    }

    protected String doWork(String imageName) {
        try {
            if (SYNCHRONOUS_PROCESSING)
                return doSync(imageName);
            return doAsync(imageName);
        } catch (Exception ex) {
            return "error"+ex;
        }
    }

    protected String doSync(String imageName) throws Exception {
        double[] vector = getVector(IMAGE_FOLDER, imageName);
        return "vector length " + vector.length + "for image name " + imageName;
    }

    protected String doAsync(String imageName) throws Exception {
        // scheduling!!!
        String response = "Work done for ";
        System.out.println("Indexing started!");
        long start = System.currentTimeMillis();
        int submittedVectorizationsCounter = 0;
        int completedCounter = 0;
        int failedCounter = 0;

        // if we can submit more tasks to the vectorizer then do it
        if (vectorizer.canAcceptMoreTasks()) {

            vectorizer.submitImageVectorizationTask(IMAGE_FOLDER, imageName);
            submittedVectorizationsCounter++;
            System.out.println("Submitted vectorization tasks: " + submittedVectorizationsCounter
                    + " image:" + imageName);
        }

        // try to get an image vectorization result and to index the vector
        ImageVectorizationResult imvr = vectorizer.getImageVectorizationResult();
        while (imvr != null) {

            String name = imvr.getImageName();
            double[] vector = imvr.getImageVector();
            System.out.println("" + new Date() + ": " + completedCounter + " vectors indexed");
            imvr = vectorizer.getImageVectorizationResult();
            response += name;
        }


        long end = System.currentTimeMillis();
        System.out.println("Total time: " + (end - start) + " ms");
        System.out.println(completedCounter + " indexing tasks completed!");
        return response;
    }

}
