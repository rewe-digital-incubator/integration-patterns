package com.rewedigital.examples.msintegration.productinformation.helper.kafka;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.commons.io.FileUtils;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rewedigital.examples.msintegration.productinformation.helper.FreePortFinder;

import kafka.admin.AdminUtils;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;

public class KafkaServer {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaServer.class);
    private static KafkaLocal kafkaLocal;
    private static ZooKeeperLocal zookeeper;
    private static Integer kafkaPort = FreePortFinder.getFreePort(FreePortFinder.portRange(32000,32100));
    private static Integer zookeeperPort = FreePortFinder.getFreePort(FreePortFinder.portRange(32101,32200));

    /**
     * Starts a local Kafka Server (inlcudes zookeper and kafka) , if it has not been started already.
     * FIXME: currently, application-test config assumes port 32000; if server starts on other port, tests will fail!
     * @return The port of the zookeeper server.
     */
    public static Integer startKafkaServer(final String topicName) {
        if (null == kafkaLocal) {
            kafkaLocal = provideKafkaServer();
            createTopicWithThreePartitions(topicName);
        }

        return zookeeperPort;
    }

    /**
     * Shuts down the Kafka Server (includes zookeeper and kafka).
     * <p>
     * HINT: currently not used because the resource is freed after the end of all tests.
     */
    public static void stopKafkaServer() {
        kafkaLocal.stop();
        zookeeper.shutdown();
    }

    public static KafkaLocal provideKafkaServer() {
        final Properties kafkaProperties = new Properties();
        kafkaProperties.put("zookeeper.connect", "localhost:" + String.valueOf(zookeeperPort));
        kafkaProperties.put("port", String.valueOf(kafkaPort));
        kafkaProperties.put("broker.id", "0");
        kafkaProperties.put("host.name", "localhost");
        kafkaProperties.put("num.partitions", "1");
        kafkaProperties.put("default.replication.factor", "1");
        kafkaProperties.put("zookeeper.connection.timeout.ms", "10000");
        kafkaProperties.put("log.dirs", "./target/data/kafka");
        kafkaProperties.put("auto.create.topics.enable", "true");
        kafkaProperties.put("auto.commit.enable", "false");

        try {
            FileUtils.deleteDirectory(new File((String) kafkaProperties.get("log.dirs")));

            final ZooKeeperLocal zooKeeperLocal = provideZooKeeperLocal();
            final KafkaLocal kafka = new KafkaLocal(kafkaProperties, zooKeeperLocal);
            kafka.start();
            Thread.sleep(2000);
            return ((kafka));

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("KafkaServer could not start: " + e.getMessage(), e);
        }
    }

    public static ZooKeeperLocal provideZooKeeperLocal() {
        final Properties zkProperties = new Properties();
        zkProperties.put("clientPort", String.valueOf(zookeeperPort));
        zkProperties.put("maxClientCnxns", "10");
        zkProperties.put("dataDir", "./target/data/zookeeper");

        try {
            FileUtils.deleteDirectory(new File((String) zkProperties.get("dataDir")));

            zookeeper = new ZooKeeperLocal(zkProperties);
            zookeeper.start();
            return zookeeper;

        } catch (final IOException e) {
            throw new RuntimeException("Zookeeper Server could not start: " + e.getMessage(), e);
        }
    }

    public static void createTopicWithThreePartitions(final String topicName) {
        final Integer sessionTimeoutMs = 10000;
        final Integer connectionTimeoutMs = 10000;
        final ZkConnection zkConnection = new ZkConnection("localhost:" + String.valueOf(zookeeperPort), sessionTimeoutMs);
        final ZkClient zkClient = new ZkClient(zkConnection, connectionTimeoutMs, ZKStringSerializer$.MODULE$);
        final ZkUtils zkUtils = new ZkUtils(zkClient, zkConnection, false);

        final Integer numPartitions = 3;
        final Integer replicationFactor = 1;
        final Properties topicConfig = new Properties();

        try {
            LOG.info("Deleting topic " + topicName);
            AdminUtils.deleteTopic(zkUtils, topicName);
        } catch (final UnknownTopicOrPartitionException e) {
            LOG.info("Topic was not existing.");
        }
        LOG.info("Creating topic " + topicName);
        AdminUtils.createTopic(zkUtils, topicName, numPartitions, replicationFactor, topicConfig, null);

        zkClient.close();
    }

    public static KafkaLocal getKafkaLocal() {
        return kafkaLocal;
    }

    public static void setKafkaLocal(final KafkaLocal kafkaLocal) {
        KafkaServer.kafkaLocal = kafkaLocal;
    }

    public static ZooKeeperLocal getZookeeper() {
        return zookeeper;
    }

    public static void setZookeeper(final ZooKeeperLocal zookeeper) {
        KafkaServer.zookeeper = zookeeper;
    }

    public static Integer getKafkaPort() {
        return kafkaPort;
    }

    public static void setKafkaPort(final Integer kafkaPort) {
        KafkaServer.kafkaPort = kafkaPort;
    }

    public static Integer getZookeeperPort() {
        return zookeeperPort;
    }

    public static void setZookeeperPort(final Integer zookeeperPort) {
        KafkaServer.zookeeperPort = zookeeperPort;
    }

}