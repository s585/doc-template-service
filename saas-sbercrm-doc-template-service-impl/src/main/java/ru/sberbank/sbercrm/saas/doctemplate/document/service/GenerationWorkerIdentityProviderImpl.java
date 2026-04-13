package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GenerationWorkerIdentityProviderImpl implements GenerationWorkerIdentityProvider {
    private final UUID workerId;
    private final String workerName;

    @Autowired
    public GenerationWorkerIdentityProviderImpl(
        @Value("${spring.application.name:application}") String applicationName
    ) {
        this(UUID.randomUUID(), buildWorkerName(applicationName, resolveHostName(), resolveProcessId()));
    }

    GenerationWorkerIdentityProviderImpl(UUID workerId, String workerName) {
        this.workerId = workerId;
        this.workerName = workerName;
    }

    @Override
    public UUID getWorkerId() {
        return workerId;
    }

    @Override
    public String getWorkerName() {
        return workerName;
    }

    private static String buildWorkerName(String applicationName, String hostName, String processId) {
        return applicationName + "@" + hostName + ":" + processId;
    }

    private static String resolveHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            return "unknown-host";
        }
    }

    private static String resolveProcessId() {
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        int delimiterIndex = runtimeName.indexOf('@');
        return delimiterIndex >= 0 ? runtimeName.substring(0, delimiterIndex) : runtimeName;
    }
}
