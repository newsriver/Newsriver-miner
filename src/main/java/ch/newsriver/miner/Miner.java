package ch.newsriver.miner;

import ch.newsriver.data.html.HTML;
import ch.newsriver.data.url.BaseURL;
import ch.newsriver.data.url.SeedURL;
import ch.newsriver.miner.cache.DownloadedHTMLs;
import ch.newsriver.miner.html.HTMLFetcher;
import ch.newsriver.util.http.HttpClientPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by eliapalme on 11/03/16.
 */
public class Miner {

    private static final Logger logger = LogManager.getLogger(Miner.class);


    public Miner() {


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
        boolean requiresAjaxCrawling = false;
        boolean extractDynamicLinks = isSeed; //if the source is a SeedURL we will extract the dynamic links from it

        //TODO: replace this with proper solution
        //Like search for the website in elasticsearch and detecting if ajax is needed.
        if (referral.getUrl().contains("sonntagszeitung.ch")) {
            requiresAjaxCrawling = true;
        }
        if (referral.getUrl().contains("swissquote.ch")) {
            requiresAjaxCrawling = true;
        }
        if (referral.getUrl().contains("aufeminin.com")) {
            requiresAjaxCrawling = true;
        }
        if (referral.getUrl().contains("boleromagazin.ch")) {
            requiresAjaxCrawling = true;
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
            HTML html = new HTMLFetcher(referral.getUrl(), referral, requiresAjaxCrawling, extractDynamicLinks).fetch();
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
