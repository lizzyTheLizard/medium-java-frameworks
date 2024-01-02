import { Component } from '@angular/core';
import { LoadingService } from '../../services/loading.service';
import { BlogService, PostsInner } from '../../../generated';
import { ReplaySubject } from 'rxjs';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { UserInfoService } from '../../services/user-info.service';

@Component({
  selector: 'app-overview-page',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, RouterLink],
  templateUrl: './overview-page.component.html',
  styleUrl: './overview-page.component.scss',
})
export class OverviewPageComponent {
  readonly posts$ = new ReplaySubject<PostsInner[]>();

  constructor(
    public readonly userService: UserInfoService,
    private readonly loadingService: LoadingService,
    private readonly blogService: BlogService,
  ) {
    this.blogService.getAllPosts().subscribe({
      next: (posts) => this.posts$.next(posts),
      complete: () => this.loadingService.loadedPage(),
    });
  }
}
