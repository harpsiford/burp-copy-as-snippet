package com.copyassnippet;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CachingRedactionEngine implements RedactionEngine {
    private final Map<String, CachedPlan> planByPresetId = new ConcurrentHashMap<>();
    private final RedactionPlanCompiler planCompiler;
    private final RequestRedactionProcessor requestProcessor;
    private final ResponseRedactionProcessor responseProcessor;
    private final RegexBodyRedactionProcessor regexBodyProcessor;

    public CachingRedactionEngine() {
        this(
                new RedactionPlanCompiler(),
                new RequestRedactionProcessor(),
                new ResponseRedactionProcessor(),
                new RegexBodyRedactionProcessor()
        );
    }

    CachingRedactionEngine(
            RedactionPlanCompiler planCompiler,
            RequestRedactionProcessor requestProcessor,
            ResponseRedactionProcessor responseProcessor,
            RegexBodyRedactionProcessor regexBodyProcessor) {
        this.planCompiler = planCompiler;
        this.requestProcessor = requestProcessor;
        this.responseProcessor = responseProcessor;
        this.regexBodyProcessor = regexBodyProcessor;
    }

    @Override
    public HttpRequest redactRequest(Preset preset, HttpRequest request) {
        return requestProcessor.redact(request, planFor(preset));
    }

    @Override
    public HttpResponse redactResponse(Preset preset, HttpResponse response) {
        return responseProcessor.redact(response, planFor(preset));
    }

    @Override
    public String format(Preset preset, HttpRequestResponse requestResponse) {
        RedactionPlan plan = planFor(preset);
        String redactedRequest = regexBodyProcessor.redact(
                requestProcessor.redact(requestResponse.request(), plan).toString(),
                plan
        );

        String responseBlock;
        if (requestResponse.response() != null) {
            responseBlock = regexBodyProcessor.redact(
                    responseProcessor.redact(requestResponse.response(), plan).toString(),
                    plan
            );
        } else {
            responseBlock = "No response was received.";
        }

        return plan.template()
                .replace("{{request}}", redactedRequest)
                .replace("{{response}}", responseBlock);
    }

    RedactionPlan planFor(Preset preset) {
        String presetId = preset.getId();
        RedactionPlanVersion version = RedactionPlanVersion.fromPreset(preset);

        CachedPlan cached = planByPresetId.compute(presetId, (id, existing) -> {
            if (existing != null && existing.version.equals(version)) {
                return existing;
            }
            return new CachedPlan(version, planCompiler.compile(preset));
        });
        return cached.plan;
    }

    private static final class CachedPlan {
        private final RedactionPlanVersion version;
        private final RedactionPlan plan;

        private CachedPlan(RedactionPlanVersion version, RedactionPlan plan) {
            this.version = version;
            this.plan = plan;
        }
    }
}
