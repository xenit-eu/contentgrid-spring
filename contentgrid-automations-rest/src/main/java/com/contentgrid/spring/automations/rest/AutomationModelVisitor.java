package com.contentgrid.spring.automations.rest;

import com.contentgrid.spring.automations.rest.AutomationsModel.AutomationModel;
import com.contentgrid.thunx.predicates.model.FunctionExpression;
import com.contentgrid.thunx.predicates.model.Scalar;
import com.contentgrid.thunx.predicates.model.SymbolicReference;
import com.contentgrid.thunx.predicates.model.SymbolicReference.PathElement;
import com.contentgrid.thunx.predicates.model.SymbolicReference.PathElementVisitor;
import com.contentgrid.thunx.predicates.model.ThunkExpression;
import com.contentgrid.thunx.predicates.model.ThunkExpressionVisitor;
import com.contentgrid.thunx.predicates.model.Variable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyAccessorFactory;

class AutomationModelVisitor implements ThunkExpressionVisitor<Scalar<?>, AutomationModel> {

    @Override
    public Scalar<?> visit(Scalar<?> scalar, AutomationModel context) {
        return scalar;
    }

    @Override
    public Scalar<?> visit(FunctionExpression<?> functionExpression, AutomationModel context) {
        var terms = functionExpression.getTerms().stream()
                .map(expr -> expr.accept(this, context))
                .toList();

        return switch (functionExpression.getOperator()) {
            case EQUALS -> {
                assertTwoTerms(terms);
                yield Scalar.of(terms.get(0).equals(terms.get(1)));
            }
            case NOT_EQUAL_TO -> {
                assertTwoTerms(terms);
                yield Scalar.of(!terms.get(0).equals(terms.get(1)));
            }
            case AND -> {
                assertNotEmpty(terms);
                yield Scalar.of(terms.stream()
                        .map(scalar -> scalar.assertResultType(Boolean.class))
                        .map(ThunkExpression::maybeValue)
                        .flatMap(Optional::stream)
                        .map(Boolean.class::cast)
                        .reduce(true, (left, right) -> left && right));
            }
            case OR -> {
                assertNotEmpty(terms);
                yield Scalar.of(terms.stream()
                        .map(scalar -> scalar.assertResultType(Boolean.class))
                        .map(ThunkExpression::maybeValue)
                        .flatMap(Optional::stream)
                        .map(Boolean.class::cast)
                        .reduce(false, (left, right) -> left || right));
            }
            case NOT -> {
                assertOneTerm(terms);
                var term = (Scalar<Boolean>) terms.get(0).assertResultType(Boolean.class);
                yield Scalar.of(!term.getValue());
            }
            default -> {
                throw new UnsupportedOperationException("Operation %s not implemented"
                        .formatted(functionExpression.getOperator().getKey()));
            }
        };
    }

    private static void assertNotEmpty(List<? extends ThunkExpression<?>> terms) {
        if (terms.isEmpty()) {
            throw new IllegalArgumentException("Operation requires at least 1 parameter.");
        }
    }

    private static void assertOneTerm(List<? extends ThunkExpression<?>> terms) {
        if (terms.size() != 1) {
            throw new IllegalArgumentException("Operation requires 1 parameter.");
        }
    }

    private static void assertTwoTerms(List<? extends ThunkExpression<?>> terms) {
        if (terms.size() != 2) {
            throw new IllegalArgumentException("Operation requires 2 parameters.");
        }
    }

    @Override
    public Scalar<?> visit(SymbolicReference symbolicReference, AutomationModel context) {
        String subject = symbolicReference.getSubject().getName();
        if (!"entity".equalsIgnoreCase(subject)) {
            throw new IllegalArgumentException(
                    "Expected symbolic-ref subject named 'entity', but got '" + subject + "'");
        }

        var path = symbolicReference.getPath();
        try {
            Object result = context;
            for (var elem : path) {
                Objects.requireNonNull(result, () -> "Cannot lookup property '%s' of null value"
                        .formatted(getPathElementName(elem)));
                var propertyAccessor = PropertyAccessorFactory.forDirectFieldAccess(result);
                result = propertyAccessor.getPropertyValue(getPathElementName(elem));
            }

            if (result == null) {
                return Scalar.nullValue();
            } else if (result instanceof Long number) {
                return Scalar.of(number);
            } else if (result instanceof Double number) {
                return Scalar.of(number);
            } else if (result instanceof BigDecimal number) {
                return Scalar.of(number);
            } else if (result instanceof Boolean bool) {
                return Scalar.of(bool);
            } else if (result instanceof String string) {
                return Scalar.of(string);
            } else {
                throw new IllegalArgumentException("Field '%s' has unknown type '%s'"
                        .formatted(symbolicReference.toPath(), result.getClass().getSimpleName()));
            }
        } catch (BeansException e) {
            throw new IllegalArgumentException("Field '%s' does not exist".formatted(symbolicReference.toPath()));
        }
    }

    private static String getPathElementName(PathElement elem) {
        return elem.accept(new PathElementVisitor<>() {
            @Override
            public String visit(Scalar<?> scalar) {
                if (scalar.getResultType().equals(String.class)) {
                    return (String) scalar.getValue();
                } else {
                    var msg = String.format( "cannot traverse symbolic reference using scalar of type %s",
                            scalar.getResultType().getSimpleName());
                    throw new UnsupportedOperationException(msg);
                }
            }

            @Override
            public String visit(Variable variable) {
                return variable.getName();
            }
        });
    }

    @Override
    public Scalar<?> visit(Variable variable, AutomationModel context) {
        throw new UnsupportedOperationException("Variables are not supported");
    }
}
