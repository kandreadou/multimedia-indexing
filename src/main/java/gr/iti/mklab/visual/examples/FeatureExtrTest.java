package gr.iti.mklab.visual.examples;

import eu.socialsensor.framework.client.dao.MediaItemDAO;
import eu.socialsensor.framework.client.dao.impl.MediaItemDAOImpl;
import eu.socialsensor.framework.client.search.visual.JsonResultSet;
import eu.socialsensor.framework.client.search.visual.VisualIndexHandler;
import eu.socialsensor.framework.common.domain.MediaItem;
import gr.iti.mklab.visual.aggregation.VladAggregatorMultipleVocabularies;
import gr.iti.mklab.visual.dimreduction.PCA;
import gr.iti.mklab.visual.extraction.AbstractFeatureExtractor;
import gr.iti.mklab.visual.extraction.SURFExtractor;
import gr.iti.mklab.visual.vectorization.ImageVectorization;
import gr.iti.mklab.visual.vectorization.ImageVectorizationResult;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

/**
 * Created by katerina on 1/21/14.
 * Just a test class
 */
public class FeatureExtrTest {

    private static int targetLengthMax = 1024;
    private static int maxNumPixels = 768 * 512;
    private static VisualIndexHandler visualIndex;
    private static MediaItemDAO mediaDao;

    private static void init() throws Exception {
        String learningFolder = "/home/katerina/workspace/learning_files/";

        String imageFolder = "/home/katerina/Desktop/";
        String imageFilename = "work.jpg";

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
        ImageVectorization.setFeatureExtractor(new SURFExtractor());
        ImageVectorization.setVladAggregator(new VladAggregatorMultipleVocabularies(codebookFiles,
                numCentroids, AbstractFeatureExtractor.SURFLength));

        if (targetLengthMax < initialLength) {
            System.out.println("targetLengthMax : "+targetLengthMax+" initialLengh "+initialLength);
            PCA pca = new PCA(targetLengthMax, 1, initialLength, true);
            pca.loadPCAFromFile(pcaFile);
            ImageVectorization.setPcaProjector(pca);
        }

        String webServiceHost = "http://localhost:8080/VIS";
        //String webServiceHost = "http://160.40.51.20:8080/VisualIndexService";
        String indexCollection =  "reveal";
        String mongoHost ="127.0.0.1";
        visualIndex = new VisualIndexHandler(webServiceHost, indexCollection);
        mediaDao = new MediaItemDAOImpl(mongoHost);
    }

    private static double[] getVector(String imageFolder, String imageFilename) throws Exception{
        ImageVectorization imvec = new ImageVectorization(imageFolder, imageFilename, targetLengthMax, maxNumPixels);

        ImageVectorizationResult imvr = imvec.call();
        double[] vector = imvr.getImageVector();
        System.out.println(Arrays.toString(vector));
        return vector;
    }

    public static void main(String[] args) throws Exception {
        init();
        String imageFolder = "/home/katerina/Desktop/obama/";
        File folder = new File(imageFolder);
        String imageId = null;

        for (File file :  folder.listFiles()) {
            if (file.isFile()) {
                imageId = String.valueOf(System.currentTimeMillis());
                String imageFilename = file.getName();
                System.out.println(imageId+" for "+imageFilename);
                double[] vector = getVector(imageFolder, imageFilename);
                boolean indexed = visualIndex.index(imageId, vector);
                System.out.println("indexed "+indexed);
                String response = visualIndex.uploadImage(id, thumbnail, contentType);
                //MediaItem item = new MediaItem(new URL(imageFilename));
                //item.setId(imageId);
                //mediaDao.addMediaItem(item);

            }
        }

        JsonResultSet result = visualIndex.getSimilarImages(imageId, 0.5);
        System.out.println(result.toJSON());

    }

    /*public static void main(String[] args) throws Exception {
        init();
        JsonResultSet result = visualIndex.getSimilarImages(new URL("http://reface.me/wp-content/uploads/obama-facebook-profile-picture.jpg"), 0.5);
        System.out.println(result.toJSON());
    }*/

    /*public static void main(String[] args) throws Exception {

        String learningFolder = "/home/katerina/workspace/learning_files/";

        String imageFolder = "/home/katerina/Desktop/";
        String imageFilename = "work.jpg";
        int targetLengthMax = 1024;
        int maxNumPixels = 768 * 512;

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
        ImageVectorization.setFeatureExtractor(new SURFExtractor());
        ImageVectorization.setVladAggregator(new VladAggregatorMultipleVocabularies(codebookFiles,
                numCentroids, AbstractFeatureExtractor.SURFLength));

        if (targetLengthMax < initialLength) {
            System.out.println("targetLengthMax : "+targetLengthMax+" initialLengh "+initialLength);
            PCA pca = new PCA(targetLengthMax, 1, initialLength, true);
            pca.loadPCAFromFile(pcaFile);
            ImageVectorization.setPcaProjector(pca);
        }

        ImageVectorization imvec = new ImageVectorization(imageFolder, imageFilename, targetLengthMax, maxNumPixels);

        ImageVectorizationResult imvr = imvec.call();
        double[] vector = imvr.getImageVector();
        System.out.println(Arrays.toString(vector));

        //String webServiceHost = "http://160.40.51.20:8080/VisualIndexService/";
        String webServiceHost = "http://localhost:8080/VIS";
        String indexCollection =  "reveal";
        //this.vectorizer = new ImageVectorizer(codebookPath, pcafilePath,  this.conf, false);
        VisualIndexHandler visualIndex = new VisualIndexHandler(webServiceHost, indexCollection);
        String id = "test123";
        boolean indexed = visualIndex.index(id, vector);
        System.out.println("image with id "+id+" has been indexed "+indexed);
    }*/
}


