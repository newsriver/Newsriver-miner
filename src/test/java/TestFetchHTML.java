import ch.newsriver.data.html.HTML;
import ch.newsriver.data.url.FeedURL;
import ch.newsriver.miner.MinerMain;
import ch.newsriver.miner.cache.DownloadedHTMLs;
import ch.newsriver.miner.cache.ResolvedURLs;
import ch.newsriver.miner.html.HTMLFetcher;
import ch.newsriver.miner.url.URLResolver;
import ch.newsriver.util.http.HttpClientPool;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Created by eliapalme on 15/04/16.
 */
public class TestFetchHTML {

    @Before
    public void initialize() throws Exception {

        HttpClientPool.initialize();
        /*
        Properties props = new Properties();
        String propFileName = "kafka.properties";
        try (InputStream inputStream = TestFetchHTML.class.getClassLoader().getResourceAsStream(propFileName)) {
            props.load(inputStream);
        }
        producer = new KafkaProducer(props);
        */
    }

    @After
    public void shutdown() throws Exception {
        HttpClientPool.shutdown();
        //producer.close();
    }

    @Test
    public void fetchHTML() throws Exception {

        String url = "https://www.google.com/url?rct=j&sa=t&url=http%3A%2F%2Fwww.bnd.com%2Fnews%2Flocal%2Farticle71845277.html&ct=ga&cd=CAIyGmE0YzFjOTVhZmE5NzgxNDI6Y29tOmVuOlVT&usg=AFQjCNHBCql_BGOXGmoi4PuWpnoUe5_EnQ";

        String resolvedURL = ResolvedURLs.getInstance().getResolved(url);
        if (resolvedURL == null) {
            try {
                resolvedURL = URLResolver.getInstance().resolveURL(url);
            } catch (URLResolver.InvalidURLException e) {
                return ;
            }
            //ResolvedURLs.getInstance().setResolved(referral.getUlr(), resolvedURL);
        }


        if (!DownloadedHTMLs.getInstance().isDownloaded(url)) {
            HTML html = new HTMLFetcher(resolvedURL, null).fetch();
            assertNotNull(html);
        }
    }

}
