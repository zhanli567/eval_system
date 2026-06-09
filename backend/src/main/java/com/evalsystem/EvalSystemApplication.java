package com.evalsystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({"com.evalsystem.dataset.mapper", "com.evalsystem.tag.mapper", "com.evalsystem.evaluator.mapper", "com.evalsystem.task.mapper"})
public class EvalSystemApplication {
  public static void main(String[] args) {
    SpringApplication.run(EvalSystemApplication.class, args);
  }
}
