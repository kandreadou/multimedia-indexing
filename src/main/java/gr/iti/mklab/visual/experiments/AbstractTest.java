package gr.iti.mklab.visual.experiments;

import eu.socialsensor.framework.client.dao.MediaItemDAO;
import eu.socialsensor.framework.client.dao.impl.MediaItemDAOImpl;
import eu.socialsensor.framework.client.search.visual.VisualIndexHandler;
import gr.iti.mklab.visual.aggregation.VladAggregatorMultipleVocabularies;
import gr.iti.mklab.visual.dimreduction.PCA;
import gr.iti.mklab.visual.extraction.AbstractFeatureExtractor;
import gr.iti.mklab.visual.extraction.SURFExtractor;
import gr.iti.mklab.visual.vectorization.ImageVectorization;
import gr.iti.mklab.visual.vectorization.ImageVectorizationResult;
import gr.iti.mklab.visual.vectorization.ImageVectorizationTrain;

/**
 * Created by kandreadou on 3/6/14.
 */
public class AbstractTest {

    protected static int targetLengthMax = 1024;
    //protected static int maxNumPixels = 768 * 512;
    protected static int maxNumPixels = 512 * 384;
    protected static VisualIndexHandler visualIndex;
    protected static MediaItemDAO mediaDao;

    protected static void init(boolean train) throws Exception {
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
        if(!train){
            ImageVectorization.setFeatureExtractor(new SURFExtractor());
            ImageVectorization.setVladAggregator(new VladAggregatorMultipleVocabularies(codebookFiles,
                    numCentroids, AbstractFeatureExtractor.SURFLength));
        }else{
            ImageVectorizationTrain.setFeatureExtractor(new SURFExtractor());
            ImageVectorizationTrain.setVladAggregator(new VladAggregatorMultipleVocabularies(codebookFiles,
                    numCentroids, AbstractFeatureExtractor.SURFLength));
        }


        if (targetLengthMax < initialLength) {
            System.out.println("targetLengthMax : "+targetLengthMax+" initialLengh "+initialLength);
            PCA pca = new PCA(targetLengthMax, 1, initialLength, true);
            pca.loadPCAFromFile(pcaFile);
            if(!train){
            ImageVectorization.setPcaProjector(pca);
            }else{
                ImageVectorizationTrain.setPcaProjector(pca);
            }
        }

        String webServiceHost = "http://localhost:8080/VIS";
        //String webServiceHost = "http://160.40.51.20:8080/VisualIndexService";
        String indexCollection =  "test";
        String mongoHost ="127.0.0.1";
        visualIndex = new VisualIndexHandler(webServiceHost, indexCollection);
        mediaDao = new MediaItemDAOImpl(mongoHost);
    }

    protected static double[] getVector(String imageFolder, String imageFilename) throws Exception{

        ImageVectorization imvec = new ImageVectorization(imageFolder, imageFilename, targetLengthMax, maxNumPixels);

        ImageVectorizationResult imvr = imvec.call();
        double[] vector = imvr.getImageVector();

        return vector;
    }
}
