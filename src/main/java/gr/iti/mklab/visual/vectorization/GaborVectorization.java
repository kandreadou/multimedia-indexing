package gr.iti.mklab.visual.vectorization;

import gr.iti.mklab.visual.aggregation.VladAggregatorMultipleVocabularies;
import gr.iti.mklab.visual.dimreduction.PCA;
import gr.iti.mklab.visual.extraction.AbstractFeatureExtractor;
import gr.iti.mklab.visual.extraction.ImageScaling;
import gr.iti.mklab.visual.extraction.SURFExtractor;
import gr.iti.mklab.visual.utilities.ImageIOGreyScale;
import net.semanticmetadata.lire.imageanalysis.Gabor;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * Created by kandreadou on 4/24/14.
 */
public class GaborVectorization implements Callable<ImageVectorizationResult> {

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
    public GaborVectorization(String imageFolder, String imageFilename, int vectorLength,
                              int maxImageSizeInPixels) {
        this.imageFolder = imageFolder;
        this.imageFilename = imageFilename;
        this.vectorLength = vectorLength;
        this.maxImageSizeInPixels = maxImageSizeInPixels;
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
    public GaborVectorization(String imageFilename, BufferedImage image, int vectorLength,
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

    public double[] transformToVector() throws Exception {
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
        // next the image is scaled
        ImageScaling scale = new ImageScaling(maxImageSizeInPixels);
        image = scale.maxPixelsScaling(image);

        Gabor gabor = new Gabor();
        double[] gaborFeature = gabor.getFeature(image);

        return gaborFeature;
    }

    /**
     * Sets the FeatureExtractor object that will be used.
     *
     * @param extractor
     */
    public static void setFeatureExtractor(AbstractFeatureExtractor extractor) {
        GaborVectorization.featureExtractor = extractor;
    }

    /**
     * Sets the VladAggregatorMultipleVocabularies object that will be used.
     *
     * @param vladAggregator
     */
    public static void setVladAggregator(VladAggregatorMultipleVocabularies vladAggregator) {
        GaborVectorization.vladAggregator = vladAggregator;
    }

    /**
     * Sets the PCA projection object that will be used.
     *
     * @param pcaProjector
     */
    public static void setPcaProjector(PCA pcaProjector) {
        GaborVectorization.pcaProjector = pcaProjector;
    }

}