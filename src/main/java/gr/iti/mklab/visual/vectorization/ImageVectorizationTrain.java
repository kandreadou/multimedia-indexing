package gr.iti.mklab.visual.vectorization;

import georegression.struct.point.Point2D_F64;
import gr.iti.mklab.visual.aggregation.VladAggregatorMultipleVocabularies;
import gr.iti.mklab.visual.dimreduction.PCA;
import gr.iti.mklab.visual.experiments.CreateWekaFile;
import gr.iti.mklab.visual.extraction.AbstractFeatureExtractor;
import gr.iti.mklab.visual.extraction.ImageScaling;
import gr.iti.mklab.visual.extraction.SURFExtractor;
import gr.iti.mklab.visual.utilities.ImageIOGreyScale;
import ij.gui.Roi;
import weka.classifiers.functions.SMO;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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

    private static CostSensitiveClassifier svm;

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

    public void paintCircleOnPoint(BufferedImage bi, int x, int y) {

        Graphics2D g2 = bi.createGraphics();

        // draw a circle with the same center
        double centerX = x;
        double centerY = y;
        double radius = 5;

        Ellipse2D circle = new Ellipse2D.Double();
        circle.setFrameFromCenter(centerX, centerY, centerX + radius, centerY + radius);
        g2.setPaint(Color.RED);
        g2.fill(circle);
        g2.draw(circle);
    }

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

        //Don't scale when training!!

        // next the local features are extracted
        double[][] features = featureExtractor.extractFeatures(image);

        Point2D_F64[] points = ((SURFExtractor) featureExtractor).points;

        ArrayList<Attribute> attributes = new ArrayList<Attribute>();
        for (int i = 0; i < 64; i++) {
            attributes.add(new Attribute("surf" + i));
        }
        ArrayList fvClassVal = new ArrayList<String>(2);
        fvClassVal.add("OUT");
        fvClassVal.add("IN");
        Attribute classAttribute = new Attribute("class", fvClassVal);
        attributes.add(classAttribute);
        // predict instance class values
        Instances data = new Instances("Test dataset", attributes, features.length);
        data.setClassIndex(64);

        for (int i = 0, len = features.length; i < len; i++) {
            double[] descriptor = features[i];
            // add data to instance
            data.add(new DenseInstance(1.0, descriptor));
        }

        ArrayList<double[]> filtered = new ArrayList<double[]>(features.length);
        ArrayList<Desc> outliers = new ArrayList<Desc>();

        for (int i = 0, len = data.numInstances(); i < len; i++) {
            // perform prediction
            Instance inst = data.instance(i);
            double[] distribution = svm.distributionForInstance(inst);
            if (distribution[0] > 0.7 && distribution[1] < 0.3) {
                int x = (int) points[i].x;
                int y = (int) points[i].y;
                outliers.add(new Desc(x, y, features[i]));
                //paintCircleOnPoint(image, x, y);
                continue;
            } else {
                filtered.add(features[i]);
            }
            //double myValue = svm.classifyInstance(inst);
            // get the name of class value
            //String label = data.classAttribute().value((int)myValue);
            //if("IN".equals(label)){
            //   filtered.add(features[i]);
            //}
            //int realvalue = (int) inst.classValue();
            //System.out.println("Prediction for instance " + i + " value: " + myValue + " prediction " + label);

        }

        //Remove additionally the descriptors that correpsond to points that are
        // not close to each other
        //int NUM_TILES = image.getWidth() > 800 || image.getHeight() > 800? 18 : 9;
        int widthStep = image.getWidth() > 800 || image.getHeight()>800 ? 50 : 30;
        int heightStep = image.getWidth() > 800 || image.getHeight()>800 ? 50 : 30;
        for (int i = 0; i < image.getWidth(); i += widthStep) {
            for (int k = 0; k < image.getHeight(); k += heightStep) {
                ArrayList<Desc> descInArea = new ArrayList<Desc>();
                Rectangle rec = new Rectangle(i, k, i + widthStep, k + heightStep);
                //System.out.println("##new tile## " + i + " " + k + " " + (i + widthStep) + " " + (k + heightStep));
                int density = 0;
                for (Iterator<Desc> iterator = outliers.iterator(); iterator.hasNext(); ) {
                    Desc item = iterator.next();
                    if (rec.contains(item.x, item.y)) {
                        density++;
                        descInArea.add(item);
                        iterator.remove();
                    }
                }
                //System.out.println("density for tile " + density);
                if (density < 8) {
                    for (Desc item : descInArea) {
                        filtered.add(item.vector);
                        //outliers.remove(item);
                    }
                }
            }
        }

        /*for (Desc item : outliers) {
            paintCircleOnPoint(image, item.x, item.y);
        }*/

        filtered.trimToSize();
        double[][] featuresFiltered = filtered.toArray(new double[filtered.size()][64]);
        //System.out.println("initial length "+features.length+" final length "+featuresFiltered.length);




        /*for (int i = 0, len = points.length; i < len; i++) {
            int x = (int) points[i].x;
            int y = (int) points[i].y;
            for (int k = 0; k < features[i].length; k++) {
                bw.write(features[i][k] + ",");
            }

            boolean foundRoi = false;
            if (rois != null) {
                for (Roi roi : rois) {
                    if (roi.contains(x, y)) {
                        bw.write("OUT");
                        foundRoi = true;
                        paintCircleOnPoint(image, x,y);
                        CreateWekaFile.NUM_FONT_DESCRIPTORS++;
                        break;
                    }
                }
            }
            if (!foundRoi) {
                bw.write("IN");
                CreateWekaFile.NUM_CONTENT_DESCRIPTORS++;
            }
            bw.newLine();
        }*/

        /*try {
            String folder = "/home/kandreadou/Pictures/showcase/";
            ImageIO.write(image, "JPEG", new File(folder, imageFilename));
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        // next the features are aggregated
        double[] vladVector = vladAggregator.aggregate(featuresFiltered);

        if (vladVector.length == vectorLength) {
            // no projection is needed
            return vladVector;
        } else {
            // pca projection is applied
            double[] projected = pcaProjector.sampleToEigenSpace(vladVector);
            return projected;
        }
    }

    class Desc {
        int x;
        int y;
        double[] vector;

        public Desc(int x, int y, double[] vector) {
            this.x = x;
            this.y = y;
            this.vector = vector;
        }
    }


    /**
     * Transforms the image into a vector and returns the result.
     *
     * @return The image's vector.
     * @throws Exception
     */
    public double[] transformToVectorRight() throws Exception {
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

        //Don't scale when training!!

        // next the local features are extracted
        double[][] features = featureExtractor.extractFeatures(image);

        Point2D_F64[] points = ((SURFExtractor) featureExtractor).points;

        for (int i = 0, len = points.length; i < len; i++) {
            int x = (int) points[i].x;
            int y = (int) points[i].y;
            for (int k = 0; k < features[i].length; k++) {
                bw.write(features[i][k] + ",");
            }

            boolean foundRoi = false;
            if (rois != null) {
                for (Roi roi : rois) {
                    if (roi.contains(x, y)) {
                        bw.write("OUT");
                        foundRoi = true;
                        paintCircleOnPoint(image, x, y);
                        CreateWekaFile.NUM_FONT_DESCRIPTORS++;
                        break;
                    }
                }
            }
            if (!foundRoi) {
                bw.write("IN");
                CreateWekaFile.NUM_CONTENT_DESCRIPTORS++;
            }
            bw.newLine();
        }

        try {
            String folder = "/home/kandreadou/Desktop/imagesWithDesc/";
            ImageIO.write(image, "JPEG", new File(folder, imageFilename));
        } catch (IOException e) {
            e.printStackTrace();
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

    public static void loadClassifier(){
        try{
        svm = (CostSensitiveClassifier) weka.core.SerializationHelper.read("/home/kandreadou/Desktop/classifier_training/models/correctRF32trees30cost5folds.model");
        }catch(Exception ex){
            System.out.println(ex);
        }
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
