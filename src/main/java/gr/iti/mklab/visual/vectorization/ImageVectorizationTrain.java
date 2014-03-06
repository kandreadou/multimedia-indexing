package gr.iti.mklab.visual.vectorization;

import georegression.struct.point.Point2D_F64;
import gr.iti.mklab.visual.aggregation.VladAggregatorMultipleVocabularies;
import gr.iti.mklab.visual.dimreduction.PCA;
import gr.iti.mklab.visual.extraction.AbstractFeatureExtractor;
import gr.iti.mklab.visual.extraction.ImageScaling;
import gr.iti.mklab.visual.extraction.SURFExtractor;
import gr.iti.mklab.visual.utilities.ImageIOGreyScale;
import ij.gui.Roi;
import weka.classifiers.functions.SMO;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * Created by kandreadou on 3/6/14.
 */
public class ImageVectorizationTrain implements Callable<ImageVectorizationResult> {

    /**
     * Image will be scaled at this maximum number of pixels before vectorization.
     */
    private int maxImageSizeInPixels = 1024 * 768;

    /**
     * The filename of the image.
     */
    private String imageFilename;

    /**
     * The directory (full path) where the image resides.
     */
    private String imageFolder;

    /**
     * The image as a BufferedImage object.
     */
    private BufferedImage image;

    /**
     * The target length of the extracted vector.
     */
    private int vectorLength;

    /**
     * This object is used for descriptor extraction.
     */
    private static AbstractFeatureExtractor featureExtractor;

    /**
     * This object is used for extracting VLAD vectors with multiple vocabulary aggregation.
     */
    private static VladAggregatorMultipleVocabularies vladAggregator;

    /**
     * This object is used for PCA projection and whitening.
     */
    private static PCA pcaProjector;

    private Roi[] rois;
    private BufferedWriter bw;

    /**
     * If set to true, debug output is displayed.
     */
    public boolean debug = false;

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * This constructor is used when the image should be read into a BufferedImage object from the given
     * folder.
     *
     * @param imageFolder          The folder (full path) where the image resides
     * @param imageFilename        The filename of the image
     * @param vectorLength         The target length of the vector
     * @param maxImageSizeInPixels The maximum image size of in pixels. It the image is larger, it is first scaled down prior
     *                             to vectorization.
     */
    public ImageVectorizationTrain(String imageFolder, String imageFilename, int vectorLength,
                              int maxImageSizeInPixels) {
        this.imageFolder = imageFolder;
        this.imageFilename = imageFilename;
        this.vectorLength = vectorLength;
        this.maxImageSizeInPixels = maxImageSizeInPixels;
    }

    public ImageVectorizationTrain(String imageFolder, String imageFilename, int vectorLength,
                              int maxImageSizeInPixels, Roi[] recs, BufferedWriter bw) {
        this(imageFolder, imageFilename, vectorLength, maxImageSizeInPixels);
        this.rois = recs;
        this.bw = bw;
    }

    /**
     * This constructor is used when the image has been already read into a BufferedImage object.
     *
     * @param imageFilename        The filename of the image
     * @param image                A BufferedImage object of the image
     * @param vectorLength         The target length of the vector
     * @param maxImageSizeInPixels The maximum image size of in pixels. It the image is larger, it is first scaled down prior
     *                             to vectorization.
     */
    public ImageVectorizationTrain(String imageFilename, BufferedImage image, int vectorLength,
                              int maxImageSizeInPixels) {
        this.imageFilename = imageFilename;
        this.vectorLength = vectorLength;
        this.image = image;
        this.maxImageSizeInPixels = maxImageSizeInPixels;
    }

    @Override
    /**
     * Returns an ImageVectorizationResult object from where the image's vector and name can be
     * obtained.
     */
    public ImageVectorizationResult call() throws Exception {
        if (debug)
            System.out.println("Vectorization for image " + imageFilename + " started.");
        double[] imageVector = transformToVector();
        if (debug)
            System.out.println("Vectorization for image " + imageFilename + " completed.");
        return new ImageVectorizationResult(imageFilename, imageVector);
    }


