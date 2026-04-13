package ru.sberbank.sbercrm.saas.doctemplate.document.model;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toUnmodifiableMap;

public enum GenerationJobStatus {
    QUEUED,
    PROCESSING,
    DONE,
    ERROR;

    private static final Map<String, GenerationJobStatus> CONSTANTS = Arrays.stream(values())
        .collect(toUnmodifiableMap(GenerationJobStatus::name, Function.identity()));

    public static GenerationJobStatus fromValue(String value) {
        GenerationJobStatus status = CONSTANTS.get(value);
        if (status == null) {
            throw new IllegalArgumentException("Unknown generation job status: " + value);
        }
        return status;
    }
}
