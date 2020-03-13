package io.nflow.engine.workflow.definition;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.joda.time.DateTimeUtils.currentTimeMillis;
import static org.joda.time.DateTimeUtils.setCurrentMillisFixed;
import static org.joda.time.DateTimeUtils.setCurrentMillisSystem;
import static org.joda.time.Duration.standardSeconds;

import java.util.function.BooleanSupplier;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WorkflowSettingsTest {
  DateTime now = new DateTime(2014, 10, 22, 20, 44, 0);

  @BeforeEach
  public void setup() {
    setCurrentMillisFixed(now.getMillis());
  }

  @AfterEach
  public void teardown() {
    setCurrentMillisSystem();
  }

  @Test
  public void verifyConstantDefaultValues() {
    WorkflowSettings s = new WorkflowSettings.Builder().build();
    assertThat(s.shortTransitionDelay, is(standardSeconds(30)));
    long delta = s.getShortTransitionActivation().getMillis() - currentTimeMillis() - 30000;
    assertThat(delta, greaterThanOrEqualTo(-1000L));
    assertThat(delta, lessThanOrEqualTo(0L));
    assertThat(s.historyDeletableAfter, is(nullValue()));
    assertThat(s.defaultPriority, is((short) 0));
  }

  @Test
  public void errorTransitionDelayIsBetweenMinAndMaxDelay() {
    Duration maxDelay = standardSeconds(1000);
    Duration minDelay = standardSeconds(1);
    WorkflowSettings s = new WorkflowSettings.Builder().setMinErrorTransitionDelay(minDelay).setMaxErrorTransitionDelay(maxDelay)
        .build();
    long prevDelay = 0;
    for (int retryCount = 0; retryCount < 100; retryCount++) {
      long delay = s.getErrorTransitionActivation(retryCount).getMillis() - now.getMillis();
      assertThat(delay, greaterThanOrEqualTo(minDelay.getMillis()));
      assertThat(delay, lessThanOrEqualTo(maxDelay.getMillis()));
      assertThat(delay, greaterThanOrEqualTo(prevDelay));
      prevDelay = delay;
    }
  }

  @Test
  public void getMaxSubsequentStateExecutionsReturns100ByDefault() {
    WorkflowSettings s = new WorkflowSettings.Builder().build();
    assertThat(s.getMaxSubsequentStateExecutions(TestWorkflow.State.begin), is(equalTo(100)));
  }

  @Test
  public void getMaxSubsequentStateExecutionsReturnsValueDefinedForTheState() {
    int executionsDefault = 200;
    int executionsForBegin = 300;
    WorkflowSettings s = new WorkflowSettings.Builder().setMaxSubsequentStateExecutions(executionsDefault)
        .setMaxSubsequentStateExecutions(TestWorkflow.State.begin, executionsForBegin).build();
    assertThat(s.getMaxSubsequentStateExecutions(TestWorkflow.State.begin), is(equalTo(executionsForBegin)));
  }

  @Test
  public void getMaxSubsequentStateExecutionsReturnsGivenDefaultValueWhenNotDefinedForState() {
    int executionsDefault = 200;
    WorkflowSettings s = new WorkflowSettings.Builder().setMaxSubsequentStateExecutions(executionsDefault).build();
    assertThat(s.getMaxSubsequentStateExecutions(TestWorkflow.State.begin), is(equalTo(executionsDefault)));
  }

  @Test
  public void deleteHistoryConditionIsApplied() {
    WorkflowSettings s = new WorkflowSettings.Builder().setDeleteHistoryCondition(() -> true).build();

    assertThat(s.deleteHistoryCondition.getAsBoolean(), is(true));
  }

  @Test
  public void oncePerDaySupplierWorks() {
    BooleanSupplier supplier = WorkflowSettings.Builder.oncePerDay();
    assertThat(supplier.getAsBoolean(), is(false));
    assertThat(supplier.getAsBoolean(), is(false));
    setCurrentMillisFixed(now.plusDays(1).withHourOfDay(5).getMillis());
    assertThat(supplier.getAsBoolean(), is(true));
    assertThat(supplier.getAsBoolean(), is(false));
    assertThat(supplier.getAsBoolean(), is(false));
    setCurrentMillisFixed(now.plusDays(2).withHourOfDay(5).getMillis());
    assertThat(supplier.getAsBoolean(), is(true));
    assertThat(supplier.getAsBoolean(), is(false));
    assertThat(supplier.getAsBoolean(), is(false));
  }

}
