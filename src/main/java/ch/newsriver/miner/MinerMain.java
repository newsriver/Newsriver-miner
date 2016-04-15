package ch.newsriver.miner;

import ch.newsriver.executable.Main;
import ch.newsriver.executable.poolExecution.MainWithPoolExecutorOptions;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Created by eliapalme on 11/03/16.
 */
public class MinerMain extends MainWithPoolExecutorOptions {

    private static final int DEFAUTL_PORT = 9097;
    private static final Logger logger = LogManager.getLogger(MinerMain.class);


    public int getDefaultPort(){
        return DEFAUTL_PORT;
    }

    static Miner miner;

    public MinerMain(String[] args){
        super(args,true);


    }

    public static void main(String[] args){
        new MinerMain(args);

    }

    public void shutdown(){

        if(miner!=null)miner.stop();
    }

    public void start(){
        try {
            System.out.println("Threads pool size:" + this.getPoolSize() +"\tbatch size:"+this.getBatchSize()+"\tqueue size:"+this.getBatchSize());
            miner = new Miner(this.getPoolSize(),this.getBatchSize(),this.getQueueSize());
            new Thread(miner).start();
        } catch (Exception e) {
            logger.fatal("Unable to initialize scout", e);
        }
    }


}
