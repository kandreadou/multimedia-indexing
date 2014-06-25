package gr.iti.mklab.visual.experiments;

import eu.socialsensor.framework.client.dao.MediaItemDAO;
import eu.socialsensor.framework.client.dao.impl.MediaItemDAOImpl;
import eu.socialsensor.framework.client.search.visual.VisualIndexHandler;
import gr.iti.mklab.visual.aggregation.AbstractFeatureAggregator;
import gr.iti.mklab.visual.aggregation.VladAggregatorMultipleVocabularies;
import gr.iti.mklab.visual.datastructures.AbstractSearchStructure;
import gr.iti.mklab.visual.datastructures.Linear;
import gr.iti.mklab.visual.dimreduction.PCA;
import gr.iti.mklab.visual.extraction.AbstractFeatureExtractor;
import gr.iti.mklab.visual.extraction.SURFExtractor;
import gr.iti.mklab.visual.vectorization.*;

/**
 * Created by kandreadou on 3/6/14.
 */
public class AbstractTest {

    protected static int maxNumPixels = 768 * 512;
    protected static int targetLengthMax = 1024;
    //protected static int maxNumPixels = 512 * 384;
    protected static VisualIndexHandler visualIndex;
    protected static MediaItemDAO mediaDao;
    protected static AbstractSearchStructure index;

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
        SURFExtractor extractor = new SURFExtractor();
        //extractor.setL2Normalization(true);
        //extractor.setPowerNormalization(true);

        if(!train){
            ImageVectorization.setFeatureExtractor(extractor);
            ImageVectorization.setVladAggregator(new VladAggregatorMultipleVocabularies(codebookFiles,
                    numCentroids, AbstractFeatureExtractor.SURFLength));
        }else{
            ImVecNew.setFeatureExtractor(extractor);
            ImVecNew.setVladAggregator(new VladAggregatorMultipleVocabularies(codebookFiles,
                    numCentroids, AbstractFeatureExtractor.SURFLength));
            ImVecNew.loadClassifier();
        }


        if (targetLengthMax < initialLength) {
            System.out.println("targetLengthMax : "+targetLengthMax+" initialLengh "+initialLength);
            PCA pca = new PCA(targetLengthMax, 1, initialLength, true);
            pca.loadPCAFromFile(pcaFile);
            if(!train){
            ImageVectorization.setPcaProjector(pca);
            }else{
                ImVecNew.setPcaProjector(pca);
            }
        }

        String webServiceHost = "http://localhost:8080/VIS";
        //String webServiceHost = "http://160.40.51.20:8080/VisualIndexService";
        String indexCollection =  "classifier";
        String mongoHost ="127.0.0.1";
        visualIndex = new VisualIndexHandler(webServiceHost, indexCollection);
        mediaDao = new MediaItemDAOImpl(mongoHost);

        String BDBEnvHome = learningFolder + "newTests_" + targetLengthMax;
        index = new Linear(targetLengthMax, 6000, false, BDBEnvHome, true,
                true, 0);
    }

    protected static double[] getVector(String imageFolder, String imageFilename) throws Exception{

        ImVecNew imvec = new ImVecNew(imageFolder, imageFilename, targetLengthMax, maxNumPixels);

        ImageVectorizationResult imvr = imvec.call();
        double[] vector = imvr.getImageVector();

        return vector;
    }
}
