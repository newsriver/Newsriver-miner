package ch.newsriver.miner;

import ch.newsriver.data.html.HTML;
import ch.newsriver.data.url.BaseURL;
import ch.newsriver.data.url.FeedURL;
import ch.newsriver.executable.BatchInterruptibleWithinExecutorPool;
import ch.newsriver.miner.cache.ResolvedURLs;
import ch.newsriver.miner.html.HTMLFetcher;
import ch.newsriver.miner.url.URLResolver;
import ch.newsriver.util.http.HttpClientPool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Created by eliapalme on 11/03/16.
 */
public class Miner extends BatchInterruptibleWithinExecutorPool implements Runnable {

    private static final Logger logger = LogManager.getLogger(Miner.class);
    private boolean run = false;
    private static final int BATCH_SIZE = 250;
    private static final int POOL_SIZE  = 50;
    private static final int QUEUE_SIZE = 500;

    private static final ObjectMapper mapper = new ObjectMapper();
    Consumer<String, String> consumer;
    Producer<String, String> producer;

    public Miner() throws IOException {

        super(POOL_SIZE,QUEUE_SIZE);
        run = true;

        try {
            HttpClientPool.initialize();
        } catch (NoSuchAlgorithmException e) {
            logger.fatal("Unable to initialize http connection pool",e);
            run = false;
            return;
        } catch (KeyStoreException e) {
            logger.error("Unable to initialize http connection pool",e);
            run = false;
            return;
        } catch (KeyManagementException e) {
            logger.error("Unable to initialize http connection pool",e);
            run = false;
            return;
        }

        Properties props = new Properties();
        InputStream inputStream = null;
        try {

            String propFileName = "kafka.properties";
            inputStream = Miner.class.getClassLoader().getResourceAsStream(propFileName);
            if (inputStream != null) {
                props.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }
        } catch (java.lang.Exception e) {
            logger.error("Unable to load kafka properties",e);
        } finally {
            try {
                inputStream.close();
            } catch (java.lang.Exception e) { }
        }


        consumer = new KafkaConsumer(props);
        consumer.subscribe(Arrays.asList("raw-urls"));
        producer = new KafkaProducer(props);

    }

    public void stop(){
        run = false;
        HttpClientPool.shutdown();
        this.shutdown();
        consumer.close();
        producer.close();
    }


    public void run() {

        while (run) {
                    try {
                        ConsumerRecords<String, String> records = consumer.poll(1000);
                        for (ConsumerRecord<String, String> record : records) {
                            this.waitFreeBatchExecutors(BATCH_SIZE);
                            MinerMain.addMetric("URLs in",1);

                            supplyAsyncInterruptWithin(() -> {
                                BaseURL referral = null;
                                try {
                                    referral = mapper.readValue(record.value(), BaseURL.class);
                                } catch (IOException e) {
                                    logger.error("Error deserialising BaseURL", e);
                                    return null;
                                }



                                String resolvedURL = ResolvedURLs.getInstance().getResolved(referral.getUlr());
                                if(resolvedURL==null) {
                                    try {
                                        resolvedURL = URLResolver.getInstance().resolveURL(referral.getUlr());
                                    } catch (URLResolver.InvalidURLException e) {
                                        logger.error("Unable to resolve URL", e);
                                        return null;
                                    }
                                    ResolvedURLs.getInstance().setResolved(referral.getUlr(),resolvedURL);
                                }

                                //here lookup to check if the article has already been imported
                                //Search in elastic search if URL exists if so get the document
                                //add the referral and pass it to the first filtering queue.


                                HTML html = new HTMLFetcher(resolvedURL,referral).fetch();
                                if(html!=null) {
                                    String json = null;
                                    try {
                                        json = mapper.writeValueAsString(html);
                                    } catch (IOException e) {
                                        logger.fatal("Unable to serialize miner result", e);
                                        return null;
                                    }
                                    producer.send(new ProducerRecord<String, String>("raw-html", html.getUrl(), json));

                                    try {
                                        URI uri = new URI(html.getUrl());
                                        producer.send(new ProducerRecord<String, String>("site-url", uri.getScheme()+"://"+uri.getHost(), json));
                                    }catch (URISyntaxException e){}

                                    MinerMain.addMetric("URLs out",1);

                                }
                                return null;
                            }, Duration.ofSeconds(60),this)
                                    .exceptionally(throwable -> {
                                        logger.error("HTMLFetcher unrecoverable error.", throwable);
                                        return null;
                                    });

                        }
                    } catch (InterruptedException ex) {
                        logger.warn("Miner job interrupted",ex);
                        run = false;
                        return;
                    }catch (BatchSizeException ex){
                        logger.fatal("Requested a batch size bigger than pool capability.");
                    }
                    continue;
                }



        }

}
