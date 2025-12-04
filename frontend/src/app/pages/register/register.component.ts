import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {
  error = '';
  success = '';
  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) { }

  submit() {
    if (this.form.invalid) return;
    this.error = '';
    this.auth.register(this.form.value as any).subscribe({
      next: () => {
        this.success = 'Cadastro concluído! Faça login para continuar.';
        setTimeout(() => this.router.navigate(['/login']), 600);
      },
      error: err => this.error = err.error?.detail || 'Erro ao cadastrar'
    });
  }
}
