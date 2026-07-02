package com.agentnexus.backend.remoteCall.config;

import com.agentnexus.backend.remoteCall.client.MasterServiceClient;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Component;

@Component
@JalorContextRoot("${agentnexus.master.service.url}")
@JalorClient(MasterServiceClient.class)
public class MasterServiceClientRegistry extends JalorClientRegistry {
}

class JalorClientRegistry {
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface JalorContextRoot {
  String value();
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface JalorClient {
  Class<?> value();
}
