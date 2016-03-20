package ch.newsriver.miner.html;

import ch.newsriver.data.html.HTML;
import ch.newsriver.data.url.BaseURL;
import ch.newsriver.util.HTMLUtils;
import com.google.common.base.Optional;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.List;


/**
 * Created by eliapalme on 14/03/16.
 */
public class HTMLFetcher {


    private static final Logger logger = LogManager.getLogger(HTMLFetcher.class);

    /*
    //Check if the Article may does not already exist in db
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
        throw new InvalidURLException("Invalid Article url:" + rawURL);
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
        throw new InvalidURLException("Invalid Article url:" + rawURL);
    }

    if(bannedHosts.contains(host)){
        logger.log(Level.WARN, "Unable to import Article, its domain is banned. Artile=" + rawURL);
        throw new InvalidURLException("Banned Article url:" + rawURL);
    }*/

    private BaseURL urlToFetch;
    private static  LanguageDetector languageDetector=null;

    static {
        try {
            List<LanguageProfile> languageProfiles = new LanguageProfileReader().readAllBuiltIn();
            languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
                    .withProfiles(languageProfiles)
                    .build();
        }catch (IOException ex){
            logger.fatal("Unable to initialize language detector",ex);
        }
    }


    public HTMLFetcher(BaseURL fetchObjeect) {

        this.urlToFetch = fetchObjeect;
    }


    public HTML fetch() {
        try {
            String htmlStr =  HTMLUtils.getHTML(urlToFetch.getNormalizeURL(), false);
            HTML html = new HTML();
            html.setRawHTML(htmlStr);
            html.setReferral(urlToFetch);
            html.setUrl(urlToFetch.getNormalizeURL());

            String pageText = Jsoup.parseBodyFragment(html.getRawHTML()).body().text();
            TextObjectFactory textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();
            TextObject textObject = textObjectFactory.forText(pageText);
            Optional<LdLocale> lang = languageDetector.detect(textObject);
            if(lang.isPresent()){
                html.setLanguage(lang.get().getLanguage());
            }


            return html;
        } catch (IOException e) {
            logger.error("Error running HTMLFetcher task", e);
        } catch (Exception e) {
            logger.error("Error running HTMLFetcher task", e);
        }
        return null;
    }


}
