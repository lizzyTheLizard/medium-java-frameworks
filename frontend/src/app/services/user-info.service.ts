import { Inject, Injectable } from '@angular/core';
import { LoginService, Post, PostsInner, User } from '../../generated';
import { BehaviorSubject, map, Observable } from 'rxjs';
import { DOCUMENT } from '@angular/common';
import { LoadingService } from './loading.service';

@Injectable({
  providedIn: 'root',
})
export class UserInfoService {
  #userInfo = new BehaviorSubject<User | undefined>(undefined);
  public readonly userInfo$: Observable<User | undefined> = this.#userInfo.asObservable();

  constructor(
    private readonly loginService: LoginService,
    private readonly loadingService: LoadingService,
    @Inject(DOCUMENT) private document: Document,
  ) {
    setInterval(() => this.update(), 5000);
    this.update();
  }

  public login() {
    this.loadingService.start();
    //This could be made a variable...
    const baseUrl = 'http://localhost:8080';
    const loginPath = '/api/start-login';
    const currentUrlEnc = encodeURIComponent(this.document.location.href);
    this.document.location.href = `${baseUrl}${loginPath}?redirect=${currentUrlEnc}`;
  }

  public logout() {
    const loadingToken = this.loadingService.start();
    this.loginService.logout().subscribe({
      complete: () => {
        this.update();
        this.loadingService.end(loadingToken);
      },
    });
  }

  public canEdit(post: PostsInner | Post): Observable<boolean> {
    return this.#userInfo.pipe(
      map((userInfo) => {
        if (!userInfo) {
          return false;
        }
        if (userInfo.roles.includes('ADMIN')) {
          return true;
        }
        return userInfo.roles.includes('WRITER') && post.author.id === userInfo.id;
      }),
    );
  }

  public canAdd() {
    return this.#userInfo.pipe(
      map((userInfo) => {
        if (!userInfo) {
          return false;
        }
        if (userInfo.roles.includes('ADMIN')) {
          return true;
        }
        return userInfo.roles.includes('WRITER');
      }),
    );
  }

  private update() {
    this.loginService.getUser().subscribe((login) => {
      if (!login.user && this.#userInfo.value) {
        console.info('Not logged in any more', this.#userInfo.value);
        this.#userInfo.next(undefined);
        return;
      }
      if (login.user && !this.#userInfo.value) {
        console.info('Logged in', login.user);
        this.#userInfo.next(login.user);
      }
    });
  }
}
