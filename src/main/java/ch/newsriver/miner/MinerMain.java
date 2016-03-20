package ch.newsriver.miner;

import ch.newsriver.executable.Main;

import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.net.www.protocol.http.HttpURLConnection;

import java.net.URI;
import java.net.URL;

/**
 * Created by eliapalme on 11/03/16.
 */
public class MinerMain extends Main {

    private static final int DEFAUTL_PORT = 9097;
    private static final Logger logger = LogManager.getLogger(MinerMain.class);


    public int getDefaultPort(){
        return DEFAUTL_PORT;
    }

    static Miner miner;

    public MinerMain(String[] args, Options options ){
        super(args,options);


    }

    public static void main(String[] args){

        Options options = new Options();

        options.addOption("f","pidfile", true, "pid file location");
        options.addOption(org.apache.commons.cli.Option.builder("p").longOpt("port").hasArg().type(Number.class).desc("port number").build());

        new MinerMain(args,options);

    }

    public void shutdown(){

        if(miner!=null)miner.stop();
    }

    public void start(){
        try {
            miner = new Miner();
            new Thread(miner).start();
        } catch (Exception e) {
            logger.fatal("Unable to initialize scout", e);
        }
    }


}
