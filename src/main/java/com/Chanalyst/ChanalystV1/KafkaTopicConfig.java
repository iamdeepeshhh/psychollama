//package com.Chanalyst.ChanalystV1;
//
//import org.apache.kafka.clients.admin.NewTopic;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class KafkaTopicConfig {
//
//    @Bean
//    public NewTopic playersTopic() {
//        return new NewTopic("game-players", 1, (short) 1);
//    }
//
//    @Bean
//    public NewTopic questionsTopic() {
//        return new NewTopic("game-questions", 1, (short) 1);
//    }
//
//    @Bean
//    public NewTopic answersTopic() {
//        return new NewTopic("game-answers", 1, (short) 1);
//    }
//
//    @Bean
//    public NewTopic votesTopic() {
//        return new NewTopic("game-votes", 1, (short) 1);
//    }
//
//    @Bean
//    public NewTopic scoresTopic() {
//        return new NewTopic("game-scores", 1, (short) 1);
//    }
//}