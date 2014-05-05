package gr.iti.mklab.visual.experiments;

import eu.socialsensor.framework.client.dao.MediaItemDAO;
import eu.socialsensor.framework.client.dao.impl.MediaItemDAOImpl;
import eu.socialsensor.framework.common.domain.MediaItem;
import gr.iti.mklab.visual.aggregation.VladAggregatorMultipleVocabularies;
import gr.iti.mklab.visual.datastructures.AbstractSearchStructure;
import gr.iti.mklab.visual.datastructures.IVFPQ;
import gr.iti.mklab.visual.datastructures.Linear;
import gr.iti.mklab.visual.datastructures.PQ;
import gr.iti.mklab.visual.dimreduction.PCA;
import gr.iti.mklab.visual.extraction.AbstractFeatureExtractor;
import gr.iti.mklab.visual.extraction.SURFExtractor;
import gr.iti.mklab.visual.utilities.Answer;
import gr.iti.mklab.visual.utilities.Result;
import gr.iti.mklab.visual.vectorization.ImageVectorization;
import gr.iti.mklab.visual.vectorization.ImageVectorizationResult;
import ij.process.ImageProcessor;
import org.apache.commons.io.FileUtils;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kandreadou on 4/30/14.
 */
public class QueriesToTheCrawl {

    private final static String queriesFolder = "/home/kandreadou/Pictures/queries/";

    private IVFPQ ivfpq_1 = null;
    private MediaItemDAO mediaDAO = null;


    private static int targetLengthMax = 1024;
    private static int maxNumPixels = 768 * 512;

    private void init() {
        this.mediaDAO = new MediaItemDAOImpl("127.0.0.1", "IVFPQ", "MediaItems");

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

        try {

            int maximumNumVectors = 1000000; //one million
            int m2 = 64;
            int k_c = 256;
            int numCoarseCentroids = 8192;
            String coarseQuantizerFile2 = learningFolder + "qcoarse_1024d_8192k.csv";
            String productQuantizerFile2 = learningFolder + "pq_1024_64x8_rp_ivf_8192k.csv";
            String ivfpqIndexFolder = learningFolder + "BUbivfpq_" + targetLengthMax;
            ivfpq_1 = new IVFPQ(targetLengthMax, maximumNumVectors, false, ivfpqIndexFolder, m2, k_c, PQ.TransformationType.RandomPermutation, numCoarseCentroids, true, 0);
            ivfpq_1.loadCoarseQuantizer(coarseQuantizerFile2);
            ivfpq_1.loadProductQuantizer(productQuantizerFile2);
            int w = 64; // larger values will improve results/increase seach time
            ivfpq_1.setW(w); // how many (out of 8192) lists should be visited during search.
            //visualIndex = new Linear(targetLengthMax, 10000000, false, BDBEnvHome, false, false, 0);
            //int existingVectors = visualIndex.getLoadCounter();
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
        } catch (Exception ex) {

        }
    }

    protected static double[] getVector(String url) throws Exception {

        BufferedImage image = ImageIO.read(new URL(url));
        ImageVectorization imvec = new ImageVectorization(url, image, targetLengthMax, maxNumPixels);

        ImageVectorizationResult imvr = imvec.call();
        double[] vector = imvr.getImageVector();

        return vector;
    }

    private void lookForImage(String foldername, String url) throws Exception {
        String folderName = queriesFolder + foldername;
        System.out.println("Creating directory " + folderName);
        File folder = new File(folderName);
        FileUtils.forceMkdir(folder);
        BufferedImage image = ImageIO.read(new URL(url));
        ImageVectorization imvec = new ImageVectorization(url, image, targetLengthMax, maxNumPixels);
        ImageVectorizationResult imvr = imvec.call();
        double[] vector = imvr.getImageVector();
        ImageIO.write(image, "JPG", new File(folder + "/11query.jpg"));
        Answer a = ivfpq_1.computeNearestNeighbors(20, vector);
        for (Result r : a.getResults()) {

            try {
                String id = r.getId();
                System.out.println("Result from search: " + id+"##");
                MediaItem item = mediaDAO.getMediaItem(id);
                if (item != null) {
                    image = ImageIO.read(new URL(item.getUrl()));
                    ImageIO.write(image, "JPG", new File(folder + "/" + id));
                }

            } catch (MalformedURLException ex) {
                System.out.println("Malformed URL exception ");
            } catch (IIOException ex) {
                System.out.println("IO Exception ");
            }
        }
    }

