import { Component } from '@angular/core';
import { LoadingService } from '../../services/loading.service';
import { CommonModule } from '@angular/common';
import { ReplaySubject } from 'rxjs';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BlogService, Post } from '../../../generated';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { UserInfoService } from '../../services/user-info.service';

@Component({
  selector: 'app-blog-page',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatCardModule, RouterLink],
  templateUrl: './blog-page.component.html',
  styleUrl: './blog-page.component.scss',
})
export class BlogPageComponent {
  readonly post$ = new ReplaySubject<Post>();

  constructor(
    private readonly loadingService: LoadingService,
    public readonly userService: UserInfoService,
    private readonly activatedRoute: ActivatedRoute,
    private readonly blogService: BlogService,
  ) {
    this.activatedRoute.params.pipe(takeUntilDestroyed()).subscribe((params) => this.loadBlog(params['id']));
  }

  private loadBlog(id: string) {
    const loadingToken = this.loadingService.start();
    this.blogService.getPost(id).subscribe({
      next: (post) => this.post$.next(post),
      complete: () => this.loadingService.loadedPage(loadingToken),
    });
  }
}
