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
import ij.gui.Roi;
import ij.io.RoiDecoder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by katerina on 1/21/14.
 * Just a test class
 */
public class FeatureExtrTest extends AbstractTest {


    /**
     * Calculate average time for visual extraction and indexing
     * @param args
     * @throws Exception
     */
    /*public static void main(String[] args) throws Exception {
        init();
        String imageFolder = "/home/kandreadou/Desktop/samples/";
        File folder = new File(imageFolder);
        String imageId = null;
        int count=0;
        long average = 0;

        for (File file :  folder.listFiles()) {
            if (file.isFile()) {
                imageId = "liberty456";
                String imageFilename = file.getName();
                System.out.println(imageId+" for "+imageFilename);
                long start = System.currentTimeMillis();
                double[] vector = getVector(imageFolder, imageFilename);
                boolean indexed = visualIndex.index(imageId, vector);
                long time = System.currentTimeMillis() - start;
                System.out.println("indexed "+indexed+" in "+time+" milliseconds");
                average+=time;
                count++;
                //String response = visualIndex.uploadImage(id, thumbnail, contentType);
                //MediaItem item = new MediaItem(new URL(imageFilename));
                //item.setId(imageId);
                //mediaDao.addMediaItem(item);

            }
        }
        System.out.println("Average time: "+average/count);
        JsonResultSet result = visualIndex.getSimilarImages(imageId, 0.5);
        System.out.println(result.toJSON());
    }*/

    public static void main(String[] args) throws Exception {
        init(false);
        String imageFolder = "/home/kandreadou/Desktop/";
        double[] vector = getVector(imageFolder, "damerkel.jpg");
        boolean indexed = visualIndex.index("merk11", vector);
        System.out.println("indexed first");
        vector = getVector(imageFolder, "damerkelnew.jpg");
        boolean indexed1 = visualIndex.index("merk12", vector);
        System.out.println("indexed second");
        JsonResultSet result = visualIndex.getSimilarImages("merk11", 0.99);
        System.out.println(result.toJSON());
        result = visualIndex.getSimilarImages("merk12", 0.99);
        System.out.println(result.toJSON());

        return;
    }



    /*public static void main(String[] args) throws Exception {
        init();
        System.out.println("http://www.theblaze.com/wp-content/uploads/2012/10/Hurricane-Sandy-Fake-Photo.jpg but smaller");
        JsonResultSet result = visualIndex.getSimilarImages(new URL("http://www.theblaze.com/wp-content/uploads/2012/10/Hurricane-Sandy-Fake-Photo.jpg"), 0.9);
        System.out.println(result.toJSON());
        result = visualIndex.getSimilarImages("liberty456", 0.75);
        System.out.println(result.toJSON());

        System.out.println("http://ichef.bbci.co.uk/wwfeatures/624_351/images/live/p0/10/h6/p010h6cj.jpg cropped");
        result = visualIndex.getSimilarImages(new URL("http://ichef.bbci.co.uk/wwfeatures/624_351/images/live/p0/10/h6/p010h6cj.jpg"), 0.9);
        System.out.println(result.toJSON());

        System.out.println("http://rack.0.mshcdn.com/media/ZgkyMDEzLzA0LzA4Lzk4LzdmYWtlaHVycmljLjZkNmMyLmpwZwpwCXRodW1iCTEyMDB4NjI3IwplCWpwZw/6eab4ffb/e67/7-fake-hurricane-sandy-photos-you-re-sharing-on-social-media-46fb017a9c.jpg cropped");
        result = visualIndex.getSimilarImages(new URL("http://rack.0.mshcdn.com/media/ZgkyMDEzLzA0LzA4Lzk4LzdmYWtlaHVycmljLjZkNmMyLmpwZwpwCXRodW1iCTEyMDB4NjI3IwplCWpwZw/6eab4ffb/e67/7-fake-hurricane-sandy-photos-you-re-sharing-on-social-media-46fb017a9c.jpg"), 0.9);
        System.out.println(result.toJSON());

        System.out.println("http://meetthematts.com/wp-content/uploads/2012/11/Hurricane-Sandy-statue-of-liberty.png colours weird");
        result = visualIndex.getSimilarImages(new URL("http://meetthematts.com/wp-content/uploads/2012/11/Hurricane-Sandy-statue-of-liberty.png"), 0.9);
        System.out.println(result.toJSON());

        System.out.println("http://www.mentalfloss.com/sites/default/legacy/blogs/wp-content/uploads/2012/10/FakeSandy10.jpeg fake with cat");
        result = visualIndex.getSimilarImages(new URL("http://www.mentalfloss.com/sites/default/legacy/blogs/wp-content/uploads/2012/10/FakeSandy10.jpeg"), 0.9);
        System.out.println(result.toJSON());

        System.out.println("https://now.mmedia.me/Pages/ImageStreamer/param/MediaID__4c3d76cf-0f71-2ab6-994b-2065556e4754/w__612/h__411/top4.jpg fake with four");
        result = visualIndex.getSimilarImages(new URL("https://now.mmedia.me/Pages/ImageStreamer/param/MediaID__4c3d76cf-0f71-2ab6-994b-2065556e4754/w__612/h__411/top4.jpg"), 0.9);
        System.out.println(result.toJSON());
        mediaDao = new MediaItemDAOImpl("127.0.0.1");
        MediaItem item = new MediaItem(new URL("http://www.theblaze.com/wp-content/uploads/2012/10/Hurricane-Sandy-Fake-Photo.jpg"));
        item.setId("345");
        mediaDao.addMediaItem(item);

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


