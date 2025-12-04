import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { TotpSetupComponent } from './pages/totp-setup/totp-setup.component';
import { OtpVerifyComponent } from './pages/otp-verify/otp-verify.component';
import { AccountSettingsComponent } from './pages/account-settings/account-settings.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { AuthGuard } from './core/auth.guard';
import { TempTokenGuard } from './core/temp-token.guard';

const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: '2fa/setup', component: TotpSetupComponent, canActivate: [AuthGuard] },
  { path: '2fa/verify', component: OtpVerifyComponent, canActivate: [TempTokenGuard] },
  { path: 'account', component: AccountSettingsComponent, canActivate: [AuthGuard] },
  { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard] }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
