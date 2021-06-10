package shared.model;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class MessageReceiverGateway {

    Connection connection = null; // to connect to the JMS
    Session session = null; // session for creating consumers

    Destination receiveDestination; //reference to a queue/topic destination
    MessageConsumer consumer = null; // for receiving messages

    public MessageReceiverGateway(String queue) {
        try {
            ConnectionFactory connectionFactory;
            connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");

            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // connect to the receiver destination (i.e., queue “myFirstDestination”)
            receiveDestination = session.createQueue(queue);

            consumer = session.createConsumer(receiveDestination);
            connection.start();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public void setListener(MessageListener ml) {
        try {
            consumer.setMessageListener(ml);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public Destination getDestination() {
        return receiveDestination;
    }

    public void stop() {
        try {
            if (consumer != null) {
                consumer.close();
            }

            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

}
