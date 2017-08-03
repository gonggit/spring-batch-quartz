package com.kingbbode.job; 

/*
 * Created By Kingbbode
 * blog : http://kingbbode.github.io
 * github : http://github.com/kingbbode
 * 
 * Author                    Date                     Description
 * ------------------       --------------            ------------------
 * kingbbode                2017-08-02      
 */

import org.quartz.JobDataMap;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;

import java.util.*;

/**
 * Spring Batch, Quartz 설정을 도와주는 Helper Class.
 */
public class BatchHelper {
    private static final String JOB_NAME_KEY = "jobName";
    private static final String JOB_PARAMETERS_NAME_KEY = "jobParameters";
    private static final List<String> KEYWORDS = Arrays.asList(JOB_NAME_KEY, JOB_PARAMETERS_NAME_KEY);

    public static JobDetailFactoryBeanBuilder jobDetailFactoryBeanBuilder() {
        return new JobDetailFactoryBeanBuilder();
    }

    public static CronTriggerFactoryBeanBuilder cronTriggerFactoryBeanBuilder() {
        return new CronTriggerFactoryBeanBuilder();
    }

    /**
     * quartz JobDataMap 로부터 job Name 을 추출
     * 
     * @param jobDataMap quartz JobDataMap
     * @return job name
     */
    public static String getJobName(JobDataMap jobDataMap){
        return (String) jobDataMap.get(JOB_NAME_KEY);
    }

    /**
     * quartz JobDataMap 로부터 JobParameters 를 추출
     * 
     * Spring Batch Job 은 Job Name 과 Job Parameter 로 동일 잡을 확인하므로, 
     * 실행 시간을 적재하여 새로운 Job Parameter 를 생성하여 반환.
     * 
     * @param jobDataMap quartz JobDataMap
     * @return Spring Batch JobParameters
     */
    public static JobParameters getJobParameters(JobDataMap jobDataMap){
        return new JobParametersBuilder((JobParameters) jobDataMap.get(JOB_PARAMETERS_NAME_KEY)).addDate("executeDate", new Date()).toJobParameters();
    }

    /**
     * JobDetailFactoryBean Builder
     * 
     * JobDetailFactoryBean 내부 설정 값과 map, JobParametersBuilder 를 조합하여 JobDetailFactoryBean 을 생성
     * @see JobDetailFactoryBean
     * @see JobParametersBuilder
     */
    public static class JobDetailFactoryBeanBuilder {
        
        boolean durability = true;
        boolean requestsRecovery = true;
        private Map<String, Object> map;
        private JobParametersBuilder jobParametersBuilder;
        
        JobDetailFactoryBeanBuilder() {
            this.map = new HashMap<>();
            this.jobParametersBuilder = new JobParametersBuilder();
        }

        /**
         * @param jobName Spring Batch Job Name
         */
        public JobDetailFactoryBeanBuilder jobName(String jobName) {
            this.map.put(JOB_NAME_KEY, jobName);
            return this;
        }

        /**
         * @param durability 작업의 비 지속성 여부. false 인 경우 트리거가 연결되지 않으면 자동 삭제. ( default true )
         */
        public JobDetailFactoryBeanBuilder durability(boolean durability){
            this.durability = durability;
            return this;
        }

        /**
         * @param requestsRecovery 작업의 복구 여부. (실패한 작업에 대한 재실행) ( default true )
         */
        public JobDetailFactoryBeanBuilder requestsRecovery(boolean requestsRecovery){
            this.requestsRecovery = requestsRecovery;
            return this;
        }

        /**
         * Spring Batch Job 으로 전달할 Job Parameter
         * 
         * @param key job parameter key
         * @param value job parameter value
         */
        public JobDetailFactoryBeanBuilder parameter(String key, Object value){
            if(KEYWORDS.contains(key)){
                throw new RuntimeException("Invalid Parameter.");
            }
            this.addParameter(key, value);
            return this;
        }

        private void addParameter(String key, Object value) {
            if (value instanceof String) {
                this.jobParametersBuilder.addString(key, (String) value);
                return;
            } else if (value instanceof Float || value instanceof Double) {
                this.jobParametersBuilder.addDouble(key, ((Number) value).doubleValue());
                return;
            } else if (value instanceof Integer || value instanceof Long) {
                this.jobParametersBuilder.addLong(key, ((Number) value).longValue());
                return;
            } else if (value instanceof Date) {
                this.jobParametersBuilder.addDate(key, (Date) value);
                return;
            } else if (value instanceof JobParameter) {
                this.jobParametersBuilder.addParameter(key, (JobParameter) value);
                return;
            }
            throw new RuntimeException("Not Supported Parameter Type.");
        }

        public JobDetailFactoryBean build(){
            if(!map.containsKey(JOB_NAME_KEY)) {
                throw new RuntimeException("Not Found Job Name.");
            }
            map.put(JOB_PARAMETERS_NAME_KEY, jobParametersBuilder.toJobParameters());

            JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
            jobDetailFactory.setJobClass(BatchJobExecutor.class);
            jobDetailFactory.setDurability(this.durability);
            jobDetailFactory.setRequestsRecovery(this.requestsRecovery);
            jobDetailFactory.setJobDataAsMap(this.map);
            return jobDetailFactory;
        }
    }

    /**
     * CronTriggerFactoryBean Builder
     * 
     * CronTriggerFactoryBean 내부 설정 값을 조합하여 CronTriggerFactoryBean 을 생성
     * @see CronTriggerFactoryBean
     */
    public static class CronTriggerFactoryBeanBuilder {
        private String name;
        private String cronExpression;
        private JobDetailFactoryBean jobDetailFactoryBean;

        /**
         * 작성되지 않으면, bean Name 을 사용
         * @see CronTriggerFactoryBean#setName(String) 
         */
        public CronTriggerFactoryBeanBuilder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * @param cronExpression Quartz 전용 Cron 양식의 Expression 
         * @link http://www.cronmaker.com
         */
        public CronTriggerFactoryBeanBuilder cronExpression(String cronExpression) {
            this.cronExpression = cronExpression;
            return this;
        }

        /**
         * @param jobDetailFactoryBean Quartz Schedule Job Detail Factory
         */
        public CronTriggerFactoryBeanBuilder jobDetailFactoryBean(JobDetailFactoryBean jobDetailFactoryBean) {
            this.jobDetailFactoryBean = jobDetailFactoryBean;
            return this;
        }
        
        public CronTriggerFactoryBean build() {
            if(this.cronExpression == null || this.jobDetailFactoryBean == null){
                throw new RuntimeException("cronExpression and jobDetailFactoryBean is required.");
            }
            CronTriggerFactoryBean cronTriggerFactoryBean = new CronTriggerFactoryBean();
            cronTriggerFactoryBean.setName(this.name);
            cronTriggerFactoryBean.setJobDetail(this.jobDetailFactoryBean.getObject());
            cronTriggerFactoryBean.setCronExpression(this.cronExpression);
            return cronTriggerFactoryBean;
        }
    }
}