package fun.xiantiao.maimaicontrol.shutdown;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ShutdownManager implements Runnable {

    private static final CompletableFuture<Void> shutdown = new CompletableFuture<>();

    public static final ShutdownManager INSTANCE = new ShutdownManager();

    private final List<Shutdown> shutdownList = new ArrayList<>();

    private ShutdownManager() {}

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(INSTANCE));
    }

    public static void shutdown(){
        INSTANCE.run();
        System.exit(0);
    }

    public static boolean isShutdown(){
        return shutdown.isDone();
    }

    public static void addToShutdownList(Shutdown shutdown){
        INSTANCE.shutdownList.add(shutdown);
    }

    public static void waitForShutdown(){
        shutdown.join();
    }

    @Override
    public void run() {
        if (isShutdown()) return;

        for (Shutdown shutdown : shutdownList) {
            shutdown.shutdown();
        }

        shutdown.complete(null);
    }


}
