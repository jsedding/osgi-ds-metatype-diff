package net.distilledcode.tools.osgi;


import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;

import java.text.MessageFormat;

public class SCRLogger implements org.apache.felix.scr.impl.helper.Logger {

    private final Logger log;

    private enum LevelLogger {
        DEBUG(LogService.LOG_DEBUG) {
            @Override
            public boolean isEnabled(final Logger logger) {
                return logger.isDebugEnabled();
            }

            @Override
            public void log(final Logger logger, final String pattern, final Object... arguments) {
                logger.debug(pattern, arguments);
            }
        },
        INFO(LogService.LOG_INFO) {
            @Override
            public boolean isEnabled(final Logger logger) {
                return logger.isInfoEnabled();
            }

            @Override
            public void log(final Logger logger, final String pattern, final Object... arguments) {
                logger.info(pattern, arguments);
            }
        },
        WARN(LogService.LOG_WARNING) {
            @Override
            public boolean isEnabled(final Logger logger) {
                return logger.isWarnEnabled();
            }

            @Override
            public void log(final Logger logger, final String pattern, final Object... arguments) {
                logger.warn(pattern, arguments);
            }
        },
        ERROR(LogService.LOG_ERROR) {
            @Override
            public boolean isEnabled(final Logger logger) {
                return logger.isErrorEnabled();
            }

            @Override
            public void log(final Logger logger, final String pattern, final Object... arguments) {
                logger.error(pattern, arguments);
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

        public abstract void log(final Logger logger, final String pattern, final Object... arguments);
    }


    SCRLogger(final Logger log) {
        this.log = log;
    }

    @Override
    public boolean isLogEnabled(final int level) {
        return LevelLogger.byLevel(level).isEnabled(log);
    }

    @Override
    public void log(final int level, final String pattern, final Object[] arguments, final ComponentMetadata metadata, final Long componentId, final Throwable ex) {
        if (isLogEnabled(level)) {
            final String message = MessageFormat.format(pattern, arguments);
            log(level, message, metadata, componentId, ex );
        }
    }

    @Override
    public void log(final int level, final String message, final ComponentMetadata metadata, final Long componentId, final Throwable ex) {
        LevelLogger.byLevel(level).log(log,"[{}({})]{}", metadata.getName(), componentId, message, ex);
    }
}
