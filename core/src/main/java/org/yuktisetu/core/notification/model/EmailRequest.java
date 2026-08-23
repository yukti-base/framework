package org.yuktisetu.core.notification.model;

import java.util.List;
import java.util.Map;

public class EmailRequest {

    private List<String> to;
    private List<String> cc;
    private List<String> bcc;
    private String subject;
    private String templateName;
    private Map<String, Object> templateVariables;

    private EmailRequest() {}

    public static Builder builder() {
        return new Builder();
    }

    // getters

    public List<String> getTo() { return to; }
    public List<String> getCc() { return cc; }
    public List<String> getBcc() { return bcc; }
    public String getSubject() { return subject; }
    public String getTemplateName() { return templateName; }
    public Map<String, Object> getTemplateVariables() { return templateVariables; }

    public static class Builder {
        private final EmailRequest request = new EmailRequest();

        public Builder to(List<String> to) { request.to = to; return this; }
        public Builder cc(List<String> cc) { request.cc = cc; return this; }
        public Builder bcc(List<String> bcc) { request.bcc = bcc; return this; }
        public Builder subject(String subject) { request.subject = subject; return this; }
        public Builder templateName(String templateName) { request.templateName = templateName; return this; }
        public Builder templateVariables(Map<String, Object> vars) { request.templateVariables = vars; return this; }

        public EmailRequest build() { return request; }
    }
}
