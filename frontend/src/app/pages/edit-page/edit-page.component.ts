import { Component } from '@angular/core';
import { UserInfoService } from '../../services/user-info.service';
import { BlogService, PostUpdate } from '../../../generated';
import { LoadingService } from '../../services/loading.service';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { v4 as uuid } from 'uuid';

@Component({
  selector: 'app-edit-page',
  standalone: true,
  imports: [CommonModule, MatInputModule, ReactiveFormsModule, FormsModule, MatIconModule, MatButtonModule],
  templateUrl: './edit-page.component.html',
  styleUrl: './edit-page.component.scss',
})
export class EditPageComponent {
  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(256)]],
    summary: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(1000)]],
    content: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100000)]],
  });
  id: string | undefined;

  constructor(
    private readonly loadingService: LoadingService,
    public readonly userInfoService: UserInfoService,
    private readonly activatedRoute: ActivatedRoute,
    private readonly blogService: BlogService,
    private readonly router: Router,
    private readonly fb: FormBuilder,
  ) {
    this.activatedRoute.params.pipe(takeUntilDestroyed()).subscribe((params) => this.loadBlog(params['id']));
  }

  save() {
    const loadingToken = this.loadingService.start();
    const id = this.id ?? uuid();
    const post: PostUpdate = {
      id: id,
      ...this.form.getRawValue(),
    };
    this.blogService.createOrUpdatePost(id, post).subscribe({
      complete: () => {
        this.router.navigate([id]).then(() => this.loadingService.end(loadingToken));
      },
    });
  }

  delete() {
    const loadingToken = this.loadingService.start();
    this.blogService.deletePost(this.id!).subscribe({
      complete: () => {
        this.router.navigate(['/']).then(() => this.loadingService.end(loadingToken));
      },
    });
  }

  private loadBlog(id: string) {
    if (!id) {
      this.id = undefined;
      this.form.reset();
      this.loadingService.loadedPage();
      return;
    }
    this.id = id;
    const loadingToken = this.loadingService.start();
    this.blogService.getPost(id).subscribe({
      next: (post) =>
        this.userInfoService.canEdit(post).subscribe((b) => {
          if (!b) console.error('Cannot edit blog post!');
          this.form.setValue({ title: post.title, content: post.content, summary: post.summary });
        }),
      complete: () => this.loadingService.loadedPage(loadingToken),
    });
  }
}
