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
    private static LanguageDetector languageDetector = null;

    static {
        try {
            List<LanguageProfile> languageProfiles = new LanguageProfileReader().readAllBuiltIn();
            languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
                    .withProfiles(languageProfiles)
                    .build();
        } catch (IOException ex) {
            logger.fatal("Unable to initialize language detector", ex);
        }
    }

    private BaseURL referral;
    private String resolvedURL;
    private boolean ajaxBased;
    private boolean extractDynamicLinks;


    public HTMLFetcher(String resolvedURL, BaseURL referral, boolean ajaxBased, boolean extractDynamicLinks) {
        this.referral = referral;
        this.resolvedURL = resolvedURL;
        this.ajaxBased = ajaxBased;
        this.extractDynamicLinks = extractDynamicLinks;
    }


    public HTML fetch() {
        try {

            HTML html;
            if (ajaxBased) {
                html = HTMLUtils.getAjaxBasedHTML(this.resolvedURL, extractDynamicLinks);
            } else {
                html = HTMLUtils.getHTML(this.resolvedURL, false);
            }

            if (html == null) {
                return null;
            }

            html.setReferral(this.referral);
            html.setUrl(this.resolvedURL);

            String pageText = Jsoup.parseBodyFragment(html.getRawHTML()).body().text();
            TextObjectFactory textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();
            TextObject textObject = textObjectFactory.forText(pageText);
            Optional<LdLocale> lang = languageDetector.detect(textObject);
            if (lang.isPresent()) {
                html.setLanguage(lang.get().getLanguage());
            }


            return html;
        } catch (IOException e) {
            logger.error("Error running HTMLFetcher task, url:" + this.resolvedURL, e);
        } catch (Exception e) {
            logger.error("Error running HTMLFetcher task, url:" + this.resolvedURL, e);
        }
        return null;
    }


}
