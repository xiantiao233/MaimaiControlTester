package fun.xiantiao.maimaicontrol.transfer.basic;

import java.util.function.Consumer;

public abstract class TransferProxy<P, T> extends Transfer<P> {

    protected final Transfer<T> transfer;

    private final Consumer<T> consumer = data -> {
        P receive = decode(data);
        if (receive != null) onReceive(receive);
    };

    public TransferProxy(Transfer<T> transfer) {
        this.transfer = transfer;
        transfer.addReceiver(consumer);
    }

    protected abstract P decode(T data);

    protected abstract T encode(P data);

    @Override
    public void send(P data) {
        transfer.send(encode(data));
    }

    public void detach() {
        transfer.removeReceiver(consumer);
    }

    public void detachAll() {
        detach();

        if (transfer instanceof TransferProxy) {
            ((TransferProxy<?, ?>) transfer).detachAll();
        }
    }

}
