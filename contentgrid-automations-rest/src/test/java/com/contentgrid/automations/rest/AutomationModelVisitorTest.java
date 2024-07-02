package com.contentgrid.automations.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.contentgrid.automations.rest.AutomationsModel.AutomationModel;
import com.contentgrid.thunx.predicates.model.Comparison;
import com.contentgrid.thunx.predicates.model.LogicalOperation;
import com.contentgrid.thunx.predicates.model.Scalar;
import com.contentgrid.thunx.predicates.model.SymbolicReference;
import com.contentgrid.thunx.predicates.model.ThunkExpression;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AutomationModelVisitorTest {
    private static final AutomationModel AUTOMATION_1 = AutomationModel.builder()
            .id(UUID.randomUUID().toString())
            .system("my-system")
            .name("test1")
            .data(Map.of("text", "hello world!"))
            .annotations(List.of())
            .build();

    private static final AutomationModel AUTOMATION_2 = AutomationModel.builder()
            .id(UUID.randomUUID().toString())
            .system("other-system")
            .name("test2")
            .data(Map.of("color", Map.of("red", 255, "green", 0, "blue", 0)))
            .annotations(List.of())
            .build();

    private static final AutomationModelVisitor VISITOR = new AutomationModelVisitor();

    public static Stream<Arguments> alwaysTruePolicy() {
        return Stream.of(
                Arguments.of(Scalar.of(true)), // true
                Arguments.of(Comparison.areEqual(Scalar.of(true), Scalar.of(true))), // true = true
                Arguments.of(Comparison.areEqual(Scalar.of(5), Scalar.of(5))), // 5 = 5
                Arguments.of(Comparison.notEqual(Scalar.of(true), Scalar.of(false))), // true != false
                Arguments.of(LogicalOperation.conjunction(Scalar.of(true), Scalar.of(true))), // true AND true
                Arguments.of(LogicalOperation.disjunction(Scalar.of(false), Scalar.of(true))), // false OR true
                Arguments.of(LogicalOperation.negation(Scalar.of(false))) // NOT false
        );
    }

    public static Stream<Arguments> alwaysFalsePolicy() {
        return Stream.of(
                Arguments.of(Scalar.of(false)), // false
                Arguments.of(Comparison.areEqual(Scalar.of(true), Scalar.of(false))), // true = false
                Arguments.of(Comparison.notEqual(Scalar.of(true), Scalar.of(true))), // true != true
                Arguments.of(Comparison.notEqual(Scalar.of(5), Scalar.of(5))), // 5 != 5
                Arguments.of(LogicalOperation.conjunction(Scalar.of(false), Scalar.of(true))), // false AND true
                Arguments.of(LogicalOperation.disjunction(Scalar.of(false), Scalar.of(false))), // false OR false
                Arguments.of(LogicalOperation.negation(Scalar.of(true))) // NOT true
        );
    }

    @ParameterizedTest
    @MethodSource
    void alwaysTruePolicy(ThunkExpression<?> expression) {
        assertThat(expression.accept(VISITOR, AUTOMATION_1).getValue()).isEqualTo(true);
        assertThat(expression.accept(VISITOR, AUTOMATION_2).getValue()).isEqualTo(true);
    }

    @ParameterizedTest
    @MethodSource
    void alwaysFalsePolicy(ThunkExpression<?> expression) {
        assertThat(expression.accept(VISITOR, AUTOMATION_1).getValue()).isEqualTo(false);
        assertThat(expression.accept(VISITOR, AUTOMATION_2).getValue()).isEqualTo(false);
    }

    @Test
    void conditionalOnSystemPolicy() {
        var policy = Comparison.areEqual(
                SymbolicReference.of("entity", path -> path.string("system")),
                Scalar.of("my-system")
        );

        assertThat(policy.accept(VISITOR, AUTOMATION_1).getValue()).isEqualTo(true);
        assertThat(policy.accept(VISITOR, AUTOMATION_2).getValue()).isEqualTo(false);
    }

    @Test
    void conditionalOnSystemPolicy_wrongSubject_shouldThrow() {
        var policy = Comparison.areEqual(
                SymbolicReference.of("automation", path -> path.string("system")),
                Scalar.of("my-system")
        );

        assertThatThrownBy(() -> policy.accept(VISITOR, AUTOMATION_1).getValue())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected symbolic-ref subject named 'entity', but got 'automation'");
    }

    @Test
    void conditionalOnSystemPolicy_wrongPath_shouldThrow() {
        var policy = Comparison.areEqual(
                SymbolicReference.of("entity", path -> path.string("sys")),
                Scalar.of("my-system")
        );

        assertThatThrownBy(() -> policy.accept(VISITOR, AUTOMATION_1).getValue())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Field 'entity.sys' does not exist");
    }

}