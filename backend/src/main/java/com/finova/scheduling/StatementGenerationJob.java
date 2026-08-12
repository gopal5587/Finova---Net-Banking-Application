package com.finova.scheduling;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

/** Quartz job that materialises monthly statements for the previous calendar month. */
@Component
@DisallowConcurrentExecution
public class StatementGenerationJob extends QuartzJobBean {

    private final StatementGenerationService statementGenerationService;

    public StatementGenerationJob(StatementGenerationService statementGenerationService) {
        this.statementGenerationService = statementGenerationService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        statementGenerationService.generateForPreviousMonth();
    }
}
