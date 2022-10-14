package com.contentgrid.spring.boot.actuator.policy;

import org.springframework.boot.actuate.endpoint.Producible;
import org.springframework.util.MimeType;

public enum RegoProducible implements Producible<RegoProducible> {

    CONTENT_TYPE_REGO_POLICY_V1 {
        @Override
        public MimeType getProducedMimeType() {
            return MimeType.valueOf("application/vnd.cncf.openpolicyagent.policy.layer.v1+rego");
        }
    };

    @Override
    public boolean isDefault() {
        return Producible.super.isDefault();
    }
}
