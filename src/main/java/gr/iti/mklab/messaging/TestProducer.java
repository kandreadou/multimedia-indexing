package gr.iti.mklab.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

/**
 * Created by kandreadou on 3/11/14.
 */
public class TestProducer {

    private final static String RABBITMQ_HOST = "localhost";
    private final static String EXCHANGE_NAME = "multimedia indexing";

    public static void main(String[] argv) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(RABBITMQ_HOST);
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            //channel.basicQos(1);
            channel.exchangeDeclare(EXCHANGE_NAME, "fanout");

            //do a loop here and create messages for images
            String message1 = "100200.jpg";
            String message2 = "100201.jpg";
            String message3 = "100400.jpg";

            // PERSISTENT_TEXT_PLAIN makes sure the message is not lost even if the server dies and restarts
            channel.basicPublish(EXCHANGE_NAME, "", MessageProperties.PERSISTENT_TEXT_PLAIN, message1.getBytes());
            System.out.println(" [x] Sent '" + message1 + "'");

            channel.basicPublish(EXCHANGE_NAME, "", MessageProperties.PERSISTENT_TEXT_PLAIN, message2.getBytes());
            System.out.println(" [x] Sent '" + message2 + "'");

            channel.basicPublish(EXCHANGE_NAME, "", MessageProperties.PERSISTENT_TEXT_PLAIN, message3.getBytes());
            System.out.println(" [x] Sent '" + message3 + "'");

            channel.close();
            connection.close();
        } catch (Exception ex) {

        }
    }

}
