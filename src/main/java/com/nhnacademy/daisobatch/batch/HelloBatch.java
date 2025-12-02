//package com.nhnacademy.daisobatch.batch;
//
//import org.springframework.batch.core.Job;
//import org.springframework.batch.core.Step;
//import org.springframework.batch.core.job.builder.JobBuilder;
//import org.springframework.batch.core.step.builder.StepBuilder;
//import org.springframework.batch.repeat.RepeatStatus;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//// HelloBatch.java
//@Configuration
//public class HelloBatch {
//    @Bean
//    public Job helloJob() {
//        return new JobBuilder("helloJob", jobRepository)
//                .start(helloStep())
//                .build();
//    }
//
//    @Bean
//    public Step helloStep() {
//        return new StepBuilder("helloStep", jobRepository)
//                .tasklet((contribution, chunkContext) -> {
//                    System.out.println("Hello Batch!");
//                    return RepeatStatus.FINISHED;
//                }, platformTransactionManager)
//                .build();
//    }
//}
