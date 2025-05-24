package fun.xiantiao.maimaicontrol.transfer.basic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class TransferReceiver<T> {

    protected final List<Consumer<T>> receivers = new ArrayList<>();

    public void addReceiver(Consumer<T> receiver) {
        if (receivers.contains(receiver)) return;
        receivers.add(receiver);
    }

    public void removeReceiver(Consumer<T> receiver) {
        receivers.remove(receiver);
    }

    protected void onReceive(T data) {
        for (Consumer<T> receiver : receivers) {
            receiver.accept(data);
        }
    }
}
