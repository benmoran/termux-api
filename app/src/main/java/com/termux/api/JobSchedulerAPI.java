package com.termux.api;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.PersistableBundle;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;

import com.termux.api.util.ResultReturner;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class JobSchedulerAPI {

    private static final String LOG_TAG = "JobSchedulerAPI";


    private static String formatJobInfo(JobInfo jobInfo) {
        final String path = jobInfo.getExtras().getString(SchedulerJobService.SCRIPT_FILE_PATH);
        List<String> description = new ArrayList<String>();
        if (jobInfo.isPeriodic()) {
            description.add(String.format("(periodic: %dms)", jobInfo.getIntervalMillis()));
        }
        if (jobInfo.isRequireCharging()) {
            description.add("(while charging)");
        }
        if (jobInfo.isRequireDeviceIdle()) {
            description.add("(while idle)");
        }
        if (jobInfo.isPersisted()) {
            description.add("(persisted)");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (jobInfo.isRequireBatteryNotLow()) {
                description.add("(battery not low)");
            }
            if (jobInfo.isRequireStorageNotLow()) {
                description.add("(storage not low)");
            }
        }
        if (Build.VERSION.SDK_INT >= 28) {
            description.add(String.format("(network: %s)", jobInfo.getRequiredNetwork().toString()));
        }

        return String.format("Job %d: %s\t%s", jobInfo.getId(), path,
                TextUtils.join(" ", description));

    }
    static void onReceive(TermuxApiReceiver apiReceiver, Context context, Intent intent) {

        final boolean pending = intent.getBooleanExtra("pending", false);

        final boolean cancel = intent.getBooleanExtra("cancel", false);

        final String scriptPath = intent.getStringExtra("script");

        final int periodicMillis = intent.getIntExtra("period_ms", 0);
        final int jobId = intent.getIntExtra("job_id", 0);
        final String networkType = intent.getStringExtra("network");
        final boolean batteryNotLow = intent.getBooleanExtra("battery_not_low", true);
        final boolean charging = intent.getBooleanExtra("charging", false);
        final boolean idle = intent.getBooleanExtra("idle", false);
        final boolean storageNotLow = intent.getBooleanExtra("storage_not_low", false);
        final boolean persisted = intent.getBooleanExtra("persisted", false);

        final String triggerContent = intent.getStringExtra("trigger_content_uri");
        final int triggerContentFlag = intent.getIntExtra("trigger_content_flag", 1);



        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (pending) {
            // Only display pending jobs
            for (JobInfo job : jobScheduler.getAllPendingJobs()) {
                final JobInfo j = job;
                ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultWriter() {
                    @Override
                    public void writeResult(PrintWriter out) {
                        out.println(String.format("Pending: %s", j.getId(), formatJobInfo(j)));
                    }
                });
            }
            return;
        } else if (cancel) {
            final JobInfo j;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                j = jobScheduler.getPendingJob(jobId);
                if (j == null) {
                    ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultWriter() {
                        @Override
                        public void writeResult(PrintWriter out) {
                            out.println(String.format("No job with id %", jobId));
                        }
                    });
                    return;
                } else {
                    ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultWriter() {
                        @Override
                        public void writeResult(PrintWriter out) {
                            out.println(String.format("Sending cancel for %s", formatJobInfo(j)));
                        }
                    });
                }
            } else {
                ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultWriter() {
                    @Override
                    public void writeResult(PrintWriter out) {
                        out.println(String.format("Sending cancel for job %d", jobId));
                    }
                });
            }
            jobScheduler.cancel(jobId);
        }


        int networkTypeCode = JobInfo.NETWORK_TYPE_NONE;
        if (networkType != null) {
            switch (networkType) {
                case "any":
                    networkTypeCode = JobInfo.NETWORK_TYPE_ANY;
                    break;
                case "unmetered":
                    networkTypeCode = JobInfo.NETWORK_TYPE_UNMETERED;
                    break;
                case "cellular":
                    networkTypeCode = JobInfo.NETWORK_TYPE_CELLULAR;
                    break;
                case "not_roaming":
                    networkTypeCode = JobInfo.NETWORK_TYPE_NOT_ROAMING;
                    break;
                default:
                case "none":
                    networkTypeCode = JobInfo.NETWORK_TYPE_NONE;
                    break;
            }
        }
        if (scriptPath == null) {
            ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultWriter() {
                @Override
                public void writeResult(PrintWriter out) {
                    out.println("No script path given");
                }
            });
            return;
        }
        final File file = new File(scriptPath);
        final String fileCheckMsg;
        if (!file.isFile()) {
            fileCheckMsg = "No such file: %s";
        } else if (!file.canRead()) {
            fileCheckMsg = "Cannot read file: %s";
        } else if (!file.canExecute()) {
            fileCheckMsg = "Cannot execute file: %s";
        } else {
            fileCheckMsg = "";
        }

        if (!fileCheckMsg.isEmpty()) {
            ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultWriter() {
                @Override
                public void writeResult(PrintWriter out) {
                    out.println(String.format(fileCheckMsg, scriptPath));
                }
            });
            return;
        }

        Uri contentUri = null;
        if (triggerContent != null) {
            contentUri = Uri.parse(triggerContent);
        }
        PersistableBundle extras = new PersistableBundle();
        extras.putString(SchedulerJobService.SCRIPT_FILE_PATH, file.getAbsolutePath());



        ComponentName serviceComponent = new ComponentName(context, SchedulerJobService.class);
        JobInfo.Builder builder = null;
        ;
        builder = new JobInfo.Builder(jobId, serviceComponent)
                .setExtras(extras)
                .setRequiredNetworkType(networkTypeCode)
                .setRequiresCharging(charging)
                .setRequiresDeviceIdle(idle)
                .setPersisted(persisted);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = builder.setRequiresBatteryNotLow(batteryNotLow);
            builder = builder.setRequiresStorageNotLow(storageNotLow);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (contentUri != null) {
                builder = builder.addTriggerContentUri(
                        new JobInfo.TriggerContentUri(contentUri,
                        triggerContentFlag));
            }
        }
        if (periodicMillis > 0) {
            builder = builder.setPeriodic(periodicMillis);
        }

        JobInfo job = builder.build();

        final int scheduleResponse = jobScheduler.schedule(job);

        Log.i(LOG_TAG, String.format("Scheduled job %d to call %s every %d ms - response %d",
                jobId, scriptPath, periodicMillis, scheduleResponse));
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultWriter() {
            @Override
            public void writeResult(PrintWriter out) {
                out.println(String.format("Scheduled job %d to call %s every %d ms - response %d",
                        jobId, scriptPath, periodicMillis, scheduleResponse));
            }
        });

    }

}
