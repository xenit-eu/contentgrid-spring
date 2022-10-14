package com.contentgrid.spring.boot.actuator.policy;

import java.util.Map;
import lombok.Data;

@Data
public class PolicyTemplatingProperties {
    private Map<String, String> variables;
}
