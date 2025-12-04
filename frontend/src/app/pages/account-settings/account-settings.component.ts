import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-account-settings',
  templateUrl: './account-settings.component.html',
  styleUrls: ['./account-settings.component.css']
})
export class AccountSettingsComponent {
  message = '';
  error = '';
  form = this.fb.group({
    otp: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
  });

  constructor(private fb: FormBuilder, private auth: AuthService) { }

  disable2fa() {
    if (this.form.invalid) return;
    this.auth.disable2fa(this.form.value as any).subscribe({
      next: () => {
        this.message = '2FA desativado';
        this.error = '';
      },
      error: err => {
        this.error = err.error?.detail || 'Erro ao desativar';
        this.message = '';
      }
    });
  }
}
