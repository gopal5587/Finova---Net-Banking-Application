package com.finova.scheduling;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers durable Quartz jobs and cron triggers for interest accrual and statement generation.
 * Cron expressions are externalised so sandbox environments can tighten the schedule for demos.
 */
@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail interestAccrualJobDetail() {
        return JobBuilder.newJob(InterestAccrualJob.class)
                .withIdentity("interestAccrualJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger interestAccrualTrigger(
            JobDetail interestAccrualJobDetail,
            @Value("${finova.scheduling.interest-cron:0 0 2 1 * ?}") String cron) {
        return TriggerBuilder.newTrigger()
                .forJob(interestAccrualJobDetail)
                .withIdentity("interestAccrualTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                .build();
    }

    @Bean
    public JobDetail statementGenerationJobDetail() {
        return JobBuilder.newJob(StatementGenerationJob.class)
                .withIdentity("statementGenerationJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger statementGenerationTrigger(
            JobDetail statementGenerationJobDetail,
            @Value("${finova.scheduling.statement-cron:0 30 2 1 * ?}") String cron) {
        return TriggerBuilder.newTrigger()
                .forJob(statementGenerationJobDetail)
                .withIdentity("statementGenerationTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                .build();
    }
}
