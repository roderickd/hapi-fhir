INSERT INTO BT2_JOB_INSTANCE (
   ID,
   JOB_CANCELLED,
   CMB_RECS_PROCESSED,
   CMB_RECS_PER_SEC,
   CREATE_TIME,
   CUR_GATED_STEP_ID,
   DEFINITION_ID,
   DEFINITION_VER,
   END_TIME,
   ERROR_COUNT,
   EST_REMAINING,
   PARAMS_JSON,
   PROGRESS_PCT,
   START_TIME,
   STAT,
   TOT_ELAPSED_MILLIS,
   ERROR_MSG,
   PARAMS_JSON_LOB,
   WORK_CHUNKS_PURGED,
   REPORT
) VALUES (
   'a59cb9c2-f699-44c7-bfee-93f1e6f68038',
   false,
   0,
   0,
   '2023-08-06 14:24:10.845',
   'WriteBundleForImportStep',
   'bulkImportJob',
   1,
   '2023-08-06 14:25:11.098',
   0,
   '0ms',
   '{"jobId":"d184be05-a634-40c3-a3e6-ab811329308b","batchSize":100}',
   1,
   '2023-08-06 14:24:10.875',
   'COMPLETED',
   200,
   'Error message',
   83006,
   true,
   72995
);
