package link.e4mc.iroh;

import dev.dirs.ProjectDirectories;
import io.netty.util.internal.PlatformDependent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;

public class Native {
    // Natives are built by CI and published as a GitHub release named natives-<tag>.
    private static final String NATIVES_TAG = "offline1";

    static {
        loadNativeLibrary();
    }

    private static void loadNativeLibrary() {
        String libName = "iroh_java";
        libName += '_' + PlatformDependent.normalizedOs()
                + '_' + PlatformDependent.normalizedArch()
                + "_" + NATIVES_TAG;

        String libraryPath = System.getProperty("link.e4mc.dialtone.native_path");
        boolean downloaded = false;

        while (libraryPath == null) {
            String fileName = System.mapLibraryName(libName);
            String legacyFolderPath = System.getProperty("user.home") + File.separatorChar + ".e4mc_cache";
            String folderPath;
            try {
                folderPath = ProjectDirectories.from("link", "e4mc", "e4mc").cacheDir;
            } catch (Throwable e) {
                folderPath = legacyFolderPath;
            }
            libraryPath = folderPath + File.separatorChar + fileName;
            new File(folderPath).mkdirs();
            if (!new File(libraryPath).isFile()) {
                try {
                    URL url = new URL(System.getProperty("link.e4mc.dialtone.native_url", "https://github.com/Chagui68/iroh-java/releases/download/natives-" + NATIVES_TAG + "/" + fileName));
                    URLConnection connection = url.openConnection();
                    // GitHub release downloads answer Java's default Accept header with HTTP 406.
                    connection.setRequestProperty("Accept", "application/octet-stream");
                    ReadableByteChannel rbc = Channels.newChannel(connection.getInputStream());
                    FileOutputStream fos = new FileOutputStream(libraryPath);
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                    fos.close();
                    rbc.close();
                    downloaded = true;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            try {
                Path tempDir = Files.createTempDirectory("e4mc_temp");
                tempDir.toFile().deleteOnExit();

                FileInputStream fis = new FileInputStream(libraryPath);
                byte[] buf = new byte[fis.available()];
                fis.read(buf);
                fis.close();

                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hashed = digest.digest(buf);
                byte[] expected = get_native_hash(fileName);
                if (!Arrays.equals(hashed, expected)) {
                    // Our download attempt failed, just bail out
                    if (downloaded) {
                        throw new RuntimeException("Could not download a valid native!");
                    }
                    Files.delete(Paths.get(libraryPath));
                    libraryPath = null;
                    continue;
                }

                Path tempFile = tempDir.resolve(fileName);
                FileOutputStream fos = new FileOutputStream(tempFile.toString());
                fos.write(buf);
                fos.close();
                libraryPath = tempFile.toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        try {
            System.load(libraryPath);
        } catch (UnsatisfiedLinkError e) {
            throw e;
        }
    }

    private static byte[] get_native_hash(String filename) {
        switch (filename) {
            case "iroh_java_windows_aarch_64_offline1.dll":
                return new byte[]{-36, 126, -50, 53, 100, -2, 0, 52, -32, -51, -27, -15, -82, 65, 75, 61, 81, -26, -92, -128, -19, -46, 45, 18, 47, 92, -58, 113, 127, 101, 16, -117};
            case "iroh_java_windows_x86_64_offline1.dll":
                return new byte[]{-38, 54, -25, -124, -59, 60, 46, -52, 56, 115, 55, -126, -113, -36, -109, -111, -124, 3, -84, 35, 36, -45, 19, 60, -26, -38, 74, -127, -86, -1, 60, 34};
            case "libiroh_java_linux_aarch_64_offline1.so":
                return new byte[]{-73, 62, -59, -32, 34, 2, 2, 119, 16, 28, 50, 10, -18, -58, 102, 86, -34, 82, -110, 21, 30, -126, 68, -14, -112, 44, 110, -19, -81, -116, -103, 9};
            case "libiroh_java_linux_x86_64_offline1.so":
                return new byte[]{-102, 81, 105, -12, -115, -44, -91, -83, 84, -107, 100, 82, -114, -81, 47, -16, 111, -122, 62, -52, -101, 34, -12, 87, 124, -81, -96, 12, 18, -54, 55, -15};
            case "libiroh_java_osx_aarch_64_offline1.dylib":
                return new byte[]{91, 25, -115, -21, 7, 126, -109, -92, 60, -118, 7, 46, -98, -45, 74, 26, -33, 99, -16, -50, -66, 25, -124, -1, 121, -86, 53, -69, -31, 6, -32, -19};
            case "libiroh_java_osx_x86_64_offline1.dylib":
                return new byte[]{61, 3, 110, 68, -100, 24, -59, 127, -65, -24, 22, 9, 107, 98, -26, -50, -113, 118, -119, -102, -68, -119, 93, -17, -50, -103, -109, 31, -75, 55, 117, 27};
        }
        return new byte[]{};
    }

    static native Object resolveDeferredInitializer(DeferredInitializer<?> deferredInitializer);
    static native void initEndpointBundle(Endpoint bundle, byte[][] alpns, String[] relays, byte[] secretKey);
    static native byte[] idEndpointBundle(Endpoint bundle);
    static native void freeEndpointBundle(Endpoint bundle);
    static native Runnable pollEndpointBundle(Endpoint bundle);
    static native String addrEndpointBundle(Endpoint bundle);
    static native void watchAddrEndpointBundle(Endpoint bundle, Resolvable<String> future);
    static native void onlineEndpointBundle(Endpoint bundle, Resolvable<String> future);
    static native void closeEndpointBundle(Endpoint bundle, Resolvable<Void> future);
    static native void acceptEndpointBundle(Endpoint bundle, Resolvable<Void> preFuture, Resolvable<Connection> future);
    static native void connectEndpointBundle(Endpoint bundle, String addr, byte[] alpn, Resolvable<Connection> future);
    static native void closeIrohConnection(Connection connection, long code, byte[] reason);
    static native String addrIrohConnection(Connection connection);
    static native byte[] exportKeyingMaterialIrohConnection(Connection connection, byte[] label, byte[] context, int length);
    static native void acceptBiIrohConnection(Connection connection, Resolvable<Stream> future);
    static native void acceptUniIrohConnection(Connection connection, Resolvable<Stream> future);
    static native void openBiIrohConnection(Connection connection, Resolvable<Stream> future);
    static native void openUniIrohConnection(Connection connection, Resolvable<Stream> future);
    static native void readIrohStreamByteBuffer(Stream stream, ByteBuffer buffer, long offset, long maxlen, Resolvable<Long> future);
    static native void readIrohStreamByteArray(Stream stream, long maxlen, Resolvable<byte[]> future);
    static native void writeIrohStreamByteBuffer(Stream stream, ByteBuffer buffer, long offset, long len, Resolvable<Void> future);
    static native void writeIrohStreamByteArray(Stream stream, byte[] array, long offset, long len, Resolvable<Void> future);
    static native void freeIrohStream(Stream stream);
    static native String sanitizeTicket(String ticket);
    static native String debugInfo(Connection connection);
}