    /**
     * Transforms the image into a vector and returns the result.
     *
     * @return The image's vector.
     * @throws Exception
     */
    public double[] transformToVector() throws Exception {
        if (vectorLength > vladAggregator.getVectorLength() || vectorLength <= 0) {
            throw new Exception("Vector length should be between 1 and " + vladAggregator.getVectorLength());
        }
        // first the image is read if the image field is null
        if (image == null) {
            try { // first try reading with the default class
                image = ImageIO.read(new File(imageFolder + imageFilename));
            } catch (IllegalArgumentException e) {
                // this exception is probably thrown because of a greyscale jpeg image
                System.out.println("Exception: " + e.getMessage() + " | Image: " + imageFilename);
                // retry with the modified class
                image = ImageIOGreyScale.read(new File(imageFolder + imageFilename));
            }
        }

        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();
        long originalSize = originalWidth * originalHeight;

        // next the image is scaled
        ImageScaling scale = new ImageScaling(maxImageSizeInPixels);
        image = scale.maxPixelsScaling(image);

        int targetWidth = image.getWidth();
        int targetHeight = image.getHeight();
        long targetSize = targetWidth * targetHeight;

        double scalingRatio = (double) targetSize / originalSize;
        // scaling ratio per dimension
        scalingRatio = Math.sqrt(scalingRatio);
        System.out.println("scaling ratio : " + scalingRatio);

        // next the local features are extracted
        double[][] features = featureExtractor.extractFeatures(image);

        Point2D_F64[] points = ((SURFExtractor) featureExtractor).points;

        for (int i = 0, len = points.length; i < len; i++) {
            int x = (int) (points[i].x / scalingRatio);
            int y = (int) (points[i].y / scalingRatio);
            for (int k = 0; k < features[i].length; k++) {
                bw.write(features[i][k] + ",");
            }

            boolean foundRoi = false;
            if (rois != null) {
                for (Roi roi : rois) {
                    if (roi.contains(x, y)) {
                        bw.write("OUT");
                        foundRoi = true;
                        break;
                    }
                }
            }
            if (!foundRoi)
                bw.write("IN");
            bw.newLine();
        }

        // next the features are aggregated
        double[] vladVector = vladAggregator.aggregate(features);

        if (vladVector.length == vectorLength) {
            // no projection is needed
            return vladVector;
        } else {
            // pca projection is applied
            double[] projected = pcaProjector.sampleToEigenSpace(vladVector);
            return projected;
        }
    }

    /**
     * Sets the FeatureExtractor object that will be used.
     *
     * @param extractor
     */
    public static void setFeatureExtractor(AbstractFeatureExtractor extractor) {
        ImageVectorizationTrain.featureExtractor = extractor;
    }

    /**
     * Sets the VladAggregatorMultipleVocabularies object that will be used.
     *
     * @param vladAggregator
     */
    public static void setVladAggregator(VladAggregatorMultipleVocabularies vladAggregator) {
        ImageVectorizationTrain.vladAggregator = vladAggregator;
    }

    /**
     * Sets the PCA projection object that will be used.
     *
     * @param pcaProjector
     */
    public static void setPcaProjector(PCA pcaProjector) {
        ImageVectorizationTrain.pcaProjector = pcaProjector;
    }

    /**
     * Example of a single image vectorization using this class.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {

        String learningFiles = "/home/manosetro/git/multimedia-indexing/learning_files/";

        File imageFolder = new File("/disk1_data/Photos/AkisGenethlia");

        String[] codebookFiles = {
                learningFiles + "surf_l2_128c_0.csv",
                learningFiles + "surf_l2_128c_1.csv",
                learningFiles + "surf_l2_128c_2.csv",
                learningFiles + "surf_l2_128c_3.csv"};

        int[] numCentroids = {128, 128, 128, 128};

        String pcaFilename = learningFiles + "pca_surf_4x128_32768to1024.txt";
        int initialLength = numCentroids.length * numCentroids[0] * AbstractFeatureExtractor.SURFLength;
        int targetLength = 1024;

        System.out.println("Initial length : " + numCentroids.length + "x" +
                numCentroids[0] + "x" + AbstractFeatureExtractor.SURFLength + "=" + initialLength);

        if (targetLength < initialLength) {
            PCA pca = new PCA(targetLength, 1, initialLength, true);
            pca.loadPCAFromFile(pcaFilename);
            ImageVectorization.setPcaProjector(pca);
            System.out.println("PCA loaded! ");
        }


        long t = System.currentTimeMillis();
        for (String imagFilename : imageFolder.list()) {

            ImageVectorization imvec = new ImageVectorization(imageFolder.toString() + "/", imagFilename, targetLength, 512 * 384);
            ImageVectorization.setFeatureExtractor(new SURFExtractor());
            ImageVectorization.setVladAggregator(new VladAggregatorMultipleVocabularies(codebookFiles,
                    numCentroids, AbstractFeatureExtractor.SURFLength));

            imvec.setDebug(false);

            ImageVectorizationResult imvr = imvec.call();
            double[] vector = imvr.getImageVector();
            String vectorStr = Arrays.toString(vector);

            System.out.println(imvr.getImageName() + " : " + vector.length);

        }

        t = System.currentTimeMillis() - t;
        System.out.println(t + " msecs to extract features from " + imageFolder.list().length + " images");

    }
}
