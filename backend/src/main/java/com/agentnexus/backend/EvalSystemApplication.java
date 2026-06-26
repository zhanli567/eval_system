package com.agentnexus.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({"com.agentnexus.backend.dataset.mapper", "com.agentnexus.backend.tag.mapper", "com.agentnexus.backend.evaluator.mapper", "com.agentnexus.backend.task.mapper"})
public class EvalSystemApplication {
  public static void main(String[] args) {
    SpringApplication.run(EvalSystemApplication.class, args);
  }
}
