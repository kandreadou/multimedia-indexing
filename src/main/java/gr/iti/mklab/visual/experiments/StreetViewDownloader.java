package gr.iti.mklab.visual.experiments;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by kandreadou on 4/30/14.
 */
public class StreetViewDownloader {

    private final static String imageFolder = "/home/kandreadou/Pictures/streetview/";
    private final static String API_KEY = "AIzaSyA9TZ-eOfPLHJCQEVZNsNbrJWl2CyTlXok";
    private final static int LENGTH = 15000;

    public static void main(String[] args) {
        download();
    }

    private static void download() {
        double lat1 = 48.837379;
        double lon1 = 2.282352;
        double lat2 = 48.891358;
        double lon2 = 2.394619;

        long start = System.currentTimeMillis();
        int count = 0;

        for (double x = lat1; x < lat2; x += 0.0001) {
            for (double y = lon1; y < lon2; y += 0.0001) {
                try {

                    BigDecimal bd = new BigDecimal(x);
                    bd = bd.setScale(6, BigDecimal.ROUND_HALF_UP);
                    double lat = bd.doubleValue();
                    bd = new BigDecimal(y);
                    bd = bd.setScale(6, BigDecimal.ROUND_HALF_UP);
                    double lon = bd.doubleValue();
                    System.out.println("trying for " + lat + " " + lon);
                    URL myUrl = new URL("http://maps.googleapis.com/maps/api/streetview?size=640x640&location=" + lat + "," + lon + "&sensor=false&key=" + API_KEY);
                    URLConnection connection = myUrl.openConnection();
                    if (connection.getContentLength() > LENGTH) {
                        BufferedImage image = ImageIO.read(myUrl);
                        ImageIO.write(image, "JPEG", new File(imageFolder + lat + lon + ".jpg"));
                        count++;
                        if (count > 23000) {
                            break;
                        }
                    }

                } catch (MalformedURLException ex) {
                    System.out.println(ex);
                } catch (IOException ioex) {
                    System.out.println(ioex);
                }
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("global time: " + (end - start));
        System.out.println("number of images " + count);
        System.out.println("throughput " + ((end - start) / count));
    }
}
