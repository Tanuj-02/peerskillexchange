package com.tanuj.peerskillexchange.service;

import java.util.List;

public interface AiSessionGenerator {
    Result generate(String sessionContext);
    record Result(String summary, List<String> questions) {}
}
