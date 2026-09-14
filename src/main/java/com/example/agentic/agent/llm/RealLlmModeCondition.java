package com.example.agentic.agent.llm;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.NonNull;

public class RealLlmModeCondition implements Condition {

    @Override
    public boolean matches(@NonNull ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
        String mode = context.getEnvironment().getProperty("agentic.agent.llm.mode", "mock");
        return "real".equalsIgnoreCase(mode);
    }
}
