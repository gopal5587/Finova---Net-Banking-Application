package com.finova.scheduling;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

/**
 * Quartz job that credits monthly savings interest. {@link DisallowConcurrentExecution} prevents
 * overlapping runs if a previous accrual is still in progress.
 */
@Component
@DisallowConcurrentExecution
public class InterestAccrualJob extends QuartzJobBean {

    private final InterestAccrualService interestAccrualService;

    public InterestAccrualJob(InterestAccrualService interestAccrualService) {
        this.interestAccrualService = interestAccrualService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        interestAccrualService.accrueMonthlyInterest();
    }
}
