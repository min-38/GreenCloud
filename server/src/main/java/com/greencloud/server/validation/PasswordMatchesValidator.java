package com.greencloud.server.validation;

import com.greencloud.server.dto.requests.SignUpRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Objects;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, SignUpRequest> {

    @Override
    public boolean isValid(SignUpRequest value, ConstraintValidatorContext context) {
        // null은 다른 @NotBlank들이 잡도록 true로 둠
        if (value == null) return true;

        boolean matches = Objects.equals(value.getPassword(), value.getPassword2());
        if (!matches) {
            // 기본 메시지 대신 특정 필드(password2)에 에러를 연결
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("비밀번호가 일치하지 않습니다.")
                    .addPropertyNode("password2")
                    .addConstraintViolation();
        }
        return matches;
    }
}