    public final static void main(String[] args) throws Exception {
        QueriesToTheCrawl queries = new QueriesToTheCrawl();
        queries.init();

        //Sochi bathroom
        String url = "https://pbs.twimg.com/media/BfyoURwCEAAQ-vF.jpg";
        queries.lookForImage("Sochi bathroom", url);

        //Sochi tapwater
        url = "https://pbs.twimg.com/media/BfwkrLmCAAAN60I.png";
        queries.lookForImage("Sochi tapwater", url);

        //Sochi gunfire
        url = "http://media.heavy.com/media/2014/02/1.jpg";
        queries.lookForImage("Sochi gunfire", url);

        //Sochi toilet
        url = "https://pbs.twimg.com/media/BegNuF2CIAA6v9V.jpg";
        queries.lookForImage("Sochi toilet", url);

        url = "http://blogs.reuters.com/photographers-blog/files/2013/02/RTR3DYOJ.jpg";
        queries.lookForImage("Sochi welcome", url);

        //Fake iPhone images
        url = "http://tctechcrunch2011.files.wordpress.com/2014/02/iphone-6-render.jpg?w=738";
        queries.lookForImage("Fake iPhone", url);

        //Venezuella 1
        url = "https://pbs.twimg.com/media/BgnFZW9CAAA_q9w.jpg";
        queries.lookForImage("Venezuella 1", url);

        //Venezuella 2
        url = "https://pbs.twimg.com/media/BgpC-MjCIAAxkkI.jpg";
        queries.lookForImage("Venezuella 2", url);

        //Venezuella 3
        url = "https://pbs.twimg.com/media/BgjhDV1CQAAtdpK.jpg";
        queries.lookForImage("Venezuella 3", url);

        //Venezuella 4
        url = "https://pbs.twimg.com/media/BgZ4O44IYAAzthr.jpg";
        queries.lookForImage("Venezuella 4", url);

        //Venezuella 5
        url = "http://www.globalpost.com/sites/default/files/imagecache/default/photos/2014-February/bulgaria2.jpg";
        queries.lookForImage("Venezuella 5", url);

        //Venezuella 6
        url = "https://pbs.twimg.com/media/BgkLAmCCUAAbjbG.jpg";
        queries.lookForImage("Venezuella 6", url);

        //Ukraine
        url = "https://pbs.twimg.com/media/BhVobTUCQAAmf7J.jpg";
        queries.lookForImage("Ukraine", url);

        //SOS
        url = "http://i.kinja-img.com/gawker-media/image/upload/s--PZC_FzhP--/c_fit,fl_progressive,q_80,w_636/acqtgxs2mh9xhvnmpeuq.jpg";
        queries.lookForImage("SOS", url);

        //frost
        url = "https://pbs.twimg.com/media/BfE_6qCCEAAy1ej.jpg";
        queries.lookForImage("frost", url);

        //fake hoverboard
        url = "http://i.kinja-img.com/gawker-media/image/upload/s--FDVMe6J7--/c_fit,fl_progressive,q_80,w_636/ebm7uu37cv357dfmjl5y.jpg";
        queries.lookForImage("fake hoverboard", url);

        //bhuda
        url = "http://i.kinja-img.com/gawker-media/image/upload/s--643DYPI3--/c_fit,fl_progressive,q_80,w_636/19ent5vhn01y7jpg.jpg";
        queries.lookForImage("bhuda", url);

        //mountain
        url = "https://pbs.twimg.com/media/BfKSV9sIIAA6sSd.jpg";
        queries.lookForImage("mountain", url);

        //earth
        url = "https://pbs.twimg.com/media/BfRVnY0IgAAwAWb.jpg";
        queries.lookForImage("earth", url);

        //waterfall
        url = "https://pbs.twimg.com/media/BfPsGF4IgAAlir-.jpg";
        queries.lookForImage("waterfall", url);

        //water house
        url = "https://pbs.twimg.com/media/BfPONziIUAAFBKK.jpg";
        queries.lookForImage("water house", url);

        //tree
        url = "https://pbs.twimg.com/media/Bf1ao5vIYAAgH1S.jpg";
        queries.lookForImage("tree", url);

        //coca cola
        url = "https://pbs.twimg.com/media/BLqw3isCAAALnU1.jpg";
        queries.lookForImage("coca cola", url);

        //sat
        url = "https://pbs.twimg.com/media/A7iDJwBCYAA8ohP.jpg";
        queries.lookForImage("sat", url);

        //sandy 1
        url = "https://pbs.twimg.com/media/A6XmTviCIAA2tin.jpg";
        queries.lookForImage("sandy 1", url);

        //sandy 2
        url = "http://media.tumblr.com/tumblr_mcnxwpfuh51qz4eis.jpg";
        queries.lookForImage("sandy 2", url);

        //sandy 3
        url = "https://pbs.twimg.com/media/A6Y0peeCAAE9Bve.png";
        queries.lookForImage("sandy 3", url);

        //sandy 4
        url = "https://pbs.twimg.com/media/A6ZJCLdCcAAG_JL.jpg";
        queries.lookForImage("sandy 4", url);

        //shark 1
        url = "http://media.tumblr.com/tumblr_mcog1cnOHW1qz4eis.jpg";
        queries.lookForImage("shark 1", url);

        //shark 2
        url = "https://pbs.twimg.com/media/A6ZfKQKCcAAFLrn.jpg";
        queries.lookForImage("shark 2", url);

    }
}
