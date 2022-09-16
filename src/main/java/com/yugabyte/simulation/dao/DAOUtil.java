package com.yugabyte.simulation.dao;

import com.yugabyte.simulation.util.GeneralUtility;

public class DAOUtil {
    protected static final String INSERT_QUERY_TABLE_FILING = getInsertQueryForFiling();
    protected static final String INSERT_QUERY_TABLE_DL_FILING = getInsertQueryForDlFiling();
    protected static final String INSERT_QUERY_TABLE_ROUTING_NUMBER = getInsertQueryForRoutingNumber();
    protected static final String INSERT_QUERY_TABLE_TRANSMISSION = getInsertQueryForTransmission();
    protected static final String INSERT_QUERY_TABLE_TRANSMISSION_FILING = getInsertQueryForTransmissionFiling();
    protected static final String INSERT_QUERY_TABLE_TRANSMIT_DATA_VALUE = getInsertQueryForTransmitDataValue();


    protected static Object[] getRandomData(long id, int arrSize, boolean jsonField){
        Object[] obj = new Object[arrSize];
        obj[0] = id;
        int counter = 1;
        if(jsonField){
            obj[1] = GeneralUtility.FIXED_JSON_STRING;
            counter = counter + 1;
        }

        for(int i = counter; i < arrSize; i++){
            obj[i] = GeneralUtility.randomIntegerVal();
        }
        return  obj;
    }

