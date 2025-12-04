import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-otp-verify',
  templateUrl: './otp-verify.component.html',
  styleUrls: ['./otp-verify.component.css']
})
export class OtpVerifyComponent {
  error = '';
  form = this.fb.group({
    otp: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
  });

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) { }

  submit() {
    const tempToken = this.auth.getTempToken();
    if (!tempToken) {
      this.error = 'Token temporário ausente.';
      return;
    }
    const payload = { temporaryToken: tempToken, otp: this.form.value.otp! };
    this.auth.verifyOtp(payload).subscribe({
      next: res => {
        if (res.token) {
          this.router.navigate(['/dashboard']);
        } else {
          this.error = res.message || 'Falha na verificação';
        }
      },
      error: err => this.error = err.error?.detail || 'Erro na verificação'
    });
  }
}
