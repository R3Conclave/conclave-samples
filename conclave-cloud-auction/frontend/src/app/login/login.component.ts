import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit, OnDestroy {
  public loginValid = true;
  public username = '';
  public password = '';

  constructor(
    private authService: AuthService,
    private router: Router
    ) { 
    }

  ngOnInit(): void {
    if (this.authService.isLoggedIn) {
      this.router.navigateByUrl('')
    }
  }

  ngOnDestroy(): void {
  }

  public onSubmit(): void {
    this.loginValid = true;
    this.authService.login(this.username, this.password);
    this.router.navigateByUrl('')
  }

}
