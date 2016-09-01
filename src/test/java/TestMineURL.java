import ch.newsriver.data.url.LinkURL;
import ch.newsriver.util.http.HttpClientPool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Created by eliapalme on 19/06/16.
 */
@Ignore("Test is ignored, used for manual testing")
public class TestMineURL {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    KafkaProducer producer;

    @Before
    public void initialize() throws Exception {

        HttpClientPool.initialize();

        Properties props = new Properties();
        String propFileName = "kafka.properties";
        try (InputStream inputStream = TestFetchHTML.class.getClassLoader().getResourceAsStream(propFileName)) {
            props.load(inputStream);
        }
        producer = new KafkaProducer(props);


    }

    @After
    public void shutdown() throws Exception {
        HttpClientPool.shutdown();
        producer.close();
    }

    @Test
    public void sendURL() throws Exception {

        //TODO: run classifier to establish if HTML contains and article
        LinkURL linkURL = new LinkURL();
        linkURL.setDiscoverDate(dateFormatter.format(new Date()));

        //linkURL.setUrl("https://www.migrosmagazin.ch/reisen/reportagen/artikel/reise-ins-land-der-bausteine");
        //linkURL.setReferralURL("https://www.migrosmagazin.ch/");
        //linkURL.setRawURL("https://www.migrosmagazin.ch/reisen/reportagen/artikel/reise-ins-land-der-bausteine");

        linkURL.setUrl("http://www.boleromagazin.ch/auf-dem-nachttisch-von-sabina/");
        linkURL.setReferralURL("http://www.boleromagazin.ch/");
        linkURL.setRawURL("http://www.boleromagazin.ch/auf-dem-nachttisch-von-sabina/");

        try {
            String json = mapper.writeValueAsString(linkURL);
            producer.send(new ProducerRecord<String, String>("raw-urls", linkURL.getUrl(), json));

        } catch (Exception e) {

        }

    }


}
