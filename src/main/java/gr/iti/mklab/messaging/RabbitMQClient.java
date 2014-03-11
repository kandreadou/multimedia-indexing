package gr.iti.mklab.messaging;

import com.rabbitmq.client.*;

import java.util.UUID;

/**
 * Created by kandreadou on 3/11/14.
 */
public class RabbitMQClient {
    private Connection connection;
    private Channel channel;
    private String requestQueueName = "multimedia";
    private String replyQueueName;
    private QueueingConsumer consumer;

    public RabbitMQClient() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.queuePurge(requestQueueName);
        replyQueueName = channel.queueDeclare().getQueue();
        consumer = new QueueingConsumer(channel);
        channel.basicConsume(replyQueueName, true, consumer);
    }

    public String call(String message) throws Exception {
        String response = null;
        String corrId = UUID.randomUUID().toString();

        BasicProperties props = new AMQP.BasicProperties.Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();

        channel.basicPublish("", requestQueueName, (AMQP.BasicProperties) props, message.getBytes());

        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                response = new String(delivery.getBody(),"UTF-8");
                break;
            }
        }

        return response;
    }

    public void close() throws Exception {
        connection.close();
    }

    public static void main(String[] argv) {
        RabbitMQClient rpc = null;
        String response = null;
        try {
            rpc = new RabbitMQClient();

            System.out.println(" [x] Requesting 100400.jpg");
            response = rpc.call("100400.jpg");
            System.out.println(" [.] Got '" + response + "'");
            System.out.println(" [x] Requesting 104001.jpg");
            response = rpc.call("100401.jpg");
            System.out.println(" [.] Got '" + response + "'");
            System.out.println(" [x] Requesting 104002.jpg");
            response = rpc.call("100402.jpg");
            System.out.println(" [.] Got '" + response + "'");
        }
        catch  (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (rpc!= null) {
                try {
                    rpc.close();
                }
                catch (Exception ignore) {}
            }
        }
    }
}
