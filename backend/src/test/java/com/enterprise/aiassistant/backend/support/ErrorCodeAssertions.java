package com.enterprise.aiassistant.backend.support;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Assertion helper that pins the exact {@link ErrorCode} of a thrown business exception.
 */
public final class ErrorCodeAssertions {

    private ErrorCodeAssertions() {
    }

    public static BusinessException assertBusinessError(Throwable thrown, ErrorCode expected) {

        assertThat(thrown)
                .as("expected a business exception with error code %s", expected)
                .isInstanceOf(BusinessException.class);

        BusinessException exception = (BusinessException) thrown;

        assertThat(exception.getErrorCode()).isEqualTo(expected);

        return exception;
    }

    public static BusinessException assertBusinessErrorOfType(
            Throwable thrown,
            Class<? extends BusinessException> expectedType,
            ErrorCode expectedCode
    ) {

        assertThat(thrown).isInstanceOf(expectedType);

        BusinessException exception = (BusinessException) thrown;

        assertThat(exception.getErrorCode()).isEqualTo(expectedCode);

        return exception;
    }
}
