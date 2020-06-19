package com.foo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class FooSync implements SchedulingConfigurer, Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(FooSync.class);
    private static final Integer CANCEL_SCHEDULED_TASK_DELAY_THRESHOLD_IN_SECONDS = 5;

    private final String jobName = "Foo";

    @Autowired
    private TaskScheduler fooTaskScheduler;

    private Date nextExecutionTime = null;

    private ScheduledTaskRegistrar scheduledTaskRegistrar;
    private ScheduledFuture<?> scheduledFuture;

    private Integer scheduleInSeconds = 10;
    private final Integer processingTimeInSeconds = 15;

    @Override
    public void run() {
        try {
            LOGGER.info("Sync kicked in..");
            TimeUnit.SECONDS.sleep(processingTimeInSeconds);
            LOGGER.info("Sync completed in..");
        } catch (InterruptedException e) {
            LOGGER.error("Sync interrupted..", e);
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {

        if (this.scheduledTaskRegistrar == null) {
            this.scheduledTaskRegistrar = scheduledTaskRegistrar;
        }

        this.scheduledTaskRegistrar.setScheduler(fooTaskScheduler);

        scheduledFuture =
                this.scheduledTaskRegistrar
                        .getScheduler()
                        .schedule(
                                this,
                                triggerContext -> {
                                    Date lastActualExecutionTime = triggerContext.lastActualExecutionTime();
                                    if (lastActualExecutionTime == null) {
                                        LOGGER.info("Sync running for the first time for job {}", jobName);
                                        // Can be extended further to support initialDelaySeconds. but then the same method should **not** be used to reconfigure tasks
                                        lastActualExecutionTime = new Date();
                                    }

                                    Calendar cal = Calendar.getInstance();
                                    cal.setTime(lastActualExecutionTime);
                                    cal.add(Calendar.SECOND, scheduleInSeconds);
                                    this.nextExecutionTime = cal.getTime();
                                    LOGGER.info(
                                            "{} sync : lastActualExecutionTime {}. Next schedule : {}. If the schedule has already passed, it would kick-in right away",
                                            this.jobName,
                                            lastActualExecutionTime,
                                            nextExecutionTime);
                                    return nextExecutionTime;
                                });
    }

    public synchronized void updateSchedule(Integer syncScheduleInSeconds) {
        LOGGER.info("Sync schedule is updated in DB for {} from {} to {} seconds", jobName, scheduleInSeconds, syncScheduleInSeconds);
        this.scheduleInSeconds = syncScheduleInSeconds;

        long delayInSeconds = this.scheduledFuture.getDelay(TimeUnit.SECONDS);
        LOGGER.info("Current scheduledTask delay {}", delayInSeconds);

        if (delayInSeconds < 0) {
            LOGGER.info("Sync run is already in process. New schedule will take effect after the current run");
        } else if (delayInSeconds < CANCEL_SCHEDULED_TASK_DELAY_THRESHOLD_IN_SECONDS) {
            LOGGER.info(
                    "Next sync is less than {} seconds away. after the next run, schedule will automatically be adjusted.",
                    CANCEL_SCHEDULED_TASK_DELAY_THRESHOLD_IN_SECONDS);
        } else {
            LOGGER.info(
                    "Next sync is more than {} seconds away. scheduledFuture.delay() is {}. Hence cancelling the schedule and rescheduling.",
                    CANCEL_SCHEDULED_TASK_DELAY_THRESHOLD_IN_SECONDS, delayInSeconds);

            boolean cancel = this.scheduledFuture.cancel(false); //do not interrupt the current run if it kicked in.
            LOGGER.info(
                    "future.cancel() returned {}. isCancelled() : {} isDone : {}",
                    cancel,
                    scheduledFuture.isCancelled(),
                    scheduledFuture.isDone());
            LOGGER.info("Reconfiguring sync for {} with new schedule {}", jobName, syncScheduleInSeconds);
            configureTasks(this.scheduledTaskRegistrar);
        }
    }

    public Date getNextSyncExecutionTime() {
        return nextExecutionTime;
    }

    public void trigger() {
        run();
    }
}
