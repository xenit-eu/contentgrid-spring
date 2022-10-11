package com.contentgrid.spring.boot.actuator.webhooks;

import org.springframework.boot.actuate.endpoint.Producible;
import org.springframework.util.MimeType;

public enum WebhookConfigProducible implements Producible<WebhookConfigProducible> {

    CONTENT_TYPE_WEBHOOK_CONFIG_V1 {
        @Override
        public MimeType getProducedMimeType() {
            return MimeType.valueOf("application/vnd.contentgrid.webhooks.v1+json");
        }
    };

    @Override
    public boolean isDefault() {
        return Producible.super.isDefault();
    }
}
