package com.example.agentic.url.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ShortCodeGeneratorTest {

    private final ShortCodeGenerator generator = new ShortCodeGenerator();

    @Test
    void generatesCodeOfRequestedLength() {
        String code = generator.generate(7);
        assertThat(code).hasSize(7);
    }

    @Test
    void generatesOnlyBase62Characters() {
        String code = generator.generate(20);
        assertThat(code).matches("[0-9A-Za-z]+");
    }

    @Test
    void generatesDistinctCodesAcrossManyCalls() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codes.add(generator.generate(7));
        }
        // Collisions are possible in principle but should be effectively absent at this sample size.
        assertThat(codes.size()).isGreaterThan(990);
    }
}
