package gr.iti.mklab.messaging;

import com.rabbitmq.client.*;

import java.io.IOException;

/**
 * Created by kandreadou on 3/11/14.
 */
public class RabbitMQWorker extends Worker {

    private final static String RABBITMQ_HOST = "localhost";
    private final static String EXCHANGE_NAME = "multimedia";

    public RabbitMQWorker() {
        Connection connection = null;
        Channel channel = null;
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(RABBITMQ_HOST);

            connection = factory.newConnection();
            channel = connection.createChannel();

            channel.queueDeclare(EXCHANGE_NAME, false, false, false, null);

            channel.basicQos(1);
            channel.queuePurge(EXCHANGE_NAME);

            QueueingConsumer consumer = new QueueingConsumer(channel);
            channel.basicConsume(EXCHANGE_NAME, false, consumer);

            System.out.println(" [x] Awaiting RPC requests");

            while (true) {
                String response = null;

                QueueingConsumer.Delivery delivery = consumer.nextDelivery();

                AMQP.BasicProperties props = delivery.getProperties();
                BasicProperties replyProps = new AMQP.BasicProperties.Builder()
                        .correlationId(props.getCorrelationId())
                        .build();

                try {
                    String message = new String(delivery.getBody(), "UTF-8");
                    System.out.println("Server received "+message);
                    doWork(message);
                    response = "Work done for "+message;
                } catch (Exception e) {
                    System.out.println("Server exception " + e.toString());
                    response = "";
                } finally {

                    channel.basicPublish("", props.getReplyTo(), (AMQP.BasicProperties) replyProps, response.getBytes("UTF-8"));

                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception ignore) {
                }
            }
        }

    }

    public static void main(String[] args) throws Exception {
        RabbitMQWorker worker = new RabbitMQWorker();
    }

}
