package com.atolcd.alfresco.trashcancleaner;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.schedule.AbstractScheduledLockedJob;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class TrashcanCleanerJob extends AbstractScheduledLockedJob {

  @Override
  public void executeJob(JobExecutionContext context) throws JobExecutionException {
    JobDataMap jobData = context.getJobDetail().getJobDataMap();
    // extract the content cleaner to use
    Object trashcanCleanerObj = jobData.get("trashcanCleaner");
    if (trashcanCleanerObj == null || !(trashcanCleanerObj instanceof TrashcanCleaner)) {
      throw new AlfrescoRuntimeException(
          "TrashcanCleanerJob data must contain valid 'trashcanCleaner' reference");
    }
    final TrashcanCleaner trashcanCleaner = (TrashcanCleaner) trashcanCleanerObj;

    AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Object>() {
      public Object doWork() throws Exception {
        trashcanCleaner.execute();
        return null;
      }
    }, AuthenticationUtil.getSystemUserName());
  }
}
