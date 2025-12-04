import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-totp-setup',
  templateUrl: './totp-setup.component.html',
  styleUrls: ['./totp-setup.component.css']
})
export class TotpSetupComponent implements OnInit {
  loading = true;
  error = '';
  qrCode?: string;
  secret?: string;
  otpauth?: string;

  constructor(private auth: AuthService) { }

  ngOnInit() {
    this.auth.setup2fa().subscribe({
      next: res => {
        this.qrCode = res.qrCodeDataUri;
        this.secret = res.secretBase32;
        this.otpauth = res.otpauthUrl;
        this.loading = false;
      },
      error: err => {
        this.error = err.error?.detail || 'Erro ao gerar QR code';
        this.loading = false;
      }
    });
  }
}
