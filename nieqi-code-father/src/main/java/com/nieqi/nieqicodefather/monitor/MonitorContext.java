package com.nieqi.nieqicodefather.monitor;

public class MonitorContext {

    private String userId;
    private String appId;
    private String codeGenType;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getCodeGenType() {
        return codeGenType;
    }

    public void setCodeGenType(String codeGenType) {
        this.codeGenType = codeGenType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final MonitorContext context = new MonitorContext();

        public Builder userId(String userId) {
            context.setUserId(userId);
            return this;
        }

        public Builder appId(String appId) {
            context.setAppId(appId);
            return this;
        }

        public Builder codeGenType(String codeGenType) {
            context.setCodeGenType(codeGenType);
            return this;
        }

        public MonitorContext build() {
            return context;
        }
    }
}

