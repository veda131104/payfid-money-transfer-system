import { Routes } from '@angular/router';

import { DashboardComponent } from './dashboard/dashboard.component';
import { LoginComponent } from './login/login.component';
import { HistoryComponent } from './history/history.component';
import { TransferComponent } from './transfer/transfer.component';
import { PinConfirmComponent } from './pin-confirm/pin-confirm.component';
import { ProfileComponent } from './profile/profile.component';
import { SignupComponent } from './signup/signup.component';
import { AccountSetupComponent } from './account-setup/account-setup.component';

export const routes: Routes = [
  { path: '', component: LoginComponent },
  { path: 'signup', component: SignupComponent },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'history', component: HistoryComponent },
  { path: 'transfer', component: TransferComponent },
  { path: 'profile', component: ProfileComponent },
  { path: 'account-setup', component: AccountSetupComponent },
  { path: 'pin-confirm', component: PinConfirmComponent },
];
