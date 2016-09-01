import ch.newsriver.data.html.HTML;
import ch.newsriver.miner.html.HTMLFetcher;
import ch.newsriver.util.http.HttpClientPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertNotNull;

/**
 * Created by eliapalme on 15/04/16.
 */
@Ignore("Test is ignored, used for manual testing")
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

        Properties props = System.getProperties();
        props.setProperty("webdriver.chrome.driver", "/Users/eliapalme/Newsriver/Newsriver-miner/chromedriver-2.22-mac");


    }

    @After
    public void shutdown() throws Exception {
        HttpClientPool.shutdown();
        //producer.close();
    }

    @Test
    public void fetchHTML() throws Exception {

        String url = "http://www.sonntagszeitung.ch/read/sz_19_06_2016/nachrichten/Schweiz-kaempft-gegen-Schlepper-Mafia-67011";


        HTML html = new HTMLFetcher(url, null, true, false).fetch();
        assertNotNull(html);

    }

}
