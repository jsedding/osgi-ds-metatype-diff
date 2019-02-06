package net.distilledcode.tools.osgi;


import org.apache.felix.scr.impl.logger.BundleLogger;
import org.apache.felix.scr.impl.logger.ScrLogger;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;

import java.text.MessageFormat;

public class SCRLogger extends BundleLogger {

    private final Logger log;

    private enum LevelLogger {
        DEBUG(LogService.LOG_DEBUG) {
            @Override
            public boolean isEnabled(final Logger logger) {
                return logger.isDebugEnabled();
            }

            @Override
            public void log(final Logger logger, final String message, final Throwable throwable) {
                logger.debug(message, throwable);
            }
        },
        INFO(LogService.LOG_INFO) {
            @Override
            public boolean isEnabled(final Logger logger) {
                return logger.isInfoEnabled();
            }

            @Override
            public void log(final Logger logger, final String message, final Throwable throwable) {
                logger.info(message, throwable);
            }
        },
        WARN(LogService.LOG_WARNING) {
            @Override
            public boolean isEnabled(final Logger logger) {
                return logger.isWarnEnabled();
            }

            @Override
            public void log(final Logger logger, final String message, final Throwable throwable) {
                logger.warn(message, throwable);
            }
        },
        ERROR(LogService.LOG_ERROR) {
            @Override
            public boolean isEnabled(final Logger logger) {
                return logger.isErrorEnabled();
            }

            @Override
            public void log(final Logger logger, final String message, final Throwable throwable) {
                logger.error(message, throwable);
            }
        };

        private final int lvl;

        LevelLogger(int lvl) {
            this.lvl = lvl;
        }

        static LevelLogger byLevel(int lvl) {
            for (final LevelLogger levelLogger : values()) {
                if (levelLogger.lvl == lvl) {
                    return levelLogger;
                }
            }
            throw new IllegalArgumentException("Level " + lvl + " is undefined.");
        }

        public abstract boolean isEnabled(final Logger logger);

        public abstract void log(final Logger logger, final String message, final Throwable throwable);
    }


    SCRLogger(final Logger log, final BundleContext bc) {
        super(bc, new ScrLogger(null, bc));
        this.log = log;
    }

    @Override
    public boolean log(int level, String message, Throwable ex) {
        if (!isLogEnabled(level)) {
            return false;
        }
        LevelLogger.byLevel(level).log(log, message, ex);
        return true;
    }

    @Override
    public boolean log(int level, String pattern, Throwable ex, Object... arguments) {
        if (!isLogEnabled(level)) {
            return false;
        }

        final String message = MessageFormat.format(pattern, arguments);
        return log(level, message, ex);
    }

    @Override
    public boolean isLogEnabled(final int level) {
        return LevelLogger.byLevel(level).isEnabled(log);
    }
}
