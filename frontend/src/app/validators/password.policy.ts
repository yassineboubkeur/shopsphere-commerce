import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export const PASSWORD_MIN = 8;
export const PASSWORD_MAX = 64;

const COMMON_PASSWORDS = new Set([
  'password', 'password1', 'password12', 'password123', 'password1234', 'password21',
  'passw0rd', 'p@ssw0rd', 'pass1234', 'pass12345',
  '12345678', '123456789', '1234567890', '12345678a', '123456789a',
  'qwerty123', 'qwertyuiop', 'qwerty1234', 'qwe12345',
  'iloveyou1', 'iloveyou12',
  'welcome1', 'welcome12', 'welcome123',
  'football1', 'baseball1', 'monkey12', 'dragon12',
  'letmein1', 'letmein12', 'sunshine1', 'princess1',
  'admin123', 'admin1234', 'administrator1', 'root1234', 'adminadmin',
  'master123', 'super123', 'shadow12',
  'a1b2c3d4', 'abcd1234', 'abc12345', 'summer12', 'hunter22',
  'password@123', 'password123!', 'password1!', 'qwerty123!', 'admin123!', 'welcome1!',
  'passw0rd!', 'admin@123', 'abcd@1234',
]);

export interface PasswordRules {
  lengthOk: boolean;
  upperOk: boolean;
  lowerOk: boolean;
  digitOk: boolean;
  specialOk: boolean;
  personalOk: boolean;
}

export interface PasswordEvaluation {
  score: number;
  rules: PasswordRules;
}

export function evaluatePassword(password: string, username: string, email: string): PasswordEvaluation {
  const rules: PasswordRules = {
    lengthOk: password.length >= PASSWORD_MIN && password.length <= PASSWORD_MAX,
    upperOk: /[A-Z]/.test(password),
    lowerOk: /[a-z]/.test(password),
    digitOk: /[0-9]/.test(password),
    specialOk: /[^A-Za-z0-9]/.test(password),
    personalOk: true,
  };

  const lower = password.toLowerCase();
  const usernameLower = (username ?? '').toLowerCase().trim();
  const emailLocal = ((email ?? '').split('@')[0] ?? '').toLowerCase().trim();
  if (
    lower === '' ||
    COMMON_PASSWORDS.has(lower) ||
    (usernameLower.length > 2 && lower.includes(usernameLower)) ||
    (emailLocal.length > 2 && lower.includes(emailLocal))
  ) {
    rules.personalOk = false;
  }

  const score = Object.values(rules).filter(Boolean).length;
  return { score, rules };
}

export function passwordPolicyValidator(control: AbstractControl): ValidationErrors | null {
  const parent = control.parent;
  const username = parent?.get('username')?.value ?? '';
  const email = parent?.get('email')?.value ?? '';
  const { rules } = evaluatePassword(control.value ?? '', username, email);
  const valid = rules.lengthOk && rules.upperOk && rules.lowerOk && rules.digitOk && rules.specialOk && rules.personalOk;
  return valid ? null : { passwordPolicy: true };
}

export function strengthLabel(score: number): string {
  if (score >= 6) return 'Strong';
  if (score >= 5) return 'Good';
  if (score >= 4) return 'Fair';
  return 'Weak';
}

export function strengthPct(score: number): number {
  return (score / 6) * 100;
}