package io.nflow.engine.exception;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.springframework.stereotype.Component;

import io.nflow.engine.exception.DispatcherExceptionHandling.Builder;
import io.nflow.engine.internal.dao.PollingBatchException;
import io.nflow.engine.internal.dao.PollingRaceConditionException;

/**
 * Dispatcher exception analyzer analyzes exceptions thrown by the workflow dispatcher and determines how the exception is
 * handled.
 */
@Component
public class DispatcherExceptionAnalyzer {

  private static final Logger logger = getLogger(DispatcherExceptionAnalyzer.class);

  /**
   * Analyze the exception.
   *
   * @param e
   *          The exception to be analyzed.
   * @return How the exception should be handled.
   */
  public final DispatcherExceptionHandling analyzeSafely(Exception e) {
    try {
      return analyze(e);
    } catch (Exception analyzerException) {
      logger.error("Failed to analyze exception, using default handling.", analyzerException);
    }
    return getDefaultHandling(e);
  }

  /**
   * Override this to provide custom handling.
   *
   * @param e
   *          The exception to be analyzed.
   * @return How the exception should be handled.
   */
  protected DispatcherExceptionHandling analyze(Exception e) {
    return getDefaultHandling(e);
  }

  /**
   * Get the default exception handling for an exception.
   *
   * @param e
   *          The exception to be analyzed.
   * @return How the exception should be handled.
   */
  protected final DispatcherExceptionHandling getDefaultHandling(Exception e) {
    Builder builder = new DispatcherExceptionHandling.Builder();
    if (e instanceof PollingRaceConditionException) {
      builder.setLogLevel(Level.DEBUG).setLogStackTrace(false).setRandomizeSleep(true);
    } else if (e instanceof PollingBatchException) {
      builder.setLogLevel(Level.WARN).setLogStackTrace(false).setSleep(false);
    } else if (e instanceof InterruptedException) {
      builder.setLog(false).setSleep(false);
    }
    return builder.build();
  }
}
