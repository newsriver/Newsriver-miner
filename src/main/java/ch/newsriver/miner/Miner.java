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
import ch.newsriver.processor.StreamProcessor;
import ch.newsriver.util.http.HttpClientPool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.streams.KeyValue;
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
import java.util.List;
import java.util.Properties;

/**
 * Created by eliapalme on 11/03/16.
 */
public class Miner  implements StreamProcessor<BaseURL, HTML> {

    private static final Logger logger = LogManager.getLogger(Miner.class);
    private static final MetricsLogger metrics = MetricsLogger.getLogger(Miner.class, Main.getInstance().getInstanceName());



    private String priorityPostFix = "";


    public Miner() throws IOException {

        try {
            HttpClientPool.initialize();
        } catch (NoSuchAlgorithmException e) {
            logger.fatal("Unable to initialize http connection pool", e);
            return;
        } catch (KeyStoreException e) {
            logger.error("Unable to initialize http connection pool", e);
            return;
        } catch (KeyManagementException e) {
            logger.error("Unable to initialize http connection pool", e);
            return;
        }

    }

    public void close() {

        HttpClientPool.shutdown();
        metrics.logMetric("shutdown", null);
    }





    @Override
    public KeyValue<String, HTML> process(String key, BaseURL referral) {


        HTML output = null;

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
                output= html;
                DownloadedHTMLs.getInstance().setDownloaded(referral.getUrl());
                metrics.logMetric("submitted html", referral);
                MinerMain.addMetric("URLs out", 1);

            }
        } else {
            HTML html = new HTML();
            html.setAlreadyFetched(true);
            html.setReferral(referral);
            html.setUrl(referral.getUrl());
            output= html;
            metrics.logMetric("submitted html update", referral);
            MinerMain.addMetric("URLs out", 1);
        }

        return new KeyValue(key,output);

    }



}
