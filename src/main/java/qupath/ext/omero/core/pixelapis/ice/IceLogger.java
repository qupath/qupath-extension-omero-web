package qupath.ext.omero.core.pixelapis.ice;

import omero.log.LogMessage;
import org.slf4j.LoggerFactory;

/**
 * Forwards log messages from the OMERO Java gateway to the logging framework used by the extension.
 */
class IceLogger implements omero.log.Logger {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(IceLogger.class);
    @Override
    public void debug(Object o, String s) {
        logger.debug(s, o);
    }

    @Override
    public void debug(Object o, LogMessage logMessage) {
        logger.debug(logMessage.toString(), o);
    }

    @Override
    public void info(Object o, String s) {
        logger.info(s, o);
    }

    @Override
    public void info(Object o, LogMessage logMessage) {
        logger.info(logMessage.toString(), o);
    }

    @Override
    public void warn(Object o, String s) {
        logger.warn(s, o);
    }

    @Override
    public void warn(Object o, LogMessage logMessage) {
        logger.warn(logMessage.toString(), o);
    }

    @Override
    public void error(Object o, String s) {
        logger.error(s, o);
    }

    @Override
    public void error(Object o, LogMessage logMessage) {
        logger.error(logMessage.toString(), o);
    }

    @Override
    public void fatal(Object o, String s) {
        logger.error(s, o);
    }

    @Override
    public void fatal(Object o, LogMessage logMessage) {
        logger.error(logMessage.toString(), o);
    }

    @Override
    public String getLogFile() {
        return "";
    }
}

