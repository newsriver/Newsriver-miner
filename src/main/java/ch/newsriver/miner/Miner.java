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
public class Miner {

    private static final Logger logger = LogManager.getLogger(Miner.class);


    public Miner(){


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
    }



    public HTML process(BaseURL referral) {

        boolean isSeed = referral instanceof SeedURL;
        boolean reuiresAjaxCrawling = false;


        //TODO: replace this with proper solution
        //Like search for the website in elasticsearch and detecting if ajax is needed.
        if(referral.getUrl().contains("sonntagszeitung.ch")){
            reuiresAjaxCrawling=true;
        }
        if(referral.getUrl().contains("swissquote.ch")){
            reuiresAjaxCrawling=true;
        }
        if(referral.getUrl().contains("aufeminin.com")){
            reuiresAjaxCrawling=true;
        }
        if(referral.getUrl().contains("boleromagazin.ch")){
            reuiresAjaxCrawling=true;
        }


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
                DownloadedHTMLs.getInstance().setDownloaded(referral.getUrl());
                return html;
            }
        } else {
            HTML html = new HTML();
            html.setAlreadyFetched(true);
            html.setReferral(referral);
            html.setUrl(referral.getUrl());
           return html;
        }
        return null;
    }
}
