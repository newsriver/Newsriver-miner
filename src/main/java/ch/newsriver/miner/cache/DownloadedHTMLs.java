package ch.newsriver.miner.cache;

import ch.newsriver.dao.RedisPoolUtil;
import redis.clients.jedis.Jedis;

/**
 * Created by eliapalme on 12/04/16.
 */
public class DownloadedHTMLs {

    private final static String REDIS_KEY_PREFIX     = "downHTML";
    private final static String REDIS_KEY_VERSION     = "1";
    private final static Long   REDIS_KEY_EXP_SECONDS = 60l * 60l * 24l * 30l * 3l; //about 3 months
    static DownloadedHTMLs instance=null;

    public static synchronized DownloadedHTMLs getInstance(){
        if(instance==null){
            instance = new DownloadedHTMLs();
        }
        return instance;
    }

    private DownloadedHTMLs(){}

    private String getKey(String url){
        StringBuilder builder = new StringBuilder();
        return builder.append(REDIS_KEY_PREFIX).append(":")
                .append(REDIS_KEY_VERSION).append(":")
                .append(url).toString();
    }

    public boolean isDownloaded(String url){
        try (Jedis jedis = RedisPoolUtil.getInstance().getResource(RedisPoolUtil.DATABASES.DOWNLOADED_HTMLS)) {
            return  jedis.exists(getKey(url));
        }
    }

    public void setDownloaded(String url){
        try (Jedis jedis = RedisPoolUtil.getInstance().getResource(RedisPoolUtil.DATABASES.DOWNLOADED_HTMLS)) {
            jedis.set(getKey(url),"","NX","EX",REDIS_KEY_EXP_SECONDS);
        }
    }
}
