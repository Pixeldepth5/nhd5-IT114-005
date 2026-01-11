package Common;

/**
 * Minimal logger utility used by both client and server.
 * Methods mirror the calls made in the client code but simply
 * print to stdout/stderr for now.
 */
public class LoggerUtil {
    public static final LoggerUtil INSTANCE = new LoggerUtil();

    public static class LoggerConfig {
        private int fileSizeLimit;
        private int fileCount;
        private String logLocation;

        public void setFileSizeLimit(int fileSizeLimit) {
            this.fileSizeLimit = fileSizeLimit;
        }

        public void setFileCount(int fileCount) {
            this.fileCount = fileCount;
        }

        public void setLogLocation(String logLocation) {
            this.logLocation = logLocation;
        }
    }

    private LoggerUtil() {
    }

    public void setConfig(LoggerConfig config) {
        // No-op for now; placeholder to satisfy existing calls.
    }

    public void info(String message) {
        System.out.println("[INFO] " + message);
    }

    public void fine(String message) {
        System.out.println("[FINE] " + message);
    }

    public void warning(String message) {
        System.out.println("[WARN] " + message);
    }

    public void warning(String message, Throwable t) {
        warning(message + " :: " + t.getMessage());
    }

    public void severe(String message) {
        System.err.println("[SEVERE] " + message);
    }

    public void severe(String message, Throwable t) {
        severe(message + " :: " + t.getMessage());
    }
}
