import ch.newsriver.data.html.HTML;
import ch.newsriver.miner.html.HTMLFetcher;
import ch.newsriver.util.http.HttpClientPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

        String url = "http://globalnews.ca/news/2602763/cp-rail-blocking-golden-whitewater-raft-groups-from-popular-run/";

        if (!DownloadedHTMLs.getInstance().isDownloaded(url)) {
            HTML html = new HTMLFetcher(url, null).fetch();
            assertNotNull(html);
        }
    }

}
