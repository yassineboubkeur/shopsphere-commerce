package com.shopsphere.auth_service.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;
import java.util.regex.Pattern;

public class PasswordPolicyValidator implements ConstraintValidator<ValidPassword, CharSequence> {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;

    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");

    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password", "password1", "password12", "password123", "password1234", "password21", "password!",
            "passw0rd", "p@ssw0rd", "pass1234", "pass12345",
            "12345678", "123456789", "1234567890", "12345678a", "123456789a",
            "qwerty123", "qwertyuiop", "qwerty1234", "qwe12345",
            "iloveyou1", "iloveyou12",
            "welcome1", "welcome12", "welcome123",
            "football1", "baseball1", "monkey12", "dragon12",
            "letmein1", "letmein12", "sunshine1", "princess1",
            "admin123", "admin1234", "administrator1", "root1234", "adminadmin",
            "master123", "super123", "shadow12",
            "a1b2c3d4", "abcd1234", "abc12345", "summer12", "hunter22",
            "password@123", "Password123!", "Password1!", "Qwerty123!", "Admin123!", "Welcome1!",
            "Passw0rd!", "Admin@123", "Abcd@1234"
    );

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        String password = value.toString();
        boolean valid = password.length() >= MIN_LENGTH
                && password.length() <= MAX_LENGTH
                && UPPERCASE.matcher(password).find()
                && LOWERCASE.matcher(password).find()
                && DIGIT.matcher(password).find()
                && SPECIAL.matcher(password).find()
                && !COMMON_PASSWORDS.contains(password.toLowerCase());

        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "Password must be 8-64 characters and include uppercase, lowercase, a number and a special character, and it must not be a commonly used password.")
                    .addConstraintViolation();
        }
        return valid;
    }
}