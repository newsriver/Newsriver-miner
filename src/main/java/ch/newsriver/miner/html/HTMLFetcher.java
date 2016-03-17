package ch.newsriver.miner.html;

import ch.newsriver.data.html.HTML;
import ch.newsriver.data.url.BaseURL;
import ch.newsriver.util.HTMLUtils;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Created by eliapalme on 14/03/16.
 */
public class HTMLFetcher {


    private static final Logger logger = LogManager.getLogger(HTMLFetcher.class);

    /*
    //Check if the article may does not already exist in db
    if (articleId == null) {
        try {
            articleId = URLLookup.getInstance().getUrlId(inputSource.getURL());
        } catch (MalformedURLException ex) {
            java.util.logging.Logger.getLogger(ArticleImporterJob.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }

    */
    /*
    //Try to clean URL by following redirects and removing google analytics campaign queries
    String URL = null;
    try {
        URL = URLResolver.getInstance().resolveUrl(rawURL);
    } catch (MalformedURLException ex) {
        logger.log(Level.WARN, "Unable to clean stand alone url. Article=" + rawURL + " " + ex.getMessage());
    } catch (URISyntaxException ex) {
        logger.log(Level.WARN, "Unable to clean stand alone url. Article=" + rawURL + " " + ex.getMessage());
    } catch (IOException ex) {
        logger.log(Level.WARN, "Unable to clean stand alone url. Article=" + rawURL + " " + ex.getMessage());
    } catch (IllegalArgumentException ex) {
        logger.log(Level.WARN, "Unable to clean stand alone url. Article=" + rawURL + " " + ex.getMessage());
    }
    if (URL == null) {
        logger.log(Level.WARN, "Unable to clean stand alone url. Artile=" + rawURL);
        throw new InvalidURLException("Invalid article url:" + rawURL);
    }

    String host=null;
    try {
        URI uri;
        uri = new URI(URL);
        host = uri.getHost();
    } catch (URISyntaxException ex) {
        logger.log(Level.WARN, "Unable to exctract host from URL", ex);
    }

    if (host == null) {
        logger.log(Level.WARN, "Unable to extract host from url. Artile=" + rawURL);
        throw new InvalidURLException("Invalid article url:" + rawURL);
    }

    if(bannedHosts.contains(host)){
        logger.log(Level.WARN, "Unable to import article, its domain is banned. Artile=" + rawURL);
        throw new InvalidURLException("Banned article url:" + rawURL);
    }*/

    private BaseURL urlToFetch;

    public HTMLFetcher(BaseURL fetchObjeect) {

        this.urlToFetch = fetchObjeect;
    }


    public HTML fetch() {
        try {
            String html =  HTMLUtils.getHTML(urlToFetch.getNormalizeURL(), false);
            HTML result = new HTML();
            result.setRawHTML(html);
            result.setRefferal(urlToFetch);
            return result;
        } catch (IOException e) {
            logger.error("Error running HTMLFetcher task", e);
        } catch (Exception e) {
            logger.error("Error running HTMLFetcher task", e);
        }
        return null;
    }


}
