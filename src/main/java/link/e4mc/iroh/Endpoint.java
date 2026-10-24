package link.e4mc.iroh;

import java.util.concurrent.CompletableFuture;

public class Endpoint implements AutoCloseable {
    @SuppressWarnings({"unused", "FieldMayBeFinal"})
    private long ptrEndpointBundle = 0;
    @SuppressWarnings({"unused", "FieldMayBeFinal"})
    private long ptrCallbackReceiver = 0;

    public Endpoint(byte[][] alpns) {
        this(alpns, null);
    }

    public Endpoint(byte[][] alpns, String[] relays) {
        this(alpns, relays, null);
    }

    /** @param secretKey 32-byte ed25519 secret key giving a stable endpoint id, or null for a random one */
    public Endpoint(byte[][] alpns, String[] relays, byte[] secretKey) {
        Native.initEndpointBundle(this, alpns, relays, secretKey);
    }

    /** The 32-byte endpoint id (public key). Peers can connect to its hex form via {@link #connect}. */
    public byte[] id() {
        return Native.idEndpointBundle(this);
    }

    public Runnable pollCallbackLoop() {
        return Native.pollEndpointBundle(this);
    }

    public String address() {
        return Native.addrEndpointBundle(this);
    }

    public void watchAddress(Resolvable<String> resolvable) {
        Native.watchAddrEndpointBundle(this, resolvable);
    }

    public CompletableFuture<String> waitOnline() {
        CompletableFuture<String> fut = new CompletableFuture<>();
        Native.onlineEndpointBundle(this, new CompletableFutureResolvable<>(fut));
        return fut;
    }

    public CompletableFuture<CompletableFuture<Connection>> accept() {
        CompletableFuture<Connection> fut = new CompletableFuture<>();
        CompletableFuture<Void> preFuture = new CompletableFuture<>();
        Native.acceptEndpointBundle(this, new CompletableFutureResolvable<>(preFuture), new CompletableFutureResolvable<>(fut));
        return preFuture.thenApply(nothing -> fut);
    }

    public CompletableFuture<Connection> connect(String addr, byte[] alpn) {
        CompletableFuture<Connection> fut = new CompletableFuture<>();
        Native.connectEndpointBundle(this, addr, alpn, new CompletableFutureResolvable<>(fut));
        return fut;
    }

    public CompletableFuture<Void> closeAsync() {
        CompletableFuture<Void> fut = new CompletableFuture<>();
        Native.closeEndpointBundle(this, new CompletableFutureResolvable<>(fut));
        return fut;
    }

    @Override
    public void close() {
        Native.freeEndpointBundle(this);
    }

    public static String sanitizeTicket(String ticket) {
        return Native.sanitizeTicket(ticket);
    }
}
