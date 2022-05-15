package com.biglucas.agena.protocol.gemini;

public class FailedGeminiRequestException extends Exception {
    FailedGeminiRequestException(String reason) {
        super(reason);
    }
    public static class GeminiInvalidResponse extends FailedGeminiRequestException {
        GeminiInvalidResponse() {
            super("Invalid response");
        }
    }
    public static class GeminiUnimplementedCase extends FailedGeminiRequestException {
        GeminiUnimplementedCase() {
            super("Unimplemented case");
        }
    }
    public static class GeminiNotFound extends  FailedGeminiRequestException {
        public GeminiNotFound () {
            super("content not found");
        }
    }

    public static class GeminiGone extends FailedGeminiRequestException {
        public GeminiGone () { super("gone"); }
    }
}
