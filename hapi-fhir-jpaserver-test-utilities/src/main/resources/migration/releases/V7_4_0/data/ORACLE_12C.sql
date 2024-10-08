INSERT INTO HFJ_RES_SEARCH_URL (
   PARTITION_DATE,
   PARTITION_ID,
   RES_SEARCH_URL,
   CREATED_TIME,
   RES_ID
) VALUES (
   SYSDATE,
   1,
   'https://example.com',
   SYSDATE,
   1906
);

INSERT INTO HFJ_IDX_CMP_STRING_UNIQ (
   PID,
   PARTITION_DATE,
   PARTITION_ID,
   HASH_COMPLETE,
   HASH_COMPLETE_2,
   IDX_STRING,
   RES_ID
) VALUES (
   2,
   SYSDATE,
   1,
   -8173309116900170400,
   -7180360017667394276,
   'Patient?birthdate=2024-07-24&family=FAM',
   1906
);

INSERT INTO TRM_CONCEPT_DESIG (
   PID,
   LANG,
   USE_CODE,
   USE_DISPLAY,
   USE_SYSTEM,
   VAL,
   VAL_VC,
   CS_VER_PID,
   CONCEPT_PID
) VALUES (
   106,
   'NL',
   '900000000000013009',
   'SYNONYM',
   'HTTP://SNOMED.INFO/SCT',
   'SYSTOLISCHE BLOEDDRUK - EXPIRATIE',
   'SYSTOLISCHE BLOEDDRUK - EXPIRATIE',
   54,
   150
);