    private static String getInsertQueryForFiling(){
        StringBuilder sb = new StringBuilder();
        sb.append("insert into FILING\n" +
                "(\n" +
                " FILING_ID,\n" +
                "  efileinfo,\n" +
                "  CLIENT_FILING_ID,\n" +
                "  AGENCY,\n" +
                "  FILING_TYPE,\n" +
                "  FILER_ID,\n" +
                "  SENDER_ID,\n" +
                "  CLIENT_APP,\n" +
                "  CLIENT_VERSION,\n" +
                "  SENDER_EMAIL,\n" +
                "  LEGAL_NAME,\n" +
                "  CRED_ID,\n" +
                "  PIN,\n" +
                "  PASSWORD,\n" +
                "  FILING_STATE,\n" +
                "  REPOS_STATE,\n" +
                "  OVERRIDE_DUPLICATE,\n" +
                "  WANTS_NOTIFICATION,\n" +
                "  PERIOD,\n" +
                "  FORMSET_VERSION,\n" +
                "  FORMSET_ENGINE_VERSION,\n" +
                "  LINKED_FILING_ID,\n" +
                "  SUBMISSION_TYPE,\n" +
                "  PAYMENT_AMOUNT,\n" +
                "  ROUTING_NUMBER,\n" +
                "  ACCOUNT_NUMBER,\n" +
                "  DCN,\n" +
                "  CLIENT_IP_ADDR,\n" +
                "  FORMSET_VERSION_CONVERTER,\n" +
                "  TRACKING_NUMBER,\n" +
                "  PRODUCT_CODE,\n" +
                "  CUST_TIMEZONE,\n" +
                "  AUTH_CODE,\n" +
                "  LAST_NAME,\n" +
                "  PHONE_NO,\n" +
                "  ZIP_CODE,\n" +
                "  ADDRESS,\n" +
                "  SPOUSE_FILER_ID,\n" +
                "  SPOUSE_LAST_NAME,\n" +
                "  HIDE_STATUS,\n" +
                "  REFERENCE_NUMBER,\n" +
                "  FORMSET,\n" +
                "  PLAN_NUMBER,\n" +
                "  SITE_ID,\n" +
                "  CLIENT_SUBMISSION_ID,\n" +
                "  PAYMENT_METHOD,\n" +
                "  POST_ACK_ENUM,\n" +
                "  AGI,\n" +
                "  PAYLOAD_FORMAT,\n" +
                "  TAXPAYER_FILING_STATUS,\n" +
                "  INCOMING_CLASS_ID,\n" +
                "  FIRST_NAME,\n" +
                "  MACHINE_ID,\n" +
                "  PRIMARY_NAME_CONTROL,\n" +
                "  COMPANY_NAME,\n" +
                "  SMS_NUMBER,\n" +
                "  SMS_OPT_IN_FLAG,\n" +
                "  SPOUSE_NAME_CONTROL,\n" +
                "  OVERRIDE_LINKED,\n" +
                "  SPOUSE_FULL_NAME,\n" +
                "  LINKED_FED_SUBMISSION_ID,\n" +
                "  FUNDING_STATUS,\n" +
                "  LINK_IF_POSSIBLE,\n" +
                "  FILED_AS_STATE_ONLY,\n" +
                "  LINKED_FED_FILING_TYPE,\n" +
                "  ERO_INFO_ID,\n" +
                "  ERO_CLIENT_ID,\n" +
                "  FILER_ID_LAST_DIGITS,\n" +
                "  SPOUSE_FILER_ID_LAST_DIGITS,\n" +
                "  ORIGINATING_BUS_UNIT,\n" +
                "  SECURE_DOB,\n" +
                "  SECURE_SPOUSE_DOB,\n" +
                "  SECURE_AGI,\n" +
                "  COUNTRY_CODE,\n" +
                "  FILER_AUTH_ID,\n" +
                "  FILER_TYPE,\n" +
                "  LAST_UPDATER,\n" +
                "  DMS_KEY\n" +
                ") \n" +
                "values");

        sb.append("(");
        for(int i= 0; i < 78; i++){
            if(i != 0){
                sb.append(",");
            }
            if(i == 1){
                sb.append("to_jsonb(?)");
            }
            else {
                sb.append("?");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private static String getInsertQueryForDlFiling() {
        StringBuilder sb = new StringBuilder();
        sb.append("insert into DL_FILING (\n" +
                " FILING_ID,\n" +
                "  FILER_ID,\n" +
                "  H_FILER_ID,\n" +
                "  SENDER_ID,\n" +
                "  AUTH_ID,\n" +
                "  ACCOUNT_NUMBER,\n" +
                "  DATE_OF_BIRTH,\n" +
                "  SPOUSE_FILER_ID,\n" +
                "  H_SPOUSE_FILER_ID,\n" +
                "  SPOUSE_DATE_OF_BIRTH,\n" +
                "  REFERENCE_NUMBER,\n" +
                "  AGI,\n" +
                "  LINKED_FED_SUBMISSION_ID,\n" +
                "  ERO_CLIENT_ID,\n" +
                "  SENDER_EMAIL,\n" +
                "  LEGAL_NAME,\n" +
                "  CRED_ID,\n" +
                "  CLIENT_IP_ADDR,\n" +
                "  LAST_NAME,\n" +
                "  PHONE_NO,\n" +
                "  ADDRESS,\n" +
                "  SPOUSE_LAST_NAME,\n" +
                "  FIRST_NAME,\n" +
                "  MACHINE_ID,\n" +
                "  COMPANY_NAME,\n" +
                "  SMS_NUMBER,\n" +
                "  SPOUSE_FULL_NAME,\n" +
                "  CLIENT_APP,\n" +
                "  LAST_UPDATER\n" +
                " ) values");
        sb.append("(");
        for(int i= 0; i < 29; i++){
            if(i != 0){
                sb.append(",");
            }
            sb.append("?");
        }
        sb.append(")");
        return sb.toString();
    }

    private static String getInsertQueryForRoutingNumber() {
        StringBuilder sb = new StringBuilder();
        sb.append("insert into ROUTING_NUMBER (\n" +
                " ROUTING_NUMBER,\n" +
                "  BANK_NAME,\n" +
                "  LAST_UPDATER\n" +
                " ) values");
        sb.append("(");
        for(int i= 0; i < 3; i++){
            if(i != 0){
                sb.append(",");
            }
            sb.append("?");
        }
        sb.append(")");
        return sb.toString();
    }

    private static String getInsertQueryForTransmission() {
        StringBuilder sb = new StringBuilder();
        sb.append("insert into TRANSMISSION (\n" +
                " SUBMISSION_ID,\n" +
                "  CORRELATION_ID,\n" +
                "  SERVICE_ID,\n" +
                "  SYSTEM_ID,\n" +
                "  LAST_UPDATER\n" +
                " ) values");
        sb.append("(");
        for(int i= 0; i < 5; i++){
            if(i != 0){
                sb.append(",");
            }
            sb.append("?");
        }
        sb.append(")");
        return sb.toString();
    }

    private static String getInsertQueryForTransmissionFiling() {
        StringBuilder sb = new StringBuilder();
        sb.append("insert into TRANSMISSION_FILING (\n" +
                " SUBMISSION_ID,\n" +
                "  CORRELATION_ID,\n" +
                "  FILING_STATE,\n" +
                "  REFERENCE_NUMBER,\n" +
                "  FILING_ID,\n" +
                "  TRANSMISSION_ID,\n" +
                "  LAST_UPDATER\n" +
                " ) values");
        sb.append("(");
        for(int i= 0; i < 7; i++){
            if(i != 0){
                sb.append(",");
            }
            sb.append("?");
        }
        sb.append(")");
        return sb.toString();
    }

    private static String getInsertQueryForTransmitDataValue() {
        StringBuilder sb = new StringBuilder();
        sb.append("insert into TRANSMIT_DATA_VALUE (\n" +
                " SUBMISSION_ID,\n" +
                "  TRANSMIT_DATA_VALUE_ID,\n" +
                "  NAME,\n" +
                "  ENC_VALUE,\n" +
                "  VALUE,\n" +
                "  DL_ENC_VALUE,\n" +
                "  LAST_UPDATER\n" +
                " ) values");
        sb.append("(");
        for(int i= 0; i < 7; i++){
            if(i != 0){
                sb.append(",");
            }
            sb.append("?");
        }
        sb.append(")");
        return sb.toString();
    }


    protected static final String QUERY_FILING_AND_DL_FILING_JOIN = "select f.FILING_ID, df.FILER_ID, df.LEGAL_NAME, df.ADDRESS, df.PHONE_NO, df.SPOUSE_DATE_OF_BIRTH,\n" +
            "f.AGENCY, f.CLIENT_APP, f.FILING_STATE, f.PAYLOAD_FORMAT, f.ZIP_CODE\n" +
            "from FILING f, DL_FILING df\n" +
            "where f.FILING_ID = df.FILING_ID\n" +
            "and f.FILING_ID = ?";

    protected static final String QUERY_ROUTING_NUMBER = "select ROUTING_NUMBER, BANK_NAME from ROUTING_NUMBER where ROUTING_NUMBER = ?";
    protected static final String QUERY_TRANSMISSION = "select SUBMISSION_ID, CORRELATION_ID,SERVICE_ID,SYSTEM_ID from TRANSMISSION where SUBMISSION_ID = ?";
    protected static final String QUERY_TRANSMISSION_FILING = "select SUBMISSION_ID, CORRELATION_ID, FILING_STATE,REFERENCE_NUMBER, FILING_ID, TRANSMISSION_ID from TRANSMISSION_FILING where SUBMISSION_ID = ?";
    protected static final String QUERY_TRANSMIT_DATA_VALUE = "select SUBMISSION_ID, TRANSMIT_DATA_VALUE_ID,NAME, ENC_VALUE from TRANSMIT_DATA_VALUE where SUBMISSION_ID= ?";
    protected static final String QUERY_FOR_STATUS_CHECK_FILING = "select FILING_STATE from FILING where filing_id = ? ";
    protected static final String QUERY_FOR_STATUS_CHECK_DL_FILING = "select FILER_ID from DL_FILING where filing_id = ? ";
    protected static final String QUERY_FOR_STATUS_CHECK_TRANSMISSION = "select SERVICE_ID from TRANSMISSION where SUBMISSION_ID = ? ";
    protected static final String QUERY_FOR_STATUS_CHECK_TRANSMISSION_FILING = "select FILING_STATE from TRANSMISSION_FILING where SUBMISSION_ID = ? ";
    protected static final String QUERY_FOR_STATUS_CHECK_TRANSMIT_DATA_VALUE = "select TRANSMIT_DATA_VALUE_ID from TRANSMIT_DATA_VALUE where SUBMISSION_ID = ? ";
    protected static final String QUERY_FOR_STATUS_UPDATE_FILING = "update FILING set FILING_STATE=? where filing_id = ? ";
    protected static final String QUERY_FOR_STATUS_UPDATE_DL_FILING = "update DL_FILING set FILER_ID=? where filing_id = ? ";
    protected static final String QUERY_FOR_STATUS_UPDATE_TRANSMISSION = "update TRANSMISSION set CORRELATION_ID=? where SUBMISSION_ID = ? ";
    protected static final String QUERY_FOR_STATUS_UPDATE_TRANSMISSION_FILING = "update TRANSMISSION_FILING set FILING_STATE=? where SUBMISSION_ID = ? ";
    protected static final String QUERY_FOR_STATUS_UPDATE_TRANSMIT_DATA_VALUE= "update TRANSMIT_DATA_VALUE set ENC_VALUE=? where SUBMISSION_ID = ? ";


    protected static final String CREATE_TABLE_FILING = "CREATE TABLE IF NOT EXISTS  FILING (\n" +
            "  FILING_ID BIGINT,\n" +
            "  efileinfo jsonb,\n" +
            "  CLIENT_FILING_ID TEXT,\n" +
            "  AGENCY TEXT,\n" +
            "  FILING_TYPE TEXT,\n" +
            "  FILER_ID TEXT,\n" +
            "  SENDER_ID TEXT,\n" +
            "  CLIENT_APP TEXT,\n" +
            "  CLIENT_VERSION TEXT,\n" +
            "  SENDER_EMAIL TEXT,\n" +
            "  LEGAL_NAME TEXT,\n" +
            "  CRED_ID TEXT,\n" +
            "  PIN TEXT,\n" +
            "  PASSWORD TEXT,\n" +
            "  FILING_STATE INT,\n" +
            "  REPOS_STATE INT,\n" +
            "  RX_CLIENT_DATE TIMESTAMP DEFAULT NOW(),\n" +
            "  NOTIFICATION_DATE TIMESTAMP DEFAULT NOW(),\n" +
            "  OVERRIDE_DUPLICATE INT,\n" +
            "  WANTS_NOTIFICATION INT,\n" +
            "  PERIOD TEXT,\n" +
            "  FORMSET_VERSION TEXT,\n" +
            "  FORMSET_ENGINE_VERSION TEXT,\n" +
            "  LINKED_FILING_ID BIGINT,\n" +
            "  STATUS_DATE TIMESTAMP DEFAULT NOW(),\n" +
            "  SUBMISSION_TYPE TEXT,\n" +
            "  SETTLEMENT_DATE TIMESTAMP DEFAULT NOW(),\n" +
            "  PAYMENT_AMOUNT INT,\n" +
            "  ROUTING_NUMBER TEXT,\n" +
            "  ACCOUNT_NUMBER TEXT,\n" +
            "  DCN TEXT,\n" +
            "  POSTMARK_DATE TIMESTAMP DEFAULT NOW(),\n" +
            "  CLIENT_IP_ADDR TEXT,\n" +
            "  FORMSET_VERSION_CONVERTER TEXT,\n" +
            "  TRACKING_NUMBER TEXT,\n" +
            "  PRODUCT_CODE TEXT,\n" +
            "  CUST_TIMEZONE TEXT,\n" +
            "  AUTH_CODE TEXT,\n" +
            "  LAST_NAME TEXT,\n" +
            "  DATE_OF_BIRTH TIMESTAMP DEFAULT NOW(),\n" +
            "  PHONE_NO TEXT,\n" +
            "  ZIP_CODE TEXT,\n" +
            "  ADDRESS TEXT,\n" +
            "  SPOUSE_FILER_ID TEXT,\n" +
            "  SPOUSE_LAST_NAME TEXT,\n" +
            "  SPOUSE_DATE_OF_BIRTH TIMESTAMP DEFAULT NOW(),\n" +
            "  HIDE_STATUS INT,\n" +
            "  REFERENCE_NUMBER TEXT,\n" +
            "  FORMSET TEXT,\n" +
            "  PLAN_NUMBER TEXT,\n" +
            "  SITE_ID INT,\n" +
            "  CLIENT_SUBMISSION_ID BIGINT,\n" +
            "  PAYMENT_METHOD TEXT,\n" +
            "  POST_ACK_ENUM TEXT,\n" +
            "  AGI TEXT,\n" +
            "  PAYLOAD_FORMAT TEXT,\n" +
            "  TAXPAYER_FILING_STATUS TEXT,\n" +
            "  INCOMING_CLASS_ID TEXT,\n" +
            "  FIRST_NAME TEXT,\n" +
            "  MACHINE_ID TEXT,\n" +
            "  PRIMARY_NAME_CONTROL TEXT,\n" +
            "  COMPANY_NAME TEXT,\n" +
            "  SMS_NUMBER TEXT,\n" +
            "  SMS_OPT_IN_FLAG INT,\n" +
            "  SPOUSE_NAME_CONTROL TEXT,\n" +
            "  OVERRIDE_LINKED INT,\n" +
            "  SPOUSE_FULL_NAME TEXT,\n" +
            "  LINKED_FED_SUBMISSION_ID TEXT,\n" +
            "  FUNDING_DATE TIMESTAMP DEFAULT NOW(),\n" +
            "  FUNDING_STATUS INT,\n" +
            "  LINK_IF_POSSIBLE TEXT,\n" +
            "  FILED_AS_STATE_ONLY INT,\n" +
            "  LINKED_FED_FILING_TYPE TEXT,\n" +
            "  ERO_INFO_ID BIGINT,\n" +
            "  ERO_CLIENT_ID TEXT,\n" +
            "  LAST_DML TIMESTAMP DEFAULT NOW(),\n" +
            "  FILER_ID_LAST_DIGITS TEXT,\n" +
            "  SPOUSE_FILER_ID_LAST_DIGITS TEXT,\n" +
            "  ORIGINATING_BUS_UNIT INT,\n" +
            "  SECURE_DOB TEXT,\n" +
            "  SECURE_SPOUSE_DOB TEXT,\n" +
            "  SECURE_AGI TEXT,\n" +
            "  COUNTRY_CODE TEXT,\n" +
            "  FILER_AUTH_ID TEXT,\n" +
            "  FILER_TYPE INT,\n" +
            "  LAST_UPDATER TEXT,\n" +
            "  DMS_KEY INT,\n" +
            "  PRIMARY KEY (FILING_ID)\n" +
            ")\n" +
            ";";

    protected static final String  CREATE_TABLE_DL_FILING = "CREATE TABLE IF NOT EXISTS DL_FILING (\n" +
            "  FILING_ID BIGINT,\n" +
            "  FILER_ID TEXT,\n" +
            "  H_FILER_ID TEXT,\n" +
            "  SENDER_ID TEXT,\n" +
            "  AUTH_ID TEXT,\n" +
            "  ACCOUNT_NUMBER TEXT,\n" +
            "  DATE_OF_BIRTH TEXT,\n" +
            "  SPOUSE_FILER_ID TEXT,\n" +
            "  H_SPOUSE_FILER_ID TEXT,\n" +
            "  SPOUSE_DATE_OF_BIRTH TEXT,\n" +
            "  REFERENCE_NUMBER TEXT,\n" +
            "  AGI TEXT,\n" +
            "  LINKED_FED_SUBMISSION_ID TEXT,\n" +
            "  LAST_DML TIMESTAMP DEFAULT NOW(),\n" +
            "  ERO_CLIENT_ID TEXT,\n" +
            "  SENDER_EMAIL TEXT,\n" +
            "  LEGAL_NAME TEXT,\n" +
            "  CRED_ID TEXT,\n" +
            "  CLIENT_IP_ADDR TEXT,\n" +
            "  LAST_NAME TEXT,\n" +
            "  PHONE_NO TEXT,\n" +
            "  ADDRESS TEXT,\n" +
            "  SPOUSE_LAST_NAME TEXT,\n" +
            "  FIRST_NAME TEXT,\n" +
            "  MACHINE_ID TEXT,\n" +
            "  COMPANY_NAME TEXT,\n" +
            "  SMS_NUMBER TEXT,\n" +
            "  SPOUSE_FULL_NAME TEXT,\n" +
            "  CLIENT_APP TEXT,\n" +
            "  LAST_UPDATER TEXT,\n" +
            "PRIMARY KEY (FILING_ID)\n" +
            ")\n" +
            ";" ;
    protected static final String CREATE_EXTENSION = "CREATE EXTENSION IF NOT EXISTS pgcrypto;";

    protected static final String  CREATE_TABLE_ROUTING_NUMBER = "CREATE TABLE IF NOT EXISTS ROUTING_NUMBER (\n" +
            "    ROUTING_NUMBER BIGINT,\n" +
            "    BANK_NAME TEXT,\n" +
            "    LAST_DML TIMESTAMP DEFAULT NOW(),\n" +
            "    LAST_UPDATER TEXT,\n" +
            "    PRIMARY KEY (ROUTING_NUMBER)\n" +
            ")\n" +
            ";";
    protected static final String  CREATE_TABLE_TRANSMISSION = "CREATE TABLE IF NOT EXISTS TRANSMISSION (\n" +
            "      SUBMISSION_ID BIGINT,\n" +
            "      CORRELATION_ID TEXT,\n" +
            "      SERVICE_ID TEXT,\n" +
            "      TRANSMISSION_DATE TIMESTAMP DEFAULT NOW(),\n" +
            "      STATUS_CHECK_DATE TIMESTAMP DEFAULT NOW(),\n" +
            "      SYSTEM_ID TEXT,\n" +
            "      LAST_DML TIMESTAMP DEFAULT NOW(),\n" +
            "      LAST_UPDATER TEXT,\n" +
            "      PRIMARY KEY(SUBMISSION_ID)\n" +
            ")\n" +
            ";";
    protected static final String  CREATE_TABLE_TRANSMISSION_FILING = "CREATE TABLE IF NOT EXISTS TRANSMISSION_FILING(\n" +
            "        SUBMISSION_ID BIGINT,\n" +
            "        CORRELATION_ID TEXT,\n" +
            "        FILING_STATE INT,\n" +
            "        REFERENCE_NUMBER TEXT,\n" +
            "        FILING_ID BIGINT,\n" +
            "        TRANSMISSION_ID BIGINT,\n" +
            "        LAST_DML TIMESTAMP DEFAULT NOW(),\n" +
            "        LAST_UPDATER TEXT,\n" +
            "        PRIMARY KEY(SUBMISSION_ID)\n" +
            "    )\n" +
            ";";
    protected static final String  CREATE_TABLE_TRANSMIT_DATA_VALUE = "CREATE TABLE IF NOT EXISTS TRANSMIT_DATA_VALUE(\n" +
            "    SUBMISSION_ID BIGINT,\n" +
            "    TRANSMIT_DATA_VALUE_ID BIGINT,\n" +
            "    NAME TEXT,\n" +
            "    ENC_VALUE TEXT,\n" +
            "    VALUE TEXT,\n" +
            "    INSERT_TIME TIMESTAMP DEFAULT NOW(),\n" +
            "    LAST_DML TIMESTAMP DEFAULT NOW(),\n" +
            "    DL_ENC_VALUE TEXT,\n" +
            "    LAST_UPDATER TEXT,\n" +
            "    PRIMARY KEY(SUBMISSION_ID)\n" +
            ")\n" +
            ";";

    protected static final String TRUNCATE_TABLE_FILING = "TRUNCATE TABLE FILING;";
    protected static final String TRUNCATE_TABLE_DL_FILING = "TRUNCATE TABLE DL_FILING;";
    protected static final String TRUNCATE_TABLE_ROUTING_NUMBER = "TRUNCATE TABLE ROUTING_NUMBER;";
    protected static final String TRUNCATE_TABLE_TRANSMISSION = "TRUNCATE TABLE TRANSMISSION;";
    protected static final String TRUNCATE_TABLE_TRANSMISSION_FILING = "TRUNCATE TABLE TRANSMISSION_FILING;";
    protected static final String TRUNCATE_TABLE_TRANSMIT_DATA_VALUE = "TRUNCATE TABLE TRANSMIT_DATA_VALUE;";
}
