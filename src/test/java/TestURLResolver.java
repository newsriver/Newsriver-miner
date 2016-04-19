import ch.newsriver.util.http.HttpClientPool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by eliapalme on 15/04/16.
 */
public class TestURLResolver {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Before
    public void initialize() throws Exception {
        HttpClientPool.initialize();
    }

    @After
    public void shutdown() throws Exception {
        HttpClientPool.shutdown();
    }

    @Test
    public void resolveURLs() throws Exception {
        /*HashMap<String,String> urls = null;
        try (InputStream inputStream = TestURLResolver.class.getClassLoader().getResourceAsStream("resolvedURLsToTest.json")) {
            TypeReference<HashMap<String,String>> typeRef = new TypeReference<HashMap<String,String>>() {};
            urls = mapper.readValue(inputStream, typeRef);
        }

        for(String url : urls.keySet()){
             String resolvedURL = URLResolver.getInstance().resolveURL(url);
             assertEquals(urls.get(url),resolvedURL);

         }*/

    }

}
