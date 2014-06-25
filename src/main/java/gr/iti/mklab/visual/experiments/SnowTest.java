package gr.iti.mklab.visual.experiments;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.socialsensor.framework.client.dao.MediaItemDAO;
import eu.socialsensor.framework.client.dao.impl.MediaItemDAOImpl;
import eu.socialsensor.framework.client.search.visual.VisualIndexHandler;
import eu.socialsensor.framework.common.domain.MediaItem;
import gr.iti.mklab.visual.aggregation.VladAggregatorMultipleVocabularies;
import gr.iti.mklab.visual.datastructures.IVFPQ;
import gr.iti.mklab.visual.datastructures.PQ;
import gr.iti.mklab.visual.dimreduction.PCA;
import gr.iti.mklab.visual.extraction.AbstractFeatureExtractor;
import gr.iti.mklab.visual.extraction.SURFExtractor;
import gr.iti.mklab.visual.vectorization.ImageVectorization;
import gr.iti.mklab.visual.vectorization.ImageVectorizationResult;
import org.hsqldb.lib.FileUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kandreadou on 6/13/14.
 */
public class SnowTest {

    private static VisualIndexHandler visualIndex;

    private static int targetLengthMax = 1024;
    private static int maxNumPixels = 768 * 512;

    private final static String webServiceHost = "http://160.40.51.20:8080/VIS";
    private final static String indexCollection = "snow";
    private static int counter = 0;
    private static long wholeDuration = 0;

    public static void main(String[] args) throws Exception {
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


        visualIndex = new VisualIndexHandler(webServiceHost, indexCollection);

        SURFExtractor extractor = new SURFExtractor();
        ImageVectorization.setFeatureExtractor(extractor);
        ImageVectorization.setVladAggregator(new VladAggregatorMultipleVocabularies(codebookFiles,
                numCentroids, AbstractFeatureExtractor.SURFLength));
        if (targetLengthMax < initialLength) {
            System.out.println("targetLengthMax : " + targetLengthMax + " initialLengh " + initialLength);
            PCA pca = new PCA(targetLengthMax, 1, initialLength, true);
            pca.loadPCAFromFile(pcaFile);
            ImageVectorization.setPcaProjector(pca);
        }
        snow();
    }

    private static void snow() throws Exception {


        String jsonFilesFolder = "/home/kandreadou/Pictures/snow/";
        JsonParser parser = new JsonParser();
        List<String> jsonFiles = new ArrayList<String>();
        for (int i = 0; i < 42; i++) {
            jsonFiles.add(jsonFilesFolder + "tweets.json." + i);
        }

        for (int i = 0; i < jsonFiles.size(); i++) {
            System.out.println(jsonFiles.get(i));
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(jsonFiles.get(i)), "UTF-8"));
            String line = null;
            while ((line = reader.readLine()) != null) {
                JsonObject tweet = parser.parse(line).getAsJsonObject();
                JsonElement mediaElement = tweet.get("entities").getAsJsonObject().get("media");
                if (mediaElement != null) {
                    JsonArray mediaArray = mediaElement.getAsJsonArray();
                    for (JsonElement element : mediaArray) {
                        JsonElement mediaUrlElement = element.getAsJsonObject().get("media_url");
                        if (mediaUrlElement != null) {
                            String mediaUrl = mediaUrlElement.getAsString();
                            try {
                                indexImage(mediaUrl, ImageIO.read(new URL(mediaUrl)));
                            } catch (Exception ex) {
                                //do nothing
                            }
                        }
                    }
                }
            }
            reader.close();
        }
        System.out.println("number " + counter);
    }

    public static void indexImage(String name, BufferedImage image) {
        try {
            counter++;
            ImageVectorization imvec = new ImageVectorization(name, image, targetLengthMax, maxNumPixels);
            ImageVectorizationResult result = imvec.call();
            long start = System.currentTimeMillis();
            boolean indexed = visualIndex.index(name, result.getImageVector());
            long duration = System.currentTimeMillis() - start;
            wholeDuration += duration;
            System.out.println("indexing time " + duration);
            System.out.println("average indexing time " + wholeDuration / counter);
            if (!indexed) {
                System.out.println("Error indexing image " + name);
            }
        } catch (Exception e) {
            System.out.println("Error indexing image " + name);
        }
    }


}
