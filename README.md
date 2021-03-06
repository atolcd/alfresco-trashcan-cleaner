[![Build Status](https://buildhive.cloudbees.com/job/atolcd/job/alfresco-trashcan-cleaner/badge/icon)](https://buildhive.cloudbees.com/job/atolcd/job/alfresco-trashcan-cleaner/)
Alfresco Trashcan Cleaner Module
================================

This Alfresco module periodically purges old content from the Alfresco trashcan.

Works with:

 - Alfresco Community from 4.2.d
 - Alfresco Enterprise from 4.1.5

Building the module
-------------------
By default, `mvn package` will build the AMP file against Alfresco Community 4.2.e. This can be changed via the Maven property `alfresco.version`.

Building with older *enterprise* versions will require to use the org.alfresco.enterprise groupId in the dependencies.
Usage of an *enterprise* version will require to have credentials to the Alfresco private Maven repository.

Installing the module
---------------------
Trashcan Cleaner is a standard Alfresco Module, so experienced users can skip these steps and proceed as usual.

1. Stop Alfresco
2. Use the Alfresco [Module Management Tool](http://wiki.alfresco.com/wiki/Module_Management_Tool) to install the module in your Alfresco WAR file:

        java -jar alfresco-mmt.jar /path/to/amp/trashcanCleaner.amp $TOMCAT_HOME/webapps/alfresco.war

3. Delete the `$TOMCAT_HOME/webapps/alfresco/` folder.
**Caution:** Please ensure you do not have custom files in the `$TOMCAT_HOME/webapps/alfresco/` folder before deleting
4. Start Alfresco

Using the module
---------------------
The default configuration enclosed with the AMP file launches the Trashcan Cleaner each day at 4am, and deletes the items one week after they have been put in the trashcan.

This configuration can be overriden in several ways. Probably the simplest is to add and update the following properties to your alfresco-global.properties file:

```
# Trigger at 4am each day
trashcan.cleaner.cron=0 0 4 * * ?

# Duration, in days, during which deleted items will be protected
trashcan.cleaner.protected.day=7
```

Alternatively, you can copy the *trashcanCleaner* bean into a Spring context file in your `$TOMCAT_HOME/shared/classes/alfresco/extension` folder, and then change the value of these two configuration items:

* protectedDays, the number of days an item can stay in the trashcan;
* cronExpression, the [Quartz-style CRON](http://wiki.alfresco.com/wiki/Scheduled_Actions#Cron_Explained) expression which launches the deletion.