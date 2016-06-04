package ch.newsriver.miner;

import ch.newsriver.data.html.HTML;
import ch.newsriver.data.url.BaseURL;
import ch.newsriver.data.url.SeedURL;
import ch.newsriver.executable.Main;
import ch.newsriver.executable.StreamExecutor;
import ch.newsriver.executable.poolExecution.MainWithPoolExecutorOptions;
import ch.newsriver.processor.Output;
import ch.newsriver.util.JSONSerde;
import org.apache.commons.cli.Options;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.kstream.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * Created by eliapalme on 11/03/16.
 */
public class MinerMain extends StreamExecutor implements Thread.UncaughtExceptionHandler {

    private static final int DEFAUTL_PORT = 9097;
    private static final Logger logger = LogManager.getLogger(MinerMain.class);


    public int getDefaultPort() {
        return DEFAUTL_PORT;
    }

    static Miner miner;
    private KafkaStreams streams;

    public MinerMain(String[] args) {
        super(args, true);


    }

    public static void main(String[] args) {
        new MinerMain(args);

    }

    public void shutdown() {

        if (miner != null) miner.close();
        if (streams != null) streams.close();
    }

    public void start() {
        try {
            miner = new Miner();


            Properties props = new Properties();
            InputStream inputStream = null;
            try {

                String propFileName = "kafka.properties";
                inputStream = Miner.class.getClassLoader().getResourceAsStream(propFileName);
                if (inputStream != null) {
                    props.load(inputStream);
                } else {
                    throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
                }
            } catch (java.lang.Exception e) {
                logger.error("Unable to load kafka properties", e);
            } finally {
                try {
                    inputStream.close();
                } catch (java.lang.Exception e) {
                }
            }

            props.setProperty(StreamsConfig.NUM_STREAM_THREADS_CONFIG, "" + this.getPoolSize());


            final Serde<String> stringSerde = Serdes.String();


            final Serde<BaseURL> baseURLSerde = new JSONSerde<BaseURL>(BaseURL.class);
            final Serde<HTML> htmlSerde = new JSONSerde<HTML>(HTML.class);


            KStreamBuilder builder = new KStreamBuilder();


            KStream<String, BaseURL> urls = builder.stream(stringSerde, baseURLSerde, "raw-urls");


            KStream<String, HTML> htmls = urls.map((url, baseURL) -> {

                //producer.send(new ProducerRecord<String, String>("processing-status", ((ManualURL) output.getIntput()).getSessionId(), "HTML mining completed."));
                KeyValue<String, HTML> output = miner.process(url, baseURL);

                return output;


            });

            Predicate<String, HTML> isSeedURL = (k, v) -> v!=null && v.getReferral() instanceof SeedURL;
            Predicate<String, HTML> isNotSeedURL = (k, v) -> v!=null && !(v.getReferral() instanceof SeedURL);
            KStream<String, HTML>[] next = htmls.branch(isSeedURL, isNotSeedURL);

            next[0].to(stringSerde, htmlSerde, "seed-html");
            next[1].to(stringSerde, htmlSerde, "raw-html");
            streams = new KafkaStreams(builder, props);



            streams.setUncaughtExceptionHandler(this);
            streams.start();


        } catch (Exception e) {
            logger.fatal("Unable to initialize miner", e);
        }

        /*




                        Output<BaseURL, HTML> output = this.process(record.value());
                        if (output.isSuccess()) {
                            String json = null;
                            try {
                                json = mapper.writeValueAsString(output.getOutput());
                            } catch (IOException e) {
                                logger.fatal("Unable to serialize miner result", e);
                                return null;
                            }
                            if (output.getIntput() instanceof ManualURL) {
                                producer.send(new ProducerRecord<String, String>("processing-status", ((ManualURL) output.getIntput()).getSessionId(), "HTML mining completed."));
                            }

                            if(record.topic().equalsIgnoreCase("seed-urls")){
                                //as key we send the input
                                producer.send(new ProducerRecord<String, String>("seed-html", record.value(), json));
                            }else{
                                producer.send(new ProducerRecord<String, String>("raw-html"+priorityPostFix, output.getOutput().getUrl(), json));
                            }



                        } else {
                            if (output.getIntput() instanceof ManualURL) {
                                producer.send(new ProducerRecord<String, String>("processing-status", ((ManualURL) output.getIntput()).getSessionId(), "Error unable to mine HTML."));
                            }
                        }


         */
    }


    @Override
    public void uncaughtException(Thread t, Throwable e) {

        logger.fatal("Miner uncaughtException", e);
    }
}
