import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Navbar } from './components/navbar';
import { Footer } from './components/footer';
import { ConfirmDialog } from './components/confirm-dialog';

@Component({
  imports: [RouterOutlet, Navbar, Footer, ConfirmDialog],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {}
