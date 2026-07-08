package com.project.muse_android.ai;

import java.util.List;

public class GeminiResponse {
    public List<Candidate> candidates;

    public static class Candidate {
        public Content content;
    }

    public static class Content {
        public List<Part> parts;
    }

    public static class Part {
        public String text;
    }

    public String getText() {
        if (candidates != null && !candidates.isEmpty() 
            && candidates.get(0).content != null 
            && candidates.get(0).content.parts != null 
            && !candidates.get(0).content.parts.isEmpty()) {
            return candidates.get(0).content.parts.get(0).text;
        }
        return "";
    }
}
