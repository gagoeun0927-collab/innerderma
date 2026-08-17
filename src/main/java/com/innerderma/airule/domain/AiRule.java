package com.innerderma.airule.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_rules", uniqueConstraints =
        @UniqueConstraint(name = "uk_ai_rule_id_version", columnNames = {"rule_id", "version"}))
public class AiRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id", nullable = false, length = 20)
    private String ruleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiRuleCategory category;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private int priority;

    @Lob
    @Column(name = "conditions_json", nullable = false)
    private String conditionsJson;

    @Lob
    @Column(name = "actions_json", nullable = false)
    private String actionsJson;

    @Lob
    @Column(name = "restrictions_json", nullable = false)
    private String restrictionsJson;

    @Column(name = "explanation_template", length = 1000)
    private String explanationTemplate;

    @Column(nullable = false, length = 30)
    private String version;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AiRule() {}

    public AiRule(String ruleId, AiRuleCategory category, String name, int priority,
                  String conditionsJson, String actionsJson, String restrictionsJson,
                  String explanationTemplate, String version, boolean enabled) {
        this.ruleId = ruleId;
        this.category = category;
        this.name = name;
        this.priority = priority;
        this.conditionsJson = conditionsJson;
        this.actionsJson = actionsJson;
        this.restrictionsJson = restrictionsJson;
        this.explanationTemplate = explanationTemplate;
        this.version = version;
        this.enabled = enabled;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getRuleId() { return ruleId; }
    public AiRuleCategory getCategory() { return category; }
    public String getName() { return name; }
    public int getPriority() { return priority; }
    public String getConditionsJson() { return conditionsJson; }
    public String getActionsJson() { return actionsJson; }
    public String getRestrictionsJson() { return restrictionsJson; }
    public String getExplanationTemplate() { return explanationTemplate; }
    public String getVersion() { return version; }
    public boolean isEnabled() { return enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
