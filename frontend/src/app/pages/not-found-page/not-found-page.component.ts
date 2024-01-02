import { Component, OnInit } from '@angular/core';
import { LoadingService } from '../../services/loading.service';

@Component({
  selector: 'app-not-found-page',
  standalone: true,
  imports: [],
  templateUrl: './not-found-page.component.html',
  styleUrl: './not-found-page.component.scss',
})
export class NotFoundPageComponent implements OnInit {
  constructor(private readonly loadingService: LoadingService) {}

  ngOnInit() {
    this.loadingService.loadedPage();
  }
}
