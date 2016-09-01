package ch.newsriver.miner;

import ch.newsriver.data.html.HTML;
import ch.newsriver.data.url.BaseURL;
import ch.newsriver.data.url.SeedURL;
import ch.newsriver.executable.Main;
import ch.newsriver.executable.poolExecution.MainWithPoolExecutorOptions;
import ch.newsriver.performance.MetricsLogger;
import ch.newsriver.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;


/**
 * Created by eliapalme on 11/03/16.
 */
public class MinerMain extends MainWithPoolExecutorOptions {

    private static final int DEFAUTL_PORT = 9097;
    private static final Logger logger = LogManager.getLogger(MinerMain.class);
    private static MetricsLogger metrics;
    private static int MAX_EXECUTUION_DURATION = 120;
    Stream<BaseURL, HTML> stream;
    Miner miner;

    public MinerMain(String[] args) {
        super(args, true);
        metrics = MetricsLogger.getLogger(MinerMain.class, Main.getInstance().getInstanceName());
    }

    public static void main(String[] args) {
        new MinerMain(args);

    }

    public int getDefaultPort() {
        return DEFAUTL_PORT;
    }

    public void shutdown() {

        if (stream != null) stream.shutdown();
        miner.close();
    }

    public void start() {

        System.out.println("Threads pool size:" + this.getPoolSize() + "\tbatch size:" + this.getBatchSize() + "\tqueue size:" + this.getQueueSize());


        miner = new Miner();


        stream = Stream.Builder.with("Miner", this.getBatchSize(), this.getPoolSize(), this.getQueueSize(), Duration.ofSeconds(MAX_EXECUTUION_DURATION))
                .from("raw-urls").from("seed-urls")
                .withClasses(BaseURL.class, HTML.class)
                .setProcessor(input -> {
                    BaseURL referral = (BaseURL) input;
                    metrics.logMetric("processing url", referral);
                    HTML html = miner.process(referral);

                    if (html != null) {


                        //if (output.getIntput() instanceof ManualURL) {
                        //    producer.send(new ProducerRecord<String, String>("processing-status", ((ManualURL) input).getSessionId(), "HTML mining completed."));
                        //}
                        metrics.logMetric("submitted html", referral);
                    } else {
                        //if (input instanceof ManualURL) {
                        //producer.send(new ProducerRecord<String, String>("processing-status", ((ManualURL) input).getSessionId(), "Error unable to mine HTML."));
                        //}

                    }


                    return html;
                })
                .to("seed-html", v -> ((HTML) v).getReferral() instanceof SeedURL)
                .to("raw-html", v -> !(((HTML) v).getReferral() instanceof SeedURL)).build();

        new Thread(stream).start();


    }


}
