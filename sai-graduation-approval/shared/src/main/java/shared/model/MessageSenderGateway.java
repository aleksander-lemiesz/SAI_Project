package shared.model;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class MessageSenderGateway {
    Connection connection = null; // to connect to the ActiveMQ
    Session session = null; // session for creating messages, producers and

    Destination sendDestination = null; // reference to a queue/topic destination
    MessageProducer producer = null; // for sending messages

    public MessageSenderGateway(String queue) {
        try {
            ConnectionFactory connectionFactory;
            connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");

            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // connect to the sender destination (i.e., queue “myFirstDestination”)
            sendDestination = session.createQueue(queue);
            producer = session.createProducer(sendDestination);

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public MessageSenderGateway() {
        try {
            ConnectionFactory connectionFactory;
            connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");

            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // connect to the sender destination (i.e., queue “myFirstDestination”)
            sendDestination = null;
            producer = session.createProducer(sendDestination);

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public Message createTextMessage(String body) {
        // create a text message
        Message msg = null;
        try {
            msg = session.createTextMessage(body);
        } catch (JMSException e) {
            e.printStackTrace();
        }
        return msg;
    }

    public void send(Message msg) throws JMSException {
        // send the message
        producer.send(msg);
    }

    public void send(Message msg, Destination dest) throws JMSException {
        // send the message
        producer.send(dest, msg);
    }

    public void stop() {
        try {
            if (producer != null) {
                producer.close();
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
