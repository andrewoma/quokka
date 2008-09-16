package ws.quokka.core.bootstrap_util;

/**
 * NullLogger consumes all messages, logging nothing
 */
public class NullLogger implements Logger {
    public boolean isDebugEnabled() {
        return false;
    }

    public boolean isInfoEnabled() {
        return false;
    }

    public boolean isWarnEnabled() {
        return false;
    }

    public boolean isErrorEnabled() {
        return false;
    }

    public boolean isVerboseEnabled() {
        return false;
    }

    public boolean isEnabled(int level) {
        return false;
    }

    public void debug(String message) {
    }

    public void verbose(String message) {
    }

    public void warn(String message) {
    }

    public void error(String message) {
    }

    public void info(String message) {
    }
}
