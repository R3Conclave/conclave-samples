import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CclService, PasswordEntry } from '../ccl.service';
import { MatDialog } from '@angular/material/dialog';
// import { ShowPasswordDialogComponent } from '../show-password-dialog/show-password-dialog.component';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-auction',
  templateUrl: './auction.component.html',
  styleUrls: ['./auction.component.scss']
})
export class AuctionComponent implements OnInit {
  public bid = '';
  public textShow = false;
  public auctionWinner = '';
  public winningBid = '';

  constructor(
    private ccl: CclService,
    private router: Router,
    private dialog: MatDialog,
    public auth: AuthService
  ) { }

  ngOnInit(): void {

  }

  public addBid(): void {
    console.log("Hello" + this.auth.email);
    this.ccl.addBid(this.auth.email, this.bid).then(() => {
        console.log(`Inside addBid:`);
        this.router.navigateByUrl('');
    });
  }

  public calculateBidWinner(): void {
      this.ccl.calculateBidWinner().then((entry) => {
          console.log(`Inside calculateBidWinner:` + entry.bid);
          this.winningBid = entry.bid;
          this.auctionWinner = entry.email;
          this.textShow = true;
          this.router.navigateByUrl('');
      });
    }

  public logout() {
    if (confirm("Are you sure you want to logout?")) {
      this.auth.logout();
      this.router.navigateByUrl('/login')
    }
  }
}
