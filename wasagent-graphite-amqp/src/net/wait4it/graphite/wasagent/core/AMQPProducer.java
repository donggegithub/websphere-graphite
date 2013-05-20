/**
 * This file is part of Wasagent.
 *
 * Wasagent is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wasagent is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Wasagent. If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package net.wait4it.graphite.wasagent.core;

import java.io.FileInputStream;
import java.io.IOException;

import java.util.Properties;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

/**
 * @author Yann Lambret
 *
 */
public class AMQPProducer {

    private static FileInputStream stream = null;
    private static Properties props = new Properties();
    private static ConnectionFactory factory = new ConnectionFactory();

    private Connection connection;
    private Channel channel;


    static {
        try {
            // Loads the AMQP broker configuration file
            stream = new FileInputStream("broker.properties");
            props.load(stream);

            // Sets the connection factory for the target broker
            factory.setHost(props.getProperty("AMQP_HOST"));
            factory.setPort(Integer.parseInt(props.getProperty("AMQP_PORT")));
            factory.setVirtualHost(props.getProperty("AMQP_VHOST"));
            factory.setUsername(props.getProperty("AMQP_USER"));
            factory.setPassword(props.getProperty("AMQP_PASSWORD"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (stream != null)
                    stream.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Default constructor.
     */
    public AMQPProducer() {}

    /**
     * Sends the collected performance data to the AMQP
     * broker defined in the 'broker.properties' file.
     * We are using the default durable topic exchange
     * created by carbon at runtime.
     * 
     * @param  message metrics data in graphite format
     * @throws Exception
     */
    public void send(String message) throws Exception {
        // Gets a connection and set a durable topic exchange
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.exchangeDeclare(props.getProperty("AMQP_EXCHANGE"), "topic", true);

        // Sends the message and cleans up resources
        channel.basicPublish(props.getProperty("AMQP_EXCHANGE"), "", null, message.getBytes());
        channel.close();
        connection.close();
    }

}
