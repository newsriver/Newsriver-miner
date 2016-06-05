package ch.newsriver.miner;

import ch.newsriver.data.html.HTML;
import ch.newsriver.data.url.BaseURL;
import ch.newsriver.data.url.ManualURL;
import ch.newsriver.data.url.SeedURL;
import ch.newsriver.executable.Main;
import ch.newsriver.miner.cache.DownloadedHTMLs;
import ch.newsriver.miner.html.HTMLFetcher;
import ch.newsriver.performance.MetricsLogger;
import ch.newsriver.processor.Output;
import ch.newsriver.processor.Processor;
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
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

/**
 * Created by eliapalme on 11/03/16.
 */
public class Miner extends Processor<BaseURL, HTML> implements Runnable {

    private static final Logger logger = LogManager.getLogger(Miner.class);
    private static final MetricsLogger metrics = MetricsLogger.getLogger(Miner.class, Main.getInstance().getInstanceName());


    private boolean run = false;
    private static int MAX_EXECUTUION_DURATION = 120;
    private int batchSize;
    private String priorityPostFix = "";
    private static final ObjectMapper mapper = new ObjectMapper();
    Consumer<String, String> consumer;
    Producer<String, String> producer;

    public Miner(int poolSize, int batchSize, int queueSize,boolean priority) throws IOException {

        super(poolSize, queueSize, Duration.ofSeconds(MAX_EXECUTUION_DURATION),priority);
        this.batchSize = batchSize;
        run = true;

        try {
            HttpClientPool.initialize();
        } catch (NoSuchAlgorithmException e) {
            logger.fatal("Unable to initialize http connection pool", e);
            run = false;
            return;
        } catch (KeyStoreException e) {
            logger.error("Unable to initialize http connection pool", e);
            run = false;
            return;
        } catch (KeyManagementException e) {
            logger.error("Unable to initialize http connection pool", e);
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
            logger.error("Unable to load kafka properties", e);
        } finally {
            try {
                inputStream.close();
            } catch (java.lang.Exception e) {
            }
        }

        if(isPriority()){
            priorityPostFix="-priority";
        }


        consumer = new KafkaConsumer(props);
        if(isPriority()) {
            consumer.subscribe(Arrays.asList("raw-urls" + priorityPostFix));
        }else{
            consumer.subscribe(Arrays.asList("raw-urls" + priorityPostFix, "seed-urls"));
        }

        producer = new KafkaProducer(props);

    }

    public void stop() {
        run = false;
        HttpClientPool.shutdown();
        this.shutdown();
        consumer.close();
        producer.close();
        metrics.logMetric("shutdown", null);
    }


    public void run() {
        metrics.logMetric("start", null);
        while (run) {
            try {
                this.waitFreeBatchExecutors(batchSize);
                //TODO: decide if we want to keep this.
                //metrics.logMetric("processing batch");

                ConsumerRecords<String, String> records;
                if(this.isPriority()){
                    records = consumer.poll(250);
                }else{
                    records = consumer.poll(60000);
                }



                for (ConsumerRecord<String, String> record : records) {


                    supplyAsyncInterruptExecutionWithin(() -> {


                        Output<BaseURL, HTML> output = this.process(record.value());
                        if (output.isSuccess()) {
                            String json = null;
                            try {
                                json = mapper.writeValueAsString(output.getOutput());
                            } catch (IOException e) {
                                logger.fatal("Unable to serialize miner result", e);
                                return null;
                            }
                            if (output.getIntput() instanceof ManualURL) {
                                producer.send(new ProducerRecord<String, String>("processing-status", ((ManualURL) output.getIntput()).getSessionId(), "HTML mining completed."));
                            }

                            if(output.getIntput() instanceof SeedURL){
                                producer.send(new ProducerRecord<String, String>("seed-html", output.getOutput().getUrl(), json));
                            }else{
                                producer.send(new ProducerRecord<String, String>("raw-html"+priorityPostFix, output.getOutput().getUrl(), json));
                            }



                        } else {
                            if (output.getIntput() instanceof ManualURL) {
                                producer.send(new ProducerRecord<String, String>("processing-status", ((ManualURL) output.getIntput()).getSessionId(), "Error unable to mine HTML."));
                            }
                        }
                        return null;



                    }, this)
                            .exceptionally(throwable -> {
                                logger.error("HTMLFetcher unrecoverable error.", throwable);
                                return null;
                            });

                }
           } catch (InterruptedException ex) {
                logger.warn("Miner job interrupted", ex);
                run = false;
                return;
            } catch (BatchSizeException ex) {
                logger.fatal("Requested a batch size bigger than pool capability.");
            }
            continue;
        }

    }

    protected Output<BaseURL, HTML> implProcess(String data) {

        Output<BaseURL, HTML> output = new Output<>();

        BaseURL referral = null;
        MinerMain.addMetric("URLs in", 1);
        try {
            referral = mapper.readValue(data, BaseURL.class);
        } catch (IOException e) {
            logger.error("Error deserialising BaseURL", e);
            return output;
        }

        output.setIntput(referral);
        metrics.logMetric("processing url", referral);
        boolean isSeed = referral instanceof SeedURL;
        boolean reuiresAjaxCrawling = false;

        /*TODO: need to be completed we may need to add a Redis layer to the WebSiteFactory
                            URI pageURL = null;
                            try {
                                pageURL = new URI(referral.getUlr());
                            } catch (URISyntaxException e) {
                                logger.warn("Inavlid URL submitted to miner", e);
                            }

                            WebSite website = WebSiteFactory.getInstance().getWebsite(pageURL.getHost());

                            if (website != null) {
                                reuiresAjaxCrawling = website.isAjaxBased();
                            }
        */

        if (!DownloadedHTMLs.getInstance().isDownloaded(referral.getUrl()) || isSeed) {
            HTML html = new HTMLFetcher(referral.getUrl(), referral, reuiresAjaxCrawling).fetch();
            if (html != null) {
                output.setOutput(html);
                output.setSuccess(true);
                DownloadedHTMLs.getInstance().setDownloaded(referral.getUrl());
                metrics.logMetric("submitted html", referral);
                MinerMain.addMetric("URLs out", 1);

            }else{
                output.setSuccess(false);
            }
        } else {
            HTML html = new HTML();
            html.setAlreadyFetched(true);
            html.setReferral(referral);
            html.setUrl(referral.getUrl());
            output.setOutput(html);
            output.setSuccess(true);
            output.setUpdate(true);
            metrics.logMetric("submitted html update", referral);
            MinerMain.addMetric("URLs out", 1);
        }


        return output;
    }
}
