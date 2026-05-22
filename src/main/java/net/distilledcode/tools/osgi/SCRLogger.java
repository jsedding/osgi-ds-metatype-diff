package net.distilledcode.tools.osgi;


import org.apache.felix.scr.impl.logger.BundleLogger;
import org.apache.felix.scr.impl.logger.ComponentLogger;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;

import java.text.MessageFormat;
import java.util.function.Supplier;

public class SCRLogger implements BundleLogger, ComponentLogger {

    private final Logger log;

    private enum LevelLogger {
        DEBUG(Level.DEBUG) {
            @Override
            public boolean isEnabled(final Logger logger) {
                return logger.isDebugEnabled();
            }

            @Override
            public void log(final Logger logger, final String message, final Throwable throwable) {
                logger.debug(message, throwable);
            }
        },
        INFO(Level.INFO) {
            @Override
            public boolean isEnabled(final Logger logger) {
                return logger.isInfoEnabled();
            }

            @Override
            public void log(final Logger logger, final String message, final Throwable throwable) {
                logger.info(message, throwable);
            }
        },
        WARN(Level.WARN) {
            @Override
            public boolean isEnabled(final Logger logger) {
                return logger.isWarnEnabled();
            }

            @Override
            public void log(final Logger logger, final String message, final Throwable throwable) {
                logger.warn(message, throwable);
            }
        },
        ERROR(Level.ERROR) {
            @Override
            public boolean isEnabled(final Logger logger) {
                return logger.isErrorEnabled();
            }

            @Override
            public void log(final Logger logger, final String message, final Throwable throwable) {
                logger.error(message, throwable);
            }
        };

        private final Level lvl;

        LevelLogger(Level lvl) {
            this.lvl = lvl;
        }

        static LevelLogger byLevel(Level lvl) {
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


    SCRLogger(final Logger log) {
        this.log = log;
    }

    @Override
    public void log(Level level, String message, Throwable ex) {
        doLog(LevelLogger.byLevel(level), log, () -> message, ex);
    }

    private void doLog(LevelLogger levelLogger, Logger log, Supplier<String> message, Throwable ex) {
        if (levelLogger.isEnabled(log)) {
            levelLogger.log(log, message.get(), ex);
        }
    }

    @Override
    public void log(Level level, String pattern, Throwable ex, Object... arguments) {
        doLog(LevelLogger.byLevel(level), log, () -> MessageFormat.format(pattern, arguments), ex);
    }

    @Override
    public boolean isLogEnabled(final Level level) {
        return LevelLogger.byLevel(level).isEnabled(log);
    }


    @Override
    public ComponentLogger component(Bundle m_bundle, String implementationClassName, String name) {
        return this;
    }

    @Override
    public void setComponentId(long m_componentId) {

    }
}
